package asia.leadsgen.psp.server.handler.payout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.email.MailUtil;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.AuthorizationException;
import asia.leadsgen.psp.exception.MailException;
import asia.leadsgen.psp.service.PayoutService;
import asia.leadsgen.psp.service.UserService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.security.wss.AESCrypt;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class PayoutWithdrawHandler implements Handler<RoutingContext> {
	
	private static String passwordAES;

    public static void setPasswordAES(String passwordAES) {
    	PayoutWithdrawHandler.passwordAES = passwordAES;
    }

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {
				Map requestBody = routingContext.getBodyAsJson().getMap();
				String payoutMethod = ParamUtil.getString(requestBody, AppParams.PAYOUT_METHOD);
				
				// Paypal
				String paypalEmail = ParamUtil.getString(requestBody, AppParams.PAYPAL_EMAIL);
				String paypalFirstName = ParamUtil.getString(requestBody, AppParams.PAYPAL_FIRST_NAME);
				String paypalLastName = ParamUtil.getString(requestBody, AppParams.PAYPAL_LAST_NAME);
				
				// Payoneer
				String payoneerEmail = ParamUtil.getString(requestBody, AppParams.PAYONEER_EMAIL);
				String payoneerId = ParamUtil.getString(requestBody, AppParams.PAYONEER_ID);
				
				// Pingpong
				String pingpongEmail = ParamUtil.getString(requestBody, "pingpong_email");
				String pingpongId = ParamUtil.getString(requestBody, "pingpong_id");
				
				// Wire Transfer
				String wireTransferAccountName = ParamUtil.getString(requestBody, AppParams.WIRE_TRANSFER_ACCOUNT_NAME);
				String wireTransferAccountNumber = ParamUtil.getString(requestBody, AppParams.WIRE_TRANSFER_ACCOUNT_NUMBER);
				String wireTransferAccountCountry = ParamUtil.getString(requestBody, AppParams.WIRE_TRANSFER_ACCOUNT_COUNTRY);
				String wireTransferAccountNameRoutingNumber = ParamUtil.getString(requestBody, AppParams.WIRE_TRANSFER_ROUTING_NUMBER);
				
				String amount = ParamUtil.getString(requestBody, AppParams.AMOUNT);
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				Map userInfoMap = UserService.get(userId);
				Map payoutMap;
				String email = ParamUtil.getString(userInfoMap, AppParams.EMAIL);
				LOGGER.log(Level.INFO, "[PayoutWithdrawHandler] " + "userId = " + userId + " email" + email);
				
				JsonObject description = new JsonObject();
				
				if (!StringUtils.isEmpty(userId)) {
					Map payoutUnApprovedMap = PayoutService.getPayoutUnApproved(userId);
					if (payoutUnApprovedMap.size() > 0) {
						throw new MailException(SystemError.PAYOUT_PENDING_PROCESSING);
					}
					if (payoutMethod.equalsIgnoreCase("paypal")) {
						if (paypalEmail.isEmpty() || paypalFirstName.isEmpty() || paypalLastName.isEmpty()) {
							throw new MailException(SystemError.INVALID_PAYMENT_INFO);
						}
						description.put(AppParams.PAYPAL_EMAIL, paypalEmail);
						description.put(AppParams.PAYPAL_FIRST_NAME, paypalFirstName);
						description.put(AppParams.PAYPAL_LAST_NAME, paypalLastName);
						
					} else if (payoutMethod.equalsIgnoreCase("payoneer")) {
						if (payoneerEmail.isEmpty()) {
							throw new MailException(SystemError.INVALID_PAYMENT_INFO);
						}
						description.put(AppParams.PAYONEER_EMAIL, payoneerEmail);
						description.put(AppParams.PAYONEER_ID, payoneerId);
						
					} else if (payoutMethod.equalsIgnoreCase("pingpong")) {
						if (pingpongEmail.isEmpty()) {
							throw new MailException(SystemError.INVALID_PAYMENT_INFO);
						}
						description.put("pingpong_email", pingpongEmail);
						description.put("pingpong_id", pingpongId);
						
					} else if (payoutMethod.equalsIgnoreCase("wire_transfer")) {
						if (wireTransferAccountName.isEmpty() || wireTransferAccountNumber.isEmpty() || wireTransferAccountCountry.isEmpty() || wireTransferAccountNameRoutingNumber.isEmpty()) {
							throw new MailException(SystemError.INVALID_PAYMENT_INFO);
						}
						description.put(AppParams.WIRE_TRANSFER_ACCOUNT_NAME, wireTransferAccountName);
						description.put(AppParams.WIRE_TRANSFER_ACCOUNT_NUMBER, wireTransferAccountNumber);
						description.put(AppParams.WIRE_TRANSFER_ACCOUNT_COUNTRY, wireTransferAccountCountry);
						description.put(AppParams.WIRE_TRANSFER_ROUTING_NUMBER, wireTransferAccountNameRoutingNumber);
						
					}
					payoutMap = PayoutService.insert(userId, payoutMethod, amount, description.encode());
					String id = ParamUtil.getString(payoutMap, AppParams.ID);
					SimpleDateFormat dateFormat = AppConstants.DEFAULT_DATE_TIME_FORMAT;
			        String sendTime = dateFormat.format(new Date());
					AESCrypt aes = new AESCrypt();
					String code = aes.encrypt(passwordAES, sendTime);
					MailUtil.sendWithdrawConfirmationEmail(email, amount, payoutMethod, paypalEmail, paypalFirstName, paypalLastName, payoneerEmail, pingpongEmail, wireTransferAccountName, wireTransferAccountNumber, wireTransferAccountCountry, wireTransferAccountNameRoutingNumber, code, id);
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, payoutMap);

					future.complete();
				} else {
					throw new AuthorizationException(SystemError.INVALID_USER);
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

	private static final Logger LOGGER = Logger.getLogger(PayoutWithdrawHandler.class.getName());
}
