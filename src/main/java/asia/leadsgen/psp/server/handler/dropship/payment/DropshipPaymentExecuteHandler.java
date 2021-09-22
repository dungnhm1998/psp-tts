package asia.leadsgen.psp.server.handler.dropship.payment;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.external.api.SSPApiConnector;
import asia.leadsgen.psp.obj.Address;
import asia.leadsgen.psp.obj.OrderToFinanceObj;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service.OrderToFinanceService;
import asia.leadsgen.psp.service.PaymentService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.HttpClientUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipPaymentExecuteHandler implements Handler<RoutingContext> {

	private String stripeCustomerId;
	private static String invoicerName;
	private static String invoicerEmail;
	private static String invoicerWebsite;
	private static String invoicerTaxId;
	private static String invoicerLogoUrl;
	private static String paspAPIBaseURL;

	public static String getInvoicerName() {
		return invoicerName;
	}

	public static void setInvoicerName(String invoicerName) {
		DropshipPaymentExecuteHandler.invoicerName = invoicerName;
	}

	public static String getInvoicerEmail() {
		return invoicerEmail;
	}

	public static void setInvoicerEmail(String invoicerEmail) {
		DropshipPaymentExecuteHandler.invoicerEmail = invoicerEmail;
	}

	public static String getInvoicerWebsite() {
		return invoicerWebsite;
	}

	public static void setInvoicerWebsite(String invoicerWebsite) {
		DropshipPaymentExecuteHandler.invoicerWebsite = invoicerWebsite;
	}

	public static String getInvoicerTaxId() {
		return invoicerTaxId;
	}

	public static void setInvoicerTaxId(String invoicerTaxId) {
		DropshipPaymentExecuteHandler.invoicerTaxId = invoicerTaxId;
	}

	public static String getInvoicerLogoUrl() {
		return invoicerLogoUrl;
	}

	public static void setInvoicerLogoUrl(String invoicerLogoUrl) {
		DropshipPaymentExecuteHandler.invoicerLogoUrl = invoicerLogoUrl;
	}

	public static String getPaspAPIBaseURL() {
		return paspAPIBaseURL;
	}

	public static void setPaspAPIBaseURL(String paspAPIBaseURL) {
		DropshipPaymentExecuteHandler.paspAPIBaseURL = paspAPIBaseURL;
	}

	private List<String> defaultPaymentMethods = Arrays.asList("card", "payout");

	private static HttpServiceConfig paymentConnectorServiceConfig;

	public static void setPaymentConnectorServiceConfig(HttpServiceConfig paymentConnectorServiceConfig) {
		DropshipPaymentExecuteHandler.paymentConnectorServiceConfig = paymentConnectorServiceConfig;
	}

	private static HttpServiceConfig aspConnectorServiceConfig;

	public static void setAspConnectorServiceConfig(HttpServiceConfig aspConnectorServiceConfig) {
		DropshipPaymentExecuteHandler.aspConnectorServiceConfig = aspConnectorServiceConfig;
	}

	@Override
	public void handle(RoutingContext routingContext) {

		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		Map requestBodyMap = routingContext.getBodyAsJson().getMap();
		LOGGER.info("requestBodyMap=" + requestBodyMap.toString());

		routingContext.vertx().executeBlocking(future -> {

			try {

				String method = ParamUtil.getString(requestBodyMap, AppParams.METHOD);
				if (StringUtils.isEmpty(method) || !defaultPaymentMethods.contains(method)) {
					throw new BadRequestException(SystemError.INVALID_PAYMENT_METHOD);
				}

//				String affUserId = ContextUtil.getString(routingContext, AppParams.USER_ID);

//				String aspUserId = ParamUtil.getString(UserService.get(userId), AppParams.ASP_ID);

				String currency = ParamUtil.getString(requestBodyMap, AppParams.CURRENCY);
				if (StringUtils.isEmpty(currency)) {
					throw new BadRequestException(SystemError.INVALID_PAYMENT_CURRENCY);
				}

				List<Map> orders = ParamUtil.getListData(requestBodyMap, AppParams.ORDERS);
				if (CollectionUtils.isEmpty(orders)) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
				}

				Map dbOrder = null;
				String dbOrderState = "";
				List<Map> addVerifyList = new ArrayList<>();
				for (Map order : orders) {
					Map addVerifyMap = new LinkedHashMap<>();
					dbOrder = DropshipOrderService.lookUp(ParamUtil.getString(order, AppParams.ID), false, false,
							false);
					dbOrderState = ParamUtil.getString(dbOrder, AppParams.STATE);
					if (!ResourceStates.CREATED.equalsIgnoreCase(dbOrderState)
							&& !ResourceStates.CHARGE_FAIL.equalsIgnoreCase(dbOrderState)) {
						throw new BadRequestException(SystemError.INVALID_ORDER);
					}

					Map shippingInfoMap = ParamUtil.getMapData(dbOrder, AppParams.SHIPPING);
					Map address = ParamUtil.getMapData(shippingInfoMap, AppParams.ADDRESS);
					String countryCode = ParamUtil.getString(address, AppParams.COUNTRY);

					String addrVerifiedNote = ParamUtil.getString(address, AppParams.ADDR_VERIFIED_NOTE);				
					if ("US".equalsIgnoreCase(countryCode)) {
						if (addrVerifiedNote.isEmpty()) {
							Map verifyResult = checkShippingInfo(shippingInfoMap);
							if (ParamUtil.getBoolean(verifyResult, "success") == false &&
								verifyResult.containsKey("reason")) {
								addVerifyMap.put(AppParams.ID, ParamUtil.getString(order, AppParams.ID));
								addVerifyMap.putAll(verifyResult);
								addVerifyList.add(addVerifyMap);
							}
						}					
					}						

				}
				if (addVerifyList != null && addVerifyList.isEmpty() == false) {
					Map addressError = new LinkedHashMap<>();
					addressError.put("error_address", addVerifyList);

					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, addressError);
					future.complete();
				} else {
					lookupCardCustomerIdAndProcessPayment(future, routingContext, userId, orders, method, currency);
				}

			} catch (Exception e) {
				routingContext.fail(e);
			}
		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	private Map checkShippingInfo(Map shippingInfo) throws SQLException {

		String name = ParamUtil.getString(shippingInfo, AppParams.NAME);

		Map address = ParamUtil.getMapData(shippingInfo, AppParams.ADDRESS);

		String line1 = ParamUtil.getString(address, AppParams.LINE1);
		String line2 = ParamUtil.getString(address, AppParams.LINE2);
		String city = ParamUtil.getString(address, AppParams.CITY);
		String state = ParamUtil.getString(address, AppParams.STATE);
		String postalCode = ParamUtil.getString(address, AppParams.POSTAL_CODE);
		String countryCode = ParamUtil.getString(address, AppParams.COUNTRY);

		Map verifyResult = new LinkedHashMap<>();
		Address addressObj = new Address(name, line1, line2, city, state, postalCode, countryCode, "");
		verifyResult = SSPApiConnector.verifyAddress(addressObj).getMap();

		return verifyResult;
	}

	private void lookupCardCustomerIdAndProcessPayment(Future<Object> future, RoutingContext routingContext,
			String aspUserId, List<Map> orders, String method, String currency) {
		String aspRequestApi = "/affiliate/" + aspUserId + "/card-info";
		String aspRequestString = "";
		HttpClientRequest aspConnectorRequest = HttpClientUtil.createHttpRequest(aspConnectorServiceConfig,
				aspRequestApi, HttpMethod.GET, new LinkedHashMap<>(), aspRequestString);
		aspConnectorRequest.handler(
				aspResponse -> aspResponseHandler(future, routingContext, aspResponse, orders, method, currency));
		aspConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
		aspConnectorRequest.write(aspRequestString);
		aspConnectorRequest.end();
	}

	private void aspResponseHandler(Future<Object> future, RoutingContext routingContext,
			HttpClientResponse aspResponse, List<Map> orders, String method, String currency) {

		int responseCode = aspResponse.statusCode();
		aspResponse.bodyHandler(responseBody -> {
			JsonObject responseBodyJson = (responseCode == HttpResponseStatus.OK.code())
					? new JsonObject(responseBody.toString("UTF-8"))
					: new JsonObject();
			Map responseBodyMap = responseBodyJson.getMap();

			String paymentType = ParamUtil.getString(responseBodyMap, AppParams.TYPE);

			LOGGER.info("aspResponseHandler:" + responseBodyMap);

			switch (paymentType) {
			case "card":

				stripeCustomerId = ParamUtil.getString(responseBodyMap, AppParams.CUSTOMER_ID);

				if (responseCode != HttpResponseStatus.OK.code() || StringUtils.isEmpty(stripeCustomerId)) {
					throw new BadRequestException(SystemError.INVALID_PAYMENT_METHOD);
				}

				try {

					processPayment(routingContext, orders, method, currency);

				} catch (SQLException | ParseException e) {
					LOGGER.severe(e.getMessage());
					routingContext.fail(e);
				}

				break;

			case "paypal":

				String receiverEmailInvoice = ParamUtil.getString(responseBodyMap, AppParams.EMAIL);

				try {
					processOrderPaymentViaPayPal(future, routingContext, orders, receiverEmailInvoice);
				} catch (SQLException | UnirestException | ParseException e) {
					LOGGER.severe(e.getMessage());
					routingContext.fail(e);
				}

				break;

			case "payoneer":

				stripeCustomerId = ParamUtil.getString(responseBodyMap, AppParams.CUSTOMER_ID);

				try {
					processPayoneerPayment(routingContext, orders, method, currency);
				} catch (SQLException | ParseException e) {
					LOGGER.severe(e.getMessage());
					routingContext.fail(e);
				}

				break;

			default:
				break;
			}

		});
		aspResponse.exceptionHandler(throwable -> routingContext.fail(throwable));
	}

	private void processPayment(RoutingContext routingContext, List<Map> orders, String method, String currency)

			throws SQLException, ParseException {

		String orderId = "", orderState = "";
		double paymentAmount = 0.0d, orderAmount = 0.0d;
		NumberFormat nbf = new DecimalFormat("#0.00");
		Map orderInfo = null;
		Map paymentInfoMap = null;

		List<String> paymentIds = new ArrayList<>();
		List<String> orderIds = new ArrayList<>();
		for (Map order : orders) {

			orderId = ParamUtil.getString(order, AppParams.ID);
			orderInfo = DropshipOrderService.lookUp(orderId, false, false, false);
			orderState = ParamUtil.getString(orderInfo, AppParams.STATE);
			orderAmount = ParamUtil.getDouble(orderInfo, AppParams.AMOUNT);
			paymentAmount += orderAmount;
			paymentInfoMap = PaymentService.insert(AppParams.SALE, orderId, "stripe", nbf.format(orderAmount),
					currency);
			paymentIds.add(ParamUtil.getString(paymentInfoMap, AppParams.ID));
			orderIds.add(orderId);
		}

		Map paymentRequestBody = new LinkedHashMap<>();

		paymentRequestBody.put(AppParams.REFERENCE, "");
		paymentRequestBody.put(AppParams.AMOUNT, nbf.format(paymentAmount));
		paymentRequestBody.put(AppParams.CURRENCY, currency);
		paymentRequestBody.put(AppParams.METHOD, "stripe");
		paymentRequestBody.put(AppParams.CUSTOMER_ID, stripeCustomerId);
		paymentRequestBody.put(AppParams.ORDER_ID, String.join(",", orderIds));

		String paymentConnectorRequestURI = "/charge/customer";
		String paymentRequestString = new JsonObject(paymentRequestBody).encode();
		HttpClientRequest paymentConnectorRequest = HttpClientUtil.createHttpRequest(paymentConnectorServiceConfig,
				paymentConnectorRequestURI, HttpMethod.POST, new LinkedHashMap<>(), paymentRequestString);

		paymentConnectorRequest.handler(paymentConnectorResponse -> paymentResponseHandler(routingContext, paymentIds,
				orderIds, paymentConnectorResponse));
		paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
		paymentConnectorRequest.write(paymentRequestString);
		paymentConnectorRequest.end();

	}

	private void processPayoneerPayment(RoutingContext routingContext, List<Map> orders, String method, String currency)

			throws SQLException, ParseException {

		String orderId = "", orderState = "";
		double paymentAmount = 0.0d, orderAmount = 0.0d;
		NumberFormat nbf = new DecimalFormat("#0.00");
		Map orderInfo = null;
		Map paymentInfoMap = null;

		List<String> paymentIds = new ArrayList<>();
		List<String> orderIds = new ArrayList<>();
		for (Map order : orders) {

			orderId = ParamUtil.getString(order, AppParams.ID);
			orderInfo = DropshipOrderService.lookUp(orderId, false, false, false);
			orderState = ParamUtil.getString(orderInfo, AppParams.STATE);
			orderAmount = ParamUtil.getDouble(orderInfo, AppParams.AMOUNT);
			paymentAmount += orderAmount;
			paymentInfoMap = PaymentService.insert(AppParams.SALE, orderId, "payoneer", nbf.format(orderAmount),
					currency);
			paymentIds.add(ParamUtil.getString(paymentInfoMap, AppParams.ID));
			LOGGER.info("paymentIds=" + paymentIds.toString());
			orderIds.add(orderId);
		}

		Map paymentRequestBody = new LinkedHashMap<>();
		String reference = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
		paymentRequestBody.put(AppParams.REFERENCE, reference);
		paymentRequestBody.put(AppParams.AMOUNT, nbf.format(paymentAmount));
		paymentRequestBody.put(AppParams.CURRENCY, currency);
		paymentRequestBody.put(AppParams.METHOD, "payoneer");
		paymentRequestBody.put(AppParams.CUSTOMER_ID, stripeCustomerId);
		paymentRequestBody.put(AppParams.ORDER_ID, String.join(",", orderIds));

		String paymentConnectorRequestURI = "/charge/payoneer";
		String paymentRequestString = new JsonObject(paymentRequestBody).encode();
		HttpClientRequest paymentConnectorRequest = HttpClientUtil.createHttpRequest(paymentConnectorServiceConfig,
				paymentConnectorRequestURI, HttpMethod.POST, new LinkedHashMap<>(), paymentRequestString);

		paymentConnectorRequest.handler(paymentConnectorResponse -> paymentResponseHandler(routingContext, paymentIds,
				orderIds, paymentConnectorResponse));
		paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
		paymentConnectorRequest.write(paymentRequestString);
		paymentConnectorRequest.end();

	}

	private void paymentResponseHandler(RoutingContext routingContext, List<String> paymentIds, List<String> orderIds,
			HttpClientResponse paymentResponse) {

		int responseCode = paymentResponse.statusCode();
		String responseMsg = paymentResponse.statusMessage();

		paymentResponse.bodyHandler(responseBody -> {
			try {
				JsonObject responseBodyJson = (responseCode == HttpResponseStatus.CREATED.code())
						? new JsonObject(responseBody.toString("UTF-8"))
						: new JsonObject();
				LOGGER.info("paymentResponseHandler()- responseBodyJson= " + responseBodyJson.toString());
				
				Map responseBodyMap = responseBodyJson.getMap();
				String paymentState = (responseCode == HttpResponseStatus.CREATED.code())
						? ParamUtil.getString(responseBodyMap, AppParams.STATE)
						: ResourceStates.FAIL;
				String gatewayPaymentId = ParamUtil.getString(responseBodyMap, AppParams.ID);
				String payerId = ParamUtil.getString(responseBodyMap, AppParams.CUSTOMER_ID);
				String txnId = ParamUtil.getString(responseBodyMap, AppParams.PAY_ID);
				String tokenId = ParamUtil.getString(responseBodyMap, AppParams.REFERENCE);
				String accountName = ParamUtil.getString(responseBodyMap, "account_name");
				LOGGER.info("tokenId=" + tokenId);
				Map paymentResultMap = new LinkedHashMap();

				if (responseCode == HttpResponseStatus.CREATED.code()) {
					if (paymentState.equalsIgnoreCase(ResourceStates.FAIL)) {
						paymentResultMap.put(AppParams.REASON, ParamUtil.getMapData(responseBodyMap, AppParams.REASON));
						paymentResultMap.put(AppParams.STATE, ResourceStates.FAIL);
					} else {
						paymentResultMap.put(AppParams.STATE, ResourceStates.PURCHASED);
					}
				} else {
					if (responseBodyMap.isEmpty()) {
						responseBodyMap.put(AppParams.NAME, SystemError.PAYMENT_PROCESSING_ERROR.getName());
						responseBodyMap.put(AppParams.CODE, responseCode);
						responseBodyMap.put(AppParams.MESSAGE, SystemError.PAYMENT_PROCESSING_ERROR.getMessage());
						responseBodyMap.put(AppParams.DETAILS, SystemError.PAYMENT_PROCESSING_ERROR.getDetails());
						responseBodyMap.put(AppParams.INFORMATION_LINK,
								SystemError.PAYMENT_PROCESSING_ERROR.getInformationLink());
					}
					paymentResultMap.put(AppParams.REASON, responseBodyMap);
				}
				LOGGER.info("tokenId=" + tokenId);
				for (String paymentId : paymentIds) {
					LOGGER.info("paymentId=" + paymentId);
					PaymentService.update(paymentId, paymentState, gatewayPaymentId, payerId, txnId, tokenId,
							new JsonObject(paymentResultMap).encode(), "", "", accountName);
				}

				if (StringUtils.equalsIgnoreCase(paymentState, ResourceStates.APPROVED)) {
					for (String orderId : orderIds) {
						DropshipOrderService.updateState(orderId, ResourceStates.PLACED);
						OrderToFinanceService.save(new OrderToFinanceObj(orderId, ResourceStates.CREATED, true));
					}
				}

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, paymentResultMap);
				routingContext.next();

			} catch (Exception e) {
				routingContext.fail(e);
			}
		});
		paymentResponse.exceptionHandler(throwable -> routingContext.fail(throwable));
	}

	// paypal

	private void processOrderPaymentViaPayPal(Future<Object> future, RoutingContext routingContext, List<Map> orders,
			String receiverEmailInvoice) throws SQLException, UnirestException, ParseException {

		String invoiceNumber = getRandomString16();
		String method = AppParams.PAYPAL;
		String state = ResourceStates.CREATED;
		String reference = "";

		try {
			int totalOrderSuccess = 0;
			List<Map> orderItems = new ArrayList<>();

			for (Map order : orders) {

				String orderId = ParamUtil.getString(order, AppParams.ID);
				if (StringUtils.isEmpty(receiverEmailInvoice)) {
//					DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);

				} else {

					Map orderInfo = DropshipOrderService.lookUp(orderId, false, false, false);
					// orderState = ParamUtil.getString(orderInfo, AppParams.STATE);
					Double orderAmount = ParamUtil.getDouble(orderInfo, AppParams.AMOUNT);

					int quantity = ParamUtil.getInt(orderInfo, AppParams.QUANTITY);

					String currency = ParamUtil.getString(orderInfo, AppParams.CURRENCY);

					if (PaymentService.payPalCreateInvoice(orderId, state, reference, orderAmount.toString(), "USD", "",
							method, invoiceNumber)) {
						totalOrderSuccess++;

						// create map item order invoice

						Map orderBody = new LinkedHashMap<>();

						orderBody.put("order_id", orderId);
						orderBody.put("name", orderId);
						orderBody.put("description", "");
						orderBody.put("quantity", 1);// quy ước là 1 order.

						Map unit_amount = new LinkedHashMap<>();
						unit_amount.put("currency_code", currency);
						unit_amount.put("value", orderAmount.toString());
						orderBody.put("unit_amount", unit_amount);

						Map tax = new LinkedHashMap<>();
						tax.put("name", "Sales Tax");
						tax.put("percent", "0");
						orderBody.put("tax", tax);

						Map amount = new LinkedHashMap<>();
						amount.put("currency_code", currency);
						amount.put("value", "0");

						Map discount = new LinkedHashMap<>();

						discount.put("amount", amount);

						orderBody.put("discount", discount);

						orderBody.put("unit_of_measure", "QUANTITY");
						orderItems.add(orderBody);
					}

				}

			}
			if (orders.size() == totalOrderSuccess) {

				String paspRequestURI = paspAPIBaseURL + "/paypal/invoice/create";
				Gson gs = new Gson();

				LOGGER.info("Body post pasp ========:"
						+ gs.toJson(invoiceBodyRequest(orderItems, invoiceNumber, receiverEmailInvoice)));

				HttpResponse<String> response = Unirest.post(paspRequestURI).header("Content-Type", "application/json")
						.body(gs.toJson(invoiceBodyRequest(orderItems, invoiceNumber, receiverEmailInvoice)))
						.asString();

				LOGGER.info("httpResponse pasp paypal result========:" + response.getBody());

				if (response.getStatus() == HttpResponseStatus.ACCEPTED.code()
						|| response.getStatus() == HttpResponseStatus.OK.code()
						|| response.getStatus() == HttpResponseStatus.CREATED.code()) {

					Map responseMap = new JsonObject(response.getBody()).getMap();
					Map data = ParamUtil.getMapData(responseMap, "data");
					String saleId = ParamUtil.getString(data, "id");
					String paypalAccountName = ParamUtil.getString(data, "acount_name");
					// update sale id
					PaymentService.paypalSentInvoiceUpdate(invoiceNumber, saleId, paypalAccountName);

					String message = ParamUtil.getString(responseMap, "message");
					String status_code = ParamUtil.getString(responseMap, "status_code");

					data.remove("acount_name");
					Map resultDataMap = new LinkedHashMap<>();
					resultDataMap.put(AppParams.MESSAGE, message);
					resultDataMap.put(AppParams.STATUS_CODE, status_code);
					resultDataMap.put(AppParams.DATA, data);
					routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);

				} else {
//					for (Map order : orders) {
//						String orderId = ParamUtil.getString(order, AppParams.ID);
//						DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
//					}
					Map resultDataMap = new LinkedHashMap<>();
					resultDataMap.put(AppParams.STATE, ResourceStates.FAIL);

					Map reason = new LinkedHashMap<>();
					if (response.getStatus() == 500 || response.getBody() == null || response.getBody().isEmpty()) {
						reason.put("message",
								"There was an error occured while processing your payment. Please try again later!");
						reason.put("code", 500);
					} else {
						Map responseMap = new JsonObject(response.getBody()).getMap();
						String message = ParamUtil.getString(responseMap, AppParams.MESSAGE);
						reason.put("message", message);
						reason.put("code", response.getStatus());
					}
					resultDataMap.put(AppParams.REASON, reason);
					routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);
				}

			} else {
				PaymentService.paypalRemoveByInvoiceNumber(invoiceNumber);
				throw new BadRequestException(SystemError.INVALID_ORDER);
			}
			future.complete();

		} catch (SQLException e) {
			PaymentService.paypalRemoveByInvoiceNumber(invoiceNumber);
			throw new BadRequestException(SystemError.PAYMENT_CREATE_INVOICE_FAILED);
		}

	}

	private static Map invoiceBodyRequest(List<Map> orderItems, String invoicerNumber, String recipientEmail) {
		Map billPaypalBody = new LinkedHashMap<>();

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate localDate = LocalDate.now();

		Map detailMap = new LinkedHashMap<>();

		detailMap.put("invoice_number", invoicerNumber);
		detailMap.put("reference", "BurgerPrints");
		detailMap.put("invoice_date", dtf.format(localDate));
		detailMap.put("currency_code", "USD");
		billPaypalBody.put("detail", detailMap);

		Map infoInvoicerMap = new LinkedHashMap<>();
		Map givenNameMap = new LinkedHashMap<>();

		givenNameMap.put("given_name", invoicerName);
		infoInvoicerMap.put("name", givenNameMap);
		infoInvoicerMap.put("email_address", invoicerEmail);
		infoInvoicerMap.put("website", invoicerWebsite);
		infoInvoicerMap.put("tax_id", invoicerTaxId);
		infoInvoicerMap.put("logo_url", invoicerLogoUrl);

		billPaypalBody.put("invoicer", infoInvoicerMap);

		List<Map> primaryRecipientsMap = new ArrayList<>();
		Map billingInfo = new LinkedHashMap<>();
		Map emailAdressBillingMap = new LinkedHashMap<>();
		emailAdressBillingMap.put("email_address", recipientEmail);
		billingInfo.put("billing_info", emailAdressBillingMap);
		primaryRecipientsMap.add(billingInfo);
		billPaypalBody.put("primary_recipients", primaryRecipientsMap);
		// create order body

		billPaypalBody.put("items", orderItems);

		return billPaypalBody;
	}

	public static String getRandomString16() {
		return String.format("%016X", new Random().nextLong()).toLowerCase();
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipPaymentExecuteHandler.class.getName());

}
