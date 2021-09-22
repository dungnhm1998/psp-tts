package asia.leadsgen.psp.server.handler.dropship.collection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.shopify.service.ShopifyCollectionService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipCollectionSearchHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
		String storeId = routingContext.request().params().get("id");

		routingContext.vertx().executeBlocking(future -> {
			try {

				DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);
				
				Map storeSearchResult = DropShipStoreService.lookUp(storeId);
				if (storeSearchResult.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
				}

				String channel = ParamUtil.getString(storeSearchResult, "channel");
				int page = GetterUtil.getInteger(routingContext.request().params().get(AppParams.PAGE), 1);
				int pageSize = GetterUtil.getInteger(routingContext.request().params().get(AppParams.PAGE_SIZE), 10);
				Map searchResult = new LinkedHashMap<>();
				if (channel.equalsIgnoreCase("shopify")) {
					String apiKey = ParamUtil.getString(storeSearchResult, "api_key");
					String domain = ParamUtil.getString(storeSearchResult, "domain");
					searchResult = ShopifyCollectionService.getCollections(apiKey, domain, page, pageSize);
				}
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, searchResult);

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

	private static final Logger LOGGER = Logger.getLogger(DropshipCollectionSearchHandler.class.getName());
}
