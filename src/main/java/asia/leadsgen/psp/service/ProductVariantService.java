package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;
import org.apache.commons.lang.StringUtils;

/**
 * Created by hungdx on 4/1/17.
 */
public class ProductVariantService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	static final String GET_VARIANT_MAP_BY_ID_AND_SIZE_ID = "{call PKG_FF_PRODUCT_VARIANT.variant_get_by_id_and_size_id(?,?,?,?,?)}";
	public static final String VARIANT_GET = "{call PKG_PRODUCT_VARIANT.variant_get(?,?,?,?)}";
	public static final String VARIANT_GET_V2 = "{call PKG_PRODUCT_VARIANT.variant_get_v2(?,?,?,?)}";
	private static final String GET_VARIANT_INFO_TO_SHOPIFY_EXPORT = "{call PKG_FF_PRODUCT_VARIANT.get_variant_info_to_shopify_export(?,?,?,?)}";
	public static final String PRODUCT_VARIANTS_EXPORT = "{call PKG_FF_PRODUCT_VARIANT.product_variants_export(?,?,?,?,?)}";

	public static Map getAndCheckCampaignNotLocked(String id) throws SQLException {

		LOGGER.fine("Product Variant lookup with id=" + id);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, VARIANT_GET_V2, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			return new LinkedHashMap<>();
		}

		LOGGER.fine("=> User look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return format(resultDataList.get(0));
	}

	public static Map get(String id) throws SQLException {
		LOGGER.fine("Product Variant lookup with id=" + id);
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
		Map searchResultMap = DBProcedureUtil.execute(dataSource, VARIANT_GET, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return new LinkedHashMap<>();
		}
		LOGGER.fine("=> User look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		return format(resultDataList.get(0));
	}

	public static Map updateState(String id, String state) throws SQLException {
//		LOGGER.info("=> updateState id= " + id + ", state=" + state);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.VARIANT_UPDATE_STATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		LOGGER.fine("=> updateState result: " + resultMap.toString());
		return Collections.EMPTY_MAP;
	}

	public static Map search(String productId, String baseId, String colorId, String frontDesignId, String backDesignId,
			String state) throws SQLException {

		LOGGER.fine("Product variants search with productId=" + productId + ", baseId=" + baseId + ", colorId="
				+ colorId + ", frontDesignId=" + frontDesignId + ", backDesignId=" + backDesignId + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);
		inputParams.put(2, baseId);
		inputParams.put(3, colorId);
		inputParams.put(4, frontDesignId);
		inputParams.put(5, backDesignId);
		inputParams.put(6, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.VARCHAR);
		outputParamsTypes.put(9, OracleTypes.NUMBER);
		outputParamsTypes.put(10, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(7, AppParams.RESULT_CODE);
		outputParamsNames.put(8, AppParams.RESULT_MSG);
		outputParamsNames.put(9, AppParams.RESULT_TOTAL);
		outputParamsNames.put(10, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.VARIANT_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(format(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.VARIANTS, dataList);

		LOGGER.fine("=> Variants search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map insert(String name, String productId, String baseId, String colorId, String colorValue, String frontDesignId, String backDesignId, String frontImageUrl, String backImageUrl,
			boolean defaultVariant, int norder) throws SQLException {

		LOGGER.fine("Product variant insert with name=" + name + ", productId=" + productId + ", baseId=" + baseId
				+ ", colorId=" + colorId + ", colorValue=" + colorValue + ", frontDesignId=" + frontDesignId
				+ ", backDesignId=" + backDesignId + ", frontImageUrl=" + frontImageUrl + ", backImageUrl="
				+ backImageUrl + ", default=" + defaultVariant + ", norder=" + norder);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, name);
		inputParams.put(2, productId);
		inputParams.put(3, baseId);
		inputParams.put(4, colorId);
		inputParams.put(5, colorValue);
		inputParams.put(6, frontDesignId);
		inputParams.put(7, backDesignId);
		inputParams.put(8, frontImageUrl);
		inputParams.put(9, backImageUrl);
		inputParams.put(10, defaultVariant);
		inputParams.put(11, norder);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.VARIANT_INSERT, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> Variant insert result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static void delete(String id) throws SQLException {

		LOGGER.fine("Variant delete with id=" + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.VARIANT_DELETE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Variant delete result: " + ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
	}

	public static List<Map> getCampaignVariants(String campaignId, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.VARIANT_GET_BY_CAMP_ID, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

//		LOGGER.info("resultMap****: " + resultMap);

		return resultDataList.stream().parallel().map(o -> format(o)).collect(Collectors.toList());

	}

	public static List<Map> getProductVariantsToExport(String campaignId, String channel) throws SQLException {

		LOGGER.fine("Product Variant to Export with id=" + campaignId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, channel);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, PRODUCT_VARIANTS_EXPORT, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> result = new ArrayList<>();
		List<Map> listResultData = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		for (Map data : listResultData){
			result.add(format(data));
		}
		
		return result;
	}
	
	public static Map getVariantMapByIdAndSizeId(String productVariantIt, String sizeId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productVariantIt);
		inputParams.put(2, sizeId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_VARIANT_MAP_BY_ID_AND_SIZE_ID, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			return new LinkedHashMap<>();
		}

		LOGGER.info("VariantMap result: " + resultDataList);

		return format(resultDataList.get(0));
	}

	public static Map format(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		String id = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, id);

		resultMap.put(AppParams.PRODUCT_ID, ParamUtil.getString(queryData, AppParams.S_PRODUCT_ID));

		resultMap.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));

		resultMap.put(AppParams.BASE_COST, ParamUtil.getString(queryData, AppParams.S_BASE_COST));

		resultMap.put(AppParams.BASE_ID, ParamUtil.getString(queryData, AppParams.S_BASE_ID));

		resultMap.put(AppParams.BASE_SHORT_CODE, ParamUtil.getString(queryData, AppParams.S_BASE_SHORT_CODE));

		resultMap.put(AppParams.PRICE, ParamUtil.getString(queryData, AppParams.S_SALE_PRICE));

		String name = ParamUtil.getString(queryData, AppParams.S_NAME);

		resultMap.put(AppParams.NAME, name);

		String colorName = ParamUtil.getString(queryData, AppParams.S_COLOR_NAME);
		if (StringUtils.isEmpty(colorName)){
			colorName = name.substring(name.lastIndexOf("- ")+2);
		}

		resultMap.put(AppParams.COLOR_NAME, colorName);

		resultMap.put(AppParams.COLOR, ParamUtil.getString(queryData, AppParams.S_COLOR_VALUE));

		resultMap.put(AppParams.COLOR_ID, ParamUtil.getString(queryData, AppParams.S_COLOR_ID));

		resultMap.put(AppParams.FRONT_DESIGN_ID, ParamUtil.getString(queryData, AppParams.S_FRONT_DESIGN_ID));

		resultMap.put(AppParams.BACK_DESIGN_ID, ParamUtil.getString(queryData, AppParams.S_BACK_DESIGN_ID));

		Map imageInfoMap = new LinkedHashMap();
		imageInfoMap.put(AppParams.FRONT, ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_URL));

		imageInfoMap.put(AppParams.BACK, ParamUtil.getString(queryData, AppParams.S_BACK_IMG_URL));

		resultMap.put(AppParams.IMAGE, imageInfoMap);

		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.DEFAULT, ParamUtil.getBoolean(queryData, AppParams.N_DEFAULT));

		resultMap.put(AppParams.SIZE_NAME, ParamUtil.getString(queryData, AppParams.S_SIZE_NAME));
		resultMap.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_FRONT_URL));
		resultMap.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_BACK_URL));
		resultMap.put(AppParams.DROPSHIP_BASE_COST, ParamUtil.getString(queryData, AppParams.S_DROPSHIP_BASE_COST));

		resultMap.put(AppParams.PRODUCT_NAME, ParamUtil.getString(queryData, AppParams.S_PRODUCT_NAME));
		resultMap.put(AppParams.SIZE_ID, ParamUtil.getString(queryData, AppParams.S_SIZE_ID));
		resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		resultMap.put(AppParams.CAMPAIGN_TITLE, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_TITLE));
		resultMap.put(AppParams.PRODUCT_TYPE, ParamUtil.getString(queryData, AppParams.S_PRODUCT_TYPE));
		resultMap.put(AppParams.POSITION, ParamUtil.getString(queryData, AppParams.N_POSITION));
		resultMap.put(AppParams.VARIANT_ID, ParamUtil.getString(queryData, AppParams.S_VARIANT_ID));
		resultMap.put(AppParams.N_DESIGN_FRONT, ParamUtil.getString(queryData, AppParams.N_DESIGN_FRONT));
		return resultMap;
	}
	
	public static List<Map> getVariantInfoToShopifyExport(String campaignId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_VARIANT_INFO_TO_SHOPIFY_EXPORT, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		return resultDataList;
	}
	
	private static final Logger LOGGER = Logger.getLogger(ProductVariantService.class.getName());

}
