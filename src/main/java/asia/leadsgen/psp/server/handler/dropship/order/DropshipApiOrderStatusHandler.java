/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import asia.leadsgen.psp.obj.DropshipCampApiOrder;
import asia.leadsgen.psp.obj.DropshipCustomApiOrder;
import asia.leadsgen.psp.obj.DropshipStoreObj;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceSource;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author liamle
 *
 */
public class DropshipApiOrderStatusHandler implements Handler<RoutingContext> {

	private DropshipStoreObj store = null;

	static final String API_SOURCE_REGEX = "^(?:camp|custom).api$";

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {

				Response response = new Response(true, "", 200);

				String apikey = routingContext.request().params().get("api_key");
				LOGGER.info("apikey= " + apikey);
				String sandboxStr = routingContext.request().params().get("sandbox");
				LOGGER.info("sandbox= " + sandboxStr);
				boolean sandbox = false;
				if ("true".equals(sandboxStr)) {
					LOGGER.info("true");
					sandbox = true;
				} else if ("false".equals(sandboxStr)) {
					LOGGER.info("false");
					sandbox = false;
				} else {
					LOGGER.info("Bad Request");
					response = new Response(false,
							"Bad Request: Sorry there was an error processing your order. Please contact support", 400);
				}
				String orderId = routingContext.request().getParam("id");
				LOGGER.info("orderId= " + orderId);
				if (response.getSuccess() && StringUtils.isEmpty(apikey)) {
					response = new Response(false, "api_key can not be empty.", 401);
				}
				LOGGER.info("response: " + response.getSuccess());
				if (response.getSuccess()) {
					store = DropShipStoreService.findByApiKey(apikey);
					LOGGER.info("store= " + store.getName());
					if (store == null || (!sandbox && !orderId.startsWith(store.getUserId() + "-"))) {
						response = new Response(false, "Failed to authenticate.", 401);
					}
				}

				if (StringUtils.isEmpty(orderId)) {
					response = new Response(false, "order_id can not be empty.", 401);
				}

				String state = "unknown";
				String amount = "";
				String sub_amount = "";
				String shipping_fee = "";
				List<Map> trackings = new ArrayList<Map>();
				if (response.getSuccess() && sandbox == false) {
					LOGGER.info("checkOrder... ");
					response = checkOrder(orderId, apikey);
					if (response.getSuccess()) {
						try {
							Map order = DropshipOrderService.lookUpV2(orderId, false, false, false);
							state = ParamUtil.getString(order, AppParams.STATE);
							amount = String.format("%.2f", ParamUtil.getDouble(order, AppParams.AMOUNT));
							sub_amount = ParamUtil.getString(order, AppParams.SUB_AMOUNT);
							shipping_fee = ParamUtil.getString(order, AppParams.SHIPPING_FEE);
//							state = DropshipOrderService.getDropshipOrderState(orderId);
							trackings = DropshipOrderService.getDropshipOrderTrackings(orderId);
						} catch (Exception e) {
							LOGGER.severe(e.getMessage());
							response = new Response(false,
									"Bad Request: Sorry there was an error processing your order. Please contact support",
									400);
						}
					}
				} else {
					orderId = "ASAMPLE-FQ79-16899";
					HashMap<String, String> tracking = new HashMap<String, String>();
					tracking.put("BGP-6868", "https://tools.usps.com/go/TrackConfirmAction?tLabels=123456789");
					trackings.add(tracking);
					state = "shipped";
				}

				Map responseM = new LinkedHashMap<String, Object>();
				if (response.getSuccess()) {
					responseM.put("id", orderId);
					responseM.put("status", state);
					responseM.put("amount", amount);
					responseM.put("sub_amount", sub_amount);
					responseM.put("shipping_fee", shipping_fee);
					if (trackings.isEmpty() == false) {
						responseM.put("trackings", trackings);
					}
				} else {
					responseM.put("is_success", response.getSuccess());
					responseM.put("message", response.getMessage());
				}
				LOGGER.info("response.getSuccess()= " + response.getSuccess());

				String reasonPhase = response.getSuccess() ? HttpResponseStatus.OK.reasonPhrase()
						: HttpResponseStatus.BAD_REQUEST.reasonPhrase();
				LOGGER.info("response.getCode()= " + response.getCode());
				LOGGER.info("reasonPhase= " + reasonPhase);
				routingContext.put(AppParams.RESPONSE_CODE, response.getCode());
				routingContext.put(AppParams.RESPONSE_MSG, reasonPhase);
				routingContext.put(AppParams.RESPONSE_DATA, responseM);

				future.complete();

			} catch (Exception e) {
				routingContext.fail(e);
			}
		}, asyncResult ->

		{
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	private Response checkOrder(String orderId, String apikey) throws SQLException {
		Response response = new Response(true, "", 200);

		OrderCancellationCheck check = DropshipOrderService.checkForApiCancellation(orderId);

		Response cannotProcessResponse = new Response(false,
				"Bad Request: Sorry there was an error processing your order. Please contact support", 400);

		Response invalidApiKey = new Response(false, "Bad Request: Invalid api_key", 400);

		if (response.getSuccess() && check == null) {
			LOGGER.info("check= null");
			response = cannotProcessResponse;
		}

//		if (response.getSuccess()
//				&& (StringUtils.isEmpty(check.getSource()) || check.getSource().matches(API_SOURCE_REGEX) == false)) {
//			response = cannotProcessResponse;
//		}

		if (response.getSuccess() && check.getSource().equals(ResourceSource.CAMP_API)) {
			DropshipCampApiOrder order = new Gson().fromJson(check.getMinifiedJson(), DropshipCampApiOrder.class);
			if (apikey.equals(order.getApiKey()) == false) {
				response = invalidApiKey;
			}
		}

		if (response.getSuccess() && check.getSource().equals(ResourceSource.CUSTOM_API)) {
			DropshipCustomApiOrder order = new Gson().fromJson(check.getMinifiedJson(), DropshipCustomApiOrder.class);
			if (apikey.equals(order.getApiKey()) == false) {
				response = invalidApiKey;
			}
		}

		return response;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	class Response {
		private Boolean success;
		private String message;
		private Integer code;

		public Response(Boolean success, String message, Integer code) {
			this.success = success;
			this.message = message;
			this.code = code;
		}

		public Response(Boolean success) {
			this.success = success;
		}

		private String shippingId;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipApiOrderStatusHandler.class.getName());

}
