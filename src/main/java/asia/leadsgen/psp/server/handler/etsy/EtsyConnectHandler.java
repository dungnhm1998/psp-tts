package asia.leadsgen.psp.server.handler.etsy;

import asia.leadsgen.psp.service_fulfill.EtsyService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import java.util.Map;
import java.util.logging.Logger;

public class EtsyConnectHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = Logger.getLogger(EtsyConnectHandler.class.getName());

    @Override
    public void handle(RoutingContext context) {
        context.vertx().executeBlocking(future -> {

            try {
                String userId = ContextUtil.getString(context, AppParams.USER_ID);
//                userId = "A2360";
                Map response = EtsyService.getOauthConnectUrl(userId);

                context.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                context.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                context.put(AppParams.RESPONSE_DATA, response);

                future.complete();
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
                e.printStackTrace();
            }

        }, asyncResult -> {
            if(asyncResult.succeeded()) {
                context.next();
            } else {
                context.fail(asyncResult.cause());
            }
        });
    }
}
