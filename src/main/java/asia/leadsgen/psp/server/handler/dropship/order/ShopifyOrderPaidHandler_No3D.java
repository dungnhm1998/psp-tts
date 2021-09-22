package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.ProductService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.thread.DropshipChargeOrderThread;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyOrderPaidHandler_No3D extends PSPOrderHandler implements Handler<RoutingContext> {

	static String _3DBase = "AVXTaGbsKjIKXkcT|GAFofttyq18A5l2u|aIEqbcaqYKa0u8mr|QOYmaDsgdE7nNy21|lWoVznlz828x8wvd|jUfTaGbsKjIKXkcT|zUCKpLPrl7xnsQU5|Bj0pbciRSov9PhE4|xaLgxhHX9KFQfLNY|KRfcLkcYItB4MQmG";
	int total2DItems = 0;
	int total3DItems = 0;

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking(future -> {

			try {

				// Verify the webhook
				Set<String> headerNames = routingContext.request().headers().names();
				headerNames.stream()
						.filter(header -> Arrays.asList(REQUIRED_HEADERS).stream().anyMatch(header::equalsIgnoreCase))
						.forEach(header -> LOGGER.info("[REQUEST] HEADER: " + header + StringPool.SPACE
								+ StringPool.COLON + StringPool.SPACE + routingContext.request().getHeader(header)));
				// end verification

				Map orderRequestBody = routingContext.getBodyAsJson().getMap();
				LOGGER.info("[Shopify request]=" + routingContext.getBodyAsJson().toString());

				new Thread(() -> {
					Map orderRequestBodyCopy = orderRequestBody;
					try {
						String storeId = routingContext.request().params().get(AppParams.ID);
						if (StringUtils.isEmpty(storeId)) {
							throw new BadRequestException(SystemError.INVALID_REQUEST);
						}

						Map dropshipStore = DropShipStoreService.lookUp(storeId);
						if (dropshipStore.isEmpty()) {
							throw new BadRequestException(SystemError.INVALID_REQUEST);
						}

						String financialStatus = ParamUtil.getString(orderRequestBodyCopy, "financial_status");
						if (!"paid".equals(financialStatus)) {
							throw new BadRequestException(SystemError.INVALID_REQUEST);
						}

						Map shippingAddressMap = ParamUtil.getMapData(orderRequestBodyCopy, AppParams.SHIPPING_ADDRESS);
						if (shippingAddressMap == null || shippingAddressMap.isEmpty()) {
							routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
							routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());

						} else {

							String userId = "", referenceOrderId = "", currency = "", shippingId = "",
									trackingNumber = "", note = "", channel = "shopify", shippingCountryCode = "";
							double orderAmount = 0d, subAmount = 0d, shippingFee = 0d;
							int totalItems = 0;
							Double totalTax =0.00d;

							List<Map> orderItems = ParamUtil.getListData(orderRequestBodyCopy, AppParams.LINE_ITEMS);

							if (orderItems != null && !orderItems.isEmpty()) {
								List<DropshipOrderProductObj> listDropshipOrderProduct = new ArrayList<>();

								Map shippingAddress = ParamUtil.getMapData(orderRequestBodyCopy,
										AppParams.SHIPPING_ADDRESS);

								shippingCountryCode = ParamUtil.getString(shippingAddress, AppParams.COUNTRY_CODE);

								Map customerInfo = ParamUtil.getMapData(orderRequestBodyCopy, AppParams.CUSTOMER);

								currency = ParamUtil.getString(orderRequestBodyCopy, AppParams.CURRENCY);

								for (Map orderItem : orderItems) {
									DropshipOrderProductObj dropshipOrderProductObj = createOrderItem(
											shippingCountryCode, currency, orderItem);
									if (dropshipOrderProductObj != null) {
										listDropshipOrderProduct.add(dropshipOrderProductObj);
									}
								}

								if (!listDropshipOrderProduct.isEmpty()) {

									shippingId = saveShippingInfo(shippingAddress, customerInfo);

									trackingNumber = AppUtil.generateOrderTrackingNumber();

									for (DropshipOrderProductObj odObj : listDropshipOrderProduct) {
										subAmount += odObj.getPrice() * odObj.getQuantity();
										shippingFee += odObj.getShippingFee();
										totalTax += odObj.getTaxAmount();
										totalItems += odObj.getQuantity();
									}

									orderAmount = GetterUtil.format(subAmount + shippingFee + totalTax, 2);
									subAmount = GetterUtil.format(subAmount, 2);

									storeId = routingContext.request().params().get(AppParams.ID);
									referenceOrderId = ParamUtil.getString(orderRequestBodyCopy, AppParams.ID);

									userId = ParamUtil.getString(dropshipStore, AppParams.USER_ID);

									String orderIdPrefix = createOrderPrefix(listDropshipOrderProduct.get(0));
									DropshipOrderObj dropshipOrderObj = new DropshipOrderObj.Builder(orderIdPrefix)
											.orderAmount(orderAmount)
											.orderCurrency(currency)
											.state(ResourceStates.CREATED)
											.shippingId(shippingId)
											.trackingNumber(trackingNumber)
											.note(note)
											.channel(channel)
											.subAmount(subAmount)
											.shippingFee(shippingFee)
											.storeId(storeId)
											.userId(userId)
											.referenceOrderId(referenceOrderId)
											.totalItems(totalItems)
											.taxAmount(totalTax)
											.build();

									Map orderMap = DropshipOrderService.insertDropshipOrder(dropshipOrderObj);
									String orderId = ParamUtil.getString(orderMap, AppParams.ID);
									for (DropshipOrderProductObj odObj : listDropshipOrderProduct) {
										odObj.setOrderId(orderId);
										DropshipOrderProductService.insertDropshipOrderProduct(odObj);
									}

									DropshipChargeOrderThread chargeOrderThread = new DropshipChargeOrderThread(orderId,dropshipOrderObj,
											userId);
									chargeOrderThread.start();
								}
							}
						}
					} catch (SQLException e) {
						LOGGER.severe("[Shopify ERROR] " + e.getMessage());
					} catch (ParseException e) {
						LOGGER.severe("[Shopify ERROR] " + e.getMessage());
					}
				}).start();

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, Collections.EMPTY_MAP);

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

	private String saveShippingInfo(Map shippingAddress, Map customerInfo) throws SQLException {

		String email = ParamUtil.getString(customerInfo, AppParams.EMAIL);
		String name = ParamUtil.getString(shippingAddress, AppParams.FIRST_NAME) + StringPool.SPACE
				+ ParamUtil.getString(shippingAddress, AppParams.LAST_NAME);
		String phone = ParamUtil.getString(shippingAddress, AppParams.PHONE);
		String line1 = ParamUtil.getString(shippingAddress, AppParams.ADDRESS1);
		String line2 = ParamUtil.getString(shippingAddress, AppParams.ADDRESS2);
		String city = ParamUtil.getString(shippingAddress, AppParams.CITY);
		String state = ParamUtil.getString(shippingAddress, AppParams.PROVINCE_CODE);
		String postalCode = ParamUtil.getString(shippingAddress, AppParams.ZIP);
		String countryCode = ParamUtil.getString(shippingAddress, AppParams.COUNTRY_CODE);
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

	private DropshipOrderProductObj createOrderItem(String countryCode, String currency, Map orderItem)
			throws SQLException {

		DropshipOrderProductObj dropshipOrderProduct = null;
		if (orderItem != null && !orderItem.isEmpty()) {

			String skuSize = ParamUtil.getString(orderItem, AppParams.SKU);
			String[] parsedStrs = skuSize.split("\\|");
			String referenceId = ParamUtil.getString(orderItem, AppParams.ID);
			String variantId = "", sizeId = "", state = ResourceStates.APPROVED;
			if (parsedStrs.length >= 2) {
				variantId = parsedStrs[0];
				sizeId = parsedStrs[1];
			}

			if (!variantId.isEmpty() && !sizeId.isEmpty()) {
				Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);
				if (!variantMap.isEmpty()) {
					dropshipOrderProduct = new DropshipOrderProductObj();
					int quantity = ParamUtil.getInt(orderItem, AppParams.QUANTITY);
					String campaignId = ParamUtil.getString(variantMap, AppParams.CAMPAIGN_ID);
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

					if (baseId.matches(_3DBase)) {
						return null;
					}

					Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, countryCode, quantity);
					Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);
					Double taxAmount = ParamUtil.getDouble(feeMap, AppParams.TAX_AMOUNT);
					double productAmount = GetterUtil.format(baseCost * quantity + shippingFee + taxAmount, 2);

//					orderProductObj = new DropshipOrderProductObj("", campaignId, productId, variantId, variantName,
//							sizeId, baseCost, currency, quantity, shippingFee, amount, baseId, baseCost, baseShortCode,
//							state, referenceId);
//					orderProductObj.setOrderId(orderId);
					dropshipOrderProduct.setCampaignId(campaignId);
					dropshipOrderProduct.setProductId(productId);
					dropshipOrderProduct.setVariantId(variantId);
					dropshipOrderProduct.setSizeId(sizeId);
					dropshipOrderProduct.setPrice(baseCost);
					dropshipOrderProduct.setShippingFee(shippingFee);
					dropshipOrderProduct.setCurrency(currency);
					dropshipOrderProduct.setQuantity(quantity);
					dropshipOrderProduct.setState(ResourceStates.APPROVED);
					dropshipOrderProduct.setVariantName(variantName);
					dropshipOrderProduct.setAmount(productAmount);
					dropshipOrderProduct.setBaseCost(baseCost);
					dropshipOrderProduct.setBaseId(baseId);
					dropshipOrderProduct.setLineItemId(referenceId);
					dropshipOrderProduct.setVariantFrontUrl(variantFrontUrl);
					dropshipOrderProduct.setVariantBackUrl(variantBackUrl);
					dropshipOrderProduct.setColorId(colorId);
					dropshipOrderProduct.setColorValue(colorValue);
//						orderProductObj.setPartnerSku(setPartnerSku);
					dropshipOrderProduct.setColorName(colorName);
					dropshipOrderProduct.setSizeName(sizeName);
					dropshipOrderProduct.setDesignFrontUrl(designFrontUrl);
					dropshipOrderProduct.setDesignBackUrl(designBackUrl);
//					orderProductObj.setShippingMethod(setShippingMethod);
//					orderProductObj.setPrintDetail(setPrintDetail);
					dropshipOrderProduct.setItemType(ResourceStates.NORMAL);
//					orderProductObj.setPartnerProperties(setPartnerProperties);
//					orderProductObj.setPartnerOption(setPartnerOption);
					dropshipOrderProduct.setBaseShortCode(baseShortCode);
					dropshipOrderProduct.setTaxAmount(taxAmount);

				}
			}

		}
		return dropshipOrderProduct;
	}

	private static final String[] REQUIRED_HEADERS = { "X-Shopify-Hmac-SHA256" };

	private static final Logger LOGGER = Logger.getLogger(ShopifyOrderPaidHandler_No3D.class.getName());
}
