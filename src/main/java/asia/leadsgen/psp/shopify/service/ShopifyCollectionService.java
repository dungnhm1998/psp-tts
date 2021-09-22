package asia.leadsgen.psp.shopify.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.JSONStringToMapUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.vertx.core.json.JsonObject;

public class ShopifyCollectionService {

	public static Map getCollections(String apiKey, String domain, int page, int pageSize) {

		String requestUrl = String.format(ShopifyAPIEndpoints.COLLECTIONS_ALL_USING_TOKEN, domain, page, pageSize);
		HttpResponse<String> shopifyResponse;
		Map response = new LinkedHashMap<>();
		try {
			shopifyResponse = Unirest.get(requestUrl).header("Cache-Control", "no-cache").header("X-Shopify-Access-Token", apiKey).asString();
			JsonObject responseBodyJson = new JsonObject(shopifyResponse.getBody());
			response.put(AppParams.DATA, ParamUtil.getListData(responseBodyJson.getMap(), "custom_collections"));
		} catch (UnirestException e) {
			LOGGER.severe(e.getMessage());
		}

		return response;
	}

	public static Map collectProduct(String apiKey, String domain, String collectionId,
			String productId) throws UnirestException {

		String requestUrl = String.format(ShopifyAPIEndpoints.COLLECTIONS_ALL_USING_TOKEN, domain);

		Map collect = new LinkedHashMap<>();
		collect.put("product_id", productId);
		collect.put("collection_id", collectionId);

		Map requestBodyMap = new LinkedHashMap<>();
		requestBodyMap.put("collect", collect);

		String requestBody = new JsonObject(requestBodyMap).encode();

		HttpResponse<String> shopifyResponse = Unirest.post(requestUrl).header("Cache-Control", "no-cache").header("X-Shopify-Access-Token", apiKey)
				.body(requestBody).asString();

		Map response = new LinkedHashMap<>();
		response.put(AppParams.DATA, JSONStringToMapUtil.toMap(shopifyResponse.getBody()));
		response.put("status", shopifyResponse.getStatus());
		response.put("statusMessage", shopifyResponse.getStatusText());

		return response;
	}

	private static final Logger LOGGER = Logger.getLogger(ShopifyCollectionService.class.getName());
}
