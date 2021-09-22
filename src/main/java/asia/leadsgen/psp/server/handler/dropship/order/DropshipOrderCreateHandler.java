package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderCreateHandler extends PSPOrderHandler implements Handler<RoutingContext> {

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
				String storeId = ParamUtil.getString(requestOrderInfoMap, AppParams.STORE_ID);
				String orderCurrency = ParamUtil.getString(requestOrderInfoMap, AppParams.CURRENCY);
				String note = ParamUtil.getString(requestOrderInfoMap, AppParams.NOTE);

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

				Map shippingMap = ShippingService.insert(name, email, phone, line1, line2, city, state, postalCode,
						countryCode, countryName);

				String shippingId = ParamUtil.getString(shippingMap, AppParams.ID);

				String trackingNumber = AppUtil.generateOrderTrackingNumber();
				List<Map> requestOrderItemList = ParamUtil.getListData(requestOrderInfoMap, AppParams.ITEMS);
				String orderIdPrefix = createOrderIdPrefix(requestOrderItemList);

				int quantity = 0;
				int totalItems = 0;
				for (Map requestOrderItem : requestOrderItemList) {
					quantity = ParamUtil.getInt(requestOrderItem, AppParams.QUANTITY);
					totalItems += quantity;
				}

				DropshipOrderObj orderObj = new DropshipOrderObj.Builder(orderIdPrefix)
						.orderCurrency(orderCurrency)
						.state(ResourceStates.CREATED)
						.shippingId(shippingId)
						.trackingNumber(trackingNumber)
						.note(note)
						.storeId(storeId)
						.userId(userId)
						.totalItems(totalItems)
						.build();

				Map orderInfoMap = DropshipOrderService.insertDropshipOrder(orderObj);

//				String shippingCountryCode = ParamUtil.getString(ParamUtil.getMapData(shippingInfoMap, AppParams.ADDRESS), AppParams.COUNTRY);

				String orderId = ParamUtil.getString(orderInfoMap, AppParams.ID);
				orderInfoMap = createOrderItems(requestOrderItemList, "", orderId, countryCode, orderCurrency);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, orderInfoMap);

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
			String baseShortCode = ParamUtil.getString(base, AppParams.BASE_SHORT_CODE);
			prefix.append(campaignId).append(StringPool.DASH).append(baseShortCode);
		}
		return prefix.toString();

	}

	private Map createOrderItems(List<Map> requestOrderItemList, String promotionCode, String orderId,
			String shippingCountryCode, String orderCurrency) throws SQLException, ParseException {

		Double orderAmountNumeric = 0.00;
		Double totalTax = 0.00;
		Double totalShippingFee = 0.00;
		int totalItems = 0;
		List<Map> orderItemList = new ArrayList<>();

		for (Map requestItem : requestOrderItemList) {
			Map orderItem = createOrderItem(orderId, requestItem, orderCurrency, promotionCode, shippingCountryCode);

			orderAmountNumeric += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
			totalShippingFee+= GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
			totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));
			totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
			orderItemList.add(orderItem);

		}

		Map orderInfoMap = DropshipOrderService.updateOrderV2(orderId, orderAmountNumeric.toString(), orderCurrency,
				ResourceStates.CREATED, StringPool.BLANK, "", "", totalItems, 0, "", totalTax.toString(), totalShippingFee);

		orderInfoMap.put(AppParams.ITEMS, orderItemList);

		return orderInfoMap;
	}

	private Map createOrderItem(String orderId, Map requestItem, String currency, String promotionCode,
			String shippingCountryCode) throws SQLException {

		String campaignId = ParamUtil.getString(requestItem, AppParams.CAMPAIGN_ID);
		String productId = ParamUtil.getString(requestItem, AppParams.PRODUCT_ID);
		String variantId = ParamUtil.getString(requestItem, AppParams.VARIANT_ID);
		String variantName = ParamUtil.getString(requestItem, AppParams.VARIANT_NAME);
		String sizeId = ParamUtil.getString(requestItem, AppParams.SIZE_ID);
		int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);


		Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);

		String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
		double baseCost = ParamUtil.getDouble(variantMap, AppParams.BASE_COST);
		String baseShortCode = ParamUtil.getString(variantMap, AppParams.BASE_SHORT_CODE);

		String colorId = ParamUtil.getString(variantMap, AppParams.COLOR_ID);
		String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
		String colorValue = ParamUtil.getString(variantMap, AppParams.COLOR);

		Map image = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
		String variantFrontUrl = ParamUtil.getString(image, AppParams.FRONT);
		String variantBackUrl = ParamUtil.getString(image, AppParams.BACK);
		String designFrontUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_FRONT_URL);
		String designBackUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_BACK_URL);

		Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, shippingCountryCode, quantity);
		Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);
		Double taxAmount = ParamUtil.getDouble(feeMap, AppParams.TAX_AMOUNT);
		double productSubTotal = GetterUtil.format(baseCost * quantity, 2);
		double productAmount = GetterUtil.format(baseCost * quantity + shippingFee + taxAmount, 2);

		DropshipOrderProductObj dropshipOrderProduct = new DropshipOrderProductObj.Builder(orderId)
				.orderId(orderId)
				.campaignId(campaignId)
				.productId(productId)
				.variantId(variantId)
				.sizeId(sizeId)
				.price(baseCost)
				.shippingFee(shippingFee)
				.currency(currency)
				.quantity(quantity)
				.state(ResourceStates.APPROVED)
				.variantName(variantName)
				.amount(productAmount)
				.baseCost(baseCost)
				.baseId(baseId)
//				.lineItemId(setLineItemId)
				.variantFrontUrl(variantFrontUrl)
				.variantBackUrl(variantBackUrl)
				.colorId(colorId)
				.colorValue(colorValue)
//				.partnerSku(setPartnerSku)
				.colorName(colorName)
				.sizeName(sizeId)
				.shippingMethod(AppParams.STANDARD)
//				.printDetail(setPrintDetail)
				.itemType(ResourceStates.NORMAL)
//				.partnerProperties(setPartnerProperties)
//				.partnerOption(setPartnerOption)
				.baseShortCode(baseShortCode)
				.designFrontUrl(designFrontUrl)
				.designBackUrl(designBackUrl)
				.taxAmount(taxAmount)
				.build();
		Map orderItem = DropshipOrderProductService.insertDropshipOrderProduct(dropshipOrderProduct);
		orderItem.put(AppParams.SUBTOTAL, productSubTotal);

		return orderItem;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderCreateHandler.class.getName());
}
