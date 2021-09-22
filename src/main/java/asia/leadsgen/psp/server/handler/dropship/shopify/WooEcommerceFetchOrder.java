package asia.leadsgen.psp.server.handler.dropship.shopify;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.data.type.RedisKeyEnum;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.MailException;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.EmailObj;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipCustomApiOrderCreateHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderImportCVSHandlerV2;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service.EmailMarketingService;
import asia.leadsgen.psp.service.EmailTemplateService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.RedisService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.BaseSizeService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreCampService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.OrderUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;

public class WooEcommerceFetchOrder extends PSPOrderHandler {
	private static final Logger LOGGER = Logger.getLogger(WooEcommerceFetchOrder.class.getName());

	public Map getOrderWoo(String userId, String storeId, String consumerKey, String consumerSecret, String domain,
						   String channel, String date, String status) throws Exception, UnirestException {
		Map result = new LinkedHashMap<>();
		String key = userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_WOO.getValue();
		LOGGER.info("key= " + key);
		Map task = RedisService.get(key);
		if (task != null && !task.isEmpty()) {

			Map mapResult = new LinkedHashMap();
			result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.SEE_OTHER.code());
			result.put(AppParams.RESPONSE_MSG, "A job is in progressing, you must wait for this job to finish.");
			result.put(AppParams.RESPONSE_DATA, mapResult);
		} else {
			int total = getTotalOrder(consumerKey, consumerSecret, domain, channel, date, status);
			if (total > 0) {
				if (total > 10) {
					Map map = new LinkedHashMap<Integer, String>();
					map.put(AppParams.START_TIME, new Date());
					map.put(AppParams.STORE_ID, storeId);
					map.put(AppParams.STATUS, status);
					map.put(AppParams.DATE, date);
					key = userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_WOO.getValue();
					RedisService.save(key, map);
					Thread one = new Thread() {
						public void run() {

							try {
								int per_page = 10;
								int page = 1;
								int total_page = (total / per_page) + 1;
								int total_order_success = 0;
								LOGGER.info("total_page=" + total_page);
								while (page <= total_page) {
									LOGGER.info("page=" + page);
									HttpResponse<String> response = getData(consumerKey, consumerSecret, domain, date,
											status, page, per_page);
									JSONArray responseMap = new JSONArray(response.getBody());
									int page_order_success = processData(userId, storeId, consumerKey, consumerSecret,
											domain, channel, date, status, responseMap);
									total_order_success += page_order_success;
									page++;

								}
								// gui mail cho kh
//							if(total_order_success > 0) {
								sendMail(userId, total_order_success, "", null);
//							}
								RedisService
										.delete(userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_WOO.getValue());
							} catch (UnirestException e) {
								e.printStackTrace();
								RedisService
										.delete(userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_WOO.getValue());
							} catch (Exception e) {
								e.printStackTrace();
								RedisService
										.delete(userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_WOO.getValue());
							}

						}
					};

					one.start();
					// tra ket qua ve
					Map mapResult = new LinkedHashMap();
					mapResult.put(AppParams.RESULT_MSG,
							"This job is being processed, we will email you when it is completed.");

					result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					result.put(AppParams.RESPONSE_MSG,
							"This job is being processed, we will email you when it is completed.");
					result.put(AppParams.RESPONSE_DATA, mapResult);
				} else {
					HttpResponse<String> response = getData(consumerKey, consumerSecret, domain, date, status, 1,
							total);
					JSONArray responseMap = new JSONArray(response.getBody());
					processData(userId, storeId, consumerKey, consumerSecret, domain, channel, date, status,
							responseMap);

					result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					result.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					result.put(AppParams.RESPONSE_DATA, new LinkedHashMap<>());
				}
			} else {
				result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				result.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				result.put(AppParams.RESPONSE_DATA, new LinkedHashMap<>());
			}
		}
		return result;
	}

	private int processData(String userId, String storeId, String consumerKey, String consumerSecret, String domain,
							String channel, String date, String status, JSONArray responseMap) {
		int success = 0;
		for (Object object : responseMap) {
			JSONObject mJSONObject = new JSONObject(object.toString());
			LOGGER.info("mJSONObject:" + mJSONObject.toString());
			initItemGroupQuantity();
			int order_success = processOrder(consumerKey, consumerSecret, domain, userId, storeId, mJSONObject,
					channel);
			success += order_success;
		}
		return success;
	}

	private int getTotalOrder(String consumerKey, String consumerSecret, String domain, String channel, String date,
							  String status) throws NumberFormatException, Exception {
		int total = 0;
		try {
			HttpResponse<String> response = getData(consumerKey, consumerSecret, domain, date, status, 1, 10);
			if (response != null) {
				LOGGER.info("Get Header response...");
				String headers = response.getHeaders().toString();
//				LOGGER.info("headers: " + headers);
				
				boolean check = response.getHeaders().containsKey("X-WP-Total");
				LOGGER.info("check: " + check);
				if (check) {
					LOGGER.info("X-WP-Total");
					total = Integer.valueOf(response.getHeaders().get("X-WP-Total").get(0));
				} else {
					LOGGER.info("x-wp-total");
					total = Integer.valueOf(response.getHeaders().get("x-wp-total").get(0));
				}
			}

		} catch (Exception e) {
			LOGGER.severe(e.toString());
			e.printStackTrace();
		}
		return total;
	}

	
	
	private HttpResponse<String> getData(String consumerKey, String consumerSecret, String domain, String date,
										 String status, int page, int per_page) throws Exception, UnirestException {
		String formatUrl = MessageFormat.format("{0}/wp-json/wc/v3/orders?consumer_key={1}&consumer_secret={2}", domain,
				consumerKey, consumerSecret);
		if (StringUtils.isNotEmpty(status)) {
			if (status.equalsIgnoreCase("fulfilled")) {
				formatUrl += "&status=" + "completed";
			} else if (status.equalsIgnoreCase("unfulfilled")) {
				formatUrl += "&status=" + "processing";
			}
		}

		formatUrl += "&page=" + String.valueOf(page) + "&per_page=" + String.valueOf(per_page);

		if (StringUtils.isNotEmpty(date)) {
			formatUrl += "&after=" + convertDateFormat(Integer.valueOf(date));
		}
		LOGGER.info("formatUrl:" + formatUrl);
		long start = System.currentTimeMillis();
		HttpResponse<String> response = Unirest.get(formatUrl).asString();

		long elapsedTimeMillis = System.currentTimeMillis() - start;
		float elapsedTimeSec = elapsedTimeMillis / 1000F;

		LOGGER.info("Thoi gian thuc thi:" + elapsedTimeSec);
		if (response.getStatus() != 201) {
			LOGGER.info("data result code:" + response.getStatus());
			// LOGGER.log(Level.SEVERE, "[ERROR]", e);
		}

		JSONArray responseMap = new JSONArray(response.getBody());
		LOGGER.info("url:" + formatUrl + " data result:" + responseMap);
		return response;
	}

	private int processOrder(String consumerKey, String consumerSecret, String domain, String userId, String storeId,
							 JSONObject obj, String channel) {
		int order_success = 0;
		String shipping_id = "";
		String order_id = "";
		String shipping_country = "";

		try {
			JSONArray list_product = obj.getJSONArray("line_items");
			String id = String.valueOf(obj.getInt("id"));
			List<JSONArray> list_order = divideProductVariantToOrder(userId, list_product);
			LOGGER.info("list_order=" + list_order.toString());
			if (!list_order.isEmpty()) {
				if (list_order.size() == 2) {
					LOGGER.info("Phai tach order");
					for (JSONArray jsonArray : list_order) {
						try {

							String source = ResourceSource.CAMP_SYNC;
							String state = ResourceStates.QUEUED;
							JSONObject firstItem = jsonArray.getJSONObject(0);
							if (!firstItem.getBoolean("is_order_camp")) {
								source = ResourceSource.CUSTOM_SYNC;
								state = ResourceStates.DRAFT;
							}
							if (StringUtils.isNotEmpty(id)
									&& DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId, id, source)) {
								LOGGER.info("order= " + id + "-- storeId=" + storeId + "-- source=" + source
										+ " is exists");
							} else {
								Map map_shipping = createShiping(obj.getJSONObject("shipping"),
										obj.getJSONObject("billing"));
								shipping_id = ParamUtil.getString(map_shipping, AppParams.ID);
								Map map_address = ParamUtil.getMapData(map_shipping, AppParams.ADDRESS);
								shipping_country = ParamUtil.getString(map_address, AppParams.COUNTRY);
								if (StringUtils.isEmpty(shipping_id)) {
									LOGGER.info("Create shipping fail.");
								} else {

									order_id = create_dropship_order(consumerKey, consumerSecret, domain, userId,
											storeId, channel, shipping_id, id, source, state, jsonArray,
											shipping_country);
									if (StringUtils.isNotEmpty(order_id)) {
										order_success++;
										order_id = "";
									}
								}
							}

						} catch (Exception e) {
							LOGGER.info("processOrder()- !list_order.isEmpty() Exception: shipping_id = " + shipping_id + " --- order_id = "
									+ order_id);
							e.printStackTrace();
							try {
								if (StringUtils.isNotEmpty(shipping_id)) {
									ShippingService.deleteByIdCSVImport(shipping_id);
								}
								if (StringUtils.isNotEmpty(order_id)) {
									DropshipOrderService.deleteByIdCSVImport(order_id);
									order_success--;
								}
							} catch (SQLException e1) {
								e1.printStackTrace();
							}
						}
					}
				} else {
					LOGGER.info("Khong phai tach order");
					String source = ResourceSource.CAMP_SYNC;
					String state = ResourceStates.QUEUED;
					JSONArray list_item = list_order.get(0);
					JSONObject firstItem = list_item.getJSONObject(0);
					if (!firstItem.getBoolean("is_order_camp")) {
						source = ResourceSource.CUSTOM_SYNC;
						state = ResourceStates.DRAFT;
					}

					if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId, id, source)) {
						ShippingService.deleteByIdCSVImport(shipping_id);
						LOGGER.info("order= " + id + "-- storeId=" + storeId + "-- source=" + source + " is exists");
					} else {
						Map map_shipping = createShiping(obj.getJSONObject("shipping"), obj.getJSONObject("billing"));
						shipping_id = ParamUtil.getString(map_shipping, AppParams.ID);
						Map map_address = ParamUtil.getMapData(map_shipping, AppParams.ADDRESS);
						shipping_country = ParamUtil.getString(map_address, AppParams.COUNTRY);
						if (StringUtils.isEmpty(shipping_id)) {
							LOGGER.info("Create shipping fail.");
						} else {

							order_id = create_dropship_order(consumerKey, consumerSecret, domain, userId, storeId,
									channel, shipping_id, id, source, state, list_item, shipping_country);
							if (StringUtils.isNotEmpty(order_id)) {
								order_success++;
							}

						}
					}
				}
			}

		} catch (Exception e) {
			LOGGER.info("processOrder()- Exception: shipping_id = " + shipping_id + " --- order_id = " + order_id);
			e.printStackTrace();
			try {
				if (StringUtils.isNotEmpty(shipping_id)) {
					ShippingService.deleteByIdCSVImport(shipping_id);
				}
				if (StringUtils.isNotEmpty(order_id)) {
					DropshipOrderService.deleteByIdCSVImport(order_id);
					order_success--;
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}

		}

		LOGGER.info("processOrder()- : shipping_id = " + shipping_id + " --- order_id = " + order_id);
		return order_success;
	}

	private String create_dropship_order(String consumerKey, String consumerSecret, String domain, String userId,
										 String storeId, String channel, String shipping_id, String id, String source, String state,
										 JSONArray list_item, String shipping_country) throws JSONException, Exception {
		boolean result = false;
		JSONObject firstItem = list_item.getJSONObject(0);
		Map order = new LinkedHashMap();
		String trackingNumber = AppUtil.generateOrderTrackingNumber();
		String orderId = "";
		if (source.equalsIgnoreCase(ResourceSource.CAMP_SYNC)) {

			String[] lineSku = firstItem.getString("sku").split("\\|");
			if (lineSku.length >= 2) {
				String variantId = lineSku[0];
				String sizeId = lineSku[1];
				Map variantMap = ProductVariantService.getAndCheckCampaignNotLocked(variantId);
				String campaignId = ParamUtil.getString(variantMap, "campaign_id");
				if (StringUtils.isNotEmpty(userId) && !variantMap.isEmpty() && campaignId.startsWith(userId)) {
					String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);

					if (BaseSizeService.checkAvailabilityForBase(sizeId, baseId) == false) {
						LOGGER.info("Invalid lineitem sku");
					} else {

						String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);
						String orderIdPrefix = DropshipOrderImportCVSHandlerV2.createOrderIdPrefix(productId);
						order = create_dropship_order(userId, storeId, channel, shipping_id, id, trackingNumber,
								orderIdPrefix, state, source, 0, id);
					}
				}
			}
		} else {
			String orderIdPrefix = userId + "-CT";
			order = create_dropship_order(userId, storeId, channel, shipping_id, id, trackingNumber, orderIdPrefix,
					state, source, 0, id);
		}

		if (!order.isEmpty()) {
			orderId = ParamUtil.getString(order, AppParams.ID);
			Map orderInfoMap = null;
			if (source.equalsIgnoreCase(ResourceSource.CAMP_SYNC)) {
				orderInfoMap = processOrderItems(list_item, orderId, id, shipping_country);
			} else {
				orderInfoMap = processOrderSKUItems(consumerKey, consumerSecret, domain, userId, list_item, orderId, id,
						shipping_country);
			}

			if (orderInfoMap.isEmpty()) {
				DropshipOrderService.deleteByIdCSVImport(orderId);
			} else {
				result = true;
			}

		} else {
			if (StringUtils.isNotEmpty(shipping_id)) {
				ShippingService.deleteByIdCSVImport(shipping_id);
			}
		}

		return orderId;
	}

	public static Map create_dropship_order(String userId, String storeId, String channel, String shippingId,
											String orderNameShopify, String trackingNumber, String orderIdPrefix, String state, String source,
											int addr_verified, String originalId) throws SQLException, ParseException {
		DropshipOrderObj dropshipOrderObj = new DropshipOrderObj();

		dropshipOrderObj.setOrderIdPrefix(orderIdPrefix);
		dropshipOrderObj.setOrderCurrency("USD");
		dropshipOrderObj.setState(state);
		dropshipOrderObj.setShippingId(shippingId);
		dropshipOrderObj.setTrackingNumber(trackingNumber);
		dropshipOrderObj.setNote("");
		dropshipOrderObj.setChannel(channel);
		dropshipOrderObj.setStoreId(storeId);
		dropshipOrderObj.setUserId(userId);
		dropshipOrderObj.setReferenceOrderId(orderNameShopify);
		dropshipOrderObj.setSource(source);
		dropshipOrderObj.setAddrVerified(addr_verified);
		dropshipOrderObj.setOriginalId(originalId);
		dropshipOrderObj.setShippingMethod(AppParams.STANDARD);
		LOGGER.info("dropshipOrderObj=" + dropshipOrderObj.toString());

		Map dropshipOrder = DropshipOrderService.insertDropshipOrder(dropshipOrderObj);
		return dropshipOrder;
	}

	private Map createShiping(JSONObject shipping, JSONObject billing) throws Exception {
		String shipping_name = shipping.getString("last_name") + " " + shipping.getString("first_name");
		String state = shipping.getString("state");
		String country_name = ShippingService.getShipingCountryByState(state);

		Map shippingResult = ShippingService.insert(shipping_name, billing.getString("email"),
				billing.getString("phone"), shipping.getString("address_1"), shipping.getString("address_2"),
				shipping.getString("city"), state, shipping.getString("postcode"), shipping.getString("country"),
				country_name);
		return shippingResult;
	}

	private List<JSONArray> divideProductVariantToOrder(String userId, JSONArray temp) {
		List<JSONArray> result = new LinkedList<JSONArray>();
		JSONArray list_product_camp = new JSONArray();
		JSONArray list_product_sku = new JSONArray();
		for (int i = 0; i < temp.length(); i++) {
			JSONObject obj = temp.getJSONObject(i);
			String lineitem_sku = "";
			LOGGER.info("sku= " + obj.get("sku"));
			if (obj.get("sku") instanceof String) {
				lineitem_sku = obj.getString("sku");
			}
			obj.put("sku", lineitem_sku);
			try {
				String[] lineSku = lineitem_sku.split("\\|");
				if (lineSku.length >= 2) {
					String variantId = lineSku[0];
					Map variantMap = ProductVariantService.get(variantId);
					if (!variantMap.isEmpty()) {
						obj.put("is_order_camp", true);
						list_product_camp.put(obj);
					} else {
						obj.put("is_order_camp", false);
						list_product_sku.put(obj);
					}
				} else {
					obj.put("is_order_camp", false);
					list_product_sku.put(obj);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (list_product_camp != null && list_product_camp.length() > 0) {
			result.add(list_product_camp);
		}
		if (list_product_sku != null && list_product_sku.length() > 0) {
			result.add(list_product_sku);
		}
		return result;
	}

	private Map processOrderItems(JSONArray dropshipOrderProducts, String orderId, String orderReferenceId,
											String shipping_country) throws Exception {
		int orderProductSuccess = 0;
		int orderProductFailed = 0;
		int orderProductTotal;
		double orderTotal = 0.00;
		List<Map> orderItemList = new ArrayList<>();
		int totalItems = 0;
		JSONArray mapItemProduct = dropshipOrderProducts;// mapVariant(dropshipOrderProducts);

		orderProductTotal = mapItemProduct.length();
		double orderShippingTotal = 0.00d;
		double orderSubTotal = 0.00d;
		int addressVerified = 0;
		Double totalTax = 0d;
		
		Set<String> setBaseId = OrderUtil.getSetBaseIdFromJsonObj(mapItemProduct);
		
		Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, shipping_country,  AppParams.STANDARD);

		Map countryTax = CountryTaxService.getTaxByCountry(shipping_country);
		
		for (int i = 0; i < orderProductTotal; i++) {
			JSONObject vObj = mapItemProduct.getJSONObject(i);
			LOGGER.info("processOrderItems()- vObj: " + vObj.toString());
			Map orderItem = createOrderItem(orderId, vObj, shipping_country, orderReferenceId, shippingInfo, countryTax);

			String orderProductId = ParamUtil.getString(orderItem, AppParams.ID);
			if (!StringUtils.isEmpty(orderProductId)) {
				orderProductSuccess++;
				orderItemList.add(orderItem);

				orderTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
				orderSubTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SUBTOTAL));
				orderShippingTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
				totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
				totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));

			} else {
				orderProductFailed++;
			}

		}
		//
		Map orderInfoMap = new LinkedHashMap<>();
		if (orderProductSuccess == orderProductTotal) {
			String addressCheckMessage = "";

			if (addressVerified > 1) {
				addressCheckMessage = DropshipCustomApiOrderCreateHandler.IGNORE_ADDRESS_CHECK_NOTE;
			}

			orderTotal = GetterUtil.format(orderTotal, 2);
			totalTax = GetterUtil.format(totalTax, 2);
			
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

	private JSONArray mapVariant(JSONArray dropshipOrderProducts) {
		JSONArray result = new JSONArray();
		Map<String, String> mapVariantName = new LinkedHashMap<String, String>();
		for (int i = 0; i < dropshipOrderProducts.length(); i++) {
			JSONObject obj = dropshipOrderProducts.getJSONObject(i);
			String lineitem_sku = obj.getString("sku");
			int lineitem_quantity = obj.getInt("quantity");

			if (!mapVariantName.containsKey(lineitem_sku)) {
				for (int j = i + 1; j < dropshipOrderProducts.length(); j++) {
					JSONObject obj1 = dropshipOrderProducts.getJSONObject(j);
					if (lineitem_sku.equalsIgnoreCase(obj1.getString("sku"))) {
						lineitem_quantity += obj1.getInt("quantity");
						obj.put("quantity", lineitem_quantity);
					}
				}
				mapVariantName.put(lineitem_sku, lineitem_sku);
				result.put(obj);
			}
		}

		return result;
	}

	private java.util.Map processOrderSKUItems(String consumerKey, String consumerSecret, String domain, String userId,
											   JSONArray dropshipOrderProducts, String orderId, String orderReferenceId, String shipping_country)
			throws JSONException, Exception {
		int orderProductSuccess = 0;

		int orderProductFailed = 0;
		JSONArray mapItemProduct = dropshipOrderProducts;// mapVariant(dropshipOrderProducts);
		int orderProductTotal = mapItemProduct.length();
		List<java.util.Map> orderItemList = new ArrayList<>();
		int totalItems = 0;
		double orderTotal = 0.00;
		double orderShippingTotal = 0.00d;
		double orderSubTotal = 0.00d;
		int addressVerified = 0;
		for (int i = 0; i < orderProductTotal; i++) {
			JSONObject vObj = mapItemProduct.getJSONObject(i);
			LOGGER.info("processOrderSKUItems()- vObj: " + vObj.toString());
			java.util.Map orderItem = createOrderSKUItem(consumerKey, consumerSecret, domain, userId, orderId, vObj,
					shipping_country, orderReferenceId);
			LOGGER.info("orderItem: " + orderItem);
			String orderProductId = ParamUtil.getString(orderItem, AppParams.ID);
			if (!StringUtils.isEmpty(orderProductId)) {
				orderProductSuccess++;
				orderItemList.add(orderItem);
				orderTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
				orderSubTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SUBTOTAL));
				orderShippingTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
				totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
			} else {
				orderProductFailed++;
			}

		}
		//
		Map orderInfoMap = new LinkedHashMap<>();
		if (orderProductSuccess == orderProductTotal) {
			String addressCheckMessage = "";
			if (addressVerified > 1) {
				addressCheckMessage = DropshipCustomApiOrderCreateHandler.IGNORE_ADDRESS_CHECK_NOTE;
			}
			
			Double totalTax = 0d; //OrderUtil.getTaxByCountry(orderTotal, shipping_country);
			
//			orderTotal = orderTotal + totalTax;
			
			orderTotal = GetterUtil.format(orderTotal, 2);
			
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

	private java.util.Map createOrderItem(String orderId, JSONObject vObj, String shipping_country, String orderReferenceId, Map shippingInfo, Map countryTax)
			throws SQLException {
		LOGGER.info("Woo createOrderItem() vObj=" + vObj);

		String source = ResourceSource.CAMP_SYNC;
		if (vObj.getBoolean("is_order_camp")) {
			source = ResourceSource.CUSTOM_SYNC;
		}

		String[] lineSku = vObj.getString("sku").split("\\|");
		String variantId = lineSku[0];
		String sizeId = lineSku[1];
		Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);


		int quantity = vObj.getInt("quantity");
		Map orderItem = new LinkedHashMap<>();
		if (!variantMap.isEmpty()) {
			String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);
			String campaignId = ParamUtil.getString(variantMap, AppParams.CAMPAIGN_ID);
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

//			Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, shipping_country, quantity);
			Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseId, AppParams.STANDARD, quantity, shippingInfo);
			
			Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);

			double productSubTotal = baseCost * quantity;
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
//					.partnerSku(setPartnerSku)
					.colorName(colorName)
					.sizeName(sizeName)
					.shippingMethod(AppParams.STANDARD)
//					.printDetail(setPrintDetail)
					.itemType(ResourceStates.NORMAL)
//					.partnerProperties(setPartnerProperties)
//					.partnerOption(setPartnerOption)
					.baseShortCode(baseShortCode)
					.designFrontUrl(designFrontUrl)
					.designBackUrl(designBackUrl)
					.taxAmount(taxAmount)
					.build();

			orderItem = DropshipOrderProductService.insertDropshipOrderProduct(orderProductObj);
			orderItem.put(AppParams.SUBTOTAL, productSubTotal);

		}
		return orderItem;
	}

	private java.util.Map createOrderSKUItem(String consumerKey, String consumerSecret, String domain, String userId,
											 String orderId, JSONObject vObj, String shipping_country, String orderReferenceId) throws JSONException, Exception {
		LOGGER.info("Woo createOrderSKUItem(): vObj" + vObj);
		String source = ResourceSource.CAMP_SYNC;
		if (vObj.getBoolean("is_order_camp")) {
			source = ResourceSource.CUSTOM_SYNC;
		}

		int quantity = vObj.getInt("quantity");
		Double shippingFee = 0.00d;
		Double taxAmount = 0.00d;
		double baseCost = 0d;
		double productSubTotal = 0;
		double productAmount = GetterUtil.format(productSubTotal + shippingFee, 2);

		JSONObject partnerOption = getColorAndSizeProductItem(vObj.getJSONArray("meta_data"));
		String product_id = String.valueOf(vObj.get("product_id"));
		String variation_id = String.valueOf(vObj.get("variation_id"));
		LOGGER.info("createOrderSKUItem()- get mockup url");
		String front_mockup = getMockupUrl(consumerKey, consumerSecret, domain, product_id, variation_id);
		String variantName = "";
		if (vObj.has("name") && vObj.get("name") instanceof String) {
			variantName = vObj.getString("name");
		}
		Set<String> optionKey = partnerOption.keySet();
		for (String key : optionKey) {
			if (StringUtils.isNotEmpty(partnerOption.getString(key)))
				variantName += " / " + partnerOption.getString(key);
		}

		DropshipOrderProductObj orderProductObj = new DropshipOrderProductObj.Builder(orderId).build();

		if (partnerOption.has("size") && partnerOption.get("size") instanceof String) {
			orderProductObj.setSizeName(partnerOption.getString("size"));
		}
		if (partnerOption.has("color") && partnerOption.get("color") instanceof String) {
			orderProductObj.setColorName(partnerOption.getString("color"));
		}

		String s_partner_url = MessageFormat.format("{0}/wp-json/wc/v3/products/{1}?consumer_key={2}&consumer_secret={3}", domain, product_id, consumerKey, consumerSecret);
		JsonObject partnerProperties = new JsonObject();
		partnerProperties.put(AppParams.PARTNER_URL, s_partner_url);

		orderProductObj.setPartnerSku(vObj.getString("sku"));
		orderProductObj.setBaseShortCode("");
		orderProductObj.setProductId(product_id);
		orderProductObj.setVariantId(variation_id);
		orderProductObj.setPrice(baseCost);
		orderProductObj.setShippingFee(shippingFee);
		orderProductObj.setCurrency("USD");
		orderProductObj.setQuantity(quantity);
		orderProductObj.setState(ResourceStates.APPROVED);
		orderProductObj.setVariantName(variantName);
		orderProductObj.setAmount(productAmount);
		orderProductObj.setBaseCost(baseCost);
		orderProductObj.setLineItemId(orderReferenceId);
		orderProductObj.setVariantFrontUrl(front_mockup);
		orderProductObj.setVariantFrontUrl(front_mockup);
		orderProductObj.setItemType(ResourceStates.NORMAL);
		orderProductObj.setPartnerOption(partnerOption.toString());
		orderProductObj.setPartnerProperties(partnerProperties.toString());
		orderProductObj.setShippingMethod(AppParams.STANDARD);
		orderProductObj.setTaxAmount(taxAmount);

		LOGGER.info("orderProductObj: " + orderProductObj.toString());
		Map orderItem = DropshipOrderProductService.insertDropshipOrderProduct(orderProductObj);

		orderItem.put(AppParams.SUBTOTAL, productSubTotal);

		return orderItem;
	}

	private String getMockupUrl(String consumerKey, String consumerSecret, String domain, String product_id,
								String variation_id) throws Exception {
		String mockup_url = "";
		String formatUrl = MessageFormat.format(
				"{0}/wp-json/wc/v3/products/{1}/variations/{2}?consumer_key={3}&consumer_secret={4}", domain,
				product_id, variation_id, consumerKey, consumerSecret);
		LOGGER.info("formatUrl= " + formatUrl);
		long start = System.currentTimeMillis();
		HttpResponse<String> response = Unirest.get(formatUrl).asString();

		long elapsedTimeMillis = System.currentTimeMillis() - start;
		float elapsedTimeSec = elapsedTimeMillis / 1000F;

		LOGGER.info("Thoi gian thuc thi:" + elapsedTimeSec);
		if (response.getStatus() != 200 && response.getStatus() != 201) {
			LOGGER.info("data result code:" + response.getStatus());
			// LOGGER.log(Level.SEVERE, "[ERROR]", e);
			mockup_url = getMockupByProduct(consumerKey, consumerSecret, domain, product_id);
		} else {
			JSONObject responseMap = new JSONObject(response.getBody());
			LOGGER.info("responseMap:" + responseMap.toString());
			if (responseMap.has("image") && responseMap.get("image") instanceof JSONObject) {
				JSONObject obj_image = responseMap.getJSONObject("image");
				mockup_url = obj_image.getString("src");
			}
		}
		return mockup_url;
	}

	private String getMockupByProduct(String consumerKey, String consumerSecret, String domain, String product_id)
			throws UnirestException {
		String mockup_url = "";

		try {
			String formatUrlProduct = MessageFormat.format(
					"{0}/wp-json/wc/v3/products/{1}?consumer_key={2}&consumer_secret={3}", domain, product_id,
					consumerKey, consumerSecret);
			LOGGER.info("formatUrlProduct= " + formatUrlProduct);
			long startProduct = System.currentTimeMillis();
			HttpResponse<String> responseProduct = Unirest.get(formatUrlProduct).asString();

			long elapsedTimeMillisProduct = System.currentTimeMillis() - startProduct;
			float elapsedTimeSecProduct = elapsedTimeMillisProduct / 1000F;
			LOGGER.info("Thoi gian thuc thi:" + elapsedTimeSecProduct);

			if (responseProduct.getStatus() != 200 && responseProduct.getStatus() != 201) {
				LOGGER.info("data result code:" + responseProduct.getStatus());
			} else {
				JSONObject responseProductMap = new JSONObject(responseProduct.getBody());
				LOGGER.info("responseProductMap:" + responseProductMap.toString());

				if (responseProductMap.has("images") && responseProductMap.get("images") instanceof JSONArray) {
					JSONObject obj_image = responseProductMap.getJSONArray("images").getJSONObject(0);
					mockup_url = obj_image.getString("src");
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mockup_url;
	}

	private JSONObject getColorAndSizeProductItem(JSONArray meta) throws Exception {
		LOGGER.info("getColorAndSizeProductItem()- meta:" + meta.toString());
		JSONObject obj_result = new JSONObject();
		for (int i = 0; i < meta.length(); i++) {
			JSONObject obj_meta = meta.getJSONObject(i);
			if (obj_meta.get("value") instanceof String) {
				obj_result.put(obj_meta.getString("key"), obj_meta.getString("value"));
			}
		}
		return obj_result;
	}

	private String convertDateFormat(int date) throws Exception {
		String result = "";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(currentDate);
		c.add(Calendar.DATE, -date);
		Date currentDatePlusOne = c.getTime();
		result = dateFormat.format(currentDatePlusOne);
		return result + "T00:00:00Z";
	}

	public static void sendMail(String userId, int total_order_success, String domain, List<Map> orderFail) {
		String userEmail = "";
		try {
			Map mailTemplateSearchResultMap = EmailTemplateService.search("seller_import_ortder",
					ResourceStates.APPROVED, true);
			int totalTemplate = ParamUtil.getInt(mailTemplateSearchResultMap, AppParams.TOTAL);

			if (totalTemplate <= 0) {
				throw new MailException(SystemError.INVALID_MAIL_TEMPLATE);
			}

			List<Map> mailTemplateList = ParamUtil.getListData(mailTemplateSearchResultMap, AppParams.TEMPLATES);

			String subject = ParamUtil.getString(mailTemplateList.get(0), AppParams.SUBJECT);

			String template = ParamUtil.getString(mailTemplateList.get(0), AppParams.CONTENT);

			Map userInfoMap = DropShipStoreCampService.getUserInfo(userId);
			userEmail = ParamUtil.getString(userInfoMap, AppParams.EMAIL);
			String userName = ParamUtil.getString(userInfoMap, AppParams.NAME);
			if (userName.isEmpty()) {
				userName = "there";
			}
			Context mailContext = new Context();
			mailContext.setVariable("userName", userName);
			mailContext.setVariable("total_order", total_order_success);
			if (orderFail != null && orderFail.size() > 0) {
				mailContext.setVariable("orderFail", orderFail);
			}
			TemplateEngine templateEngine = new TemplateEngine();
			String mailContent = templateEngine.process(template, mailContext);

			String content = compress(mailContent);
			EmailObj emailObj = new EmailObj(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY, userEmail, subject, content,
					"pending", "", "image");
			emailObj.setId(null);
			emailObj.setType(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY);
			emailObj.setState(ResourceStates.PENDING);
			emailObj.setReceiver(userEmail);
			emailObj.setSubject(subject);
			emailObj.setContent(content);
			emailObj.setDomain(domain);
			emailObj.setCreate(new Date());

//			LOGGER.info("emailObj=" + emailObj.toString());

			EmailMarketingService.insert(emailObj);
			LOGGER.info("emailObj=" + emailObj.toString());
		} catch (Exception e) {

			LOGGER.log(Level.WARNING, "Exception while sending confirmation email for user: " + userEmail, e);
		}
	}

	public static String compress(String source) {

		HtmlCompressor htmlCompressor = new HtmlCompressor();
		htmlCompressor.setRemoveComments(true);
		htmlCompressor.setRemoveMultiSpaces(true);
		htmlCompressor.setRemoveIntertagSpaces(true);
		htmlCompressor.setSimpleDoctype(true);
		htmlCompressor.setRemoveSurroundingSpaces(HtmlCompressor.ALL_TAGS);

		return htmlCompressor.compress(
				source.replaceAll("<!doctype[^>]*>\\n", "").replaceAll("<html>", "").replaceAll("</html>", ""));
	}
}
