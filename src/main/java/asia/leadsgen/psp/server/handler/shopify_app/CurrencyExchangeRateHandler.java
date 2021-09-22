package asia.leadsgen.psp.server.handler.shopify_app;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;


import asia.leadsgen.psp.service_fulfill.ExchangeRateService;
import asia.leadsgen.psp.util.AppParams;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class CurrencyExchangeRateHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				final String date = routingContext.request().params().get("date");
				final String fromCurrency = routingContext.request().params().get("from_currency");
				final String toCurrency = routingContext.request().params().get("to_currency");

//				ExchangeRateService.syncDataFromFixerIO();
				
				Map result = new LinkedHashMap();
				String rate = ExchangeRateService.getRate(toCurrency);
				result.put("rate", Double.parseDouble(rate));
//				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                routingContext.put(AppParams.RESPONSE_DATA, result);
                
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
	
	private static final Logger LOGGER = Logger.getLogger(CurrencyExchangeRateHandler.class.getName());
}
