package asia.leadsgen.psp.shopify.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.util.JSONStringToMapUtil;
import asia.leadsgen.psp.util.ParamUtil;

public class ShopifyAPISetupCheck {

	public static Map checkProductsEndpoints(String apiKey, String apiSecret, String domain) throws UnirestException {

		String requestUrl = String.format(ShopifyAPIEndpoints.PRODUCTS_ALL_USING_TOKEN, domain);
		HttpResponse<String> response = Unirest.post(requestUrl).header("Content-Type", "application/json")
				.header("X-Shopify-Access-Token", apiKey).body(
				"{\n  \"product\": {\n    \"title\": \"John Snow\",\n    \"body_html\": \"<strong>You know nothing, John Snow.</strong>\",\n    \"vendor\": \"HBO\",\n    \"product_type\": \"Setup Test\",\n    \"published\": false\n  }\n}")
				.asString();
		
		Map responseMap = response.getStatus() == 201 ?   JSONStringToMapUtil.toMap(response.getBody()) : new LinkedHashMap<>();

		Map resultData = new LinkedHashMap<>();
		resultData.put("access", response.getStatus() == 201);
		resultData.put("errors", ParamUtil.getString(responseMap, "errors"));

		return resultData;

	}

	public static Map checkReadOrders(String apiKey, String apiSecret, String domain) throws UnirestException {

		String requestUrl = String.format(ShopifyAPIEndpoints.ORDERS_USING_TOKEN, domain);
		HttpResponse<String> response = Unirest.get(requestUrl).header("X-Shopify-Access-Token", apiKey).asString();
		Map responseMap = JSONStringToMapUtil.toMap(response.getBody());

		Map resultData = new LinkedHashMap<>();
		resultData.put("access", response.getStatus() == 200);
		resultData.put("errors", ParamUtil.getString(responseMap, "errors"));

		return resultData;
	}

}
