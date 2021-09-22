package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderQueuedPayallHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
		LOGGER.info("userId= " + userId);
		if (userId.isEmpty()) {
			throw new LoginException(SystemError.LOGIN_REQUIRED);
		}

		MultiMap requestParams = routingContext.request().params();

		String storeId = requestParams.contains(AppParams.STORE_ID) ? requestParams.get(AppParams.STORE_ID) : "";
		LOGGER.info("storeId= " + storeId);

		if (storeId != "") {
			Map storeResult = null;
			try {
				storeResult = DropShipStoreService.getStoreApprovedAndDisconnectedById(storeId);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			String storeUserId = ParamUtil.getString(storeResult, AppParams.USER_ID);
			LOGGER.info("storeUserId= " + storeUserId);
			if (!storeUserId.equalsIgnoreCase(userId)) {
				throw new LoginException(SystemError.INVALID_USER);
			}
		}

		routingContext.vertx().executeBlocking(future -> {

			try {

				Boolean isOwner = ContextUtil.getBoolean(routingContext, AppParams.OWNER);
				List<String> listAccessStoreId = ContextUtil.getListData(routingContext, AppParams.STORES);
				Map payallQueuedOrderMap = new HashedMap<>();
				if (isOwner) {
					payallQueuedOrderMap = DropshipOrderService.payallQueuedOrder(userId, storeId);
				} else if (!isOwner && !listAccessStoreId.isEmpty()) {
					String listStore = "";
					if (StringUtils.isEmpty(storeId)) {

						listStore = listAccessStoreId.stream().collect(Collectors.joining(","));

						payallQueuedOrderMap = DropshipOrderService.payallQueuedOrderSubAccount(userId, listStore);
					} else if (listAccessStoreId.contains(storeId)) {
						listStore = storeId;
						payallQueuedOrderMap = DropshipOrderService.payallQueuedOrderSubAccount(userId, listStore);
					}
				}

//				Map payallQueuedOrderMap = DropshipOrderService.payallQueuedOrder(userId, storeId);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, payallQueuedOrderMap);

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

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderQueuedPayallHandler.class.getName());

}
