package asia.leadsgen.psp.server.handler.shopify_app.webhook;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.thymeleaf.util.StringUtils;

import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class WebhookShopifyDeleteOrderHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				
				LOGGER.info("WebhookShopifyDeleteOrderHandler() - requestBodyMap= " + routingContext.getBodyAsString());
				String storeId = routingContext.request().getParam("id");
				LOGGER.info("WebhookShopifyDeleteOrderHandler() - storeId= " + storeId);
				if (!StringUtils.isEmpty(storeId) && !StringUtils.isEmpty(routingContext.getBodyAsString())) {
					try {
						Map storeResult = DropShipStoreService.lookUp(storeId);
						if (!storeResult.isEmpty()) {
							JSONObject mJSONObject = new JSONObject(routingContext.getBodyAsString());
							String id = String.valueOf(mJSONObject.getLong("id"));
							Map order = DropshipOrderService.getOrderByOriginalId(id);
							if (!order.isEmpty() ) {
								String state = ParamUtil.getString(order, AppParams.STATE);
								String shippingId = ParamUtil.getString(order, AppParams.SHIPPING_ID);
								
								if(state.equalsIgnoreCase(ResourceStates.DRAFT) || state.equalsIgnoreCase(ResourceStates.QUEUED) ) {
									DropshipOrderService.updateStateOrderByOriginalId(storeId, id, "deleted");
								}
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
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
	
	private static final Logger LOGGER = Logger.getLogger(WebhookShopifyDeleteOrderHandler.class.getName());

}
