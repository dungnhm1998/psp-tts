package asia.leadsgen.psp.server.handler.dropship.store;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.HashedMap;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.Common;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropShipStoreSearchHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			try {
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				MultiMap params =  routingContext.request().params();
				
				String state = params.contains("state") ? params.get("state") : "";
				String channel = params.contains("channel") ? params.get("channel") : "";
				String clientId = params.contains("client_id") ? params.get("client_id") : "";
				
				String search = params.contains("search") ? params.get("search") : StringPool.BLANK;
				
				int page = GetterUtil.getInteger(params.get(AppParams.PAGE), 1);
				int pageSize = GetterUtil.getInteger(params.get(AppParams.PAGE_SIZE), 10);
				
//				Map searchResult = DropShipStoreService.searchStores(userId, channel, state, clientId,search,page,pageSize);
				Map searchResult = null;
				
				Boolean isOwner = routingContext.get(AppParams.OWNER); 
				
				String lstStoreIds = "";
				boolean dbSearch = true;
				if (!isOwner) {
					List<String> lstStoreAccess = ContextUtil.getListData(routingContext, AppParams.STORES);
					if (CollectionUtils.isEmpty(lstStoreAccess)) {
						searchResult = new HashedMap<>();
						searchResult.put(AppParams.DATA, Collections.EMPTY_LIST);
						searchResult.put(AppParams.TOTAL, 0);
						dbSearch = false;
					} else {
						lstStoreIds = lstStoreAccess.stream().collect(Collectors.joining(","));
					}
				}
				
				if (dbSearch) {
					searchResult = DropShipStoreService.searchStores(userId, channel, lstStoreIds, state, clientId,search,page,pageSize);
				}
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, searchResult);

			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "[ERROR]", e);
				routingContext.fail(e.getCause());
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

	private static final Logger LOGGER = Logger.getLogger(DropShipStoreSearchHandler.class.getName());
}
