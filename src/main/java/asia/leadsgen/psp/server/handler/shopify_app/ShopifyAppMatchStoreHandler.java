package asia.leadsgen.psp.server.handler.shopify_app;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyAppMatchStoreHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			LOGGER.info("userId: " + userId);
			if (userId.isEmpty()) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
					
			Map requestBodyMap = routingContext.getBodyAsJson().getMap();
			
			String clientId = ParamUtil.getString(requestBodyMap, AppParams.CLIENT_ID);
			LOGGER.info("clientId: " + clientId);		
			if (StringUtils.isEmpty(clientId)) {
				throw new LoginException(SystemError.INVALID_REQUEST);
			}
			
			String storeName = ParamUtil.getString(requestBodyMap, AppParams.STORE_NAME);
			LOGGER.info("storeName: " + storeName);
			if (StringUtils.isEmpty(storeName)) {
				throw new LoginException(SystemError.INVALID_REQUEST);
			}
			
			List<Map> checkUserAndStoreId = null;
			try {
				checkUserAndStoreId = DropShipStoreService.checkUserAndStoreNameInShopifyApp(clientId, storeName);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			LOGGER.info("checkUserAndStoreId: " + checkUserAndStoreId.toString());
			
			if (CollectionUtils.isEmpty(checkUserAndStoreId)) {
				throw new LoginException(SystemError.INVALID_STORE);
			}
			
			try {
		
				Map result = new HashedMap<>();
				String userIdDb = ParamUtil.getString(checkUserAndStoreId.get(0), AppParams.USER_ID);
				LOGGER.info("userIdDb= " + userIdDb);
				String storeId = "";
				String currency = "";
				if (StringUtils.isEmpty(userIdDb)) {
					Map storeMap = DropShipStoreService.matchShopifyAppStoreId(userId, clientId, storeName);
					storeId = ParamUtil.getString(storeMap, AppParams.ID);
					LOGGER.info("StoreId: " + storeId);
					currency = ParamUtil.getString(storeMap, AppParams.CURRENCY);			
					result.put(AppParams.STORE_ID, storeId);
					result.put(AppParams.CURRENCY, currency);
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, result);
					
				} else {
					if (!userIdDb.equalsIgnoreCase(userId)) {
						throw new BadRequestException(new SystemError
								("INVALID_USER", "This store is already associcated with other user!", "", "http://developer.30usd.com/errors/401.html"));
					} else {
						storeId = ParamUtil.getString(checkUserAndStoreId.get(0), AppParams.ID);
						LOGGER.info("StoreId: " + storeId);
						currency = ParamUtil.getString(checkUserAndStoreId.get(0), AppParams.CURRENCY);
						result.put(AppParams.STORE_ID, storeId);
						result.put(AppParams.CURRENCY, currency);
						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
						routingContext.put(AppParams.RESPONSE_DATA, result);
					}
				}
				
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
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyAppMatchStoreHandler.class.getName());
}
