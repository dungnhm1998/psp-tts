package asia.leadsgen.psp.server.handler.dropship.store.campaign;

import java.util.LinkedHashMap;
import java.util.logging.Logger;

import asia.leadsgen.psp.server.handler.dropship.store.DropshipStoreLookupHandler;
import asia.leadsgen.psp.service_fulfill.DropShipStoreCampService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipCampaignReUploadHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			try {
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

				JsonObject requestBody = routingContext.getBodyAsJson();
				String storeId = requestBody.getString(AppParams.STORE_ID);

				DataAccessSecurer.secureDropshipStoreV2(userId, storeId);
				
				String campaignId = requestBody.getString(AppParams.CAMPAIGN_ID);

				DropShipStoreCampService.updateState(storeId, campaignId,
						asia.leadsgen.psp.util.ResourceStates.CREATED);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, new LinkedHashMap<>());

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
