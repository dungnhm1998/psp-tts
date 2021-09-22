/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import asia.leadsgen.psp.external.api.ISPApiConnector;
import asia.leadsgen.psp.obj.BaseSKUObj;
import asia.leadsgen.psp.obj.DropshipCampApiOrder;
import asia.leadsgen.psp.obj.DropshipCustomApiItem;
import asia.leadsgen.psp.obj.DropshipCustomApiOrder;
import asia.leadsgen.psp.obj.DropshipStoreObj;
import asia.leadsgen.psp.server.handler.dropship.order.CheckDesignsResponse;
import asia.leadsgen.psp.service_fulfill.BaseSKUService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ResourceSource;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author liamle
 *
 */
public class DropshipApiOrderCancelHandler implements Handler<RoutingContext> {

	private DropshipStoreObj store = null;

	static final String API_SOURCE_REGEX = "^(?:camp|custom).api$";

	static Integer DEFAULT_CANCELLABLE_IN_HOURS = 6;

	Boolean isRequireRefund = null;

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {
				String requestString = routingContext.getBodyAsString();

				Response response = new Response(true, "Order was cancelled successfully", 200);

				DropshipApiCancelOrderRequest cancelOrderRequest = null;
				if (response.getSuccess() && StringUtils.isNotEmpty(requestString)) {
					cancelOrderRequest = new Gson().fromJson(requestString, DropshipApiCancelOrderRequest.class);
				} else {
					response = new Response(false,
							"Bad Request: Sorry there was an error processing your order. Please contact support", 400);
				}

				if (response.getSuccess() && StringUtils.isEmpty(cancelOrderRequest.getApiKey())) {
					response = new Response(false, "api_key can not be empty.", 401);
				}

				if (response.getSuccess()) {
					store = DropShipStoreService.findByApiKey(cancelOrderRequest.getApiKey());
					if (store == null) {
						response = new Response(false, "Failed to authenticate.", 401);
					}
				}

				if (StringUtils.isEmpty(cancelOrderRequest.getOrderId())) {
					response = new Response(false, "order_id can not be empty.", 401);
				}

				if (response.getSuccess()) {
					response = checkForCancellability(cancelOrderRequest);
				}

				String orderId = null;
				if (response.getSuccess() && cancelOrderRequest.getSandbox() == false) {
					try {
						orderId = cancelOrderRequest.getOrderId();
						Boolean cancelledSuccess = DropshipOrderService.cancel(orderId, isRequireRefund);
						if (!cancelledSuccess) {
							response = new Response(false,
									"Bad Request: Sorry there was an error processing your order. Please contact support",
									400);
						} else {
							response = new Response(true, "Order was cancelled successfully", 200);
						}
						isRequireRefund = null;

					} catch (Exception e) {
						response = new Response(false,
								"Bad Request: Sorry there was an error processing your order. Please contact support",
								400);
					}
				} else {
					orderId = "ASAMPLE-FQ79-16899";
				}

				Map responseM = new HashMap<String, Object>();
				responseM.put("is_success", response.getSuccess());
				responseM.put("message", response.getMessage());

				if (response.getSuccess()) {
					responseM.put("order_id", orderId);
				}

				String reasonPhase = response.getCode().intValue() == 200 ? HttpResponseStatus.OK.reasonPhrase()
						: HttpResponseStatus.BAD_REQUEST.reasonPhrase();

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

	private Response checkForCancellability(DropshipApiCancelOrderRequest cancelOrderRequest) throws SQLException {
		Response response = new Response(true);

		OrderCancellationCheck check = DropshipOrderService.checkForApiCancellation(cancelOrderRequest.getOrderId());

		Response cannotProcessResponse = new Response(false,
				String.format("Cannot process order cancellation for %s", cancelOrderRequest.getOrderId()), 400);

		Response orderWasFulfilled = new Response(false,
				String.format("Cannot process order cancellation for %s because order fulfillment is processing.",
						cancelOrderRequest.getOrderId()),
				400);

		Response orderWasCancelled = new Response(false,
				String.format("Cannot process order cancellation for %s because order was already cancelled.",
						cancelOrderRequest.getOrderId()),
				400);

		if (response.getSuccess() && check == null) {
			response = cannotProcessResponse;
		}

		if (response.getSuccess()
				&& (StringUtils.isEmpty(check.getSource()) || check.getSource().matches(API_SOURCE_REGEX) == false)) {
			response = cannotProcessResponse;
		}

		if (response.getSuccess() && check.getSource().equals(ResourceSource.CAMP_API)) {
			DropshipCampApiOrder order = new Gson().fromJson(check.getMinifiedJson(), DropshipCampApiOrder.class);
			if (cancelOrderRequest.getApiKey().contentEquals(order.getApiKey()) == false) {
				response = cannotProcessResponse;
			}
		}

		if (response.getSuccess() && check.getSource().equals(ResourceSource.CUSTOM_API)) {
			DropshipCustomApiOrder order = new Gson().fromJson(check.getMinifiedJson(), DropshipCustomApiOrder.class);
			if (cancelOrderRequest.getApiKey().contentEquals(order.getApiKey()) == false) {
				response = cannotProcessResponse;
			}
		}

		if (response.getSuccess() && check.getFulfillmentRecords() > 0) {
			response = orderWasFulfilled;
		}

		if (response.getSuccess()
				&& ("DELETED".equalsIgnoreCase(check.getState()) || "REFUNDED".equalsIgnoreCase(check.getState()))) {
			response = orderWasCancelled;
		}

		if (response.getSuccess()
				&& (check.getHoursDiffFromPurchasedTime() != null && 0 < check.getHoursDiffFromPurchasedTime()
						&& DEFAULT_CANCELLABLE_IN_HOURS < check.getHoursDiffFromPurchasedTime())) {
			response = cannotProcessResponse;
		} else {
			if (check.getHoursDiffFromPurchasedTime() != null && 0 < check.getHoursDiffFromPurchasedTime()) {
				isRequireRefund = true;
			}
		}

		return response;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
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

	private static final Logger LOGGER = Logger.getLogger(DropshipApiOrderCancelHandler.class.getName());

}
