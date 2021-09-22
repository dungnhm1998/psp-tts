package asia.leadsgen.psp.server.handler.shopify_app;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyUnsyncProductVariantHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
				throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
			}
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
			Map requestBodyMap = routingContext.getBodyAsJson().getMap();
			
//			String storeId = ParamUtil.getString(requestBodyMap, AppParams.STORE_ID);
//			LOGGER.info("storeId= " + storeId);
//			if (StringUtils.isEmpty(storeId)) {
//				throw new LoginException(SystemError.INVALID_DROPSHIP_STORE_ID);
//			}
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
							
				List<Long> variants = ParamUtil.getListData(requestBodyMap, AppParams.VARIANTS);
				if (CollectionUtils.isEmpty(variants)) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
				}
				
				Long productId = Long.parseLong(ParamUtil.getString(requestBodyMap, AppParams.PRODUCT_ID));
				Map productVariantMap = ShopifyAppService.lookup(productId);
				int synced = ParamUtil.getInt(productVariantMap, AppParams.SYNCED);
				
				int countUnsync = 0;
				
				for (Long variantId : variants) {
					ShopifyAppService.updateStateSyncedProductVariant(variantId, ResourceStates.UN_SYNC);
					countUnsync++;
					if (countUnsync == synced) {
						ShopifyAppService.updateShopifyProduct(productId, "", "");
					}
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
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyUnsyncProductVariantHandler.class.getName());
}
