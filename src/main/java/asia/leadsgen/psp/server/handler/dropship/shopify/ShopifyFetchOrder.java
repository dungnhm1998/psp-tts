package asia.leadsgen.psp.server.handler.dropship.shopify;

import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.obj.DropshipOrderProductTypeObj;
import asia.leadsgen.psp.obj.DropshipOrderTypeObj;
import asia.leadsgen.psp.service_fulfill.DropshipOrderServiceV2;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.mashape.unirest.http.Headers;
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
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.OrderUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyFetchOrder extends PSPOrderHandler {
	
	static final String FIELDS = "id,line_items,name,email,landing_site,shipping_address";
	
	public Map getOrderShopify(RoutingContext routingContext, String userId, String storeId, String consumerKey,
			String domain, String channel, String date, String status) throws Exception, UnirestException {
		
		String timezone = DateTimeUtil.formatTimezone(routingContext.request().getParam(AppParams.TIMEZONE));		
		String created_at_min = convertDateFormat(Integer.valueOf(date), timezone);
		String paid = "paid";
		
		Map result = new LinkedHashMap<>();
		String key = userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_SHOPIFY.getValue();
		Map task = RedisService.get(key);
		
		if(task != null && !task.isEmpty()) {
			
			Map mapResult = new LinkedHashMap();
			mapResult.put(AppParams.RESULT_MSG, "A job is in progressing, you must wait for this job to finish.");
			
			result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.SEE_OTHER.code());
			result.put(AppParams.RESPONSE_MSG, "A job is in progressing, you must wait for this job to finish.");
			result.put(AppParams.RESPONSE_DATA, mapResult);	
			
		} else {
			
			int total = getTotalOrder(consumerKey, domain, paid, created_at_min, status);
			LOGGER.info("count= " + total);
			
			if (total > 10) {
				
				Map map = new LinkedHashMap<Integer, String>();
				map.put(AppParams.START_TIME, new Date());
				map.put(AppParams.STORE_ID, storeId);
				map.put(AppParams.STATUS, status);
				map.put(AppParams.DATE, date);
				
				key = userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_SHOPIFY.getValue();
				RedisService.save(key, map);
				Thread one = new Thread() {
					public void run() {
						try {
							int order_per_page = 5;
							int page = 1;
							int total_page = (total / order_per_page) + 1;
							int total_order_success = 0;
							String page_info = "";
							List<String> link = new ArrayList<String>();
							LOGGER.info("total_page=" + total_page);
							while (page <= total_page) {
								if (page == 1) {
									
									LOGGER.info("page=" + page);
									HttpResponse<String> response = getData(consumerKey, domain, created_at_min, status, paid, FIELDS, order_per_page);
																
									Headers header = response.getHeaders();	
									link = header.get("Link");
									
									Map<String, String> query_pairs = new LinkedHashMap<String, String>();
									String[] pairs = link.get(0).split("&");
									for (String pair : pairs) {
								        int idx = pair.indexOf("=");
								        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
								    }

									String page_info_next = query_pairs.get("page_info");
									page_info = page_info_next.substring(0, page_info_next.lastIndexOf(">"));
									LOGGER.info("page_info= " + page_info);
									
									Map mapResult = new JsonObject(response.getBody()).getMap();
									List<Map> orders = ParamUtil.getListData(mapResult, "orders");
									int page_order_success = processData(userId, storeId, consumerKey, domain, channel, date, status, orders);
									total_order_success += page_order_success;
									page++;			
									
								} else {
									
									LOGGER.info("page=" + page);
									LOGGER.info("page_info= " + page_info);
									HttpResponse<String> response = getData(consumerKey, domain, created_at_min, status, paid, FIELDS, order_per_page, page_info);								
									Headers header = response.getHeaders();	
									link = header.get("Link");
									
									Map<String, String> query_pairs = new LinkedHashMap<String, String>();
									String[] pairs = link.get(0).split("&");
									for (String pair : pairs) {
								        int idx = pair.indexOf("=");
								        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
								    }
									
									String page_info_next = query_pairs.get("page_info");
									page_info = page_info_next.substring(0, page_info_next.lastIndexOf(">"));
									LOGGER.info("page_info_next= " + page_info);
									
									Map mapResult = new JsonObject(response.getBody()).getMap();
									List<Map> orders = ParamUtil.getListData(mapResult, "orders");
									int page_order_success = processData(userId, storeId, consumerKey, domain, channel, date, status, orders);
									total_order_success += page_order_success;
									page++;
								}					
							}
							
							// send mail
							sendMail(userId, total_order_success, "", null);
							
							RedisService.delete(userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_SHOPIFY.getValue());
							
						} catch (UnirestException e) {
							e.printStackTrace();
							RedisService.delete(userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_SHOPIFY.getValue());
						} catch (Exception e) {
							e.printStackTrace();
							RedisService.delete(userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_SHOPIFY.getValue());
						}
					}					
				};
				
				one.start();
				
				Map mapResult = new LinkedHashMap();
				mapResult.put(AppParams.RESULT_MSG, "This job is being processed, we will email you when it is completed.");
				
				result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				result.put(AppParams.RESPONSE_MSG, "This job is being processed, we will email you when it is completed.");
				result.put(AppParams.RESPONSE_DATA, mapResult);
				
			} else if (total == 0) {
				
				Map success = new LinkedHashMap();
				String msg = "TOTAL ORDER SYNC SUCCESSFULLY: " + total;
				success.put("result_msg", msg);
				
				result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				result.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				result.put(AppParams.RESPONSE_DATA, success);
				
			} else {
				
				HttpResponse<String> response = getData(consumerKey, domain, created_at_min, status, paid, FIELDS, 10);
				
				Map mapResult = new JsonObject(response.getBody()).getMap();
				List<Map> orders = ParamUtil.getListData(mapResult, "orders");
				int order_success = processData(userId, storeId, consumerKey, domain, channel, date, status, orders);
				LOGGER.info("order_success= " + order_success);
				Map success = new LinkedHashMap();
				String msg = "TOTAL ORDER SYNC SUCCESSFULLY: " + order_success;
				success.put("result_msg", msg);
				
				result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				result.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				result.put(AppParams.RESPONSE_DATA, success);
			}				
		}
		
		return result;
	}
	
	private int getTotalOrder(String consumerKey, String domain, String paid, String created_at_min, String status) throws Exception {
		
		if(status.equalsIgnoreCase("fulfilled")) {
			status = "shipped";
		} else if(status.equalsIgnoreCase("unfulfilled")) {
			status = "unshipped";
		}
		LOGGER.info("status= " + status);
		String url = String.format(ShopifyAPIEndpoints.COUNT_PRODUCT, domain);
		
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fulfillment_status", status)
                .queryString("financial_status", paid)
                .queryString("created_at_min", created_at_min)
				.asString();

		Map mapResult = new JsonObject(response.getBody()).getMap();
		
		int count = ParamUtil.getInt(mapResult, "count");
		
		return count;
	}
	
	private HttpResponse<String> getData(String consumerKey, String domain, String created_at_min,
			String status, String paid, String fields, int order_per_page) throws Exception {
				
		if(status.equalsIgnoreCase("fulfilled")) {
			status = "shipped";
		} else if(status.equalsIgnoreCase("unfulfilled")) {
			status = "unshipped";
		}

		String url = String.format(ShopifyAPIEndpoints.FETCH_ORDER_USING_TOKEN, domain);
		
		long start = System.currentTimeMillis();
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", fields)
                .queryString("fulfillment_status", status)
                .queryString("financial_status", paid)
                .queryString("created_at_min", created_at_min)
                .queryString("limit", order_per_page)
                .asString();
		
		long elapsedTimeMillis = System.currentTimeMillis() - start;
		float elapsedTimeSec = elapsedTimeMillis / 1000F;

		LOGGER.info("Thoi gian thuc thi:" + elapsedTimeSec);
		if (response.getStatus() != 201) {
		    LOGGER.info("data result code:" + response.getStatus());
		}
		
		return response;
	}
	
	private HttpResponse<String> getData(String consumerKey, String domain, String created_at_min,
			String status, String paid, String fields, int order_per_page, String page_info) throws Exception {
			
		if(status.equalsIgnoreCase("fulfilled")) {
			status = "shipped";
		} else if(status.equalsIgnoreCase("unfulfilled")) {
			status = "unshipped";
		}

		String url = String.format(ShopifyAPIEndpoints.FETCH_ORDER_USING_TOKEN, domain);
		
		LOGGER.info("page_info:" + page_info);
		
		long start = System.currentTimeMillis();
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", fields)
                .queryString("limit", order_per_page)
                .queryString("page_info", page_info)
                .asString();
		
		long elapsedTimeMillis = System.currentTimeMillis() - start;
		float elapsedTimeSec = elapsedTimeMillis / 1000F;

		LOGGER.info("Thoi gian thuc thi:" + elapsedTimeSec);
		if (response.getStatus() != 201) {
		    LOGGER.info("data result code:" + response.getStatus());
		}
		
		return response;
	}
	
	private int processData(String userId, String storeId, String consumerKey, String domain, 
			String channel, String date, String status, List<Map> orders) throws InterruptedException {
			
		int success = 0;
		for (Map object : orders) {
			
			initItemGroupQuantity();
			JSONObject mJSONObject = new JSONObject(object);
			LOGGER.info("mJSONObject:" + mJSONObject.toString());
			LOGGER.info("Process Order...");
			int order_success = processOrder(consumerKey, domain, userId, storeId, mJSONObject, channel);
			success += order_success;
			Thread.sleep(2000);
		}
		return success;
	}

	private int processOrder(String consumerKey, String domain, String userId, String storeId, JSONObject obj, String channel) {
		
		int order_success = 0;
		String shipping_id = "";
		String order_id = "";
		String shipping_country = "";
		
		try {
			
			JSONArray line_items = obj.getJSONArray("line_items");
			String id = obj.optString("name", "");
			String email = obj.optString("email", "");
			String landing_site = obj.optString("landing_site", "");
			String partner_url = domain + landing_site;

			Long refId = obj.getLong("id");
			String originalId = Long.toString(refId);
			LOGGER.info("originalId: " + originalId);
			
			List<JSONArray> list_order = divideProductVariantToOrder(userId, line_items);
			JSONObject shipping_address = obj.getJSONObject("shipping_address");		
			LOGGER.info("shipping_address: " + shipping_address.toString());
			if(list_order.size() == 2) {			
				LOGGER.info("Phai tach order");
				for (JSONArray jsonArray : list_order) {				
					try {
						
						String source = ResourceSource.CAMP_SYNC;
						String state = ResourceStates.QUEUED;
						JSONObject firstItem = jsonArray.getJSONObject(0);
						
						if(!firstItem.getBoolean("is_order_camp")) {
							source = ResourceSource.CUSTOM_SYNC;
							state = ResourceStates.DRAFT;
						}
						
						if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId, id, source)) {
							LOGGER.info("order= " + id + "-- storeId=" + storeId + "-- source=" + source +  " is exists");
						} else {
							LOGGER.info("order= " + id + "-- storeId=" + storeId + "-- source=" + source +  " is NOT exists");
							Map map_shipping = createShiping(shipping_address, email);
							shipping_id = ParamUtil.getString(map_shipping, AppParams.ID);
							Map map_address = ParamUtil.getMapData(map_shipping, AppParams.ADDRESS);
							shipping_country = ParamUtil.getString(map_address, AppParams.COUNTRY);
							if(StringUtils.isEmpty(shipping_id)) {
								LOGGER.info("Create shipping fail.");
							} else {
								if(create_dropship_order(originalId, consumerKey, domain, userId, storeId, channel, shipping_id, id, source, state,
										jsonArray, shipping_country, partner_url)) {
									order_success++;
									order_id = "";
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
				}			
			} else {
				
				LOGGER.info("Khong phai tach order");
				String source = ResourceSource.CAMP_SYNC;
				String state = ResourceStates.QUEUED;
				JSONArray list_item = list_order.get(0);
				JSONObject firstItem = list_item.getJSONObject(0);
				
				if(!firstItem.getBoolean("is_order_camp")) {
					source = ResourceSource.CUSTOM_SYNC;
					state = ResourceStates.DRAFT;
				}
				
				if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId, id, source)) {
					ShippingService.deleteByIdCSVImport(shipping_id);
					LOGGER.info("order= " + id + "-- storeId=" + storeId + "-- source=" + source +  " is exists");
				} else {
					LOGGER.info("order= " + id + "-- storeId=" + storeId + "-- source=" + source +  " is NOT exists");
					Map map_shipping = createShiping(shipping_address, email);
					shipping_id = ParamUtil.getString(map_shipping, AppParams.ID);
					Map map_address = ParamUtil.getMapData(map_shipping, AppParams.ADDRESS);
					shipping_country = ParamUtil.getString(map_address, AppParams.COUNTRY);
					if(StringUtils.isEmpty(shipping_id)) {
						LOGGER.info("Create shipping fail.");
					} else {				
						if(create_dropship_order(originalId, consumerKey, domain, userId, storeId, channel, shipping_id, id, source, state,
								list_item, shipping_country, partner_url)) {
							order_success++;
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
		
		return order_success;
	}
	
	private List<JSONArray> divideProductVariantToOrder(String userId, JSONArray line_items) {
		
		List<JSONArray> result = new LinkedList<JSONArray>();
		JSONArray list_product_camp = new JSONArray();
		JSONArray list_product_sku = new JSONArray();
		
		for (int i = 0; i < line_items.length(); i++) {
			JSONObject obj = line_items.getJSONObject(i);
			LOGGER.info("line_item: " + obj.toString());
			String lineitem_sku = obj.optString("sku", "");
			LOGGER.info("sku " + lineitem_sku);
			
			if (lineitem_sku == "" || lineitem_sku.isEmpty()) {
				obj.put("is_order_camp", false);
				list_product_sku.put(obj);
				continue;
			}
			
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

	private boolean create_dropship_order(String originalId, String consumerKey, String domain, String userId, String storeId, String channel, 
			String shipping_id, String id, String source, String state, JSONArray list_item, String shipping_country, String partner_url) 
					throws JSONException, Exception {
		boolean result = false;
		JSONObject firstItem = list_item.getJSONObject(0);
		Map order = new LinkedHashMap();
		String trackingNumber = AppUtil.generateOrderTrackingNumber();
		if(source.equalsIgnoreCase(ResourceSource.CAMP_SYNC)) {
			LOGGER.info("source: " + source);
			String[] lineSku = firstItem.getString("sku").split("\\|");
			if (lineSku.length >= 2) {
				String variantId = lineSku[0];
				String sizeId = lineSku[1];
				Map variantMap = ProductVariantService.getAndCheckCampaignNotLocked(variantId);
				String campaignId = ParamUtil.getString(variantMap, "campaign_id");
				LOGGER.info("campaignId: " + campaignId);
				if (StringUtils.isNotEmpty(userId) && !variantMap.isEmpty() && campaignId.startsWith(userId)) {
					String baseId = ParamUtil.getString(variantMap,
							AppParams.BASE_ID);
					LOGGER.info("baseId: " + baseId);

					if (BaseSizeService.checkAvailabilityForBase(sizeId,
							baseId) == false) {
						LOGGER.info("Invalid lineitem sku");
					} else {

						String productId = ParamUtil.getString(variantMap,
								AppParams.PRODUCT_ID);
						String orderIdPrefix = DropshipOrderImportCVSHandlerV2.createOrderIdPrefix(productId);
						order = create_dropship_order(userId, storeId, channel, shipping_id, id,
								trackingNumber, orderIdPrefix, state, source, 0, originalId);
					}
				}
			}
		} else {
			String orderIdPrefix = userId + "-CT";
			order = create_dropship_order(userId, storeId, channel, shipping_id, id,
					trackingNumber, orderIdPrefix, state, source, 0, originalId);
			
		}

		if(!order.isEmpty()) {
			String orderId = ParamUtil.getString(order,AppParams.ID);
			LOGGER.info("orderId: " + orderId);

			Map countryTax = CountryTaxService.getTaxByCountry(shipping_country);

			Map orderInfoMap = null;
			try {
				if(source.equalsIgnoreCase(ResourceSource.CAMP_SYNC)) {
					orderInfoMap = processOrderItems(list_item, orderId, shipping_country, countryTax);
					LOGGER.info("processOrderItems: " + orderInfoMap.toString());
				} else {				
					orderInfoMap = processOrderSKUItems(consumerKey, domain, userId, list_item, orderId, shipping_country, partner_url);
					LOGGER.info("processOrderSKUItems: " + orderInfoMap.toString());					
				}	
			} catch (Exception e) {
				e.printStackTrace();
				if (StringUtils.isNotEmpty(shipping_id)) {
					ShippingService.deleteByIdCSVImport(shipping_id);
				}
				if (StringUtils.isNotEmpty(orderId)) {
					DropshipOrderService.deleteByIdCSVImport(orderId);
				}
			}
			
			if (orderInfoMap.isEmpty()) {
				DropshipOrderService.deleteByIdCSVImport(orderId);
			} else {
				result = true;
			}
			
		} else {
			if (StringUtils.isNotEmpty(shipping_id)) {
				LOGGER.info("DELETE Shipping");
				ShippingService.deleteByIdCSVImport(shipping_id);
			}
		}
		
		return result;
	}

	public static Map create_dropship_order(String userId, String storeId, String channel, String shippingId,
			String orderNameShopify, String trackingNumber, String orderIdPrefix, String state, String source,
			int addrVerified, String originalId) throws SQLException, ParseException {

		DropshipOrderTypeObj dropshipOrderObj = DropshipOrderTypeObj.builder()
				.idPrefix(orderIdPrefix)
				.currency("USD")
				.state(state)
				.shippingId(shippingId)
				.trackingCode(trackingNumber)
				.channel(channel)
				.storeId(storeId)
				.userId(userId)
				.referenceOrder(orderNameShopify)
				.source(source)
				.addrVerified(addrVerified)
				.originalId(originalId)
				.shippingMethod(AppParams.STANDARD)
				.build();

		LOGGER.info("dropshipOrderObj=" + dropshipOrderObj.toString());

		Map dropshipOrder = DropshipOrderServiceV2.insertDropshipOrderV2(dropshipOrderObj);
		return dropshipOrder;
	}

	private Map processOrderSKUItems(String consumerKey, String domain, String userId,
			JSONArray dropshipOrderProducts, String orderId, String shipping_country, String partner_url) throws JSONException, Exception {
		
		int orderProductSuccess = 0;
		
		int orderProductFailed = 0;
		int orderProductTotal = dropshipOrderProducts.length();
		List<Map> orderItemList = new ArrayList<>();
		int totalItems = 0;
		double orderTotal = 0.00;
		double orderShippingTotal = 0.00d;
		double orderSubTotal = 0.00d;
		int addressVerified = 0;
		Double totalTax = 0d;
		
		for (int i = 0; i < orderProductTotal; i++) {
			JSONObject vObj = dropshipOrderProducts.getJSONObject(i);
			
			Map orderItem = createOrderSKUItem(consumerKey, domain, userId, orderId, vObj, shipping_country, partner_url);
			LOGGER.info("orderItem: " + orderItem);
			String orderProductId = ParamUtil.getString(orderItem, AppParams.ID);
			if (!StringUtils.isEmpty(orderProductId)) {
				orderProductSuccess++;
				orderItemList.add(orderItem);
				orderTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
				orderSubTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SUBTOTAL));
				orderShippingTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
				totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
				totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem,AppParams.TAX_AMOUNT));
				
			} else {
				orderProductFailed++;
			}

		}

		Map orderInfoMap = new LinkedHashMap<>();
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
		
		return orderInfoMap;
	}

	private Map processOrderItems(JSONArray dropshipOrderProducts, String orderId, String shipping_country, Map countryTax) throws SQLException, ParseException, Exception {
		
		int orderProductSuccess = 0;
		int orderProductFailed = 0;
		int orderProductTotal;
		double orderTotal = 0.00;
		List<Map> orderItemList = new ArrayList<>();
		int totalItems = 0;
		orderProductTotal = dropshipOrderProducts.length();
		double orderShippingTotal = 0.00d;
		double orderSubTotal = 0.00d;
		int addressVerified = 0;
		
		Set<String> setBaseId = OrderUtil.getSetBaseIdFromJsonObj(dropshipOrderProducts);
		
		Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, shipping_country,  AppParams.STANDARD);

		for (int i = 0; i < orderProductTotal; i++) {
			JSONObject vObj = dropshipOrderProducts.getJSONObject(i);
			String line_item_id = "";
			if(vObj.has("id") && vObj.get("id") instanceof Long) {
				line_item_id = String.valueOf(vObj.get("id"));
			}
			Map orderItem = createOrderItem(orderId, vObj, shipping_country, line_item_id, shippingInfo, countryTax);
			
			if (orderItem != null && orderItem.isEmpty() == false) {
				String orderProductId = ParamUtil.getString(orderItem, AppParams.ID);
				if (!StringUtils.isEmpty(orderProductId)) {
					orderProductSuccess++;
					orderItemList.add(orderItem);

					orderTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
					orderSubTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SUBTOTAL));
					orderShippingTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
					totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
				}
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
			LOGGER.info("orderItem is Empty -- orderId " + orderId);
			DropshipOrderProductService.deleteByOrderCSVImport(orderId);
		}

		return orderInfoMap;
	}

	private Map createOrderSKUItem(String consumerKey, String domain, String userId,
			String orderId, JSONObject vObj, String shipping_country, String partnerUrl) throws JSONException, Exception {
		
		LOGGER.info("Shopify CreateOrderSKUItem --- vObj= " + vObj.toString());
		String source = ResourceSource.CAMP_SYNC;
		if (vObj.getBoolean("is_order_camp")) {
			source = ResourceSource.CUSTOM_SYNC;
		}
		Map orderItem = new LinkedHashMap<>();
		
		try {
			int quantity = vObj.getInt("quantity");
			Double shippingFee = 0.00d;
			Double taxAmount = 0.00d;
			double sellerBaseCost = 0d;
			double productSubTotal = 0;
			double productAmount = GetterUtil.format(productSubTotal + shippingFee, 2);

			String product_id = "";
			if(vObj.has("product_id") && vObj.get("product_id") instanceof Long) {
				product_id = String.valueOf(vObj.get("product_id"));
			}
			String variant_id = "";
			if(vObj.has("variant_id") && vObj.get("variant_id") instanceof Long) {
				variant_id = String.valueOf(vObj.get("variant_id"));
			}
			String lineItemId = "";
			if(vObj.has("id") && vObj.get("id") instanceof Long) {
				lineItemId = String.valueOf(vObj.get("id"));
			}
			String variantName = String.valueOf(vObj.get("name"));
			
			if( StringUtils.isNotEmpty(product_id) && StringUtils.isNotEmpty(variant_id)) {
				
				Map variantObj = getColorAndSizeProductItem(consumerKey, domain, product_id, variant_id);

				String partnerSku = String.valueOf(variantObj.get("sku"));
				String option1 = String.valueOf(variantObj.get("option1"));
				String option2 = String.valueOf(variantObj.get("option2"));
				String option3 = String.valueOf(variantObj.get("option3"));

				List<String> optionsString = new ArrayList<String>();
				optionsString.add(option1);
				optionsString.add(option2);
				optionsString.add(option3);

				String partnerSize = "";
				String partnerColor = "";
				String partnerName = "";

				Map optionsMap = getOptionProduct(consumerKey, domain, product_id);
				for (int i = 0; i < optionsMap.size(); i++) {
					LOGGER.info("optionsMap= " + optionsMap.get(i+1).toString());
					if (optionsMap.get(i+1).equals("Size")) {
						partnerSize = optionsString.get(i);
					} else if (optionsMap.get(i+1).equals("Color")) {
						partnerColor = optionsString.get(i);
					} else {
						partnerName = optionsString.get(i);
					}
				}

				String variantFrontUrl = "";
				String image_id = String.valueOf(variantObj.get("image_id"));		
				LOGGER.info("image_id= " + image_id);
				
				if(image_id != "null") {
					LOGGER.info("get variant image...");
					variantFrontUrl = getMockupUrl(consumerKey, domain, product_id, image_id);
				} else {
					LOGGER.info("get product image...");
					variantFrontUrl = getProductImgUrl(consumerKey, domain, product_id);
				}

				JSONObject partnerOption = new JSONObject(variantObj);

				JsonObject partnerProperties = new JsonObject();
				partnerProperties.put(AppParams.PARTNER_URL, partnerUrl);

				DropshipOrderProductTypeObj orderProductObj = DropshipOrderProductTypeObj.builder()
						.orderId(orderId)
						.price(String.valueOf(sellerBaseCost))
						.shippingFee(String.valueOf(shippingFee))
						.currency("USD")
						.quantity(quantity)
						.state(ResourceStates.APPROVED)
						.variantName(variantName)
						.amount(String.valueOf(productAmount))
						.baseCost(String.valueOf(sellerBaseCost))
						.lineItemId(lineItemId)
						.variantFrontUrl(variantFrontUrl)
						.partnerSku(partnerSku)
						.itemType(ResourceStates.NORMAL)
						.partnerProperties(partnerProperties.toString())
						.partnerOption(partnerOption.toString())
						.shippingMethod(AppParams.STANDARD)
						.taxAmount(String.valueOf(taxAmount))
						.sizeName(partnerSize)
						.colorName(partnerColor)
						.build();

				orderItem = DropshipOrderProductService.insertDropshipOrderProductV2(orderProductObj);

				if (orderItem != null && orderItem.isEmpty() == false) {
					orderItem.put(AppParams.SUBTOTAL, productSubTotal);
				}			
				
			} else {
				LOGGER.info("khong co thong tin variant");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return orderItem;
	}

	private Map createOrderItem(String orderId, JSONObject vObj, String shipping_country, String line_item_id, Map shippingInfo, Map countryTax) throws SQLException {
		
		LOGGER.info("Shopify CreateOrderItem --- vObj= " + vObj.toString());
		String source = ResourceSource.CAMP_SYNC;
		if (vObj.getBoolean("is_order_camp")) {
			source = ResourceSource.CUSTOM_SYNC;
		}
		LOGGER.info("source: " +source);
		
		
		String[] lineSku = vObj.getString("sku").split("\\|");
		String variantId = lineSku[0];
		LOGGER.info("variantId: " +variantId);
		String sizeId = lineSku[1];
		Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);


		Map orderItem = new LinkedHashMap<>();
		if (!variantMap.isEmpty()) {
			String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
			double baseCost = ParamUtil.getDouble(variantMap, AppParams.BASE_COST);
			String baseShortCode = ParamUtil.getString(variantMap, AppParams.BASE_SHORT_CODE);

			String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);
			String campaignId = ParamUtil.getString(variantMap, AppParams.CAMPAIGN_ID);
			String variantName = ParamUtil.getString(variantMap, AppParams.NAME);

			String colorId = ParamUtil.getString(variantMap, AppParams.COLOR_ID);
			String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
			String colorValue = ParamUtil.getString(variantMap, AppParams.COLOR);
			String sizeName = ParamUtil.getString(variantMap, AppParams.SIZE_NAME);

			Map image = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
			String variantFrontUrl = ParamUtil.getString(image, AppParams.FRONT);
			String variantBackUrl = ParamUtil.getString(image, AppParams.BACK);

			String designFrontUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_FRONT_URL);
			String designBackUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_BACK_URL);

			int quantity = vObj.getInt("quantity");

//			Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, shipping_country, quantity);
			Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseId, AppParams.STANDARD, quantity, shippingInfo);
			
			Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);

			double productSubTotal = baseCost * quantity;
			double productAmount = GetterUtil.format(productSubTotal + shippingFee, 2);
			LOGGER.info("+++productAmount = " + productAmount);
			Double taxRate=OrderUtil.getTaxRateFromCountryTax(countryTax);
			Double taxAmount = OrderUtil.getTaxByAmountAndByCountry(productAmount,countryTax);
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
					.currency("USD")
					.quantity(quantity)
					.state(ResourceStates.APPROVED)
					.variantName(variantName)
					.amount(String.valueOf(productAmount))
					.baseCost(String.valueOf(baseCost))
					.baseId(baseId)
					.lineItemId(line_item_id)
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
					.designFrontUrl(designFrontUrl)
					.designBackUrl(designBackUrl)
					.taxAmount(String.valueOf(taxAmount))
					.taxRate(String.valueOf(taxRate))
					.build();

			orderItem = DropshipOrderProductService.insertDropshipOrderProductV2(orderProductObj);
			
			orderItem.put(AppParams.SUBTOTAL, productSubTotal);

		}
		return orderItem;
	}
	
	private Map getColorAndSizeProductItem(String consumerKey, String domain, String product_id,
			String variant_id) throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.GET_PRODUCT_VARIANT, domain, product_id, variant_id);
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
                .asString();
		
		Map mapResult = new JsonObject(response.getBody()).getMap();
        Map variantMap = ParamUtil.getMapData(mapResult, "variant");
		
		return variantMap;
	}
	
	private Map getOptionProduct(String consumerKey, String domain, String product_id) throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.GET_PRODUCT_ONE, domain, product_id);
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
                .asString();
		
		Map mapResult = new JsonObject(response.getBody()).getMap();
		Map productMap = ParamUtil.getMapData(mapResult, "product");
		List<Map> optionList = ParamUtil.getListData(productMap, "options");

		Map optionsMap = new LinkedHashMap<>();
		for (Map option : optionList) {
			optionsMap.put(option.get("position"), option.get("name"));
		}
		
		return optionsMap;		
	}
	
	private String getMockupUrl(String consumerKey, String domain, String product_id, String image_id) throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.GET_PRODUCT_IMAGE, domain, product_id, image_id);
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
                .asString();
		LOGGER.info("product_id= " + product_id + " - image_id= " + image_id);
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

	private String convertDateFormat(int date, String timezone) throws Exception{
    	String result = "";
    	
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");  	
    	dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
    	
    	Date currentDate = new Date();
    	Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.DATE, -date);
        
        Date fetchSinceDate = c.getTime();
        result = dateFormat.format(fetchSinceDate);
    	return result;
    }
	
	public static void sendMail(String userId, int total_order_success, String domain, List<Map> orderFail) {
		String userEmail = "";
		try {
			Map mailTemplateSearchResultMap = EmailTemplateService.search("seller_import_ortder", ResourceStates.APPROVED, true);
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
			if(orderFail != null && orderFail.size() > 0) {
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
			
			LOGGER.info("emailObj=" + emailObj.toString());
			EmailMarketingService.insert(emailObj);

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

	private static final Logger LOGGER = Logger.getLogger(ShopifyFetchOrder.class.getName());
	
}
