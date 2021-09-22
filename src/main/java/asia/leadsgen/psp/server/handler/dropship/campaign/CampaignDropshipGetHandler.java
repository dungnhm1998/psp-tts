package asia.leadsgen.psp.server.handler.dropship.campaign;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.GetterUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.ext.web.RoutingContext;

public class CampaignDropshipGetHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				MultiMap requestParams = routingContext.request().params();
				
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				LOGGER.info("userId= " + userId);
				if (StringUtils.isEmpty(userId)) {
					throw new LoginException(SystemError.LOGIN_REQUIRED);
				}
				
				String domainName = requestParams.contains(AppParams.DOMAIN_NAME) ? requestParams.get(AppParams.DOMAIN_NAME) : "";
				
				String title = requestParams.contains(AppParams.TITLE) ? requestParams.get(AppParams.TITLE) : "";
				title = title.trim();
				int index = title.lastIndexOf("/");
				if (index > -1) {
					title = title.substring(index + 1);
				}
				
				String startDate = requestParams.get(AppParams.START_DATE);
				String endDate = requestParams.get(AppParams.END_DATE);
				
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
				
				String state = requestParams.contains(AppParams.STATE) ? requestParams.get(AppParams.STATE) : AppParams.DROPSHIP;
				
				int page = GetterUtil.getInteger(requestParams.get(AppParams.PAGE), 1);
				int pageSize = GetterUtil.getInteger(requestParams.get(AppParams.PAGE_SIZE), 10);
				
				String orderby = requestParams.contains(AppParams.SORT) ? requestParams.get(AppParams.SORT) : "";
				String orderDriection = requestParams.contains(AppParams.DIR) ? requestParams.get(AppParams.DIR) : "";
				
				boolean isOwner = false;
				isOwner = routingContext.get(AppParams.OWNER);

				if (!isOwner && StringUtils.isEmpty(domainName)) {
					Set<String> accessibleDomains = routingContext.get(AppParams.DOMAINS);
					domainName = String.join(",", accessibleDomains);
				}
				
				Map campaignSearchResultMap = new LinkedHashMap();
				campaignSearchResultMap = CampaignService.getCampaignDropship(userId, domainName, title, startDate, endDate, state, page, pageSize, orderby, orderDriection, isOwner);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, campaignSearchResultMap);

				future.complete();
				
			}catch (Exception e) {
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
	
	private static final Logger LOGGER = Logger.getLogger(CampaignDropshipGetHandler.class.getName());
}