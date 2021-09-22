package asia.leadsgen.psp.server.handler.session;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service.SessionService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class SessionLookupHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {

				String sessionId = routingContext.request().getParam(AppParams.ID);

				if (sessionId.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_SESSION);
				}
				Map userInfo = new LinkedHashMap<>();
				Map session = SessionService.find(sessionId);
				if (session != null && !session.isEmpty()) {
					Map user = ParamUtil.getMapData(session, AppParams.USER);
					userInfo.put(AppParams.ID, ParamUtil.getString(user, AppParams.ASP_REF_ID));
					userInfo.put(AppParams.AFFID, ParamUtil.getString(user, AppParams.ID));
				}

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, userInfo);

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

	private static final Logger LOGGER = Logger.getLogger(SessionLookupHandler.class.getName());
}
