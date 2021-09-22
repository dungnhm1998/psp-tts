package asia.leadsgen.psp.server.handler.dropship.dashboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class GetCatalogsHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				
				LOGGER.info("GetTopSellingProductHandler Start >>> " );
				
				String searchCatalogId = routingContext.request().getParam("search_catalog_id");
				String search = routingContext.request().getParam("search");
				
				List<Map> resultSearch = BaseService.getListCatalogs();
				List<Map> result = formatList(resultSearch,searchCatalogId, search);
				
				Map responseData = new LinkedHashMap<>();
				responseData.put(AppParams.DATA, result);
				
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
	
	@SuppressWarnings("unchecked")
	private List<Map> formatList(List<Map> resultSearch, String searchCatalogId, String search) {
		List<Map> lstFormat = new ArrayList<Map>();
		for (Map record : resultSearch) {
			lstFormat.add(formatRecord(record));
		}
		
		Map<String, List<Map>> groupByBaseGroup = lstFormat.stream().collect(Collectors.groupingBy(e -> ParamUtil.getString(e, CATALOG_ID)));
		
		List<Map> result = new ArrayList<Map>();
		for (Map.Entry<String,List<Map>> entry : groupByBaseGroup.entrySet()) {
			Map groupBase = new LinkedHashMap<>();
			groupBase.put(CATALOG_NAME, ParamUtil.getString(entry.getValue().get(0), CATALOG_NAME));
			groupBase.put(AppParams.POSITION, ParamUtil.getString(entry.getValue().get(0), AppParams.POSITION));
			groupBase.put(CATALOG_ID, entry.getKey());
			
			List<Map> bases = entry.getValue();
			
			if (StringUtils.isNotEmpty(search)) {
				bases = bases.stream().filter(e -> ParamUtil.getString(e, AppParams.BASE_NAME).toLowerCase().contains(search.toLowerCase())).collect(Collectors.toList());
			}
			
			groupBase.put(AppParams.TOTAL, bases.size());
			
			groupBase.put("bases", bases);
			result.add(groupBase);
		}
		
		result.sort(Comparator.comparing(e -> ParamUtil.getString(e, AppParams.POSITION)));
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private Map formatRecord(Map record) {
		Map rs = new LinkedHashMap<>();
		rs.put(CATALOG_ID, ParamUtil.getString(record, "S_CATALOG_ID"));
		rs.put(AppParams.POSITION, ParamUtil.getString(record, AppParams.N_POSITION));
		rs.put(CATALOG_NAME, ParamUtil.getString(record, "S_CATALOG_NAME"));
		rs.put(AppParams.BASE_ID	, ParamUtil.getString(record, AppParams.S_ID));
		rs.put(AppParams.BASE_NAME, ParamUtil.getString(record, AppParams.S_NAME));
		rs.put("base_image", ParamUtil.getString(record, AppParams.S_BASE_MOCKUP));
		rs.put(BASE_SHORT_CODE, ParamUtil.getString(record, AppParams.S_SHORT_CODE));
		rs.put(BASE_COST, ParamUtil.getString(record, AppParams.S_PRICE));
		rs.put(AppParams.CURRENCY, ParamUtil.getString(record, AppParams.S_CURRENCY));
		rs.put(AppParams.SHIPPING_TIME_US, ParamUtil.getString(record, AppParams.S_SHIPPING_TIME_US));
		rs.put(AppParams.SHIPPING_LINES, ParamUtil.getString(record, AppParams.S_SHIPPING_LINES));
		return rs;
	}
	
	private static final String CATALOG_ID = "catalog_id";
	private static final String CATALOG_NAME = "catalog_name";
	private static final String BASE_SHORT_CODE = "base_short_code";
	private static final String BASE_COST = "base_cost";
	
	private static final Logger LOGGER = Logger.getLogger(GetCatalogsHandler.class.getName());
	
}	
