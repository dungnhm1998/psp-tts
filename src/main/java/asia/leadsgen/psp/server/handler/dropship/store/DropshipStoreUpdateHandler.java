package asia.leadsgen.psp.server.handler.dropship.store;

import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.shopify.service.ShopifyAPISetupCheck;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipStoreUpdateHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			try {
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				String storeId = routingContext.request().params().get("id");
				
				DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);
				
				Map databaseStore = DropShipStoreService.lookUp(storeId);
				
				Map requestBody = routingContext.getBodyAsJson().getMap();

				String currentApiKey = ParamUtil.getString(databaseStore, "api_key");
				String currentApiPassword = ParamUtil.getString(databaseStore, "api_password");
				String currentState = ParamUtil.getString(databaseStore, "state");

				String name = ParamUtil.getString(requestBody, "name");
				String apiKey = ParamUtil.getString(requestBody, "api_key");
				String apiPassword = ParamUtil.getString(requestBody, "api_password");
				String sharedSecret = ParamUtil.getString(requestBody, "shared_secret");
				String domain = ParamUtil.getString(requestBody, "domain");

				if (currentApiKey != apiKey || currentApiPassword != apiPassword) {
					currentState = ResourceStates.CREATED;
				}

				Map dsStore = DropShipStoreService.update(storeId, name, domain, apiKey, apiPassword, sharedSecret,
						currentState);

				Map productCheckMap = ShopifyAPISetupCheck.checkProductsEndpoints(apiKey, apiPassword, domain);
				boolean canAccessProduct = ParamUtil.getBoolean(productCheckMap, "access");

				if (canAccessProduct) {

					Map orderCheckMap = ShopifyAPISetupCheck.checkReadOrders(apiKey, apiPassword, domain);
					boolean canAccessOrder = ParamUtil.getBoolean(orderCheckMap, "access");

					if (canAccessOrder) {
						dsStore = DropShipStoreService.updateStoreState(storeId, ResourceStates.APPROVED);
					}
				}

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, dsStore);

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

	private static final Logger LOGGER = Logger.getLogger(DropshipStoreUpdateHandler.class.getName());
}
