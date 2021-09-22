package asia.leadsgen.psp.server.handler.shopify_app;

import java.sql.SQLException;
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
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyDeleteProductHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		routingContext.vertx().executeBlocking(future -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			LOGGER.info("userId= " + userId);
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
				
				List<String> products = ParamUtil.getListData(requestBodyMap, AppParams.PRODUCTS);
				if (CollectionUtils.isEmpty(products)) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
				}
				
				for (String id : products) {
					Long productId = Long.parseLong(id);
					LOGGER.info("Delete productId: " + productId);
//					deleteProductInShopify(id, consumerKey, domain);
					ShopifyAppService.updateStateProduct(productId, ResourceStates.DELETED);
					
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
	
	public static void deleteProductInShopify(String productId, String consumerKey, String domain) throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.PRODUCT_USING_TOKEN, domain, productId);	
		HttpResponse<String> deleteResponse = Unirest.delete(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.asString();
		LOGGER.info("delete variant response-status:" + deleteResponse.getStatus());
	    LOGGER.info("data result text:" + deleteResponse.getStatusText());
	    LOGGER.info("message: " + deleteResponse.getBody());
	}
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyDeleteProductHandler.class.getName());
}
