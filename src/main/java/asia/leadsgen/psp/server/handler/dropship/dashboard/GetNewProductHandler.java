package asia.leadsgen.psp.server.handler.dropship.dashboard;

import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.util.AppParams;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class GetNewProductHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				
				Map newProduct = BaseService.getNewProductCache();
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, newProduct);
				
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
	
	private static final Logger LOGGER = Logger.getLogger(GetNewProductHandler.class.getName());

}