package asia.leadsgen.psp.server.handler.dropship.store;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.server.handler.dropship.collection.DropshipCollectionSearchHandler;
import asia.leadsgen.psp.server.handler.etsy.EtsyApi;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.EtsyService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;

public class DropshipStoreReconnectHanlder implements Handler<RoutingContext> {

	private static String shopifyApiKey;
	private static String shopifyScope;
	private static String shopifyRedirectUrl;
	
	private static String wooAppName;
	private static String wooReturnUrl;
	private static String wooCallBackUrl;
	
	private static String etsyConsumerKey;
	private static String etsyConsumerSecret;
	private static String etsyRedirectUrl;
	
	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			try {
				
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				if (StringUtils.isEmpty(userId)) {
					throw new LoginException(SystemError.LOGIN_REQUIRED);
				}		
				
				String storeId = routingContext.request().params().get(AppParams.ID);

				Map store = DropShipStoreService.getStoreByIdAndState(storeId, ResourceStates.DISCONNECTED);
				
				if (store == null) {
					throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
				}
				
				int responseCode = HttpResponseStatus.OK.code();
				String responseMsg = HttpResponseStatus.OK.reasonPhrase();
				Map responseData = new LinkedHashMap<>();
				
				String storeName = ParamUtil.getString(store, AppParams.NAME);
				String channel = ParamUtil.getString(store, AppParams.CHANNEL);
				
				if (AppConstants.SHOPIFY.equalsIgnoreCase(channel)) {
					
					String apiKey = ParamUtil.getString(store, AppParams.API_KEY);
					String checkingRequest = "https://" + storeName + "." + AppConstants.SHOPIFY_DOMAIN + "/admin/shop.json";
					
					HttpResponse<String> apiResponse = Unirest.get(checkingRequest).header("Cache-Control", "no-cache").header("X-Shopify-Access-Token", apiKey).asString();
					LOGGER.info("connect to shopify store response: " + apiResponse.toString());
					
					if (apiResponse.getStatus() == HttpResponseStatus.OK.code()) {
						LOGGER.info("connect to shopify store " + storeName + " success");
						DropShipStoreService.updateStateAndNConnect(storeId, ResourceStates.APPROVED, 1);
					} else {
						String url = "https://" + storeName + ".myshopify.com/admin/oauth/authorize?client_id=" + shopifyApiKey
								+ "&amp;scope=" + shopifyScope + "&amp;redirect_uri="
								+ shopifyRedirectUrl;
						responseCode = 333;
						responseMsg = HttpResponseStatus.MOVED_PERMANENTLY.reasonPhrase();
						responseData.put(AppParams.URL, url);
					}
					
				} else if (AppConstants.WOOCOMMERCE.equalsIgnoreCase(channel)) {
					
					String wooDomain = ParamUtil.getString(store, AppParams.DOMAIN);
					String consumerKey = ParamUtil.getString(store, AppParams.API_KEY);
					String consumerSecret = ParamUtil.getString(store, AppParams.SECRET);
					String checkingRequest = wooDomain  + "/wp-json/wc/v3/orders?consumer_key=" + consumerKey + "&consumer_secret=" + consumerSecret;
					
					HttpResponse<String> apiResponse = Unirest.get(checkingRequest).asString();
					LOGGER.info("connect to woocommerce store response: " + apiResponse.toString());
					
					if (apiResponse.getStatus() == HttpResponseStatus.OK.code()) {
						LOGGER.info("connect to woocommerce store " + storeName + " success");
						DropShipStoreService.updateStateAndNConnect(storeId, ResourceStates.APPROVED, 1);
					} else {
						LOGGER.info("connect to woocommerce store " + storeName + " not success ");
						String id = ParamUtil.getString(store, AppParams.ID);
						String url = wooDomain + "/wc-auth/v1/authorize?app_name=" + wooAppName + "&scope=read_write&user_id=" + id
								+ "&return_url=" + wooReturnUrl + "&callback_url=" + wooCallBackUrl;
						responseCode = 333;
						responseMsg = HttpResponseStatus.MOVED_PERMANENTLY.reasonPhrase();
						responseData.put(AppParams.URL, url);
					}
					
				} else if (AppConstants.ETSY.equalsIgnoreCase(channel)) {

					OAuth10aService service = new ServiceBuilder().apiKey(etsyConsumerKey).apiSecret(etsyConsumerSecret).build(EtsyApi.instance());
					OAuthConsumer consumer = new DefaultOAuthConsumer(etsyConsumerKey, etsyConsumerSecret);
					
					String token = ParamUtil.getString(store, AppParams.API_KEY);
					String tokenSecret = ParamUtil.getString(store, AppParams.SECRET);
					
					OAuth1AccessToken accessToken = new OAuth1AccessToken(token, tokenSecret);
					
					OAuthRequest callRequest = new OAuthRequest(Verb.GET, "https://openapi.etsy.com/v2/users/__SELF__", service);
					service.signRequest(accessToken, callRequest);
					
					Response resp = callRequest.send();
					
					if (resp.isSuccessful()) {
						LOGGER.info("connect to etsy store " + storeName + " success");
						DropShipStoreService.updateStateAndNConnect(storeId, ResourceStates.APPROVED, 1);
					} else {
						OAuthProvider provider = new DefaultOAuthProvider(
					            "https://openapi.etsy.com/v2/oauth/request_token",
					            "https://openapi.etsy.com/v2/oauth/access_token",
					            "https://www.etsy.com/oauth/signin");
						String url = provider.retrieveRequestToken(consumer, etsyRedirectUrl);
						responseCode = 333;
						responseMsg = HttpResponseStatus.MOVED_PERMANENTLY.reasonPhrase();
						responseData.put(AppParams.URL, url);
					}
					
				} else {
					
				}
				
				routingContext.put(AppParams.RESPONSE_CODE, responseCode);
				routingContext.put(AppParams.RESPONSE_MSG, responseMsg);
				routingContext.put(AppParams.RESPONSE_DATA, responseData);

			} catch (Exception e) {
				routingContext.fail(e);
			}

			future.complete();

		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}
	
	public void setShopifyApiKey(String apiKey) {
		DropshipStoreReconnectHanlder.shopifyApiKey = apiKey;
	}

	public void setShopifyScope(String shopifyScope) {
		DropshipStoreReconnectHanlder.shopifyScope = shopifyScope;
	}
	
	public void setShopifyRedirectUrl(String shopifyRedirectUrl) {
		DropshipStoreReconnectHanlder.shopifyRedirectUrl = shopifyRedirectUrl;
	}
	
	public void setWooAppName(String wooAppName) {
		DropshipStoreReconnectHanlder.wooAppName = wooAppName;
	}

	public void setWooReturnUrl(String wooReturnUrl) {
		DropshipStoreReconnectHanlder.wooReturnUrl = wooReturnUrl;
	}
	
	public void setWooCallBackUrl(String wooCallBackUrl) {
		DropshipStoreReconnectHanlder.wooCallBackUrl = wooCallBackUrl;
	}
	
	public void setEtsyConsumerKey(String etsyConsumerKey) {
		DropshipStoreReconnectHanlder.etsyConsumerKey = etsyConsumerKey;
	}

	public void setEtsyConsumerSecret(String etsyConsumerSecret) {
		DropshipStoreReconnectHanlder.etsyConsumerSecret = etsyConsumerSecret;
	}
	
	public void setEtsyRedirectUrl(String etsyRedirectUrl) {
		DropshipStoreReconnectHanlder.etsyRedirectUrl = etsyRedirectUrl;
	}
	
	private static final Logger LOGGER = Logger.getLogger(DropshipCollectionSearchHandler.class.getName());
	
}
