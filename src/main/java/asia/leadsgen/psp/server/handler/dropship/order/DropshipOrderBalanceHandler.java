package asia.leadsgen.psp.server.handler.dropship.order;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderBalanceHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {

        routingContext.vertx().executeBlocking(future -> {
            try {
                String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
                LOGGER.info("userId= " + userId);
                if (StringUtils.isEmpty(userId)) {
                    throw new LoginException(SystemError.LOGIN_REQUIRED);
                }
                LOGGER.info("Handler DropshipOrderBalanceHandler with userId:=" + userId);
                Map orderInfoMap = DropshipOrderService.getBalance(userId);
                routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                routingContext.put(AppParams.RESPONSE_DATA, orderInfoMap);

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

    private static final Logger LOGGER = Logger.getLogger(DropshipOrderBalanceHandler.class.getName());
}
