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
import asia.leadsgen.psp.service.OrderToFinanceService;
import asia.leadsgen.psp.service.PaymentService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.HttpClientUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipPaymentExecuteV2Handler implements Handler<RoutingContext> {

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
		DropshipPaymentExecuteV2Handler.invoicerName = invoicerName;
	}

	public static String getInvoicerEmail() {
		return invoicerEmail;
	}

	public static void setInvoicerEmail(String invoicerEmail) {
		DropshipPaymentExecuteV2Handler.invoicerEmail = invoicerEmail;
	}

	public static String getInvoicerWebsite() {
		return invoicerWebsite;
	}

	public static void setInvoicerWebsite(String invoicerWebsite) {
		DropshipPaymentExecuteV2Handler.invoicerWebsite = invoicerWebsite;
	}

	public static String getInvoicerTaxId() {
		return invoicerTaxId;
	}

	public static void setInvoicerTaxId(String invoicerTaxId) {
		DropshipPaymentExecuteV2Handler.invoicerTaxId = invoicerTaxId;
	}

	public static String getInvoicerLogoUrl() {
		return invoicerLogoUrl;
	}

	public static void setInvoicerLogoUrl(String invoicerLogoUrl) {
		DropshipPaymentExecuteV2Handler.invoicerLogoUrl = invoicerLogoUrl;
	}

	public static String getPaspAPIBaseURL() {
		return paspAPIBaseURL;
	}

	public static void setPaspAPIBaseURL(String paspAPIBaseURL) {
		DropshipPaymentExecuteV2Handler.paspAPIBaseURL = paspAPIBaseURL;
	}

	private List<String> defaultPaymentMethods = Arrays.asList("card", "payout");

	private static HttpServiceConfig paymentConnectorServiceConfig;

	public static void setPaymentConnectorServiceConfig(HttpServiceConfig paymentConnectorServiceConfig) {
		DropshipPaymentExecuteV2Handler.paymentConnectorServiceConfig = paymentConnectorServiceConfig;
	}

	private static HttpServiceConfig aspConnectorServiceConfig;

	public static void setAspConnectorServiceConfig(HttpServiceConfig aspConnectorServiceConfig) {
		DropshipPaymentExecuteV2Handler.aspConnectorServiceConfig = aspConnectorServiceConfig;
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
				String card_id = ParamUtil.getString(requestBodyMap, AppParams.CARD_ID);
				if (StringUtils.isEmpty(card_id)) {
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

				boolean errorAddress200 = ParamUtil.getBoolean(requestBodyMap, "error_address_200");

				Map dbOrder = null;
				String dbOrderState = "";
				List<Map> addVerifyList = new ArrayList<>();
				for (Map order : orders) {
					Map addVerifyMap = new LinkedHashMap<>();
					dbOrder = DropshipOrderService.lookUpV2(ParamUtil.getString(order, AppParams.ID), false, false,
							false);
					dbOrderState = ParamUtil.getString(dbOrder, AppParams.STATE);
					if (!ResourceStates.QUEUED.equalsIgnoreCase(dbOrderState)
							&& !ResourceStates.CHARGE_FAIL.equalsIgnoreCase(dbOrderState)) {
						String source = ParamUtil.getString(dbOrder, AppParams.SOURCE);
						if (ResourceStates.DRAFT.equalsIgnoreCase(dbOrderState)
								&& ResourceSource.CUSTOM_SHOPIFY_APP.equalsIgnoreCase(source)) {
							if (!check_order_draft_to_payment(ParamUtil.getString(order, AppParams.ID), userId)) {
								LOGGER.info("order invalid");
								throw new BadRequestException(SystemError.INVALID_ORDER);
							}
						} else {
							throw new BadRequestException(SystemError.INVALID_ORDER);
						}
					}
					
					boolean is_valid_design = check_order_design(ParamUtil.getString(order, AppParams.ID), userId);
					if (!is_valid_design) {
						throw new BadRequestException(SystemError.INVALID_DESIGN);
					}

					Map shippingInfoMap = ParamUtil.getMapData(dbOrder, AppParams.SHIPPING);
					Map address = ParamUtil.getMapData(shippingInfoMap, AppParams.ADDRESS);
					String countryCode = ParamUtil.getString(address, AppParams.COUNTRY);
					LOGGER.info("countryCode:" + countryCode);
					if (countryCode == null || countryCode.isEmpty()) {
						throw new BadRequestException(SystemError.INVALID_ORDER);
					}
					Boolean addrVerified = ParamUtil.getBoolean(address, AppParams.ADDR_VERIFIED);
					if ("US".equalsIgnoreCase(countryCode)) {
						if (addrVerified == false) {
							Map verifyResult = checkShippingInfo(shippingInfoMap);
							if (ParamUtil.getBoolean(verifyResult, "success") == false
									&& verifyResult.containsKey("reason")) {
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

					if (errorAddress200) {
						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
						routingContext.put(AppParams.RESPONSE_DATA, addressError);
					} else {
						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
						routingContext.put(AppParams.RESPONSE_DATA, addressError);
					}
					future.complete();
				} else {
					lookupCardCustomerIdAndProcessPayment(future, routingContext, userId, orders, method, currency,
							card_id);
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
			String aspUserId, List<Map> orders, String method, String currency, String card_id) {
		String aspRequestApi = "/affiliate/card-private/" + card_id;
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

					processPayment(routingContext, orders, paymentType, currency);

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
					processPayoneerPayment(routingContext, orders, paymentType, currency);
				} catch (SQLException | ParseException e) {
					LOGGER.severe(e.getMessage());
					routingContext.fail(e);
				}

				break;

			case "balance":

				stripeCustomerId = ParamUtil.getString(responseBodyMap, AppParams.CUSTOMER_ID);

				try {
					processBalancePayment(routingContext, orders, paymentType, currency);
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
		double paymentAmount = 0.0d, orderAmount = 0.0d, orderFee = 0.0d;
		NumberFormat nbf = new DecimalFormat("#0.00");
		Map orderInfo = null;
		Map paymentInfoMap = null;

		List<String> paymentIds = new ArrayList<>();
		List<String> orderIds = new ArrayList<>();
		for (Map order : orders) {

			orderId = ParamUtil.getString(order, AppParams.ID);
			orderInfo = DropshipOrderService.lookUpV2(orderId, false, false, false);
			orderState = ParamUtil.getString(orderInfo, AppParams.STATE);
			orderAmount = ParamUtil.getDouble(orderInfo, AppParams.AMOUNT);
			orderAmount = Math.ceil(orderAmount * 100) / 100;
			orderFee = orderAmount * 0.03;
			orderFee = Math.ceil(orderFee * 100) / 100;
			paymentAmount += orderAmount + orderFee;
			paymentInfoMap = PaymentService.insert(AppParams.SALE, orderId, "stripe", nbf.format(orderAmount),
					currency);
			paymentIds.add(ParamUtil.getString(paymentInfoMap, AppParams.ID));
			orderIds.add(orderId);
		}
		if (paymentAmount > 0) {
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

			paymentConnectorRequest.handler(paymentConnectorResponse -> paymentResponseHandler(routingContext,
					paymentIds, orderIds, paymentConnectorResponse, method));
			paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
			paymentConnectorRequest.write(paymentRequestString);
			paymentConnectorRequest.end();
		} else {
			try {
				Map responseBodyMap = new LinkedHashMap<>();
				Map paymentResultMap = new LinkedHashMap();
				paymentResultMap.put(AppParams.STATE, ResourceStates.FAIL);
				responseBodyMap.put(AppParams.NAME, SystemError.BALANCE_IS_NOT_ENOUGH.getName());
				responseBodyMap.put(AppParams.MESSAGE, "Payment amount must be greater than zero");
				responseBodyMap.put(AppParams.DETAILS, SystemError.BALANCE_IS_NOT_ENOUGH.getDetails());
				responseBodyMap.put(AppParams.CODE, HttpResponseStatus.BAD_REQUEST.code());
				responseBodyMap.put(AppParams.METHOD, method);
				paymentResultMap.put(AppParams.REASON, responseBodyMap);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, paymentResultMap);
				routingContext.next();
			} catch (Exception e) {
				routingContext.fail(e);
			}
		}

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
			orderInfo = DropshipOrderService.lookUpV2(orderId, false, false, false);
			orderState = ParamUtil.getString(orderInfo, AppParams.STATE);
			orderAmount = ParamUtil.getDouble(orderInfo, AppParams.AMOUNT);
			orderAmount = Math.ceil(orderAmount * 100) / 100;
			paymentAmount += orderAmount;
			paymentInfoMap = PaymentService.insert(AppParams.SALE, orderId, "payoneer", nbf.format(orderAmount),
					currency);
			paymentIds.add(ParamUtil.getString(paymentInfoMap, AppParams.ID));
			LOGGER.info("paymentIds=" + paymentIds.toString());
			orderIds.add(orderId);
		}

		if (paymentAmount > 0) {
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

			paymentConnectorRequest.handler(paymentConnectorResponse -> paymentResponseHandler(routingContext,
					paymentIds, orderIds, paymentConnectorResponse, method));
			paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
			paymentConnectorRequest.write(paymentRequestString);
			paymentConnectorRequest.end();
		} else {
			try {
				Map responseBodyMap = new LinkedHashMap<>();
				Map paymentResultMap = new LinkedHashMap();
				paymentResultMap.put(AppParams.STATE, ResourceStates.FAIL);
				responseBodyMap.put(AppParams.NAME, SystemError.BALANCE_IS_NOT_ENOUGH.getName());
				responseBodyMap.put(AppParams.MESSAGE, "Payment amount must be greater than zero");
				responseBodyMap.put(AppParams.DETAILS, SystemError.BALANCE_IS_NOT_ENOUGH.getDetails());
				responseBodyMap.put(AppParams.CODE, HttpResponseStatus.BAD_REQUEST.code());
				responseBodyMap.put(AppParams.METHOD, method);
				paymentResultMap.put(AppParams.REASON, responseBodyMap);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, paymentResultMap);
				routingContext.next();
			} catch (Exception e) {
				routingContext.fail(e);
			}
		}

	}

	private void processBalancePayment(RoutingContext routingContext, List<Map> orders, String method, String currency)
			throws SQLException, ParseException {
		try {
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			String orderId = "", orderState = "";
			double paymentAmount = 0.0d, orderAmount = 0.0d;
			NumberFormat nbf = new DecimalFormat("#0.00");
			Map orderInfo = null;
			Map paymentInfoMap = null;

			List<Map> payments = new ArrayList<>();
			List<String> orderIds = new ArrayList<>();
			for (Map order : orders) {

				orderId = ParamUtil.getString(order, AppParams.ID);
				orderInfo = DropshipOrderService.lookUpV2(orderId, false, false, false);
				orderState = ParamUtil.getString(orderInfo, AppParams.STATE);
				orderAmount = ParamUtil.getDouble(orderInfo, AppParams.AMOUNT);
				orderAmount = Math.ceil(orderAmount * 100) / 100;
				paymentAmount += orderAmount;
				paymentInfoMap = PaymentService.insert(AppParams.SALE, orderId, "balance", nbf.format(orderAmount),
						currency);
				payments.add(paymentInfoMap);
				LOGGER.info("payments=" + payments.toString());
				orderIds.add(orderId);
			}
			if (paymentAmount > 0) {
				int responseCode = PaymentService.updateWalletBalanceByUserID(userId, paymentAmount);

				Map responseBodyMap = new LinkedHashMap<>();
				Map paymentResultMap = new LinkedHashMap();
				String paymentState = ResourceStates.FAIL;
				if (responseCode == HttpResponseStatus.OK.code()) {
					paymentState = ResourceStates.APPROVED;
					paymentResultMap.put(AppParams.STATE, ResourceStates.PURCHASED);
					responseBodyMap.put(AppParams.NAME, "Success");
					responseBodyMap.put(AppParams.MESSAGE, "Success");
					responseBodyMap.put(AppParams.DETAILS, "Success");
				} else {
					paymentResultMap.put(AppParams.STATE, ResourceStates.FAIL);
					responseBodyMap.put(AppParams.NAME, SystemError.BALANCE_IS_NOT_ENOUGH.getName());
					responseBodyMap.put(AppParams.MESSAGE, SystemError.BALANCE_IS_NOT_ENOUGH.getMessage());
					responseBodyMap.put(AppParams.DETAILS, SystemError.BALANCE_IS_NOT_ENOUGH.getDetails());
					responseBodyMap.put(AppParams.INFORMATION_LINK,
							SystemError.BALANCE_IS_NOT_ENOUGH.getInformationLink());
				}
				responseBodyMap.put(AppParams.CODE, responseCode);
				responseBodyMap.put(AppParams.METHOD, method);
				paymentResultMap.put(AppParams.REASON, responseBodyMap);

				String txnId = getRandomString16();
				for (Map payment : payments) {
					String paymentId = ParamUtil.getString(payment, AppParams.ID);
					String gatewayPaymentId = ParamUtil.getString(payment, AppParams.ID);
					String payerId = ParamUtil.getString(payment, AppParams.SALE_ID);
					String tokenId = ParamUtil.getString(payment, AppParams.TOKEN);
					String accountName = ParamUtil.getString(payment, AppParams.PAYMENT_NAME);
					PaymentService.update(paymentId, paymentState, gatewayPaymentId, payerId, txnId, tokenId,
							new JsonObject(paymentResultMap).encode(), "", "", accountName);
				}

				if (StringUtils.equalsIgnoreCase(paymentState, ResourceStates.APPROVED)) {
					for (String order_id : orderIds) {
						DropshipOrderService.updateExtraFeeAndState(order_id, ResourceStates.PLACED, 0, method);
						OrderToFinanceService.save(new OrderToFinanceObj(order_id, ResourceStates.CREATED, true));
					}
				}
				paymentResultMap.put(AppParams.METHOD, "balance");
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, paymentResultMap);
				routingContext.next();
			} else {
				try {
					Map responseBodyMap = new LinkedHashMap<>();
					Map paymentResultMap = new LinkedHashMap();
					paymentResultMap.put(AppParams.STATE, ResourceStates.FAIL);
					responseBodyMap.put(AppParams.NAME, SystemError.BALANCE_IS_NOT_ENOUGH.getName());
					responseBodyMap.put(AppParams.MESSAGE, "Payment amount must be greater than zero");
					responseBodyMap.put(AppParams.DETAILS, SystemError.BALANCE_IS_NOT_ENOUGH.getDetails());
					responseBodyMap.put(AppParams.CODE, HttpResponseStatus.BAD_REQUEST.code());
					responseBodyMap.put(AppParams.METHOD, method);
					paymentResultMap.put(AppParams.REASON, responseBodyMap);

					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, paymentResultMap);
					routingContext.next();
				} catch (Exception e) {
					routingContext.fail(e);
				}
			}

		} catch (Exception e) {
			routingContext.fail(e);
		}
	}

	private void paymentResponseHandler(RoutingContext routingContext, List<String> paymentIds, List<String> orderIds,
			HttpClientResponse paymentResponse, String method) {

		int responseCode = paymentResponse.statusCode();
		String responseMsg = paymentResponse.statusMessage();

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
						if(method.equalsIgnoreCase("payoneer")) {
							DropshipOrderService.updateExtraFeeAndState(orderId, ResourceStates.PLACED, 0, method);
						} else {
							DropshipOrderService.updateExtraFeeAndState(orderId, ResourceStates.PLACED, 1, method);
						}
						OrderToFinanceService.save(new OrderToFinanceObj(orderId, ResourceStates.CREATED, true));
					}
				}
				paymentResultMap.put(AppParams.METHOD, method);
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

			Double totalOrderFee = 0d;
			String currency = "";
			for (Map order : orders) {

				String orderId = ParamUtil.getString(order, AppParams.ID);
				if (StringUtils.isEmpty(receiverEmailInvoice)) {
//					DropshipOrderService.updateStateV2(orderId, ResourceStates.CHARGE_FAIL);

				} else {

					Map orderInfo = DropshipOrderService.lookUpV2(orderId, false, false, false);
					// orderState = ParamUtil.getString(orderInfo, AppParams.STATE);
					Double orderAmount = ParamUtil.getDouble(orderInfo, AppParams.AMOUNT);
					orderAmount = Math.ceil(orderAmount * 100) / 100;
					totalOrderFee += orderAmount * 0.03;
					totalOrderFee = Math.ceil(totalOrderFee * 100) / 100;
					currency = ParamUtil.getString(orderInfo, AppParams.CURRENCY);

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

				Map orderBody = new LinkedHashMap<>();

				orderBody.put("order_id", "Process fee");
				orderBody.put("name", "Process fee");
				orderBody.put("description", "");
				orderBody.put("quantity", 1);// quy ước là 1 order.

				Map unit_amount = new LinkedHashMap<>();
				unit_amount.put("currency_code", currency);
				unit_amount.put("value", totalOrderFee);
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
					resultDataMap.put(AppParams.METHOD, method);
					routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);

				} else {
//					for (Map order : orders) {
//						String orderId = ParamUtil.getString(order, AppParams.ID);
//						DropshipOrderService.updateStateV2(orderId, ResourceStates.CHARGE_FAIL);
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
					resultDataMap.put(AppParams.METHOD, method);
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

	boolean check_order_draft_to_payment(String order_id, String user_id) {
		boolean is_payment = true;
		try {
			LOGGER.info("check order draft to payment");
			if (StringUtils.isEmpty(user_id)) {
				LOGGER.info("user id null");
				is_payment = false;
				return is_payment;
			}

			Map dbOrderInfoMap = DropshipOrderService.lookUpV2(order_id, true, false, false);

			String storeId = ParamUtil.getString(dbOrderInfoMap, AppParams.STORE_ID);
			if (storeId == null || storeId.isEmpty()) {
				LOGGER.info("store id null");
				is_payment = false;
				return is_payment;
			}
			String userIdOfdbOrderInfoMap = ParamUtil.getString(dbOrderInfoMap, AppParams.USER_ID);

			if (!user_id.equals(userIdOfdbOrderInfoMap)) {
				LOGGER.info("not userid create order");
				is_payment = false;
				return is_payment;
			}

			String orderState = ParamUtil.getString(dbOrderInfoMap, AppParams.STATE);
			LOGGER.info("OrderId= " + order_id + " - Order State= " + orderState);
			if (!ResourceStates.DRAFT.equalsIgnoreCase(orderState)) {
				LOGGER.info("state != draft");
				is_payment = false;
				return is_payment;
			}

			Map rqShipping = ParamUtil.getMapData(dbOrderInfoMap, AppParams.SHIPPING);
			if (rqShipping == null || rqShipping.isEmpty()) {
				LOGGER.info("shipping is null");
				is_payment = false;
				return is_payment;
			}

			String amount = ParamUtil.getString(dbOrderInfoMap, AppParams.AMOUNT);
			if (amount == null || amount.isEmpty()) {
				LOGGER.info("amount is null");
				is_payment = false;
				return is_payment;
			}
			List<Map> requestOrderItemList = ParamUtil.getListData(dbOrderInfoMap, AppParams.ITEMS);
			if (requestOrderItemList.size() > 0) {
				for (Map requestItem : requestOrderItemList) {
					LOGGER.info("requestItem: " + requestItem.toString());
					String baseId = ParamUtil.getString(requestItem, AppParams.BASE_ID);
					if (baseId == null || baseId.isEmpty()) {
						LOGGER.info("base item is null");
						is_payment = false;
						break;
					}
					String sizeId = ParamUtil.getString(requestItem, AppParams.SIZE_ID);
					if (sizeId == null || sizeId.isEmpty()) {
						LOGGER.info("sizeId item is null");
						is_payment = false;
						break;
					}
					Map campaign = ParamUtil.getMapData(requestItem, AppParams.CAMPAIGN);
					String campaignId = ParamUtil.getString(campaign, AppParams.ID);
					if (campaignId == null || campaignId.isEmpty()) {
						LOGGER.info("campaignId item is null");
						is_payment = false;
						break;
					}
					String color_name = ParamUtil.getString(requestItem, AppParams.COLOR_NAME);
					if (color_name == null || color_name.isEmpty()) {
						LOGGER.info("color_name item is null");
						is_payment = false;
						break;
					}

					int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
					if (quantity <= 0) {
						LOGGER.info("quantity item is null");
						is_payment = false;
						break;
					}

					Map designMap = ParamUtil.getMapData(requestItem, AppParams.DESIGNS);
					String design_front_url = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL);
					String design_back_url = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL);
					if ((design_front_url == null || design_front_url.isEmpty())
							&& (design_back_url == null || design_back_url.isEmpty())) {
						LOGGER.info("design item is null");
						is_payment = false;
						break;
					}
					is_payment = true;
				}
			}
		} catch (Exception e) {
			is_payment = false;
		}
		return is_payment;
	}
	
	private boolean check_order_design(String order_id, String userId) throws SQLException, ParseException {
		boolean is_valid_design = true;
		Map dbOrderInfoMap = DropshipOrderService.lookUpV2(order_id, true, false, false);
		String source = ParamUtil.getString(dbOrderInfoMap, AppParams.SOURCE);
		if (!source.contains("camp.")) {
			List<Map> requestOrderItemList = ParamUtil.getListData(dbOrderInfoMap, AppParams.ITEMS);
			if (requestOrderItemList.size() > 0) {
				for (Map requestItem : requestOrderItemList) {

					Map designMap = ParamUtil.getMapData(requestItem, AppParams.DESIGNS);
					LOGGER.info("check_order_design: designMap=" + designMap);
					String design_front_url = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL);
					String design_back_url = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL);
					String mock_front_url = ParamUtil.getString(designMap, AppParams.MOCKUP_FRONT_URL);
					String mock_back_url = ParamUtil.getString(designMap, AppParams.MOCKUP_BACK_URL);

					boolean check_design = false;

					if ((StringUtils.isNotEmpty(design_front_url) && StringUtils.isNotEmpty(mock_front_url))
							|| (StringUtils.isNotEmpty(design_back_url) && StringUtils.isNotEmpty(mock_back_url))) {
						check_design = true;
					}

					if (!check_design) {
						LOGGER.info("design and mockup is not valid");
						is_valid_design = false;
						break;
					}
				}
			}
		}
		return is_valid_design;
	}



	public static String getRandomString16() {
		return String.format("%016X", new Random().nextLong()).toLowerCase();
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipPaymentExecuteV2Handler.class.getName());

}
