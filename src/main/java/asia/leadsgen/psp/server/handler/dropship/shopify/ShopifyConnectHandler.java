package asia.leadsgen.psp.server.handler.dropship.shopify;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.util.AppParams;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyConnectHandler implements Handler<RoutingContext> {

	private static String apiKey;
	private static String redirectUrl;
	private static String scope;
	
	public void setApiKey(String apiKey) {
		ShopifyConnectHandler.apiKey = apiKey;
	}

	public void setRedirectUrl(String redirectUrl) {
		ShopifyConnectHandler.redirectUrl = redirectUrl;
	}
	
	public void setScope(String scope) {
		ShopifyConnectHandler.scope = scope;
	}

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {
				
				String storeName = routingContext.request().params().get(AppParams.STORE_NAME);
				String url = "https://" + storeName + ".myshopify.com/admin/oauth/authorize?client_id=" + apiKey
						+ "&amp;scope=" + scope + "&amp;redirect_uri="
						+ redirectUrl;
				
				Map responseMap = new LinkedHashMap<>();
				responseMap.put(AppParams.URL, url);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, responseMap);

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

	private static final Logger LOGGER = Logger.getLogger(ShopifyConnectHandler.class.getName());
}
