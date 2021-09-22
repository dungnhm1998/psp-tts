package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

import com.google.gson.Gson;

/**
 * Created by hungdx on 4/1/17.
 */
public class ProductDesignService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public static final String PRODUCT_DESIGN_INSERT_CAMP_V2 = "{call PKG_PRODUCT_DESIGN.product_design_insert_camp_v2(?,?,?,?,?)}";

	public static Map search(String productId, String designType) throws SQLException {

//		LOGGER.fine("Product design search with productId=" + productId + " and designType=" + designType);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);
		inputParams.put(2, designType);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_DESIGN_SEARCH, inputParams,
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
		resultMap.put(AppParams.DESIGNS, dataList);

		LOGGER.fine("=> Product design search result: " + resultMap.toString());

		return resultMap;
	}

	public static void insert(String productId, String designId, boolean mainProduct, String printPrice)
			throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);
		inputParams.put(2, designId);
		inputParams.put(3, mainProduct);
		inputParams.put(4, printPrice);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_DESIGN_INSERT, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}
	}

	public static List<Map> getAllProductsDesignsByCampId(String campId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campId);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_DESIGNS_BY_CAMP_ID, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		return resultDataList.stream().map(o -> format(o)).collect(Collectors.toList());

	}

	public static void updateProductDesigns(String campaignId, String productId) throws SQLException {

		LOGGER.fine("Update related products designs with campaignId=" + campaignId + " and productId=" + productId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, productId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_DESIGN_UPDATE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Product design insert result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
	}

	public static void setMainProductDesigns(String campaignId, String productId, String designId) throws SQLException {

		LOGGER.fine("Update campaign main product designs with campaignId=" + campaignId + ", productId=" + productId
				+ ", designId=" + designId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, productId);
		inputParams.put(3, designId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_DESIGN_SET_MAIN, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Product design set main result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
	}

	public static void productDesignDelete(String productId, String designId) throws SQLException {

		LOGGER.fine("Delete product design with productId=" + productId + " and designId=" + designId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);
		inputParams.put(2, designId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_DESIGN_DELETE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Product design delete result: " + resultCode);
	}

	public static void deleteProductDesigns(String productId, String designType) throws SQLException {

		LOGGER.fine("Delete all product designs with productId=" + productId + ", designType=" + designType);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);
		inputParams.put(2, designType);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_DESIGNS_DELETE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Delete result: " + resultCode);
	}

	public static void updateDesigns(String productId, String designIds) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);
		inputParams.put(2, designIds);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.UPDATE_DESIGNS, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Delete result: " + resultCode);
	}

	public static Map format(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_DESIGN_ID));
		resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_DESIGN_TYPE));
		resultMap.put(AppParams.ART_ID, ParamUtil.getString(queryData, AppParams.S_ART_ID));
		resultMap.put(AppParams.ART_PRICE_TYPE, ParamUtil.getString(queryData, AppParams.S_ART_PRICE_TYPE));
		resultMap.put(AppParams.ART_PRICE, ParamUtil.getString(queryData, AppParams.S_ART_PRICE));
		resultMap.put(AppParams.PRODUCT_ID, ParamUtil.getString(queryData, AppParams.S_PRODUCT_ID));
		resultMap.put("custom_texts", "");
		String customData = ParamUtil.getString(queryData, AppParams.S_CUSTOM_DATA);
		if (!StringUtils.isEmpty(customData)) {
			Map customDataMap = new Gson().fromJson(customData, Map.class);
			List<Map> customTextsList = ParamUtil.getListData(customDataMap, "custom_data");
			if (!CollectionUtils.isEmpty(customTextsList)) {
				String customTexts = new Gson().toJson(customTextsList);
				resultMap.replace("custom_texts", customTexts);
			} else {
				Map customTextsMapCamp3D = ParamUtil.getMapData(customDataMap, "custom_data");
				if (customTextsMapCamp3D != null && customTextsMapCamp3D.isEmpty() == false) {
					String customTextsCamp3D = new Gson().toJson(customTextsMapCamp3D);
					resultMap.replace("custom_texts", customTextsCamp3D);
				}
			}
		}

		if (!ParamUtil.getString(queryData, AppParams.S_IMAGE_ID).isEmpty()) {

			Map imageInfoMap = new LinkedHashMap();

			imageInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_IMAGE_ID));
			imageInfoMap.put(AppParams.POSITION, ParamUtil.getString(queryData, AppParams.N_IMAGE_POSITION));
			imageInfoMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_URL));
			imageInfoMap.put(AppParams.THUMB_URL, ParamUtil.getString(queryData, AppParams.S_PREVIEW));
			imageInfoMap.put(AppParams.WIDTH, ParamUtil.getString(queryData, AppParams.S_WIDTH));
			imageInfoMap.put(AppParams.HEIGHT, ParamUtil.getString(queryData, AppParams.S_HEIGHT));
			imageInfoMap.put(AppParams.CROP_GEOMETRY, ParamUtil.getString(queryData, AppParams.S_CROP_GEOMETRY));
			imageInfoMap.put(AppParams.PRINTABLE_TOP, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_TOP));
			imageInfoMap.put(AppParams.PRINTABLE_LEFT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_LEFT));
			imageInfoMap.put(AppParams.PRINTABLE_WIDTH, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_WIDTH));
			imageInfoMap.put(AppParams.PRINTABLE_HEIGHT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_HEIGHT));
			imageInfoMap.put(AppParams.ZINDEX, ParamUtil.getString(queryData, AppParams.S_Z_INDEX));

			resultMap.put(AppParams.IMAGE, imageInfoMap);
		}

		resultMap.put(AppParams.MAIN, ParamUtil.getBoolean(queryData, AppParams.N_MAIN));

		return resultMap;
	}
	
	public static void insertCampV2(String productId, String designId, boolean mainProduct) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);
		inputParams.put(2, designId);
		inputParams.put(3, mainProduct);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, PRODUCT_DESIGN_INSERT_CAMP_V2, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ProductDesignService.class.getName());

}
