package asia.leadsgen.psp.server.handler.payout;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.AuthorizationException;
import asia.leadsgen.psp.exception.MailException;
import asia.leadsgen.psp.service.PayoutService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.security.wss.AESCrypt;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class PayoutConfirmHandler implements Handler<RoutingContext> {
	
	private static String passwordAES;

    public static void setPasswordAES(String passwordAES) {
    	PayoutConfirmHandler.passwordAES = passwordAES;
    }

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {
				Map requestBody = routingContext.getBodyAsJson().getMap();
				String code = ParamUtil.getString(requestBody, AppParams.CODE);
				String id = ParamUtil.getString(requestBody, AppParams.ID);
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

				if (!StringUtils.isEmpty(userId)) {
					
					AESCrypt aes = new AESCrypt();
					String sendDate = aes.decrypt(passwordAES, code);
					
					if (AppUtil.isExpiredAfter2Hours(sendDate)) {
						PayoutService.delete();
						throw new MailException(SystemError.OPERATION_EXPIRED);
					}
					Map payoutConfirmMap = PayoutService.update(userId, id);
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, payoutConfirmMap);

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

	private static final Logger LOGGER = Logger.getLogger(PayoutConfirmHandler.class.getName());
}
