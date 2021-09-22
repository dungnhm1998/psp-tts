package asia.leadsgen.psp.server.handler.warehouse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;

import asia.leadsgen.psp.service_fulfill.FulfillmentService;
import asia.leadsgen.psp.service_fulfill.UpdateTrackingService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

public class RosalindaGetOrdersHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {

				Map response = new HashedMap<>();

				HttpServerRequest httpServerRequest = routingContext.request();
				MultiMap requestHeaders = httpServerRequest.headers().getDelegate();
				String token = requestHeaders.get("Authorization");

				if (!UpdateTrackingService.validateToken(token)) {
					response = new LinkedHashMap<>();
					response.put("code", 1);
					response.put("message", "Invalid Token");
				} else {

					String date = routingContext.request().getParam(AppParams.DATE);

					if (date == null || date.isEmpty()) {
						SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);
						date = dateFormat.format(new Date());
					}

					int page = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE), 1);
					int pageSize = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE_SIZE), 50);

					String ref_type = GetterUtil.getString(routingContext.request().getParam("ref_type"), "");
					String ref_value = GetterUtil.getString(routingContext.request().getParam("ref_value"), "");

					LOGGER.info("page " + page + " pageSize " + pageSize + " date = " + date);

					List<Map> listOrders = FulfillmentService.rosalindaGetOrders(page, pageSize, date, ref_type, ref_value);

					Set<String> listPkg = listOrders.stream().map(o -> ParamUtil.getString(o, AppParams.S_PACKAGE_ID)).collect(Collectors.toSet());
					int total = listPkg.size();

					List<JsonObject> data = new ArrayList<>();
					for (String pkg : listPkg) {
						List<JsonObject> orderProductInPkg = listOrders.stream().filter(order -> pkg.equals(ParamUtil.getString(order, AppParams.S_PACKAGE_ID))).map(order -> {
							JsonObject item = new JsonObject();
							item.put("id", ParamUtil.getString(order, AppParams.S_ORDER_PRODUCT_ID));
							item.put("name", ParamUtil.getString(order, AppParams.S_PRODUCT_NAME));
							item.put("product_type", ParamUtil.getString(order, AppParams.S_BASE_NAME));
							item.put("front_url", ParamUtil.getString(order, AppParams.S_PRODUCT_FRONT_IMG_URL));
							item.put("back_url", ParamUtil.getString(order, AppParams.S_PRODUCT_BACK_IMG_URL));
							item.put("size", ParamUtil.getString(order, AppParams.S_SIZE));
							item.put("color", ParamUtil.getString(order, AppParams.S_COLOR));
							int quantity = ParamUtil.getInt(order, AppParams.N_QUANTITY);
							item.put("quantity", quantity);
							item.put("sku", ParamUtil.getString(order, AppParams.S_SKU));
							item.put("unit_amount", String.valueOf(ParamUtil.getDouble(order, AppParams.S_UNIT_AMOUNT)));
							item.put("weight", ParamUtil.getDouble(order, AppParams.WEIGHT));
							item.put("hts_code", ParamUtil.getString(order, AppParams.S_TARIFF_NUMBER));
							item.put("country", "US");
							Double shippingValue =  GetterUtil.getDouble(ParamUtil.getString(order, AppParams.S_SHIPPING_VALUE)) ;
							item.put("unit_price", shippingValue/quantity);

							JsonObject dimension = new JsonObject();
							dimension.put("length", 9);
							dimension.put("width", 6);
							dimension.put("height", 2);
							dimension.put("unit", "inch");

							item.put("dimension", dimension);
							return item;
						}).collect(Collectors.toList());

						JsonObject orderInfo = new JsonObject();
						for (Map order : listOrders) {
							Map dataPkg = new HashedMap<>();
							if (ParamUtil.getString(order, AppParams.S_PACKAGE_ID).equals(pkg) && dataPkg.get(pkg) == null) {
								orderInfo.put("order_id", pkg);
								orderInfo.put("ref_order_id", ParamUtil.getString(order, AppParams.S_ORDER_ID));
								orderInfo.put("create_date", ParamUtil.getString(order, AppParams.D_CREATE));
								orderInfo.put("currency", "USD");
								orderInfo.put("shipping_name", ParamUtil.getString(order, "SHIPPING_NAME"));
								orderInfo.put("shipping_email", ParamUtil.getString(order, "SHIPPING_EMAIL"));
								orderInfo.put("shipping_phone", ParamUtil.getString(order, "SHIPPING_PHONE"));
								orderInfo.put("shipping_address1", ParamUtil.getString(order, "SHIPPING_ADD_LINE1"));
								orderInfo.put("shipping_address2", ParamUtil.getString(order, "SHIPPING_ADD_LINE2"));

								orderInfo.put("shipping_city", ParamUtil.getString(order, "SHIPPING_ADD_CITY"));
								orderInfo.put("shipping_state", ParamUtil.getString(order, "SHIPPING_ADD_STATE"));
								orderInfo.put("shipping_zipcode", ParamUtil.getString(order, "SHIPPING_POSTAL_CODE"));
								orderInfo.put("shipping_method", ParamUtil.getString(order, AppParams.S_SHIPPING_METHOD));

								String shippingCountry = ParamUtil.getString(order, "SHIPPING_COUNTRY_CODE");
								orderInfo.put("shipping_country", ParamUtil.getString(order, "SHIPPING_COUNTRY_CODE"));
								String shippingService = shippingCountry.equalsIgnoreCase("US") ? "usps":"asendia";
								orderInfo.put("shipping_service ", shippingCountry.equalsIgnoreCase("US") ? "usps":"asendia");

								orderInfo.put("return_name", "Dmitriy");
								orderInfo.put("return_address1", "16085nw52ndave");
								orderInfo.put("return_address2", "");
								orderInfo.put("return_city", "MiamiLakes");
								orderInfo.put("return_state", "FL");
								orderInfo.put("return_country", "US");
								orderInfo.put("return_zipcode", "33014");
								orderInfo.put("return_email", "support@burgerprints.com");

								orderInfo.put("items_amount", String.valueOf(orderProductInPkg.stream().mapToDouble(p ->
										Double.parseDouble(p.getString(AppParams.UNIT_AMOUNT, "0.0"))
								).sum()));
								orderInfo.put("items_quantity", String.valueOf(orderProductInPkg.size()));
								orderInfo.put("items", orderProductInPkg);
								orderInfo.put("barcode_url", ParamUtil.getString(order, AppParams.S_BARCODE_URL));

								LOGGER.info("-----orderInfo" + orderInfo);
								dataPkg.put(pkg, orderInfo);
								data.add(orderInfo);
								break;
							}
						}
					}
					response.put("total", total);
					response.put("data", data);
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

	private static final Logger LOGGER = Logger.getLogger(RosalindaGetOrdersHandler.class.getName());
}
