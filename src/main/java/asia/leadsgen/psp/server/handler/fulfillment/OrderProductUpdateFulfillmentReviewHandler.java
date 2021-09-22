package asia.leadsgen.psp.server.handler.fulfillment;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.BaseSKUObj;
import asia.leadsgen.psp.obj.Image;
import asia.leadsgen.psp.service.ImageService;
import asia.leadsgen.psp.service_fulfill.BaseSKUService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.CheckDesignsResponse;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ISPUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.PartnerConst;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import oracle.jdbc.OracleTypes;

public class OrderProductUpdateFulfillmentReviewHandler implements Handler<RoutingContext> {
	private static final String GET_SEQ = "{call PKG_FULFILLMENT_REVIEW.GET_SEQ(?,?,?,?,?)}";
	private static final String GET_FULFILLMENT_REVIEW_BY_CAMPAIGN_ID_BASE_ID_COLOR_NAME = "{call PKG_FULFILLMENT_REVIEW.get_fulfillment_review_by_campaign_id_base_id_color_name(?,?,?,?,?,?)}";
	public static final String GET_UPDATE_FULFILLMENT_REVIEW_BY_ORDER_ID = "{call PKG_FULFILLMENT_REVIEW.get_update_fulfillment_review_by_order_id(?,?,?,?)}";
	public static final String GET_DESIGN_INFO = "{call PKG_FULFILLMENT_REVIEW.GET_DESIGN_INFO(?,?,?,?)}";
	public static final String CHECK_EXSITS_FULFILLMENT_2D = "{call PKG_FULFILLMENT_REVIEW.CHECK_EXSITS(?,?,?,?,?,?)}";
	public static final String INSERT_FULFILLMENT_2D = "{call PKG_FULFILLMENT_REVIEW.INSERT_FULFILLMENT_REVIEW(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String GET_PRODUCT_INFO = "{call PKG_FULFILLMENT_REVIEW.GET_PRODUCT_INFO(?,?,?,?)}";
	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	private static String baseType;
	
	public static void setBaseType(String baseType) {
		OrderProductUpdateFulfillmentReviewHandler.baseType = baseType;
	}

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {
				String order_id = routingContext.request().getParam("id");
				if(StringUtils.isNotEmpty(order_id)) {
					logger.info("order_id: " + order_id);
					insertFulfillMent2D(order_id);
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
				} else {
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
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

	public static void insertFulfillMent2D(String order_id) throws SQLException {
		PartnerConst.initPartnerConst();
		List<Map> lst_campaign = getOrder2D(order_id);
		List<Map> list_order_designs = mapingCampaignProduct(lst_campaign);
		if (list_order_designs != null && list_order_designs.size() > 0) {
			for (Map item : list_order_designs) {
				logger.info("item= " + item);
				String campaign_id = ParamUtil.getString(item, AppParams.CAMPAIGN_ID);
				String base_id = ParamUtil.getString(item, AppParams.BASE_ID);
				String design_front_id = ParamUtil.getString(item, AppParams.DESIGN_FRONT_ID);
				String design_back_id = ParamUtil.getString(item, AppParams.DESIGN_BACK_ID);
				String color_id = ParamUtil.getString(item, AppParams.COLOR_ID);
				String seq = "";
				String sku = "";
				int is_exists = checkExistsOrder2D(campaign_id, base_id, color_id);
				if (is_exists <= 0) {
					String url_print_front = ParamUtil.getString(item, AppParams.URL_PRINT_FRONT);
					String url_print_back = ParamUtil.getString(item, AppParams.URL_PRINT_BACK);
					String url_design_front = ParamUtil.getString(item, AppParams.URL_DESIGN_FRONT);
					String url_design_back = ParamUtil.getString(item, AppParams.URL_DESIGN_BACK);
					String url_mockup_front = ParamUtil.getString(item, AppParams.VARIANT_FRONT_URL);
					String url_mockup_back = ParamUtil.getString(item, AppParams.VARIANT_BACK_URL);
					String base_name = ParamUtil.getString(item, AppParams.BASE_NAME);
					String color_name = ParamUtil.getString(item, AppParams.COLOR_NAME);
					String color_value = ParamUtil.getString(item, AppParams.COLOR_VALUE);
					String print_type = ParamUtil.getString(item, AppParams.PRINT_TYPE);
					String source = ParamUtil.getString(item, AppParams.SOURCE);
					String base_type = ParamUtil.getString(item, AppParams.BASE_TYPE);
					String state = "created";
					int main = ParamUtil.getInt(item, AppParams.MAIN);
					boolean is_insert = true;
					if (base_id.matches(PartnerConst.getLeecowleatherBases())) {
						seq = getSED(campaign_id, design_front_id);
//						logger.info("seq= " + seq);
						if(StringUtils.isNotEmpty(seq)) {
							BaseSKUObj baseSKU = BaseSKUService.getSkuByParterIdBaseIdColorId(PartnerConst.LEE_COW_LEATHER, base_id, color_id);
							logger.info("baseSKU= " + baseSKU.toString());
							if (baseSKU == null || StringUtils.isEmpty(baseSKU.getSku())) {
								is_insert = false;
							} else {
								sku = baseSKU.getSku();
								if (seq.length() < 6) {
									int temp = 6 - seq.length();
									for (int i = 0; i < temp; i++) {
										seq = "0" + seq;
									}
								}
//								sku = sku.replace("BG-LN-A", "BG-LN-A" + seq);
								sku = sku.substring(0, 7) + seq + sku.substring(7);
							}
						} else {
							is_insert = false;
						}
					}

					if (is_insert) {
						if (source.equalsIgnoreCase("dropship") || base_type.matches(baseType)) {
							if(base_type.matches(baseType)) {
//								state = "approved";
								state = "created";
							} else {
								boolean is_approved = true;
								if (StringUtils.isNotEmpty(design_front_id) && StringUtils.isEmpty(url_print_front)) {
									is_approved = false;
								}
								
								if(is_approved && (StringUtils.isNotEmpty(design_back_id) && StringUtils.isEmpty(url_print_back))) {
									is_approved = false;
								}
								
								if(is_approved) {
//									state = "approved";
									state = "created";
								}
							}
						} 

						Map data = insertOrder2D(campaign_id, design_front_id, url_design_front, design_back_id,
								url_design_back, url_mockup_front, url_mockup_back, base_id, base_name, main,
								url_print_front, url_print_back, color_id, color_name, color_value, seq, sku,
								print_type, source, state);
						logger.info("data= " + data);
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
//				else if (is_exists > 1){
//					break;
//				}
			}
		}

	}

	public static List<Map> mapingCampaignProduct(List<Map> list_order) throws SQLException {
		List<Map> list_result = new ArrayList<Map>();
		try {
			for (Map map : list_order) {
				Map item = map;
				String product_id = ParamUtil.getString(map, AppParams.PRODUCT_ID);
				List<Map> lst_designs = getDesignInfo(product_id);
				logger.info("lst_designs= " + lst_designs.toString());
				String url_print_front = "";
				String url_print_back = "";

				String design_front_id = "";
				String url_design_front = "";
				String design_back_id = "";
				String url_design_back = "";
				String print_type = "normal";
				String baseId = ParamUtil.getString(item, AppParams.BASE_ID);
				String base_type = ParamUtil.getString(item, AppParams.BASE_TYPE);
				String type_print = "";
				if (baseId.matches(PartnerConst.getScalablepressMugBases())) {
					type_print = "mug";
				} else if (baseId.matches(PartnerConst.getScalablepressPosterBases())) {
					type_print = "poster";
				} else if (baseId.matches(PartnerConst.getPrintwayBases())) {
					type_print = "phonecase";
				} else if (baseId.matches(PartnerConst.getLeecowleatherBases())) {
					type_print = "leather";
				}

				String color_id = ParamUtil.getString(item, AppParams.COLOR_ID);
				String color_name = ParamUtil.getString(item, AppParams.COLOR_NAME);
				String color_value = ParamUtil.getString(item, AppParams.COLOR_VALUE);

				if (lst_designs != null && lst_designs.size() > 0) {
					int main = 0;
					for (Map design : lst_designs) {
						String type = ParamUtil.getString(design, AppParams.TYPE);
						String image_id = ParamUtil.getString(design, AppParams.IMAGE_ID);
//						logger.info("image_id=" + image_id);
						Image image = ImageService.getImage(image_id);
						if (ParamUtil.getInt(design, AppParams.MAIN) > 0) {
							main = ParamUtil.getInt(design, AppParams.MAIN);
						}
//						logger.info("image= " + image.toString() + " -- type_print= " + type_print + " -- baseId= "
//								+ baseId + " -- type= " + type);

						image.setColorName(color_name);
						image.setColorValue(color_value);
						if (type.equalsIgnoreCase("front") || type.equalsIgnoreCase("full")) {
							design_front_id = ParamUtil.getString(design, AppParams.DESIGN_ID);
							url_design_front = ParamUtil.getString(design, AppParams.URL);
							if(!base_type.matches(baseType)) {
								if (type_print.equalsIgnoreCase("leather")) {
									CheckDesignsResponse adjustedFrontUrl = ISPUtil
											.adjustLeeCowLeatherDesign(image.getUrl(), baseId, color_id);
									if (adjustedFrontUrl != null) {
										if (adjustedFrontUrl.getHasLaser() == 1) {
											print_type = "laser";
										} else {
											print_type = "normal";
										}
										url_print_front = adjustedFrontUrl.getUrl();
									}
								} else {
									if (StringUtils.isEmpty(type_print)) {
										url_print_front = ISPUtil.adjustDesign(image, true);
									} else {
										url_print_front = ISPUtil.adjustAccessoriesDesign(image, type_print, baseId);
									}
								}
							} else {
								url_print_front = url_design_front;
							}
							
						} else if (type.equalsIgnoreCase("back")) {
							if(!base_type.matches(baseType)) {
								if (type_print.equalsIgnoreCase("leather")) {
									CheckDesignsResponse adjustedFrontUrl = ISPUtil
											.adjustLeeCowLeatherDesign(image.getUrl(), baseId, color_id);
									if (adjustedFrontUrl != null) {
										if (adjustedFrontUrl.getHasLaser() == 1) {
											print_type = "laser";
										} else {
											print_type = "normal";
										}
										url_print_back = adjustedFrontUrl.getUrl();
									}
								} else {
									if (StringUtils.isEmpty(type_print)) {
										url_print_back = ISPUtil.adjustDesign(image, false);
									} else {
										url_print_back = ISPUtil.adjustAccessoriesDesign(image, type_print, baseId);
									}
								}
							} else {
								url_print_back = url_design_back;
							}

							design_back_id = ParamUtil.getString(design, AppParams.DESIGN_ID);
							url_design_back = ParamUtil.getString(design, AppParams.URL);
						}
					}
//					logger.info("url_print_front= " + url_print_front + " -- url_print_back= " + url_print_back);
					if (type_print.equalsIgnoreCase("mug")) {
						if (!StringUtils.isEmpty(url_print_front)) {
							url_print_back = url_print_front;
						} else {
							url_print_front = url_print_back;
						}
					}
//					if(!StringUtils.isEmpty(url_print_front) || !StringUtils.isEmpty(url_print_back)) {
					item.put(AppParams.URL_PRINT_FRONT, url_print_front);
					item.put(AppParams.URL_PRINT_BACK, url_print_back);
					item.put(AppParams.DESIGN_FRONT_ID, design_front_id);
					item.put(AppParams.URL_DESIGN_FRONT, url_design_front);
					item.put(AppParams.DESIGN_BACK_ID, design_back_id);
					item.put(AppParams.URL_DESIGN_BACK, url_design_back);
					item.put(AppParams.PRINT_TYPE, print_type);
					item.put(AppParams.MAIN, main);
					list_result.add(item);
//					} else {
//						logger.info(" -- type_print= " + type_print + " -- baseId= " + baseId);
//						continue;
//					}

				}
			}
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return list_result;
	}

	public static List<Map> getOrder2D(String order_id) throws SQLException {
		logger.info("order_id= " + order_id);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, order_id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_UPDATE_FULFILLMENT_REVIEW_BY_ORDER_ID, inputParams,
				outputParamsTypes, outputParamsNames);
        logger.info("getOrder2D with resultMap" + resultMap);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> result_order = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<Map> list_campaign = new ArrayList<>();
		try {
			for (Map item : result_order) {
				list_campaign.add(format(item));
			}
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return list_campaign;
	}

	public static List<Map> getProductInfo(String campaign_id) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaign_id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_PRODUCT_INFO, inputParams,
				outputParamsTypes, outputParamsNames);
		logger.info("getDesignInfo with resultMap" + resultMap);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> result = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		logger.info("result= " + result);

		List<Map> list_design = new ArrayList<>();
		try {
			for (Map item : result) {
				item.put(AppParams.S_CAMPAIGN_ID, campaign_id);
				list_design.add(format(item));
			}
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}

		logger.info("list_design= " + list_design);
		return list_design;
	}

	public static List<Map> getDesignInfo(String product_id) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, product_id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_DESIGN_INFO, inputParams,
				outputParamsTypes, outputParamsNames);
		logger.info("getDesignInfo with resultMap" + resultMap);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> result = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		logger.info("result= " + result);

		List<Map> list_design = new ArrayList<>();
		try {
			for (Map item : result) {
				list_design.add(formatGetDesignInfo(item));
			}
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}

		logger.info("list_design= " + list_design);
		return list_design;
	}

	public static int checkExistsOrder2D(String campaign_id, String base_id, String color_id) throws SQLException {
		logger.info("campaign_id= " + campaign_id + " -- base_id= " + base_id + " -- color_id= " + color_id);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaign_id);
		inputParams.put(2, base_id);
		inputParams.put(3, color_id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_TOTAL);
		outputParamsNames.put(6, AppParams.RESULT_MSG);

		Map resultMap = DBProcedureUtil.execute(dataSource, CHECK_EXSITS_FULFILLMENT_2D, inputParams,
				outputParamsTypes, outputParamsNames);
        logger.info("checkExistsOrder2D with resultMap" + resultMap);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		int result = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);

        logger.info("result= " + result);
		return result;
	}

	public static String getSED(String campaign_id, String design_front_id) throws SQLException {
		logger.info("campaign_id= " + campaign_id + " -- design_front_id= " + design_front_id);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaign_id);
		inputParams.put(2, design_front_id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_SEQ);

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_SEQ, inputParams, outputParamsTypes, outputParamsNames);
        logger.info("checkExistsOrder2D with resultMap" + resultMap);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		String result = ParamUtil.getString(resultMap, AppParams.RESULT_SEQ);

        logger.info("result= " + result);
		return result;
	}

	public static Map insertOrder2D(String campaign_id, String design_front_id, String url_design_front,
			String design_back_id, String url_design_back, String url_mockup_front, String url_mockup_back,
			String base_id, String base_name, int main, String url_print_front, String url_print_back, String color_id,
			String color_name, String color_value, String seq, String sku, String print_type, String source, String state)
			throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaign_id);
		inputParams.put(2, design_front_id);
		inputParams.put(3, url_design_front);
		inputParams.put(4, design_back_id);
		inputParams.put(5, url_design_back);
		inputParams.put(6, url_mockup_front);
		inputParams.put(7, url_mockup_back);
		inputParams.put(8, base_id);
		inputParams.put(9, base_name);
		inputParams.put(10, main);
		inputParams.put(11, url_print_front);
		inputParams.put(12, url_print_back);
		inputParams.put(13, color_id);
		inputParams.put(14, color_name);
		inputParams.put(15, color_value);
		inputParams.put(16, seq);
		inputParams.put(17, sku);
		inputParams.put(18, print_type);
		inputParams.put(19, source);
		inputParams.put(20, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(21, OracleTypes.NUMBER);
		outputParamsTypes.put(22, OracleTypes.VARCHAR);
		outputParamsTypes.put(23, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(21, AppParams.RESULT_CODE);
		outputParamsNames.put(22, AppParams.RESULT_MSG);
		outputParamsNames.put(23, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, INSERT_FULFILLMENT_2D, inputParams,
				outputParamsTypes, outputParamsNames);
//        logger.info("insertFulfillMent2D with resultMap" + resultMap);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> result_order = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

//        logger.info("result_order= " + result_order);

		return result_order.get(0);
	}

	private static Map format(Map queryData) throws SQLException, ParseException {

		Map resultMap = new LinkedHashMap<>();

		String campaignId = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));
		resultMap.put(AppParams.VARIANT_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_VARIANT_FRONT_URL));
		resultMap.put(AppParams.VARIANT_BACK_URL, ParamUtil.getString(queryData, AppParams.S_VARIANT_BACK_URL));
		resultMap.put(AppParams.BASE_ID, ParamUtil.getString(queryData, AppParams.S_BASE_ID));
		resultMap.put(AppParams.BASE_TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE_ID));
		resultMap.put(AppParams.BASE_NAME, ParamUtil.getString(queryData, AppParams.S_BASE_NAME));
		resultMap.put(AppParams.PRODUCT_ID, ParamUtil.getString(queryData, AppParams.S_PRODUCT_ID));
		resultMap.put(AppParams.COLOR_ID, ParamUtil.getString(queryData, AppParams.S_COLOR_ID));
		resultMap.put(AppParams.COLOR_NAME, ParamUtil.getString(queryData, AppParams.S_COLOR_NAME));
		resultMap.put(AppParams.COLOR_VALUE, ParamUtil.getString(queryData, AppParams.S_COLOR_VALUE));
		resultMap.put(AppParams.SOURCE, ParamUtil.getString(queryData, AppParams.S_SOURCE));
		logger.info("resultMap= " + resultMap);
		return resultMap;
	}

	private static Map formatGetDesignInfo(Map queryData) throws SQLException, ParseException {

		Map resultMap = new LinkedHashMap<>();

		String campaignId = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.DESIGN_ID, ParamUtil.getString(queryData, AppParams.S_DESIGN_ID));
		resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));
		resultMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_URL));
		resultMap.put(AppParams.IMAGE_ID, ParamUtil.getString(queryData, AppParams.S_IMAGE_ID));
		resultMap.put(AppParams.MAIN, ParamUtil.getInt(queryData, AppParams.N_MAIN));
		logger.info("resultMap= " + resultMap);
		return resultMap;
	}

	public static Map getFulfillmentReviewByCampaignIdBaseIdColorName(String campaign_id, String base_id,
			String color_name) throws SQLException {
		logger.info("campaign_id= " + campaign_id + " --- base_id= " + base_id + " --- color_name= " + color_name);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaign_id);
		inputParams.put(2, base_id);
		inputParams.put(3, color_name);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_FULFILLMENT_REVIEW_BY_CAMPAIGN_ID_BASE_ID_COLOR_NAME,
				inputParams, outputParamsTypes, outputParamsNames);
		logger.info("getDesignInfo with resultMap" + resultMap);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> result = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (result != null && result.size() > 0) {
			try {
				return formatFulfillmentReview(result.get(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static Map formatFulfillmentReview(Map queryData) throws SQLException, ParseException {

		Map resultMap = new LinkedHashMap<>();

		String campaignId = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.SKU, ParamUtil.getString(queryData, AppParams.S_SKU));
		resultMap.put(AppParams.URL_PRINT_FRONT, ParamUtil.getString(queryData, AppParams.S_URL_PRINT_FRONT));
		resultMap.put(AppParams.PRINT_TYPE, ParamUtil.getString(queryData, AppParams.S_PRINT_TYPE));
		resultMap.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));
		resultMap.put(AppParams.DESIGN_FRONT_ID, ParamUtil.getString(queryData, AppParams.S_DESIGN_FRONT_ID));
		resultMap.put(AppParams.BASE_ID, ParamUtil.getString(queryData, AppParams.S_BASE_ID));
		resultMap.put(AppParams.COLOR_ID, ParamUtil.getString(queryData, AppParams.S_COLOR_ID));
		logger.info("resultMap= " + resultMap);
		return resultMap;
	}

	private static final Logger logger = Logger.getLogger(OrderProductUpdateFulfillmentReviewHandler.class.getName());

}
;
