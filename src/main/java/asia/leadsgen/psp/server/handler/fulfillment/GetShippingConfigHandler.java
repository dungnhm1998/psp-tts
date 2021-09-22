package asia.leadsgen.psp.server.handler.fulfillment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service_fulfill.RedisService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class GetShippingConfigHandler implements Handler<RoutingContext> {
	
	private static final String REDIS_KEY = "shipping-express-info";
	
	private static String shippingConfig;

	public static void setShippingConfig(String shippingConfig) {
		GetShippingConfigHandler.shippingConfig = shippingConfig;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {
				
				Map responseMap = RedisService.get(REDIS_KEY);
				
				if (responseMap == null) {
					
					responseMap = new LinkedHashMap<>();

					ObjectMapper mapper = new ObjectMapper();
					byte[] shippingConfigUtf8 = new String(shippingConfig.getBytes(), StandardCharsets.UTF_8).getBytes(StandardCharsets.ISO_8859_1);
					Map shippingCfg = mapper.readValue(shippingConfigUtf8, Map.class );
					
					responseMap.putAll(shippingCfg);
					
					List<Map> resultSearch = ShippingFeeService.getShippingExpressInfo();
					
					Map<String,List<Map>> groupByBase = resultSearch.stream().collect(Collectors.groupingBy(e -> ParamUtil.getString(e, AppParams.S_BASE_ID)));
					
					Map formatData = new LinkedHashMap<>();

					for (Map.Entry<String, List<Map>> eachBase : groupByBase.entrySet()) {
						String baseId = eachBase.getKey();
						
						
						boolean shippingExpressToAll = eachBase.getValue().stream().anyMatch(e -> "ALL".equalsIgnoreCase(ParamUtil.getString(e, AppParams.S_COUNTRY_CODE)));
						
						Map baseInfo = new LinkedHashMap<>();
						
						List<String> lstCountry = new ArrayList<String>();
						if (!shippingExpressToAll) {
							lstCountry = eachBase.getValue().stream().map(e -> ParamUtil.getString(e, AppParams.S_COUNTRY_CODE)).collect(Collectors.toList());
						}
						
						baseInfo.put("express_to_all", shippingExpressToAll);
						baseInfo.put("list_country", lstCountry);
						
						formatData.put(baseId, baseInfo);
						
					}
					
					responseMap.put("base_has_shipping_express", formatData);
					
					RedisService.save(REDIS_KEY, responseMap);
				}
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, responseMap);

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
	
	private static final Logger LOGGER = Logger.getLogger(GetShippingConfigHandler.class.getName());
}
