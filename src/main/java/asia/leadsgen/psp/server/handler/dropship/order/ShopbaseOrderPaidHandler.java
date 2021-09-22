package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

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
import asia.leadsgen.psp.thread.DropshipChargeOrderThread;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopbaseOrderPaidHandler extends PSPOrderHandler implements Handler<RoutingContext> {

	private static final String USD = "USD";

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking(future -> {

			try {

				LOGGER.info("shopbase = " + routingContext.getBodyAsJson().encode());

				Map orderRequestBody = routingContext.getBodyAsJson().getMap();
				try {
					String storeId = routingContext.request().params().get(AppParams.ID);
					if (StringUtils.isEmpty(storeId)) {
						throw new BadRequestException(SystemError.INVALID_REQUEST);
					}
					
					DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);
					
					Map dropshipStore = DropShipStoreService.lookUp(storeId);
					if (!dropshipStore.isEmpty()) {
						
						String financialStatus = ParamUtil.getString(orderRequestBody, "financial_status");
						Boolean autoFulfill = ParamUtil.getBoolean(dropshipStore, AppParams.AUTO_FULFILL);

						if ("paid".equals(financialStatus)) {
							initItemGroupQuantity();
							Map shippingAddressMap = ParamUtil.getMapData(orderRequestBody, AppParams.SHIPPING_ADDRESS);
							if (shippingAddressMap == null || shippingAddressMap.isEmpty()) {
								routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
								routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
							} else {
								String referenceOrderId = ParamUtil.getString(orderRequestBody, AppParams.ID);
								boolean isAdded = DropshipOrderService.checkIfAddedOrder(storeId, referenceOrderId);

								if (!isAdded) {
									addDropshipOrder(orderRequestBody, storeId, dropshipStore, autoFulfill,
											referenceOrderId);
								}

							}
						}
					}

				} catch (SQLException | ParseException e) {
					routingContext.fail(e);
				}

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

	private void addDropshipOrder(Map orderRequestBody, String storeId, Map dropshipStore, Boolean autoFulfill,
			String referenceOrderId) throws SQLException, ParseException {
		String userId = "", shippingId = "", trackingNumber = "", note = "", channel = "shopbase",
				shippingCountryCode = "";
		double orderAmount = 0d, subAmount = 0d, shippingFee = 0d;
		int totalItems = 0;
		Double totalTax = 0.00;

		List<Map> orderItems = ParamUtil.getListData(orderRequestBody, AppParams.LINE_ITEMS);

		if (orderItems != null && !orderItems.isEmpty()) {
			List<DropshipOrderProductObj> orderProductObjs = new ArrayList<>();

			Map shippingAddress = ParamUtil.getMapData(orderRequestBody, AppParams.SHIPPING_ADDRESS);

			shippingCountryCode = ParamUtil.getString(shippingAddress, AppParams.COUNTRY_CODE);
			Map customerInfo = ParamUtil.getMapData(orderRequestBody, AppParams.CUSTOMER);
			for (Map orderItem : orderItems) {
				DropshipOrderProductObj dropshipOrderProductObj = createOrderItem(shippingCountryCode, USD, orderItem);
				if (dropshipOrderProductObj != null) {
					orderProductObjs.add(dropshipOrderProductObj);
				}
			}
			if (!orderProductObjs.isEmpty()) {

				shippingId = saveShippingInfo(shippingAddress, customerInfo);
				trackingNumber = AppUtil.generateOrderTrackingNumber();
				for (DropshipOrderProductObj odObj : orderProductObjs) {
					subAmount += odObj.getPrice() * odObj.getQuantity();
					shippingFee += odObj.getShippingFee();
					totalTax += odObj.getTaxAmount();
					totalItems += odObj.getQuantity();
				}
				orderAmount = GetterUtil.format(subAmount + shippingFee + totalTax, 2);
				subAmount = GetterUtil.format(subAmount, 2);
				userId = ParamUtil.getString(dropshipStore, AppParams.USER_ID);
				String orderIdPrefix = createOrderPrefix(orderProductObjs.get(0));

				// check orderId match userId
				if (orderIdPrefix.contains(userId + StringPool.DASH)) {
					DropshipOrderObj orderObj = new DropshipOrderObj.Builder(orderIdPrefix)
							.orderAmount(orderAmount)
							.orderCurrency("USD")
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
							.minifiedJson(new JsonObject(orderRequestBody).encode())
							.taxAmount(totalTax)
							.build();

					Map orderMap = DropshipOrderService.insertDropshipOrder(orderObj);
					String orderId = ParamUtil.getString(orderMap, AppParams.ID);
					for (DropshipOrderProductObj odObj : orderProductObjs) {
						odObj.setOrderId(orderId);
						DropshipOrderProductService.insertDropshipOrderProduct(odObj);
					}
					if (autoFulfill) {
						DropshipChargeOrderThread chargeOrderThread = new DropshipChargeOrderThread(orderId, orderObj,
								userId);
						chargeOrderThread.start();
					}
				}
			}
		}
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

			if (!variantId.isEmpty() && !sizeId.isEmpty()) {
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
//						DropshipOrderProductTypeObj orderProductObj = new DropshipOrderProductTypeObj();
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
//						orderProductObj.setPartnerSku();
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

	private static final Logger LOGGER = Logger.getLogger(ShopbaseOrderPaidHandler.class.getName());
}
