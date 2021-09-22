package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import asia.leadsgen.psp.exception.LoginException;
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

public class DropshipOrderSearchTrackingHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			MultiMap requestParams = routingContext.request().params();
			
			LOGGER.info("userId= " + userId);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}

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
				List<String> listAccessStoreId = ContextUtil.getListData(routingContext, AppParams.STORES);
				Map orderSearchTrackingResult = new HashedMap<>();
				
				if (isOwner) {
					orderSearchTrackingResult = DropshipOrderService.searchTrackingOrder(userId, storeId, channel, state, startDate, endDate, page, pageSize, sort, dir, orderId, source);
				} else if (!isOwner && !listAccessStoreId.isEmpty()) {
					String listStore = "";
					if (StringUtils.isEmpty(storeId)) {
						listStore = listAccessStoreId.stream().collect(Collectors.joining(","));
						orderSearchTrackingResult = DropshipOrderService.searchTrackingOrderSubAccount(userId, listStore, channel, state, startDate, endDate, page, pageSize, sort, dir, orderId, source);
						
					} else if (listAccessStoreId.contains(storeId)) {
						listStore = storeId;
						orderSearchTrackingResult = DropshipOrderService.searchTrackingOrderSubAccount(userId, listStore, channel, state, startDate, endDate, page, pageSize, sort, dir, orderId, source);
					}
				}
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, orderSearchTrackingResult);
				
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
	
	private static final Logger LOGGER = Logger.getLogger(DropshipOrderSearchTrackingHandler.class.getName());
}
