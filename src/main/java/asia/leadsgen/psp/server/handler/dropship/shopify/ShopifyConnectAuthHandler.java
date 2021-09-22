package asia.leadsgen.psp.server.handler.dropship.shopify;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.ShopifyService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyConnectAuthHandler implements Handler<RoutingContext> {
	
	private static String apiKey;
	private static String secretKey;
	private static String url;
	
	public void setApiKey(String apiKey) {
		ShopifyConnectAuthHandler.apiKey = apiKey;
	}

	public static void setSecretKey(String secretKey) {
		ShopifyConnectAuthHandler.secretKey = secretKey;
	}
	
	public static void setUrl(String url) {
		ShopifyConnectAuthHandler.url = url;
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
				String domain = storeName + "." + AppConstants.SHOPIFY_DOMAIN;
				LOGGER.info("ShopifyConnectAuthHandler - authorizationCode: " + authorizationCode);
				LOGGER.info("ShopifyConnectAuthHandler - storeName: " + storeName);
                
                Map shopifyAuthMap = ShopifyService.connectStore(authorizationCode, storeName, apiKey, secretKey);	
                Map storeMap = new LinkedHashMap<>();
                
                String accessToken = ParamUtil.getString(shopifyAuthMap, "access_token");
                
				if (!accessToken.isEmpty()) {
					String locationId = null;
//					Map storeLocationMap = ShopifyService.getStoreLocation(domain, accessToken);
//					List<Map> locationIdList = ParamUtil.getListData(storeLocationMap, "locations");
//					if (!locationIdList.isEmpty()) {
//						locationId = ParamUtil.getString(locationIdList.get(0), AppParams.ID);
//					}
					String url_primary_location_id = "https://" + domain + "/admin/shop.json";
					HttpResponse<String> response = Unirest.get(url_primary_location_id).header("Content-Type", "application/json").header("X-Shopify-Access-Token", accessToken).asString();
					LOGGER.info("response: " + response.toString());
					LOGGER.info("processData()- status: " + response.getStatus());
					LOGGER.info("processData()- status text: " + response.getStatusText());
					if (response.getStatus() == 201 || response.getStatus() == 200) {

						Map responseMap = new JsonObject(response.getBody()).getMap();
						LOGGER.info("processData()- responseMap: " + responseMap.toString());
						Map shop = ParamUtil.getMapData(responseMap, "shop");
						LOGGER.info("processData()- responseMap: " + responseMap.toString());
						long primary_location_id = ParamUtil.getLong(shop, "primary_location_id");
						LOGGER.info("processData()- primary_location_id: " + primary_location_id);
						locationId = String.valueOf(primary_location_id);
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
						storeMap = DropShipStoreService.addStore(userId, "shopify", storeName, domain, accessToken, "", "", locationId, ResourceStates.APPROVED);
						String storeId_DB = ParamUtil.getString(storeMap, AppParams.ID);
						LOGGER.info("ShopifyConnectAuthHandler - storeId_DB: " + storeId_DB);
//						new Thread(() -> {
//							try {
//								boolean result = false;
//								result = ShopifyService.createWebhook("orders/paid", url, storeId_DB, domain, accessToken, "paid-order");
//								if (!result) {
//									LOGGER.info("Create Webhook orders/paid Failed.");
//								}
//								result = ShopifyService.createWebhook("refunds/create", url, storeId_DB, domain, accessToken, "refund-order");
//								if (!result) {
//									LOGGER.info("Create Webhook refunds/create Failed.");
//								}
//							} catch (UnirestException e) {
//								LOGGER.info("Exception when create Webhook!");
//							}
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
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyConnectAuthHandler.class.getName());

}
