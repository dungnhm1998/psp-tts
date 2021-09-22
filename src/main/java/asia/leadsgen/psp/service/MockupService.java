package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
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
import asia.leadsgen.psp.util.DimensionUnits;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class MockupService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static Map categorySearch(String parentId) throws SQLException {

		LOGGER.fine("Mockup categories search with parentId=" + parentId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, parentId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_TOTAL);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MOCKUP_CATEGORIES_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(categoryFormat(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.MOCKUP_CATEGORIES, dataList);

		LOGGER.fine("=> Mock up categories search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map typeSearch(String baseTypeId, String parentId) throws SQLException {

		LOGGER.fine("Mockup types search with baseTypeId=" + baseTypeId + " and parentId=" + parentId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, baseTypeId);
		inputParams.put(2, parentId);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MOCKUP_TYPES_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(typeFormat(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.MOCKUP_TYPES, dataList);

		LOGGER.fine("=> Mock up types search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map templateSearch(String typeId, String categories) throws SQLException {

		LOGGER.fine("Mockup template search with typeId=" + typeId + " and categories=" + categories);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, typeId);
		inputParams.put(2, categories);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MOCKUP_TEMPLATES_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(templateFormat(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.TEMPLATES, dataList);

		LOGGER.fine("=> Mock up templates search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map temlateGet(String id) throws SQLException {

		LOGGER.fine("Mockup template get with id=" + id);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.TEMPLATE_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		LOGGER.fine("=> Mockup Template look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return templateFormat(resultDataList.get(0));
	}

	/**
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */

	public static Map get(String id) throws SQLException {

		LOGGER.fine("Product Variant m lookup with id=" + id);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MOCKUP_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		LOGGER.fine("=> Mockup look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return format(resultDataList.get(0));
	}

	public static Map search(String variantId, String state) throws SQLException {

		LOGGER.fine("Product variants mock up search with variantId=" + variantId + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, variantId);
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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MOCKUP_SEARCH, inputParams,
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
		resultMap.put(AppParams.MOCKUPS, dataList);

		LOGGER.fine("=> Variants mock up search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map insert(String variantId, String campaignId, String type, String imageUrl) throws SQLException {

		LOGGER.fine("Product variant insert with variantId=" + variantId + ", type=" + type + ", imageUrl=" + imageUrl);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, variantId);
		inputParams.put(2, campaignId);
		inputParams.put(3, type);
		inputParams.put(4, imageUrl);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MOCKUP_INSERT, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> Variant mock up insert result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static void delete(String id) throws SQLException {

		LOGGER.fine("Variant mock up delete with id=" + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MOCKUP_DELETE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Variant delete result: " + ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
	}

	public static List<Map> getMockupByCampaignId(String campaignId) throws SQLException {

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

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MOCKUP_SEARCH_BY_CAMP_ID, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		return resultDataList.stream().map(o -> format(o)).collect(Collectors.toList());

	}

	public static Map format(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.VARIANT_ID, ParamUtil.getString(queryData, AppParams.S_VARIANT_ID));
		resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));
		resultMap.put(AppParams.PRODUCT_ID, ParamUtil.getString(queryData, AppParams.S_PRODUCT_ID));
		resultMap.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));

		Map imageInfoMap = new LinkedHashMap();
		imageInfoMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_IMAGE_URL));

		resultMap.put(AppParams.IMAGE, imageInfoMap);

		return resultMap;
	}

	private static Map categoryFormat(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		if (!ParamUtil.getString(queryData, AppParams.S_PARENT_ID).isEmpty()) {
			resultMap.put(AppParams.PARENT_ID, ParamUtil.getString(queryData, AppParams.S_PARENT_ID));
		}

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		if (!ParamUtil.getString(queryData, AppParams.S_DESC).isEmpty()) {
			resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		}

		resultMap.put(AppParams.POSITION, ParamUtil.getInt(queryData, AppParams.N_POSITION));

		return resultMap;
	}

	private static Map typeFormat(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		if (!ParamUtil.getString(queryData, AppParams.S_PARENT_ID).isEmpty()) {
			resultMap.put(AppParams.PARENT_ID, ParamUtil.getString(queryData, AppParams.S_PARENT_ID));
		}

		if (!ParamUtil.getString(queryData, AppParams.S_BASE_TYPE_ID).isEmpty()) {
			Map baseTypeInfoMap = new LinkedHashMap<>();
			baseTypeInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_BASE_TYPE_ID));
			baseTypeInfoMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_BASE_TYPE_NAME));

			resultMap.put(AppParams.BASE_TYPE, baseTypeInfoMap);
		}

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		if (!ParamUtil.getString(queryData, AppParams.S_DESC).isEmpty()) {
			resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		}

		resultMap.put(AppParams.POSITION, ParamUtil.getInt(queryData, AppParams.N_POSITION));

		return resultMap;
	}

	private static Map templateFormat(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		Map typeMap = new LinkedHashMap();
		typeMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_TYPE_ID));
		typeMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_TYPE_NAME));

		resultMap.put(AppParams.TYPE, typeMap);

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		if (!ParamUtil.getString(queryData, AppParams.S_DESC).isEmpty()) {
			resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		}

		Map templateFrontImageInfoMap = new LinkedHashMap();
		templateFrontImageInfoMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_URL));
		templateFrontImageInfoMap.put(AppParams.WIDTH, ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_WIDTH));
		templateFrontImageInfoMap.put(AppParams.HEIGHT, ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_HEIGHT));

		Map templateBackImageInfoMap = new LinkedHashMap();
		templateBackImageInfoMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_BACK_IMG_URL));
		templateBackImageInfoMap.put(AppParams.WIDTH, ParamUtil.getString(queryData, AppParams.S_BACK_IMG_WIDTH));
		templateBackImageInfoMap.put(AppParams.HEIGHT, ParamUtil.getString(queryData, AppParams.S_BACK_IMG_HEIGHT));

		Map templateImageInfoMap = new LinkedHashMap();
		templateImageInfoMap.put(AppParams.FRONT, templateFrontImageInfoMap);
		templateImageInfoMap.put(AppParams.BACK, templateBackImageInfoMap);

		resultMap.put(AppParams.IMAGE, templateImageInfoMap);

		Map printableInfoMap = new LinkedHashMap();

		printableInfoMap.put(AppParams.FRONT_TOP, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_TOP));

		printableInfoMap.put(AppParams.FRONT_LEFT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_LEFT));

		printableInfoMap.put(AppParams.FRONT_WIDTH, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_WIDTH));

		printableInfoMap.put(AppParams.FRONT_HEIGHT,
				ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_HEIGHT));

		printableInfoMap.put(AppParams.BACK_TOP, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_TOP));

		printableInfoMap.put(AppParams.BACK_LEFT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_LEFT));

		printableInfoMap.put(AppParams.BACK_WIDTH, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_WIDTH));

		printableInfoMap.put(AppParams.BACK_HEIGHT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_HEIGHT));

		printableInfoMap.put(AppParams.UNIT, ParamUtil.getString(queryData, AppParams.S_UNIT, DimensionUnits.PIXEL));

		resultMap.put(AppParams.PRINTABLE, printableInfoMap);

		return resultMap;
	}

	public static String campainURlSearch(String mockupId) throws SQLException {

		LOGGER.fine("Search campaign with mockup id=" + mockupId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, mockupId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_URL_MOCKUP_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

		List<Map> results = ParamUtil.getListData(updateResultMap, AppParams.RESULT_DATA);
		if (results.isEmpty()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

		String campaignUri = ParamUtil.getString(results.get(0), AppParams.S_URI);
		LOGGER.fine("=> campaign uri: " + campaignUri);

		return campaignUri;

	}

	public static void alloverUpdate(String mockupId, String mockupImageUrl) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, mockupId);
		inputParams.put(2, mockupImageUrl);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MOCKUP_ALLOVER_UPDATE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

	}
	
	

	private static final Logger LOGGER = Logger.getLogger(MockupService.class.getName());

}
