package asia.leadsgen.psp.service_fulfill;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.util.AppParams;
import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONArray;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.StoreOptionObj;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 *
 * @author HIEPHV
 */
public class WooService {

	public static JSONArray OrderPullData(String domain, String consumer_key, String consumer_secret, String page, int offset, String perPage, String status, String fromDate, String toDate, String ids) throws UnirestException, IOException {
		String formatUrl = MessageFormat.format("{0}/wp-json/wc/v3/orders?consumer_key={1}&consumer_secret={2}", domain, consumer_key, consumer_secret);

		HttpResponse<String> response = Unirest.get(formatUrl).header("Content-Type", "application/json").queryString("page", page).queryString("offset", offset <= 0 ? 1
				: offset).queryString("per_page", perPage).queryString("status", status).queryString("after", fromDate).queryString("before", toDate).queryString("include", ids).asString();

		if (response.getStatus() != 200) {

			throw new BadRequestException(SystemError.INVALID_REQUEST);
		}
		JSONArray orderArray = new JSONArray(response.getBody());
		return orderArray;

	}

	public static Map GetOrderById(String domain, String consumer_key, String consumer_secret, String id) throws UnirestException {

		String formatUrl = MessageFormat.format("{0}/wp-json/wc/v3/orders/{1}?consumer_key={2}&consumer_secret={3}", domain, id + "", consumer_key, consumer_secret);
		HttpResponse<String> response = Unirest.get(formatUrl).header("Content-Type", "application/json").asString();

		Map result = new LinkedHashMap<>();
		if (response.getStatus() == 200) {
			result = new JsonObject(response.getBody()).getMap();

		}
		return result;

	}

	public static JsonArray getListAttribute(String domain, String consumer_key, String consumer_secret) throws UnirestException {
		JsonArray listAttribute = new JsonArray();
		String formatUrl = MessageFormat.format("{0}/wp-json/wc/v3/products/attributes?consumer_key={1}&consumer_secret={2}", domain,consumer_key,consumer_secret);
		HttpResponse response = Unirest.get(formatUrl).header("Content-Type", "application/json").asString();
		if (response.getStatus() == 200) {
			listAttribute = new JsonArray(response.getBody().toString());
		}
		return listAttribute;
	}

	public static JsonArray getListTag(String domain, String consumer_key, String consumer_secret) throws UnirestException {
		JsonArray listAttribute = new JsonArray();
		String formatUrl = MessageFormat.format("{0}/wp-json/wc/v3/products/tags", domain);
		HttpResponse response = Unirest.get(formatUrl).basicAuth(consumer_key, consumer_secret).header("Content-Type", "application/json").asString();
		if (response.getStatus() == 200) {
			listAttribute = new JsonArray(response.getBody().toString());
		}
		return listAttribute;
	}
	
	public static boolean checkStoreIsMapped(String storeId) throws UnirestException, SQLException {
		List<Map> listOption = StoreOptionService.lookUp(storeId, AppParams.ATTRIBUTE);
		if (CollectionUtils.isEmpty(listOption)) {
			return false;
		} else {
			return true;
		}
	}
	
	private static final Logger LOGGER = Logger.getLogger(WooService.class.getName());
}
