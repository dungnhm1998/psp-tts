package asia.leadsgen.psp.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import io.vertx.core.json.JsonObject;

public class HttpClient {

	/**
	 * 
	 * @param uri
	 * @param method
	 * @param headers
	 * @param params
	 * @param data
	 * @return
	 */

	public static JsonObject sendRequest(String url, String method, Map<String, String> headers,
			Map<String, String> params, Map<String, Object> data) {

		URL obj;
		HttpURLConnection con = null;
		StringBuffer response = new StringBuffer();
		InputStreamReader inputStreamReader;
		BufferedReader bufferedReader;
		int responseCode = 0;
		String message = null;

		method = method.toUpperCase();

		try {

			url = addParams(url, params);
			obj = new URL(url);
			con = (HttpURLConnection) obj.openConnection();

			addHeaders(con, headers);
			con.setRequestMethod(method);
			con.setDoOutput(true);
			con.setDoInput(true);

			if (method.matches("POST|PUT")) {
				con.setRequestProperty("Content-Type", "application/json");
				OutputStreamWriter streamWriter = new OutputStreamWriter(con.getOutputStream());
				streamWriter.write(new JsonObject(data).encode());
				streamWriter.flush();
			}

			responseCode = con.getResponseCode();
			message = con.getResponseMessage();

			if (responseCode == 200 || responseCode == 201) {
				inputStreamReader = new InputStreamReader(con.getInputStream());
				bufferedReader = new BufferedReader(inputStreamReader);
				String inputLine;
				while ((inputLine = bufferedReader.readLine()) != null) {
					response.append(inputLine);
				}
				bufferedReader.close();
			} else {
				response = new StringBuffer("{}");
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		JsonObject res = new JsonObject();
		res.put("code", responseCode);
		res.put("message", message);

		JsonObject resData;
		if (StringUtils.isEmpty(response.toString()) || "{}".equals(response.toString())) {
			resData = new JsonObject();
		} else {
			resData = new JsonObject(response.toString());
		}
		res.put("data", resData);

//		LOGGER.log(Level.INFO, "[RESPONSE]", res.encode());

		return res;
	}

	private static String addParams(String url, Map<String, String> params) {
		StringBuffer sb = new StringBuffer(url);
		if (params != null && params.isEmpty() == false) {
			sb.append("?");
			NavigableMap<String, String> navparams = new TreeMap<>(params);
			for (String key : navparams.navigableKeySet()) {
				sb.append(key).append("=").append(navparams.get(key));
			}
		}
		url = sb.toString();
		if (url.endsWith("=")) {
			url = url.substring(0, url.length() - 1);
		}
//		LOGGER.info("REQUEST URL:" + url);
		return url;
	}

	private static void addHeaders(HttpURLConnection con, Map<String, String> headers) {

		con.addRequestProperty("User-Agent", "Mozilla/4.0");
		if (headers != null && headers.isEmpty() == false) {
			NavigableMap<String, String> navHeader = new TreeMap<>(headers);
			for (String key : navHeader.navigableKeySet()) {
//				LOGGER.info("REQUEST HEADERS:" + key + " | " + navHeader.get(key));
				con.addRequestProperty(key, navHeader.get(key));
			}
		}

	}

	private static final Logger LOGGER = Logger.getLogger(HttpClient.class.getName());

}
