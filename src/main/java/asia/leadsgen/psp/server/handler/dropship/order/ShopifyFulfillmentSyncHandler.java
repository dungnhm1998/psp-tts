package asia.leadsgen.psp.server.handler.dropship.order;

import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.shopify.service.ShopifyFulfillmentService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyFulfillmentSyncHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking(future -> {

			try {

				Map fulfillmentUpdateRequest = routingContext.getBodyAsJson().getMap();

				LOGGER.info("job request = " + fulfillmentUpdateRequest.toString());

				String domain = ParamUtil.getString(fulfillmentUpdateRequest, AppParams.DOMAIN);
				String token = ParamUtil.getString(fulfillmentUpdateRequest, AppParams.TOKEN);
				String shopifyOrderId = ParamUtil.getString(fulfillmentUpdateRequest, AppParams.SHOPIFY_ORDER_ID);
				String trackingNumber = ParamUtil.getString(fulfillmentUpdateRequest, AppParams.TRACKING_CODE);
				String trackingUrl = ParamUtil.getString(fulfillmentUpdateRequest, AppParams.TRACKING_URL);
				String locationId = ParamUtil.getString(fulfillmentUpdateRequest, AppParams.LOCATION_ID);
				String itemId = ParamUtil.getString(fulfillmentUpdateRequest, AppParams.LINE_ITEM_ID);

				Map updateResult = ShopifyFulfillmentService.updateFulfillment(domain, token, shopifyOrderId,
						locationId, trackingNumber, trackingUrl, itemId);

				routingContext.put(AppParams.RESPONSE_CODE, ParamUtil.getInt(updateResult, AppParams.RESPONSE_CODE));
				routingContext.put(AppParams.RESPONSE_MSG, ParamUtil.getInt(updateResult, AppParams.RESPONSE_MSG));
				routingContext.put(AppParams.RESPONSE_DATA, ParamUtil.getMapData(updateResult, AppParams.FULFILLMENT));

				future.complete();
			} catch (Exception e) {
				routingContext.fail(e);
			}
		}, asyncResult ->

		{
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	private static final Logger LOGGER = Logger.getLogger(ShopifyFulfillmentSyncHandler.class.getName());
}
