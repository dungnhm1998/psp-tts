/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.ProductService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 *
 * @author HIEPHV
 */
public class WoocommerceOrderSyncHandler extends PSPOrderHandler implements Handler<RoutingContext> {

    private static final String USD = "USD";

    @Override
    public void handle(RoutingContext routingContext) {

        if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
            throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
        }

        routingContext.vertx().executeBlocking(future -> {
            try {

//                JsonObject requestBodyJson = routingContext.getBodyAsJson();
//                String orderIds = requestBodyJson.getString(AppParams.ORDERS);
//                List<DropshipOrderResultImport> orderResult = new LinkedList<>();

//                if (!orderIds.isEmpty()) {

//                    String storeId = routingContext.request().getParam(AppParams.ID);
//                    Map storeMap = DropShipStoreService.lookUp(storeId);
//                    if (storeMap.isEmpty()) {
//                        throw new BadRequestException(SystemError.INVALID_STORE);
//                    }

//                    String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
//                    String consumerSecret = ParamUtil.getString(storeMap, AppParams.SECRET);
//                    String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
//                    String userId = ParamUtil.getString(storeMap, AppParams.USER_ID);
//                    String[] IdList = orderIds.split("\\,");
//                    initItemGroupQuantity();
//                    for (String orderId : IdList) {
//
//                        Boolean conflict = DropshipOrderService.CheckConflict(orderId, userId, storeId);
//
//                        if (!conflict) {
//
//                            Map wooOrder = WooService.GetOrderById(domain, consumerKey, consumerSecret, orderId);
//
//                            if (!wooOrder.isEmpty()) {
//                                Map shippingAddressMap = ParamUtil.getMapData(wooOrder, AppParams.SHIPPING);
//
//                                if (shippingAddressMap == null || shippingAddressMap.isEmpty()) {
//
//                                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
//                                    routingContext.put(AppParams.RESPONSE_MSG,
//                                            HttpResponseStatus.OK.reasonPhrase());
//
//                                } else {
//
//                                    String shippingId = "", trackingNumber = "", note = "",
//                                            channel = "woocommerce", shippingCountryCode = "";
//                                    double orderAmount = 0d, subAmount = 0d, shippingFee = 0d;
//                                    int totalItems = 0;
//                                    DropshipOrderObj orderObj = null;
//
//                                    List<Map> orderItems = ParamUtil.getListData(wooOrder, AppParams.LINE_ITEMS);
//
//                                    if (orderItems != null && !orderItems.isEmpty()) {
//                                        List<DropshipOrderProductTypeObj> orderProductObjs = new ArrayList<>();
//
//                                        Map shippingAddress = ParamUtil.getMapData(wooOrder, AppParams.SHIPPING);
//
//                                        shippingCountryCode = ParamUtil.getString(shippingAddress,
//                                                AppParams.COUNTRY);
//
//                                        for (Map orderItem : orderItems) {
//
//                                        	DropshipOrderProductTypeObj dropshipOrderProductObj = createOrderItem(
//                                                    shippingCountryCode, USD, orderItem, userId);
//                                            if (dropshipOrderProductObj != null) {
//                                                orderProductObjs.add(dropshipOrderProductObj);
//                                            }
//                                        }
//                                        if (!orderProductObjs.isEmpty()) {
//
//                                            shippingId = saveShippingInfo(shippingAddress);
//
//                                            trackingNumber = AppUtil.generateOrderTrackingNumber();
//                                            for (DropshipOrderProductTypeObj odObj : orderProductObjs) {
//                                                subAmount += odObj.getPrice() * odObj.getQuantity();
//                                                shippingFee += odObj.getShippingFee();
//                                                totalItems += odObj.getQuantity();
//                                            }
//                                            orderAmount = GetterUtil.format(subAmount + shippingFee, 2);
//                                            subAmount = GetterUtil.format(subAmount, 2);
//
//                                            orderObj = new DropshipOrderObj(
//                                                    createOrderPrefix(orderProductObjs.get(0)), orderAmount, USD,
//                                                    ResourceStates.CREATED, shippingId, trackingNumber, note,
//                                                    channel, subAmount, shippingFee, storeId, userId,
//                                                    orderId, totalItems);
//
//                                            Map orderMap = DropshipOrderService.insert(orderObj);
//
//                                            for (DropshipOrderProductTypeObj odObj : orderProductObjs) {
//                                                odObj.setOrderId(orderId);
//                                                DropshipOrderProductService.insertDropshipOrderProduct(odObj);
//                                            }
//                                            orderResult.add(new DropshipOrderResultImport(orderId,
//                                                    ResourceStates.SUCCEEDED, ""));
//
//                                        }
//
//                                    }
//
//                                }
//                            }
//
//                        } else {
//                            orderResult.add(new DropshipOrderResultImport(orderId,
//                                    ResourceStates.EXIST, "sync failed"));
//
//                        }
//
//                    }

//                    Map<String, DropshipOrderResultImport> map = orderResult.stream().collect(Collectors.toMap(
//                            DropshipOrderResultImport::getName, DropshipOrderResultImport -> DropshipOrderResultImport));
//                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
//                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
//                    routingContext.put(AppParams.RESPONSE_DATA, map);
//                }
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

    private String saveShippingInfo(Map shippingAddress) throws SQLException {

        String email = ParamUtil.getString(shippingAddress, AppParams.EMAIL);
        String name = ParamUtil.getString(shippingAddress, AppParams.FIRST_NAME) + StringPool.SPACE
                + ParamUtil.getString(shippingAddress, AppParams.LAST_NAME);
        String phone = ParamUtil.getString(shippingAddress, AppParams.PHONE);
        String line1 = ParamUtil.getString(shippingAddress, AppParams.ADDRESS_1);
        String line2 = ParamUtil.getString(shippingAddress, AppParams.ADDRESS_2);
        String city = ParamUtil.getString(shippingAddress, AppParams.CITY);
        String state = ParamUtil.getString(shippingAddress, AppParams.PROVINCE_CODE);
        String postalCode = ParamUtil.getString(shippingAddress, AppParams.ZIP);
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
                    orderProductObj = new DropshipOrderProductObj();
                    int quantity = ParamUtil.getInt(orderItem, AppParams.QUANTITY);
                    String campaignId = ParamUtil.getString(variantMap, AppParams.CAMPAIGN_ID);
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

//                    orderProductObj = new DropshipOrderProductObj("", campaignId, productId, variantId, variantName,
//                            sizeId, baseCost, currency, quantity, shippingFee, amount, baseId, baseCost, baseShortCode,
//                            state, referenceId);
//            		orderProductObj.setOrderId(orderId);
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
                    orderProductObj.setDesignFrontUrl(designFrontUrl);
                    orderProductObj.setDesignBackUrl(designBackUrl);
//            		orderProductObj.setShippingMethod(setShippingMethod);
//            		orderProductObj.setPrintDetail(setPrintDetail);
            		orderProductObj.setItemType(ResourceStates.NORMAL);
//            		orderProductObj.setPartnerProperties(setPartnerProperties);
//            		orderProductObj.setPartnerOption(setPartnerOption);
            		orderProductObj.setBaseShortCode(baseShortCode);
                    orderProductObj.setTaxAmount(taxAmount);
            		
                }
            }

        }
        return orderProductObj;
    }

    private static final Logger LOGGER = Logger.getLogger(WoocommerceOrderSyncHandler.class.getName());
}