package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderLookupV2Handler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
		if (userId.isEmpty()) {
			throw new LoginException(SystemError.LOGIN_REQUIRED);
		}
		
//		MultiMap requestParams = routingContext.request().params();
//		
//		String storeId = requestParams.get(AppParams.STORE_ID);
//		LOGGER.info("storeId= " + storeId);
//		if (!StringUtils.isEmpty(storeId)) {
//			Map storeMap = null;
//			try {
//				storeMap = DropShipStoreService.lookUp(storeId);
//			} catch (SQLException e1) {
//				e1.printStackTrace();
//			}
//			String storeUserId = ParamUtil.getString(storeMap, AppParams.USER_ID);
//			LOGGER.info("storeUserId= " + storeUserId);
//			if (!storeUserId.equalsIgnoreCase(userId)) {
//				throw new LoginException(SystemError.INVALID_USER);
//			}
//		}
	
		routingContext.vertx().executeBlocking(future -> {
			
			try {

				String orderId = routingContext.request().getParam(AppParams.ID);
				Map orderInfoMap = DropshipOrderService.lookUpV2(orderId, true, true, false);

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
	
	private static final Logger LOGGER = Logger.getLogger(DropshipOrderLookupV2Handler.class.getName());

}
