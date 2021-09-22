
package asia.leadsgen.psp.server.handler.dropship.store;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipApiStoreUpdateHandler implements Handler<RoutingContext> {

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
				String storeId = routingContext.request().params().get("id");
				if (StringUtils.isEmpty(storeId)) {
					throw new BadRequestException(SystemError.INVALID_STORE);
				}

				DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);
				
				Map store = DropShipStoreService.lookUp(storeId);

				if (store == null || store.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_STORE);
				}

				String currentChannel = ParamUtil.getString(store, AppParams.CHANNEL);
				String currentDOMAIN = ParamUtil.getString(store, AppParams.DOMAIN);

				String channel = ParamUtil.getString(requestBody, "channel");
				if (!"api".equalsIgnoreCase(channel)) {
					throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
				}

				if (!StringUtils.equals(channel, currentChannel)) {
					throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
				}

				String name = ParamUtil.getString(requestBody, "name");
				if (StringUtils.isEmpty(name)) {
					throw new BadRequestException(SystemError.STORE_NAME_CAN_NOT_BE_EMPTY);
				}

				String website = ParamUtil.getString(requestBody, "domain");

				Map dsStore;
				String currentName = ParamUtil.getString(store, AppParams.NAME);
				if (StringUtils.equals(currentName, name) && StringUtils.equals(currentDOMAIN, website)) {
					dsStore = store;
				} else {
					if (!StringUtils.equals(currentName, name)) {
						boolean isDuplicateStore = DropShipStoreService.isDuplicateStore(userId, "api", name);
						if (isDuplicateStore) {
							throw new BadRequestException(SystemError.DUPLICATE_DROPSHIP_STORE);
						}
					}
					dsStore = DropShipStoreService.update(storeId, name, website, "", "", "", "");
				}

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

	private static final Logger LOGGER = Logger.getLogger(DropshipApiStoreUpdateHandler.class.getName());
}
