package asia.leadsgen.psp.server.handler.shopify_app.webhook;

import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.util.AppParams;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class WebhookShopifyRedactHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking((Future<Object> future) -> {

			try {
				LOGGER.info("WebhookShopifyRedactHandler() - requestBodyMap= " + routingContext.getBodyAsString());
				Map map = ShopifyAppService.insertGrpcWebhooks(routingContext.getBodyAsString());
				LOGGER.info("map= " + map.toString());

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
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

	private static final Logger LOGGER = Logger.getLogger(WebhookShopifyRedactHandler.class.getName());

}
