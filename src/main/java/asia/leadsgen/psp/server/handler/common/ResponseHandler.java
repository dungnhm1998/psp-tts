package asia.leadsgen.psp.server.handler.common;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.HttpClientUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class ResponseHandler implements Handler<RoutingContext> {

	private static boolean fraudCheckEnable;
	private static HttpServiceConfig fspServiceConfig;

	public void setFraudCheckEnable(boolean fraudCheckEnable) {
		this.fraudCheckEnable = fraudCheckEnable;
	}

	public void setFspServiceConfig(HttpServiceConfig fspServiceConfig) {
		this.fspServiceConfig = fspServiceConfig;
	}

	@Override
	public void handle(RoutingContext routingContext) {

		int responseCode = ContextUtil.getInt(routingContext, AppParams.RESPONSE_CODE,
				HttpResponseStatus.NOT_FOUND.code());
		String responseDesc = ContextUtil.getString(routingContext, AppParams.RESPONSE_MSG,
				HttpResponseStatus.NOT_FOUND.reasonPhrase());

		Map responseBodyMap = ContextUtil.getMapData(routingContext, AppParams.RESPONSE_DATA, new LinkedHashMap<>());

		HttpServerResponse httpServerResponse = routingContext.response();

		if (responseCode == HttpResponseStatus.TEMPORARY_REDIRECT.code()) {
			LOGGER.info("[RESPONSE] responseBodyMap  " + responseBodyMap.toString());

			String redirectUrl = ParamUtil.getString(responseBodyMap, AppParams.URL);
			httpServerResponse.setStatusCode(responseCode);
			httpServerResponse.setStatusMessage(responseDesc);
			httpServerResponse.putHeader(HttpHeaders.LOCATION.toString(), redirectUrl);
			LOGGER.info("[RESPONSE] Redirect to " + redirectUrl);

			httpServerResponse.end(new JsonObject().encode());
			return;
		}

		String responseBody = new JsonObject(responseBodyMap).encode();

		httpServerResponse.setStatusCode(responseCode);
		httpServerResponse.setStatusMessage(responseDesc);
		httpServerResponse.putHeader(HttpHeaders.CONTENT_TYPE.toString(), AppConstants.CONTENT_TYPE_APPLICATION_JSON);
		httpServerResponse.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), AppUtil.getContentLength(responseBody));

		if (fraudCheckEnable && !ContextUtil.getString(routingContext, AppParams.FRAUD_DATA_ID).isEmpty()) {
			updateFraudData(routingContext, ResourceStates.APPROVED, responseBodyMap);
		}

		LOGGER.info(StringPool.NEW_LINE);
		LOGGER.log(Level.INFO, "[RESPONSE] " + responseCode + StringPool.DOUBLE_SPACE + responseDesc);

		Set<String> headerNames = httpServerResponse.headers().names();

		headerNames.stream()
				.filter(header -> Arrays.asList(RESPONSE_LOGGING_HEADERS).stream().anyMatch(header::equalsIgnoreCase))
				.forEach(header -> LOGGER.info("[RESPONSE] HEADER: " + header + StringPool.SPACE + StringPool.COLON
						+ StringPool.SPACE + httpServerResponse.headers().get(header)));

		String loggingResponseBody = responseBody.length() > 1000 ? responseBody.substring(0, 1000) + "..."
				: responseBody;

		LOGGER.log(Level.INFO, "[RESPONSE] BODY: " + loggingResponseBody);

		LOGGER.log(Level.INFO,
				"[RESPONSE] ****************************** DONE ******************************" + StringPool.NEW_LINE);

		httpServerResponse.end(new JsonObject(responseBody).encode());

	}

	private void updateFraudData(RoutingContext routingContext, String dataState, Map responseBodyMap) {

		HttpServerResponse httpServerResponse = routingContext.response();

		SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

		String responseTime = dateFormat.format(new Date());

		Map responseDataMap = new LinkedHashMap();
		responseDataMap.put(AppParams.STATUS_CODE, httpServerResponse.getStatusCode());
		responseDataMap.put(AppParams.STATUS_MESSAGE, httpServerResponse.getStatusMessage());

		MultiMap responseHeaders = httpServerResponse.headers().getDelegate();

		Set<String> headerNames = httpServerResponse.headers().names();

		Map responseHeadersMap = new LinkedHashMap();

		for (String headerName : headerNames) {
			responseHeadersMap.put(headerName, responseHeaders.get(headerName));
		}

		responseDataMap.put(AppParams.HEADERS, responseHeadersMap);
		responseDataMap.put(AppParams.BODY, responseBodyMap);

		Map fspRequestBodyMap = new LinkedHashMap();
		fspRequestBodyMap.put(AppParams.STATE, dataState);
		fspRequestBodyMap.put(AppParams.RESPONSE_TIME, responseTime);
		fspRequestBodyMap.put(AppParams.RESPONSE_DATA, responseDataMap);

		String fspRequestURI = "/data/" + ContextUtil.getString(routingContext, AppParams.FRAUD_DATA_ID);

		String fspRequestBody = new JsonObject(fspRequestBodyMap).encode();

		HttpClientRequest fspClientRequest = HttpClientUtil.createHttpRequest(fspServiceConfig, fspRequestURI,
				HttpMethod.PUT, new LinkedHashMap<>(), fspRequestBody);

		fspClientRequest.handler(fspResponse -> fspDataUpdateHandler(routingContext, fspResponse));

		fspClientRequest.exceptionHandler(throwable -> routingContext.fail(throwable));

		fspClientRequest.write(fspRequestBody);
		fspClientRequest.end();
	}

	private static void fspDataUpdateHandler(RoutingContext routingContext, HttpClientResponse fspResponse) {

		int responseCode = fspResponse.statusCode();

		String responseMsg = fspResponse.statusMessage();

		LOGGER.log(Level.INFO, "[FSP RESPONSE] " + responseCode + " " + responseMsg);

		fspResponse.bodyHandler(responseBody -> {

			try {

				JsonObject responseBodyJson = new JsonObject(responseBody.toString("UTF-8"));

				LOGGER.log(Level.INFO, "[FSP RESPONSE] " + responseBodyJson);

			} catch (Exception e) {
				routingContext.fail(e);
			}
		});

		fspResponse.exceptionHandler(throwable -> routingContext.fail(throwable));
	}

	private static final String[] RESPONSE_LOGGING_HEADERS = { HttpHeaders.COOKIE.toString() };

	private static final Logger LOGGER = Logger.getLogger(ResponseHandler.class.getName());
}
