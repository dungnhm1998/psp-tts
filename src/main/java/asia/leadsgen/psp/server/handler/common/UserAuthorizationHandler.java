package asia.leadsgen.psp.server.handler.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class UserAuthorizationHandler implements Handler<RoutingContext> {

	private boolean loginRequired;

	public UserAuthorizationHandler(boolean loginRequired) {
		this.loginRequired = loginRequired;
	}

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			if (loginRequired) {

				try {

					String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

					String requestUri = routingContext.request().uri();

					Pattern ptCamp = Pattern.compile(ECOMMERCE_PR_CAMP_CHECK_URI_VALIDATION_REGEXP);
					Matcher mcCamp = ptCamp.matcher(requestUri);

					Pattern ptStore = Pattern.compile(ECOMMERCE_PR_STORE_CHECK_URI_VALIDATION_REGEXP);
					Matcher mcStore = ptStore.matcher(requestUri);

					if (!mcCamp.find() && !mcStore.find() && userId.isEmpty()) {
						throw new LoginException(SystemError.SESSION_EXPIRED);
					}

					future.complete();

				} catch (Exception e) {
					routingContext.fail(e);
				}
			} else {
				routingContext.next();
			}
		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	private static final String ECOMMERCE_PR_CAMP_CHECK_URI_VALIDATION_REGEXP = "code=([^&]+)&campaign=([^&]+)";
	private static final String ECOMMERCE_PR_STORE_CHECK_URI_VALIDATION_REGEXP = "code=([^&]+)&store=([^&]+)";
}
