package asia.leadsgen.psp.server.handler.dropship.woo;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.StoreOptionService;
import asia.leadsgen.psp.service_fulfill.WooService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class WooCommerceGetAttribute implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {
				String storeId = routingContext.request().getParam(AppParams.ID);
				Map storeMap = DropShipStoreService.lookUp(storeId);
				if (storeMap.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_STORE);
				}
				String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
				String consumerSecret = ParamUtil.getString(storeMap, AppParams.SECRET);
				String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
//                    String userId = ParamUtil.getString(storeMap, AppParams.USER_ID);

				Map<Object, Object> response = new HashedMap<>();

				JsonArray listAttributeWoo = WooService.getListAttribute(domain, consumerKey, consumerSecret);
				List<Map> listAttributeBgp = StoreOptionService.lookUp(storeId, AppParams.ATTRIBUTE);
				if (!listAttributeBgp.isEmpty()) {
					// put BGP Option
					JsonArray listOption = new JsonArray();
					JsonObject jsStyle = new JsonObject().put(AppParams.OPTION, AppParams.STYLE).put(AppParams.ATTRIBUTE, "");
					JsonObject jsColor = new JsonObject().put(AppParams.OPTION, AppParams.COLOR).put(AppParams.ATTRIBUTE, "");
					JsonObject jsSize = new JsonObject().put(AppParams.OPTION, AppParams.SIZE).put(AppParams.ATTRIBUTE, "");
					
					for (Map attributeBgp : listAttributeBgp) {
//						JsonArray listOption = new JsonArray();
						String bgpOption = ParamUtil.getString(attributeBgp, AppParams.S_BGP_OPTION);
						String wcOptionName = ParamUtil.getString(attributeBgp, AppParams.S_OPTION_NAME);
						if (bgpOption.equals(AppParams.STYLE)) {
							jsStyle.put(AppParams.ATTRIBUTE, wcOptionName);
						}else if (bgpOption.equals(AppParams.COLOR)) {
							jsColor.put(AppParams.ATTRIBUTE, wcOptionName);
						}else if (bgpOption.equals(AppParams.SIZE)) {
							jsSize.put(AppParams.ATTRIBUTE, wcOptionName);
						}
					}
					listOption.add(jsStyle);
					listOption.add(jsColor);
					listOption.add(jsSize);
					
					
					response.put("list_option", listOption);
				}
				if (!listAttributeWoo.isEmpty())  {
					// put WC Attribute
					JsonArray listAttribute = new JsonArray();
					listAttributeWoo.forEach(item -> {
						JsonObject jsonItem = (JsonObject) item;
						JsonObject attribute = new JsonObject();
						attribute.put(AppParams.ID, String.valueOf(jsonItem.getInteger(AppParams.ID)));
						attribute.put(AppParams.NAME, jsonItem.getString(AppParams.NAME));
						listAttribute.add(attribute);
					});

					response.put("list_attribute", listAttribute);
				}

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, response);
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

	private static final Logger LOGGER = Logger.getLogger(WooCommerceGetAttribute.class.getName());
}
