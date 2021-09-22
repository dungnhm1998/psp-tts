package asia.leadsgen.psp.server.handler.dropship.dashboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.server.handler.etsy.EtsyConnectHandler;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class GetCatalogDetailHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				LOGGER.info("GetCatalogDetailHandler Start >>> " );
				
				String baseId = routingContext.request().params().get(AppParams.ID);
				
				if (StringUtils.isEmpty(baseId)) {
					throw new BadRequestException(SystemError.INVALID_BASE_ID); 
				}
				
				List<Map> resultSearch = BaseService.getCatalogDetail(baseId);
				
				Map responseData = formatData(resultSearch);
				
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
	
	private Map formatData(List<Map> resultSearch) {
		Map result = new LinkedHashMap<>();
		if (CollectionUtils.isNotEmpty(resultSearch)) {

			result.put(AppParams.ID, ParamUtil.getString(resultSearch.get(0), AppParams.S_ID));
			result.put(AppParams.NAME, ParamUtil.getString(resultSearch.get(0), AppParams.S_NAME));
			result.put(AppParams.HTML_DESC, ParamUtil.getString(resultSearch.get(0), AppParams.S_HTML_DESC));
			result.put(AppParams.IMAGE, ParamUtil.getString(resultSearch.get(0), AppParams.S_BASE_MOCKUP));
			result.put(AppParams.SHIPPING_LINES, ParamUtil.getString(resultSearch.get(0), AppParams.S_SHIPPING_LINES));
			result.put(AppParams.PROCESSING_TIME, ParamUtil.getString(resultSearch.get(0), AppParams.S_PROCESSING_TIME));
//			result.put(AppParams.BASE_COST, ParamUtil.getString(resultSearch.get(0), AppParams.S_BASE_COST));
			result.put(AppParams.CURRENCY, ParamUtil.getString(resultSearch.get(0), AppParams.S_CURRENCY));
			result.put(AppParams.SHIPPING_TIME_US, ParamUtil.getString(resultSearch.get(0), AppParams.S_SHIPPING_TIME_US));
			result.put(AppParams.SHIPPING_COST_US, ParamUtil.getString(resultSearch.get(0), AppParams.S_SHIPPING_COST_US));
			result.put(AppParams.SHIPPING_TIME_WW, ParamUtil.getString(resultSearch.get(0), AppParams.S_SHIPPING_TIME_WW));
			result.put(AppParams.SHIPPING_COST_WW, ParamUtil.getString(resultSearch.get(0), AppParams.S_SHIPPING_COST_WW));
			
			List<Map> lstBaseSku = new ArrayList<>();
			for (Map record : resultSearch) {
				Map baseSku = new LinkedHashMap<>();
				baseSku.put(AppParams.SIZE_ID, ParamUtil.getString(record, AppParams.S_SIZE_ID));
				baseSku.put(AppParams.SIZE_NAME, ParamUtil.getString(record, AppParams.S_SIZE_NAME)); 
				baseSku.put(AppParams.COLOR_ID, ParamUtil.getString(record, AppParams.S_COLOR_ID));
				baseSku.put(AppParams.COLOR_NAME, ParamUtil.getString(record, AppParams.S_COLOR_NAME));
				baseSku.put(AppParams.SKU, ParamUtil.getString(record, AppParams.S_SKU));
				baseSku.put(AppParams.BASE_COST, ParamUtil.getString(record, AppParams.S_BASE_COST));
				lstBaseSku.add(baseSku);
			}
			
			result.put("base_sku", lstBaseSku);
		}
		
		return result;
	}

	private static final Logger LOGGER = Logger.getLogger(EtsyConnectHandler.class.getName());
	
}
