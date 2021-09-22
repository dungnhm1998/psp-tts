package asia.leadsgen.psp.server.handler.shopify_app;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.ShopifyService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyAppConnectAuthHandler implements Handler<RoutingContext> {
	
	private static String url;
	private static String secretKey;
	
	public static void setUrl(String url) {
		ShopifyAppConnectAuthHandler.url = url;
	}
	
	public static void setSecretKey(String secretKey) {
		ShopifyAppConnectAuthHandler.secretKey = secretKey;
	}

	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
            throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
        }
		
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				JsonObject requestBodyJson = routingContext.getBodyAsJson();
				
				String clientId = requestBodyJson.getString(AppParams.CLIENT_ID);
				String authorizationCode = requestBodyJson.getString(AppParams.CODE);
				String storeName = requestBodyJson.getString(AppParams.STORE_NAME);
				String domain = storeName + "." + AppConstants.SHOPIFY_DOMAIN;
				LOGGER.info("Shopify-AppConnectAuthHandler - clientId: " + clientId);
				LOGGER.info("Shopify-AppConnectAuthHandler - authorizationCode: " + authorizationCode);
				LOGGER.info("Shopify-AppConnectAuthHandler - storeName: " + storeName);
				
                Map storeMap = new LinkedHashMap<>();
                
				List<Map> checkUserAndStoreId = DropShipStoreService.checkUserAndStoreNameInShopifyApp(clientId, storeName);
				if (!CollectionUtils.isEmpty(checkUserAndStoreId)) {
					storeMap = checkUserAndStoreId.get(0);
				}

				Map shopifyAuthMap = ShopifyService.connectStore(authorizationCode, storeName, clientId, secretKey);
                String accessToken = ParamUtil.getString(shopifyAuthMap, "access_token");
                
                if (!accessToken.isEmpty()) {
                	
                	String locationId = null;
                	
                	String primary_location_url = "https://" + domain + "/admin/shop.json";
					HttpResponse<String> response = Unirest.get(primary_location_url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", accessToken).asString();
					LOGGER.info("response: " + response.toString());
					LOGGER.info("processData()- status: " + response.getStatus());
					LOGGER.info("processData()- status text: " + response.getStatusText());
					String storeId_DB = "";
					String currency = "";
					if (response.getStatus() == 201 || response.getStatus() == 200) {

						Map responseMap = new JsonObject(response.getBody()).getMap();
						LOGGER.info("processData()- responseMap: " + responseMap.toString());
						Map shop = ParamUtil.getMapData(responseMap, "shop");
						LOGGER.info("processData()- responseMap: " + responseMap.toString());
						long primary_location_id = ParamUtil.getLong(shop, "primary_location_id");
						LOGGER.info("processData()- primary_location_id: " + primary_location_id);
						locationId = String.valueOf(primary_location_id);
						currency = ParamUtil.getString(shop, "currency");
					}
					if(storeMap.isEmpty()) {
						storeMap  = DropShipStoreService.addShopifyAppStore(clientId, "shopify", storeName, domain, accessToken, "", "", locationId, ResourceStates.APPROVED);
						storeId_DB = ParamUtil.getString(storeMap, AppParams.ID);
						DropShipStoreService.updateStoreCurrency(storeId_DB, currency);
					} else {
						storeId_DB = ParamUtil.getString(storeMap, AppParams.ID);
						storeMap  = DropShipStoreService.update(storeId_DB, accessToken);
						DropShipStoreService.updateStoreCurrency(storeId_DB, currency);
					}
					
					try {
						boolean result = false;
						
						String urlCreateOrder = url + "/shopify-notification/create-order?id=" + storeId_DB;
						result = ShopifyService.createWebhookV2("orders/paid", urlCreateOrder, domain, accessToken);
						if (!result) {
							LOGGER.info("Create Webhook orders/paid Failed.");
						}
						
						String urlUpdateOrder = url + "/shopify-notification/update-order?id=" + storeId_DB;
						result = ShopifyService.createWebhookV2("orders/updated", urlUpdateOrder, domain, accessToken);
						if (!result) {
							LOGGER.info("Create Webhook orders/updated Failed.");
						}
						
						String urlDeleteOrder = url + "/shopify-notification/delete-order?id=" + storeId_DB;
						result = ShopifyService.createWebhookV2("orders/delete", urlDeleteOrder, domain, accessToken);
						if (!result) {
							LOGGER.info("Create Webhook orders/delete Failed.");
						}
						
						String urlCancelOrder = url + "/shopify-notification/cancelled-order?id=" + storeId_DB;
						result = ShopifyService.createWebhookV2("orders/cancelled", urlCancelOrder, domain, accessToken);
						if (!result) {
							LOGGER.info("Create Webhook orders/cancelled Failed.");
						}
						
//						String urlCreateProduct = url + "/shopify-notification/create-product?id=" + storeId_DB;
//						result = ShopifyService.createWebhookV2("products/create", urlCreateProduct, domain, accessToken);
//						if (!result) {
//							LOGGER.info("Create Webhook products/create Failed.");
//						}
						
						String urlUpdateProduct = url + "/shopify-notification/update-product?id=" + storeId_DB;
						result = ShopifyService.createWebhookV2("products/update", urlUpdateProduct, domain, accessToken);
						if (!result) {
							LOGGER.info("Create Webhook products/update Failed.");
						}
						
						String urlDeleteProduct = url + "/shopify-notification/delete-product?id=" + storeId_DB;
						result = ShopifyService.createWebhookV2("products/delete", urlDeleteProduct, domain, accessToken);
						if (!result) {
							LOGGER.info("Create Webhook products/delete Failed.");
						}
						
					} catch (UnirestException e) {
						LOGGER.info("Exception when create Webhook!");
					}
					
					LOGGER.info("Shopify-AppConnectAuthHandler - storeId: " + storeId_DB);
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
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyAppConnectAuthHandler.class.getName());

}
