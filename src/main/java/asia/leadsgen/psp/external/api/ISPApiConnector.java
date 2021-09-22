package asia.leadsgen.psp.external.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import asia.leadsgen.psp.obj.LabelFileObj;
import asia.leadsgen.psp.server.handler.dropship.order.CheckDesignsResponse;
import asia.leadsgen.psp.util.AppParams;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ISPApiConnector {

	private static String ispAPIPrefix;

	public static String zipLabels(List<LabelFileObj> labelFiles) {

		HttpURLConnection httpConnection = null;
		StringBuffer responseBodyBuffer = new StringBuffer();

		JsonObject response = null;
		String zipUrl = null;
		try {

			String requestURL = ispAPIPrefix + "/download-label";

			JsonObject ispRequest = new JsonObject();
			JsonArray files = new JsonArray();
			for (LabelFileObj file : labelFiles) {
				JsonObject obj = new JsonObject();
				obj.put("name", file.getFileName());
				obj.put("url", file.getFileUrl());
				files.add(obj);
			}
			ispRequest.put("files", files);
			ispRequest.put("zip", "1");

			URL url = new URL(requestURL);

			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setDoOutput(true);
			httpConnection.setDoInput(true);
			httpConnection.setRequestMethod(HttpMethod.POST.name());

			httpConnection.setRequestProperty("Accept", "application/json");
			httpConnection.setRequestProperty("Content-Type", "application/json");

			OutputStreamWriter streamWriter = new OutputStreamWriter(httpConnection.getOutputStream());
			streamWriter.write(ispRequest.encode());
			streamWriter.flush();

			int responseCode = httpConnection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				InputStreamReader inputStreamReader = new InputStreamReader(httpConnection.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String inputLine;
				while ((inputLine = bufferedReader.readLine()) != null) {
					responseBodyBuffer.append(inputLine);
				}
				bufferedReader.close();
				LOGGER.info("[RESPONSE] BODY: " + responseBodyBuffer.toString());
				response = new JsonObject(responseBodyBuffer.toString());
				zipUrl = response.getString(AppParams.URL);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}
		return zipUrl;
	}

	public static CheckDesignsResponse checkDesignUrls(String userId, String baseId, String frontDesignUrl,
			String backDesignUrl) {

		HttpURLConnection httpConnection = null;
		StringBuffer responseBodyBuffer = new StringBuffer();
		CheckDesignsResponse response = null;
		try {

			String requestURL = ispAPIPrefix + "/design-custom";

			JsonObject ispRequest = new JsonObject();
			ispRequest.put(AppParams.USER_ID, userId);
			ispRequest.put(AppParams.BASE_ID, baseId);
			ispRequest.put(AppParams.DESIGN_FRONT_URL, frontDesignUrl);
			ispRequest.put(AppParams.DESIGN_BACK_URL, backDesignUrl);
			URL url = new URL(requestURL);

			httpConnection = createHTTPConnection(url);

			LOGGER.info("checkDesignUrls()- [REQUEST] BODY: " + ispRequest.encode());
			OutputStreamWriter streamWriter = new OutputStreamWriter(httpConnection.getOutputStream());
			streamWriter.write(ispRequest.encode());
			streamWriter.flush();

			int responseCode = httpConnection.getResponseCode();
			LOGGER.info("responseCode= " + responseCode);
			LOGGER.info("responseMessage= " + httpConnection.getResponseMessage());
			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				String responseString = getResponseString(httpConnection, responseBodyBuffer);
				LOGGER.info("responseString= " + responseString);
				response = new Gson().fromJson(responseString, CheckDesignsResponse.class);

			} else {
				response = new CheckDesignsResponse();
				response.setIsValid(false);
				response.setCode(responseCode);
				response.setMessage("Download design failed.");
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}
		return response;
	}

	public static boolean checkDesignUrl(String user_id, String base_id, String design_front_url,
			String design_back_url) {

		HttpURLConnection httpConnection = null;
		StringBuffer responseBodyBuffer = new StringBuffer();

		boolean result = false;
		try {

			String requestURL = ispAPIPrefix + "/design-custom";

			JsonObject ispRequest = new JsonObject();
			ispRequest.put(AppParams.USER_ID, user_id);
			ispRequest.put(AppParams.BASE_ID, base_id);
			ispRequest.put(AppParams.DESIGN_FRONT_URL, design_front_url);
			ispRequest.put(AppParams.DESIGN_BACK_URL, design_back_url);

			URL url = new URL(requestURL);

			httpConnection = createHTTPConnection(url);

			OutputStreamWriter streamWriter = new OutputStreamWriter(httpConnection.getOutputStream());
			streamWriter.write(ispRequest.encode());
			streamWriter.flush();

			int responseCode = httpConnection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				JsonObject objResponse = getResponseURL(httpConnection, responseBodyBuffer);
				result = objResponse.getBoolean("is_valid");
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}
		return result;
	}

	private static String getResponseString(HttpURLConnection httpConnection, StringBuffer responseBodyBuffer)
			throws IOException {
		InputStreamReader inputStreamReader = new InputStreamReader(httpConnection.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		String inputLine;
		while ((inputLine = bufferedReader.readLine()) != null) {
			responseBodyBuffer.append(inputLine);
		}
		bufferedReader.close();
		return responseBodyBuffer.toString();
	}

	private static JsonObject getResponseURL(HttpURLConnection httpConnection, StringBuffer responseBodyBuffer)
			throws IOException {
		return new JsonObject(getResponseString(httpConnection, responseBodyBuffer));
	}

	private static HttpURLConnection createHTTPConnection(URL url) throws IOException, ProtocolException {
		HttpURLConnection httpConnection;
		httpConnection = (HttpURLConnection) url.openConnection();
		httpConnection.setDoOutput(true);
		httpConnection.setDoInput(true);
		httpConnection.setRequestMethod(HttpMethod.POST.name());

		httpConnection.setRequestProperty("Accept", "application/json");
		httpConnection.setRequestProperty("Content-Type", "application/json");
		return httpConnection;
	}

	public static String getIspAPIPrefix() {
		return ispAPIPrefix;
	}

	public static void setIspAPIPrefix(String ispAPIPrefix) {
		ISPApiConnector.ispAPIPrefix = ispAPIPrefix;
	}

	private static final Logger LOGGER = Logger.getLogger(ISPApiConnector.class.getName());

}
