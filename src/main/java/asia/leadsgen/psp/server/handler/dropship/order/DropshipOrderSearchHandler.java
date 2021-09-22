package asia.leadsgen.psp.server.handler.dropship.order;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.GetterUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderSearchHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {
			try {
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

				MultiMap requestParams = routingContext.request().params();
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
				
				String storeId = requestParams.contains(AppParams.STORE_ID) ? requestParams.get(AppParams.STORE_ID)
						: "";
				String orderId = routingContext.request().getParam(AppParams.ID);
				int page = GetterUtil.getInteger(requestParams.get(AppParams.PAGE), 1);
				int pageSize = GetterUtil.getInteger(requestParams.get(AppParams.PAGE_SIZE), 10);
				String sort = requestParams.contains(AppParams.SORT) ? requestParams.get(AppParams.SORT) : "";
				String dir = requestParams.contains(AppParams.DIR) ? requestParams.get(AppParams.DIR) : "";

				Map orderSearchResult = DropshipOrderService.searchOrder(userId, storeId, channel, state, startDate,
						endDate, page, pageSize, sort, dir, orderId);
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, orderSearchResult);

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

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderSearchHandler.class.getName());

}
