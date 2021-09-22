
package asia.leadsgen.psp.server.handler.dropship.store;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipApiStoreCreateHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
		if (userId.isEmpty()) {
			throw new BadRequestException(SystemError.LOGIN_REQUIRED);
		}
		Map requestBody = routingContext.getBodyAsJson().getMap();
		LOGGER.info(requestBody.toString());

		routingContext.vertx().executeBlocking(future -> {
			try {
				String channel = ParamUtil.getString(requestBody, "channel");
				if (!channel.equalsIgnoreCase("api")) {
					throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
				}
				Map dsStore = Collections.EMPTY_MAP;
				String name = ParamUtil.getString(requestBody, "name");
				String website = ParamUtil.getString(requestBody, "domain");
				dsStore = setUpApiStore(userId, name, website);
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

	private Map setUpApiStore(String userId, String name, String website) throws Exception {
		if (StringUtils.isEmpty(name)) {
			throw new BadRequestException(SystemError.STORE_NAME_CAN_NOT_BE_EMPTY);
		}
		String apiKey = "";
		boolean isDuplicateStore = DropShipStoreService.isDuplicateStore(userId, "api", name);
		if (isDuplicateStore) {
			throw new BadRequestException(SystemError.DUPLICATE_DROPSHIP_STORE);
		}
		boolean isDuplicateAPiKey = true;
		while (isDuplicateAPiKey) {
			isDuplicateAPiKey = DropShipStoreService.isExistThisApiKey(apiKey);
			apiKey = UUID.randomUUID().toString();
		}
		Map dsStore = DropShipStoreService.addStore(userId, "api", name, website, apiKey, "", "", "",
				ResourceStates.APPROVED);
		return dsStore;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipApiStoreCreateHandler.class.getName());
}
