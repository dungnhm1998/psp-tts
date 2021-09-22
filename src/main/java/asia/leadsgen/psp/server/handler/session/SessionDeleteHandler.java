package asia.leadsgen.psp.server.handler.session;

import java.util.Map;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service.DomainService;
import asia.leadsgen.psp.service.SessionService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class SessionDeleteHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {

				String sessionId = routingContext.request().getParam(AppParams.ID);

				if (sessionId.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_SESSION);
				}

				SessionService.delete(sessionId);

				String host = routingContext.get(AppParams.HOST);
				Map defaultDomain = DomainService.getDefaultDomain(host);

				String domainName = ParamUtil.getString(defaultDomain, AppParams.NAME);

				Cookie sessionCookie = Cookie.cookie(AppParams.SESSION_ID, StringPool.BLANK);
				sessionCookie.setDomain(domainName);
				sessionCookie.setMaxAge(-1);
				sessionCookie.setPath(StringPool.FORWARD_SLASH);

				Cookie userCookie = Cookie.cookie(AppParams.USER_EMAIL, StringPool.BLANK);
				userCookie.setDomain(domainName);
				userCookie.setMaxAge(-1);
				userCookie.setPath(StringPool.FORWARD_SLASH);

				routingContext.addCookie(sessionCookie);
				routingContext.addCookie(userCookie);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());

				future.complete();

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
}
