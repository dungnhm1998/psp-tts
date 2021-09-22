package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.interfaces.LoggerInterface;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.EmailObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.EmailMarketingService;
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
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyOrderPaidHandler extends PSPOrderHandler implements Handler<RoutingContext>, LoggerInterface {

	private static final String USD = "USD";

	@Override
	public void handle(RoutingContext routingContext) {

//		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
//			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
//		}

		routingContext.vertx().executeBlocking(future -> {

			try {

				// Verify the webhook
//				Set<String> headerNames = routingContext.request().headers().names();
//				headerNames.stream()
//						.filter(header -> Arrays.asList(REQUIRED_HEADERS).stream().anyMatch(header::equalsIgnoreCase))
//						.forEach(header -> LOGGER.info("[REQUEST] HEADER: " + header + StringPool.SPACE
//								+ StringPool.COLON + StringPool.SPACE + routingContext.request().getHeader(header)));
//				// end verification

//				logger.info("shopifyy = " + routingContext.getBodyAsJson().encode());
//				Map orderRequestBody = routingContext.getBodyAsJson().getMap();
////				try {
//				String storeId = routingContext.request().params().get(AppParams.ID);
//				if (StringUtils.isEmpty(storeId)) {
//					throw new BadRequestException(SystemError.INVALID_REQUEST);
//				}
//				
////				DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);
//				
//				Map dropshipStore = DropShipStoreService.lookUp(storeId);
//
//				String storeState = ParamUtil.getString(dropshipStore, AppParams.STATE);
//
//				if (!dropshipStore.isEmpty() && ResourceStates.APPROVED.equals(storeState)) {
//					
//					String financialStatus = ParamUtil.getString(orderRequestBody, "financial_status");
//
//					if ("paid".equals(financialStatus)) {
//
//						initItemGroupQuantity();
//
//						Map shippingAddressMap = ParamUtil.getMapData(orderRequestBody, AppParams.SHIPPING_ADDRESS);
//						if (shippingAddressMap == null || shippingAddressMap.isEmpty()) {
//
//							routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
//							routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
//
//						} else {
//
//							String referenceOrderId = ParamUtil.getString(orderRequestBody, AppParams.NAME);
////							boolean isAdded = DropshipOrderService.checkIfAddedOrder(storeId, referenceOrderId);
//							boolean isAdded = DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId,
//									referenceOrderId, ResourceSource.CAMP_WEBOOK);
//							if (!isAdded) {
//								addDropshipOrder(orderRequestBody, storeId, dropshipStore, referenceOrderId);
//							}
//						}
//					}
//				}

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, Collections.EMPTY_MAP);

				future.complete();

			} catch (Exception e) {
//				logger.severe("Fail to process shopify request");
//				logger.severe("Error message = " + e.getMessage());
//
//				String emailBody = routingContext.getBodyAsJson().encode();
//				String emailSubject = "[BurgerPrints] Fail to process shopify request";
//				String user = "tuan@leadsgen.asia";
//
//				EmailObj em = new EmailObj("notify", user, emailSubject, emailBody, "pending", "", "image");
//				EmailMarketingService.insert(em);
//
//				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
//				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
//				routingContext.put(AppParams.RESPONSE_DATA, Collections.EMPTY_MAP);
//
//				future.complete();
			}
		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	private void addDropshipOrder(Map orderRequestBody, String storeId, Map dropshipStore, String referenceOrderId)
			throws SQLException, ParseException {
		String userId = "", shippingId = "", trackingNumber = "", note = "", channel = "shopify",
				shippingCountryCode = "";
		double orderAmount = 0d, subAmount = 0d, shippingFee = 0d;
		int totalItems = 0;
		Double toltalTax = 0.00d;
		List<Map> orderItems = ParamUtil.getListData(orderRequestBody, AppParams.LINE_ITEMS);

		if (orderItems != null && !orderItems.isEmpty()) {
			List<DropshipOrderProductObj> listDropshipOrderProduct = new ArrayList<>();

			Map shippingAddress = ParamUtil.getMapData(orderRequestBody, AppParams.SHIPPING_ADDRESS);

			shippingCountryCode = ParamUtil.getString(shippingAddress, AppParams.COUNTRY_CODE);
			Map customerInfo = ParamUtil.getMapData(orderRequestBody, AppParams.CUSTOMER);
			for (Map orderItem : orderItems) {
				DropshipOrderProductObj dropshipOrderProductObj = createOrderItem(shippingCountryCode, USD, orderItem);
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
					toltalTax += odObj.getShippingFee();
					totalItems += odObj.getQuantity();
				}
				orderAmount = GetterUtil.format(subAmount + shippingFee + toltalTax, 2);
				subAmount = GetterUtil.format(subAmount, 2);
				userId = ParamUtil.getString(dropshipStore, AppParams.USER_ID);
				String orderIdPrefix = createOrderPrefix(listDropshipOrderProduct.get(0));
				// check orderId match userId
				if (orderIdPrefix.contains(userId + StringPool.DASH)) {
					DropshipOrderObj dropshipOrderObj = new DropshipOrderObj.Builder(orderIdPrefix)
							.orderAmount(orderAmount)
							.orderCurrency("USD")
							.state(ResourceStates.QUEUED)
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
							.source(ResourceSource.CAMP_WEBOOK)
							.minifiedJson(new JsonObject(orderRequestBody).encode())
							.taxAmount(toltalTax)
							.build();

					Map orderMap = DropshipOrderService.insertDropshipOrder(dropshipOrderObj);
					String orderId = ParamUtil.getString(orderMap, AppParams.ID);
					for (DropshipOrderProductObj odObj : listDropshipOrderProduct) {
						odObj.setOrderId(orderId);
						DropshipOrderProductService.insertDropshipOrderProduct(odObj);
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
		String postalCode = ParamUtil.getString(shippingAddress, AppParams.ZIP);
		String countryCode = ParamUtil.getString(shippingAddress, AppParams.COUNTRY_CODE);
		Locale locale = new Locale("", countryCode);
		String countryName = locale.getDisplayCountry();
		String province = "";
		if ("US".equalsIgnoreCase(countryCode)
				|| "CA".equalsIgnoreCase(countryCode)) {
			province = ParamUtil.getString(shippingAddress, AppParams.PROVINCE_CODE);
		} else if ("MX".equalsIgnoreCase(countryCode)) {
			String provinceName = ParamUtil.getString(shippingAddress, AppParams.PROVINCE);
			province = ShippingService.mexicoStateAlpha2Code.get(provinceName);	
			logger.info("province 0:=" + province);
			// Fix bug
			if (StringUtils.isEmpty(province)) {
				province = ParamUtil.getString(shippingAddress, AppParams.PROVINCE_CODE);
				logger.info("province 1:=" + province);
			}
		} else {
			province = ParamUtil.getString(shippingAddress, AppParams.PROVINCE);
		}

		Map shipping = ShippingService.insert(name, email, phone, line1, line2, city, province, postalCode, countryCode,
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
				Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId,sizeId);
				if (!variantMap.isEmpty()) {
					int quantity = ParamUtil.getInt(orderItem, AppParams.QUANTITY);
					String campaignId = ParamUtil.getString(variantMap, AppParams.CAMPAIGN_ID);
					String campaignState = CampaignService.getCampaignState(campaignId);
					if (!StringUtils.isEmpty(campaignState) && !ResourceStates.LOCKED.equalsIgnoreCase(campaignState)) {
						String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);
						String variantName = ParamUtil.getString(variantMap, AppParams.NAME);
						String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
						String baseShortCode = ParamUtil.getString(variantMap, AppParams.BASE_SHORT_CODE);
						double baseCost = ParamUtil.getDouble(variantMap, AppParams.BASE_COST);
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
//						orderProductObj.setShippingMethod(setShippingMethod);
//						orderProductObj.setPrintDetail(setPrintDetail);
						dropshipOrderProduct.setItemType(ResourceStates.NORMAL);
//						orderProductObj.setPartnerProperties(setPartnerProperties);
//						orderProductObj.setPartnerOption(setPartnerOption);
						dropshipOrderProduct.setBaseShortCode(baseShortCode);
						dropshipOrderProduct.setTaxAmount(taxAmount);
					}
				}
			}

		}
		return dropshipOrderProduct;
	}

}
