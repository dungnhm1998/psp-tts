package asia.leadsgen.psp.test.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.util.AppParams;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

/**
 * Created by HungDX on 08-Aug-16.
 */
public class MailClient {

    private static String espServiceBaseUrl;

    public void setEspServiceBaseUrl(String espServiceBaseUrl) {
        this.espServiceBaseUrl = espServiceBaseUrl;
    }

    public static int send(String type, String receiver, String subject, String content) {

        int responseCode = 0;

        HttpURLConnection httpConnection = null;

        try {

            if(espServiceBaseUrl == null || espServiceBaseUrl.isEmpty()){
                espServiceBaseUrl = "http://api.30usd.com/esp/api/v1";
            }

            String espRequestURL = espServiceBaseUrl + "/email";

            Map espRequestBodyMap = new LinkedHashMap();
            espRequestBodyMap.put(AppParams.TYPE, type);

            Map emailInfoMap = new LinkedHashMap();
            emailInfoMap.put(AppParams.RECEIVE, receiver);
            emailInfoMap.put(AppParams.SUBJECT, subject);
            emailInfoMap.put(AppParams.BODY, content);

            espRequestBodyMap.put(AppParams.EMAIL, emailInfoMap);

            String espRequestBody = new JsonObject(espRequestBodyMap).encode();

            URL url = new URL(espRequestURL);

            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
            httpConnection.setRequestMethod(HttpMethod.POST.name());
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Content-Type", "application/json");

            LOGGER.info("[ESP REQUEST] URL: " + httpConnection.getURL());
            LOGGER.info("[ESP REQUEST] METHOD: " + httpConnection.getRequestMethod());
            LOGGER.info("[ESP REQUEST] HEADERS: " + httpConnection.getRequestProperties().toString());
            LOGGER.info("[ESP REQUEST] BODY: " + espRequestBody);

            OutputStreamWriter streamWriter = new OutputStreamWriter(httpConnection.getOutputStream());
            streamWriter.write(espRequestBody);
            streamWriter.flush();

            responseCode = httpConnection.getResponseCode();

            String responseMsg = httpConnection.getResponseMessage();

            LOGGER.info("[ESP RESPONSE] CODE: " + responseCode);
            LOGGER.info("[ESP RESPONSE] MESSAGE: " + responseMsg);
            LOGGER.info("[ESP RESPONSE] HEADERS: " + httpConnection.getHeaderFields().toString());

            if (responseCode == HttpURLConnection.HTTP_OK
                    || responseCode == HttpURLConnection.HTTP_CREATED) {

                InputStreamReader inputStreamReader = new InputStreamReader(httpConnection.getInputStream());

                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuffer responseBodyBuffer = new StringBuffer();

                String inputLine;

                while ((inputLine = bufferedReader.readLine()) != null) {
                    responseBodyBuffer.append(inputLine);
                }

                bufferedReader.close();

                LOGGER.info("[ESP RESPONSE] BODY: " + responseBodyBuffer.toString());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ERROR]", e);
        } finally {
            if (httpConnection != null){
                httpConnection.disconnect();
            }
        }

        return responseCode;
    }

    private static final Logger LOGGER = Logger.getLogger(MailClient.class.getName());
}
