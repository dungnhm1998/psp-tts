package asia.leadsgen.psp.server.handler.dropship.payment;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.EmailObj;
import asia.leadsgen.psp.obj.TopupHistoryObj;
import asia.leadsgen.psp.service_fulfill.DropShipStoreCampService;
import asia.leadsgen.psp.service.EmailMarketingService;
import asia.leadsgen.psp.service.PaymentService;
import asia.leadsgen.psp.service_fulfill.TopupHistoryService;
import asia.leadsgen.psp.util.AppConstants;
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

public class DropshipTopupExecuteHandler implements Handler<RoutingContext> {

	private String stripeCustomerId;
	private static String invoicerName;
	private static String invoicerEmail;
	private static String invoicerWebsite;
	private static String invoicerTaxId;
	private static String invoicerLogoUrl;
	private static String paspAPIBaseURL;
	private static String adminEmail;

	public static String getInvoicerName() {
		return invoicerName;
	}

	public static void setInvoicerName(String invoicerName) {
		DropshipTopupExecuteHandler.invoicerName = invoicerName;
	}

	public static String getInvoicerEmail() {
		return invoicerEmail;
	}

	public static void setInvoicerEmail(String invoicerEmail) {
		DropshipTopupExecuteHandler.invoicerEmail = invoicerEmail;
	}

	public static String getInvoicerWebsite() {
		return invoicerWebsite;
	}

	public static void setInvoicerWebsite(String invoicerWebsite) {
		DropshipTopupExecuteHandler.invoicerWebsite = invoicerWebsite;
	}

	public static String getInvoicerTaxId() {
		return invoicerTaxId;
	}

	public static void setInvoicerTaxId(String invoicerTaxId) {
		DropshipTopupExecuteHandler.invoicerTaxId = invoicerTaxId;
	}

	public static String getInvoicerLogoUrl() {
		return invoicerLogoUrl;
	}

	public static void setInvoicerLogoUrl(String invoicerLogoUrl) {
		DropshipTopupExecuteHandler.invoicerLogoUrl = invoicerLogoUrl;
	}

	public static String getPaspAPIBaseURL() {
		return paspAPIBaseURL;
	}

	public static void setPaspAPIBaseURL(String paspAPIBaseURL) {
		DropshipTopupExecuteHandler.paspAPIBaseURL = paspAPIBaseURL;
	}
	
	public static String getAdminEmail() {
		return adminEmail;
	}

	public static void setAdminEmail(String adminEmail) {
		DropshipTopupExecuteHandler.adminEmail = adminEmail;
	}

	private List<String> defaultPaymentMethods = Arrays.asList("payoneer", "paypal", "card");

	private static HttpServiceConfig paymentConnectorServiceConfig;

	public static void setPaymentConnectorServiceConfig(HttpServiceConfig paymentConnectorServiceConfig) {
		DropshipTopupExecuteHandler.paymentConnectorServiceConfig = paymentConnectorServiceConfig;
	}

	private static HttpServiceConfig aspConnectorServiceConfig;

	public static void setAspConnectorServiceConfig(HttpServiceConfig aspConnectorServiceConfig) {
		DropshipTopupExecuteHandler.aspConnectorServiceConfig = aspConnectorServiceConfig;
	}

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		Map requestBodyMap = routingContext.getBodyAsJson().getMap();
		LOGGER.info("requestBodyMap=" + requestBodyMap.toString());

		routingContext.vertx().executeBlocking(future -> {

			try {

				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				String param_type = routingContext.request().getParam(AppParams.TYPE);
				LOGGER.info("param_type=" + param_type);
				if(StringUtils.isNotEmpty(param_type)) {
					userId = ParamUtil.getString(requestBodyMap, AppParams.USER_ID);
				}
				LOGGER.info("userId=" + userId);
				if (StringUtils.isEmpty(userId)) {
					throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
				}
				String type = ParamUtil.getString(requestBodyMap, AppParams.TYPE);
				String transaction_id = ParamUtil.getString(requestBodyMap, AppParams.TRANSACTION_ID);
				if (StringUtils.isNotEmpty(type)) {
					String method = ParamUtil.getString(requestBodyMap, AppParams.METHOD);
					if (StringUtils.isEmpty(method) || !defaultPaymentMethods.contains(method)) {
						throw new BadRequestException(SystemError.INVALID_PAYMENT_METHOD);
					}

					String currency = ParamUtil.getString(requestBodyMap, AppParams.CURRENCY);
//					if (StringUtils.isEmpty(currency)) {
//						throw new BadRequestException(SystemError.INVALID_PAYMENT_CURRENCY);
//					}
					String note = ParamUtil.getString(requestBodyMap, AppParams.NOTE);
//					if (StringUtils.isEmpty(note)) {
//						throw new BadRequestException(SystemError.INVALID_PAYMENT_METHOD);
//					}
					Double amount = 0.0d;
					try {
						amount = ParamUtil.getDouble(requestBodyMap, AppParams.AMOUNT);
					} catch (Exception e) {
					}
					if (amount <= 0) {
						throw new BadRequestException(SystemError.INVALID_PAYMENT_METHOD);
					}
					if (type.equalsIgnoreCase("auto")) {
						String card_id = ParamUtil.getString(requestBodyMap, AppParams.CARD_ID);
						if (StringUtils.isEmpty(card_id)) {
							throw new BadRequestException(SystemError.INVALID_PAYMENT_METHOD);
						}
						lookupCardCustomerIdAndProcessPayment(future, routingContext, userId, amount, method, currency,
								note, card_id);
					} else {
							boolean check_transaction_id_exists = TopupHistoryService.checkTransactionExist(transaction_id);
							if(StringUtils.isNotEmpty(param_type) || (StringUtils.isEmpty(param_type) && !check_transaction_id_exists)) {
								LOGGER.info("transaction is not exists");
								TopupHistoryObj obj = TopupHistoryService.insertTopupHistory(type, method, transaction_id,
										"in_review", note, "", userId, "", amount, 0.0);

								notifyEmail(obj);
								
								LOGGER.info("TopupHistoryObj = " + obj.toString());
								routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
								routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
							} else {
								LOGGER.info("transaction is exists");
								Map data = new LinkedHashMap<>();
								data.put("state", "fail");
								Map reason = new LinkedHashMap<>();
								reason.put("message", "The transaction ID already redeemed");
								reason.put("code", "403");
								
								data.put("reason", reason);
								
								routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
								routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
								routingContext.put(AppParams.RESPONSE_DATA, data);
								
							}
						future.complete();
					}

				} else {

					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
					future.complete();
				}

			} catch (Exception e) {
				e.printStackTrace();
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

	public static void notifyEmail(TopupHistoryObj obj) throws SQLException {
		
		String domain = "";
		String userEmail = "";
		Map userInfoMap = DropShipStoreCampService.getUserInfo(obj.getUserId());
		userEmail = ParamUtil.getString(userInfoMap, AppParams.EMAIL);
		String subject = " [Topup Review] Topup by seller " + userEmail;
		String content = obj.getTransactionID() + ": " + obj.getAmount();
		
		EmailObj emailObj = new EmailObj(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY, userEmail, subject, content,
				"pending", "", "image");
		emailObj.setId(null);
		emailObj.setType(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY);
		emailObj.setState(ResourceStates.PENDING);
		emailObj.setReceiver(adminEmail);
		emailObj.setSubject(subject);
		emailObj.setContent(content);
		emailObj.setDomain(domain);
		emailObj.setCreate(new Date());
		EmailMarketingService.insert(emailObj);
		LOGGER.info("emailObj=" + emailObj.toString());
	}

	private void lookupCardCustomerIdAndProcessPayment(Future<Object> future, RoutingContext routingContext,
			String aspUserId, Double amount, String method, String currency, String note, String card_id) {
		String aspRequestApi = "/affiliate/card-private/" + card_id;
		String aspRequestString = "";
		LOGGER.info("aspConnectorServiceConfig= " + aspConnectorServiceConfig + aspRequestApi);
		HttpClientRequest aspConnectorRequest = HttpClientUtil.createHttpRequest(aspConnectorServiceConfig,
				aspRequestApi, HttpMethod.GET, new LinkedHashMap<>(), aspRequestString);
		aspConnectorRequest.handler(aspResponse -> aspResponseHandler(future, routingContext, aspResponse, method,
				amount, currency, aspUserId, note));
		aspConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
		aspConnectorRequest.write(aspRequestString);
		aspConnectorRequest.end();
	}

	private void aspResponseHandler(Future<Object> future, RoutingContext routingContext,
			HttpClientResponse aspResponse, String method, Double amount, String currency, String user_id,
			String note) {

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
					processPayment(routingContext, method, amount, currency, user_id, note);

				} catch (SQLException | ParseException e) {
					LOGGER.severe(e.getMessage());
					routingContext.fail(e);
				}

				break;

			case "paypal":

				String receiverEmailInvoice = ParamUtil.getString(responseBodyMap, AppParams.EMAIL);

				try {
					processOrderPaymentViaPayPal(future, routingContext, amount, currency, user_id, note, receiverEmailInvoice);
					;
				} catch (SQLException | UnirestException | ParseException e) {
					LOGGER.severe(e.getMessage());
					routingContext.fail(e);
				}

				break;

			case "payoneer":

				stripeCustomerId = ParamUtil.getString(responseBodyMap, AppParams.CUSTOMER_ID);

				try {
					processPayoneerPayment(routingContext, method, amount, currency, user_id, note);
					;
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

	private void processPayment(RoutingContext routingContext, String method, Double amount, String currency,
			String user_id, String note)

			throws SQLException, ParseException {

		NumberFormat nbf = new DecimalFormat("#0.00");

		String orderId = "Topup by " + user_id;
		Double fee = Math.ceil(((amount * 0.03) * 100)) / 100;
		Double total_amount = amount + fee;
		Map paymentInfoMap = PaymentService.insert(AppParams.SALE, orderId, "stripe", nbf.format(total_amount), currency);
		String paymentId = ParamUtil.getString(paymentInfoMap, AppParams.ID);
		Map paymentRequestBody = new LinkedHashMap<>();

		paymentRequestBody.put(AppParams.REFERENCE, "");
		paymentRequestBody.put(AppParams.AMOUNT, nbf.format(total_amount));
		paymentRequestBody.put(AppParams.CURRENCY, currency);
		paymentRequestBody.put(AppParams.METHOD, "stripe");
		paymentRequestBody.put(AppParams.CUSTOMER_ID, stripeCustomerId);
		paymentRequestBody.put(AppParams.ORDER_ID, orderId);

		String paymentConnectorRequestURI = "/charge/customer";
		String paymentRequestString = new JsonObject(paymentRequestBody).encode();
		HttpClientRequest paymentConnectorRequest = HttpClientUtil.createHttpRequest(paymentConnectorServiceConfig,
				paymentConnectorRequestURI, HttpMethod.POST, new LinkedHashMap<>(), paymentRequestString);

		paymentConnectorRequest.handler(paymentConnectorResponse -> paymentResponseHandler(routingContext, paymentId,
				amount, fee, currency, user_id, note, paymentConnectorResponse));
		paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
		paymentConnectorRequest.write(paymentRequestString);
		paymentConnectorRequest.end();

	}

	private void processPayoneerPayment(RoutingContext routingContext, String method, Double amount, String currency,
			String user_id, String note)

			throws SQLException, ParseException {

		NumberFormat nbf = new DecimalFormat("#0.00");
		String orderId = "Topup by " + user_id;
		Map paymentInfoMap = PaymentService.insert(AppParams.SALE, orderId, "payoneer", nbf.format(amount), currency);
		String paymentId = ParamUtil.getString(paymentInfoMap, AppParams.ID);
		LOGGER.info("paymentIds=" + paymentId.toString());

		Map paymentRequestBody = new LinkedHashMap<>();
		String reference = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
		paymentRequestBody.put(AppParams.REFERENCE, reference);
		paymentRequestBody.put(AppParams.AMOUNT, nbf.format(amount));
		paymentRequestBody.put(AppParams.CURRENCY, currency);
		paymentRequestBody.put(AppParams.METHOD, "payoneer");
		paymentRequestBody.put(AppParams.CUSTOMER_ID, stripeCustomerId);
		paymentRequestBody.put(AppParams.ORDER_ID, orderId);

		String paymentConnectorRequestURI = "/charge/payoneer";
		String paymentRequestString = new JsonObject(paymentRequestBody).encode();
		HttpClientRequest paymentConnectorRequest = HttpClientUtil.createHttpRequest(paymentConnectorServiceConfig,
				paymentConnectorRequestURI, HttpMethod.POST, new LinkedHashMap<>(), paymentRequestString);

		paymentConnectorRequest.handler(paymentConnectorResponse -> paymentResponseHandler(routingContext, paymentId,
				amount, 0.0, currency, user_id, note, paymentConnectorResponse));
		paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
		paymentConnectorRequest.write(paymentRequestString);
		paymentConnectorRequest.end();

	}

	private void paymentResponseHandler(RoutingContext routingContext, String paymentId, Double amount, Double extra_fee, String currency,
			String user_id, String note, HttpClientResponse paymentResponse) {

		int responseCode = paymentResponse.statusCode();
		paymentResponse.bodyHandler(responseBody -> {
			try {

				JsonObject responseBodyJson = (responseCode == HttpResponseStatus.CREATED.code())
						? new JsonObject(responseBody.toString("UTF-8"))
						: new JsonObject();
				Map responseBodyMap = responseBodyJson.getMap();
				LOGGER.info("paymentResponseHandler()- responseBodyMap= " + responseBodyMap.toString());
				String paymentState = (responseCode == HttpResponseStatus.CREATED.code())
						? ParamUtil.getString(responseBodyMap, AppParams.STATE)
						: ResourceStates.FAIL;
				String gatewayPaymentId = ParamUtil.getString(responseBodyMap, AppParams.ID);
				String payerId = ParamUtil.getString(responseBodyMap, AppParams.CUSTOMER_ID);
				String method = ParamUtil.getString(responseBodyMap, AppParams.METHOD);
				String txnId = ParamUtil.getString(responseBodyMap, AppParams.PAY_ID);
				String tokenId = ParamUtil.getString(responseBodyMap, AppParams.REFERENCE);
				String accountName = ParamUtil.getString(responseBodyMap, "account_name");
				LOGGER.info("tokenId=" + tokenId);
				Map paymentResultMap = new LinkedHashMap();
				String topup_state = "failed";
				if (responseCode == HttpResponseStatus.CREATED.code()) {
					if (paymentState.equalsIgnoreCase(ResourceStates.FAIL)) {
						paymentResultMap.put(AppParams.REASON, ParamUtil.getMapData(responseBodyMap, AppParams.REASON));
						paymentResultMap.put(AppParams.STATE, ResourceStates.FAIL);
					} else {
						paymentResultMap.put(AppParams.STATE, ResourceStates.PURCHASED);
						topup_state = "approved";
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

				PaymentService.update(paymentId, paymentState, gatewayPaymentId, payerId, txnId, tokenId,
						new JsonObject(paymentResultMap).encode(), "", "", accountName);

				TopupHistoryObj obj = TopupHistoryService.insertTopupHistory("auto", method, "", topup_state, note, "",
						user_id, "", amount, extra_fee);
				LOGGER.info("obj= " + obj.toString());
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

	private void processOrderPaymentViaPayPal(Future<Object> future, RoutingContext routingContext, Double amount,
			String currency, String user_id, String note, String receiverEmailInvoice) throws SQLException, UnirestException, ParseException {

		String invoiceNumber = "topup_" + getRandomString16();
		String method = AppParams.PAYPAL;
		String state = ResourceStates.CREATED;
		String reference = "";
		String order_id = "Topup by " + user_id;
		Double fee = Math.ceil(((amount * 0.03) * 100)) / 100;
		try {
			List<Map> orderItems = new ArrayList<>();

			if (PaymentService.payPalCreateInvoice(order_id, state, reference, String.valueOf(amount + fee), "USD",
					"", method, invoiceNumber)) {

				Map orderBody = new LinkedHashMap<>();
				orderBody.put("order_id", order_id);
				orderBody.put("name", order_id);
				orderBody.put("description", note);
				orderBody.put("quantity", 1);// quy ước là 1 order.

				Map unit_amount = new LinkedHashMap<>();
				unit_amount.put("currency_code", currency);
				unit_amount.put("value", amount.toString());
				orderBody.put("unit_amount", unit_amount);

				Map tax = new LinkedHashMap<>();
				tax.put("name", "Sales Tax");
				tax.put("percent", "0");
				orderBody.put("tax", tax);

				Map amount_obj = new LinkedHashMap<>();
				amount_obj.put("currency_code", currency);
				amount_obj.put("value", "0");

				Map discount = new LinkedHashMap<>();
				discount.put("amount", amount_obj);
				orderBody.put("discount", discount);

				orderBody.put("unit_of_measure", "QUANTITY");
				orderItems.add(orderBody);
			}
			
			Map orderBody = new LinkedHashMap<>();
			orderBody.put("order_id", "Process fee");
			orderBody.put("name", "Process fee");
			orderBody.put("description", note);
			orderBody.put("quantity", 1);// quy ước là 1 order.

			Map unit_amount = new LinkedHashMap<>();
			unit_amount.put("currency_code", currency);
			unit_amount.put("value", fee.toString());
			orderBody.put("unit_amount", unit_amount);

			Map tax = new LinkedHashMap<>();
			tax.put("name", "Sales Tax");
			tax.put("percent", "0");
			orderBody.put("tax", tax);

			Map amount_obj = new LinkedHashMap<>();
			amount_obj.put("currency_code", currency);
			amount_obj.put("value", "0");

			Map discount = new LinkedHashMap<>();
			discount.put("amount", amount_obj);
			orderBody.put("discount", discount);

			orderBody.put("unit_of_measure", "QUANTITY");
			orderItems.add(orderBody);
			

			String paspRequestURI = paspAPIBaseURL + "/paypal/invoice/create";
			Gson gs = new Gson();

			LOGGER.info("Body post pasp ========:" + gs.toJson(invoiceBodyRequest(orderItems, invoiceNumber, receiverEmailInvoice)));

			HttpResponse<String> response = Unirest.post(paspRequestURI).header("Content-Type", "application/json")
					.body(gs.toJson(invoiceBodyRequest(orderItems, invoiceNumber, receiverEmailInvoice))).asString();

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
				TopupHistoryObj obj = TopupHistoryService.insertTopupHistory("auto", method, "", "pending", note,
						invoiceNumber, user_id, "", amount, fee);
				LOGGER.info("obj= " + obj.toString());

				data.remove("acount_name");
				Map resultDataMap = new LinkedHashMap<>();
				resultDataMap.put(AppParams.MESSAGE, message);
				resultDataMap.put(AppParams.STATUS_CODE, status_code);
				resultDataMap.put(AppParams.DATA, data);
				routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);

			} else {
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

				TopupHistoryObj obj = TopupHistoryService.insertTopupHistory("auto", method, "", "failed", note,
						invoiceNumber, user_id, "", amount, fee);
				LOGGER.info("obj= " + obj.toString());
				resultDataMap.put(AppParams.REASON, reason);
				routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);
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
		detailMap.put("terms_and_conditions", "No refund or cancellation after payment");
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

	private static final Logger LOGGER = Logger.getLogger(DropshipTopupExecuteHandler.class.getName());

}
