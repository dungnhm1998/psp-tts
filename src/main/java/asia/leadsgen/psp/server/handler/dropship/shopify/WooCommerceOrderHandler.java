/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.server.handler.dropship.shopify;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.ProductService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 *
 * @author HIEPHV
 * @maintainer Liam
 */
public class WooCommerceOrderHandler extends PSPOrderHandler implements Handler<RoutingContext> {

	private static final String USD = "USD";
	private static HttpServiceConfig paymentConnectorServiceConfig;

	public static void setPaymentConnectorServiceConfig(HttpServiceConfig paymentConnectorServiceConfig) {
		WooCommerceOrderHandler.paymentConnectorServiceConfig = paymentConnectorServiceConfig;
	}

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() != null || !routingContext.getBodyAsString().isEmpty()) {

			routingContext.vertx().executeBlocking(future -> {

				try {

//					String storeId = routingContext.request().getParam(AppParams.ID);
//					LOGGER.info("woo order body post:" + routingContext.getBodyAsJson().encode());
//
//					Map dropshipStore;
//					try {
//						JsonObject orderRequest = routingContext.getBodyAsJson();
//						String referenceOrderId = orderRequest.getInteger("arg").toString();
//
//						if (StringUtils.isEmpty(storeId)) {
//							LOGGER.log(Level.WARNING, "store Id is empty: " + storeId);
//							throw new BadRequestException(SystemError.INVALID_STORE_ID);
//						}
//
//						if (StringUtils.isEmpty(referenceOrderId)) {
//							LOGGER.log(Level.WARNING, "dropship order Id is empty");
//							throw new BadRequestException(SystemError.INVALID_ORDER_ID);
//						}
//
//						dropshipStore = DropShipStoreService.lookUp(storeId);
//
//						if (!dropshipStore.isEmpty()) {
//
//							String consumerKey = ParamUtil.getString(dropshipStore, AppParams.API_KEY);
//							String consumerSecret = ParamUtil.getString(dropshipStore, AppParams.SECRET);
//							String domain = ParamUtil.getString(dropshipStore, AppParams.DOMAIN);
//
//							String dropshipStoreStatus = ParamUtil.getString(dropshipStore, AppParams.STATE);
//							if (!"approved".equalsIgnoreCase(dropshipStoreStatus)) {
//								String dropshipStoreName = ParamUtil.getString(dropshipStore, AppParams.NAME);
//								LOGGER.log(Level.WARNING,
//										"dropship store [" + dropshipStoreName + "|" + storeId + "] is deleted: ");
//								throw new BadRequestException(SystemError.INVALID_STORE);
//							}
//
//							// get order from woo
//							Map wooOrder = woocommerceGetOrderById(domain, consumerKey, consumerSecret,
//									referenceOrderId);
//							LOGGER.info("woo order:" + wooOrder);
//							if (!wooOrder.isEmpty()) {
//								String orderStatus = ParamUtil.getString(wooOrder, "status");
//								String userId = ParamUtil.getString(dropshipStore, AppParams.USER_ID);
//								Boolean autoFulfill = ParamUtil.getBoolean(dropshipStore, AppParams.AUTO_FULFILL);
//
//								if ("processing".equals(orderStatus)) {
//
//									initItemGroupQuantity();
//
//									Map shippingAddressMap = ParamUtil.getMapData(wooOrder, AppParams.SHIPPING);
//
//									if (shippingAddressMap == null || shippingAddressMap.isEmpty()) {
//
//										routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
//										routingContext.put(AppParams.RESPONSE_MSG,
//												HttpResponseStatus.OK.reasonPhrase());
//
//									} else {
//
//										String shippingId = "", trackingNumber = "", note = "", channel = "woocommerce",
//												shippingCountryCode = "";
//										double orderAmount = 0d, subAmount = 0d, shippingFee = 0d;
//										int totalItems = 0;
//
//										boolean isExist = DropshipOrderService.isExistStoreIdReferenceOrderIdSource(
//												storeId, referenceOrderId, ResourceSource.CAMP_WEBOOK);
//
//										List<Map> orderItems = ParamUtil.getListData(wooOrder, AppParams.LINE_ITEMS);
//
//										if (!isExist && orderItems != null && !orderItems.isEmpty()) {
//											List<DropshipOrderProductObj> orderProductObjs = new ArrayList<>();
//
//											Map shippingAddress = ParamUtil.getMapData(wooOrder, AppParams.SHIPPING);
//											Map billingData = ParamUtil.getMapData(wooOrder, AppParams.BILLING);
//
//											shippingCountryCode = ParamUtil.getString(shippingAddress,
//													AppParams.COUNTRY);
//
//											for (Map orderItem : orderItems) {
//
//												DropshipOrderProductObj dropshipOrderProductObj = createOrderItem(
//														shippingCountryCode, USD, orderItem, userId);
//												if (dropshipOrderProductObj != null) {
//													orderProductObjs.add(dropshipOrderProductObj);
//												}
//											}
//											if (!orderProductObjs.isEmpty()) {
//
//												shippingId = saveShippingInfo(shippingAddress, billingData);
//
//												trackingNumber = AppUtil.generateOrderTrackingNumber();
//												for (DropshipOrderProductObj odObj : orderProductObjs) {
//													subAmount += odObj.getPrice() * odObj.getQuantity();
//													shippingFee += odObj.getShippingFee();
//													totalItems += odObj.getQuantity();
//												}
//												orderAmount = GetterUtil.format(subAmount + shippingFee, 2);
//												subAmount = GetterUtil.format(subAmount, 2);
//
//												// referenceOrderId = ParamUtil.getString(wooOrder,AppParams.ID);
//												String orderIdPrefix = createOrderPrefix(orderProductObjs.get(0));
//
//												boolean isAdded = DropshipOrderService .isExistStoreIdReferenceOrderIdSource(storeId, referenceOrderId, ResourceSource.CAMP_WEBOOK);
//
//												if (!isAdded) {
//													DropshipOrderObj dropshipOrderObj = new DropshipOrderObj.Builder(orderIdPrefix)
//															.orderAmount(orderAmount)
//															.orderCurrency("USD")
//															.state(ResourceStates.QUEUED)
//															.shippingId(shippingId)
//															.trackingNumber(trackingNumber)
//															.note(note)
//															.channel(channel)
//															.subAmount(subAmount)
//															.shippingFee(shippingFee)
//															.storeId(storeId)
//															.userId(userId)
//															.referenceOrderId(referenceOrderId)
//															.totalItems(totalItems)
//															.source(ResourceSource.CUSTOM_WEBHOOK)
//															.minifiedJson(routingContext.getBodyAsJson().encode())
//															.build();
//
//													Map orderMap = DropshipOrderService.insertDropshipOrder(dropshipOrderObj);
//
//													String orderId = ParamUtil.getString(orderMap, AppParams.ID);
//													for (DropshipOrderProductObj odObj : orderProductObjs) {
//														odObj.setOrderId(orderId);
//														DropshipOrderProductService.insertDropshipOrderProduct(odObj);
//													}
//												}
//
//											}
//
//										}
//
//									}
//								}
//							}
//						} else {
//							LOGGER.log(Level.WARNING, "INVALID STORE: " + storeId);
//
//							throw new BadRequestException(SystemError.INVALID_STORE);
//						}
//
//					} catch (SQLException | ParseException e) {
//						routingContext.fail(e);
//					}
//
//					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
//					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
//					routingContext.put(AppParams.RESPONSE_DATA, Collections.EMPTY_MAP);

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

	}

	private String saveShippingInfo(Map shippingAddress, Map billingData) throws SQLException {

		String email = ParamUtil.getString(billingData, AppParams.EMAIL);
		String phone = ParamUtil.getString(billingData, AppParams.PHONE);

		String name = ParamUtil.getString(shippingAddress, AppParams.FIRST_NAME) + StringPool.SPACE
				+ ParamUtil.getString(shippingAddress, AppParams.LAST_NAME);

		String line1 = ParamUtil.getString(shippingAddress, AppParams.ADDRESS_1);
		String line2 = ParamUtil.getString(shippingAddress, AppParams.ADDRESS_2);
		String city = ParamUtil.getString(shippingAddress, AppParams.CITY);
		String state = ParamUtil.getString(shippingAddress, AppParams.STATE);
		String postalCode = ParamUtil.getString(shippingAddress, AppParams.POSTCODE);
		String countryCode = ParamUtil.getString(shippingAddress, AppParams.COUNTRY);
		String countryName = ParamUtil.getString(shippingAddress, AppParams.COUNTRY_NAME);

		Map shipping = ShippingService.insert(name, email, phone, line1, line2, city, state, postalCode, countryCode,
				countryName);

		return ParamUtil.getString(shipping, AppParams.ID);
	}

	private String createOrderPrefix(DropshipOrderProductObj obj) {

		StringBuilder prefix = new StringBuilder();
		prefix.append(obj.getCampaignId()).append(StringPool.DASH).append(obj.getBaseShortCode());

		return prefix.toString();
	}

	private DropshipOrderProductObj createOrderItem(String countryCode, String currency, Map orderItem, String userId)
			throws SQLException, ParseException {

		DropshipOrderProductObj orderProductObj = null;
		if (orderItem != null && !orderItem.isEmpty()) {

			String skuSize = ParamUtil.getString(orderItem, AppParams.SKU);
			String[] parsedStrs = skuSize.split("\\|");
			String referenceId = ParamUtil.getString(orderItem, AppParams.ID);
			String variantId = "", sizeId = "", state = ResourceStates.APPROVED;
			if (parsedStrs.length >= 2) {
				variantId = parsedStrs[0];
				sizeId = parsedStrs[1];
			}

			if (!variantId.isEmpty() && !sizeId.isEmpty()
					&& DropshipOrderService.validationSourceOrderByVariantAndUserId(variantId, userId + "-")) {

				Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);
//				LOGGER.info("variantMap=" + new JsonObject(variantMap).toString());
				if (!variantMap.isEmpty()) {
					int quantity = ParamUtil.getInt(orderItem, AppParams.QUANTITY);
					String campaignId = ParamUtil.getString(variantMap, AppParams.CAMPAIGN_ID);
					String campaignState = CampaignService.getCampaignState(campaignId);
					if (!StringUtils.isEmpty(campaignState) && !ResourceStates.LOCKED.equalsIgnoreCase(campaignState)) {
						String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);
						String variantName = ParamUtil.getString(variantMap, AppParams.NAME);
						String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
						double baseCost = ParamUtil.getDouble(variantMap, AppParams.BASE_COST);
						String baseShortCode = ParamUtil.getString(variantMap, AppParams.BASE_SHORT_CODE);

						String colorId = ParamUtil.getString(variantMap, AppParams.COLOR_ID);
						String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
						String colorValue = ParamUtil.getString(variantMap, AppParams.COLOR);
						String sizeName = ParamUtil.getString(variantMap, AppParams.SIZE_NAME);

						Map image = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
						String variantFrontUrl = ParamUtil.getString(image, AppParams.FRONT);
						String variantBackUrl = ParamUtil.getString(image, AppParams.BACK);

						String designFrontUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_FRONT_URL);
						String designBackUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_BACK_URL);

						Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, countryCode, quantity);
						Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);
						Double taxAmount = ParamUtil.getDouble(feeMap, AppParams.TAX_AMOUNT);
						double productAmount = GetterUtil.format(baseCost * quantity + shippingFee + taxAmount, 2);

//						orderProductObj = new DropshipOrderProductObj("", campaignId, productId, variantId, variantName,
//								sizeId, baseCost, currency, quantity, shippingFee, amount, baseId, baseCost,
//								baseShortCode, state, referenceId);
//						orderProductObj.setOrderId(orderId);
						orderProductObj.setCampaignId(campaignId);
						orderProductObj.setProductId(productId);
						orderProductObj.setVariantId(variantId);
						orderProductObj.setSizeId(sizeId);
						orderProductObj.setPrice(baseCost);
						orderProductObj.setShippingFee(shippingFee);
						orderProductObj.setCurrency(currency);
						orderProductObj.setQuantity(quantity);
						orderProductObj.setState(ResourceStates.APPROVED);
						orderProductObj.setVariantName(variantName);
						orderProductObj.setAmount(productAmount);
						orderProductObj.setBaseCost(baseCost);
						orderProductObj.setBaseId(baseId);
						orderProductObj.setLineItemId(referenceId);
						orderProductObj.setVariantFrontUrl(variantFrontUrl);
						orderProductObj.setVariantBackUrl(variantBackUrl);
						orderProductObj.setColorId(colorId);
						orderProductObj.setColorValue(colorValue);
//						orderProductObj.setPartnerSku(setPartnerSku);
						orderProductObj.setColorName(colorName);
						orderProductObj.setSizeName(sizeName);
//						orderProductObj.setShippingMethod(setShippingMethod);
//						orderProductObj.setPrintDetail(setPrintDetail);
						orderProductObj.setItemType(ResourceStates.NORMAL);
//						orderProductObj.setPartnerProperties(setPartnerProperties);
//						orderProductObj.setPartnerOption(setPartnerOption);
						orderProductObj.setBaseShortCode(baseShortCode);
						orderProductObj.setDesignFrontUrl(designFrontUrl);
						orderProductObj.setDesignBackUrl(designBackUrl);
						orderProductObj.setTaxAmount(taxAmount);
					}
				}
			}

		}
		return orderProductObj;
	}

	public static Map woocommerceGetOrderById(String domain, String consumer_key, String consumer_secret,
			String orderId) throws UnirestException {

		String formatUrl = MessageFormat.format("{0}/wp-json/wc/v3/orders/{1}?consumer_key={2}&consumer_secret={3}",
				domain, orderId, consumer_key, consumer_secret);
		HttpResponse<String> response = Unirest.post(formatUrl).header("Content-Type", "application/json").asString();
		Map result = new LinkedHashMap<>();
		if (response.getStatus() == 200) {
			result = new JsonObject(response.getBody()).getMap();

		}
		return result;
	}

	private static final Logger LOGGER = Logger.getLogger(WooCommerceOrderHandler.class.getName());
}
