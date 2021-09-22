package asia.leadsgen.psp.server.handler.dropship.woo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.StoreOptionObj;
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

public class WooCommerceMapAttribute implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking(future -> {

			try {
				String storeId = routingContext.request().getParam(AppParams.ID);
				Map storeMap = DropShipStoreService.lookUp(storeId);
				if (storeMap.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_STORE);
				}

				JsonObject requestBody = routingContext.getBodyAsJson();

				JsonArray listAttributeToMap = requestBody.getJsonArray("list_attribute");

				List<StoreOptionObj> listAttributeName = new ArrayList<>();

				Map response = new HashedMap<>();

				// Merge attribute vao TB_STORE_OPTION
				listAttributeToMap.forEach(item -> {
					JsonObject attribute = (JsonObject) item;
					StoreOptionObj storeMediaObj = new StoreOptionObj();

					storeMediaObj.setS_store_id(storeId);
					storeMediaObj.setS_bgp_option(attribute.getString("option"));
					storeMediaObj.setS_option_id(attribute.getString("id"));
					storeMediaObj.setS_option_name(attribute.getString("name"));
					storeMediaObj.setS_type(AppParams.ATTRIBUTE);
					storeMediaObj.setS_state("approved");
					listAttributeName.add(storeMediaObj);
//							StoreMediaService.mergeAttribute(storeMediaObj);
				});
				StoreOptionService.mergeAttributeV2(listAttributeName);

				// Load Tags
				List<StoreOptionObj> listTagName = new ArrayList<>();
				String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
				String consumerSecret = ParamUtil.getString(storeMap, AppParams.SECRET);
				String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);

				JsonArray listTagWoo = WooService.getListTag(domain, consumerKey, consumerSecret);
//
				if (!listTagWoo.isEmpty()) {
					// put WC Attribute
					JsonArray listTag = new JsonArray();
					listTagWoo.forEach(item -> {
						JsonObject tag = (JsonObject) item;
						StoreOptionObj storeMediaObj = new StoreOptionObj();
						storeMediaObj.setS_store_id(storeId);
						storeMediaObj.setS_bgp_option("Tag");
						storeMediaObj.setS_option_id(String.valueOf(tag.getInteger("id")));
						storeMediaObj.setS_option_name(tag.getString("name"));
						storeMediaObj.setS_type("tag");
						storeMediaObj.setS_state("approved");

						JsonObject jsonTag = new JsonObject();
						jsonTag.put("id", storeMediaObj.getS_option_id());
						jsonTag.put("name", storeMediaObj.getS_option_name());
						listTag.add(jsonTag);
						listTagName.add(storeMediaObj);
					});
					StoreOptionService.mergeTag(listTagName);
					response.put("list_tag", listTag);
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

	private static final Logger LOGGER = Logger.getLogger(WooCommerceMapAttribute.class.getName());
}
