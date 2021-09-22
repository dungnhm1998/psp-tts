package asia.leadsgen.psp.server.handler.fraud;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.FraudException;
import asia.leadsgen.psp.exception.HttpServiceException;
import asia.leadsgen.psp.exception.SystemException;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.HttpClientUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class FraudHandler implements Handler<RoutingContext> {

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

        if(fraudCheckEnable) {

            routingContext.vertx().executeBlocking(future -> {

                try {

                    String fraudDataType = ContextUtil.getString(routingContext, AppParams.FRAUD_DATA_TYPE);

                    if(fraudDataType.isEmpty()){
                        throw new SystemException(SystemError.INVALID_FRAUD_DATA);
                    }

                    HttpServerRequest httpServerRequest = routingContext.request();

                    MultiMap requestHeaders = httpServerRequest.headers().getDelegate();

                    Set<String> headerNames = httpServerRequest.headers().names();

                    Map requestHeadersMap = new LinkedHashMap();

                    for (String headerName : headerNames){
                        requestHeadersMap.put(headerName, requestHeaders.get(headerName));
                    }

                    SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

                    dateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));

                    String requestTime = requestHeaders.get(AppParams.X_DATE) != null
                            ? requestHeaders.get(AppParams.X_DATE) : dateFormat.format(new Date());

                    Map requestContentMap = new LinkedHashMap();
                    requestContentMap.put(AppParams.URI, httpServerRequest.uri());
                    requestContentMap.put(AppParams.METHOD, httpServerRequest.method().name());

                    Map requestBodyMap = routingContext.getBodyAsString() != null && !routingContext.getBodyAsString().isEmpty()
                            ? routingContext.getBodyAsJson().getMap() : new LinkedHashMap();

                    requestContentMap.put(AppParams.BODY, requestBodyMap);

                    Map fspRequestBodyMap = new LinkedHashMap();
                    fspRequestBodyMap.put(AppParams.TYPE, fraudDataType);
                    fspRequestBodyMap.put(AppParams.REQUEST_TIME, requestTime);
                    fspRequestBodyMap.put(AppParams.REQUEST_CONTENT, requestContentMap);

                    String fspRequestURI = "/data";

                    String fspRequestBody = new JsonObject(fspRequestBodyMap).encode();

                    HttpClientRequest fspClientRequest = HttpClientUtil.createHttpRequest(fspServiceConfig, fspRequestURI, HttpMethod.POST, new LinkedHashMap<>(), fspRequestBody);

                    fspClientRequest.handler(fspResponse -> fspDataInsertHandler(routingContext, fspResponse));

                    fspClientRequest.exceptionHandler(throwable -> routingContext.fail(throwable));

                    fspClientRequest.write(fspRequestBody);
                    fspClientRequest.end();

                } catch (Exception e) {
                    routingContext.fail(e);
                }

            }, asyncResult -> {
                if (asyncResult.succeeded()) {
                    routingContext.next();
                } else {
                    routingContext.fail(asyncResult.cause());
                }
            });

        }else {
            routingContext.next();
        }
    }

    private static void fspDataInsertHandler(RoutingContext routingContext, HttpClientResponse fspDataPostResponse) {

        int responseCode = fspDataPostResponse.statusCode();

        String responseMsg = fspDataPostResponse.statusMessage();

        LOGGER.log(Level.INFO, "[FSP RESPONSE] " + responseCode + " " + responseMsg);

        fspDataPostResponse.bodyHandler(responseBody -> {

            try {

                JsonObject responseBodyJson = new JsonObject(responseBody.toString("UTF-8"));

                LOGGER.log(Level.INFO, "[FSP RESPONSE] " + responseBodyJson);

                if (responseCode != HttpResponseStatus.CREATED.code()) {
                    throw new HttpServiceException(responseCode, responseMsg, responseBodyJson.getMap());
                }

                String fraudDataId = responseBodyJson.getString(AppParams.ID);

                routingContext.put(AppParams.FRAUD_DATA_ID, fraudDataId);

                String fraudDataType = responseBodyJson.getString(AppParams.TYPE);

                Map queryParamsMap = new LinkedHashMap();
                queryParamsMap.put(AppParams.TYPE, fraudDataType);
                queryParamsMap.put(AppParams.REFERENCE, fraudDataId);

                String fspRequestURI = "/frauds?type=" + fraudDataType + "&reference=" + fraudDataId;

                HttpClientRequest fspClientRequest = HttpClientUtil.createHttpRequest(fspServiceConfig, fspRequestURI, HttpMethod.GET, queryParamsMap, StringPool.BLANK);

                fspClientRequest.handler(fspResponse -> fspFraudCheckHandler(routingContext, fspResponse));

                fspClientRequest.exceptionHandler(throwable -> routingContext.fail(throwable));

                fspClientRequest.end();

            } catch (Exception e) {
                routingContext.fail(e);
            }
        });

        fspDataPostResponse.exceptionHandler(throwable -> routingContext.fail(throwable));
    }

    private static void fspFraudCheckHandler(RoutingContext routingContext, HttpClientResponse fspFraudCheckResponse) {

        int responseCode = fspFraudCheckResponse.statusCode();

        String responseMsg = fspFraudCheckResponse.statusMessage();

        LOGGER.log(Level.INFO, "[FSP RESPONSE] " + responseCode + " " + responseMsg);

        fspFraudCheckResponse.bodyHandler(responseBody -> {

            try {

                JsonObject responseBodyJson = new JsonObject(responseBody.toString("UTF-8"));

                LOGGER.log(Level.INFO, "[FSP RESPONSE] " + responseBodyJson);

                if (responseCode != HttpResponseStatus.OK.code()) {
                    throw new HttpServiceException(responseCode, responseMsg, responseBodyJson.getMap());
                }

                int totalFraudRules = responseBodyJson.getInteger(AppParams.TOTAL, 0);

                if(totalFraudRules > 0){
                    throw new FraudException(responseBodyJson.encode());
                }

                routingContext.next();

            } catch (Exception e) {
                routingContext.fail(e);
            }
        });

        fspFraudCheckResponse.exceptionHandler(throwable -> routingContext.fail(throwable));
    }

    private static final Logger LOGGER = Logger.getLogger(FraudHandler.class.getName());
}
