package asia.leadsgen.psp.server.handler.shopify_app;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
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
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyEditPricesHandler implements Handler<RoutingContext> {

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
				
				Long productRefId = Long.parseLong(ParamUtil.getString(requestBodyMap, AppParams.PRODUCT_REF_ID));
				
				List<Map> variants = ParamUtil.getListData(requestBodyMap, AppParams.VARIANTS);
				if (CollectionUtils.isEmpty(variants)) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
				}
				
				for (Map variantMap : variants) {
					updateToShopifyStore(variantMap, productRefId, consumerKey, domain);
				}
				
				Map resultMap = ShopifyAppService.lookup(productRefId);
				
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
	
	private void updateToShopifyStore(Map variantMap, Long productRefId, String consumerKey, String domain) throws UnirestException, SQLException {
		
		String variantRefId = ParamUtil.getString(variantMap, AppParams.REFERENCE_ID);
		
		double price = ParamUtil.getDouble(variantMap, AppParams.RETAIL_PRICE);
		double compare_at_price = price * 120 / 100;
		
		Map requestMap = new HashMap<>();
		
		Map variant = new LinkedHashMap<>();
		variant.put("id", variantRefId);
		variant.put("price", price);
		variant.put("compare_at_price", compare_at_price);
		
		requestMap.put("variant", variant);
		LOGGER.info("requestMap: " + requestMap.toString());	  
		
		String url = String.format(ShopifyAPIEndpoints.VARIANTS_ONE_USING_TOKEN, domain, variantRefId);
		
		String requestBody = new JsonObject(requestMap).encode();
		HttpResponse<String> response = Unirest.put(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.body(requestBody).asString();
		
		if (response.getStatus() != 200 && response.getStatus() != 201) {
		    LOGGER.info("data result code:" + response.getStatus());	    	    
		    throw new BadRequestException(SystemError.INVALID_REQUEST);
		}
		
		Long variantId = Long.parseLong(variantRefId);
		String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);
		LOGGER.info("productId: " + productId);	 
		String sizeId = ParamUtil.getString(variantMap, AppParams.SIZE_ID);
		LOGGER.info("sizeId: " + sizeId);	
		String salePrice = String.valueOf(price);
		LOGGER.info("salePrice: " + salePrice);	 
		ShopifyAppService.updatePricesProductVariant(variantId, productRefId, productId, sizeId, salePrice);
	}

	private static final Logger LOGGER = Logger.getLogger(ShopifyEditPricesHandler.class.getName());
}
