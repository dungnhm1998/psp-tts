package asia.leadsgen.psp.server.handler.dropship.store;

import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipStoreLookupHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			try {
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				String storeId = routingContext.request().params().get("id");
				
				DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);

				Map storeResult = DropShipStoreService.lookUp(storeId);
				if(storeResult.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
				}
					
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, storeResult);

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

	private static final Logger LOGGER = Logger.getLogger(DropshipStoreLookupHandler.class.getName());
}
