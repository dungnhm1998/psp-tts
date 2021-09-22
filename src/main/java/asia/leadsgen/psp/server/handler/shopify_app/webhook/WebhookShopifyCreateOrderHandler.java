package asia.leadsgen.psp.server.handler.shopify_app.webhook;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipCustomApiOrderCreateHandler;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.BasePhoneCaseUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class WebhookShopifyCreateOrderHandler extends PSPOrderHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {

//				LOGGER.info("WebhookShopifyCreateOrderHandler() - requestBodyMap= " + routingContext.getBodyAsString());
//				String storeId = routingContext.request().getParam("id");
//				LOGGER.info("WebhookShopifyCreateOrderHandler() - storeId= " + storeId);
//				if (!StringUtils.isEmpty(storeId) && !StringUtils.isEmpty(routingContext.getBodyAsString())) {
//					try {
//						Map storeResult = DropShipStoreService.lookUp(storeId);
//						if (!storeResult.isEmpty()) {
//							initItemGroupQuantity();
//							String consumerKey = ParamUtil.getString(storeResult, AppParams.API_KEY);
//							String domain = ParamUtil.getString(storeResult, AppParams.DOMAIN);
//							String channel = ParamUtil.getString(storeResult, AppParams.CHANNEL);
//							String userId = ParamUtil.getString(storeResult, AppParams.USER_ID);
//	
//							processOrder(routingContext.getBodyAsString(), consumerKey, domain, userId, storeId, channel);
//						}
//
//					} catch (SQLException e) {
//						e.printStackTrace();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//
//				}
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
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

	private void processOrder(String body_string, String consumerKey, String domain, String userId, String storeId, String channel) {

		String shipping_id = "";
		String order_id = "";
		String shipping_country = "";

		try {

			JSONObject obj = new JSONObject(body_string);
			JSONArray line_items = obj.getJSONArray("line_items");
			String id = obj.optString("name", "");
			String originalId = String.valueOf(obj.getLong("id"));
			String email = obj.optString("email", "");
			String landing_site = obj.optString("landing_site", "");
			String partner_url = domain + landing_site;

			JSONArray list_item_map = checkVariantMap(userId, storeId, line_items);
			JSONObject shipping_address = obj.getJSONObject("shipping_address");

			String source = ResourceSource.CUSTOM_SHOPIFY_APP;
			String state = ResourceStates.QUEUED;
			boolean isAllItemVariantMap = allVariantMap(userId, list_item_map);
			if (!isAllItemVariantMap) {
				state = ResourceStates.DRAFT;
			}

			if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId, id, source)) {
//				ShippingService.deleteByIdCSVImport(shipping_id);
				LOGGER.info("order= " + id + "-- storeId=" + storeId + "-- source=" + source + " is exists");
			} else {
				LOGGER.info("order= " + id + "-- storeId=" + storeId + "-- source=" + source + " is not exists");
				LOGGER.info("list_item_map= " + list_item_map.toString());
				Map map_shipping = createShiping(shipping_address, email);
				shipping_id = ParamUtil.getString(map_shipping, AppParams.ID);
				Map map_address = ParamUtil.getMapData(map_shipping, AppParams.ADDRESS);
				shipping_country = ParamUtil.getString(map_address, AppParams.COUNTRY);
				if (StringUtils.isEmpty(shipping_id)) {
					LOGGER.info("Create shipping fail.");
				} else {
					if (create_dropship_order(body_string, consumerKey, domain, userId, storeId, channel, shipping_id, id,
							source, state, list_item_map, shipping_country, partner_url, originalId)) {
					}
				}
			}

		} catch (Exception e) {
			LOGGER.info("processOrder()- Exception: shipping_id = " + shipping_id + " --- order_id = " + order_id);
			e.printStackTrace();
//			try {
//				if (StringUtils.isNotEmpty(shipping_id)) {
//					ShippingService.deleteByIdCSVImport(shipping_id);
//				}
//				if (StringUtils.isNotEmpty(order_id)) {
//					DropshipOrderService.deleteByIdCSVImport(order_id);
//				}
//			} catch (SQLException e1) {
//				e1.printStackTrace();
//			}
		}
	}
	
	private JSONArray checkVariantMap(String userId, String store_id,JSONArray line_items) {
		
		JSONArray result = new JSONArray();
		
		for (int i = 0; i < line_items.length(); i++) {
			JSONObject obj = line_items.getJSONObject(i);
			Long variant_id = null;
			if(obj.has("variant_id") && obj.get("variant_id") instanceof Long) {
				variant_id = obj.getLong("variant_id");
			}
			
			Long product_id = null;
			if(obj.has("product_id") && obj.get("product_id") instanceof Long) {
				product_id = obj.getLong("product_id");
			}
			
			try {
				Map variantMap = null;
				try {
					variantMap = ShopifyAppService.getShopifyProductVariantSync(store_id, product_id, variant_id);
				} catch (Exception e) {
					// TODO: handle exception
				}
				if(variantMap == null || variantMap.isEmpty()) {
					obj.put("is_map", false);
					result.put(obj);
					LOGGER.info("not map variant");
				} else {
					
					String campaignId = ParamUtil.getString(variantMap, AppParams.S_CAMPAIGN_ID);
					String campaignState = CampaignService.getCampaignState(campaignId);
					if (StringUtils.isEmpty(campaignState) || ResourceStates.LOCKED.equalsIgnoreCase(campaignState)) {
						LOGGER.info("This campaign is Locked: " + campaignId);
						obj.put("is_map", false);
						result.put(obj);
					} else {
						LOGGER.info("map variant");
						obj.put("is_map", true);
						obj.put("size_name", ParamUtil.getString(variantMap, AppParams.S_SIZE_NAME));
						obj.put("bgp_product_id", ParamUtil.getString(variantMap, AppParams.S_BGP_PRODUCT_ID));
						obj.put("bgp_variant_id", ParamUtil.getString(variantMap, AppParams.S_BGP_VARIANT_ID));
						obj.put("size_name", ParamUtil.getString(variantMap, AppParams.S_SIZE_NAME));
						obj.put("color_name", ParamUtil.getString(variantMap, AppParams.S_COLOR_NAME));
						obj.put("color_value", ParamUtil.getString(variantMap, AppParams.S_COLOR));
						obj.put("partner_design_back", ParamUtil.getString(variantMap, AppParams.S_PARTNER_DESIGN_BACK));
						obj.put("partner_design_front", ParamUtil.getString(variantMap, AppParams.S_PARTNER_DESIGN_FRONT));
						obj.put("variant_back_url", ParamUtil.getString(variantMap, AppParams.S_VARIANT_BACK_URL));
						obj.put("variant_front_url", ParamUtil.getString(variantMap, AppParams.S_VARIANT_FRONT_URL));
						obj.put("color_id", ParamUtil.getString(variantMap, AppParams.S_COLOR_ID));
						obj.put("size_id", ParamUtil.getString(variantMap, AppParams.S_SIZE_ID));
						obj.put("base_id", ParamUtil.getString(variantMap, AppParams.S_BASE_ID));
						obj.put("sale_price", ParamUtil.getString(variantMap, AppParams.S_SALE_PRICE));
						obj.put("campaign_id", campaignId);
						result.put(obj);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return result;
	}

	
	boolean allVariantMap(String userId, JSONArray line_items) {
		
		boolean result = true;
		
		for (int i = 0; i < line_items.length(); i++) {
			JSONObject obj = line_items.getJSONObject(i);
			LOGGER.info("obj= " + obj.toString());
			boolean is_map = obj.getBoolean("is_map");
			if(!is_map) {
				result = false;
				break;
			}
		}
		
		return result;
	}
	
	private Map createShiping(JSONObject shipping_address, String email) throws Exception {

		String last_name = shipping_address.optString("last_name", "");
		String first_name = shipping_address.optString("first_name", "");
		String address1 = shipping_address.optString("address1", "");
		String address2 = shipping_address.optString("address2", "");
		String phone = shipping_address.optString("phone", "");
		String city = shipping_address.optString("city", "");		
		String zip = shipping_address.optString("zip", "");
		String country_code = shipping_address.optString("country_code", "");
		Locale locale = new Locale("", country_code);
		String country = locale.getDisplayCountry();
		String province = "";
		if ("US".equalsIgnoreCase(country_code)
				|| "CA".equalsIgnoreCase(country_code)) {
			province = shipping_address.optString("province_code", "");
		} else if ("MX".equalsIgnoreCase(country_code)) {
			String provinceName = shipping_address.optString("province", "");
			province = ShippingService.mexicoStateAlpha2Code.get(provinceName);		
		} else {
			province = shipping_address.optString("province", "");
		}
			
		String shipping_name = first_name + " " + last_name;
		Map shippingResult = ShippingService.insert(
					shipping_name,
					email, 
					phone,
					address1, 
					address2,
					city, 
					province,
					zip, 
					country_code,
					country);
		return shippingResult;
	}

	private boolean create_dropship_order(String body_string, String consumerKey, String domain, String userId, String storeId,
			String channel, String shippingId, String referenceOrderId, String source, String state, JSONArray list_item,
			String shipping_country, String partner_url, String originalId) throws JSONException, Exception {
		
		boolean result = false;
		JSONObject firstItem = list_item.getJSONObject(0);
		Map order = new LinkedHashMap();
		String trackingNumber = AppUtil.generateOrderTrackingNumber();
		String orderIdPrefix = userId + "-SPF-APP";

		DropshipOrderObj dropshipOrderObj = new DropshipOrderObj.Builder(orderIdPrefix)
				.orderCurrency("USD")
				.state(state)
				.shippingId(shippingId)
				.trackingNumber(trackingNumber)
				.channel(channel)
				.storeId(storeId)
				.userId(userId)
				.referenceOrderId(referenceOrderId)
				.source(source)
				.minifiedJson(body_string)
				.originalId(originalId)
				.addrVerified(0)
				.build();
		
		LOGGER.info("dropshipOrderObj=" + dropshipOrderObj.toString());

		order = DropshipOrderService.insertDropshipOrder(dropshipOrderObj);

		if (!order.isEmpty()) {
			
			String orderId = ParamUtil.getString(order, AppParams.ID);
			LOGGER.info("orderId: " + orderId);
			Map orderInfoMap = null;
			
			try {
				orderInfoMap = processOrderSKUItems(consumerKey, domain, userId, list_item, orderId, referenceOrderId,
						shipping_country, partner_url);
				LOGGER.info("processOrderSKUItems: " + orderInfoMap.toString());
			} catch (Exception e) {
				e.printStackTrace();
				if (StringUtils.isNotEmpty(orderId)) {
					DropshipOrderService.deleteByIdCSVImport(orderId);
				}
//				if (StringUtils.isNotEmpty(shipping_id)) {
//					ShippingService.deleteByIdCSVImport(shipping_id);
//				}
			}
			
			if (orderInfoMap.isEmpty()) {
				DropshipOrderService.deleteByIdCSVImport(orderId);
			} else {
				result = true;
			}
			
		} else {
			if (StringUtils.isNotEmpty(shippingId)) {
				LOGGER.info("DELETE Shipping");
				ShippingService.deleteByIdCSVImport(shippingId);
			}
		}

		return result;
	}

	private Map processOrderSKUItems(String consumerKey, String domain, String userId, JSONArray dropshipOrderProducts,
			String orderId, String orderReferenceId, String shipping_country, String partner_url)
			throws JSONException, Exception {

		int orderProductSuccess = 0;

		int orderProductTotal = dropshipOrderProducts.length();
		List<Map> orderItemList = new ArrayList<>();
		int totalItems = 0;
		double orderTotal = 0.00;
		double orderShippingTotal = 0.00d;
		double orderSubTotal = 0.00d;
		int addressVerified = 0;
		Double totalTax = 0.00d;
		for (int i = 0; i < orderProductTotal; i++) {
			
			try {
				
				JSONObject vObj = dropshipOrderProducts.getJSONObject(i);

				Map orderItem = createOrderSKUItem(consumerKey, domain, userId, orderId, vObj, shipping_country,
						orderReferenceId, partner_url);
				LOGGER.info("orderItem: " + orderItem);
				String orderProductId = ParamUtil.getString(orderItem, AppParams.ID);
				if (!StringUtils.isEmpty(orderProductId)) {
					orderProductSuccess++;
					orderItemList.add(orderItem);
					orderTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
					totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));
					orderSubTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SUBTOTAL));
					orderShippingTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
					totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
				}
				
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "[ERROR]", e);
				LOGGER.info("processOrderSKUItems()- Exception: orderId = " + orderId);
				e.printStackTrace();
				break;
			}
		}

		Map orderInfoMap = new LinkedHashMap<>();
		if (orderProductSuccess == orderProductTotal) {
			String addressCheckMessage = "";
			if (addressVerified > 1) {
				addressCheckMessage = DropshipCustomApiOrderCreateHandler.IGNORE_ADDRESS_CHECK_NOTE;
			}
			DropshipOrderService.updateQuantityAmountAddressCheck(orderId, orderTotal, orderSubTotal,
					orderShippingTotal, totalItems, addressVerified, addressCheckMessage, totalTax.toString());
			orderInfoMap.put(AppParams.ITEMS, orderItemList);
			orderInfoMap.put(AppParams.ID, orderId);
		} else {
			// remove order product by id
			DropshipOrderProductService.deleteByOrderCSVImport(orderId);
		}

		return orderInfoMap;
	}

	private Map createOrderSKUItem(String consumerKey, String domain, String userId, String orderId, JSONObject vObj,
			String shipping_country, String orderReferenceId, String partnerUrl) throws JSONException, Exception {

		LOGGER.info("dropshipOrderProduct= " + vObj.toString());
		
		String product_id = "";
		String variant_id = "";
		String bgpProductId = "";
		String bgpVariantId = "";
		String campaignId = "";
		String variantName = "";
		if(vObj.has("name") && vObj.get("name") instanceof String) {
			variantName = String.valueOf(vObj.get("name"));
		} else if (vObj.has("title") && vObj.get("title") instanceof String) {
			variantName = String.valueOf(vObj.get("title"));
		} else if (vObj.has("variant_title") && vObj.get("variant_title") instanceof String) {
			variantName = String.valueOf(vObj.get("variant_title"));
		}
		String sizeName = "";
		String colorName = "";
		String colorValue = "";
		String partnerDesignBack = "";
		String partnerDesignFront = "";
		String variantBackUrl = "";
		String variantFrontUrl = "";
		String colorId = "";
		String sizeId = "";
		String baseId = "";
		String partnerSize = "";
		String partnerColor = "";
		String partnerSku = "";
		
		int quantity = vObj.getInt("quantity");
		Double shippingFee = 0.00d;
		double productSubTotal = 0;
		double productAmount = 0;
		double baseCost = 0d;
		Double taxAmount = 0.00d;
		
		boolean is_map = vObj.getBoolean("is_map");
		
		if (is_map) {
			bgpProductId = vObj.getString("bgp_product_id");
			bgpVariantId = vObj.getString("bgp_variant_id");
			campaignId = vObj.getString("campaign_id");
			sizeName = vObj.getString("size_name");
			colorName = vObj.getString("color_name");
			colorValue = vObj.getString("color_value");
			partnerDesignBack = vObj.getString("partner_design_back");
			partnerDesignFront = vObj.getString("partner_design_front");
			variantBackUrl = vObj.getString("variant_back_url");
			variantFrontUrl = vObj.getString("variant_front_url");
			colorId = vObj.getString("color_id");
			sizeId = vObj.getString("size_id");
			baseId = vObj.getString("base_id");
			
			if (BasePhoneCaseUtil.isPhoneCase(baseId)) {
				variantName = variantName.substring(0, variantName.lastIndexOf('-') - 1);
			}
			
			int isTwoDesigns = 0;
			if ((partnerDesignFront != null && partnerDesignFront.isEmpty() == false)
					&& (partnerDesignBack != null && partnerDesignBack.isEmpty() == false)) {
				isTwoDesigns = 1;
			}
			
			baseCost = BaseService.getDropshipBaseCost(baseId, sizeId, isTwoDesigns);
			LOGGER.info("baseCost: " + baseCost);

			Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, shipping_country, quantity);
			shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);
			taxAmount = ParamUtil.getDouble(feeMap, AppParams.TAX_AMOUNT);
			productAmount = GetterUtil.format(baseCost * quantity + shippingFee + taxAmount, 2);
			
		} else {

			if(vObj.has("variant_id") && vObj.get("variant_id") instanceof Long) {
				variant_id = String.valueOf(vObj.get("variant_id"));
			}		
			if(vObj.has("product_id") && vObj.get("product_id") instanceof Long) {
				product_id = String.valueOf(vObj.get("product_id"));
			}
			
			if (!StringUtils.isEmpty(variant_id) && !StringUtils.isEmpty(product_id)) {
				
				Map variantObj = getColorAndSizeProductItem(consumerKey, domain, product_id, variant_id);
				partnerSku = String.valueOf(variantObj.get("sku"));
				String option1 = String.valueOf(variantObj.get("option1"));
				String option2 = String.valueOf(variantObj.get("option2"));
				String option3 = String.valueOf(variantObj.get("option3"));

				List<String> optionsString = new ArrayList<String>();
				optionsString.add(option1);
				optionsString.add(option2);
				optionsString.add(option3);

				Map optionsMap = getOptionProduct(consumerKey, domain, product_id);
				for (int i = 0; i < optionsMap.size(); i++) {
					if (optionsMap.get(i + 1).equals("Size")) {
						partnerSize = optionsString.get(i);
					} else if (optionsMap.get(i + 1).equals("Color")) {
						partnerColor = optionsString.get(i);
					}
				}
				String image_id = String.valueOf(variantObj.get("image_id"));
				LOGGER.info("image_id= " + image_id);

				if(image_id != "null") {
					LOGGER.info("get variant image...");
					variantFrontUrl = getMockupUrl(consumerKey, domain, product_id, image_id);
				} else {
					LOGGER.info("get product image...");
					variantFrontUrl = getProductImgUrl(consumerKey, domain, product_id);
				}				
			}
		}

		JsonObject partnerOption = new JsonObject();
		partnerOption.put(AppParams.PARTNER_COLOR, partnerColor);
		partnerOption.put(AppParams.PARTNER_SIZE, partnerSize);

		JsonObject partnerProperties = new JsonObject();
		partnerProperties.put(AppParams.PARTNER_URL, partnerUrl);
		partnerProperties.put(AppParams.PARTNER_DESIGN_FRONT, partnerDesignFront);
		partnerProperties.put(AppParams.PARTNER_DESIGN_FRONT, partnerDesignBack);

		DropshipOrderProductObj orderProductObj = new DropshipOrderProductObj.Builder(orderId)
				.campaignId(campaignId)
				.productId(bgpProductId)
				.variantId(bgpVariantId)
				.sizeId(sizeId)
				.price(baseCost)
				.shippingFee(shippingFee)
				.currency("USD")
				.quantity(quantity)
				.state(ResourceStates.APPROVED)
				.variantName(variantName)
				.amount(productAmount)
				.baseCost(baseCost)
				.baseId(baseId)
				.lineItemId(orderReferenceId)
				.variantFrontUrl(variantFrontUrl)
				.variantBackUrl(variantBackUrl)
				.colorId(colorId)
				.colorValue(colorValue)
				.partnerSku(partnerSku)
				.colorName(colorName)
				.sizeName(sizeName)
//				.shippingMethod(shippingMethod)
//				.printDetail(setPrintDetail)
//				.itemType(setItemType)
				.partnerProperties(partnerProperties.toString())
				.partnerOption(partnerOption.toString())
//				.baseShortCode(baseShortCode)
				.taxAmount(taxAmount)
				.build();

		LOGGER.info("orderProductObj: " + orderProductObj.toString());
		Map orderItem = DropshipOrderProductService.insertDropshipOrderProduct(orderProductObj);

		if (orderItem != null && orderItem.isEmpty() == false) {
			orderItem.put(AppParams.SUBTOTAL, productSubTotal);

//			if (bgpVariantId != null && bgpVariantId.isEmpty() == false) {
//				ShopifyAppService.orderProductUpdateThumbUrl(bgpVariantId, orderId);
//			}
		}

		return orderItem;
	}

	private Map getColorAndSizeProductItem(String consumerKey, String domain, String product_id, String variant_id)
			throws UnirestException {

		String url = String.format(ShopifyAPIEndpoints.GET_PRODUCT_VARIANT, domain, product_id, variant_id);
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json")
				.header("X-Shopify-Access-Token", consumerKey).asString();

		Map mapResult = new JsonObject(response.getBody()).getMap();
		Map variantMap = ParamUtil.getMapData(mapResult, "variant");

		return variantMap;
	}

	private Map getOptionProduct(String consumerKey, String domain, String product_id) throws UnirestException {

		String url = String.format(ShopifyAPIEndpoints.GET_PRODUCT_ONE, domain, product_id);
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json")
				.header("X-Shopify-Access-Token", consumerKey).asString();

		Map mapResult = new JsonObject(response.getBody()).getMap();
		Map productMap = ParamUtil.getMapData(mapResult, "product");
		List<Map> optionList = ParamUtil.getListData(productMap, "options");

		Map optionsMap = new LinkedHashMap<>();
		for (Map option : optionList) {
			optionsMap.put(option.get("position"), option.get("name"));
		}

		return optionsMap;
	}

	private String getMockupUrl(String consumerKey, String domain, String product_id, String image_id)
			throws UnirestException {

		String url = String.format(ShopifyAPIEndpoints.GET_PRODUCT_IMAGE, domain, product_id, image_id);
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json")
				.header("X-Shopify-Access-Token", consumerKey).asString();
		LOGGER.info("product_id= " + product_id + " - image_id=" + image_id);
		Map mapResult = new JsonObject(response.getBody()).getMap();
		Map imageMap = ParamUtil.getMapData(mapResult, "image");
		String mockUpUrl = String.valueOf(imageMap.get("src"));
		return mockUpUrl;
	}
	
	private String getProductImgUrl(String consumerKey, String domain, String product_id) throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.GET_PRODUCT_ONE, domain, product_id);
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
                .asString();
		
		if (response.getStatus() != 200 && response.getStatus() != 201) {
		    LOGGER.info("get product image response-status:" + response.getStatus());
		    LOGGER.info("data result text:" + response.getStatusText());
		}
		
		Map mapResult = new JsonObject(response.getBody()).getMap();
		Map productMap = ParamUtil.getMapData(mapResult, "product");	
		Map imageMap = ParamUtil.getMapData(productMap, "image");	
		String productImgSrc = String.valueOf(imageMap.get("src"));
		
		return productImgSrc;
	}

	private static final Logger LOGGER = Logger.getLogger(WebhookShopifyCreateOrderHandler.class.getName());

}
