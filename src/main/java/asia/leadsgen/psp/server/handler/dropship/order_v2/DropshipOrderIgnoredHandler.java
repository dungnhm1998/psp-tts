package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderIgnoredHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
		LOGGER.info("userId= " + userId);
		if (StringUtils.isEmpty(userId)) {
			throw new LoginException(SystemError.LOGIN_REQUIRED);
		}
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		Map requestBodyMap = routingContext.getBodyAsJson().getMap();
		LOGGER.info("requestBodyMap=" + requestBodyMap.toString());
		
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				
				List<String> orders = ParamUtil.getListData(requestBodyMap, AppParams.ORDERS);
				if (CollectionUtils.isEmpty(orders)) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
				}
				
				for (String orderId : orders) {

					Map orderInfoMap = DropshipOrderService.lookUpV2(orderId, false, false, false);
					String orderUserId = ParamUtil.getString(orderInfoMap, AppParams.USER_ID);

					if (!userId.equals(orderUserId)) {
						throw new BadRequestException(SystemError.OPERATION_NOT_PERMITTED);
					}

					orderInfoMap = DropshipOrderService.ignoreOrder(orderId);
				}
				
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
	
	private static final Logger LOGGER = Logger.getLogger(DropshipOrderIgnoredHandler.class.getName());

}
