package asia.leadsgen.psp.server.handler.etsy;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.AuthorizationException;
import asia.leadsgen.psp.service_fulfill.EtsyService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.logging.Logger;

public class EtsyConnectAuthHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = Logger.getLogger(EtsyConnectAuthHandler.class.getName());

    @Override
    public void handle(RoutingContext context) {
        context.vertx().executeBlocking(future -> {

            String userId = ContextUtil.getString(context, AppParams.USER_ID);
            if (StringUtils.isEmpty(userId)) {
                throw new AuthorizationException(SystemError.LOGIN_REQUIRED);
            }

            JsonObject json = context.getBodyAsJson();
            String code = json.getString(AppParams.CODE);

            try {
                Map response = EtsyService.retrieveAccessToken(userId, code);

                context.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                context.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                context.put(AppParams.RESPONSE_DATA, response);

                future.complete();

            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
                e.printStackTrace();
            }


        }, asyncResult -> {
            if (asyncResult.succeeded()) {
                context.next();
            } else {
                context.fail(asyncResult.cause());
            }
        });
    }
}
