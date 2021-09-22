package asia.leadsgen.psp.thread;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import asia.leadsgen.psp.email.MailUtil;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service.PaymentService;
import asia.leadsgen.psp.service.UserService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;

public class ShopbaseChargeOrderThread extends Thread {

	private String orderId;
	private String userId;
	private static String aspAPIBaseURL;
	private static String paspAPIBaseURL;

	@Override
	public void run() {
		if (StringUtils.isNotEmpty(userId)) {
			try {

				Map user = UserService.get(userId);

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
					String stripeCustomerId = cardInfo.getString(AppParams.CUSTOMER_ID);
					if (StringUtils.isEmpty(stripeCustomerId)) {
						DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
					} else {
						Map orderInfo = DropshipOrderService.lookUp(orderId, false, false, false);
						String amount = ParamUtil.getString(orderInfo, AppParams.AMOUNT);
						String currency = ParamUtil.getString(orderInfo, AppParams.CURRENCY);
						Map payment = PaymentService.insert(AppParams.SALE, orderId, "stripe", amount, currency);
						String paymentId = ParamUtil.getString(payment, AppParams.ID);

						processOrderPayment(orderId, username, email, paymentId, amount, currency, stripeCustomerId);

					}
				}

			} catch (Exception e) {
				LOGGER.severe(e.getMessage());
			}

		}
	}

	private void processOrderPayment(String orderId, String username, String email, String paymentId, String amount,
			String currency, String stripeCustomerId) {

		JsonObject paymentRequestBody = new JsonObject();
		paymentRequestBody.put(AppParams.REFERENCE, paymentId);
		paymentRequestBody.put(AppParams.AMOUNT, amount);
		paymentRequestBody.put(AppParams.CURRENCY, currency);
		paymentRequestBody.put(AppParams.METHOD, "stripe");
		paymentRequestBody.put(AppParams.CUSTOMER_ID, stripeCustomerId);
		String paspRequestURL = paspAPIBaseURL + "/charge/customer";
		try {
			HttpResponse<String> paspResponse = Unirest.post(paspRequestURL).header("Content-Type", "application/json")
					.body(paymentRequestBody.encode()).asString();

			if (paspResponse.getStatus() != HttpResponseStatus.CREATED.code()) {
				DropshipOrderService.updateState(orderId, ResourceStates.CHARGE_FAIL);
				MailUtil.shopifyChargeFailNotify(orderId, username, email);
			} else {
				JsonObject responseBody = new JsonObject(paspResponse.getBody());
				String accountName = responseBody.getString("account_name");
				String reference = responseBody.getString(AppParams.REFERENCE);
				String txnId = responseBody.getString(AppParams.PAY_ID);
				PaymentService.update(paymentId, ResourceStates.APPROVED, reference, "", txnId, "", "", "", "",
						accountName);
				DropshipOrderService.updateState(orderId, ResourceStates.PLACED);
			}

		} catch (Exception e) {
			LOGGER.severe(e.getMessage());
		}

	}

	public ShopbaseChargeOrderThread() {
		super();
	}

	/**
	 * 
	 * @param orderId
	 * @param userId
	 */
	public ShopbaseChargeOrderThread(String orderId, String userId) {
		super();
		this.orderId = orderId;
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
		ShopbaseChargeOrderThread.aspAPIBaseURL = aspAPIBaseURL;
	}

	public static void setPaspAPIBaseURL(String paspAPIBaseURL) {
		ShopbaseChargeOrderThread.paspAPIBaseURL = paspAPIBaseURL;
	}

	private static final Logger LOGGER = Logger.getLogger(ShopbaseChargeOrderThread.class.getName());

}
