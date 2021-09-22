package asia.leadsgen.psp.server.handler.shopify_app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyGetExitsVariantHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
			Long productId = Long.parseLong(routingContext.request().getParam(AppParams.PRODUCT_ID));
			LOGGER.info("productId:" + productId);
			String baseId = routingContext.request().getParam(AppParams.BASE_ID);
			LOGGER.info("baseId:" + baseId);
			
			try {
				
				List<Map> exitsVariant = ShopifyAppService.getExitsShopifyVariant(productId, baseId);
				Map result = new HashMap();
				result.put("result", exitsVariant);
				
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
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyGetExitsVariantHandler.class.getName());
}
