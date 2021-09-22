package asia.leadsgen.psp.server.handler.preferences;

import java.util.Map;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service.PreferencesService;
import asia.leadsgen.psp.util.AppParams;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class PreferencesLookUpHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {

        String preferenceKey = routingContext.request().getParam(AppParams.KEY);

        if(preferenceKey == null || preferenceKey.isEmpty()){
            throw new BadRequestException(SystemError.INVALID_KEY);
        }

        routingContext.vertx().executeBlocking(future -> {

            try {

                Map preferenceInfoMap = PreferencesService.getInfo(preferenceKey);

                routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                routingContext.put(AppParams.RESPONSE_DATA, preferenceInfoMap);

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
}
