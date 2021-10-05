package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.external.api.SSPApiConnector;
import asia.leadsgen.psp.obj.Address;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.BaseSKUService;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.Common;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.OrderUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderUpdateCustomHandler extends PSPOrderHandler implements Handler<RoutingContext> {
	
	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		routingContext.vertx().executeBlocking(future -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			LOGGER.info("userId= " + userId);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
			Map requestOrderInfoMap = routingContext.getBodyAsJson().getMap();
			LOGGER.info("requestOrderInfoMap= " + requestOrderInfoMap.toString());

			String storeId = ParamUtil.getString(requestOrderInfoMap, AppParams.STORE_ID);
			LOGGER.info("storeId= " + storeId);
			if (StringUtils.isEmpty(storeId)) {
				throw new LoginException(SystemError.INVALID_DROPSHIP_STORE_ID);
			}
			
			Map storeResult = null;
			try {
				storeResult = DropShipStoreService.getStoreApprovedAndDisconnectedById(storeId);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			String storeUserId = ParamUtil.getString(storeResult, AppParams.USER_ID);
			LOGGER.info("storeUserId= " + storeUserId);
			if (!storeUserId.equalsIgnoreCase(userId)) {
				throw new LoginException(SystemError.INVALID_USER);
			}
			
			try {
				
				initItemGroupQuantity();

				String orderId = routingContext.request().getParam(AppParams.ID);
		
				String referenceOrderId = ParamUtil.getString(requestOrderInfoMap, AppParams.REFERENCE_ID);
				String source = ParamUtil.getString(requestOrderInfoMap, AppParams.SOURCE);
				LOGGER.info("Source= " + source);
				if (StringUtils.isEmpty(source)) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
				}
				String shippingMethod = ParamUtil.getString(requestOrderInfoMap, AppParams.SHIPPING_METHOD);
				
				boolean errorAddress200 = ParamUtil.getBoolean(requestOrderInfoMap, "error_address_200");

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
					
					if (errorAddress200) {
						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
						routingContext.put(AppParams.RESPONSE_DATA, addressError);
					} else {
						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
						routingContext.put(AppParams.RESPONSE_DATA, addressError);
					}				
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
					LOGGER.info("requestOrderItemList= " + requestOrderItemList.toString());
								
					Double totalTax = 0d; 
					if (requestOrderItemList.size() > 0) {
						checkInvalidOrder(requestOrderItemList, dbItemsList);
//						DropshipOrderProductService.deleteByOrder(orderId);
						checkDeletedItem(requestOrderItemList, dbItemsList, source);
						Map orderItem = null;

						Set<String> setBaseId = OrderUtil.getSetBaseIdFromItemCustom(requestOrderItemList);

						Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, shippingCountryCode, shippingMethod);

						Map countryTax = CountryTaxService.getTaxByCountry(shippingCountryCode);

						for (Map requestItem : requestOrderItemList) {
							String itemId = ParamUtil.getString(requestItem, AppParams.ID);

							if (StringUtils.isEmpty(itemId)) {
								LOGGER.info("itemId is Empty... ");
								orderItem = createOrderItem(orderId, requestItem, orderCurrency, shippingCountryCode, userId, source, shippingMethod, referenceOrderId, shippingInfo, countryTax, iossNumber);
							} else {
								LOGGER.info("itemId= " + itemId);
								orderItem = updateOrderItem(orderId, shippingCountryCode, requestItem, userId, source, shippingMethod, shippingInfo, countryTax, iossNumber);
							}
							LOGGER.info("orderItem: " + orderItem.toString());

							orderAmount += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));;
							totalShippingFee+= GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
							totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
							totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));
						}
					}
					
					DropshipOrderProductService.updateShippingMethod(orderId, shippingMethod);
					NumberFormat amountFormatter = new DecimalFormat("#0.00");
					totalTax = GetterUtil.format(totalTax, 2);

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
	
	private void checkInvalidOrder(List<Map> requestOrderItemList, List<Map> dbItemsList) throws SQLException {
		LOGGER.info("checkInvalidOrder...");

		for (Map requestItem : requestOrderItemList) {
			String baseId = ParamUtil.getString(requestItem, AppParams.BASE_ID);
			if (baseId == null || baseId.isEmpty()) {
				throw new BadRequestException(SystemError.INVALID_ORDER);
			}
			String sizeId = ParamUtil.getString(requestItem, AppParams.SIZE_ID);
			if (sizeId == null || sizeId.isEmpty()) {
				throw new BadRequestException(SystemError.INVALID_ORDER);
			}
			String colorId = ParamUtil.getString(requestItem, AppParams.COLOR_ID);
			if (colorId == null || colorId.isEmpty()) {
				throw new BadRequestException(SystemError.INVALID_ORDER);
			}
			String colorName = ParamUtil.getString(requestItem, AppParams.COLOR_NAME);
			if (colorName == null || colorName.isEmpty()) {
				throw new BadRequestException(SystemError.INVALID_ORDER);
			}
			int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
			if (quantity <= 0) {
				throw new BadRequestException(SystemError.INVALID_ORDER);
			}
			String campaignId = ParamUtil.getString(requestItem, AppParams.CAMPAIGN_ID);
			LOGGER.info("campaignId: " + campaignId);

			if (campaignId == null || campaignId.isEmpty()) {
				Map designMap = ParamUtil.getMapData(requestItem, AppParams.DESIGNS);
				String design_front_url_md5 = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL_MD5);
				String design_back_url_md5 = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL_MD5);
				if ((design_front_url_md5 == null || design_front_url_md5.isEmpty())
						&& (design_back_url_md5 == null || design_back_url_md5.isEmpty())) {
					throw new BadRequestException(SystemError.INVALID_DESIGN);
				}
			} else if (campaignId != null) {
				if (campaignId.length() > 32) {
					Map designMap = ParamUtil.getMapData(requestItem, AppParams.DESIGNS);
					String request_design_front_url = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL);
					String request_design_back_url = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL);
					String dropshipOrderProductId = ParamUtil.getString(requestItem, AppParams.ID);
					Map db_dropshipOrderProduct = DropshipOrderProductService.getById(dropshipOrderProductId);
					Map db_designs_Map = ParamUtil.getMapData(db_dropshipOrderProduct, AppParams.DESIGNS);

					String db_design_front_url = db_designs_Map.get(AppParams.DESIGN_FRONT_URL) != null
							? db_designs_Map.get(AppParams.DESIGN_FRONT_URL).toString()
							: "";
					String db_design_back_url = db_designs_Map.get(AppParams.DESIGN_BACK_URL) != null
							? db_designs_Map.get(AppParams.DESIGN_BACK_URL).toString()
							: "";

					if ((StringUtils.isNotEmpty(request_design_front_url) && !request_design_front_url.equals(db_design_front_url))
							|| (StringUtils.isNotEmpty(request_design_back_url) && !request_design_back_url.equals(db_design_back_url))) {
						LOGGER.info("design_front_url != db_design_front_url && design_back_url != db_design_back_url");
						String design_front_url_md5 = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL_MD5);
						String design_back_url_md5 = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL_MD5);

						if ((design_front_url_md5 == null || design_front_url_md5.isEmpty())
								&& (design_back_url_md5 == null || design_back_url_md5.isEmpty())) {
							throw new BadRequestException(SystemError.INVALID_DESIGN);
						}
					} else {
						LOGGER.info(
								"request_design_back_url == db_design_back_url && request_design_front_url == db_design_front_url");
					}
				}
			}
		}
	}

	private Map getDbItem(String lineItemId, List<Map> dbOrderList) {
		for (Map dbItem : dbOrderList) {
			if (lineItemId.equals(ParamUtil.getString(dbItem, AppParams.ID))) {
				return dbItem;
			}
		}
		return null;
	}
	
	private void checkDeletedItem(List<Map> requestOrderItemList,  List<Map> dbOrderList, String source) throws SQLException {
		
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
//		if (source.equalsIgnoreCase(ResourceSource.CUSTOM_SHOPIFY_APP)) {
//			int countdbItemSpf = 0;
//			for (String dbItemId : dbItemIdList) {
//				if (!rqItemIdList.contains(dbItemId)) {
//					countdbItemSpf++;
//				}
//			}
//			if (countdbItemSpf == rqItemIdList.size()) {
//				throw new BadRequestException(new SystemError("Invalid Request Item", "This Order had Synchronized, please wait a moment!", "", "http://developer.30usd.com/errors/401.html"));
//			}
//		}
		
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
	
	/**
	 * 
	 * @param orderId
	 * @param requestItem
	 * @param orderCurrency
	 * @param shippingCountryCode
	 * @return
	 * @throws SQLException
	 */
	private Map createOrderItem(String orderId, Map requestItem, String orderCurrency, String shippingCountryCode,
								String userId, String source, String shippingMethod, String referenceOrderId, Map shippingInfo, Map countryTax, String iossNumber)
			throws SQLException {
		
		LOGGER.info("requestItem: " + requestItem.toString());
		String baseId = ParamUtil.getString(requestItem, AppParams.BASE_ID);
		String variantName = ParamUtil.getString(requestItem, AppParams.VARIANT_NAME);
		String sizeId = ParamUtil.getString(requestItem, AppParams.SIZE_ID);
		String sizeName = ParamUtil.getString(requestItem, AppParams.SIZE_NAME);
		int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
		String colorId = ParamUtil.getString(requestItem, AppParams.COLOR_ID);
		String color = ParamUtil.getString(requestItem, AppParams.COLOR);
		String colorName = ParamUtil.getString(requestItem, AppParams.COLOR_NAME);
		
		String colorValueDB = "";
        String colorNameDB = "";
        String sizeNameDB = "";
        
//        boolean checkBaseMap = allBase.stream().filter(m -> (m.get(AppParams.BASE_ID)).equals(baseId)).findFirst().isPresent();
//        LOGGER.info("checkBaseMap: " + checkBaseMap);
//        if (checkBaseMap) {
//        	Map baseMap = allBase.stream().filter(m -> (m.get(AppParams.BASE_ID)).equals(baseId)).findFirst().get();
//        	
//        	List<Map> colorList = ParamUtil.getListData(baseMap, AppParams.COLORS);
//        	boolean checkColorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().isPresent();
//        	if (checkColorMap) {
//        		Map colorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().get();
//        		colorNameDB = ParamUtil.getString(colorMap, AppParams.NAME);
//        		colorValueDB = ParamUtil.getString(colorMap, AppParams.VALUE);
//        	}
//        	
//        	List<Map> sizeList = ParamUtil.getListData(baseMap, AppParams.SIZES);
//        	boolean checkSizeMap = sizeList.stream().filter(m -> (m.get(AppParams.ID)).equals(sizeId)).findFirst().isPresent();
//        	if (checkSizeMap) {
//        		Map sizeMap = sizeList.stream().filter(m -> (m.get(AppParams.ID)).equals(sizeId)).findFirst().get();
//        		sizeNameDB = ParamUtil.getString(sizeMap, AppParams.NAME);
//        	}
//        }
        
        Map baseMap = BaseService.get(baseId);
        if (baseMap == null || baseMap.isEmpty()) {
        	throw new BadRequestException(SystemError.INVALID_BASE_ID);
        }
        
        List<Map> colorList = ParamUtil.getListData(baseMap, AppParams.COLORS);
        boolean checkColorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().isPresent();
    	if (checkColorMap) {
    		Map colorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().get();
    		colorNameDB = ParamUtil.getString(colorMap, AppParams.NAME);
    		colorValueDB = ParamUtil.getString(colorMap, AppParams.VALUE);
    	}
        
    	Map<String, String> baseSizeMap = BaseService.getBaseSizeMap();
    	sizeNameDB = baseSizeMap.get(sizeId);
    	
        LOGGER.info("get colorNameDB: " + colorNameDB);
        LOGGER.info("get colorValueDB: " + colorValueDB);
        LOGGER.info("get sizeNameDB: " + sizeNameDB);
        
        if (!colorNameDB.equalsIgnoreCase(colorName) || !sizeNameDB.equalsIgnoreCase(sizeName)) {
        	LOGGER.info("INVALID COLOR_ID OR SIZE_ID");
        	throw new BadRequestException(SystemError.INVALID_COLOR_OR_SIZE);
        }
		
		String productId = ParamUtil.getString(requestItem, AppParams.PRODUCT_ID);
		String variantId =  ParamUtil.getString(requestItem, AppParams.VARIANT_ID);
		LOGGER.info("productId: " + productId + " - variantId: " + variantId);
		if (!variantName.contains(colorName)){
			variantName = variantName + " - " + colorName;
			LOGGER.info("-createOrderItem()- variantName= " + variantName);
		}

		List<Map> skuListMap = BaseSKUService.getSkuByBaseIdSizeIdColorName(baseId, sizeId, "");
		String baseName = ParamUtil.getString(skuListMap.get(0), AppParams.S_BASE_NAME);
		LOGGER.info("createOrderItem()- baseName: " + baseName + " - colorName: " + colorName);
		variantName = baseName + " - " + colorName;
		
		Map designMap = ParamUtil.getMapData(requestItem, AppParams.DESIGNS);
		String designFrontUrl = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL);
		String designBackUrl = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL);
		String mockupFrontUrl = ParamUtil.getString(designMap, AppParams.MOCKUP_FRONT_URL);
		String mockupBackUrl = ParamUtil.getString(designMap, AppParams.MOCKUP_BACK_URL);
		
		String campaignId = ParamUtil.getString(requestItem, AppParams.CAMPAIGN_ID);
		
		String unitAmount = ParamUtil.getString(requestItem, AppParams.UNIT_AMOUNT);

		String designFrontUrlMd5 = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL_MD5);
		String designBackUrlMd5 = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL_MD5);
		if (StringUtils.isEmpty(campaignId)) {
			campaignId = userId + "-";
			String uuid = Common.getUUID();
			if (StringUtils.isNotEmpty(designFrontUrlMd5) || StringUtils.isNotEmpty(designBackUrlMd5)) {
				campaignId = campaignId + uuid;
			} else {
				LOGGER.info("INVALID_DESIGN");
				throw new BadRequestException(SystemError.INVALID_DESIGN);
			}
		}
		
		int isTwoDesigns = 0;
		if ((designFrontUrl != null && designFrontUrl.isEmpty() == false)
				&& (designBackUrl != null && designBackUrl.isEmpty() == false)) {
			isTwoDesigns = 1;
		}
		
		double baseCost = BaseService.getDropshipBaseCost(baseId, sizeId, isTwoDesigns);
		LOGGER.info("baseCost: " + baseCost);
		
		double productSubTotal = 0.00;

		Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseId, shippingMethod, quantity, shippingInfo);
		Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);

		double productAmount = GetterUtil.format(baseCost * quantity + shippingFee, 2);
		LOGGER.info("+++productAmount = " + productAmount);
//		Double taxAmount = OrderUtil.getTaxByAmountAndByCountry(productAmount, countryTax, iossNumber);
		Double taxRate=0.0;
		Double taxAmount =0d;
		if (StringUtils.isEmpty(iossNumber)) {
			taxRate=OrderUtil.getTaxRateFromCountryTax(countryTax);
			taxAmount = OrderUtil.getTaxByAmountAndByCountry(productAmount, countryTax);
		}

		productAmount = GetterUtil.format(productAmount + taxAmount, 2);
		LOGGER.info("+++taxAmount = " + taxAmount + ", taxRate = " + taxRate);

		DropshipOrderProductTypeObj dropshipOrderProduct = DropshipOrderProductTypeObj.builder()
				.orderId(orderId)
				.campaignId(campaignId)
				.productId(productId)
				.variantId(variantId)
				.sizeId(sizeId)
				.price(String.valueOf(baseCost))
				.shippingFee(String.valueOf(shippingFee))
				.currency("USD")
				.quantity(quantity)
				.state(ResourceStates.APPROVED)
				.variantName(variantName)
				.amount(String.valueOf(productAmount))
				.baseCost(String.valueOf(baseCost))
				.baseId(baseId)
				.lineItemId(referenceOrderId)
				.variantFrontUrl(mockupFrontUrl)
				.variantBackUrl(mockupBackUrl)
				.colorId(colorId)
				.colorValue(color)
				.colorName(colorName)
				.sizeName(sizeName)
				.shippingMethod(shippingMethod)
				.itemType(ResourceStates.NORMAL)
				.designFrontUrl(designFrontUrl)
				.designBackUrl(designBackUrl)
				.unitAmount(unitAmount)
				.taxAmount(String.valueOf(taxAmount))
				.taxRate(String.valueOf(taxRate))
				.build();

		LOGGER.info("orderProductObj: " + dropshipOrderProduct.toString());
		Map orderItem = DropshipOrderProductService.insertDropshipOrderProductV2(dropshipOrderProduct);
		
//		if (variantId != null && variantId.isEmpty() == false) {
//			ShopifyAppService.orderProductUpdateThumbUrl(variantId, orderId);
//		}
		
		return orderItem;
	}

	private Map updateOrderItem(String orderId, String shippingCountryCode, Map requestItem, String userId, String source,
								String shippingMethod, Map shippingInfo, Map countryTax, String iossNumber) throws SQLException {
		
		LOGGER.info("updateOrderItem... ");
		LOGGER.info("requestItem= " + requestItem.toString());
		
		String orderProductId = ParamUtil.getString(requestItem, AppParams.ID);
		String baseId = ParamUtil.getString(requestItem, AppParams.BASE_ID);
		String variantName = ParamUtil.getString(requestItem, AppParams.VARIANT_NAME);
		String sizeId = ParamUtil.getString(requestItem, AppParams.SIZE_ID);
		String sizeName = ParamUtil.getString(requestItem, AppParams.SIZE_NAME);
		int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
		String colorId = ParamUtil.getString(requestItem, AppParams.COLOR_ID);
		String color = ParamUtil.getString(requestItem, AppParams.COLOR);
		String colorName = ParamUtil.getString(requestItem, AppParams.COLOR_NAME);
		
		String colorValueDB = "";
        String colorNameDB = "";
        String sizeNameDB = "";
        
//        boolean checkBaseMap = allBase.stream().filter(m -> (m.get(AppParams.BASE_ID)).equals(baseId)).findFirst().isPresent();
//        LOGGER.info("checkBaseMap: " + checkBaseMap);
//        if (checkBaseMap) {
//        	Map baseMap = allBase.stream().filter(m -> (m.get(AppParams.BASE_ID)).equals(baseId)).findFirst().get();
//        	
//        	List<Map> colorList = ParamUtil.getListData(baseMap, AppParams.COLORS);
//        	boolean checkColorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().isPresent();
//        	if (checkColorMap) {
//        		Map colorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().get();
//        		colorNameDB = ParamUtil.getString(colorMap, AppParams.NAME);
//        		colorValueDB = ParamUtil.getString(colorMap, AppParams.VALUE);
//        	}
//        	
//        	List<Map> sizeList = ParamUtil.getListData(baseMap, AppParams.SIZES);
//        	boolean checkSizeMap = sizeList.stream().filter(m -> (m.get(AppParams.ID)).equals(sizeId)).findFirst().isPresent();
//        	if (checkSizeMap) {
//        		Map sizeMap = sizeList.stream().filter(m -> (m.get(AppParams.ID)).equals(sizeId)).findFirst().get();
//        		sizeNameDB = ParamUtil.getString(sizeMap, AppParams.NAME);
//        	}
//        }
        
        Map baseMap = BaseService.get(baseId);
        if (baseMap == null || baseMap.isEmpty()) {
        	throw new BadRequestException(SystemError.INVALID_BASE_ID);
        }
        
        List<Map> colorList = ParamUtil.getListData(baseMap, AppParams.COLORS);
        boolean checkColorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().isPresent();
    	if (checkColorMap) {
    		Map colorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().get();
    		colorNameDB = ParamUtil.getString(colorMap, AppParams.NAME);
    		colorValueDB = ParamUtil.getString(colorMap, AppParams.VALUE);
    	}
        
    	Map<String, String> baseSizeMap = BaseService.getBaseSizeMap();
    	sizeNameDB = baseSizeMap.get(sizeId);
    	
        LOGGER.info("get colorNameDB: " + colorNameDB);
        LOGGER.info("get colorValueDB: " + colorValueDB);
        LOGGER.info("get sizeNameDB: " + sizeNameDB);
        
        if (!colorNameDB.equalsIgnoreCase(colorName) || !sizeNameDB.equalsIgnoreCase(sizeName)) {
        	LOGGER.info("INVALID COLOR_ID OR SIZE_ID");
        	throw new BadRequestException(SystemError.INVALID_COLOR_OR_SIZE);
        }
		
		String productId = ParamUtil.getString(requestItem, AppParams.PRODUCT_ID);
		String variantId =  ParamUtil.getString(requestItem, AppParams.VARIANT_ID);

		List<Map> skuListMap = BaseSKUService.getSkuByBaseIdSizeIdColorName(baseId, sizeId, "");
		String baseName = ParamUtil.getString(skuListMap.get(0), AppParams.S_BASE_NAME);
		LOGGER.info("updateOrderItem()- baseName: " + baseName + " - colorName: " + colorName);
		variantName = baseName + " - " + colorName;

		String campaignId = ParamUtil.getString(requestItem, AppParams.CAMPAIGN_ID);
		Map designMap = ParamUtil.getMapData(requestItem, AppParams.DESIGNS);
		String designFrontUrl = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL);
		String designBackUrl = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL);
		String mockupFrontUrl = ParamUtil.getString(designMap, AppParams.MOCKUP_FRONT_URL);
		String mockupBackUrl = ParamUtil.getString(designMap, AppParams.MOCKUP_BACK_URL);
		
		String designFrontUrlMd5 = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL_MD5);
		String designBackUrlMd5 = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL_MD5);
		
		String unitAmount = ParamUtil.getString(requestItem, AppParams.UNIT_AMOUNT);

		if (StringUtils.isEmpty(campaignId)) {
			campaignId = userId + "-";
			String uuid =  Common.getUUID();
			if (StringUtils.isNotEmpty(designFrontUrlMd5) || StringUtils.isNotEmpty(designBackUrlMd5)) {
				campaignId = campaignId + uuid;
			} else {
				LOGGER.info("INVALID_DESIGN");
				throw new BadRequestException(SystemError.INVALID_DESIGN);
			}
		}
		
		if (campaignId.length() < 32 && campaignId.equalsIgnoreCase(userId + "-")) {
			String campaignState = CampaignService.getCampaignState(campaignId);
			if (StringUtils.isEmpty(campaignState) || ResourceStates.LOCKED.equalsIgnoreCase(campaignState)) {
				throw new BadRequestException(SystemError.INVALID_CAMPAIGN);
			}
		}
		
		int isTwoDesigns = 0;
		if ((designFrontUrl != null && designFrontUrl.isEmpty() == false)
				&& (designBackUrl != null && designBackUrl.isEmpty() == false)) {
			isTwoDesigns = 1;
		}

		double baseCost = BaseService.getDropshipBaseCost(baseId, sizeId, isTwoDesigns);
		LOGGER.info("baseCost: " + baseCost);

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

		DropshipOrderProductObj orderProduct = new DropshipOrderProductObj();
		orderProduct.setId(orderProductId);
		orderProduct.setSizeId(sizeId);
		orderProduct.setPrice(baseCost);
		orderProduct.setQuantity(quantity);
		orderProduct.setShippingFee(shippingFee);
		orderProduct.setAmount(productAmount);
		orderProduct.setBaseId(baseId);
		orderProduct.setBaseCost(baseCost);
		orderProduct.setDesignFrontUrl(designFrontUrl);
		orderProduct.setVariantFrontUrl(mockupFrontUrl);
		orderProduct.setDesignBackUrl(designBackUrl);
		orderProduct.setVariantBackUrl(mockupBackUrl);
		orderProduct.setVariantName(variantName);
		orderProduct.setCampaignId(campaignId);
		orderProduct.setColorId(colorId);
		orderProduct.setColorValue(color);
		orderProduct.setColorName(colorName);
		orderProduct.setSizeName(sizeName);
		orderProduct.setProductId(productId);
		orderProduct.setVariantId(variantId);
		orderProduct.setUnitAmount(unitAmount);
		orderProduct.setTaxAmount(taxAmount);
		orderProduct.setTaxRate(taxRate);

		LOGGER.info("orderProduct= " + orderProduct.toString());

		Map orderItem = DropshipOrderProductService.updateByPredefinedSku(orderProduct);
		
		return orderItem;
	}
	
	private static final Logger LOGGER = Logger.getLogger(DropshipOrderUpdateCustomHandler.class.getName());

}