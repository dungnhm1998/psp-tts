package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import asia.leadsgen.psp.obj.DropshipOrderProductTypeObj;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.external.api.SSPApiConnector;
import asia.leadsgen.psp.obj.Address;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service.ProductService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.OrderUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderUpdateV2Handler extends PSPOrderHandler implements Handler<RoutingContext> {

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
				String source =  ParamUtil.getString(requestOrderInfoMap, AppParams.SOURCE);
				String shippingMethod = ParamUtil.getString(requestOrderInfoMap, AppParams.SHIPPING_METHOD);
				LOGGER.info("Source= " + source);
				
				Map dbOrderInfoMap = DropshipOrderService.lookUpV2(orderId, true, false, false);
				String userIdOfdbOrderInfoMap = ParamUtil.getString(dbOrderInfoMap, AppParams.USER_ID);
				String dbStoreId = ParamUtil.getString(dbOrderInfoMap, AppParams.STORE_ID);
				String dbReferenceOrderId = ParamUtil.getString(dbOrderInfoMap, AppParams.REFERENCE_ID);
				
				if (!userId.equals(userIdOfdbOrderInfoMap)) {
					throw new BadRequestException(SystemError.OPERATION_NOT_PERMITTED);
				}
				
				if (!dbReferenceOrderId.equalsIgnoreCase(referenceOrderId)) {
					if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId,
							referenceOrderId, source)) {
						throw new BadRequestException(SystemError.DUPLICATE_REFERENCE_ORDER);
					}
				} else {
					if (!dbStoreId.equalsIgnoreCase(storeId)) {					
						if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId,
								referenceOrderId, source)) {
							throw new BadRequestException(SystemError.DUPLICATE_REFERENCE_ORDER);
						}
					}
				}

				String iossNumber = ParamUtil.getString(requestOrderInfoMap, AppParams.IOSS_NUMBER);
				Boolean isValidIossNumber = OrderUtil.checkValidIossNumber(iossNumber);

				if (!isValidIossNumber) {
					throw new BadRequestException(SystemError.INVALID_IOSS_NUMBER);
				}
							
				String dbOrderState = ParamUtil.getString(dbOrderInfoMap, AppParams.STATE);
				LOGGER.info("OrderId= " + orderId + " - Order State= " + dbOrderState);
				if (!ResourceStates.QUEUED.equalsIgnoreCase(dbOrderState)
						&& !ResourceStates.DRAFT.equalsIgnoreCase(dbOrderState)
						&& !ResourceStates.PLACED.equalsIgnoreCase(dbOrderState)) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
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
//						storeId = ParamUtil.getString(dbOrderInfoMap, AppParams.STORE_ID);
//						referenceOrderId = ParamUtil.getString(dbOrderInfoMap, AppParams.REFERENCE_ID);
						addrVerifiedNote = ParamUtil.getString(ParamUtil.getMapData
								(ParamUtil.getMapData(dbOrderInfoMap, AppParams.SHIPPING), AppParams.ADDRESS),
								AppParams.ADDR_VERIFIED_NOTE);
						addrVerified = ParamUtil.getBoolean(ParamUtil.getMapData
								(ParamUtil.getMapData(dbOrderInfoMap, AppParams.SHIPPING), AppParams.ADDRESS),
								AppParams.ADDR_VERIFIED);
						isAddrVerified = (addrVerified == false) ? 0 : 1;
					}					

					String orderCurrency = ParamUtil.getString(requestOrderInfoMap, AppParams.CURRENCY);

					List<Map> requestOrderItemList = ParamUtil.getListData(requestOrderInfoMap, AppParams.ITEMS);
					Double orderAmount = 0.00;
					int totalItems = 0;
					Double totalShippingFee = 0.00;
					Double totalTax = 0d; 
					LOGGER.info("requestOrderItemList= " + requestOrderItemList.toString());

					if (requestOrderItemList.size() > 0) {
						checkInvalidOrder(requestOrderItemList);
//						DropshipOrderProductService.deleteByOrder(orderId);
						checkDeletedItem(requestOrderItemList, dbItemsList);

						Set<String> setBaseId = OrderUtil.getSetBaseFromItemCamp(requestOrderItemList);

						Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, shippingCountryCode, shippingMethod);

						Map orderItem = null;

						Map countryTax = CountryTaxService.getTaxByCountry(shippingCountryCode);
						
						for (Map requestItem : requestOrderItemList) {
							String itemId = ParamUtil.getString(requestItem, AppParams.ID);
							if (StringUtils.isEmpty(itemId)) {
								LOGGER.info("itemId is Empty... ");
								orderItem = createOrderItem(orderId, requestItem, orderCurrency, shippingMethod, shippingInfo, countryTax, iossNumber);
							} else {
								LOGGER.info("itemId= " + itemId);
								orderItem = updateOrderItem(requestItem, shippingMethod, shippingInfo, countryTax, iossNumber);
							}
							LOGGER.info("orderItem: " + orderItem.toString());
							orderAmount += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));;
							totalShippingFee+= GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
							totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
							totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));
						}
					}
					
					orderAmount = GetterUtil.format(orderAmount, 2);
					
					totalTax = GetterUtil.format(totalTax, 2);
					
					DropshipOrderProductService.updateShippingMethod(orderId, shippingMethod);
					NumberFormat amountFormatter = new DecimalFormat("#0.00");

					dbOrderInfoMap = DropshipOrderService.updateOrderV2(orderId, amountFormatter.format(orderAmount),
							orderCurrency, "", shippingId, storeId, referenceOrderId, totalItems, isAddrVerified, addrVerifiedNote, totalTax.toString(), totalShippingFee, iossNumber);


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
	
	private void checkInvalidOrder(List<Map> requestOrderItemList) throws SQLException {
		for (Map requestItem : requestOrderItemList) {
			int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
			if (quantity <= 0) {
				throw new BadRequestException(SystemError.INVALID_ORDER);
			}
		}
	}
	
	private void checkDeletedItem(List<Map> requestOrderItemList,  List<Map> dbOrderList) throws SQLException {
		List<String> rqItemIdList = new ArrayList<String>();	
		for (Map requestItem : requestOrderItemList) {
			String itemId = ParamUtil.getString(requestItem, AppParams.ID);
			rqItemIdList.add(itemId);
		}
		LOGGER.info("rqItemIdList: " + rqItemIdList.toString());
		List<String> dbItemIdList = new ArrayList<String>();
		for (Map dbItem : dbOrderList ) {
			String dbItemId = ParamUtil.getString(dbItem, AppParams.ID);
			dbItemIdList.add(dbItemId);
		}
		LOGGER.info("dbItemIdList: " + dbItemIdList.toString());
		for (String dbItemId : dbItemIdList) {
			if (!rqItemIdList.contains(dbItemId)) {			
				DropshipOrderProductService.deleteOrderItem(dbItemId);
				LOGGER.info("itemId: " + dbItemId + "is deleted!");
			}
		}
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
		String countryCode = ParamUtil.getString(address, AppParams.COUNTRY);
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

	private Map createOrderItem(String orderId, Map requestItem, String currency, String shippingMethod, Map shippingInfo, Map countryTax, String iossNumber)
			throws SQLException {
		
		LOGGER.info("requestItem= " + requestItem.toString());
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

		double productSubTotal = 0.00;

		Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseId, shippingMethod, quantity, shippingInfo);
		Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);

		double productAmount = GetterUtil.format(baseCost * quantity + shippingFee, 2);
		LOGGER.info("+++productAmount = " + productAmount);
		Double taxRate =0d;
		Double taxAmount =0d;
		if (StringUtils.isEmpty(iossNumber)) {
			taxRate=OrderUtil.getTaxRateFromCountryTax(countryTax);
			taxAmount = OrderUtil.getTaxByAmountAndByCountry(productAmount, countryTax);
		}
		productAmount = GetterUtil.format(productAmount + taxAmount, 2);
		LOGGER.info("+++taxAmount = " + taxAmount + ", taxRate = " + taxRate);

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
//				.partnerSku(setPartnerSku)
				.colorName(colorName)
				.sizeName(sizeName)
				.shippingMethod(shippingMethod)
				.printDetail(customData)
				.itemType(typeItem)
//				.partnerProperties(setPartnerProperties)
//				.partnerOption(setPartnerOption)
				.unitAmount(unitAmount)
				.designFrontUrl(designFrontUrl)
				.designBackUrl(designBackUrl)
				.taxAmount(String.valueOf(taxAmount))
				.taxRate(String.valueOf(taxRate))
				.build();

		LOGGER.info("orderProductObj: " + orderProductObj.toString());
		Map orderItem = DropshipOrderProductService.insertDropshipOrderProductV2(orderProductObj);

//		Map orderItem = DropshipOrderProductService.insertV2(orderProductObj);

		return orderItem;
	}

	private Map updateOrderItem(Map requestItem, String shippingMethod, Map shippingInfo, Map countryTax, String iossNumber) throws SQLException {
		
		LOGGER.info("updateOrderItem... ");
		LOGGER.info("requestItem= " + requestItem.toString());
		
		DropshipOrderProductObj orderProductObj = new DropshipOrderProductObj();
		orderProductObj.setId(ParamUtil.getString(requestItem, AppParams.ID));
		orderProductObj.setSizeId(ParamUtil.getString(requestItem, AppParams.SIZE_ID));
		int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
		orderProductObj.setQuantity(quantity);
		
		String productId = ParamUtil.getString(requestItem, AppParams.PRODUCT_ID);

		LOGGER.info("productId= " + productId);
		Map productInfoMap = ProductService.getBaseInfoAndPrice(productId, orderProductObj.getSizeId());
		String baseId = ParamUtil.getString(productInfoMap, AppParams.BASE_ID);
		double baseCost = ParamUtil.getDouble(productInfoMap, AppParams.DROPSHIP_BASE_COST);
		
		String unitAmount = ParamUtil.getString(requestItem, AppParams.UNIT_AMOUNT);

		Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseId, shippingMethod, quantity, shippingInfo);
		Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);

		double productAmount = GetterUtil.format(baseCost * quantity + shippingFee, 2);
		LOGGER.info("+++productAmount = " + productAmount);
		Double taxRate =0d;
		Double taxAmount =0d;
		if (StringUtils.isEmpty(iossNumber)) {
			taxRate=OrderUtil.getTaxRateFromCountryTax(countryTax);
			taxAmount = OrderUtil.getTaxByAmountAndByCountry(productAmount, countryTax);
		}
		productAmount = GetterUtil.format(productAmount + taxAmount, 2);
		LOGGER.info("+++taxAmount = " + taxAmount + ", taxRate = " + taxRate);
		
		orderProductObj.setPrice(baseCost);
		orderProductObj.setBaseCost(baseCost);
		orderProductObj.setShippingFee(shippingFee);
		orderProductObj.setAmount(productAmount);
		orderProductObj.setState(ResourceStates.APPROVED);
		orderProductObj.setUnitAmount(unitAmount);
		orderProductObj.setTaxRate(taxRate);
		orderProductObj.setTaxAmount(taxAmount);
		orderProductObj.setShippingMethod(shippingMethod);

		Map itemInfo = DropshipOrderProductService.update(orderProductObj);

		return itemInfo;
	}
	
	private static final Logger LOGGER = Logger.getLogger(DropshipOrderUpdateV2Handler.class.getName());
}