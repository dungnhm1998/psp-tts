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
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author HIEPHV
 */
public class WooEcommerceConnectHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {
        String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
        JsonObject requestBody = routingContext.getBodyAsJson();
        LOGGER.info(requestBody.toString());

        String name = requestBody.getString("name");
        String domain = requestBody.getString("domain");

        routingContext.vertx().executeBlocking(future -> {
            try {

                Map dsStore = setupStore(userId, name, domain, "woocommerce");
                int resultCode = ParamUtil.getInt(dsStore, AppParams.RESULT_CODE);
                if (resultCode == HttpResponseStatus.CONFLICT.code()) {
                    throw new BadRequestException(SystemError.WOO_STORE_CONFLIC);
                } else if (resultCode == HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                    throw new BadRequestException(SystemError.INTERNAL_SERVER_ERROR);
                } else {
                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                    routingContext.put(AppParams.RESPONSE_DATA, dsStore);
                }

            } catch (Exception e) {
                routingContext.fail(e);
            }

            future.complete();

        }, asyncResult -> {
            if (asyncResult.succeeded()) {
                routingContext.next();
            } else {
                routingContext.fail(asyncResult.cause());
            }
        });
    }

    private Map setupStore(String userId, String name, String domain, String channel) throws UnirestException, SQLException {

        if (name.isEmpty() || domain.isEmpty()) {
            throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
        }

        Map dsStore = DropShipStoreService.addWooStore(userId, channel, name, domain, "", ResourceStates.CREATED);

        return dsStore;

    }
    private static final Logger LOGGER = Logger.getLogger(WooEcommerceConnectHandler.class.getName());
}
