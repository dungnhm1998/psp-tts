package asia.leadsgen.psp.external.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.json.JsonObject;

public class HttpRequestUtil {

	public static JsonObject sendGet(String url) {
		URL obj;
		HttpURLConnection con = null;
		StringBuffer response = new StringBuffer();

		try {
			obj = new URL(url);
			con = (HttpURLConnection) obj.openConnection();
			con.addRequestProperty("User-Agent", "Mozilla/4.0");
			int responseCode = con.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				InputStreamReader inputStreamReader = new InputStreamReader(con.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String inputLine;
				while ((inputLine = bufferedReader.readLine()) != null) {
					response.append(inputLine);
				}
				bufferedReader.close();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		return new JsonObject(response.toString());
	}

	private static final Logger LOGGER = Logger.getLogger(HttpRequestUtil.class.getName());

}
