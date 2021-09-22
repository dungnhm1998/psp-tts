package asia.leadsgen.psp.server.handler.shopify_app.webhook;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import org.thymeleaf.util.StringUtils;

import com.google.gson.Gson;

import asia.leadsgen.psp.obj.ShopifyProductObj;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class WebhookShopifyDeleteProductHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				LOGGER.info("WebhookShopifyDeleteProductHandler() - requestBodyMap= " + routingContext.getBodyAsString());
				String storeId = routingContext.request().getParam("id");
				LOGGER.info("WebhookShopifyDeleteProductHandler() - storeId= " + storeId);
				if (!StringUtils.isEmpty(storeId) && !StringUtils.isEmpty(routingContext.getBodyAsString()) ) {
					try {
						Map storeResult = DropShipStoreService.lookUp(storeId);
						String storeName = ParamUtil.getString(storeResult, AppParams.NAME);
						if (!storeResult.isEmpty()) {
							ShopifyProductObj productObj =  new Gson().fromJson(routingContext.getBodyAsString(), ShopifyProductObj.class);	
							ShopifyAppService.deleteShopifyProduct(storeName, productObj.getId());
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
	
	private static final Logger LOGGER = Logger.getLogger(WebhookShopifyDeleteProductHandler.class.getName());
}
