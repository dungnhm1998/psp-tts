package asia.leadsgen.psp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import asia.leadsgen.psp.obj.ISPAllOverV1Request;
import asia.leadsgen.psp.obj.Image;
import asia.leadsgen.psp.obj.ImgDirectory;
import asia.leadsgen.psp.obj.ImgFile;
import asia.leadsgen.psp.obj.LabelFile;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ISPUtil {

	private static String ispAPIPrefix;
	private static String ispBaiduAPIPrefix;

	public static String zipLabels(List<LabelFile> labelFiles) {

		HttpURLConnection httpConnection = null;
		StringBuffer responseBodyBuffer = new StringBuffer();

		JsonObject response = null;
		String zipUrl = null;
		try {

			String requestURL = ispAPIPrefix + "/download-label";

			JsonObject ispRequest = new JsonObject();
			JsonArray files = new JsonArray();
			for (LabelFile file : labelFiles) {
				JsonObject obj = new JsonObject();
				obj.put("name", file.getFileName());
				obj.put("url", file.getFileUrl());
				files.add(obj);
			}
			ispRequest.put("files", files);
			ispRequest.put("zip", "0");

			URL url = new URL(requestURL);

			httpConnection = createHTTPConnection(url);

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

	public static String adjustDesign(Image image, Boolean isLeftChest) {

		HttpURLConnection httpConnection = null;
		StringBuffer responseBodyBuffer = new StringBuffer();

		String responseUrl = null;
		try {

			String requestURL = ispAPIPrefix + "/adjust-design";

			JsonObject ispRequest = new JsonObject();
			ispRequest.put(AppParams.IMAGE, image.toJsonObject());
			ispRequest.put(AppParams.LEFT_CHEST, isLeftChest);

			if(image.getCustomData() != null && !image.getCustomData().isEmpty()) {
				ispRequest.mergeIn(image.getCustomData());
			}
			LOGGER.info("adjustDesign()- [REQUEST] BODY: " + ispRequest.encode());
			URL url = new URL(requestURL);

			httpConnection = createHTTPConnection(url);

			OutputStreamWriter streamWriter = new OutputStreamWriter(httpConnection.getOutputStream());
			streamWriter.write(ispRequest.encode());
			streamWriter.flush();

			int responseCode = httpConnection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				responseUrl = getResponseURL(httpConnection, responseBodyBuffer);
				LOGGER.info("adjustDesign()-[RESPONSE] BODY: " + responseBodyBuffer.toString());
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}
		return responseUrl;
	}

	private static String getResponseURL(HttpURLConnection httpConnection, StringBuffer responseBodyBuffer)
			throws IOException {
		JsonObject response;
		String responseUrl;
		InputStreamReader inputStreamReader = new InputStreamReader(httpConnection.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		String inputLine;
		while ((inputLine = bufferedReader.readLine()) != null) {
			responseBodyBuffer.append(inputLine);
		}
		bufferedReader.close();
		response = new JsonObject(responseBodyBuffer.toString());
		responseUrl = response.getString(AppParams.URL);
		return responseUrl;
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

	public static String adjustAccessoriesDesign(Image image, String type, String baseId) {

		HttpURLConnection httpConnection = null;
		StringBuffer responseBodyBuffer = new StringBuffer();

		JsonObject response = null;
		String responseUrl = null;
		try {

			String requestURL = ispAPIPrefix + "/adjust-design-accessory";

			JsonObject imageJson = image.toJsonObject(type);
			imageJson.put(AppParams.BASE_ID, baseId);
			
			JsonObject ispRequest = new JsonObject();
			ispRequest.put(AppParams.IMAGE, imageJson);
			
			if(image.getCustomData() != null && !image.getCustomData().isEmpty()) {
				ispRequest.mergeIn(image.getCustomData());
			}

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
			LOGGER.info("adjustAccessoriesDesign()- [REQUEST] BODY: " + ispRequest.encode());
			int responseCode = httpConnection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				InputStreamReader inputStreamReader = new InputStreamReader(httpConnection.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String inputLine;
				while ((inputLine = bufferedReader.readLine()) != null) {
					responseBodyBuffer.append(inputLine);
				}
				bufferedReader.close();
				LOGGER.info("adjustAccessoriesDesign()- [RESPONSE] BODY: " + responseBodyBuffer.toString());
				response = new JsonObject(responseBodyBuffer.toString());
				responseUrl = response.getString(AppParams.URL);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}
		return responseUrl;
	}
	
	public static CheckDesignsResponse adjustLeeCowLeatherDesign(String url_design, String baseId, String colorId) {

		HttpURLConnection httpConnection = null;
		StringBuffer responseBodyBuffer = new StringBuffer();

		CheckDesignsResponse response = null;
		try {

			String requestURL = ispAPIPrefix + "/adjust-design-leather";
			
			JsonObject ispRequest = new JsonObject();
			ispRequest.put(AppParams.URL, url_design);
			ispRequest.put(AppParams.BASE_ID, baseId);
			ispRequest.put(AppParams.COLOR_ID, colorId);

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
			LOGGER.info("adjustLeeCowLeatherDesign()- [REQUEST] BODY: " + ispRequest.encode());
			int responseCode = httpConnection.getResponseCode();
			LOGGER.info("adjustLeeCowLeatherDesign()- [RESPONSE] responseCode: " + responseCode);
			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				InputStreamReader inputStreamReader = new InputStreamReader(httpConnection.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String inputLine;
				while ((inputLine = bufferedReader.readLine()) != null) {
					responseBodyBuffer.append(inputLine);
				}
				bufferedReader.close();
				LOGGER.info("adjustLeeCowLeatherDesign()- [RESPONSE] BODY: " + responseBodyBuffer.toString());
				response = new Gson().fromJson(responseBodyBuffer.toString(), CheckDesignsResponse.class);
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

	public static int downloadImages(ArrayList<ImgFile> imgFiles) throws IOException {

		ArrayList<ImgDirectory> directories = new ArrayList<>();
		if (imgFiles != null && imgFiles.isEmpty() == false) {
			Set<String> dirList = imgFiles.stream().map(o -> o.getDirectory()).collect(Collectors.toSet());

			ArrayList<ImgFile> subfiles;
			for (String dirName : dirList) {
				subfiles = new ArrayList<ImgFile>();
				for (ImgFile imgfile : imgFiles) {
					if (dirName.equals(imgfile.getDirectory())) {
						subfiles.add(imgfile);
					}
				}
				directories.add(new ImgDirectory(dirName, subfiles));
			}
		}

		JsonObject requestBody = new JsonObject().mapFrom(new ISPAllOverV1Request(directories));

		HttpURLConnection httpConnection = null;
		int code = 0;
		try {
			String requestURL = ispBaiduAPIPrefix;
			LOGGER.info("[**BAIDU**] URL " + requestURL);
			LOGGER.info("[**BAIDU**] " + requestBody.toString());
			URL url = new URL(requestURL);

			httpConnection = createHTTPConnection(url);

			OutputStreamWriter streamWriter = new OutputStreamWriter(httpConnection.getOutputStream());

			streamWriter.write(requestBody.encode());
			streamWriter.flush();

			code = httpConnection.getResponseCode();

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}
		return code;
	}

	public static String getIspAPIPrefix() {
		return ispAPIPrefix;
	}

	public static void setIspAPIPrefix(String ispAPIPrefix) {
		ISPUtil.ispAPIPrefix = ispAPIPrefix;
	}

	public static String getIspBaiduAPIPrefix() {
		return ispBaiduAPIPrefix;
	}

	public static void setIspBaiduAPIPrefix(String ispBaiduAPIPrefix) {
		ISPUtil.ispBaiduAPIPrefix = ispBaiduAPIPrefix;
	}

	private static final Logger LOGGER = Logger.getLogger(ISPUtil.class.getName());

}
