package asia.leadsgen.psp.server.handler.shopify_app;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyEditDescriptionHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
			Map requestBodyMap = routingContext.getBodyAsJson().getMap();
			
			String storeId = ParamUtil.getString(requestBodyMap, AppParams.STORE_ID);
			LOGGER.info("storeId= " + storeId);
			if (StringUtils.isEmpty(storeId)) {
				throw new LoginException(SystemError.INVALID_DROPSHIP_STORE_ID);
			}
			
			Map storeMap = null;
			try {
				storeMap = DropShipStoreService.lookUp(storeId);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			String storeUserId = ParamUtil.getString(storeMap, AppParams.USER_ID);
			LOGGER.info("storeUserId= " + storeUserId);
			if (!storeUserId.equalsIgnoreCase(userId)) {
				throw new LoginException(SystemError.INVALID_USER);
			}
			
			try {
							
            	String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
				String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
				
				String productRefId = ParamUtil.getString(requestBodyMap, AppParams.ID);
				String desc = ParamUtil.getString(requestBodyMap, AppParams.DESCRIPTION);
				String title = ParamUtil.getString(requestBodyMap, AppParams.TITLE);			
				
				Map resultMap = updateToShopifyStore(productRefId, desc, title, consumerKey, domain);
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, resultMap);
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
	
	private Map updateToShopifyStore(String productRefId, String desc, String title, String consumerKey, String domain) 
			throws UnirestException, SQLException, ParseException {
		
		String url = String.format(ShopifyAPIEndpoints.PRODUCTS_ONE_USING_TOKEN, domain, productRefId);
		
		Long productId = Long.parseLong(productRefId);
		Map requestMap = new HashMap<>();
		
		Map updateMap = new LinkedHashMap<>();
		updateMap.put("id", productRefId);
		updateMap.put("body_html", StringUtil.urlDecode(desc));
		updateMap.put("title", title);
	
		requestMap.put("product", updateMap);
		
		LOGGER.info("requestMap: " + requestMap.toString());
		
		String requestBody = new JsonObject(requestMap).encode();
		HttpResponse<String> response = Unirest.put(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.body(requestBody).asString();
		
		if (response.getStatus() != 200 && response.getStatus() != 201) {
		    LOGGER.info("data result code:" + response.getStatus());	    	    
		    throw new BadRequestException(SystemError.INVALID_REQUEST);
		}
		
		ShopifyAppService.updateDescShopifyProduct(productId, desc, title);
		Map resultMap = ShopifyAppService.lookup(productId);
		return resultMap;
	}

	private static final Logger LOGGER = Logger.getLogger(ShopifyEditDescriptionHandler.class.getName());
}
