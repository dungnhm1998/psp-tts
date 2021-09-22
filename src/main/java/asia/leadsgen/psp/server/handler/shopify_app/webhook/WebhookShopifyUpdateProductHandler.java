package asia.leadsgen.psp.server.handler.shopify_app.webhook;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.thymeleaf.util.StringUtils;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.ShopifyProductObj;
import asia.leadsgen.psp.obj.ShopifyProductPullObj;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class WebhookShopifyUpdateProductHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			
			try {
				LOGGER.info("WebhookShopifyUpdateProductHandler() - requestBodyMap= " + routingContext.getBodyAsString());
				String storeId = routingContext.request().getParam("id");
				LOGGER.info("WebhookShopifyUpdateProductHandler() - storeId= " + storeId);
				
				if (!StringUtils.isEmpty(storeId) && !StringUtils.isEmpty(routingContext.getBodyAsString())) {
//					try {
//						
//						ShopifyProductObj productObj =  new Gson().fromJson(routingContext.getBodyAsString(), ShopifyProductObj.class);
//						String productStoreId = productObj.getId().toString();
//						LOGGER.info("productStoreId: " + productStoreId);
						
//						String jsonBody = productObj.toString();
//						LOGGER.info("webhook product body: " + jsonBody);

//						ShopifyAppService.insertReceiveWebhook(storeId, productStoreId, "product", new Gson().toJson(productObj));
						
//						Map storeResult = DropShipStoreService.lookUp(storeId);
//						if (!storeResult.isEmpty()) {
//							String consumerKey = ParamUtil.getString(storeResult, AppParams.API_KEY);
//		    				String domain = ParamUtil.getString(storeResult, AppParams.DOMAIN);
//		    				String store_name = ParamUtil.getString(storeResult, AppParams.NAME);
//							ShopifyProductObj productObj =  new Gson().fromJson(routingContext.getBodyAsString(), ShopifyProductObj.class);
//							
//							String productStoreId = productObj.getId().toString();
//							LOGGER.info("productStoreId: " + productStoreId);
//							
//						    String getMetaUrl = String.format(ShopifyAPIEndpoints.GET_PRODUCT_METAFIELDS, domain, productStoreId);
//						    HttpResponse<String> responseGetMeta = Unirest.get(getMetaUrl).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
//						        .asString();
//						    
//						    Map responseGetMetaMap = new JsonObject(responseGetMeta.getBody()).getMap();
////						    LOGGER.info("responseGetMetaMap:" + responseGetMetaMap.toString());
//						    
//						    List<Map> metafields = ParamUtil.getListData(responseGetMetaMap, "metafields");
//						    LOGGER.info("metafields: " + metafields.toString());
//						    
//						    if (metafields == null || metafields.isEmpty()) {
//						    	ShopifyProductPullObj productPullObj = new ShopifyProductPullObj();
//						    	productPullObj.setProduct(productObj);
//						    	LOGGER.info("Merge Product with Not metafields...");
//						    	ShopifyAppService.mergeProduct(storeId, store_name, productPullObj);
//						    } else {
//						    	Map metafield = metafields.get(0);
//						    	String namespace = ParamUtil.getString(metafield, "namespace");				    
//							    String keyMetafield = ParamUtil.getString(metafield, "key");
//							    LOGGER.info("namespace= " + namespace + " - key= " + keyMetafield);
//							    
//							    if (!namespace.equalsIgnoreCase("burgerprints")
//							    		&& !keyMetafield.equalsIgnoreCase("webhook_create_product")) {
//									ShopifyProductPullObj productPullObj = new ShopifyProductPullObj();
//							    	productPullObj.setProduct(productObj);
//							    	ShopifyAppService.mergeProduct(storeId, store_name, productPullObj);
//							    } else {
//							    	LOGGER.info("Product had pushed from BG Shopify-app!");
//							    }
//						    }
//						    
//						    LOGGER.info("done!");
//						}

//					} catch (SQLException e) {
//						e.printStackTrace();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
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

	private static final Logger LOGGER = Logger.getLogger(WebhookShopifyUpdateProductHandler.class.getName());

}