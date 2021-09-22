package asia.leadsgen.psp.server.handler.dropship.shopbase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.ShopbaseService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopbaseConnectAuthHandler implements Handler<RoutingContext> {
	
	private static String apiKey;
	private static String secretKey;
	private static String url;
	
	public void setApiKey(String apiKey) {
		ShopbaseConnectAuthHandler.apiKey = apiKey;
	}

	public static void setSecretKey(String secretKey) {
		ShopbaseConnectAuthHandler.secretKey = secretKey;
	}
	
	public static void setUrl(String url) {
		ShopbaseConnectAuthHandler.url = url;
	}
	
	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
            throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
        }

		routingContext.vertx().executeBlocking(future -> {

			try {
				JsonObject requestBodyJson = routingContext.getBodyAsJson();
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				if (userId.isEmpty()) {
					throw new LoginException(SystemError.LOGIN_REQUIRED);
				}
				String authorizationCode = requestBodyJson.getString(AppParams.CODE);
				String storeName = requestBodyJson.getString("store_name");
				String domain = storeName + "." + AppConstants.SHOPBASE_DOMAIN;
				LOGGER.info("ShopbaseConnectAuthHandler - authorizationCode: " + authorizationCode);
				LOGGER.info("ShopbaseConnectAuthHandler - storeName: " + storeName);
                
                Map shopifyAuthMap = ShopbaseService.connectStore(authorizationCode, storeName, apiKey, secretKey);	
                Map storeMap = new LinkedHashMap<>();
                
                String accessToken = ParamUtil.getString(shopifyAuthMap, "access_token");
                
				if (!accessToken.isEmpty()) {
					String locationId = null;
					Map storeLocationMap = ShopbaseService.getStoreLocation(domain, accessToken);
					List<Map> locationIdList = ParamUtil.getListData(storeLocationMap, "locations");
					if (!locationIdList.isEmpty()) {
						locationId = ParamUtil.getString(locationIdList.get(0), AppParams.ID);
					}
					
					// Check exist accessToken
					storeMap = DropShipStoreService.find(userId, domain);
					String storeId = ParamUtil.getString(storeMap, AppParams.ID);
					String accessToken_DB = ParamUtil.getString(storeMap, AppParams.API_KEY);
					if (!storeId.isEmpty()) {
						if (!accessToken.equals(accessToken_DB)) {
							storeMap = DropShipStoreService.update(storeId, accessToken);
						}
					} else {
						storeMap = DropShipStoreService.addStore(userId, AppConstants.SHOPBASE, storeName, domain, accessToken, "", "", locationId, ResourceStates.APPROVED);
						String storeId_DB = ParamUtil.getString(storeMap, AppParams.ID);
						LOGGER.info("ShopbaseConnectAuthHandler - storeId_DB: " + storeId_DB);
//						new Thread(() -> {
							try {
								boolean result = false;
								result = ShopbaseService.createWebhook("orders/paid", url, storeId_DB, domain, accessToken, "paid-order");
								if (!result) {
									LOGGER.info("Shopbase Create Webhook orders/paid Failed.");
								}
								result = ShopbaseService.createWebhook("refunds/create", url, storeId_DB, domain, accessToken, "refund-order");
								if (!result) {
									LOGGER.info("Shopbase Create Webhook refunds/create Failed.");
								}
							} catch (UnirestException e) {
								LOGGER.info("Exception when create Shopbase Webhook!");
							}
//						});
					}
				}
                
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, storeMap);

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
	
	private static final Logger LOGGER = Logger.getLogger(ShopbaseConnectAuthHandler.class.getName());

}
