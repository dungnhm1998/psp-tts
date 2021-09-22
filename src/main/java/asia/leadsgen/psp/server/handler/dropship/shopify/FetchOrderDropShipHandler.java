package asia.leadsgen.psp.server.handler.dropship.shopify;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.service_fulfill.EtsyService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;


public class FetchOrderDropShipHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {
    	
        routingContext.vertx().executeBlocking((Future<Object> future) -> {

            try {
            	Map result = new LinkedHashMap<>();
        		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
                String storeId = routingContext.request().params().get(AppParams.STORE_ID);
                String date = routingContext.request().params().get(AppParams.DATE);
        		String status = routingContext.request().params().get(AppParams.STATUS);
        		
                Map storeMap = DropShipStoreService.lookUp(storeId);
                String channel = null;
            	LOGGER.info("storeMap= " + storeMap);
                if(!storeMap.isEmpty()) {

                    String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
    				String consumerSecret = ParamUtil.getString(storeMap, AppParams.SECRET);
    				String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
    				channel = ParamUtil.getString(storeMap, AppParams.CHANNEL).toLowerCase();

                    switch (channel) {
                        case "etsy" :
							EtsyFetchOrder etsy = new EtsyFetchOrder();
                            String etsyStoreId = String.valueOf(storeMap.get("channel_store_id"));
                            result = etsy.getEtsyOrderByShopId(consumerKey, consumerSecret, storeId, userId, etsyStoreId, date, status);
                            break;

                        case "shopify":
                            ShopifyFetchOrder shopify = new ShopifyFetchOrder();
                            result = shopify.getOrderShopify(routingContext, userId, storeId, consumerKey, domain, channel, date, status);
                            break;

                        case "woocommerce":
                            WooEcommerceFetchOrder woo = new WooEcommerceFetchOrder();
                            result = woo.getOrderWoo(userId, storeId, consumerKey, consumerSecret, domain, channel, date, status);
                            break;
                    }
    				
                } else {
                	LOGGER.info("store is not valid");
                }
                if(!result.isEmpty() && channel.equalsIgnoreCase("etsy")) {
                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                    routingContext.put(AppParams.RESPONSE_DATA, result);
                }

                else if (!result.isEmpty() && !channel.equalsIgnoreCase("etsy")) {

                	routingContext.put(AppParams.RESPONSE_CODE, result.get(AppParams.RESPONSE_CODE));
	                routingContext.put(AppParams.RESPONSE_MSG, result.get(AppParams.RESPONSE_MSG));
	                routingContext.put(AppParams.RESPONSE_DATA, result.get(AppParams.RESPONSE_DATA));

                } else {
	                routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
	                routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
	                routingContext.put(AppParams.RESPONSE_DATA, new LinkedHashMap<>());
                }
                future.complete();

            } catch (Exception e) {
            	LOGGER.severe(e.getMessage());
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

	

    private static final Logger LOGGER = Logger.getLogger(FetchOrderDropShipHandler.class.getName());

}
