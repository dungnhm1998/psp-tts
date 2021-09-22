/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.server.handler.dropship.shopify;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author HIEPHV
 */
public class WooEcommerceConnectAuthHandler implements Handler<RoutingContext> {

    private static String url;

    public static void setUrl(String url) {
        WooEcommerceConnectAuthHandler.url = url;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.vertx().executeBlocking((Future<Object> future) -> {

            try {
                JsonObject requestBody = routingContext.getBodyAsJson();
                LOGGER.info("requestBody=" + requestBody.toString());

                int key_id = requestBody.getInteger("key_id");
                String storeId = requestBody.getString("user_id");
                String consumer_key = requestBody.getString("consumer_key");
                String consumer_secret = requestBody.getString("consumer_secret");
                String key_permissions = requestBody.getString("key_permissions");
                
             

                if (DropShipStoreService.wooUpdate(storeId, consumer_key, consumer_secret, ResourceStates.APPROVED)) {

                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                    routingContext.put(AppParams.RESPONSE_DATA, new LinkedHashMap<>());

                    Map storeMap = DropShipStoreService.lookUp(storeId);

                    String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
                   

                    Map webhook = new LinkedHashMap<>();

                    webhook.put("name", "BurgerPrints Integration");
                    webhook.put("topic", "action.woocommerce_order_status_processing");
                    webhook.put("delivery_url", url + "/dropship/woocommerce/order?id="+storeId);
                    
//                    AddWooWebhook(domain, consumer_key, consumer_secret, webhook);
                    
                } else {
                    throw new BadRequestException(SystemError.INTERNAL_SERVER_ERROR);
                }

                future.complete();
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

    public static Map AddWooWebhook(String domain, String consumer_key, String consumer_secret, Map body) throws UnirestException {
        Gson gson = new Gson();
        String formatUrl = MessageFormat.format("{0}/wp-json/wc/v3/webhooks?consumer_key={1}&consumer_secret={2}", domain, consumer_key, consumer_secret);
        HttpResponse<String> response = Unirest.post(formatUrl)
                .header("Content-Type", "application/json").body(gson.toJson(body))
                .asString();

        Map responseMap = new JsonObject(response.getBody()).getMap();
        LOGGER.info("url:" + formatUrl + " data result:" + responseMap);
        Map result = new LinkedHashMap<>();
        result.put("code", response.getStatus());
        result.put("message", response.getStatus() == 201 ? "ok" : ParamUtil.getString(responseMap, AppParams.MESSAGE));
        result.put("data", response.getStatus() == 201 ? responseMap : Collections.EMPTY_MAP);
        return result;

    }
    

    private static final Logger LOGGER = Logger.getLogger(WooEcommerceConnectAuthHandler.class.getName());

}
