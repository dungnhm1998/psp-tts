package asia.leadsgen.psp.server.handler.shopify_app;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyGetCollectionsHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
			String storeId = routingContext.request().getParam(AppParams.STORE_ID);
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
							
				List<Map> collections = getCollections(consumerKey, domain);
				
				Map result = new HashMap();
				result.put("collections", collections);
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
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
	
	private List<Map> getCollections (String consumerKey, String domain) throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.COLLECTIONS_USING_TOKEN, domain);
		
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
                .asString();	
		
		Map mapResult = new JsonObject(response.getBody()).getMap();
		
		List<Map> collections = ParamUtil.getListData(mapResult, "custom_collections");
		LOGGER.info("collections= " + collections.toString());
		
		return collections.stream().map(o -> {
			Map m = new HashedMap<>();
			m.put(AppParams.ID, ParamUtil.getString(o, AppParams.ID));
			m.put(AppParams.TITLE, ParamUtil.getString(o, AppParams.TITLE));
			return m;
		}).collect(Collectors.toList());
		
	}
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyGetCollectionsHandler.class.getName());

}
