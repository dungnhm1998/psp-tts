package asia.leadsgen.psp.thread;

import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.email.MailUtil;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.OrderToFinanceObj;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service.OrderToFinanceService;
import asia.leadsgen.psp.service.PaymentService;
import asia.leadsgen.psp.service.UserService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;

public class DropshipChargeOrderThread extends Thread {

	private String orderId;
	private DropshipOrderObj orderObj;
	private String userId;
	private static String aspAPIBaseURL;
	private static String paspAPIBaseURL;

	private static String invoicerName;
	private static String invoicerEmail;
	private static String invoicerWebsite;
	private static String invoicerTaxId;
	private static String invoicerLogoUrl;

	public DropshipOrderObj getOrderObj() {
		return orderObj;
	}

	public void setOrderObj(DropshipOrderObj orderObj) {
		this.orderObj = orderObj;
	}

	public static String getInvoicerName() {
		return invoicerName;
	}

	public static String getInvoicerEmail() {
		return invoicerEmail;
	}

	public static String getInvoicerWebsite() {
		return invoicerWebsite;
	}

	public static String getInvoicerTaxId() {
		return invoicerTaxId;
	}

	public static String getInvoicerLogoUrl() {
		return invoicerLogoUrl;
	}

	public static void setInvoicerName(String invoicerName) {
		DropshipChargeOrderThread.invoicerName = invoicerName;
	}

	public static void setInvoicerEmail(String invoicerEmail) {
		DropshipChargeOrderThread.invoicerEmail = invoicerEmail;
	}

	public static void setInvoicerWebsite(String invoicerWebsite) {
		DropshipChargeOrderThread.invoicerWebsite = invoicerWebsite;
	}

	public static void setInvoicerTaxId(String invoicerTaxId) {
		DropshipChargeOrderThread.invoicerTaxId = invoicerTaxId;
	}

	public static void setInvoicerLogoUrl(String invoicerLogoUrl) {
		DropshipChargeOrderThread.invoicerLogoUrl = invoicerLogoUrl;
	}

	@Override
	public void run() {
		if (StringUtils.isNotEmpty(userId)) {
			try {

				Map user = UserService.get(userId);

				LOGGER.info("Call Thread========" + user);

//				String aspUserId = ParamUtil.getString(user, AppParams.ASP_ID);

				String username = ParamUtil.getString(user, AppParams.NAME);
				String email = ParamUtil.getString(user, AppParams.EMAIL);

				String aspRequestURL = aspAPIBaseURL + "/affiliate/" + userId + "/card-info";

				HttpResponse<String> response = Unirest.get(aspRequestURL).asString();
				int responseCode = response.getStatus();
				if (responseCode != HttpResponseStatus.OK.code()) {
					DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
					MailUtil.shopifyChargeFailNotify(orderId, username, email);
				} else {

					JsonObject cardInfo = new JsonObject(response.getBody());
					LOGGER.info("[Thread] cardInfo ========" + cardInfo);

					String paymentType = cardInfo.getString(AppParams.TYPE);
					String stripeCustomerId = "";
					switch (paymentType) {
					case "card":
						stripeCustomerId = cardInfo.getString(AppParams.CUSTOMER_ID);
						if (StringUtils.isEmpty(stripeCustomerId)) {
							DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
						} else {
							Map orderInfo = DropshipOrderService.lookUp(orderId, false, false, false);
							String amount = ParamUtil.getString(orderInfo, AppParams.AMOUNT);
							String currency = ParamUtil.getString(orderInfo, AppParams.CURRENCY);

							Map payment = PaymentService.insert(AppParams.SALE, orderId, "stripe", amount, currency);
							String paymentId = ParamUtil.getString(payment, AppParams.ID);
							processOrderPaymentViaStripe(orderId, username, email, paymentId, amount, currency,
									stripeCustomerId);

						}
						break;
					case "paypal":
						String receiverEmailInvoice = cardInfo.getString(AppParams.EMAIL);

						if (StringUtils.isEmpty(receiverEmailInvoice)) {
							DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
						} else {

							Double amount = orderObj.getOrderAmount();
							String currency = orderObj.getOrderCurrency();
							processOrderPaymentViaPayPal(orderId, orderObj, receiverEmailInvoice, amount, currency);

						}
						break;

					case "payoneer":
						stripeCustomerId = cardInfo.getString(AppParams.CUSTOMER_ID);
						if (StringUtils.isEmpty(stripeCustomerId)) {
							DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
						} else {
							Map orderInfo = DropshipOrderService.lookUp(orderId, false, false, false);
							String amount = ParamUtil.getString(orderInfo, AppParams.AMOUNT);
							String currency = ParamUtil.getString(orderInfo, AppParams.CURRENCY);

							Map payment = PaymentService.insert(AppParams.SALE, orderId, "payoneer", amount, currency);
							String paymentId = ParamUtil.getString(payment, AppParams.ID);
							processOrderPaymentViaPayoneer(orderId, username, email, paymentId, amount, currency,
									stripeCustomerId);

						}
						break;
					default:
						break;
					}

				}

			} catch (Exception e) {
				LOGGER.severe(e.getMessage());
			}

		}
	}

	private void processOrderPaymentViaStripe(String orderId, String username, String email, String paymentId,
			String amount, String currency, String stripeCustomerId) {

		JsonObject paymentRequestBody = new JsonObject();
		paymentRequestBody.put(AppParams.REFERENCE, paymentId);
		paymentRequestBody.put(AppParams.AMOUNT, amount);
		paymentRequestBody.put(AppParams.CURRENCY, currency);
		paymentRequestBody.put(AppParams.METHOD, "stripe");
		paymentRequestBody.put(AppParams.CUSTOMER_ID, stripeCustomerId);
		paymentRequestBody.put(AppParams.ORDER_ID, orderId);
		String paspRequestURL = paspAPIBaseURL + "/charge/customer";
		try {

			HttpResponse<String> paspResponse = Unirest.post(paspRequestURL).header("Content-Type", "application/json")
					.body(paymentRequestBody.encode()).asString();

			Map responseBodyMap = new JsonObject(paspResponse.getBody().toString()).getMap();

			String paymentState = (paspResponse.getStatus() == HttpResponseStatus.CREATED.code())
					? ParamUtil.getString(responseBodyMap, AppParams.STATE)
					: ResourceStates.FAIL;

			String gatewayPaymentId = ParamUtil.getString(responseBodyMap, AppParams.ID);
			String payerId = ParamUtil.getString(responseBodyMap, AppParams.PAYER_ID);
			String txnId = ParamUtil.getString(responseBodyMap, AppParams.PAY_ID);
			String tokenId = ParamUtil.getString(responseBodyMap, AppParams.TOKEN_ID);
			String accountName = ParamUtil.getString(responseBodyMap, "account_name");

			Map paymentResultMap = new LinkedHashMap();

			if (paspResponse.getStatus() == HttpResponseStatus.CREATED.code()) {
				if (paymentState.equalsIgnoreCase(ResourceStates.FAIL)) {
					paymentResultMap.put(AppParams.REASON, ParamUtil.getMapData(responseBodyMap, AppParams.REASON));
				} else {
					paymentResultMap.put(AppParams.STATE, ResourceStates.PURCHASED);
				}
			} else {
				paymentResultMap.put(AppParams.REASON, responseBodyMap);
			}

			PaymentService.update(paymentId, paymentState, gatewayPaymentId, payerId, txnId, tokenId,
					new JsonObject(paymentResultMap).encode(), "", "", accountName);

			if (StringUtils.equalsIgnoreCase(paymentState, ResourceStates.APPROVED)) {
				DropshipOrderService.updateState(orderId, ResourceStates.PLACED);
				OrderToFinanceService.save(new OrderToFinanceObj(orderId, ResourceStates.CREATED, true));
			} else {
				DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
			}

		} catch (Exception e) {
			LOGGER.severe(e.getMessage());
		}

	}

	private void processOrderPaymentViaPayoneer(String orderId, String username, String email, String paymentId,
			String amount, String currency, String payoneerCustomerId) {

		JsonObject paymentRequestBody = new JsonObject();
		paymentRequestBody.put(AppParams.REFERENCE, paymentId);
		paymentRequestBody.put(AppParams.AMOUNT, amount);
		paymentRequestBody.put(AppParams.CURRENCY, currency);
		paymentRequestBody.put(AppParams.METHOD, "payoneer");
		paymentRequestBody.put(AppParams.CUSTOMER_ID, payoneerCustomerId);
		paymentRequestBody.put(AppParams.ORDER_ID, orderId);
		String paspRequestURL = paspAPIBaseURL + "/charge/payoneer";
		try {

			HttpResponse<String> paspResponse = Unirest.post(paspRequestURL).header("Content-Type", "application/json")
					.body(paymentRequestBody.encode()).asString();

			Map responseBodyMap = new JsonObject(paspResponse.getBody().toString()).getMap();

			String paymentState = (paspResponse.getStatus() == HttpResponseStatus.CREATED.code())
					? ParamUtil.getString(responseBodyMap, AppParams.STATE)
					: ResourceStates.FAIL;

			String gatewayPaymentId = ParamUtil.getString(responseBodyMap, AppParams.ID);
			String payerId = ParamUtil.getString(responseBodyMap, AppParams.CUSTOMER_ID);
			String txnId = ParamUtil.getString(responseBodyMap, AppParams.PAY_ID);
			String tokenId = ParamUtil.getString(responseBodyMap, AppParams.REFERENCE);
			String accountName = ParamUtil.getString(responseBodyMap, "account_name");

			LOGGER.info("tokenId=" + tokenId);

			Map paymentResultMap = new LinkedHashMap();

			if (paspResponse.getStatus() == HttpResponseStatus.CREATED.code()) {
				if (paymentState.equalsIgnoreCase(ResourceStates.FAIL)) {
					paymentResultMap.put(AppParams.REASON, ParamUtil.getMapData(responseBodyMap, AppParams.REASON));
				} else {
					paymentResultMap.put(AppParams.STATE, ResourceStates.PURCHASED);
				}
			} else {
				paymentResultMap.put(AppParams.REASON, responseBodyMap);
			}

			PaymentService.update(paymentId, paymentState, gatewayPaymentId, payerId, txnId, tokenId,
					new JsonObject(paymentResultMap).encode(), "", "", accountName);

			if (StringUtils.equalsIgnoreCase(paymentState, ResourceStates.APPROVED)) {
				DropshipOrderService.updateState(orderId, ResourceStates.PLACED);
			} else {
				DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
			}

		} catch (Exception e) {
			LOGGER.severe(e.getMessage());
		}

	}

	private void processOrderPaymentViaPayPal(String orderId, DropshipOrderObj orderObj, String recipientEmail,
			Double amount, String currency) throws SQLException, UnirestException {

		String invoiceNumber = getRandomString16();
		String method = AppParams.PAYPAL;
		String state = ResourceStates.CREATED;
		String reference = "";

		try {

			if (PaymentService.payPalCreateInvoice(orderId, state, reference, amount.toString(), "USD", "", method,
					invoiceNumber)) {
				String paspRequestURI = paspAPIBaseURL + "/paypal/invoice/create";
				Gson gs = new Gson();

				HttpResponse<String> response = Unirest.post(paspRequestURI).header("Content-Type", "application/json")
						.body(gs.toJson(invoiceBodyRequest(orderId, invoiceNumber, recipientEmail, orderObj)))
						.asString();

				if (response.getStatus() == HttpResponseStatus.ACCEPTED.code()
						|| response.getStatus() == HttpResponseStatus.OK.code()
						|| response.getStatus() == HttpResponseStatus.CREATED.code()) {

					Map responseMap = new JsonObject(response.getBody()).getMap();

					LOGGER.info("httpResponse pasp paypal resunt========:" + responseMap);

					Map data = ParamUtil.getMapData(responseMap, "data");
					String saleId = ParamUtil.getString(data, "id");
					String paypalAccountName = ParamUtil.getString(data, "acount_name");
					// update sale id
					PaymentService.paypalSentInvoiceUpdate(invoiceNumber, saleId, paypalAccountName);
				} else {

					try {
						DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
					} catch (ParseException e) {
						LOGGER.severe(e.getMessage());
					}

				}

			}

		} catch (SQLException e) {
			PaymentService.paypalRemoveByInvoiceNumber(invoiceNumber);
			throw new BadRequestException(SystemError.PAYMENT_CREATE_INVOICE_FAILED);
		}

	}

	private static Map invoiceBodyRequest(String orderId, String invoicerNumber, String recipientEmail,
			DropshipOrderObj order) {
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
		Map orderBody = new LinkedHashMap<>();

		orderBody.put("order_id", orderId);
		orderBody.put("name", orderId);
		orderBody.put("description", "");
//		orderBody.put("quantity", order.getTotalItems());
		orderBody.put("quantity", 1);

		Map unit_amount = new LinkedHashMap<>();
		unit_amount.put("currency_code", order.getOrderCurrency());
		unit_amount.put("value", order.getOrderAmount());
		orderBody.put("unit_amount", unit_amount);

		Map tax = new LinkedHashMap<>();
		tax.put("name", "Sales Tax");
		tax.put("percent", "0");
		orderBody.put("tax", tax);

		Map amount = new LinkedHashMap<>();
		amount.put("currency_code", order.getOrderCurrency());
		amount.put("value", "0");

		Map discount = new LinkedHashMap<>();

		discount.put("amount", amount);

		orderBody.put("discount", discount);

		orderBody.put("unit_of_measure", "QUANTITY");

		List<Map> OrderIitems = new ArrayList<>();

		OrderIitems.add(orderBody);
		billPaypalBody.put("items", OrderIitems);

		return billPaypalBody;
	}

	public static String getRandomString16() {
		return String.format("%016X", new Random().nextLong()).toLowerCase();
	}

	public DropshipChargeOrderThread() {
		super();
	}

	/**
	 * 
	 * @param orderId
	 * @param userId
	 */
	public DropshipChargeOrderThread(String orderId, DropshipOrderObj orderObj, String userId) {
		super();
		this.orderId = orderId;
		this.orderObj = orderObj;
		this.userId = userId;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getUserId() {
		return userId;
	}

	public static String getAspAPIBaseURL() {
		return aspAPIBaseURL;
	}

	public static String getPaspAPIBaseURL() {
		return paspAPIBaseURL;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public static void setAspAPIBaseURL(String aspAPIBaseURL) {
		DropshipChargeOrderThread.aspAPIBaseURL = aspAPIBaseURL;
	}

	public static void setPaspAPIBaseURL(String paspAPIBaseURL) {
		DropshipChargeOrderThread.paspAPIBaseURL = paspAPIBaseURL;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipChargeOrderThread.class.getName());

}
