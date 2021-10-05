package asia.leadsgen.psp.server.handler.dropship.order;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.external.api.SSPApiConnector;
import asia.leadsgen.psp.obj.Address;
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
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DropshipOrderUpdateHandler extends PSPOrderHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		routingContext.vertx().executeBlocking(future -> {
			try {

				initItemGroupQuantity();

				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

				if (StringUtils.isEmpty(userId)) {
					throw new BadRequestException(SystemError.LOGIN_REQUIRED);
				}

				String orderId = routingContext.request().getParam(AppParams.ID);
				Map requestOrderInfoMap = routingContext.getBodyAsJson().getMap();

				String storeId = ParamUtil.getString(requestOrderInfoMap, AppParams.STORE_ID);
				String referenceOrderId = ParamUtil.getString(requestOrderInfoMap, AppParams.REFERENCE_ID);

				Map dbOrderInfoMap = DropshipOrderService.lookUp(orderId, true, false, false);
				String userIdOfdbOrderInfoMap = ParamUtil.getString(dbOrderInfoMap, AppParams.USER_ID);

				if (!userId.equals(userIdOfdbOrderInfoMap)) {
					throw new BadRequestException(SystemError.OPERATION_NOT_PERMITTED);
				}

				List<Map> dbItemsList = ParamUtil.getListData(dbOrderInfoMap, AppParams.ITEMS);

				String shippingId = ParamUtil.getString(ParamUtil.getMapData(dbOrderInfoMap, AppParams.SHIPPING),
						AppParams.ID);

				Map rqShipping = ParamUtil.getMapData(requestOrderInfoMap, AppParams.SHIPPING);
				Map shippingInfoMap = null;
				String shippingCountryCode;

				Map addVerifyMap = new LinkedHashMap<>();
				List<Map> addVerifyList = new ArrayList<>();
				boolean isError = false;
				
				if (rqShipping != null && !rqShipping.isEmpty()) {
					Map addVerifyResult = updateOrderShipping(shippingId, 
							ParamUtil.getMapData(requestOrderInfoMap, AppParams.SHIPPING));
					if (ParamUtil.getBoolean(addVerifyResult, "success") == true && addVerifyResult.containsKey("verifiedAdd")
							&& MapUtils.isNotEmpty(ParamUtil.getMapData(addVerifyResult, "verifiedAdd"))) {
						shippingInfoMap = ParamUtil.getMapData(addVerifyResult, "verifiedAdd");
					} else {
						if (addVerifyResult.containsKey("reason") == false) {
							shippingInfoMap = addVerifyResult;
						} else {
							isError = true;
							addVerifyMap.put(AppParams.ID, orderId);
							addVerifyMap.putAll(addVerifyResult);
							addVerifyList.add(addVerifyMap);
						}					
					}
				} else {
					shippingInfoMap = ParamUtil.getMapData(dbOrderInfoMap, AppParams.SHIPPING);
				}

				if (isError) {
					Map addressError = new LinkedHashMap<>();
					addressError.put("error_address", addVerifyList);
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, addressError);
				} else {
					shippingCountryCode = ParamUtil.getString(ParamUtil.getMapData(shippingInfoMap, AppParams.ADDRESS),
							AppParams.COUNTRY);
					
					String addrVerifiedNote;
					Boolean addrVerified;
					int isAddrVerified;
					if (rqShipping != null && !rqShipping.isEmpty()) {
						addrVerifiedNote = ParamUtil.getString(ParamUtil.getMapData(rqShipping, AppParams.ADDRESS),
								AppParams.ADDR_VERIFIED_NOTE);
						addrVerified = ParamUtil.getBoolean(ParamUtil.getMapData(rqShipping, AppParams.ADDRESS),
								AppParams.ADDR_VERIFIED);
						isAddrVerified = (addrVerified == false) ? 0 : 1;
						
						if ("US".equalsIgnoreCase(shippingCountryCode) && addrVerified) {
							addrVerifiedNote = "Seller agree for bypass address verified";
						}
					} else {
						storeId = ParamUtil.getString(dbOrderInfoMap, AppParams.STORE_ID);
						referenceOrderId = ParamUtil.getString(dbOrderInfoMap, AppParams.REFERENCE_ID);
						addrVerifiedNote = ParamUtil.getString(ParamUtil.getMapData
								(ParamUtil.getMapData(dbOrderInfoMap, AppParams.SHIPPING), AppParams.ADDRESS),
								AppParams.ADDR_VERIFIED_NOTE);
						addrVerified = ParamUtil.getBoolean(ParamUtil.getMapData
								(ParamUtil.getMapData(dbOrderInfoMap, AppParams.SHIPPING), AppParams.ADDRESS),
								AppParams.ADDR_VERIFIED);
						isAddrVerified = (addrVerified == false) ? 0 : 1;
					}					

					String orderCurrency = ParamUtil.getString(dbOrderInfoMap, AppParams.CURRENCY);
					double orderAmount = 0.00;
					DropshipOrderProductService.deleteByOrder(orderId);

					List<Map> requestOrderItemList = ParamUtil.getListData(requestOrderInfoMap, AppParams.ITEMS);
					int quantity = 0;
					int totalItems = 0;
					Double totalTax = 0.00;
					Double totalShippingFee = 0.00;
					if (requestOrderItemList.size() > 0) {
						Map orderItem = null;
						for (Map requestItem : requestOrderItemList) {
							String itemId = ParamUtil.getString(requestItem, AppParams.ID);
							quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
							if (StringUtils.isEmpty(itemId)) {
								orderItem = createOrderItem(orderId, requestItem, orderCurrency, shippingCountryCode);
							} else {
								int dbQty;
								Map dbItem = getDbItem(itemId, dbItemsList);
								dbQty = ParamUtil.getInt(dbItem, AppParams.QUANTITY);
								if (dbQty != quantity) {
									orderItem = updateOrderItem(shippingCountryCode, requestItem);
								}
							}
							double itemAmount = GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
							orderAmount += itemAmount;
							totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));
							totalShippingFee+= GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
							totalItems += quantity;
						}
					}

					NumberFormat amountFormatter = new DecimalFormat("#0.00");
					dbOrderInfoMap = DropshipOrderService.updateOrderV2(orderId, amountFormatter.format(orderAmount),
							orderCurrency, "", shippingId, storeId, referenceOrderId, totalItems, isAddrVerified, addrVerifiedNote, totalTax.toString(), totalShippingFee,"");

					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, dbOrderInfoMap);
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

	private Map getDbItem(String lineItemId, List<Map> dbOrderList) {
		for (Map dbItem : dbOrderList) {
			if (lineItemId.equals(ParamUtil.getString(dbItem, AppParams.ID))) {
				return dbItem;
			}
		}
		return null;
	}

	private Map updateOrderShipping(String shippingId, Map shippingInfo) throws SQLException {

		String name = ParamUtil.getString(shippingInfo, AppParams.NAME);
		String email = ParamUtil.getString(shippingInfo, AppParams.EMAIL);
		String phone = ParamUtil.getString(shippingInfo, AppParams.PHONE);

		Map address = ParamUtil.getMapData(shippingInfo, AppParams.ADDRESS);

		String line1 = ParamUtil.getString(address, AppParams.LINE1);
		String line2 = ParamUtil.getString(address, AppParams.LINE2);
		String city = ParamUtil.getString(address, AppParams.CITY);
		String state = ParamUtil.getString(address, AppParams.STATE);
		String postalCode = ParamUtil.getString(address, AppParams.POSTAL_CODE);
		String countryCode = ParamUtil.getString(address, AppParams.COUNTRY_CODE);
		String countryName = ParamUtil.getString(address, AppParams.COUNTRY_NAME);
		Boolean addrVerified = ParamUtil.getBoolean(address, AppParams.ADDR_VERIFIED);

		Map verifyResult = new LinkedHashMap<>();
		if ("US".equalsIgnoreCase(countryCode)) {
			if (!addrVerified) {
				Address addressObj = new Address(name, line1, line2, city, state, postalCode, countryCode, "");
				verifyResult = SSPApiConnector.verifyAddress(addressObj).getMap();
				if (ParamUtil.getBoolean(verifyResult, "success") == true) {
					Map verifiedAdd = ShippingService.updateDropshipOrder(shippingId, name, email, phone, line1, line2,
							city, state, postalCode, countryCode, countryName, false);
					verifyResult.put("verifiedAdd", verifiedAdd);
				}
				return verifyResult;
			}		
		}
		return ShippingService.updateDropshipOrder(shippingId, name, email, phone, line1, line2, city, state,
				postalCode, countryCode, countryName, false);
	}

	/**
	 * 
	 * @param orderId
	 * @param requestItem
	 * @param orderCurrency
	 * @param shippingCountryCode
	 * @return
	 * @throws SQLException
	 */
	private Map createOrderItem(String orderId, Map requestItem, String orderCurrency, String shippingCountryCode)
			throws SQLException {

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

		DropshipOrderProductObj orderProductObj = new DropshipOrderProductObj.Builder(orderId)
				.campaignId(campaignId)
				.productId(productId)
				.variantId(variantId)
				.variantName(variantName)
				.sizeId(sizeId)
				.price(baseCost)
				.currency("USD")
				.quantity(quantity)
				.amount(productAmount)
				.baseCost(baseCost)
				.shippingFee(shippingFee)
				.baseShortCode(baseShortCode)
				.state(ResourceStates.APPROVED)
				.variantFrontUrl(variantFrontUrl)
				.variantBackUrl(variantBackUrl)
				.colorId(colorId)
				.colorName(colorName)
				.colorValue(colorValue)
				.designFrontUrl(designFrontUrl)
				.designBackUrl(designBackUrl)
				.taxAmount(taxAmount)
				.build();

		Map orderItem = DropshipOrderProductService.insertDropshipOrderProduct(orderProductObj);
		orderItem.put(AppParams.SUBTOTAL, productSubTotal);
		return orderItem;
	}

	private Map updateOrderItem(String shippingCountryCode, Map requestItem) throws SQLException {

		DropshipOrderProductObj orderProductObj = new DropshipOrderProductObj();
		orderProductObj.setId(ParamUtil.getString(requestItem, AppParams.ID));
		orderProductObj.setSizeId(ParamUtil.getString(requestItem, AppParams.SIZE_ID));
		int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
		orderProductObj.setQuantity(quantity);

		String productId = ParamUtil.getString(requestItem, AppParams.PRODUCT_ID);

		Map productInfoMap = ProductService.getBaseInfoAndPrice(productId, orderProductObj.getSizeId());
		String baseId = ParamUtil.getString(productInfoMap, AppParams.BASE_ID);
		double baseCost = ParamUtil.getDouble(productInfoMap, AppParams.DROPSHIP_BASE_COST);

		Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, shippingCountryCode, quantity);
		Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);
		Double taxAmount = ParamUtil.getDouble(feeMap, AppParams.TAX_AMOUNT);
		double productSubTotal = GetterUtil.format(baseCost * quantity, 2);
		Double productAmount = GetterUtil.format(baseCost * quantity + shippingFee + taxAmount, 2);

		orderProductObj.setShippingFee(shippingFee);
		orderProductObj.setAmount(productAmount);
		orderProductObj.setState(ResourceStates.APPROVED);
		orderProductObj.setTaxAmount(taxAmount);

		Map itemInfo = DropshipOrderProductService.update(orderProductObj);

		return itemInfo;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderUpdateHandler.class.getName());
}
