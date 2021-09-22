package asia.leadsgen.psp.server.handler.dropship.product;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.server.handler.etsy.EtsyConnectHandler;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.ext.web.RoutingContext;

public class GetTopSellingProductHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				
				LOGGER.info("GetTopSellingProductHandler Start >>> " );
				
				MultiMap requestParams = routingContext.request().params();
				
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				
				if (StringUtils.isEmpty(userId)) {
					throw new LoginException(SystemError.LOGIN_REQUIRED);
				}
				
				String storeId = Optional.ofNullable(requestParams.get(AppParams.STORE_ID)).orElse(StringPool.BLANK);
				
				String listStoreId = StringPool.BLANK;
				
				boolean storeValid = true;
				if (StringUtils.isNotEmpty(storeId)) {
					DataAccessSecurer.secureDropshipStoreV2(userId, storeId);
					DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);
					listStoreId = storeId;
				} else {
					
					Boolean isOwner = ContextUtil.getBoolean(routingContext, AppParams.OWNER);
					
					if (!isOwner) {
						//sub account
						List<String> listAccessStoreId = ContextUtil.getListData(routingContext, AppParams.STORES);
						if (listAccessStoreId.isEmpty()) {
							LOGGER.info("Subaccount's store is empty");
							storeValid = false;
						} else {
							listStoreId = listAccessStoreId.stream().collect(Collectors.joining(","));
						}
					}
				}
				
				Map resultData = new LinkedHashMap();
				
				if (storeValid) {
					String startDate = requestParams.get(AppParams.START_DATE);
					String endDate = requestParams.get(AppParams.END_DATE);
					
					if (StringUtils.isNotEmpty(startDate) || StringUtils.isNotEmpty(endDate)) {
						
						String requestTimeZone = routingContext.request().getParam(AppParams.TIMEZONE);
						if (StringUtils.isEmpty(requestTimeZone)) {
							throw new BadRequestException(new SystemError("INVALID_REQUEST", "Timezone required !", "",
									"http://developer.30usd.com/errors/400.html"));
						}
						
						Calendar calendar = Calendar.getInstance();
						DateFormat df = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);
						String timezone = DateTimeUtil.formatTimezone(requestTimeZone);
						df.setTimeZone(TimeZone.getTimeZone(timezone));
						
						if (StringUtils.isNotEmpty(startDate)) {
							calendar.setTimeInMillis(Long.valueOf(startDate));
							startDate = df.format(calendar.getTime());
						}

						if (StringUtils.isNotEmpty(endDate)) {
							calendar.setTimeInMillis(Long.valueOf(endDate));
							endDate = df.format(calendar.getTime());
						}
					}
					
					int page = GetterUtil.getInteger(requestParams.get(AppParams.PAGE), 1);
					int pageSize = GetterUtil.getInteger(requestParams.get(AppParams.PAGE_SIZE), 10);
					
					String search = requestParams.get(AppParams.SEARCH);
					
					resultData = DropshipOrderProductService.getTopSellingProduct(userId,listStoreId, search, startDate, endDate, page, pageSize);
				}
				
				Map responseData = new LinkedHashMap<>();
				responseData.put("total", ParamUtil.getInt(resultData, AppParams.RESULT_TOTAL));
				responseData.put("data", format(ParamUtil.getListData(resultData, AppParams.RESULT_DATA)));
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, responseData);
				
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
	
	
	private static List<Map> format(List<Map> rawData) {
		List<Map> listResult = new ArrayList<Map>();
		for (Map raw : rawData) {
			Map result = new LinkedHashMap();
			result.put("product_name", ParamUtil.getString(raw, "PRODUCT_NAME"));
			result.put("total_order", ParamUtil.getInt(raw, "TOTAL_ORDER"));
			result.put("total_item", ParamUtil.getInt(raw, "TOTAL_ITEM"));
			result.put("total_amount", ParamUtil.getString(raw, "TOTAL_AMOUNT"));
			result.put("currency", ParamUtil.getString(raw, "CURRENCY"));
			listResult.add(result);
		}
		return listResult;
	}
	
	private static final Logger LOGGER = Logger.getLogger(EtsyConnectHandler.class.getName());
}
