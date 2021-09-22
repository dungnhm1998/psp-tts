package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.obj.DropshipStoreObj;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderSearchV2Handler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			String userId, storeId = "";

			MultiMap requestParams = routingContext.request().params();
			String apiKey = requestParams.contains(AppParams.API_KEY) ? requestParams.get(AppParams.API_KEY) : "";
			LOGGER.info("apiKey= " + apiKey);

			if (StringUtils.isEmpty(apiKey)) {

				userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

				LOGGER.info("userId= " + userId);
				if (StringUtils.isEmpty(userId)) {
					throw new LoginException(SystemError.LOGIN_REQUIRED);
				}

				storeId = requestParams.contains(AppParams.STORE_ID) ? requestParams.get(AppParams.STORE_ID) : "";
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

			} else {

				DropshipStoreObj storeObj = null;
				try {
					storeObj = DropShipStoreService.findByApiKey(apiKey);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				if (storeObj == null) {
					throw new BadRequestException(SystemError.INVALID_AUTH_TOKEN);
				}
				userId = storeObj.getUserId();
//				storeId = storeObj.getId();
			}

			try {

				String channel = requestParams.contains(AppParams.CHANNEL) ? requestParams.get(AppParams.CHANNEL) : "";
				String state = requestParams.contains(AppParams.STATE) ? requestParams.get(AppParams.STATE) : "";

				String startDate = routingContext.request().getParam(AppParams.START_DATE);
				String endDate = routingContext.request().getParam(AppParams.END_DATE);

				Calendar affCal = Calendar.getInstance();
				DateFormat df = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);
				String timezone = DateTimeUtil.formatTimezone(routingContext.request().getParam(AppParams.TIMEZONE));
				df.setTimeZone(TimeZone.getTimeZone(timezone));

				if (StringUtils.isNotEmpty(startDate)) {
					affCal.setTimeInMillis(Long.valueOf(startDate));
					startDate = df.format(affCal.getTime());
				}

				if (StringUtils.isNotEmpty(endDate)) {
					affCal.setTimeInMillis(Long.valueOf(endDate));
					endDate = df.format(affCal.getTime());
				}

				String source = requestParams.contains(AppParams.SOURCE) ? requestParams.get(AppParams.SOURCE) : "";

				String orderId = routingContext.request().getParam(AppParams.ID);
				int page = GetterUtil.getInteger(requestParams.get(AppParams.PAGE), 1);
				int pageSize = GetterUtil.getInteger(requestParams.get(AppParams.PAGE_SIZE), 10);
				String sort = requestParams.contains(AppParams.SORT) ? requestParams.get(AppParams.SORT) : "";
				String dir = requestParams.contains(AppParams.DIR) ? requestParams.get(AppParams.DIR) : "";

				Boolean isOwner = ContextUtil.getBoolean(routingContext, AppParams.OWNER);
				LOGGER.info("--isOwner= "+isOwner);
				List<String> listAccessStoreId = ContextUtil.getListData(routingContext, AppParams.STORES);

				Map orderSearchResult = new HashedMap<>();

				if (isOwner) {
					orderSearchResult = DropshipOrderService.searchOrderV2(userId, storeId, channel, state, startDate, endDate, page, pageSize, sort, dir, orderId, source);
				} else if (!isOwner && !listAccessStoreId.isEmpty()) {
					String listStore = "";
					if (StringUtils.isEmpty(storeId)) {

						listStore = listAccessStoreId.stream().collect(Collectors.joining(","));

						orderSearchResult = DropshipOrderService.searchOrderSubAccount(userId, listStore, channel, state, startDate, endDate, page, pageSize, sort, dir, orderId, source);
					} else if (listAccessStoreId.contains(storeId)) {
						listStore = storeId;
						orderSearchResult = DropshipOrderService.searchOrderSubAccount(userId, listStore, channel, state, startDate, endDate, page, pageSize, sort, dir, orderId, source);
					}
				}else{
					List<Map> formatList = new ArrayList();
					orderSearchResult.put(AppParams.TOTAL, 0);
					orderSearchResult.put(AppParams.DATA, formatList);
					orderSearchResult.put("all_orders", 0);
					orderSearchResult.put("fulfilled", 0);
					orderSearchResult.put("unfulfilled", 0);
				}

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, orderSearchResult);

			} catch (Exception e) {

				LOGGER.log(Level.INFO, "[ERROR]", e);
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

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderSearchV2Handler.class.getName());

}
