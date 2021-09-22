package asia.leadsgen.psp.service_fulfill;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.JSONStringToMapUtil;
import io.vertx.core.json.JsonObject;

public class ShopbaseService {
	
	public static Map connectStore(String authorizationCode, String storeName, String apiKey, String secretKey) {
		String url = "https://" + storeName + "." + AppConstants.SHOPBASE_AUTHENTICATION_URL;
		LOGGER.info("ShopbaseService connectStore auth url: " + url);
		Map authentication = new LinkedHashMap<>();
		Map shopbaseAuthMap = new LinkedHashMap<>();
		authentication.put("client_id", apiKey);
		authentication.put("client_secret", secretKey);
		authentication.put("code", authorizationCode);
		
		LOGGER.info("authentication: " + authentication);

		String requestBody = new JsonObject(authentication).encode();
		try {
			HttpResponse<String> response = Unirest.post(url).header("accept", "application/json").header("Content-Type", "application/json").body(requestBody).asString();
			shopbaseAuthMap = JSONStringToMapUtil.toMap(response.getBody());
			int result = response.getStatus();
			if (result != 200) {
				throw new BadRequestException(SystemError.INVALID_AUTH_TOKEN);
			}
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return shopbaseAuthMap;
	}
	
	public static Map getStoreLocation(String domain, String token) {
		String url = String.format(AppConstants.SHOPBASE_STORE_LOCATION, domain);
		
		Map storeLocationMap =  new LinkedHashMap<>();

		try {
			HttpResponse<String> response = Unirest.get(url).header("accept", "application/json").header("Content-Type", "application/json").header("X-ShopBase-Access-Token", token).asString();
			storeLocationMap = JSONStringToMapUtil.toMap(response.getBody());
			int result = response.getStatus();
//			if (result != 200) {
//				throw new BadRequestException(SystemError.INVALID_AUTH_TOKEN);
//			}
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return storeLocationMap;
	}

	
	public static boolean createWebhook(String topic, String urlApi, String storeId, String domain, String apiKey, String address) throws UnirestException {

		Map requestBodyMap = new LinkedHashMap<>();
		Map webhook = new LinkedHashMap<>();
		webhook.put("topic", topic);
		webhook.put("address", urlApi + "/shopbase-store/" + storeId + "/" + address);
		webhook.put("format", "json");
		requestBodyMap.put("webhook", webhook);
		
		String requestBody = new JsonObject(requestBodyMap).encode();
		
		String url = String.format(AppConstants.CREATE_WEBHOOK, domain);
		LOGGER.info("ShopbaseService createWebhook url requestUrl=" + url);
		LOGGER.info("ShopbaseService createWebhook requestBody=" + requestBody);
		HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json").header("X-ShopBase-Access-Token", apiKey).body(requestBody).asString();
		
		LOGGER.info("ShopbaseService response: " + response.getStatus());
		LOGGER.info("ShopbaseService responseBody: " + response.getBody());
		if (response.getStatus() == 201 || response.getStatus() == 200) {
			return true;
		}
		return false;
	}
	
	private static final Logger LOGGER = Logger.getLogger(ShopbaseService.class.getName());

}
