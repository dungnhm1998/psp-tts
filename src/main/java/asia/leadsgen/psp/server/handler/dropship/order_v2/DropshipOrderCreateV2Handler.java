package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import asia.leadsgen.psp.obj.DropshipOrderProductTypeObj;
import asia.leadsgen.psp.obj.DropshipOrderTypeObj;
import asia.leadsgen.psp.service_fulfill.DropshipOrderServiceV2;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service.ProductService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.OrderUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderCreateV2Handler extends PSPOrderHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				
				initItemGroupQuantity();

				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

				Map requestOrderInfoMap = routingContext.getBodyAsJson().getMap();
				LOGGER.info("requestOrderInfoMap: " + requestOrderInfoMap.toString());
				String storeId = ParamUtil.getString(requestOrderInfoMap, AppParams.STORE_ID);
				if (StringUtils.isEmpty(storeId)) {
					throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
				}
				String referenceOrderId = ParamUtil.getString(requestOrderInfoMap, AppParams.REFERENCE_ID);
				String orderCurrency = ParamUtil.getString(requestOrderInfoMap, AppParams.CURRENCY);
				String note = ParamUtil.getString(requestOrderInfoMap, AppParams.NOTE);
				String source =  ParamUtil.getString(requestOrderInfoMap, AppParams.SOURCE);
				String shippingMethod = ParamUtil.getString(requestOrderInfoMap, AppParams.SHIPPING_METHOD);
				LOGGER.info("source: " + source);
				if (StringUtils.isEmpty(source)) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
				}
				
				if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId,
						referenceOrderId, source)) {
					throw new BadRequestException(SystemError.DUPLICATE_REFERENCE_ORDER);
				}

				String iossNumber = ParamUtil.getString(requestOrderInfoMap, AppParams.IOSS_NUMBER);
				Boolean isValidIossNumber = OrderUtil.checkValidIossNumber(iossNumber);

				if (!isValidIossNumber) {
					throw new BadRequestException(SystemError.INVALID_IOSS_NUMBER);
				}

				Map shippingInfoMap = ParamUtil.getMapData(requestOrderInfoMap, AppParams.SHIPPING);
				String name = ParamUtil.getString(shippingInfoMap, AppParams.NAME);
				String email = ParamUtil.getString(shippingInfoMap, AppParams.EMAIL);
				String phone = ParamUtil.getString(shippingInfoMap, AppParams.PHONE);

				Map address = ParamUtil.getMapData(shippingInfoMap, AppParams.ADDRESS);	
				String line1 = ParamUtil.getString(address, AppParams.LINE1);
				String line2 = ParamUtil.getString(address, AppParams.LINE2);
				String city = ParamUtil.getString(address, AppParams.CITY);
				String state = ParamUtil.getString(address, AppParams.STATE);
				String postalCode = ParamUtil.getString(address, AppParams.POSTAL_CODE);
				String countryCode = ParamUtil.getString(address, AppParams.COUNTRY, "US");
				String countryName = ParamUtil.getString(address, AppParams.COUNTRY_NAME);
				boolean addrVerified = ParamUtil.getBoolean(address, AppParams.ADDR_VERIFIED);
				int isAddrVerified = (addrVerified == false) ? 0 : 1;
				LOGGER.info("isAddrVerified: " + isAddrVerified);

				Map shippingMap = ShippingService.insert(name, email, phone, line1, line2, city, state, postalCode,
						countryCode, countryName);
				String shippingId = ParamUtil.getString(shippingMap, AppParams.ID);

				String trackingNumber = AppUtil.generateOrderTrackingNumber();
				List<Map> requestOrderItemList = ParamUtil.getListData(requestOrderInfoMap, AppParams.ITEMS);

				int quantity = 0;
				int totalItems = 0;
				Double totalTax = 0.0d;
				for (Map requestOrderItem : requestOrderItemList) {

					String campaignId = ParamUtil.getString(requestOrderItem, AppParams.CAMPAIGN_ID);
					String campaignState = CampaignService.getCampaignState(campaignId);
					if (StringUtils.isEmpty(campaignState) || ResourceStates.LOCKED.equalsIgnoreCase(campaignState)) {
						throw new BadRequestException(SystemError.INVALID_CAMPAIGN);
					}

					quantity = ParamUtil.getInt(requestOrderItem, AppParams.QUANTITY);
					if (quantity <= 0) {
						throw new BadRequestException(SystemError.INVALID_ORDER);
					}
					totalItems += quantity;
				}

				String orderIdPrefix = createOrderIdPrefix(requestOrderItemList);

				DropshipOrderTypeObj dropshipOrderObj = DropshipOrderTypeObj.builder()
						.idPrefix(orderIdPrefix)
						.currency(orderCurrency)
						.state(ResourceStates.QUEUED)
						.shippingId(shippingId)
						.trackingCode(trackingNumber)
						.note(note)
						.storeId(storeId)
						.userId(userId)
						.referenceOrder(referenceOrderId)
						.totalItem(totalItems)
						.source(source)
						.addrVerified(isAddrVerified)
						.iossNumber(iossNumber)
						.build();

				LOGGER.info("dropshipOrderObj: " + dropshipOrderObj.toString());

				Map orderInfoMap = DropshipOrderServiceV2.insertDropshipOrderV2(dropshipOrderObj);

				Map countryTax = CountryTaxService.getTaxByCountry(countryCode);

				String orderId = ParamUtil.getString(orderInfoMap, AppParams.ID);

				try {

					orderInfoMap = createOrderItems(requestOrderItemList, "", orderId, countryCode, orderCurrency, storeId, referenceOrderId, isAddrVerified, shippingMethod, countryTax, iossNumber);

					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, orderInfoMap);

				} catch (Exception e) {
                	e.printStackTrace();
                	LOGGER.severe(e.toString());
                	DropshipOrderService.updateStateV2(orderId, ResourceStates.DELETED);
                	LOGGER.info("delete orderId: " + orderId);
                	routingContext.fail(e);
                }

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

	private String createOrderIdPrefix(List<Map> requestOrderItemList) throws SQLException {

		StringBuilder prefix = new StringBuilder();
		if (requestOrderItemList.isEmpty() == false) {
			Map firstItems = requestOrderItemList.get(0);
			String campaignId = ParamUtil.getString(firstItems, AppParams.CAMPAIGN_ID);
			String productId = ParamUtil.getString(firstItems, AppParams.PRODUCT_ID);
			Map productInfoMap = ProductService.get(productId, true, false, false, false);
			Map base = ParamUtil.getMapData(productInfoMap, AppParams.BASE);
			String baseShortCode = ParamUtil.getString(base, AppParams.SHORT_CODE);
			if (baseShortCode == null || baseShortCode.isEmpty()) {
				throw new BadRequestException(SystemError.INVALID_BASE_ID);
			}
			prefix.append(campaignId).append(StringPool.DASH).append(baseShortCode);
		}
		return prefix.toString();

	}

	private Map createOrderItems(List<Map> requestOrderItemList, String promotionCode, String orderId, String shippingCountryCode,
			String orderCurrency, String storeId, String referenceOrderId, int isAddrVerified, String shippingMethod, Map countryTax, String iossNumber) throws Exception {

		List<Map> orderItemList = new ArrayList<>();
		Double orderAmount = 0.00;
		int totalItems = 0;
		Double totalShippingFee = 0.00;

		Set<String> setBaseId = OrderUtil.getSetBaseFromItemCamp(requestOrderItemList);

		Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, shippingCountryCode, shippingMethod);

		Double totalTax = 0d;

		for (Map requestItem : requestOrderItemList) {
			Map orderItem = createOrderItem(orderId, requestItem, orderCurrency, promotionCode, shippingMethod ,shippingInfo, countryTax, iossNumber);

			orderAmount += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));;
			totalShippingFee+= GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
			totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
			totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));

			orderItemList.add(orderItem);

		}
		String addrVerifiedNote = "";
		if ("US".equalsIgnoreCase(shippingCountryCode) && isAddrVerified == 1) {
			addrVerifiedNote = "Seller agree for bypass address verified";
		}

		if (shippingMethod.equalsIgnoreCase("express")) {
			DropshipOrderProductService.updateShippingMethod(orderId, shippingMethod);
		}

		LOGGER.info("--orderAmount = " + orderAmount + " --totalTax = " + totalTax);
		orderAmount = GetterUtil.format(orderAmount, 2);
		totalTax = GetterUtil.format(totalTax, 2);

		Map orderInfoMap = DropshipOrderService.updateOrderV2(orderId, orderAmount.toString(), orderCurrency,
				ResourceStates.QUEUED, StringPool.BLANK, storeId, referenceOrderId, totalItems, isAddrVerified, addrVerifiedNote, totalTax.toString(), totalShippingFee, iossNumber);

		orderInfoMap.put(AppParams.ITEMS, orderItemList);

		return orderInfoMap;
	}

	private Map createOrderItem(String orderId, Map requestItem, String currency, String promotionCode,
			String shippingMethod, Map shippingInfo, Map countryTax, String iossNumber) throws SQLException {

		LOGGER.info("requestItem: " + requestItem.toString());
		String campaignId = ParamUtil.getString(requestItem, AppParams.CAMPAIGN_ID);
		String productId = ParamUtil.getString(requestItem, AppParams.PRODUCT_ID);
		String variantId = ParamUtil.getString(requestItem, AppParams.VARIANT_ID);
		String variantName = ParamUtil.getString(requestItem, AppParams.VARIANT_NAME);
		String sizeId = ParamUtil.getString(requestItem, AppParams.SIZE_ID);
		int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
		String colorNameRequest = ParamUtil.getString(requestItem, AppParams.COLOR_NAME);
		String sizeNameRequest = ParamUtil.getString(requestItem, AppParams.SIZE_NAME);
		String customData = ParamUtil.getString(requestItem, AppParams.CUSTOM_DATA);
		String typeItem = ResourceStates.NORMAL;
		if (!StringUtils.isEmpty(customData)) {
			typeItem = ResourceStates.PERSONALIZE;
			JsonObject printDetail = new JsonObject();
			printDetail.put("design", "");
			printDetail.put(ResourceStates.PERSONALIZE, customData);
			customData = printDetail.toString();
		}

		Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);

		String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
		double baseCost = ParamUtil.getDouble(variantMap, AppParams.BASE_COST);
		String baseShortCode = ParamUtil.getString(variantMap, AppParams.BASE_SHORT_CODE);

		String colorId = ParamUtil.getString(variantMap, AppParams.COLOR_ID);
		String colorValue = ParamUtil.getString(variantMap, AppParams.COLOR);

		String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
		String sizeName = ParamUtil.getString(variantMap, AppParams.SIZE_NAME);
		LOGGER.info("get colorNameDB: " + colorName);
        LOGGER.info("get sizeNameDB: " + sizeName);

		if (!colorName.equalsIgnoreCase(colorNameRequest) || !sizeName.equalsIgnoreCase(sizeNameRequest)) {
        	LOGGER.info("INVALID COLOR_ID OR SIZE_ID");
        	throw new BadRequestException(SystemError.INVALID_COLOR_OR_SIZE);
        }

		Map image = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
		String variantFrontUrl = ParamUtil.getString(image, AppParams.FRONT);
		String variantBackUrl = ParamUtil.getString(image, AppParams.BACK);
		String designFrontUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_FRONT_URL);
		String designBackUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_BACK_URL);

		String unitAmount = ParamUtil.getString(requestItem, AppParams.UNIT_AMOUNT);

		Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseId, shippingMethod, quantity, shippingInfo);

		Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);

		double productAmount = GetterUtil.format(baseCost * quantity + shippingFee, 2);
		LOGGER.info("+++productAmount = " + productAmount);
		Double taxAmount =0d;
		if (StringUtils.isEmpty(iossNumber)) {
			taxAmount = OrderUtil.getTaxByAmountAndByCountry(productAmount, countryTax);
		}
		productAmount = GetterUtil.format(productAmount + taxAmount, 2);
		LOGGER.info("+++taxAmount = " + taxAmount);

		DropshipOrderProductTypeObj orderProductObj = DropshipOrderProductTypeObj.builder()
				.orderId(orderId)
				.campaignId(campaignId)
				.productId(productId)
				.variantId(variantId)
				.sizeId(sizeId)
				.price(String.valueOf(baseCost))
				.shippingFee(String.valueOf(shippingFee))
				.currency(currency)
				.quantity(quantity)
				.state(ResourceStates.APPROVED)
				.variantName(variantName)
				.amount(String.valueOf(productAmount))
				.baseCost(String.valueOf(baseCost))
				.baseId(baseId)
//				.lineItemId(setLineItemId)
				.variantFrontUrl(variantFrontUrl)
				.variantBackUrl(variantBackUrl)
				.colorId(colorId)
				.colorValue(colorValue)
				.designFrontUrl(designFrontUrl)
				.designBackUrl(designBackUrl)
//				.baseShortCode(baseShortCode)
//				.partnerSku(setPartnerSku)
				.colorName(colorName)
				.sizeName(sizeName)
				.shippingMethod(shippingMethod)
				.printDetail(customData)
				.itemType(typeItem)
//				.partnerProperties(setPartnerProperties)
//				.partnerOption(setPartnerOption)
				.unitAmount(unitAmount)
				.taxAmount(String.valueOf(taxAmount))
				.build();

		LOGGER.info("orderProductObj: " + orderProductObj.toString());
		Map orderItem = DropshipOrderProductService.insertDropshipOrderProductV2(orderProductObj);

		return orderItem;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderCreateV2Handler.class.getName());

}