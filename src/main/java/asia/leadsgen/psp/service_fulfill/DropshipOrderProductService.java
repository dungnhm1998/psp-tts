package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class DropshipOrderProductService extends  MasterService{
	
	static final String DROPSHIP_ORDER_PRODUCT_INSERT = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.dropship_order_product_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_PRD_UPDATE = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.order_prd_update(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_PRD_UPDATE_BY_PREDEFINED_SKU = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.order_prd_update_by_predefined_sku(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_PRD_SEARCH = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.order_prd_search(?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_PRD_DELETE_ALL = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.order_prd_delete_all(?,?,?)}";
	static final String DROPSHIP_ORDER_PRD_DELETE_ALL_CSV_IMPORT = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.order_prd_csv_delete_all_by_order_id(?,?,?)}";
	static final String DROPSHIP_ORDER_PRD_DELETE_ITEM = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.order_prd_delete_item(?,?,?)}";
	static final String DROPSHIP_ORDER_UPDATE_SHIPPING_METHOD = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.order_update_shipping_method(?,?,?,?)}";
	static final String DROPSHIP_ORDER_PRD_GET_BY_ID = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.order_prd_get_by_id(?,?,?,?)}";
	static final String GET_DROPSHIP_TOP_SELLING_PRODUCT = "{call PKG_FF_DROPSHIP_ORDER_PRODUCT.get_topselling_product(?,?,?,?,?,?,?,?,?,?,?)}";
	static final String UPDATE_ETSY_ORDER = "{call pkg_dropship_order_product.update_etsy_order(?,?,?,?,?,?,?,?)}";
	static final String INSERT_PRODUCT = "{call pkg_dropship_order_product.insert_etsy_product(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static Map insertDropshipOrderProduct(DropshipOrderProductObj dropshipOrderProduct) throws SQLException {

		LOGGER.info("Inserting dropship order product " + dropshipOrderProduct.toString());
		Map inputParams = new LinkedHashMap<Integer, DropshipOrderProductObj>();

//		inputParams.put(index++, dropshipOrderProduct.getId());
		inputParams.put(1,dropshipOrderProduct.getOrderId());
		inputParams.put(2,dropshipOrderProduct.getCampaignId());
		inputParams.put(3,dropshipOrderProduct.getProductId());
		inputParams.put(4,dropshipOrderProduct.getVariantId());
		inputParams.put(5,dropshipOrderProduct.getSizeId());
		inputParams.put(6,dropshipOrderProduct.getPrice());
		inputParams.put(7,dropshipOrderProduct.getShippingFee());
		inputParams.put(8,dropshipOrderProduct.getCurrency());
		inputParams.put(9,dropshipOrderProduct.getQuantity());
		inputParams.put(10,dropshipOrderProduct.getState());
		inputParams.put(11,dropshipOrderProduct.getVariantName());
		inputParams.put(12,dropshipOrderProduct.getAmount());
		inputParams.put(13,dropshipOrderProduct.getBaseCost());
		inputParams.put(14,dropshipOrderProduct.getBaseId());
		inputParams.put(15,dropshipOrderProduct.getLineItemId());
		inputParams.put(16,dropshipOrderProduct.getVariantFrontUrl());
		inputParams.put(17,dropshipOrderProduct.getVariantBackUrl());
		inputParams.put(18,dropshipOrderProduct.getColorId());
		inputParams.put(19,dropshipOrderProduct.getColorValue());
		inputParams.put(20,dropshipOrderProduct.getPartnerSku());
		inputParams.put(21,dropshipOrderProduct.getColorName());
		inputParams.put(22,dropshipOrderProduct.getSizeName());
		inputParams.put(23,dropshipOrderProduct.getShippingMethod());
		inputParams.put(24,dropshipOrderProduct.getPrintDetail());
		inputParams.put(25,dropshipOrderProduct.getItemType());
		inputParams.put(26,dropshipOrderProduct.getPartnerProperties());
		inputParams.put(27,dropshipOrderProduct.getPartnerOption());
		inputParams.put(28,dropshipOrderProduct.getBaseShortCode());
		inputParams.put(29,dropshipOrderProduct.getDesignFrontUrl());
		inputParams.put(30,dropshipOrderProduct.getDesignBackUrl());
		inputParams.put(31,dropshipOrderProduct.getUnitAmount());
		inputParams.put(32,dropshipOrderProduct.getTax());
		inputParams.put(33,dropshipOrderProduct.getTaxAmount());

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(34, OracleTypes.NUMBER);
		outputParamsTypes.put(35, OracleTypes.VARCHAR);
		outputParamsTypes.put(36, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(34, AppParams.RESULT_CODE);
		outputParamsNames.put(35, AppParams.RESULT_MSG);
		outputParamsNames.put(36, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PRODUCT_INSERT, inputParams, outputParamsTypes, outputParamsNames);

		LOGGER.info("insertResultMap: " + insertResultMap.toString());

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = format3(resultDataList.get(0));

		LOGGER.info("=> Dropship Order product insert result: " + resultMap.toString());

		return resultMap;
	}
	
	public static Map update(DropshipOrderProductObj obj) throws SQLException {

		LOGGER.info("Updating dropship order product " + obj.toString());
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, obj.getId());
		inputParams.put(2, obj.getSizeId());
		inputParams.put(3, obj.getQuantity());
		inputParams.put(4, obj.getShippingFee());
		inputParams.put(5, obj.getAmount());
		inputParams.put(6, obj.getState());
		inputParams.put(7, obj.getPrice());
		inputParams.put(8, obj.getBaseCost());
		inputParams.put(9, obj.getUnitAmount());
		inputParams.put(10, obj.getTaxAmount());

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.VARCHAR);
		outputParamsTypes.put(13, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(11, AppParams.RESULT_CODE);
		outputParamsNames.put(12, AppParams.RESULT_MSG);
		outputParamsNames.put(13, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PRD_UPDATE,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = format3(resultDataList.get(0));

		LOGGER.info("=>Dropship Order product update result: " + resultMap.toString());

		return resultMap;
	}
	
	public static Map updateByPredefinedSku(DropshipOrderProductObj dropshipOrderProduct) throws SQLException {
		Map resultMap = null;
		try {
			LOGGER.info("Updating dropship order product " + dropshipOrderProduct.toString());
			Map inputParams = new LinkedHashMap<Integer, DropshipOrderProductObj>();
			inputParams.put(1, dropshipOrderProduct.getId());
			inputParams.put(2, dropshipOrderProduct.getSizeId());
			inputParams.put(3, dropshipOrderProduct.getPrice());
			inputParams.put(4, dropshipOrderProduct.getQuantity());
			inputParams.put(5, dropshipOrderProduct.getShippingFee());
			inputParams.put(6, dropshipOrderProduct.getAmount());
			inputParams.put(7, dropshipOrderProduct.getBaseId());
			inputParams.put(8, dropshipOrderProduct.getBaseCost());
			inputParams.put(9, dropshipOrderProduct.getDesignFrontUrl());
			inputParams.put(10, dropshipOrderProduct.getVariantFrontUrl());
			inputParams.put(11, dropshipOrderProduct.getDesignBackUrl());
			inputParams.put(12, dropshipOrderProduct.getVariantBackUrl());
			inputParams.put(13, dropshipOrderProduct.getColorId());
			inputParams.put(14, dropshipOrderProduct.getColorName());
			inputParams.put(15, dropshipOrderProduct.getColorValue());
			inputParams.put(16, dropshipOrderProduct.getVariantName());
			inputParams.put(17, dropshipOrderProduct.getSizeName());
			inputParams.put(18, dropshipOrderProduct.getCampaignId());
			inputParams.put(19, dropshipOrderProduct.getProductId());
			inputParams.put(20, dropshipOrderProduct.getVariantId());
			inputParams.put(21, dropshipOrderProduct.getUnitAmount());
			inputParams.put(22, dropshipOrderProduct.getTaxAmount());
			LOGGER.info("=>Dropship Order product update result: 1");
			Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
			outputParamsTypes.put(23, OracleTypes.NUMBER);
			outputParamsTypes.put(24, OracleTypes.VARCHAR);
			outputParamsTypes.put(25, OracleTypes.CURSOR);
			LOGGER.info("=>Dropship Order product update result: 2");
			Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
			outputParamsNames.put(23, AppParams.RESULT_CODE);
			outputParamsNames.put(24, AppParams.RESULT_MSG);
			outputParamsNames.put(25, AppParams.RESULT_DATA);
			LOGGER.info("=>Dropship Order product update result: 3");


			Map insertResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PRD_UPDATE_BY_PREDEFINED_SKU, 
					inputParams, outputParamsTypes, outputParamsNames);
			LOGGER.info("=>Dropship Order product update result: 4");
			int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
			LOGGER.info("=>Dropship Order product update result: 5");
			if (resultCode != HttpResponseStatus.OK.code()) {
				throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
			}
			LOGGER.info("=>Dropship Order product update result: 6");
			List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
			LOGGER.info("=>Dropship Order product update result: 7");
			if (resultDataList.isEmpty()) {
				throw new OracleException(
						ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
			}
			LOGGER.info("=>Dropship Order product update result: 8");
			resultMap = format3(resultDataList.get(0));

			LOGGER.info("=>Dropship Order product update result: " + resultMap.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return resultMap;
	}
	
	public static void deleteByOrder(String orderId) throws SQLException {

		LOGGER.info("Dropship Delete order products with orderId=: " + orderId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PRD_DELETE_ALL,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.info("=>Dropship Order products delete result: " + resultCode);
	}
	
	public static void deleteByOrderCSVImport(String orderId) throws SQLException {

		LOGGER.info("Dropship Delete order products with orderId=: " + orderId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PRD_DELETE_ALL_CSV_IMPORT,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.info("=>Dropship Order products delete result: " + resultCode);
	}
	
	public static void deleteOrderItem(String id) throws SQLException {

		LOGGER.info("Dropship Delete order products with Id=: " + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PRD_DELETE_ITEM,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.info("=>Dropship Order products delete result: " + resultCode);
	}
	
	public static void updateShippingMethod(String orderId, String method) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, method);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		
		Map updateResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_UPDATE_SHIPPING_METHOD,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static Map getById(String id) throws SQLException {

		LOGGER.info("Get dropship order product by id=" + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PRD_GET_BY_ID, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		Map resultMap = formatDropshipOrderProduct(resultDataList.get(0));
		LOGGER.info("=> get dropship order product by id  result: " + resultMap.toString());

		return resultMap;
	}
	
	private static Map formatDropshipOrderProduct(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		resultMap.put(AppParams.BASE_ID, ParamUtil.getString(queryData, AppParams.S_BASE_ID));
		resultMap.put(AppParams.VARIANT_ID, ParamUtil.getString(queryData, AppParams.S_VARIANT_ID));
		resultMap.put(AppParams.VARIANT_NAME, ParamUtil.getString(queryData, AppParams.S_VARIANT_NAME));
		resultMap.put(AppParams.COLOR, ParamUtil.getString(queryData, AppParams.S_COLOR_VALUE));
		resultMap.put(AppParams.COLOR_ID, ParamUtil.getString(queryData, AppParams.S_COLOR_ID));
		resultMap.put(AppParams.COLOR_NAME, ParamUtil.getString(queryData, AppParams.S_COLOR_NAME));
		resultMap.put(AppParams.SIZE_ID, ParamUtil.getString(queryData, AppParams.S_SIZE_ID));
		resultMap.put(AppParams.SIZE_NAME, ParamUtil.getString(queryData, AppParams.S_SIZE_NAME));
		resultMap.put(AppParams.QUANTITY, ParamUtil.getString(queryData, AppParams.N_QUANTITY));
		resultMap.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));

		Map designMap = new LinkedHashMap<>();
		designMap.put(AppParams.MOCKUP_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_VARIANT_FRONT_URL));
		designMap.put(AppParams.MOCKUP_BACK_URL, ParamUtil.getString(queryData, AppParams.S_VARIANT_BACK_URL));
		designMap.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_FRONT_URL));
		designMap.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_BACK_URL));

		resultMap.put(AppParams.DESIGNS, designMap);

		return resultMap;
	}
	
	public static Map search(String orderId, String state) throws SQLException {

		LOGGER.info("Order item search with orderId=" + orderId + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_TOTAL);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PRD_SEARCH,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();
		Set<String> setOrderProductId = new HashSet<>();
		for (Map resultDataMap : resultDataList) {
			String id = ParamUtil.getString(resultDataMap, AppParams.S_ID);
			if(!setOrderProductId.contains(id)) {
				dataList.add(format3(resultDataMap));
				setOrderProductId.add(id);
			}
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.ITEMS, dataList);

		LOGGER.info("=> Order items search result: " + resultTotalRow);

		return resultMap;
	}
	
	private static Map format3(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		String id = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, id);

		Map campaignInfoMap = new LinkedHashMap<>();
		campaignInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));
		campaignInfoMap.put(AppParams.TITLE, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_TITLE));

		String campaignUrl = ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_URL);

		resultMap.put(AppParams.CAMPAIGN_TITLE, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_TITLE));
		resultMap.put(AppParams.CAMPAIGN_URL, campaignUrl);
		resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));

		campaignInfoMap.put(AppParams.URL, campaignUrl);
		campaignInfoMap.put(AppParams.END_TIME, ParamUtil.getString(queryData, AppParams.D_CAMPAIGN_END));
		campaignInfoMap.put(AppParams.DOMAIN, ParamUtil.getString(queryData, AppParams.S_DOMAIN));
		campaignInfoMap.put(AppParams.DOMAIN_ID, ParamUtil.getString(queryData, AppParams.S_DOMAIN_ID));

		resultMap.put(AppParams.CAMPAIGN, campaignInfoMap);

		resultMap.put(AppParams.PRODUCT_ID, ParamUtil.getString(queryData, AppParams.S_PRODUCT_ID));
		resultMap.put(AppParams.BASE_ID, ParamUtil.getString(queryData, AppParams.S_BASE_ID));
		resultMap.put(AppParams.VARIANT_ID, ParamUtil.getString(queryData, AppParams.S_VARIANT_ID));
		resultMap.put(AppParams.VARIANT_NAME, ParamUtil.getString(queryData, AppParams.S_VARIANT_NAME));

		resultMap.put(AppParams.SIZE_ID, ParamUtil.getString(queryData, AppParams.S_SIZE_ID));
		resultMap.put(AppParams.SIZE_NAME, ParamUtil.getString(queryData, AppParams.S_SIZE_NAME));

		resultMap.put(AppParams.COLOR_ID, ParamUtil.getString(queryData, AppParams.S_COLOR_ID));
		resultMap.put(AppParams.COLOR_NAME, ParamUtil.getString(queryData, AppParams.S_COLOR_NAME));
		resultMap.put(AppParams.COLOR, ParamUtil.getString(queryData, AppParams.S_COLOR_VALUE));

		boolean backView = ParamUtil.getBoolean(queryData, AppParams.N_BACK_VIEW);

		String variantImageUrl = backView ? ParamUtil.getString(queryData, AppParams.S_BACK_IMG_URL) : ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_URL);

		resultMap.put(AppParams.VARIANT_IMAGE, variantImageUrl);
		resultMap.put(AppParams.PRICE, ParamUtil.getString(queryData, AppParams.S_PRICE));
		resultMap.put(AppParams.CURRENCY, ParamUtil.getString(queryData, AppParams.S_CURRENCY));
		resultMap.put(AppParams.QUANTITY, ParamUtil.getInt(queryData, AppParams.N_QUANTITY));
		resultMap.put(AppParams.SHIPPING_FEE, ParamUtil.getString(queryData, AppParams.S_SHIPPING_FEE));
		resultMap.put(AppParams.AMOUNT, ParamUtil.getString(queryData, AppParams.S_AMOUNT));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_PRODUCT_STATE));
		
		resultMap.put(AppParams.BASE_COST, ParamUtil.getString(queryData, AppParams.S_BASE_COST));
		resultMap.put(AppParams.LINE_ITEM_ID, ParamUtil.getString(queryData, AppParams.S_LINE_ITEM_ID));
		resultMap.put(AppParams.SHIPPING_METHOD, ParamUtil.getString(queryData, AppParams.S_SHIPPING_METHOD));
		resultMap.put(AppParams.ITEM_TYPE, ParamUtil.getString(queryData, AppParams.S_ITEM_TYPE));
		resultMap.put(AppParams.PARTNER_PROPERTIES, ParamUtil.getString(queryData, AppParams.S_PARTNER_PROPERTIES));
		resultMap.put(AppParams.PARTNER_OPTION, ParamUtil.getString(queryData, AppParams.S_PARTNER_OPTION));
		resultMap.put(AppParams.PRINT_DETAIL, ParamUtil.getString(queryData, AppParams.S_PRINT_DETAIL));

		Map designMap = new LinkedHashMap<>();
		designMap.put(AppParams.MOCKUP_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_URL));
		designMap.put(AppParams.MOCKUP_BACK_URL, ParamUtil.getString(queryData, AppParams.S_BACK_IMG_URL));
		designMap.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_FRONT_URL));
		designMap.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_BACK_URL));
		resultMap.put(AppParams.DESIGNS, designMap);

		resultMap.put(AppParams.PARTNER_SKU, ParamUtil.getString(queryData, AppParams.S_PARTNER_SKU));
		resultMap.put(AppParams.PARTNER_URL, ParamUtil.getString(queryData, AppParams.S_PARTNER_URL));

		resultMap.put(AppParams.BASE_SHORT_CODE, ParamUtil.getString(queryData, AppParams.S_BASE_SHORT_CODE));
		resultMap.put(AppParams.UNIT_AMOUNT, ParamUtil.getString(queryData, AppParams.S_UNIT_AMOUNT));

		resultMap.put(AppParams.TAX, ParamUtil.getString(queryData, AppParams.S_TAX));
		resultMap.put(AppParams.TAX_AMOUNT, ParamUtil.getString(queryData, AppParams.S_TAX_AMOUNT));

		Map brandMap = new LinkedHashMap<>();
		String heat_press = ParamUtil.getString(queryData, AppParams.N_HEAT_PRESS);
		if(StringUtils.isNotEmpty(heat_press)) {
			Map heatPressMap = new LinkedHashMap<>();
			heatPressMap.put(AppParams.TITLE, "Heat press");
			heatPressMap.put(AppParams.TOTAL, heat_press);
			brandMap.put(AppParams.HEAT_PRESS, heatPressMap);
		}
		
		String brand_box = ParamUtil.getString(queryData, AppParams.N_BRAND_BOX);
		if(StringUtils.isNotEmpty(brand_box)) {
			Map brandBoxMap = new LinkedHashMap<>();
			brandBoxMap.put(AppParams.TITLE, "Box with brand name");
			brandBoxMap.put(AppParams.TOTAL, brand_box);
			brandMap.put(AppParams.BRAND_BOX, brandBoxMap);
		}
		
		String shipping_tag = ParamUtil.getString(queryData, AppParams.N_SHIPPING_TAG);
		if(StringUtils.isNotEmpty(shipping_tag)) {
			Map shippingTagMap = new LinkedHashMap<>();
			shippingTagMap.put(AppParams.TITLE, "Shipping tag");
			shippingTagMap.put(AppParams.TOTAL, shipping_tag);
			brandMap.put(AppParams.SHIPPING_TAG, shippingTagMap);
		
		}
		String thankyou_tag = ParamUtil.getString(queryData, AppParams.N_THANKYOU_TAG);
		if(StringUtils.isNotEmpty(thankyou_tag)) {
			Map thankyouTagMap = new LinkedHashMap<>();
			thankyouTagMap.put(AppParams.TITLE, "Thank you tag");
			thankyouTagMap.put(AppParams.TOTAL, thankyou_tag);
			brandMap.put(AppParams.THANKYOU_TAG, thankyouTagMap);
		}
		
		String giftcard = ParamUtil.getString(queryData, AppParams.N_GIFTCARD);
		if(StringUtils.isNotEmpty(giftcard)) {
			Map giftcardMap = new LinkedHashMap<>();
			giftcardMap.put(AppParams.TITLE, "Gift card");
			giftcardMap.put(AppParams.TOTAL, giftcard);
			brandMap.put(AppParams.GIFTCARD, giftcardMap);
		}
		
		resultMap.put(AppParams.BRAND, brandMap);
		
		if (!ParamUtil.getString(queryData, AppParams.S_TRACKING_ID).isEmpty()) {
			Map trackingInfoMap = new LinkedHashMap<>();
			trackingInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_TRACKING_ID));
			trackingInfoMap.put(AppParams.CODE, ParamUtil.getString(queryData, AppParams.S_TRACKING_CODE));
			trackingInfoMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_TRACKING_URL));
			trackingInfoMap.put(AppParams.INFORMATION, ParamUtil.getString(queryData, AppParams.S_TRACKING_INFO));
			trackingInfoMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_TRACKING_STATE));

			resultMap.put(AppParams.TRACKING, trackingInfoMap);
		}

		return resultMap;
	}
	
	public static Map getTopSellingProduct(String userId,String storeIds , String search, String startDate, String endDate , int page, int pageSize) throws SQLException {
		LOGGER.info("getTopSellingProduct input userId = " + userId + ", storeIds = " + storeIds + ", startDate = " + startDate + " , endDate = " + endDate+ ", page= " + page + ", size = " + pageSize);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, storeIds);
		inputParams.put(3, search);
		inputParams.put(4, startDate);
		inputParams.put(5, endDate);
		inputParams.put(6, page);
		inputParams.put(7, pageSize);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.VARCHAR);
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(8, AppParams.RESULT_CODE);
		outputParamsNames.put(9, AppParams.RESULT_MSG);
		outputParamsNames.put(10, AppParams.RESULT_TOTAL);
		outputParamsNames.put(11, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, GET_DROPSHIP_TOP_SELLING_PRODUCT,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> result = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		return resultMap;
	}

	public static Map updateEtsyOrder(String orderId, int totalItem, String orderPrefix, String source, String state) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, totalItem);
		inputParams.put(3, orderPrefix);
		inputParams.put(4, source);
		inputParams.put(5, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, UPDATE_ETSY_ORDER,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDatalist = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		return resultDatalist.get(0);
	}

	public static void insertEtsyProduct(DropshipOrderProductObj obj, String countryCode) throws SQLException {

		LOGGER.info("Inserting dropship order product " + obj.toString());

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, obj.getOrderId());
		inputParams.put(2, obj.getCampaignId());
		inputParams.put(3, obj.getProductId());
		inputParams.put(4, obj.getVariantId());
		inputParams.put(5, obj.getVariantName());
		inputParams.put(6, obj.getSizeId());
		inputParams.put(7, obj.getPrice());
		inputParams.put(8, obj.getCurrency());
		inputParams.put(9, obj.getQuantity());
		inputParams.put(10, obj.getShippingFee());
		inputParams.put(11, obj.getAmount());
		inputParams.put(12, obj.getBaseId());
		inputParams.put(13, obj.getBaseCost());
		inputParams.put(14, obj.getLineItemId());
		inputParams.put(15, obj.getColorName());
		inputParams.put(16, countryCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(17, OracleTypes.NUMBER);
		outputParamsTypes.put(18, OracleTypes.VARCHAR);
		outputParamsTypes.put(19, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(17, AppParams.RESULT_CODE);
		outputParamsNames.put(18, AppParams.RESULT_MSG);
		outputParamsNames.put(19, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, INSERT_PRODUCT,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

	}

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderProductService.class.getName());

}