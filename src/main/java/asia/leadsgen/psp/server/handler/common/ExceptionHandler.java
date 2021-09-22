package asia.leadsgen.psp.server.handler.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.io.JsonEOFException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.AuthorizationException;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.FraudException;
import asia.leadsgen.psp.exception.HttpServiceException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.exception.ResourceConflictException;
import asia.leadsgen.psp.exception.ResourceNotFoundException;
import asia.leadsgen.psp.exception.SystemException;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.HttpClientUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class ExceptionHandler implements Handler<RoutingContext> {

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

		SystemError systemError;

		Throwable throwable = routingContext.failure();

		int statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();

		String statusMessage = HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase();

		if (throwable instanceof SystemException) {

			SystemException systemException = (SystemException) throwable;

			systemError = systemException.getSystemError();

			if (throwable instanceof LoginException) {

				statusCode = HttpResponseStatus.UNAUTHORIZED.code();
				statusMessage = HttpResponseStatus.UNAUTHORIZED.reasonPhrase();

				Cookie sessionCookie = routingContext.getCookie(AppParams.SESSION_ID);
				if (sessionCookie != null) {
					sessionCookie.setMaxAge(0);
				}

				Cookie userCookie = routingContext.getCookie(AppParams.USER_EMAIL);
				if (userCookie != null) {
					userCookie.setMaxAge(0);
				}

			} else if (throwable instanceof AuthorizationException) {

				statusCode = HttpResponseStatus.FORBIDDEN.code();
				statusMessage = HttpResponseStatus.FORBIDDEN.reasonPhrase();

			} else if (throwable instanceof BadRequestException) {

				statusCode = HttpResponseStatus.BAD_REQUEST.code();
				statusMessage = HttpResponseStatus.BAD_REQUEST.reasonPhrase();

			} else if (throwable instanceof ResourceConflictException) {

				statusCode = HttpResponseStatus.CONFLICT.code();
				statusMessage = HttpResponseStatus.CONFLICT.reasonPhrase();

			} else if (throwable instanceof ResourceNotFoundException) {

				statusCode = HttpResponseStatus.NOT_FOUND.code();
				statusMessage = HttpResponseStatus.NOT_FOUND.reasonPhrase();

			} else if (throwable instanceof FraudException) {

				statusCode = HttpResponseStatus.FORBIDDEN.code();
				statusMessage = systemError.getReason();

			} else if (throwable instanceof HttpServiceException) {

				statusCode = systemError.getCode();
				statusMessage = systemError.getReason();

			} else {

				statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
				statusMessage = HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase();
			}

		} else if (throwable instanceof DecodeException || throwable instanceof JsonEOFException) {

			systemError = SystemError.INVALID_JSON_FORMAT;

		} else {
			LOGGER.log(Level.SEVERE, "[ERROR]", throwable);
			systemError = SystemError.INTERNAL_SERVER_ERROR;
		}

		Map responseBodyMap = new LinkedHashMap<>();

		responseBodyMap.put(AppParams.NAME, systemError.getName());
		responseBodyMap.put(AppParams.MESSAGE, systemError.getMessage());
		responseBodyMap.put(AppParams.DETAILS,
				systemError.getDetails() != null ? systemError.getDetails() : StringPool.BLANK);
		responseBodyMap.put(AppParams.INFORMATION_LINK,
				systemError.getInformationLink() != null ? systemError.getInformationLink() : StringPool.BLANK);

		String responseBody = new JsonObject(responseBodyMap).encode();

		if (throwable instanceof FraudException) {
			responseBody = systemError.getDetails();
		}

		HttpServerResponse httpServerResponse = routingContext.response();

		httpServerResponse.setStatusCode(statusCode);
		httpServerResponse.setStatusMessage(statusMessage);
		httpServerResponse.putHeader(HttpHeaders.CONTENT_TYPE.toString(), AppConstants.CONTENT_TYPE_APPLICATION_JSON);
		httpServerResponse.putHeader(HttpHeaders.CONTENT_LENGTH.toString(), AppUtil.getContentLength(responseBody));

		if (fraudCheckEnable && !ContextUtil.getString(routingContext, AppParams.FRAUD_DATA_ID).isEmpty()) {
			updateFraudData(routingContext, ResourceStates.FAIL, responseBodyMap);
		}

		LOGGER.info("[RESPONSE] " + statusCode + StringPool.DOUBLE_SPACE + statusMessage);
		LOGGER.info("[RESPONSE] BODY: " + responseBody);
		LOGGER.info(
				"[RESPONSE] ****************************** DONE ******************************" + StringPool.NEW_LINE);

		httpServerResponse.end(new JsonObject(responseBody).encode());
	}

	private void updateFraudData(RoutingContext routingContext, String dataState, Map responseBodyMap) {

		HttpServerResponse httpServerResponse = routingContext.response();

		SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

//		dateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));

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

	private static final Logger LOGGER = Logger.getLogger(ExceptionHandler.class.getName());
}
