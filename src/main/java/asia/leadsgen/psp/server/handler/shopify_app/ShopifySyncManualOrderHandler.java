package asia.leadsgen.psp.server.handler.shopify_app;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.WebhookShopifyUpdateOrderHandler;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifySyncManualOrderHandler extends PSPOrderHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			
			LOGGER.info("ShopifySyncManualOrderHandler");
			String userIdRequest = ContextUtil.getString(routingContext, AppParams.USER_ID);
			LOGGER.info("userId= " + userIdRequest);
			if (StringUtils.isEmpty(userIdRequest)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
			try {
				String order_id = routingContext.request().getParam(AppParams.ID);
				if (StringUtils.isEmpty(order_id)) {
					throw new BadRequestException(SystemError.INVALID_REQUEST);
				}
				Map order = DropshipOrderService.lookUpV2(order_id, false, false, false);
				if (!order.isEmpty() ) {
					LOGGER.info("order= " + order.toString());
					String state = ParamUtil.getString(order, AppParams.STATE);
					
					if(state.equalsIgnoreCase(ResourceStates.DRAFT) || state.equalsIgnoreCase(ResourceStates.QUEUED) ) {
						String original_id = ParamUtil.getString(order, AppParams.ORIGINAL_ID);
						String store_id = ParamUtil.getString(order, AppParams.STORE_ID);
						LOGGER.info("storeId= " + store_id);
						Map storeResult = DropShipStoreService.lookUp(store_id);
						if (!storeResult.isEmpty()) {
							initItemGroupQuantity();
							String storeUserId = ParamUtil.getString(storeResult, AppParams.USER_ID);
							LOGGER.info("storeUserId= " + storeUserId);
							if (!storeUserId.equalsIgnoreCase(userIdRequest)) {
								throw new BadRequestException(SystemError.INVALID_USER);
							}
							String consumerKey = ParamUtil.getString(storeResult, AppParams.API_KEY);
							String domain = ParamUtil.getString(storeResult, AppParams.DOMAIN);
							String channel = ParamUtil.getString(storeResult, AppParams.CHANNEL);
							String userId = ParamUtil.getString(storeResult, AppParams.USER_ID);
							
							String url = String.format(ShopifyAPIEndpoints.ORDERS_DETAIL_USING_TOKEN, domain, original_id);
							HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
									.asString();
							Map mapResult = new JsonObject(response.getBody()).getMap();
							LOGGER.info("mapResult= " + mapResult.toString());
							Map orderFetched = ParamUtil.getMapData(mapResult, "order");
							JSONObject obj = new JSONObject(orderFetched);
							String body_string = obj.toString();
							LOGGER.info("body_string= " + body_string);
							
							WebhookShopifyUpdateOrderHandler process_order = new WebhookShopifyUpdateOrderHandler();
							process_order.processOrder(body_string, consumerKey, domain, userId, store_id, channel);
						}
					}
					order = DropshipOrderService.lookUpV2(order_id, true, true, false);
				}
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, order);
				
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

	private static final Logger LOGGER = Logger.getLogger(ShopifySyncManualOrderHandler.class.getName());

}
