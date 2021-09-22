/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.server.handler.payment;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service.PaymentService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.HttpClientUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import com.google.gson.Gson;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 *
 * @author HIEPHV
 */
public class PaypalCreateInvoiceHandler implements Handler<RoutingContext> {

    private static String invoicerName;
    private static String invoicerEmail;
    private static String invoicerWebsite;
    private static String invoicerTaxId;
    private static String invoicerLogoUrl;

    public static void setInvoicerName(String invoicerName) {
        PaypalCreateInvoiceHandler.invoicerName = invoicerName;
    }

    public static void setInvoicerEmail(String invoicerEmail) {
        PaypalCreateInvoiceHandler.invoicerEmail = invoicerEmail;
    }

    public static void setInvoicerWebsite(String invoicerWebsite) {
        PaypalCreateInvoiceHandler.invoicerWebsite = invoicerWebsite;
    }

    public static void setInvoicerTaxId(String invoicerTaxId) {
        PaypalCreateInvoiceHandler.invoicerTaxId = invoicerTaxId;
    }

    public static void setInvoicerLogoUrl(String invoicerLogoUrl) {
        PaypalCreateInvoiceHandler.invoicerLogoUrl = invoicerLogoUrl;
    }

    private static HttpServiceConfig paymentConnectorServiceConfig;

    public static void setPaymentConnectorServiceConfig(HttpServiceConfig paymentConnectorServiceConfig) {
        PaypalCreateInvoiceHandler.paymentConnectorServiceConfig = paymentConnectorServiceConfig;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
            throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
        }

        routingContext.vertx().executeBlocking((Future<Object> future) -> {
            try {
                JsonObject requestBody = routingContext.getBodyAsJson();

                LOGGER.info("requestBody=" + requestBody.toString());
//                long currentTimeMillis = System.currentTimeMillis();
                String invoiceNumber = getRandomString(16);
                String method = AppParams.PAYPAL;
                String state = ResourceStates.CREATED;
                String reference = "";
                String recipientEmail = requestBody.getString("recipient-email");
                List<Map> orders = ParamUtil.getListData(requestBody.getMap(), "items");
                int totalOrderSuccess = 0;
                for (Map order : orders) {
                    String orderId = ParamUtil.getString(order, "order_id");
                    Map unit_amount = ParamUtil.getMapData(order, "unit_amount");
                    String amount = ParamUtil.getString(unit_amount, "value");

                    if (PaymentService.payPalCreateInvoice(orderId, state, reference, amount, "USD", "", method, invoiceNumber)) {
                        totalOrderSuccess++;
                    }

                }
                if (orders.size() == totalOrderSuccess) {

                    String paymentConnectorRequestURI = "/paypal/invoice/create";
                    Gson gs = new Gson();

                    String paymentRequestString = gs.toJson(invoiceBodyRequest(invoiceNumber, recipientEmail, orders));

                    LOGGER.info("Call pasp========" + paymentRequestString);

                    HttpClientRequest paymentConnectorRequest = HttpClientUtil.createHttpRequest(paymentConnectorServiceConfig,
                            paymentConnectorRequestURI, HttpMethod.POST, new LinkedHashMap<>(), paymentRequestString);

                    paymentConnectorRequest.handler(HttpResponse -> invoiceCreatedHandler(future, routingContext, HttpResponse, invoiceNumber));
                    paymentConnectorRequest.exceptionHandler(throwable -> routingContext.fail(throwable));
                    paymentConnectorRequest.write(paymentRequestString);
                    paymentConnectorRequest.end();

                } else {
                    PaymentService.paypalRemoveByInvoiceNumber(invoiceNumber);
                    throw new BadRequestException(SystemError.INVALID_ORDER);
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

    private void invoiceCreatedHandler(Future<Object> future, RoutingContext routingContext, HttpClientResponse httpResponse, String invoiceNumber) {

        httpResponse.bodyHandler(responseBody -> {
            try {
                JsonObject responseBodyJson = new JsonObject(responseBody.toString("UTF-8"));

                Map resultMap = new LinkedHashMap<>();
                LOGGER.info("pasp return:" + responseBodyJson);

                if (httpResponse.statusCode() == HttpResponseStatus.ACCEPTED.code() || httpResponse.statusCode() == HttpResponseStatus.OK.code() || httpResponse.statusCode() == HttpResponseStatus.CREATED.code()) {
                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.ACCEPTED.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.ACCEPTED.reasonPhrase());

                    String message = responseBodyJson.getString("message");
                    int status_code = responseBodyJson.getInteger("status_code");
                    Map data = ParamUtil.getMapData(responseBodyJson.getMap(), "data");

                    Map detailInvoice = ParamUtil.getMapData(responseBodyJson.getMap(), "detail");
                    String saleId = ParamUtil.getString(data, "id");
                    String paypalAccountName = ParamUtil.getString(data, "acount_name");
                    // update sale id
                    PaymentService.paypalSentInvoiceUpdate(invoiceNumber, saleId, paypalAccountName);

                    data.remove("acount_name");
                    Map resultDataMap = new LinkedHashMap<>();
                    resultDataMap.put(AppParams.MESSAGE, message);
                    resultDataMap.put(AppParams.STATUS_CODE, status_code);
                    resultDataMap.put(AppParams.DATA, data);
                    routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);

                } else if (httpResponse.statusCode() == HttpResponseStatus.BAD_REQUEST.code()) {
                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());

                    String message = responseBodyJson.getString("message");
                    int status_code = responseBodyJson.getInteger("status_code");
                    Map data = ParamUtil.getMapData(responseBodyJson.getMap(), "data");

                    Map resultDataMap = new LinkedHashMap<>();
                    resultDataMap.put(AppParams.MESSAGE, message);
                    resultDataMap.put(AppParams.STATUS_CODE, status_code);
                    resultDataMap.put(AppParams.DATA, data);
                    routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);
                } else {
                    throw new BadRequestException(SystemError.PAYMENT_CREATE_INVOICE_FAILED);
                }

                future.complete();
            } catch (Exception e) {
                routingContext.fail(e);
            }
        });

    }

    private static Map invoiceBodyRequest(String invoicerNumber, String recipientEmail, List<Map> items) {
        Map billPaypalBody = new LinkedHashMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.now();
        Map detailMap = new LinkedHashMap<>();
        detailMap.put("invoice_number", invoicerNumber);
        detailMap.put("reference", "BurgerPrints");
        detailMap.put("invoice_date", dtf.format(localDate));
        detailMap.put("currency_code", "USD");
        billPaypalBody.put("detail", detailMap);

        Map infoInvoicerMap = new LinkedHashMap<>();
        Map givenNameMap = new LinkedHashMap<>();
        givenNameMap.put("given_name", invoicerName);
        infoInvoicerMap.put("name", givenNameMap);
        infoInvoicerMap.put("email_address", invoicerEmail);
        infoInvoicerMap.put("website", invoicerWebsite);
        infoInvoicerMap.put("tax_id", invoicerTaxId);
        infoInvoicerMap.put("logo_url", invoicerLogoUrl);
        billPaypalBody.put("invoicer", infoInvoicerMap);

        List<Map> primaryRecipientsMap = new ArrayList<>();
        Map billingInfo = new LinkedHashMap<>();
        Map emailAdressBillingMap = new LinkedHashMap<>();
        emailAdressBillingMap.put("email_address", recipientEmail);
        billingInfo.put("billing_info", emailAdressBillingMap);
        primaryRecipientsMap.add(billingInfo);
        billPaypalBody.put("primary_recipients", primaryRecipientsMap);
        billPaypalBody.put("items", items.toArray());

        return billPaypalBody;
    }

    public static String getRandomString(int lenght) {
        String ret = null;
        Random r = new Random();
        String token = Long.toHexString(Math.abs(r.nextLong()));
        ret = token.substring(0, lenght);
        return ret;
    }
    private static final Logger LOGGER = Logger.getLogger(PaypalCreateInvoiceHandler.class.getName());

}
