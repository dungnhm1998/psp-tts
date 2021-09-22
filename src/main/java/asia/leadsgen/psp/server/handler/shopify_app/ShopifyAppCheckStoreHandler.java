package asia.leadsgen.psp.server.handler.shopify_app;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyAppCheckStoreHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			String clientId = routingContext.request().getParam(AppParams.CLIENT_ID);
			LOGGER.info("clientId= " + clientId);
			if (StringUtils.isEmpty(clientId)) {
				throw new LoginException(SystemError.INVALID_REQUEST);
			}
			
			String storeName = routingContext.request().getParam(AppParams.STORE_NAME);
			LOGGER.info("storeName= " + storeName);
			if (StringUtils.isEmpty(storeName)) {
				throw new LoginException(SystemError.INVALID_REQUEST);
			}
			
			try {
		
				String domain = storeName + "." + AppConstants.SHOPIFY_DOMAIN;
				
				List<Map> checkUserAndStoreId = DropShipStoreService.checkUserAndStoreNameInShopifyApp(clientId, storeName);
				boolean exists = false;
				Map result = new HashedMap<>();
				if (CollectionUtils.isEmpty(checkUserAndStoreId)) {
					result.put("result", exists);
				} else {
					String accessToken_DB = ParamUtil.getString(checkUserAndStoreId.get(0), AppParams.API_KEY);
					LOGGER.info("accessToken_DB: " + accessToken_DB);
					if (!accessToken_DB.isEmpty()) {
						String url = "https://" + domain + "/admin/shop.json";
						HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", accessToken_DB).asString();
						LOGGER.info("response: " + response.toString());
						LOGGER.info("processData()- status: " + response.getStatus());
						LOGGER.info("processData()- status text: " + response.getStatusText());
						if (response.getStatus() == 201 || response.getStatus() == 200) {
							exists = true;
							Map responseMap = new JsonObject(response.getBody()).getMap();
							Map shop = ParamUtil.getMapData(responseMap, "shop");
							String currency = ParamUtil.getString(shop, "currency");
							String storeId_DB = ParamUtil.getString(checkUserAndStoreId.get(0), AppParams.ID);
							DropShipStoreService.updateStoreCurrency(storeId_DB, currency);
						}
					}
					result.put("result", exists);
				}
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, result);

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
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyAppCheckStoreHandler.class.getName());
}
