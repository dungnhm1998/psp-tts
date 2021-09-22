package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderListBaseHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
		if (userId.isEmpty()) {
			throw new LoginException(SystemError.LOGIN_REQUIRED);
		}

		routingContext.vertx().executeBlocking(future -> {

			try {

				Map listBase = BaseService.getAllBaseCache();
				Map resultMap = new LinkedHashMap();
				resultMap.put(AppParams.BASES, listBase);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, resultMap);

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

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderListBaseHandler.class.getName());
}
