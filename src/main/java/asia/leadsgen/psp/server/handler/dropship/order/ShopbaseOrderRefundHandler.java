package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderRefundService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service_fulfill.FulfillmentService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.HttpClientUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopbaseOrderRefundHandler extends PSPOrderHandler implements Handler<RoutingContext> {

	private static HttpServiceConfig paymentConnectorServiceConfig;

	public static void setPaymentConnectorServiceConfig(HttpServiceConfig paymentConnectorServiceConfig) {
		ShopbaseOrderRefundHandler.paymentConnectorServiceConfig = paymentConnectorServiceConfig;
	}

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking(future -> {

			try {

				Map requestBodyMap = routingContext.getBodyAsJson().getMap();
				LOGGER.info("[Shopbase Request] " + routingContext.getBodyAsJson().encode());

				String storeId = routingContext.request().params().get(AppParams.ID);
				if (StringUtils.isNotEmpty(storeId)) {
					
					DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);
					
					Map dropshipStore = DropShipStoreService.lookUp(storeId);
					if (dropshipStore != null && !dropshipStore.isEmpty()) {
						
						String shopbaseOrderId = ParamUtil.getString(requestBodyMap, AppParams.ORDER_ID, "");
						Map burgerOrder = DropshipOrderService.findByReferenceOrder(storeId, shopbaseOrderId);
						if (refundAvailable(burgerOrder)) {
							List<Map> refundLineItems = ParamUtil.getListData(requestBodyMap,
									AppParams.REFUND_LINE_ITEMS);
							String orderId = ParamUtil.getString(burgerOrder, AppParams.ID);
							List<Map> dbOrderItemList = ParamUtil.getListData(burgerOrder, AppParams.ITEMS);

							if (refundLineItems != null && !refundLineItems.isEmpty()) {
								initItemGroupQuantity();
								Double currentRefundAmount = calculateItemRefundAmount(refundLineItems, orderId,
										dbOrderItemList);
								Double oldShippingTotal = 0d, newShippingTotal = 0d, refundedShipping = 0d;
								if (0 < currentRefundAmount) {
									Map shippingAddress = ParamUtil.getMapData(burgerOrder, AppParams.SHIPPING);
									String countryCode = ParamUtil.getString(shippingAddress, AppParams.COUNTRY);
									for (Map orderItem : dbOrderItemList) {
										oldShippingTotal += ParamUtil.getDouble(orderItem, AppParams.SHIPPING_FEE);
										String baseId = ParamUtil.getString(orderItem, AppParams.BASE_ID);
										ShippingFeeObj shippingFeeObj = ShippingFeeService.getShippingFee(baseId, countryCode);
										int ramainingQty;
										if (orderItem.containsKey(AppParams.REMAINING)) {
											ramainingQty = ParamUtil.getInt(orderItem, AppParams.REMAINING, 0);
										} else {
											ramainingQty = ParamUtil.getInt(orderItem, AppParams.QUANTITY);
										}
										int groupQty = itemGroupQuantity.get(shippingFeeObj.getGroupId());
										if (groupQty == 0) {
											newShippingTotal += ProductUtil.calculateShippingFee( shippingFeeObj.getDropshipPrice(), ramainingQty, shippingFeeObj.getDropshipAddingPrice());
										} else {
											newShippingTotal += shippingFeeObj.getDropshipAddingPrice() * ramainingQty;
										}
										itemGroupQuantity.put(shippingFeeObj.getGroupId(), groupQty + ramainingQty);
									}
									refundedShipping = DropshipOrderRefundService.getRefundedShipping(orderId);
									Double refundShipping = oldShippingTotal - newShippingTotal - refundedShipping;
									currentRefundAmount += refundShipping;

									Map payment = ParamUtil.getMapData(burgerOrder, AppParams.PAYMENT);
									String transactionId = ParamUtil.getString(payment, AppParams.TRANSACTION_ID);
									String paymentMethod = ParamUtil.getString(payment, AppParams.METHOD);
									String paymentName = ParamUtil.getString(payment, AppParams.PAYMENT_NAME);

									Map<String, Object> refundRequestBody = new HashMap<String, Object>();
									refundRequestBody.put("account_name", paymentName);
									Map<String, Object> data = new HashMap<String, Object>();
									refundRequestBody.put("data", data);
									Map<String, Object> amount = new HashMap<String, Object>();
									data.put(AppParams.AMOUNT, amount);
									amount.put(AppParams.TOTAL, formatter.format(currentRefundAmount));

									String refundRequestUri = "/" + paymentMethod + "/" + transactionId + "/refund";
									String paymentRequestString = new JsonObject(refundRequestBody).encode();
									HttpClientRequest paymentConnectorRequest = HttpClientUtil.createHttpRequest(
											paymentConnectorServiceConfig, refundRequestUri, HttpMethod.POST,
											new LinkedHashMap<>(), paymentRequestString);
									
									routingContext.put("currentRefundAmount", String.format("%.2f", currentRefundAmount));
									
									paymentConnectorRequest.handler(
											refundResponse -> refundResponseHandler(routingContext, refundResponse,
													orderId, refundShipping, dbOrderItemList));

									paymentConnectorRequest
											.exceptionHandler(throwable -> routingContext.fail(throwable));
									paymentConnectorRequest.write(paymentRequestString);
									paymentConnectorRequest.end();
								}

							}

						}
					}
				}
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_MSG, Collections.EMPTY_MAP);
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

	private boolean refundAvailable(Map burgerOrder) throws SQLException {
		boolean available = false;
		String orderState, orderId;
		if (!burgerOrder.isEmpty()) {
			orderState = ParamUtil.getString(burgerOrder, AppParams.STATE);
			available = ResourceStates.PLACED.equals(orderState) || ResourceStates.PARTIALLY_REFUND.equals(orderState);
			orderId = ParamUtil.getString(burgerOrder, AppParams.ID);
			Map fulfillmentInfo = FulfillmentService.searchByOrderId(orderId);
			available = available && fulfillmentInfo.isEmpty();
		}
		return available;
	}

	private Double calculateItemRefundAmount(List<Map> refundLineItems, String orderId, List<Map> dbOrderItemList) {
		Double currentRefundAmount = 0d;
		Iterator<Map> it = refundLineItems.iterator();
		while (it.hasNext()) {
			Map item = (Map) it.next();
			Map lineItem = ParamUtil.getMapData(item, AppParams.LINE_ITEM);
			String[] sku = ParamUtil.getString(lineItem, AppParams.SKU).split("\\|");

			// sku[0] variantId
			// sku[1] size id
			if (sku.length == 2 && StringUtils.isNotEmpty(sku[0]) && StringUtils.isNotEmpty(sku[1])) {
//									Map itemInfo = 
				Map<String, Object> matchDbItem = findMatchingDbItem(sku[0], sku[1], dbOrderItemList);
				if (matchDbItem != null) {
					int currentRefundQty = ParamUtil.getInt(item, AppParams.QUANTITY);
//						int refundedQty = DropshipOrderRefundService.getRefundedItems(orderId, sku[0], sku[1]);
					int remainingQty = ParamUtil.getInt(matchDbItem, AppParams.QUANTITY) - currentRefundQty;
					matchDbItem.put(AppParams.REMAINING, remainingQty);
					matchDbItem.put(AppParams.REFUND, currentRefundQty);
					currentRefundAmount += ParamUtil.getDouble(matchDbItem, AppParams.PRICE, 0d) * currentRefundQty;
				}
			}
		}
		return currentRefundAmount;
	}

	private Map<String, Object> findMatchingDbItem(String variantId, String sizeId, List<Map> dbOrderItemList) {
		Map<String, Object> matchedItem = null;
		for (Map dbOrderItem : dbOrderItemList) {
			String dbOrderItemSizeId = ParamUtil.getString(dbOrderItem, AppParams.SIZE_ID);
			String dbOrderItemVariantId = ParamUtil.getString(dbOrderItem, AppParams.VARIANT_ID);
			if (variantId.equals(dbOrderItemVariantId) && sizeId.equals(dbOrderItemSizeId)) {
				matchedItem = dbOrderItem;
				break;
			}
		}
		return matchedItem;
	}

	private void refundResponseHandler(RoutingContext routingContext, HttpClientResponse refundResponse, String orderId,
			Double refundShippingAmount, List<Map> orderItemList) {
		int responseCode = refundResponse.statusCode();
		String responseMsg = refundResponse.statusMessage();

		refundResponse.bodyHandler(responseBody -> {
			try {
				JsonObject jsonErr = new JsonObject();
				jsonErr.put(AppParams.STATUS, "failed");
				JsonObject responseBodyJson = (responseCode == HttpResponseStatus.CREATED.code())
						? new JsonObject(responseBody.toString("UTF-8"))
						: jsonErr;
				Map responseBodyMap = responseBodyJson.getMap();
				if (ResourceStates.SUCCEEDED.equals(ParamUtil.getString(responseBodyMap, AppParams.STATUS))) {
					DropshipOrderRefundService.refundShipping(orderId, formatter.format(refundShippingAmount));
					int totalRemaining = 0;
					for (Map item : orderItemList) {
						if (item.containsKey(AppParams.REMAINING)) {
							totalRemaining += ParamUtil.getInt(item, AppParams.REMAINING, 0);
						} else {
							totalRemaining += ParamUtil.getInt(item, AppParams.QUANTITY, 0);
						}
						int refundQty = ParamUtil.getInt(item, AppParams.REFUND, 0);
						if (refundQty > 0) {
							String variantId = ParamUtil.getString(item, AppParams.VARIANT_ID);
							String sizeId = ParamUtil.getString(item, AppParams.SIZE_ID);
							Double amount = refundQty * ParamUtil.getDouble(item, AppParams.PRICE, 0);
							DropshipOrderRefundService.saveRefund(orderId, variantId, sizeId, refundQty,
									formatter.format(amount));
						}
					}
					
					String orderState = totalRemaining == 0 ? "refunded" : "partially_refund";
					DropshipOrderRefundService.updateOrderState(orderId, orderState);
					String currentRefundAmount = ContextUtil.getString(routingContext, "currentRefundAmount");
					DropshipOrderService.updateOrderRefundedAmount(orderId, currentRefundAmount);
				} else {
					responseBodyMap = jsonErr.getMap();
				}
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, responseBodyMap);
//				routingContext.next();
//				future.complete();
			} catch (Exception e) {
				routingContext.fail(e);
			}
		});
	}

	private static final Logger LOGGER = Logger.getLogger(ShopbaseOrderRefundHandler.class.getName());
}
