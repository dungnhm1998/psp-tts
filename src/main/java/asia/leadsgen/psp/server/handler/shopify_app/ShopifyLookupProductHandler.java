package asia.leadsgen.psp.server.handler.shopify_app;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyLookupProductHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
//			MultiMap requestParams = routingContext.request().params();
//			
//			String storeId = requestParams.get(AppParams.STORE_ID);
//			LOGGER.info("storeId= " + storeId);
//			
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
			
			try {
							
				Long productId = Long.parseLong(routingContext.request().getParam(AppParams.ID));
				Map productVariantMap = ShopifyAppService.lookup(productId);
				
				String productState = ParamUtil.getString(productVariantMap, AppParams.STATE);
				if (ResourceStates.LOCKED.equalsIgnoreCase(productState)) {
					throw new BadRequestException(SystemError.INVALID_PRODUCT);
				}
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, productVariantMap);
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
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyLookupProductHandler.class.getName());
}
