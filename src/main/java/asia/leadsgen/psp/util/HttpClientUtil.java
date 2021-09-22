package asia.leadsgen.psp.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.server.vertical.PSPVertical;
import asia.leadsgen.security.wss.Authorization;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;

/**
 * Created by HungDX on 28-May-16.
 */
public class HttpClientUtil {

    public static HttpClientRequest createHttpRequest(HttpServiceConfig httpServiceConfig, String requestURI, HttpMethod requestMethod, Map queryParametersMap, String requestBody) {

        Date requestDate = new Date();

        SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

//        dateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));

        HttpClient httpClient = PSPVertical.httpClient;

        try {

            URI uri = new URI(httpServiceConfig.getServiceURL());

            if ("https".equals(uri.getScheme())) {
                httpClient = PSPVertical.httpsClient;
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }

        String requestURL = httpServiceConfig.getServiceURL() + requestURI;

        long requestTimeout = httpServiceConfig.getServiceTimeOut() > 0 ? httpServiceConfig.getServiceTimeOut() : 90000;

        HttpClientRequest httpClientRequest = httpClient.requestAbs(requestMethod, requestURL);

        httpClientRequest.setTimeout(requestTimeout);

        String signatureRequestURI = httpClientRequest.uri();

        if (httpClientRequest.uri().contains(StringPool.QUESTION)) {

            int queryIndex = httpClientRequest.uri().indexOf(StringPool.QUESTION);

            signatureRequestURI = httpClientRequest.uri().substring(0, queryIndex);
        }

        Map<String, String> signedHeaderMap = new LinkedHashMap<>();

        signedHeaderMap.put(AppParams.X_DATE, dateFormat.format(requestDate));
        signedHeaderMap.put(AppParams.X_EXPIRES, String.valueOf(requestTimeout));

        Authorization serviceAuthorization = new Authorization(httpServiceConfig.getServiceAuthAlgorithm(), httpServiceConfig.getServiceAuthId(), httpServiceConfig.getServiceAuthKey(),
                httpServiceConfig.getServiceRegion(), httpServiceConfig.getServiceName(), httpServiceConfig.getServiceAuthType(),
                httpClientRequest.method().name(), signatureRequestURI, queryParametersMap, signedHeaderMap,
                requestBody.getBytes(), requestDate, httpServiceConfig.getServiceTimeOut());

//        LOGGER.log(Level.INFO, "DEBUG INFO : " + serviceAuthorization.getDebugInfo().toString());

        httpClientRequest.putHeader(HttpHeaders.ACCEPT.toString(), AppConstants.CONTENT_TYPE_APPLICATION_JSON);
        httpClientRequest.putHeader(HttpHeaders.CONTENT_TYPE.toString(), AppConstants.CONTENT_TYPE_APPLICATION_JSON);
        httpClientRequest.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), AppUtil.getContentLength(requestBody));
        httpClientRequest.putHeader(AppParams.X_DATE, dateFormat.format(requestDate));
        httpClientRequest.putHeader(AppParams.X_EXPIRES, String.valueOf(requestTimeout));
        httpClientRequest.putHeader(AppParams.X_AUTHORIZATION, serviceAuthorization.toString());

        String serviceName = "[" + httpServiceConfig.getServiceName().toUpperCase() + " REQUEST] ";
        LOGGER.log(Level.INFO, serviceName + httpClientRequest.method().name() + StringPool.DOUBLE_SPACE + httpClientRequest.uri());
//        if(!requestBody.isEmpty() && !requestBody.contains(AppParams.DATA)) {
//            LOGGER.log(Level.INFO, serviceName + requestBody);
//        }

        return httpClientRequest;
    }

    private static final Logger LOGGER = Logger.getLogger(HttpClientUtil.class.getName());
}
