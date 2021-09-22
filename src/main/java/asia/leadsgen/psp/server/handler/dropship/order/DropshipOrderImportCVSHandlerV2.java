package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.service_fulfill.BaseService;
import org.thymeleaf.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import asia.leadsgen.psp.data.type.RedisKeyEnum;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.external.api.ISPApiConnector;
import asia.leadsgen.psp.obj.BaseSKUObj;
import asia.leadsgen.psp.obj.DropshipBaseSkuObj;
import asia.leadsgen.psp.obj.DropshipOrderCSVObj;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.dropship.shopify.WooEcommerceFetchOrder;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.ProductService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.RedisService;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.BaseSKUService;
import asia.leadsgen.psp.service_fulfill.BaseSizeService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipBaseSkuService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.Common;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.IsoUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 *
 * @author ToanNN 25/12/2019.
 */
public class DropshipOrderImportCVSHandlerV2 extends PSPOrderHandler implements Handler<RoutingContext> {
	private int total_order_success = 0;
	@Override
	public void handle(RoutingContext routingContext) {
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
//		String userId = "A1955";
		routingContext.vertx().executeBlocking(future -> {

			String storeId = routingContext.request().getParam("id");
			String channel = "";
			if (!StringUtils.isEmpty(storeId)) {
				try {
					Map storeResult = DropShipStoreService.getStoreApprovedAndDisconnectedById(storeId);
					if (storeResult.isEmpty()) {
						throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
					}
					channel = ParamUtil.getString(storeResult, AppParams.CHANNEL);

				} catch (SQLException e) {
					e.printStackTrace();
				}

			}
//			channel = "0123456789";
			if (StringUtils.isEmpty(storeId) || StringUtils.isEmpty(channel)) {

				List<Map> lstResult = new ArrayList<>();
				Map map = new LinkedHashMap<>();
				map.put("name", "");
				map.put("type", ResourceStates.FAIL);
				map.put("msg", "Invalid store");
				map.put("source", "Invalid store");
				lstResult.add(map);
				Map mapResult = new LinkedHashMap();
				mapResult.put(AppParams.RESULT_DATA, lstResult);
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, mapResult);
				future.complete();
			} else {
				try {
					Map result = new LinkedHashMap<>();
					String key = userId + "_" + RedisKeyEnum.TASK_PROCESS_IMPORT_ORDER_CSV.getValue();
					Map task = RedisService.get(key);
					if(task != null && !task.isEmpty()) {
						Map mapResult = new LinkedHashMap();
						mapResult.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
						mapResult.put(AppParams.RESPONSE_MSG, "A job is in progressing, you must wait for this job to finish.");

						routingContext.put(AppParams.RESULT_CODE, HttpResponseStatus.SEE_OTHER.code());
						routingContext.put(AppParams.RESULT_MSG, "A job is in progressing, you must wait for this job to finish.");
						routingContext.put(AppParams.RESPONSE_DATA, mapResult);
					} else {
						JsonArray temp = routingContext.getBodyAsJsonArray();
						JsonArray requestBodyArray = mapOrderAndValidOrder(temp);
						LOGGER.info("requestBodyArray:" + requestBodyArray.toString());
						int totalItem = 0;
						for (int i = 0; i < requestBodyArray.size(); i++) {
							ArrayList<DropshipOrderCSVObj> dropshipOrderProducts = new Gson().fromJson(
									requestBodyArray.getJsonArray(i).toString(),
									new TypeToken<List<DropshipOrderCSVObj>>() {
									}.getType());
							totalItem += dropshipOrderProducts.size();
						}
						int orderTotal = requestBodyArray.size();
						LOGGER.info("totalItem= " + totalItem);
						if(totalItem > 10) {
							key = userId + "_" + RedisKeyEnum.TASK_PROCESS_IMPORT_ORDER_CSV.getValue();
							Map map = new LinkedHashMap<Integer, String>();
							map.put(AppParams.START_TIME, new Date());
							map.put(AppParams.STORE_ID, storeId);
							map.put(AppParams.DATA, routingContext.getBodyAsString());

							RedisService.save(key, map);
							Thread one = new Thread() {
							    public void run() {

									try {
										Map storeResult = DropShipStoreService.getStoreApprovedAndDisconnectedById(storeId);
										String domain = ParamUtil.getString(storeResult, AppParams.DOMAIN);
										List<Map> lstResult = processData(requestBodyArray, userId, storeId, ParamUtil.getString(storeResult, AppParams.CHANNEL));
										//gui mail cho kh

										List<Map> orderFail = new ArrayList<>();
										lstResult.forEach(order -> {
											if(ParamUtil.getString(order, "type") == ResourceStates.FAIL) {
												Map itemInfo = new LinkedHashMap<>();
												itemInfo.put("name", ParamUtil.getString(order, "name"));
												itemInfo.put("msg", ParamUtil.getString(order, "msg"));
												orderFail.add(itemInfo);
											}
										});
										WooEcommerceFetchOrder.sendMail(userId, total_order_success, "", orderFail);
										RedisService.delete(userId + "_" + RedisKeyEnum.TASK_PROCESS_IMPORT_ORDER_CSV.getValue());

										LOGGER.info("lstResult:" + lstResult);
									} catch (Exception e) {
										RedisService.delete(userId + "_" + RedisKeyEnum.TASK_PROCESS_IMPORT_ORDER_CSV.getValue());
										e.printStackTrace();
									}

							    }
							};

							one.start();
							//tra ket qua ve
							Map mapResult = new LinkedHashMap();
							mapResult.put(AppParams.RESULT_CODE, HttpResponseStatus.OK.code());
							mapResult.put(AppParams.RESULT_MSG, "This job is being processed, we will email you when it is completed.");

							routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
							routingContext.put(AppParams.RESPONSE_MSG, "This job is being processed, we will email you when it is completed.");
							routingContext.put(AppParams.RESPONSE_DATA, mapResult);

						} else {
							List<Map> lstResult = processData(requestBodyArray, userId, storeId, channel);
							Map mapResult = new LinkedHashMap();
							mapResult.put(AppParams.RESULT_DATA, lstResult);

//							mapResult.put(AppParams.RESULT_CODE, HttpResponseStatus.OK.code());
//							mapResult.put(AppParams.RESULT_MSG, HttpResponseStatus.CREATED.reasonPhrase());

							routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
							routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
							routingContext.put(AppParams.RESPONSE_DATA, mapResult);
						}
					}
					future.complete();
				} catch (Exception e) {
					routingContext.fail(e);
				}
			}

		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	private List<Map> processData(JsonArray requestBodyArray, String userId, String storeId, String channel)
			throws SQLException {
		LOGGER.info("orderTotal:" + requestBodyArray.size());
		List<Map> lstResult = new ArrayList<>();
		this.total_order_success = 0;
		for (int i = 0; i < requestBodyArray.size(); i++) {
			LOGGER.info("orderTotal:" + requestBodyArray.size() + "/" + i);
			initItemGroupQuantity();
			ArrayList<DropshipOrderCSVObj> dropshipOrderProducts = new Gson().fromJson(
					requestBodyArray.getJsonArray(i).toString(),
					new TypeToken<List<DropshipOrderCSVObj>>() {
					}.getType());

			DropshipOrderCSVObj orderProductFisrtItem = dropshipOrderProducts.get(0);
			if (orderProductFisrtItem != null) {
				String orderId = "";
				String shippingId = "";
				try {
					String orderNameShopify = orderProductFisrtItem.getName();
					String source = ResourceSource.CAMP_IMPORT;
					if (orderProductFisrtItem.isIs_order_sku()) {
						source = ResourceSource.CUSTOM_IMPORT;
					}

					if(!IsoUtil.isValidISOCountry(orderProductFisrtItem.getShipping_country())) {
						Map map = new LinkedHashMap<>();
						map.put("name", orderNameShopify);
						map.put("type", ResourceStates.FAIL);
						map.put("msg", "Invalid shipping country");
						map.put("source", source);
						lstResult.add(map);
					} else {

						// insert shipping
						shippingId = create_shipping(orderProductFisrtItem);
						if (StringUtils.isEmpty(shippingId)) {
							Map map = new LinkedHashMap<>();
							map.put("name", orderNameShopify);
							map.put("type", ResourceStates.FAIL);
							map.put("msg", "Invalid shipping order");
							map.put("source", source);
							lstResult.add(map);

							DropshipOrderService.deleteByIdCSVImport(orderId);
							ShippingService.deleteByIdCSVImport(shippingId);
						} else {

							boolean isExists = false;
							if (org.apache.commons.lang.StringUtils.isNotEmpty(orderNameShopify)) {
								if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(storeId,
										orderNameShopify, source)) {
									isExists = true;
								}
							}

							if (isExists) {
								Map map = new LinkedHashMap<>();
								map.put("name", orderNameShopify);
								map.put("type", ResourceStates.EXIST);
								map.put("msg", String.format("Order %s is exist!.", orderNameShopify));
								map.put("source", source);
								lstResult.add(map);

								DropshipOrderService.deleteByIdCSVImport(orderId);
								ShippingService.deleteByIdCSVImport(shippingId);
							} else {
								String trackingNumber = AppUtil.generateOrderTrackingNumber();
								if (!orderProductFisrtItem.isIs_order_sku()) {
									LOGGER.info("order campaign");
									String[] lineSku = orderProductFisrtItem.getLineitem_sku().split("\\|");
									if (lineSku.length >= 2) {
										String variantId = lineSku[0];
										String sizeId = lineSku[1];
										Map variantMap = ProductVariantService
												.getAndCheckCampaignNotLocked(variantId);
										String campaignId = ParamUtil.getString(variantMap, "campaign_id");
										if (org.apache.commons.lang.StringUtils.isNotEmpty(userId)
												&& !variantMap.isEmpty() && campaignId.contains(userId)) {
											String baseId = ParamUtil.getString(variantMap,
													AppParams.BASE_ID);

											if (BaseSizeService.checkAvailabilityForBase(sizeId,
													baseId) == false) {
												Map map = new LinkedHashMap<>();
												map.put("name", orderNameShopify);
												map.put("type", ResourceStates.FAIL);
												map.put("msg", "Invalid lineitem sku");
												map.put("source", source);
												lstResult.add(map);

												DropshipOrderService.deleteByIdCSVImport(orderId);
												ShippingService.deleteByIdCSVImport(shippingId);
											} else {
												String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);

												String orderIdPrefix = createOrderIdPrefix(productId);
												int addressVerified = 0;
												if (!StringUtils.isEmpty(orderProductFisrtItem.getCheck_vaild_adress())) {
													try {
														addressVerified = Integer.valueOf(orderProductFisrtItem.getCheck_vaild_adress());
													} catch (Exception e) {
														// TODO: handle exception
													}
												}
												Map dropshipOrder = create_dropship_order(userId, storeId, channel,
														shippingId, orderNameShopify, trackingNumber, orderIdPrefix,
														ResourceStates.QUEUED, ResourceSource.CAMP_IMPORT,
														addressVerified);

												orderId = ParamUtil.getString(dropshipOrder, AppParams.ID);
	                                            Map dropshipOrderUpdate = processOrderItems(dropshipOrderProducts, orderId, orderNameShopify);
	                                            String dropshipOrderUpdateId = ParamUtil.getString(dropshipOrderUpdate,
	                                                    AppParams.ID);

	                                            if (!StringUtils.isEmpty(dropshipOrderUpdateId)) {

	                                            	Map map = new LinkedHashMap<>();
	                                        		map.put("name", orderNameShopify);
	                                        		map.put("type", ResourceStates.SUCCEEDED);
	                                        		map.put("msg", "");
	                                        		map.put("source", source);
	                                        		lstResult.add(map);

	                                        		this.total_order_success++;

	                                            } else {
//	                                        		Map map = new LinkedHashMap<>();
//	                                        		map.put("name", orderNameShopify);
//	                                        		map.put("type", ResourceStates.FAIL);;
//	                                        		map.put("msg", "Order " + orderNameShopify + " cannot import");
//	                                        		map.put("source", source);
//	                                        		lstResult.add(map);
	                                        		DropshipOrderService.deleteByIdCSVImport(orderId);
	                                        		ShippingService.deleteByIdCSVImport(shippingId);
	                                            }
											}
										} else {
											Map map = new LinkedHashMap<>();
											map.put("name", orderNameShopify);
											map.put("type", ResourceStates.FAIL);
											map.put("msg", "Product variant not found");
											map.put("source", source);
											lstResult.add(map);

											DropshipOrderService.deleteByIdCSVImport(orderId);
											ShippingService.deleteByIdCSVImport(shippingId);
										}
									} else {
										Map map = new LinkedHashMap<>();
										map.put("name", orderNameShopify);
										map.put("type", ResourceStates.FAIL);
										map.put("msg", "Invalid lineitem sku");
										map.put("source", source);
										lstResult.add(map);

										DropshipOrderService.deleteByIdCSVImport(orderId);
										ShippingService.deleteByIdCSVImport(shippingId);
									}
								} else {
									LOGGER.info("order sku");
									BaseSKUObj baseSKUobj = BaseSKUService
											.getBySku(orderProductFisrtItem.getLineitem_sku());
									if(baseSKUobj == null) {
										Map map = new LinkedHashMap<>();
										map.put("name", orderNameShopify);
										map.put("type", ResourceStates.FAIL);
										map.put("msg", "Invalid lineitem sku");
										map.put("source", source);
										lstResult.add(map);

										DropshipOrderService.deleteByIdCSVImport(orderId);
										ShippingService.deleteByIdCSVImport(shippingId);
									} else {

										String orderIdPrefix = userId + "-" + baseSKUobj.getBaseShortCode();
										int addressVerified = 0;
										if (!StringUtils.isEmpty(orderProductFisrtItem.getCheck_vaild_adress())) {
											try {
												addressVerified = Integer.valueOf(orderProductFisrtItem.getCheck_vaild_adress());
											} catch (Exception e) {
												// TODO: handle exception
											}
										}
										Map dropshipOrder = create_dropship_order(userId, storeId, channel,
												shippingId, orderNameShopify, trackingNumber, orderIdPrefix,
												ResourceStates.QUEUED, ResourceSource.CUSTOM_IMPORT,
												addressVerified);

										orderId = ParamUtil.getString(dropshipOrder, AppParams.ID);
										Map dropshipOrderUpdate = processOrderSKUItems(userId,
												dropshipOrderProducts, orderId, orderNameShopify, lstResult);
										String dropshipOrderUpdateId = ParamUtil.getString(dropshipOrderUpdate,
												AppParams.ID);

										if (!StringUtils.isEmpty(dropshipOrderUpdateId)) {
											Map map = new LinkedHashMap<>();
											map.put("name", orderNameShopify);
											map.put("type", ResourceStates.SUCCEEDED);
											map.put("msg", "");
											map.put("source", source);
											lstResult.add(map);
											this.total_order_success++;
										} else {
//														Map map = new LinkedHashMap<>();
//														map.put("name", orderNameShopify);
//														map.put("type", ResourceStates.FAIL);;
//														map.put("msg", "Order " + orderNameShopify + " cannot import");
//														map.put("source", source);
//														lstResult.add(map);
											DropshipOrderService.deleteByIdCSVImport(orderId);
											ShippingService.deleteByIdCSVImport(shippingId);
										}
									}
								}

							}

						}
					}
					LOGGER.info("orderNameShopify : " + orderNameShopify);
				} catch (Exception ex) {
					Logger.getLogger(DropshipOrderImportCVSHandlerV2.class.getName()).log(Level.SEVERE, null,
							ex);
					DropshipOrderService.deleteByIdCSVImport(orderId);
					ShippingService.deleteByIdCSVImport(shippingId);
				}

			}

		}
		return lstResult;
	}

	public static Map create_dropship_order(String userId, String storeId, String channel, String shippingId,
			String orderNameShopify, String trackingNumber, String orderIdPrefix, String state, String source,
			int addrVerified) throws SQLException, ParseException {

		DropshipOrderObj dropshipOrderObj = new DropshipOrderObj.Builder(orderIdPrefix)
				.orderCurrency("USD")
				.state(state)
				.shippingId(shippingId)
				.trackingNumber(trackingNumber)
				.channel(channel)
				.storeId(storeId)
				.userId(userId)
				.referenceOrderId(orderNameShopify)
				.source(source)
				.addrVerified(addrVerified)
				.build();

		LOGGER.info("dropshipOrderObj=" + dropshipOrderObj.toString());
		Map dropshipOrder = DropshipOrderService.insertDropshipOrder(dropshipOrderObj);
		return dropshipOrder;
	}

	private String create_shipping(DropshipOrderCSVObj orderProductFisrtItem) throws SQLException {
		Map shippingResult = ShippingService.insert(orderProductFisrtItem.getShipping_name(),
				orderProductFisrtItem.getEmail(), orderProductFisrtItem.getShipping_phone(),
				orderProductFisrtItem.getShipping_address1(), orderProductFisrtItem.getShipping_address2(),
				orderProductFisrtItem.getShipping_city(), orderProductFisrtItem.getShipping_province(),
				orderProductFisrtItem.getShipping_zip(), orderProductFisrtItem.getShipping_country(), "");
		// get shipping id
		String shippingId = ParamUtil.getString(shippingResult, AppParams.ID);
		return shippingId;
	}

	private JsonArray mapOrderAndValidOrder(JsonArray requestBodyArray) {
		JsonArray result = new JsonArray();
		int orderTotal = requestBodyArray.size();
		for (int i = 0; i < orderTotal; i++) {
			JsonArray temp = requestBodyArray.getJsonArray(i);
//			List<JsonArray> listProductVariantToOrder = divideProductVariantToOrder(mapVariant(temp));
			List<JsonArray> listProductVariantToOrder = divideProductVariantToOrder(temp);
			for (JsonArray jsonArray : listProductVariantToOrder) {
				result.add(jsonArray);
			}
		}
		return result;
	}

	private JsonArray mapVariant(JsonArray temp) {
		JsonArray result = new JsonArray();
		Map<String, String> mapVariantName = new LinkedHashMap<String, String>();
		for (int i = 0; i < temp.size(); i++) {
			JsonObject obj = temp.getJsonObject(i);
			String lineitem_sku = obj.getString("lineitem_sku");
			String lineitem_quantity = obj.getString("lineitem_quantity");
			try {
				if (BaseSKUService.isExistThisSku(lineitem_sku)) {

					String sDesignBack = obj.getString("design_back_url");
					String sDesignFront = obj.getString("design_front_url");
					String lineitem_sku_md5 = lineitem_sku + "_" + Common.getMD5(sDesignFront + sDesignBack);
					if (!mapVariantName.containsKey(lineitem_sku_md5)) {
						for (int j = i + 1; j < temp.size(); j++) {
							JsonObject obj1 = temp.getJsonObject(j);
							if(lineitem_sku.equalsIgnoreCase(obj1.getString("lineitem_sku"))
									&& obj1.getString("financial_status").equalsIgnoreCase("paid") &&
									sDesignBack.equalsIgnoreCase(obj1.getString("design_back_url"))
									&& sDesignFront.equalsIgnoreCase(obj1.getString("design_front_url"))) {
								int lineitem_quantity_total = Integer.valueOf(lineitem_quantity)
										+ Integer.valueOf(obj1.getString("lineitem_quantity"));
								lineitem_quantity = String.valueOf(lineitem_quantity_total);
								obj.put("lineitem_quantity", String.valueOf(lineitem_quantity_total));
							}
						}
						mapVariantName.put(lineitem_sku_md5, lineitem_sku_md5);
						result.add(obj);
					}

				} else {
					if (obj.getString("financial_status").equalsIgnoreCase("paid")) {
						if (!mapVariantName.containsKey(lineitem_sku)) {
							for (int j = i + 1; j < temp.size(); j++) {
								JsonObject obj1 = temp.getJsonObject(j);
								if (lineitem_sku.equalsIgnoreCase(obj1.getString("lineitem_sku"))
										&& obj1.getString("financial_status").equalsIgnoreCase("paid")) {
									int lineitem_quantity_total = Integer.valueOf(lineitem_quantity)
											+ Integer.valueOf(obj1.getString("lineitem_quantity"));
									lineitem_quantity = String.valueOf(lineitem_quantity_total);
									obj.put("lineitem_quantity", String.valueOf(lineitem_quantity_total));
								}
							}
							mapVariantName.put(lineitem_sku, lineitem_sku);
							result.add(obj);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}



		}
		LOGGER.info("mapVariantName: " + mapVariantName.toString());
		LOGGER.info("result=" + result.toString());
		return result;
	}

	private List<JsonArray> divideProductVariantToOrder(JsonArray temp) {
		List<JsonArray> result = new LinkedList<JsonArray>();
		JsonArray list_product_camp = new JsonArray();
		JsonArray list_product_sku = new JsonArray();
		for (int i = 0; i < temp.size(); i++) {
			JsonObject obj = temp.getJsonObject(i);
			String lineitem_sku = obj.getString("lineitem_sku");
			try {
				if (BaseSKUService.isExistThisSku(lineitem_sku)) {
					obj.put("is_order_sku", true);
					list_product_sku.add(obj);
				} else {
					list_product_camp.add(obj);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (list_product_camp != null && list_product_camp.size() > 0) {
			result.add(list_product_camp);
		}
		if (list_product_sku != null && list_product_sku.size() > 0) {
			result.add(list_product_sku);
		}
		return result;
	}

	public static String createOrderIdPrefix(String productId) throws SQLException {

		StringBuilder prefix = new StringBuilder();
		java.util.Map productInfoMap = ProductService.getV2(productId, true, false, false, false);
		if (!productInfoMap.isEmpty()) {
			java.util.Map base = ParamUtil.getMapData(productInfoMap, AppParams.BASE);
			String baseShortCode = ParamUtil.getString(base, AppParams.BASE_SHORT_CODE);
			String campaignId = ParamUtil.getString(productInfoMap, AppParams.CAMPAIGN_ID);
			prefix.append(campaignId).append(StringPool.DASH).append(baseShortCode);
		}
		return prefix.toString();

	}

	private java.util.Map processOrderItems(ArrayList<DropshipOrderCSVObj> dropshipOrderProducts, String orderId,
			String orderReferenceId) throws SQLException, ParseException {
		int orderProductSuccess = 0;
		int orderProductFailed = 0;
		int orderProductTotal;
		double orderTotal = 0.00;
		List<java.util.Map> orderItemList = new ArrayList<>();
		int totalItems = 0;
		orderProductTotal = dropshipOrderProducts.size();
		double orderShippingTotal = 0.00d;
		double orderSubTotal = 0.00d;
		int addressVerified = 0;
		Double totalTax = 0.00;
		for (int i = 0; i < orderProductTotal; i++) {
			DropshipOrderCSVObj vObj = dropshipOrderProducts.get(i);
			if (i == 0) {
				if (StringUtils.isEmpty(vObj.getCheck_vaild_adress())) {
					addressVerified = 0;
				} else {
					try {
						addressVerified = Integer.valueOf(vObj.getCheck_vaild_adress());
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			}
			java.util.Map orderItem = createOrderItem(orderId, vObj);

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
				break;
			}

		}
		//
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

	private java.util.Map processOrderSKUItems(String userId, ArrayList<DropshipOrderCSVObj> dropshipOrderProducts,
			String orderId, String orderReferenceId, List<Map> lstResult) throws SQLException, ParseException {
		int orderProductSuccess = 0;
		int orderProductFailed = 0;
		int orderProductTotal = dropshipOrderProducts.size();
		List<java.util.Map> orderItemList = new ArrayList<>();
		int totalItems = 0;
		double orderTotal = 0.00;
		double orderShippingTotal = 0.00d;
		double orderSubTotal = 0.00d;
		int addressVerified = 0;
		Double totalTax = 0.00;
		for (int i = 0; i < orderProductTotal; i++) {
			DropshipOrderCSVObj vObj = dropshipOrderProducts.get(i);
			if (i == 0) {
				if (StringUtils.isEmpty(vObj.getCheck_vaild_adress())) {
					addressVerified = 0;
				} else {
					try {
						addressVerified = Integer.valueOf(vObj.getCheck_vaild_adress());
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			}

			DropshipBaseSkuObj baseSKUobj = DropshipBaseSkuService.getBySku(vObj.getLineitem_sku());

			java.util.Map orderItem = createOrderSKUItem(userId, orderId, vObj, baseSKUobj, lstResult);
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
			} else {
				orderProductFailed++;
				break;
			}

		}
		//
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

	private Map createOrderItem(String orderId, DropshipOrderCSVObj vObj) throws SQLException {

		String source = ResourceSource.CAMP_IMPORT;
		if (vObj.isIs_order_sku()) {
			source = ResourceSource.CUSTOM_IMPORT;
		}

		String[] lineSku = vObj.getLineitem_sku().split("\\|");
		String variantId = lineSku[0];
		String sizeId = lineSku[1];
		Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);

		int quantity = Integer.parseInt(vObj.getLineitem_quantity());

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


			Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, vObj.getShipping_country(), quantity);
			Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);
			Double taxAmount = ParamUtil.getDouble(feeMap, AppParams.TAX_AMOUNT);
			double productSubTotal = baseCost * quantity;
			double productAmount = GetterUtil.format(baseCost * quantity + shippingFee + taxAmount, 2);

			DropshipOrderProductObj orderProductObj = new DropshipOrderProductObj.Builder(orderId)
					.orderId(orderId)
					.campaignId(campaignId)
					.productId(productId)
					.variantId(variantId)
					.variantName(variantName)
					.sizeId(sizeId)

					.sizeName(sizeName)
					.colorId(colorId)
					.colorName(colorName)
					.colorValue(colorValue)
					.variantFrontUrl(variantFrontUrl)
					.variantBackUrl(variantBackUrl)
					.designFrontUrl(designFrontUrl)
					.designBackUrl(designBackUrl)

					.price(baseCost)
					.currency("USD")
					.quantity(quantity)
					.amount(productAmount)
					.baseCost(baseCost)
					.baseShortCode(baseShortCode)
					.state(ResourceStates.APPROVED)
					.lineItemId( vObj.getId())
					.taxAmount(taxAmount)
					.build();

			orderItem = DropshipOrderProductService.insertDropshipOrderProduct(orderProductObj);
			orderItem.put(AppParams.SUBTOTAL, productSubTotal);

		}
		return orderItem;
	}

	private Map createOrderSKUItem(String userId, String orderId, DropshipOrderCSVObj vObj,
			DropshipBaseSkuObj baseSKUobj, List<Map> lstResult) throws SQLException {
		String source = ResourceSource.CAMP_IMPORT;
		if (vObj.isIs_order_sku()) {
			source = ResourceSource.CUSTOM_IMPORT;
		}

		Map orderItem = new LinkedHashMap<>();

		if (org.apache.commons.lang.StringUtils.isNotEmpty(vObj.getDesign_back_url()) && (vObj.getLineitem_sku()
				.matches(DropshipCustomApiOrderCreateHandler.LADIES_RACERBACK_TANK_REGEX)
				|| (!vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler._2D_SHIRT_SKU_REGEX) && !vObj
						.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler._3D_ALLOW_2SIDE_DESIGNS)))) {
			LOGGER.info("vObj.getDesign_back_url()" + vObj.getDesign_back_url());
			LOGGER.info("vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler.LADIES_RACERBACK_TANK_REGEX) " + vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler.LADIES_RACERBACK_TANK_REGEX));
			LOGGER.info("!vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler._2D_SHIRT_SKU_REGEX) " + !vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler._2D_SHIRT_SKU_REGEX));
			LOGGER.info("!vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler._3D_ALLOW_2SIDE_DESIGNS) " + !vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler._3D_ALLOW_2SIDE_DESIGNS));
			LOGGER.info("Order items: sku does not support back print. " + vObj.getLineitem_sku());
			Map map = new LinkedHashMap<>();
			map.put("name", vObj.getName());
			map.put("type", ResourceStates.FAIL);
			map.put("msg", "Sku does not support back print.");
			map.put("source", source);
			lstResult.add(map);
		} else {
			boolean check_design = false;
			if(!StringUtils.isEmpty(vObj.getDesign_front_url())) {
				if(!StringUtils.isEmpty(vObj.getMockup_front_url())) {
					check_design = true;
				} else {
					check_design = false;
				}
			}

			if(!StringUtils.isEmpty(vObj.getDesign_back_url())) {
				if(!StringUtils.isEmpty(vObj.getMockup_back_url())) {
					check_design = true;
				} else {
					check_design = false;
				}
			}

			if (check_design) {
				CheckDesignsResponse checkDesignsResponse = ISPApiConnector.checkDesignUrls(userId, baseSKUobj.getBaseId(), vObj.getDesign_front_url(), vObj.getDesign_back_url());
				if (checkDesignsResponse.getIsValid()) {
					String md5_image = checkDesignsResponse.getDesignFront().getMd5Checksum();
					if(StringUtils.isEmpty(md5_image)) {
						md5_image = checkDesignsResponse.getDesignBack().getMd5Checksum();
					}
					int quantity = Integer.parseInt(vObj.getLineitem_quantity());

					int isTwoDesigns = 0;
					if (org.apache.commons.lang.StringUtils.isNotEmpty(vObj.getDesign_front_url()) && org.apache.commons.lang.StringUtils.isNotEmpty(vObj.getDesign_back_url())) {
						isTwoDesigns = 1;
					}
					double baseCost = BaseService.getDropshipBaseCost(baseSKUobj.getBaseId(), baseSKUobj.getSizeId(), isTwoDesigns);
					LOGGER.info("baseCost: " + baseCost);
					Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseSKUobj.getBaseId(), vObj.getShipping_country(), quantity);
					Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);
					Double taxAmount = ParamUtil.getDouble(feeMap, AppParams.TAX_AMOUNT);
					double productSubTotal = baseCost * quantity;
					double productAmount = GetterUtil.format(baseCost * quantity + shippingFee + taxAmount, 2);

					String design_front_url = "";
					String design_back_url = "";

					if (checkDesignsResponse.getDesignFront() != null && org.apache.commons.lang.StringUtils
							.isNotEmpty(checkDesignsResponse.getDesignFront().getUrl())) {
						design_front_url = checkDesignsResponse.getDesignFront().getUrl();
					}

					if (checkDesignsResponse.getDesignBack() != null && org.apache.commons.lang.StringUtils
							.isNotEmpty(checkDesignsResponse.getDesignBack().getUrl())) {
						design_back_url = checkDesignsResponse.getDesignBack().getUrl();
					}

					DropshipOrderProductObj orderProductObj = new DropshipOrderProductObj.Builder(orderId)
							.campaignId(userId + "-" + md5_image)
//							.productId(productId)
//							.variantId(variantId)
							.sizeId(baseSKUobj.getSizeId())
							.price(baseCost)
							.shippingFee(shippingFee)
							.currency("USD")
							.quantity(quantity)
							.state(ResourceStates.APPROVED)
							.variantName(baseSKUobj.getBaseName() + " -" + baseSKUobj.getColorName())
							.amount(productAmount)
							.baseCost(baseCost)
							.baseId(baseSKUobj.getBaseId())
//							.lineItemId(referenceOrderId)
							.variantFrontUrl(vObj.getMockup_front_url())
							.variantBackUrl(vObj.getMockup_back_url())
							.colorId(baseSKUobj.getColorId())
							.colorValue(baseSKUobj.getColorValue())
							.partnerSku(baseSKUobj.getSku())
							.colorName(baseSKUobj.getColorName())
							.sizeName(baseSKUobj.getSizeName())
                            .designFrontUrl(design_front_url)
                            .designBackUrl(design_back_url)
//							.shippingMethod(shippingMethod)
//							.printDetail(setPrintDetail)
							.itemType(ResourceStates.NORMAL)
//							.partnerProperties(setPartnerProperties)
//							.partnerOption(setPartnerOption)
							.baseShortCode(baseSKUobj.getBaseShortCode())
							.taxAmount(taxAmount)
							.build();
					if (vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler.POSTER_SKU_REGEX)
                            || vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler.MUG_SKU_REGEX)) {
                        orderProductObj.setColorName("White");
                        orderProductObj.setColorValue("#ffffff");
                    }
					LOGGER.info("orderProductObj: " + orderProductObj.toString());
					orderItem = DropshipOrderProductService.insertDropshipOrderProduct(orderProductObj);
					LOGGER.info("createOrderSKUItem()- create item done orderItem=" + orderItem.toString());
					orderItem.put(AppParams.SUBTOTAL, productSubTotal);
				} else {
					String message = checkDesignsResponse.getDesignFront().getDescription();
					if(StringUtils.isEmpty(message)) {
						message = checkDesignsResponse.getDesignBack().getDescription();
					}
					LOGGER.info("File design is not valid");
					Map map = new LinkedHashMap<>();
					map.put("name", vObj.getName());
					map.put("type", ResourceStates.FAIL);
					map.put("msg", message);
					map.put("source", source);
					lstResult.add(map);
				}
				try {
					Thread.sleep(300);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} else {
				LOGGER.info("File design is not null");
				Map map = new LinkedHashMap<>();
				map.put("name", vObj.getName());
				map.put("type", ResourceStates.FAIL);
				map.put("msg", "File design is not null");
				map.put("source", source);
				lstResult.add(map);
			}
		}

		return orderItem;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderImportCVSHandlerV2.class.getName());

}
