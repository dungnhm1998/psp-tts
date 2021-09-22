package asia.leadsgen.psp.server.handler.dropship.store;

import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreCampService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.GetterUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipStoreCampListHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			try {
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

				String storeId = routingContext.request().params().get(AppParams.ID);

				String title = routingContext.request().getParam(AppParams.TITLE);
				title = title.trim();
				int index = title.lastIndexOf("/");
				if (index > -1) {
					title = title.substring(index + 1);
				}
				String campaignId = routingContext.request().getParam(AppParams.CAMPAIGN_ID);
				
				DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);
				
				Map store = DropShipStoreService.getStoreApprovedAndDisconnectedById(storeId);
				if (store.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
				}
				
				int page = GetterUtil.getInteger(routingContext.request().params().get(AppParams.PAGE), 1);
				int pageSize = GetterUtil.getInteger(routingContext.request().params().get(AppParams.PAGE_SIZE), 10);

				int uploadable = GetterUtil.getInteger(routingContext.request().params().get(AppParams.UPLOADABLE), -1);

				Map campaigns = DropShipStoreCampService.listCampaigns(campaignId, title, userId, storeId, page,
						pageSize, uploadable);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, campaigns);

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

	private static final Logger LOGGER = Logger.getLogger(DropshipStoreCampListHandler.class.getName());
}
