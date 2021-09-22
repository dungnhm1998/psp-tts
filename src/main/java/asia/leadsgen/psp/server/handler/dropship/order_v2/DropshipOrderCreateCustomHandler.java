package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.Common;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.OrderUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderCreateCustomHandler extends PSPOrderHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {

        if (routingContext.getBodyAsString()==null || routingContext.getBodyAsString().isEmpty()) {
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

                String referenceOrderId = ParamUtil.getString(requestOrderInfoMap, AppParams.REFERENCE_ID);
                String orderCurrency = ParamUtil.getString(requestOrderInfoMap, AppParams.CURRENCY);
                String note = ParamUtil.getString(requestOrderInfoMap, AppParams.NOTE);
                String source = ParamUtil.getString(requestOrderInfoMap, AppParams.SOURCE);
                LOGGER.info("Source= " + source);
                if (StringUtils.isEmpty(source)) {
                    throw new BadRequestException(SystemError.INVALID_ORDER);
                }

                if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId,
                        referenceOrderId, source)) {
                    throw new BadRequestException(SystemError.DUPLICATE_REFERENCE_ORDER);
                }

                Map shippingInfoMap = ParamUtil.getMapData(requestOrderInfoMap, AppParams.SHIPPING);
                String name = ParamUtil.getString(shippingInfoMap, AppParams.NAME);
                String email = ParamUtil.getString(shippingInfoMap, AppParams.EMAIL);
                String phone = ParamUtil.getString(shippingInfoMap, AppParams.PHONE);
                String shippingMethod = ParamUtil.getString(requestOrderInfoMap, AppParams.SHIPPING_METHOD);

                Map address = ParamUtil.getMapData(shippingInfoMap, AppParams.ADDRESS);
                String line1 = ParamUtil.getString(address, AppParams.LINE1);
                String line2 = ParamUtil.getString(address, AppParams.LINE2);
                String city = ParamUtil.getString(address, AppParams.CITY);
                String state = ParamUtil.getString(address, AppParams.STATE);
                String postalCode = ParamUtil.getString(address, AppParams.POSTAL_CODE);
                String countryCode = ParamUtil.getString(address, AppParams.COUNTRY, "US");
                String countryName = ParamUtil.getString(address, AppParams.COUNTRY_NAME);
                boolean addrVerified = ParamUtil.getBoolean(address, AppParams.ADDR_VERIFIED);
                int isAddrVerified = (addrVerified==false) ? 0:1;
                LOGGER.info("isAddrVerified: " + isAddrVerified);

                Map shippingMap = ShippingService.insert(name, email, phone, line1, line2, city, state, postalCode,
                        countryCode, countryName);

                String shippingId = ParamUtil.getString(shippingMap, AppParams.ID);


                String trackingNumber = AppUtil.generateOrderTrackingNumber();
                List<Map> requestOrderItemList = ParamUtil.getListData(requestOrderInfoMap, AppParams.ITEMS);
                String orderIdPrefix = userId;
                if (source.equalsIgnoreCase(ResourceSource.CUSTOM_SHOPIFY_APP)) {
                    orderIdPrefix = orderIdPrefix + "-SPF-APP";
                } else {
                    orderIdPrefix = orderIdPrefix + "-CT";
                }

                int quantity = 0;
                int totalItems = 0;
                for (Map requestOrderItem : requestOrderItemList) {
                    quantity = ParamUtil.getInt(requestOrderItem, AppParams.QUANTITY);
                    if (quantity <= 0) {
                        throw new BadRequestException(SystemError.INVALID_ORDER);
                    }
                    totalItems += quantity;

                    String campaignId = ParamUtil.getString(requestOrderItem, AppParams.CAMPAIGN_ID);
                    LOGGER.info("campaignId: " + campaignId);
                    if (campaignId!=null && campaignId.isEmpty()==false) {
                        if (campaignId.length() < 32 && campaignId.length() > 0) {
                            String campaignState = CampaignService.getCampaignState(campaignId);
                            if (StringUtils.isEmpty(campaignState) || ResourceStates.LOCKED.equalsIgnoreCase(campaignState)) {
                                throw new BadRequestException(SystemError.INVALID_CAMPAIGN);
                            }
                        }

                    }
                }

                DropshipOrderObj dropshipOrderObj = new DropshipOrderObj.Builder(orderIdPrefix)
                        .orderCurrency(orderCurrency)
                        .state(ResourceStates.DRAFT)
                        .shippingId(shippingId)
                        .trackingNumber(trackingNumber)
                        .note(note)
                        .storeId(storeId)
                        .userId(userId)
                        .referenceOrderId(referenceOrderId)
                        .totalItems(totalItems)
                        .source(source)
                        .addrVerified(isAddrVerified)
                        .build();

                LOGGER.info("dropshipOrderObj: " + dropshipOrderObj.toString());

                Map orderInfoMap = DropshipOrderService.insertDropshipOrder(dropshipOrderObj);

                Map countryTax = CountryTaxService.getTaxByCountry(countryCode);

                String orderId = ParamUtil.getString(orderInfoMap, AppParams.ID);
                
                try {
                	
                	orderInfoMap = createOrderItems(requestOrderItemList, "", orderId, countryCode, orderCurrency, storeId, referenceOrderId, isAddrVerified, userId, source, shippingMethod, countryTax);

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

    private Map createOrderItems(List<Map> requestOrderItemList, String promotionCode, String orderId, String shippingCountryCode, String orderCurrency,
                                 String storeId, String referenceOrderId, int isAddrVerified, String userId, String source, String shippingMethod, Map countryTax)
            throws SQLException, ParseException {
    	initItemGroupQuantity();
        List<Map> orderItemList = new ArrayList<>();
        Double orderAmount = 0.00;
        int totalItems = 0;
        Double totalShippingFee = 0.00;
        
        Set<String> setBaseId = OrderUtil.getSetBaseIdFromItemCustom(requestOrderItemList);

        Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, shippingCountryCode, shippingMethod);

        Double totalTax = 0d;
        

        List<Map> allBase = new ArrayList<Map>();
        Map listBase = BaseService.getAllBaseCache();
        listBase.forEach((k, v) -> allBase.addAll((Collection<? extends Map>) v));
        
        for (Map requestItem : requestOrderItemList) {

            Map orderItem = createOrderItem(orderId, requestItem, orderCurrency, promotionCode, shippingCountryCode, userId, source, shippingMethod, referenceOrderId, shippingInfo, countryTax, allBase);
            double itemAmount = GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
            int quantity  = GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));

            orderAmount += itemAmount;
            totalShippingFee+= GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
            totalItems += quantity;
            
            double taxAmount = GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));
            totalTax += taxAmount;
            
            orderItemList.add(orderItem);
        }

        String addrVerifiedNote = "";
        if ("US".equalsIgnoreCase(shippingCountryCode) && isAddrVerified==1) {
            addrVerifiedNote = "Seller agree for bypass address verified";
        }

        if (shippingMethod.equalsIgnoreCase("express")) {
            DropshipOrderProductService.updateShippingMethod(orderId, shippingMethod);
        }
        orderAmount = GetterUtil.format(orderAmount, 2);
        totalTax = GetterUtil.format(totalTax, 2);
        Map orderInfoMap = DropshipOrderService.updateOrderV2(orderId, orderAmount.toString(), orderCurrency,
                ResourceStates.QUEUED, StringPool.BLANK, storeId, referenceOrderId, totalItems, isAddrVerified, addrVerifiedNote, totalTax.toString(), totalShippingFee);

        orderInfoMap.put(AppParams.ITEMS, orderItemList);

        return orderInfoMap;
    }

    private Map createOrderItem(String orderId, Map requestItem, String currency, String promotionCode, String shippingCountryCode,
                                String userId, String source, String shippingMethod, String referenceOrderId, Map shippingInfo, Map countryTax, List<Map> allBase) throws SQLException {

        LOGGER.info("requestItem: " + requestItem.toString());
        String baseId = ParamUtil.getString(requestItem, AppParams.BASE_ID);

        String sizeId = ParamUtil.getString(requestItem, AppParams.SIZE_ID);
        String sizeName = ParamUtil.getString(requestItem, AppParams.SIZE_NAME);
        int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
        String colorId = ParamUtil.getString(requestItem, AppParams.COLOR_ID);
        String color = ParamUtil.getString(requestItem, AppParams.COLOR);
        String colorName = ParamUtil.getString(requestItem, AppParams.COLOR_NAME);
        
        String colorValueDB = "";
        String colorNameDB = "";
        String sizeNameDB = "";
        
        boolean checkBaseMap = allBase.stream().filter(m -> (m.get(AppParams.BASE_ID)).equals(baseId)).findFirst().isPresent();
        LOGGER.info("checkBaseMap: " + checkBaseMap);
        if (checkBaseMap) {
        	Map baseMap = allBase.stream().filter(m -> (m.get(AppParams.BASE_ID)).equals(baseId)).findFirst().get();
        	
        	List<Map> colorList = ParamUtil.getListData(baseMap, AppParams.COLORS);
        	boolean checkColorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().isPresent();
        	if (checkColorMap) {
        		Map colorMap = colorList.stream().filter(m -> (m.get(AppParams.ID)).equals(colorId)).findFirst().get();
        		colorNameDB = ParamUtil.getString(colorMap, AppParams.NAME);
        		colorValueDB = ParamUtil.getString(colorMap, AppParams.VALUE);
        	}
        	
        	List<Map> sizeList = ParamUtil.getListData(baseMap, AppParams.SIZES);
        	boolean checkSizeMap = sizeList.stream().filter(m -> (m.get(AppParams.ID)).equals(sizeId)).findFirst().isPresent();
        	if (checkSizeMap) {
        		Map sizeMap = sizeList.stream().filter(m -> (m.get(AppParams.ID)).equals(sizeId)).findFirst().get();
        		sizeNameDB = ParamUtil.getString(sizeMap, AppParams.NAME);
        	}
        }
        LOGGER.info("get colorNameDB: " + colorNameDB);
        LOGGER.info("get colorValueDB: " + colorValueDB);
        LOGGER.info("get sizeNameDB: " + sizeNameDB);
        
        if (StringUtils.isEmpty(colorNameDB) || StringUtils.isEmpty(sizeNameDB)) {
        	throw new BadRequestException(SystemError.INVALID_BASE_ID);
        }
        
        if (!colorNameDB.equalsIgnoreCase(colorName) || !sizeNameDB.equalsIgnoreCase(sizeName)) {
        	LOGGER.info("INVALID COLOR_ID OR SIZE_ID");
        	throw new BadRequestException(SystemError.INVALID_COLOR_OR_SIZE);
        }

        Map designMap = ParamUtil.getMapData(requestItem, AppParams.DESIGNS);
        String designFrontUrl = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL);
        String designBackUrl = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL);
        String mockup_front_url = ParamUtil.getString(designMap, AppParams.MOCKUP_FRONT_URL);
        String mockup_back_url = ParamUtil.getString(designMap, AppParams.MOCKUP_BACK_URL);

        String design_front_url_md5 = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL_MD5);
        String design_back_url_md5 = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL);

        String campaignId = ParamUtil.getString(requestItem, AppParams.CAMPAIGN_ID);

        String unitAmount = ParamUtil.getString(requestItem, AppParams.UNIT_AMOUNT);

        LOGGER.info("campaignId: " + campaignId);
        if (StringUtils.isEmpty(campaignId)) {
            campaignId = userId + "-";
            String md5 = "";
            try {
                md5 = Common.getMD5(design_front_url_md5 + design_back_url_md5);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (StringUtils.isNotEmpty(design_front_url_md5) || StringUtils.isNotEmpty(design_back_url_md5)) {
                campaignId = campaignId + md5;
            } else {
                LOGGER.info("INVALID_DESIGN");
                throw new BadRequestException(SystemError.INVALID_DESIGN);
            }
        }

        String productId = ParamUtil.getString(requestItem, AppParams.PRODUCT_ID);
        String variantId = ParamUtil.getString(requestItem, AppParams.VARIANT_ID);

        String baseName = ParamUtil.getString(requestItem, AppParams.BASE_NAME);
        String variantName = ParamUtil.getString(requestItem, AppParams.VARIANT_NAME);
        if (StringUtils.isEmpty(variantName)){
            variantName = baseName + " - " + colorName;
        }

        LOGGER.info("productId: " + productId + " - variantId: " + variantId);

        int isTwoDesigns = 0;
        if ((designFrontUrl!=null && designFrontUrl.isEmpty()==false)  && (designBackUrl!=null && designBackUrl.isEmpty()==false)) {
            isTwoDesigns = 1;
        }

        double baseCost = BaseService.getDropshipBaseCost(baseId, sizeId, isTwoDesigns);
        LOGGER.info("baseCost: " + baseCost);
        Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseId, shippingMethod, quantity, shippingInfo);
        Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);

        double productAmount = GetterUtil.format(baseCost * quantity + shippingFee, 2);
        LOGGER.info("+++productAmount = " + productAmount);
        Double taxAmount = OrderUtil.getTaxByAmountAndByCountry(productAmount,countryTax);
        productAmount = GetterUtil.format(productAmount + taxAmount, 2);
        LOGGER.info("+++taxAmount = " + taxAmount);

        DropshipOrderProductObj orderProductObj = new DropshipOrderProductObj.Builder(orderId)
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
                .lineItemId(referenceOrderId)
                .variantFrontUrl(mockup_front_url)
                .variantBackUrl(mockup_back_url)
                .colorId(colorId)
                .colorValue(color)
//                .partnerSku(setPartnerSku)
                .colorName(colorName)
                .sizeName(sizeName)
                .shippingMethod(shippingMethod)
//                .printDetail(setPrintDetail)
                .itemType(ResourceStates.NORMAL)
//                .partnerProperties(setPartnerProperties)
//                .partnerOption(setPartnerOption)
                .designFrontUrl(designFrontUrl)
                .designBackUrl(designBackUrl)
                .unitAmount(unitAmount)
                .taxAmount(taxAmount)
                .build();

        LOGGER.info("orderProductObj: " + orderProductObj.toString());
        Map orderItem = DropshipOrderProductService.insertDropshipOrderProduct(orderProductObj);

//		if (variantId != null && variantId.isEmpty() == false) {
//			ShopifyAppService.orderProductUpdateThumbUrl(variantId, orderId);
//		}

        return orderItem;
    }

    private static final Logger LOGGER = Logger.getLogger(DropshipOrderCreateCustomHandler.class.getName());

}