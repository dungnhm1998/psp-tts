package asia.leadsgen.psp.shopify.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.vertx.core.json.JsonObject;

public class ShopifyFulfillmentService {

	public static Map updateFulfillment(String domain, String token, String orderId, String location,
			String trackingNumber, String trackingUrl, String itemId) {

		Map requestBodyMap = new LinkedHashMap<>();
		Map fulfillment = new LinkedHashMap<>();
		fulfillment.put("location_id", location);
		fulfillment.put("tracking_number", trackingNumber);
		fulfillment.put("tracking_url", trackingUrl);

		List<Map> lineItems = new ArrayList<>();
		Map lineItem = new LinkedHashMap<>();
		lineItem.put("id", itemId);
		lineItems.add(lineItem);

		fulfillment.put("line_items", lineItems);
		requestBodyMap.put("fulfillment", fulfillment);

		String requestUrl = String.format(ShopifyAPIEndpoints.FULFILLMENT_ALL_USING_TOKEN, domain, orderId);
		LOGGER.info("request url=" + requestUrl);

		LOGGER.info("request payload=" + requestBodyMap.toString());
		String requestBodyJson = new JsonObject(requestBodyMap).encode();

		Map response = new LinkedHashMap<>();
		HttpResponse<String> shopifyResponse;
		try {
			shopifyResponse = Unirest.post(requestUrl).header("Cache-Control", "no-cache")
					.header("X-Shopify-Access-Token", token).body(requestBodyJson).asString();
			JsonObject responseBodyJson = new JsonObject(shopifyResponse.getBody());
			response.put(AppParams.RESPONSE_CODE, shopifyResponse.getStatus());
			response.put(AppParams.RESPONSE_MSG, shopifyResponse.getStatusText());
			response.put(AppParams.FULFILLMENT, ParamUtil.getMapData(responseBodyJson.getMap(), AppParams.FULFILLMENT));
			LOGGER.info("shopify payload="
					+ ParamUtil.getMapData(responseBodyJson.getMap(), AppParams.FULFILLMENT).toString());
		} catch (UnirestException e) {
			LOGGER.severe(e.getMessage());
		}

		return response;
	}

	private static final Logger LOGGER = Logger.getLogger(ShopifyFulfillmentService.class.getName());

}
