package asia.leadsgen.psp.external.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.obj.Address;
import asia.leadsgen.psp.obj.CMSCreateLabelResult;
import asia.leadsgen.psp.obj.FulfillmentDetail;
import asia.leadsgen.psp.obj.LabelFileObj;
import asia.leadsgen.psp.obj.Shipping;
import asia.leadsgen.psp.obj.ShippingOwnerObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.CharPool;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SSPApiConnector {

	private static String sspAPIPrefix;

	public static CMSCreateLabelResult createLabel(FulfillmentDetail detail, ShippingOwnerObj shippingOwner) {

		CMSCreateLabelResult result = new CMSCreateLabelResult();

		HttpURLConnection httpConnection = null;
		StringBuffer responseBodyBuffer = new StringBuffer();

		JsonObject response = null;

		try {

			String requestURL = sspAPIPrefix + "/shipments";

			JsonObject sspRequestBody = createSSPRequestBody(detail, shippingOwner);

			URL url = new URL(requestURL);

			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setDoOutput(true);
			httpConnection.setDoInput(true);
			httpConnection.setRequestMethod(HttpMethod.POST.name());

			httpConnection.setRequestProperty("Accept", "application/json");
			httpConnection.setRequestProperty("Content-Type", "application/json");

			OutputStreamWriter streamWriter = new OutputStreamWriter(httpConnection.getOutputStream());
			streamWriter.write(sspRequestBody.encode());
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

				result.setSuccess(true);
				result.setId(response.getString("id"));
				result.setCarrier(response.getString("carrier"));
				result.setTrackingCode(response.getString("tracking_code"));
				result.setTrackingUrl(response.getString("tracking_url"));
				result.setUrl(response.getString("url"));

				String customerName = detail.getShipping().getName().trim().replace(CharPool.SPACE, CharPool.DASH);
				String campTitle = detail.getCampaignTitle().replace(CharPool.SPACE, CharPool.DASH);
				String fileName = String.format("%s-%s-%s-%s", campTitle, detail.getFulfillmentId(), customerName,
						detail.getId());
				LabelFileObj laFileObj = new LabelFileObj(AppUtil.generateFriendlyText(fileName),
						response.getString("url"));

				result.setFfDetailId(detail.getId());
				result.setLabelFile(laFileObj);

			} else {
				result.setSuccess(false);
			}
		} catch (Exception e) {

			LOGGER.log(Level.SEVERE, "[ERROR]", e);
			result.setSuccess(false);

		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}

		LOGGER.info(result.toString());

		return result;
	}

	private static JsonObject createSSPRequestBody(FulfillmentDetail detail, ShippingOwnerObj shippingOwner)
			throws SQLException {
		JsonObject sspRequestBody = new JsonObject();
		JsonObject fromAdd = new JsonObject();
		fromAdd.put(AppParams.LINE1, shippingOwner.getAddLine1());
		fromAdd.put(AppParams.LINE2, shippingOwner.getAddLine2());
		fromAdd.put(AppParams.CITY, shippingOwner.getCity());
		fromAdd.put(AppParams.STATE, shippingOwner.getAddState());
		fromAdd.put(AppParams.POSTAL_CODE, shippingOwner.getPostalCode());
		fromAdd.put(AppParams.COUNTRY, shippingOwner.getCountry());
		fromAdd.put(AppParams.PHONE, shippingOwner.getPhone());
		fromAdd.put(AppParams.COMPANY, shippingOwner.getCompany());

		JsonObject toAdd = new JsonObject();
		Shipping shippingTo = detail.getShipping();
		toAdd.put(AppParams.NAME, shippingTo.getName());
		toAdd.put(AppParams.LINE1, shippingTo.getLine1());
		toAdd.put(AppParams.LINE2, shippingTo.getLine2());
		toAdd.put(AppParams.CITY, shippingTo.getCity());
		toAdd.put(AppParams.STATE, shippingTo.getState());
		toAdd.put(AppParams.POSTAL_CODE, shippingTo.getPostalCode());
		toAdd.put(AppParams.COUNTRY, shippingTo.getCountryCode());
		toAdd.put(AppParams.PHONE, shippingTo.getPhone());

		JsonObject parcel = new JsonObject();
//		double parcelWeight = detail.getQuantity()
//				* ShippingService.getByBaseAndSize(detail.getBaseId(), detail.getSize()).getValue();

		double parcelWeight = detail.getPackageWeight();
		if (parcelWeight > 13.0) {
			parcelWeight = 13.0;
		}
		parcel.put("weight", parcelWeight + "");
		parcel.put("predefined_package", "");

		JsonArray items = new JsonArray();
		JsonObject item = new JsonObject();
		item.put("description", detail.getProductName());
		item.put("quantity", detail.getQuantity() + "");
		item.put("value", detail.getShippingValue());
		item.put("weight", parcelWeight + "");
		item.put("country", detail.getShipping().getCountryCode());
		item.put("hs_tariff_number", detail.getTariffNumber());
		items.add(item);

		JsonObject options = new JsonObject();
		options.put("print_custom_1",
				String.format("%s - %s - %s", detail.getProductName(), detail.getSize(), detail.getQuantity()));
		options.put("print_custom_2", String.format("%s-%s", detail.getCampaignTitle(), detail.getFulfillmentId()));
		options.put("print_custom_3", "");

		sspRequestBody.put("from_addr", fromAdd);
		sspRequestBody.put("to_addr", toAdd);
		sspRequestBody.put("parcel", parcel);
		sspRequestBody.put("items", items);
		sspRequestBody.put("options", options);
		return sspRequestBody;
	}

	public static JsonObject verifyAddress(Address address) {

		HttpURLConnection httpConnection = null;
		StringBuffer responseBodyBuffer = new StringBuffer();

		JsonObject response = null;

		try {

			String requestURL = sspAPIPrefix + "/addresses";

			JsonObject sspRequestBody = new JsonObject(address.toMap());

			LOGGER.info("[address]" + address.toMap());

			URL url = new URL(requestURL);

			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setDoOutput(true);
			httpConnection.setDoInput(true);
			httpConnection.setRequestMethod(HttpMethod.POST.name());

			httpConnection.setRequestProperty("Accept", "application/json");
			httpConnection.setRequestProperty("Content-Type", "application/json");

			OutputStreamWriter streamWriter = new OutputStreamWriter(httpConnection.getOutputStream());
			streamWriter.write(sspRequestBody.encode());
			streamWriter.flush();

			int responseCode = httpConnection.getResponseCode();
			LOGGER.info("[RESPONSE] CODE = " + responseCode);

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

			} else {
				InputStreamReader inputStreamReader = new InputStreamReader(httpConnection.getErrorStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String inputLine;
				while ((inputLine = bufferedReader.readLine()) != null) {
					responseBodyBuffer.append(inputLine);
				}
				bufferedReader.close();
				LOGGER.info("[RESPONSE] BODY: " + responseBodyBuffer.toString());
				response = new JsonObject(responseBodyBuffer.toString());
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

	public static String getSspAPIPrefix() {
		return sspAPIPrefix;
	}

	public static void setSspAPIPrefix(String sspAPIPrefix) {
		SSPApiConnector.sspAPIPrefix = sspAPIPrefix;
	}

	private static final Logger LOGGER = Logger.getLogger(SSPApiConnector.class.getName());

}
