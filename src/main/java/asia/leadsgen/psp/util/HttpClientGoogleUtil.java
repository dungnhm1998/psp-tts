/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;

import asia.leadsgen.psp.interfaces.LoggerInterface;
import asia.leadsgen.psp.server.vertical.PSPVertical;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;

/**
 * 
 * @author liamle
 *
 */
public class HttpClientGoogleUtil implements LoggerInterface {

	public static HttpClientRequest createHttpRequest(HttpServiceConfig httpServiceConfig, String requestURI,
			HttpMethod requestMethod, Map queryParametersMap, String requestBody, Map<String, String> requestHeader) {
		DateFormat yyyyMMddTHHmmssZ = AppConstants.DEFAULT_DATE_TIME_FORMAT;
		yyyyMMddTHHmmssZ.setTimeZone(TimeZone.getTimeZone("UTC"));
		HttpClient httpClient = PSPVertical.httpClient;
		try {
			URI uri = new URI(httpServiceConfig.getServiceURL());
			if ("https".equals(uri.getScheme())) {
				httpClient = PSPVertical.httpsClient;
			}
		} catch (URISyntaxException e) {
			logger.log(Level.SEVERE, "", e);
		}
		String requestURL = httpServiceConfig.getServiceURL() + requestURI;
		long requestTimeout = httpServiceConfig.getServiceTimeOut() > 0 ? httpServiceConfig.getServiceTimeOut() : 60000;
		HttpClientRequest httpClientRequest = httpClient.requestAbs(requestMethod, requestURL);
		httpClientRequest.setTimeout(requestTimeout);
//		httpClientRequest.putHeader(HttpHeaders.ACCEPT.toString(), AppConstants.CONTENT_TYPE_APPLICATION_JSON);
		httpClientRequest.putHeader(HttpHeaders.CONTENT_TYPE.toString(),
				AppConstants.CONTENT_TYPE_APPLICATION_X_WWW_FORM_URLENCODED);

		httpClientRequest.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), AppUtil.getContentLength(requestBody));

		if (!requestHeader.isEmpty()) {
			requestHeader.entrySet().stream().forEach((entrySet) -> {
				httpClientRequest.putHeader(entrySet.getKey(), entrySet.getValue());
			});
		}
		String serviceName = "[" + httpServiceConfig.getServiceName().toUpperCase() + " REQUEST] ";
		logger.log(Level.INFO, "{0}{1}" + StringPool.DOUBLE_SPACE + "{2}",
				new Object[] { serviceName, httpClientRequest.method().name(), httpClientRequest.uri() });
		if (!requestBody.isEmpty() && !requestBody.contains(AppParams.DATA)) {
			logger.log(Level.INFO, "{0}{1}", new Object[] { serviceName, requestBody });
		}
		return httpClientRequest;
	}
}
