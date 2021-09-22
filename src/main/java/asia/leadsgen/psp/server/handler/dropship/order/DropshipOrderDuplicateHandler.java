package asia.leadsgen.psp.server.handler.dropship.order;

import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderDuplicateHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
		routingContext.vertx().executeBlocking(future -> {
			try {

				String orderId = routingContext.request().getParam(AppParams.ID);

				Map orderInfoMap = DropshipOrderService.lookUp(orderId, false, false, false);
				String orderUserId = ParamUtil.getString(orderInfoMap, AppParams.USER_ID);

				if (!userId.equals(orderUserId)) {
					throw new BadRequestException(SystemError.OPERATION_NOT_PERMITTED);
				}

				String trackingCode = AppUtil.generateOrderTrackingNumber();
				orderInfoMap = DropshipOrderService.duplicate(orderId, trackingCode);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, orderInfoMap);

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

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderDuplicateHandler.class.getName());
}
