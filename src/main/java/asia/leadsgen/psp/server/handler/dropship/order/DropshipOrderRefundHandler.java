package asia.leadsgen.psp.server.handler.dropship.order;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.util.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service_fulfill.FulfillmentService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.HttpClientUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderRefundHandler implements Handler<RoutingContext> {

    private static HttpServiceConfig paymentConnectorServiceConfig;

    public static void setPaymentConnectorServiceConfig(HttpServiceConfig paymentConnectorServiceConfig) {
        DropshipOrderRefundHandler.paymentConnectorServiceConfig = paymentConnectorServiceConfig;
    }

    @Override
    public void handle(RoutingContext routingContext) {
    	LOGGER.info("handle ===============");
        String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

        routingContext.vertx().executeBlocking(future -> {
            try {
                String orderId = routingContext.request().getParam(AppParams.ID);
                Map orderInfoMap = DropshipOrderService.lookUp(orderId, false, false, true);
                LOGGER.info("orderInfoMap: " + orderInfoMap.toString());
                String orderUserId = ParamUtil.getString(orderInfoMap, AppParams.USER_ID);
                String orderState = ParamUtil.getString(orderInfoMap, AppParams.STATE);

                if (!userId.equals(orderUserId)) {
                    throw new BadRequestException(SystemError.OPERATION_NOT_PERMITTED);
                }

                if (!ResourceStates.PLACED.equals(orderState)) {
                    throw new BadRequestException(SystemError.INVALID_ORDER);
                }

                Map fulfillmentInfo = FulfillmentService.searchByOrderId(orderId);

                if (!fulfillmentInfo.isEmpty()) {
                    throw new BadRequestException(SystemError.CANT_REFUND_ORDER_READY_TO_PRINT);
                } else {
                    Map paymentInfo = ParamUtil.getMapData(orderInfoMap, AppParams.PAYMENT);
                	LOGGER.info("paymentInfo= " + paymentInfo.toString());
                    String amount = ParamUtil.getString(orderInfoMap, AppParams.AMOUNT);
                    
                    String transactionId = ParamUtil.getString(paymentInfo, AppParams.TRANSACTION_ID);
                    String paymentMethod = ParamUtil.getString(paymentInfo, AppParams.METHOD);
                    String accountName = ParamUtil.getString(paymentInfo, AppParams.PAYMENT_NAME);
                    if (StringUtils.isEmpty(transactionId) || StringUtils.isEmpty(paymentMethod)) {
                        throw new BadRequestException(SystemError.INVALID_PAYMENT_INFO);
                    }
                    if (paymentMethod.equalsIgnoreCase(AppParams.PAYPAL)) {
                    	transactionId = ParamUtil.getString(paymentInfo, AppParams.SALE_ID);
                    }

                    LOGGER.info("paymentInfo: " + paymentInfo.toString());
                    
//                    if (paymentMethod.equalsIgnoreCase(AppParams.PAYONEER)) {
//                    	String currency = ParamUtil.getString(paymentInfo, AppParams.CURRENCY);
//                        String token = ParamUtil.getString(paymentInfo, AppParams.TOKEN);
//                    	processPayOneerRefund(routingContext, future, transactionId, paymentMethod, amount, orderId, accountName, currency, token);
//        			} else {
//        				processRefund(routingContext, future, transactionId, paymentMethod, amount, orderId, accountName);
//        			}
                    
                    // Adjust order dropship
                    String trackingCode = ParamUtil.getString(orderInfoMap, AppParams.TRACKING_CODE);
                    String message = "Refund this order " + orderId;
                    DropshipOrderService.adjust(trackingCode, message);
                    
                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                    future.complete();
                    
                    
//                    if (paymentMethod.equalsIgnoreCase(AppParams.PAYPAL) && StringUtils.isEmpty(transactionId)) {
//
//                        String paypalSaleId = ParamUtil.getString(paymentInfo, AppParams.SALE_ID);
//
//                        String paymentConnectorRequestURI = "/paypal/dropship-order/" + paypalSaleId + "/refund";
//                        Gson gs = new Gson();
//                        String paymentRequestString = gs.toJson(invoiceBodyRequest(amount, accountName));
//                        LOGGER.info("Call pasp ========" + paymentRequestString);
//                        HttpClientRequest paymentConnectorRequest = HttpClientUtil.createHttpRequest(paymentConnectorServiceConfig,
//                                paymentConnectorRequestURI, HttpMethod.POST, new LinkedHashMap<>(), paymentRequestString);
//                        paymentConnectorRequest.handler(HttpResponse -> invoiceRefundHandler(future, routingContext, HttpResponse, orderId));
//                        paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
//                        paymentConnectorRequest.write(paymentRequestString);
//                        paymentConnectorRequest.end();
//
//                    } else {
//                        if (StringUtils.isEmpty(transactionId) || StringUtils.isEmpty(paymentMethod)) {
//                            throw new BadRequestException(SystemError.INVALID_PAYMENT_INFO);
//                        }
//                        processRefund(routingContext, future, transactionId, paymentMethod, amount, orderId, accountName);
//                    }

                }

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
    }

    private void processPayOneerRefund(RoutingContext routingContext, Future future, String transactionId, String paymentMethod,
            String amount, String orderId, String accountName, String currency, String token) {
        Map refundRequestBody = new LinkedHashMap<>();
        Map dataObj = new LinkedHashMap<>();
        refundRequestBody.put("account_name", accountName);
        
        Map amountObj = new LinkedHashMap<>();
        amountObj.put(AppParams.TOTAL, amount);
        amountObj.put(AppParams.CURRENCY, currency);
        
        dataObj.put(AppParams.NOTE, "Refund dropship order " + orderId);
        dataObj.put(AppParams.TOKEN, token);
        dataObj.put(AppParams.AMOUNT, amountObj);
        
        refundRequestBody.put("data", dataObj);

        String refundRequestUri = "/payoneer/dropship-order/" + transactionId + "/refund";
        String paymentRequestString = new JsonObject(refundRequestBody).encode();
        HttpClientRequest paymentConnectorRequest = HttpClientUtil.createHttpRequest(paymentConnectorServiceConfig,
                refundRequestUri, HttpMethod.POST, new LinkedHashMap<>(), paymentRequestString);

        paymentConnectorRequest
                .handler(refundResponse -> refundResponseHandler(routingContext, future, refundResponse, orderId, amount));
        paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
        paymentConnectorRequest.write(paymentRequestString);
        paymentConnectorRequest.end();
    }
    
    private void processRefund(RoutingContext routingContext, Future future, String transactionId, String paymentMethod,
    		String amount, String orderId, String accountName) {
    	Map refundRequestBody = new LinkedHashMap<>();
    	refundRequestBody.put("account_name", accountName);
    	refundRequestBody.put("data", Collections.EMPTY_MAP);
//		refundRequestBody.put(AppParams.CHARGE_ID, transactionId);
//		refundRequestBody.put(AppParams.METHOD, paymentMethod);
//		refundRequestBody.put(AppParams.AMOUNT, amount);
    	
    	String refundRequestUri = "/" + paymentMethod + "/" + transactionId + "/refund";
    	String paymentRequestString = new JsonObject(refundRequestBody).encode();
    	HttpClientRequest paymentConnectorRequest = HttpClientUtil.createHttpRequest(paymentConnectorServiceConfig,
    			refundRequestUri, HttpMethod.POST, new LinkedHashMap<>(), paymentRequestString);
    	
    	paymentConnectorRequest
    	.handler(refundResponse -> refundResponseHandler(routingContext, future, refundResponse, orderId, amount));
    	paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
    	paymentConnectorRequest.write(paymentRequestString);
    	paymentConnectorRequest.end();
    }

    private void refundResponseHandler(RoutingContext routingContext, Future future, HttpClientResponse refundResponse,
            String orderId, String amount) {
        int responseCode = refundResponse.statusCode();
        String responseMsg = refundResponse.statusMessage();

        refundResponse.bodyHandler(responseBody -> {
            try {
                JsonObject jsonErr = new JsonObject();
                jsonErr.put(AppParams.STATUS, "failed");
                JsonObject responseBodyJson = (responseCode == HttpResponseStatus.CREATED.code())
                        ? new JsonObject(responseBody.toString("UTF-8"))
                        : jsonErr;
                Map responseBodyMap = responseBodyJson.getMap();
                LOGGER.info(responseBodyMap.toString());
                if (ResourceStates.SUCCEEDED.equals(ParamUtil.getString(responseBodyMap, AppParams.STATUS))) {
                    DropshipOrderService.updateState(orderId, ResourceStates.REFUNDED);
                    DropshipOrderService.updateOrderRefundedAmount(orderId, amount);
//					DropshipOrderRefundService.refundFullOrder(orderId);
                } else {
                    responseBodyMap = jsonErr.getMap();
                }

                routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                routingContext.put(AppParams.RESPONSE_DATA, responseBodyMap);
                routingContext.next();
                
            } catch (Exception e) {
                routingContext.fail(e);
            }
        });

    }

    private void invoiceRefundHandler(Future<Object> future, RoutingContext routingContext, HttpClientResponse httpResponse, String orderId) {

        httpResponse.bodyHandler(responseBody -> {
            try {
                JsonObject responseBodyJson = new JsonObject(responseBody.toString("UTF-8"));

                Map resultMap = new LinkedHashMap<>();
                LOGGER.info("pasp return:" + responseBodyJson);

                if (httpResponse.statusCode() == HttpResponseStatus.ACCEPTED.code() || httpResponse.statusCode() == HttpResponseStatus.OK.code() || httpResponse.statusCode() == HttpResponseStatus.CREATED.code()) {
                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());

                    String message = responseBodyJson.getString("message");
                    int status_code = responseBodyJson.getInteger("status_code");
                    String id = responseBodyJson.getString("id");
                    Map data = ParamUtil.getMapData(responseBodyJson.getMap(), "data");

                    Map detailInvoice = ParamUtil.getMapData(responseBodyJson.getMap(), "detail");
                    String paypalAccountName = ParamUtil.getString(data, "acount_name");

                    DropshipOrderService.updateState(orderId, ResourceStates.REFUNDED);
                    Map resultDataMap = new LinkedHashMap<>();
                    resultDataMap.put(AppParams.ID, id);
                    resultDataMap.put(AppParams.MESSAGE, message);
                    resultDataMap.put(AppParams.STATUS, "succeeded");
                    resultDataMap.put(AppParams.STATUS_CODE, status_code);
                    resultDataMap.put(AppParams.DATA, data);

                    routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);

                } else {
                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());

                    String message = responseBodyJson.getString("message");
                    int status_code = responseBodyJson.getInteger("status_code");
                    Map data = ParamUtil.getMapData(responseBodyJson.getMap(), "data");
                    Map resultDataMap = new LinkedHashMap<>();
                    resultDataMap.put(AppParams.ID, "");
                    resultDataMap.put(AppParams.MESSAGE, message);
                    resultDataMap.put(AppParams.STATUS, "failed");
                    resultDataMap.put(AppParams.STATUS_CODE, status_code);
                    resultDataMap.put(AppParams.DATA, data);
                    
                    routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);

                }

                future.complete();
            } catch (Exception e) {
                routingContext.fail(e);
            }
        });

    }

    private static Map invoiceBodyRequest(String amount, String account_name) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.now();
        Map bodyRequest = new LinkedHashMap<>();
        bodyRequest.put("account_name", account_name);
        bodyRequest.put("method", "PAYPAL");
        bodyRequest.put("refund_date", dtf.format(localDate));
        Map amountMap = new LinkedHashMap<>();
        amountMap.put("currency_code", "USD");
        amountMap.put("value", amount);
        bodyRequest.put("amount", amountMap);

        return bodyRequest;
    }
    private static final Logger LOGGER = Logger.getLogger(DropshipOrderRefundHandler.class.getName());
}
