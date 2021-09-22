
package asia.leadsgen.psp.server.handler.dropship.store;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.shopify.service.ShopifyAPISetupCheck;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipStoreCreateHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		 String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

		Map requestBody = routingContext.getBodyAsJson().getMap();
		LOGGER.info(requestBody.toString());

		String name = ParamUtil.getString(requestBody, "name");
		String apiKey = ParamUtil.getString(requestBody, "api_key");
		String apiPassword = ParamUtil.getString(requestBody, "api_password");
		String sharedSecret = ParamUtil.getString(requestBody, "shared_secret");
		String domain = ParamUtil.getString(requestBody, "domain");
		String channel = ParamUtil.getString(requestBody, "channel");

		if (!channel.equalsIgnoreCase("shopify")) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		routingContext.vertx().executeBlocking(future -> {
			try {

				Map dsStore = setupShopify(userId, name, apiKey, apiPassword, sharedSecret, domain, channel);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
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

	private Map setupShopify(String userId, String name, String apiKey, String apiPassword, String sharedSecret,
			String domain, String channel) throws UnirestException, SQLException {

		if (name.isEmpty() || apiKey.isEmpty() || apiPassword.isEmpty() || domain.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		Map dsStore = DropShipStoreService.addStore(userId, "shopify", name, domain, apiKey, apiPassword, sharedSecret, "", ResourceStates.CREATED);
		Map productCheckMap = ShopifyAPISetupCheck.checkProductsEndpoints(apiKey, apiPassword, domain);
		boolean canAccessProduct = ParamUtil.getBoolean(productCheckMap, "access");

		String storeId = ParamUtil.getString(dsStore, AppParams.ID);

		if (canAccessProduct) {
			Map orderCheckMap = ShopifyAPISetupCheck.checkReadOrders(apiKey, apiPassword, domain);
			boolean canAccessOrder = ParamUtil.getBoolean(orderCheckMap, "access");
			if (canAccessOrder) {
				dsStore = DropShipStoreService.updateStoreState(storeId, "approved");
			}
		}
		return dsStore;

	}

	private static final Logger LOGGER = Logger.getLogger(DropshipStoreCreateHandler.class.getName());
}
