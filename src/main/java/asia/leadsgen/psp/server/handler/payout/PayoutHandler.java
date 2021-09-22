package asia.leadsgen.psp.server.handler.payout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.AuthorizationException;
import asia.leadsgen.psp.service.PayoutService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class PayoutHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {

				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				String type = routingContext.request().getParam(AppParams.TYPE);
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
				
				int page = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE), 1);
				int pageSize = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE_SIZE), 10);
				
				Map payoutProfit, payoutInfoMap, payoutPayment; 

				if (!StringUtils.isEmpty(userId)) {
					payoutInfoMap = PayoutService.get(userId, type, startDate, endDate, page, pageSize);
					payoutProfit = PayoutService.get(userId, AppParams.PROFIT, "", "", page, pageSize);
					payoutPayment = PayoutService.get(userId, AppParams.PAYOUT, "", "", page, pageSize);
					
					double availableBalance = GetterUtil.getDouble(ParamUtil.getString(payoutProfit, AppParams.TOTAL_AMOUNT)) - GetterUtil.getDouble(ParamUtil.getString(payoutPayment, AppParams.TOTAL_AMOUNT));
					double acountBalance = GetterUtil.getDouble(ParamUtil.getString(payoutInfoMap, AppParams.TOTAL_PROFIT));
//					double refBalance = GetterUtil.getDouble(ParamUtil.getString(payoutProfit, AppParams.TOTAL_AMOUNT_REF));
					double totalPayoutAmount = GetterUtil.getDouble(ParamUtil.getString(payoutPayment, AppParams.TOTAL_AMOUNT));
//					double currentBalance = acountBalance - totalAmount + refBalance;
					double currentBalance = acountBalance - totalPayoutAmount;
					
					payoutInfoMap.put(AppParams.AVAILABLE_BALANCE, availableBalance);
					payoutInfoMap.put(AppParams.CURRENT_BALANCE, currentBalance);
					payoutInfoMap.put(AppParams.ACCOUNT_BALANCE, acountBalance);
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, payoutInfoMap);

					future.complete();
				} else {
					throw new AuthorizationException(SystemError.INVALID_USER);
				}

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

	private static final Logger LOGGER = Logger.getLogger(PayoutHandler.class.getName());
}
