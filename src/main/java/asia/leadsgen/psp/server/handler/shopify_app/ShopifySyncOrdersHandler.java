package asia.leadsgen.psp.server.handler.shopify_app;

import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
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
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.exception.MailException;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.EmailObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipCustomApiOrderCreateHandler;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service.EmailMarketingService;
import asia.leadsgen.psp.service.EmailTemplateService;
import asia.leadsgen.psp.service.RedisService;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreCampService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.BasePhoneCaseUtil;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.OrderUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifySyncOrdersHandler extends PSPOrderHandler implements Handler<RoutingContext> {
	
	static final String FIELDS = "id,line_items,name,email,landing_site,shipping_address,refunds";
	static final String PAID = "paid";

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			LOGGER.info("userId= " + userId);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
			String storeId = routingContext.request().params().get(AppParams.STORE_ID);
    		LOGGER.info("storeId= " + storeId);
    		if (StringUtils.isEmpty(storeId)) {
				throw new LoginException(SystemError.INVALID_DROPSHIP_STORE_ID);
			}
    		
    		Map storeMap = null;
			try {
				storeMap = DropShipStoreService.lookUp(storeId);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}      	
     		String storeUserId = ParamUtil.getString(storeMap, AppParams.USER_ID);
     		LOGGER.info("storeUserId= " + storeUserId);
			if (!storeUserId.equalsIgnoreCase(userId)) {
				throw new LoginException(SystemError.INVALID_USER);
			}
         			
			try {							
				          
                String date = routingContext.request().params().get(AppParams.DATE);
        		String status = routingContext.request().params().get(AppParams.STATUS);
             	
             	String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
				String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
				String channel = ParamUtil.getString(storeMap, AppParams.CHANNEL);
				if(!channel.equalsIgnoreCase("shopify")) {
					throw new BadRequestException(SystemError.INVALID_STORE);
				}
				
				String timezone = DateTimeUtil.formatTimezone(routingContext.request().getParam(AppParams.TIMEZONE));		
				String created_at_min = convertDateFormat(Integer.valueOf(date), timezone);
				
				Map result = new LinkedHashMap<>();
				final String key = userId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_ORDER_SHOPIFY_APP.getValue();
				Map task = RedisService.get(key);
				
				if (task != null && !task.isEmpty()) {
					
					Map mapResult = new LinkedHashMap();
					mapResult.put(AppParams.RESULT_MSG, "A job is in progressing, you must wait for this job to finish.");
					
					result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.SEE_OTHER.code());
					result.put(AppParams.RESPONSE_MSG, "A job is in progressing, you must wait for this job to finish.");
					result.put(AppParams.RESPONSE_DATA, mapResult);	
					
				} else {
					
					int total = getTotalOrder(consumerKey, domain, created_at_min, status);
					LOGGER.info("count= " + total);
					
					if (total > 10) {
						
						Map map = new LinkedHashMap<Integer, String>();
						map.put(AppParams.START_TIME, new Date());
						map.put(AppParams.STORE_ID, storeId);
						map.put(AppParams.STATUS, status);
						map.put(AppParams.DATE, date);
						
						LOGGER.info("save key: " + key);
						RedisService.save(key, map);
						Thread one = new Thread() {
							public void run() {
								try {
									
									int order_per_page = 10;
									int page = 1;
									int total_page = (total / order_per_page) + 1;
									int total_order_success = 0;
									String page_info = "";
									List<String> link = new ArrayList<String>();
									LOGGER.info("total_page=" + total_page);
									while (page <= total_page) {
										if (page == 1) {
											
											LOGGER.info("page=" + page);
											HttpResponse<String> response = getData(consumerKey, domain, created_at_min, status, order_per_page);
																		
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
											
											Map mapResult = new JsonObject(response.getBody()).getMap();
											List<Map> orders = ParamUtil.getListData(mapResult, "orders");
											int page_order_success = processData(userId, storeId, consumerKey, domain, channel, date, status, orders);
											total_order_success += page_order_success;
											page++;
											
										} else {
											
											LOGGER.info("page=" + page);
											HttpResponse<String> response = getData(consumerKey, domain, created_at_min, status, order_per_page, page_info);								
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
											
											Map mapResult = new JsonObject(response.getBody()).getMap();
											List<Map> orders = ParamUtil.getListData(mapResult, "orders");
											int page_order_success = processData(userId, storeId, consumerKey, domain, channel, date, status, orders);
											total_order_success += page_order_success;
											page++;
										}
									}
									
									// send mail
									sendMail(userId, total_order_success, domain, null);
									
									LOGGER.info("delete key: " + key);
									RedisService.delete(key);
									
								} catch (UnirestException e) {
									e.printStackTrace();
									LOGGER.info("delete key: " + key);
									RedisService.delete(key);
								} catch (Exception e) {
									e.printStackTrace();
									LOGGER.info("delete key: " + key);
									RedisService.delete(key);
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
						String msg = "ORDER SYNC SUCCESSFULLY: " + total;
						success.put("result_msg", msg);
						
						result.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
						result.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
						result.put(AppParams.RESPONSE_DATA, success);
						
					} else {
						
						HttpResponse<String> response = getData(consumerKey, domain, created_at_min, status, 10);
						
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
				
				if(!result.isEmpty()) {
                	routingContext.put(AppParams.RESPONSE_CODE, result.get(AppParams.RESPONSE_CODE));
	                routingContext.put(AppParams.RESPONSE_MSG, result.get(AppParams.RESPONSE_MSG));
	                routingContext.put(AppParams.RESPONSE_DATA, result.get(AppParams.RESPONSE_DATA));
                } else {
	                routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
	                routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
	                routingContext.put(AppParams.RESPONSE_DATA, new LinkedHashMap<>());
                }
				future.complete();
						
			} catch (Exception e) {
            	LOGGER.severe(e.getMessage());
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
	
	private int getTotalOrder(String consumerKey, String domain, String created_at_min, String status) throws Exception {
		
		if(status.equalsIgnoreCase("fulfilled")) {
			status = "shipped";
		} else if(status.equalsIgnoreCase("unfulfilled")) {
			status = "unshipped";
		}
		LOGGER.info("status= " + status);
		String url = String.format(ShopifyAPIEndpoints.COUNT_PRODUCT, domain);
		
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fulfillment_status", status)
                .queryString("financial_status", PAID)
                .queryString("created_at_min", created_at_min)
				.asString();

		Map mapResult = new JsonObject(response.getBody()).getMap();
		
		int count = ParamUtil.getInt(mapResult, "count");
		
		return count;
	}
	
	private HttpResponse<String> getData(String consumerKey, String domain, String created_at_min, String status, int order_per_page) 
			throws Exception {
				
		if(status.equalsIgnoreCase("fulfilled")) {
			status = "shipped";
		} else if(status.equalsIgnoreCase("unfulfilled")) {
			status = "unshipped";
		}

		String url = String.format(ShopifyAPIEndpoints.FETCH_ORDER_USING_TOKEN, domain);
		
		long start = System.currentTimeMillis();
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", FIELDS)
                .queryString("fulfillment_status", status)
                .queryString("financial_status", PAID)
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
	
	private HttpResponse<String> getData(String consumerKey, String domain, String created_at_min, String status, int order_per_page, String page_info) 
			throws Exception {
			
		if(status.equalsIgnoreCase("fulfilled")) {
			status = "shipped";
		} else if(status.equalsIgnoreCase("unfulfilled")) {
			status = "unshipped";
		}

		String url = String.format(ShopifyAPIEndpoints.FETCH_ORDER_USING_TOKEN, domain);
		
		LOGGER.info("page_info:" + page_info);
		
		long start = System.currentTimeMillis();
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", FIELDS)
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
	
	private int processData(String userId, String storeId, String consumerKey, String domain, String channel, String date, String status, List<Map> orders) 
			throws InterruptedException {
			
		int success = 0;
		for (Map order : orders) {
			
			initItemGroupQuantity();
			JSONObject mJSONObject = new JSONObject(order);
			LOGGER.info("mJSONObject:" + mJSONObject.toString());

			LOGGER.info("Process Order...");
			int order_success = processOrder(consumerKey, domain, userId, storeId, mJSONObject.toString(), channel);
			success += order_success;
			Thread.sleep(2000);
		}
		return success;
	}
	
	private JSONArray processLineItemsAndRefunds(JSONArray line_items, JSONArray refunds) {
		JSONArray result = new JSONArray();
		LOGGER.info("refunds.length()= " + refunds.length());
		for (int i = 0; i < line_items.length(); i++) {
			JSONObject obj = line_items.getJSONObject(i);
			Long id = obj.getLong("id");
			int total_quantity = obj.getInt("quantity");
			boolean is_add_item = true;
			int total_quantity_refund = 0;
			for (int j = 0; j < refunds.length(); j++) {
				JSONArray refund_line_items = refunds.getJSONObject(j).getJSONArray("refund_line_items");
				for (int k = 0; k < refund_line_items.length(); k++) {
					JSONObject obj_refund = refund_line_items.getJSONObject(k);
					Long id_refund = obj_refund.getLong("line_item_id");
					if(id.toString().equals(id_refund.toString())) {
						total_quantity_refund += obj_refund.getInt("quantity");
					}
				}
				
			}
			int quantity = total_quantity - total_quantity_refund;
			if( quantity <= 0) {
				is_add_item = false;
			} else {
				obj.put("quantity", quantity);
			}
			
			if(is_add_item) {
				result.put(obj);
			}
		}
		
		return result;
	}
	
	private int processOrder(String consumerKey, String domain, String userId, String storeId, String body_string, String channel) {
		
		int order_success = 0;
		String shipping_id = "";
		String order_id = "";
		String shipping_country = "";
		
		try {
			
			JSONObject obj = new JSONObject(body_string);		
			JSONArray line_items = obj.getJSONArray("line_items");
			try {
				JSONArray refunds = obj.getJSONArray("refunds");
				if(refunds != null && refunds.length() > 0) {
					line_items = processLineItemsAndRefunds(obj.getJSONArray("line_items"), refunds);
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
			
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
			LOGGER.info("isAllItemVariantMap= " + isAllItemVariantMap);
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
						order_success++;
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
//					order_success--;
//				}
//			} catch (SQLException e1) {
//				e1.printStackTrace();
//			}
		}
		
		return order_success;
		
	}
	
	private JSONArray checkVariantMap(String userId, String store_id, JSONArray line_items) {
		
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

		DropshipOrderTypeObj dropshipOrderObj = DropshipOrderTypeObj.builder()
				.idPrefix(orderIdPrefix)
				.currency("USD")
				.state(state)
				.shippingId(shippingId)
				.trackingCode(trackingNumber)
				.channel(channel)
				.storeId(storeId)
				.userId(userId)
				.referenceOrder(referenceOrderId)
				.source(source)
				.minifiedJson(body_string)
				.originalId(originalId)
				.addrVerified(0)
				.build();
		LOGGER.info("dropshipOrderObj=" + dropshipOrderObj.toString());

		order = DropshipOrderServiceV2.insertDropshipOrderV2(dropshipOrderObj);

		if (!order.isEmpty()) {
			
			String orderId = ParamUtil.getString(order, AppParams.ID);
			LOGGER.info("orderId: " + orderId);
			Map orderInfoMap = null;
			
			try {
				orderInfoMap = processOrderSKUItems(consumerKey, domain, userId, list_item, orderId,
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
			String orderId, String shipping_country, String partner_url)
			throws JSONException, Exception {

		int orderProductSuccess = 0;

		int orderProductTotal = dropshipOrderProducts.length();
		List<Map> orderItemList = new ArrayList<>();
		int totalItems = 0;
		double orderTotal = 0.00;
		double orderShippingTotal = 0.00d;
		double orderSubTotal = 0.00d;
		int addressVerified = 0;
		Double totalTax = 0d;
		
		Set<String> setBaseId = OrderUtil.getSetBaseIdFromJsonObj(dropshipOrderProducts);
		
		Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, shipping_country,  AppParams.STANDARD);

		Map countryTax = CountryTaxService.getTaxByCountry(shipping_country);
				
		for (int i = 0; i < orderProductTotal; i++) {
			
			try {
				
				JSONObject vObj = dropshipOrderProducts.getJSONObject(i);

				Map orderItem = createOrderSKUItem(consumerKey, domain, userId, orderId, vObj, shipping_country, partner_url, shippingInfo, countryTax);
				LOGGER.info("orderItem: " + orderItem);
				String orderProductId = ParamUtil.getString(orderItem, AppParams.ID);
				if (!StringUtils.isEmpty(orderProductId)) {
					orderProductSuccess++;
					orderItemList.add(orderItem);
					orderTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
					orderSubTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SUBTOTAL));
					orderShippingTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
					totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
					totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));
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
	
	private Map createOrderSKUItem(String consumerKey, String domain, String userId, String orderId, JSONObject vObj,
			String shipping_country, String partner_url, Map shippingInfo, Map countryTax) throws JSONException, Exception {

		LOGGER.info("dropshipOrderProduct= " + vObj.toString());
		
		String productId = "";
		String variantId = "";
		String bgp_product_id = "";
		String bgp_variant_id = "";
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
		String lineItemId ="";
		if(vObj.has("id") && vObj.get("id") instanceof Long) {
			lineItemId = String.valueOf(vObj.get("id"));
		}
		String partnerSize = "";
		String partnerColor = "";
		String partnerSku = "";
		String partnerUrl = "";
		
		int quantity = vObj.getInt("quantity");
		Double shippingFee = 0.00d;
		Double taxRate=0d;
		Double taxAmount = 0.00d;
		double productSubTotal = 0;
		double productAmount = 0;
		double baseCost = 0d;
		
		boolean is_map = vObj.getBoolean("is_map");
		
		if (is_map) {
			bgp_product_id = vObj.getString("bgp_product_id");
			bgp_variant_id = vObj.getString("bgp_variant_id");
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

//			Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, shipping_country, quantity);
			Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseId, AppParams.STANDARD, quantity, shippingInfo);
			
			shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);
//			taxAmount = ParamUtil.getDouble(feeMap, AppParams.TAX_AMOUNT);
//			productSubTotal = baseCost * quantity;
//			productAmount = GetterUtil.format(productSubTotal + shippingFee + taxAmount, 2);
//			productSubTotal = GetterUtil.format(productSubTotal, 2);

			productAmount = GetterUtil.format(baseCost * quantity + shippingFee, 2);
			LOGGER.info("+++productAmount = " + productAmount);
			taxRate=OrderUtil.getTaxRateFromCountryTax(countryTax);
			taxAmount = OrderUtil.getTaxByAmountAndByCountry(productAmount,countryTax);
			productAmount = GetterUtil.format(productAmount + taxAmount, 2);
			LOGGER.info("+++taxAmount = " + taxAmount + ", taxRate = " + taxRate);
			
		} else {
			
			if(vObj.has("variant_id") && vObj.get("variant_id") instanceof Long) {
				variantId = String.valueOf(vObj.get("variant_id"));
			}		
			if(vObj.has("product_id") && vObj.get("product_id") instanceof Long) {
				productId = String.valueOf(vObj.get("product_id"));
			}
			
			if (!StringUtils.isEmpty(variantId) && !StringUtils.isEmpty(productId)) {
				
				Map variantObj = getColorAndSizeProductItem(consumerKey, domain, productId, variantId);
				partnerSku = String.valueOf(variantObj.get("sku"));
				String option1 = String.valueOf(variantObj.get("option1"));
				String option2 = String.valueOf(variantObj.get("option2"));
				String option3 = String.valueOf(variantObj.get("option3"));

				List<String> optionsString = new ArrayList<String>();
				optionsString.add(option1);
				optionsString.add(option2);
				optionsString.add(option3);

				Map optionsMap = getOptionProduct(consumerKey, domain, productId);
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
					variantFrontUrl = getMockupUrl(consumerKey, domain, productId, image_id);
				} else {
					LOGGER.info("get product image...");
					variantFrontUrl = getProductImgUrl(consumerKey, domain, productId);
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

		DropshipOrderProductTypeObj orderProductObj = DropshipOrderProductTypeObj.builder()
				.orderId(orderId)
				.campaignId(campaignId)
				.productId(bgp_product_id)
				.variantId(bgp_variant_id)
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
				.lineItemId(lineItemId)
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
				.taxAmount(String.valueOf(taxAmount))
				.taxRate(String.valueOf(taxRate))
				.build();

		LOGGER.info("orderProductObj: " + orderProductObj.toString());
		Map orderItem = DropshipOrderProductService.insertDropshipOrderProductV2(orderProductObj);
		
		if (orderItem != null && orderItem.isEmpty() == false) {
			orderItem.put(AppParams.SUBTOTAL, productSubTotal);

//			if (bgp_variant_id != null && bgp_variant_id.isEmpty() == false) {
//				ShopifyAppService.orderProductUpdateThumbUrl(bgp_variant_id, orderId);
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
	
	private static final Logger LOGGER = Logger.getLogger(ShopifySyncOrdersHandler.class.getName());

}
