package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.util.StringUtils;

import asia.leadsgen.psp.data.type.RedisKeyEnum;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.CategoryObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class CategoryService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	static final String SAVE_CATEGORY = "{call PKG_CATEGORY.save_category(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String GET_CATEGORY_NAME_BY_USER_ID_AND_DOMAIN = "{call PKG_CATEGORY.get_category_name_by_user_id_and_domain(?,?,?,?,?)}";
	static final String FIND_BY_ID = "{call PKG_CATEGORY.find_by_id(?,?,?,?,?)}";
	static final String FIND_BY_PARENT_ID = "{call PKG_CATEGORY.find_by_parent_id(?,?,?,?)}";
	static final String DELETE = "{call PKG_CATEGORY.delete(?,?,?,?)}";
	static final String FIND_LEVEL1_BY_USER_ID_AND_DOMAIN_ID = "{call PKG_CATEGORY.find_level1_by_user_id_and_domain_id(?,?,?,?,?)}";
	static final String SAVE_TAG_FOR_CUSTOM_MENU = "{call PKG_CATEGORY.save_tag_for_custom_menu(?,?,?,?,?,?)}";
	static final String FIND_BY_USER_ID_AND_DOMAIN_ID_WITHOUT_LEVELING = "{call PKG_CATEGORY.find_by_user_id_and_domain_id_without_leveling(?,?,?,?,?)}";
	static final String CHECK_EXISTENCE_OF_A_TAG = "{call PKG_CATEGORY.CHECK_EXISTENCE_OF_A_TAG(?,?,?,?,?,?,?)}";
	static final String TOGGLE_SHOW_ON_MENU_OPTION = "{call PKG_CATEGORY.TOGGLE_SHOW_ON_MENU_OPTION(?,?,?,?,?,?,?)}";

	public static Map search(String userId, String parentId, String name, int visible, String state)
			throws SQLException {

		LOGGER.info("Category search with parentId=" + parentId + ", name=" + name + ", visible=" + visible + ", state="
				+ state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, parentId);
		inputParams.put(2, name);
		inputParams.put(3, visible);
		inputParams.put(4, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_TOTAL);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		String query = StringUtils.isEmpty(userId) ? DBProcedurePool.CATEGORY_SEARCH
				: DBProcedurePool.CATEGORY_AFF_SEARCH;

		Map searchResultMap = DBProcedureUtil.execute(dataSource, query, inputParams, outputParamsTypes,
				outputParamsNames);

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
		resultMap.put(AppParams.CATEGORIES, dataList);

		LOGGER.info("=> Search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map insert(String parentId, String name, String desc, int visible, String state) throws SQLException {

		LOGGER.fine("Category insert with parentId=" + parentId + ", name=" + name + ", desc=" + desc + ", visible="
				+ visible + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, parentId);
		inputParams.put(2, name);
		inputParams.put(3, desc);
		inputParams.put(4, visible);
		inputParams.put(5, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CATEGORY_INSERT, inputParams,
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

		LOGGER.fine("=> Category insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Map insert(String parentId, String name, String desc, int visible, String state, String domainId)
			throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, parentId);
		inputParams.put(2, name);
		inputParams.put(3, desc);
		inputParams.put(4, visible);
		inputParams.put(5, state);
		inputParams.put(6, domainId);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.VARCHAR);
		outputParamsTypes.put(9, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(7, AppParams.RESULT_CODE);
		outputParamsNames.put(8, AppParams.RESULT_MSG);
		outputParamsNames.put(9, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CATEGORY_INSERT_BY_DOMAIN,
				inputParams, outputParamsTypes, outputParamsNames);

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

		LOGGER.fine("=> Category insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Map update(String id, String parentId, String name, String desc, int visible, String state)
			throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, parentId);
		inputParams.put(3, name);
		inputParams.put(4, desc);
		inputParams.put(5, visible);
		inputParams.put(6, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.VARCHAR);
		outputParamsTypes.put(9, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(7, AppParams.RESULT_CODE);
		outputParamsNames.put(8, AppParams.RESULT_MSG);
		outputParamsNames.put(9, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CATEGORY_UPDATE_BY_DOMAIN,
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

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> Category insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Map searchCategory(String userId, String domain, String parentId, int visible, String state)
			throws SQLException {

		LOGGER.info("Category search with parentId=" + parentId + ", userId=" + userId + ", visible=" + visible
				+ ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, parentId);
		inputParams.put(2, domain);
		inputParams.put(3, userId);
		inputParams.put(4, visible);
		inputParams.put(5, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_TOTAL);
		outputParamsNames.put(9, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CATEGORY_SEARCH_V2, inputParams,
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
		resultMap.put(AppParams.CATEGORIES, dataList);

		LOGGER.info("=> Search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map GetListByDomain(String domainId, String parentId, int visible, String state) throws SQLException {

		LOGGER.fine("Category search with parentId=" + parentId + ", domainId=" + domainId + ", visible=" + visible
				+ ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, parentId);
		inputParams.put(2, domainId);
		inputParams.put(3, visible);
		inputParams.put(4, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.CURSOR);
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_TOTAL);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CATEGORY_GET_LIST_BY_DOMAIN,
				inputParams, outputParamsTypes, outputParamsNames);

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
		resultMap.put(AppParams.CATEGORIES, dataList);

		LOGGER.fine("=> Search result: " + resultTotalRow);

		return resultMap;
	}

	private static Map format(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		if (!ParamUtil.getString(queryData, AppParams.S_PARENT_ID).isEmpty()) {
			resultMap.put(AppParams.PARENT_ID, ParamUtil.getString(queryData, AppParams.S_PARENT_ID));
		}

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		String uri = ParamUtil.getString(queryData, AppParams.S_URI);

//		if (uri != null && !uri.isEmpty() && !uri.startsWith(StringPool.FORWARD_SLASH)) {
//			uri = StringPool.FORWARD_SLASH + uri;
//		}

		resultMap.put(AppParams.URL, uri);

		resultMap.put(AppParams.VISIBLE, ParamUtil.getString(queryData, AppParams.N_VISIBLE));

		resultMap.put(AppParams.NUMBER_PRODUCTS, ParamUtil.getString(queryData, AppParams.N_PRODUCTS));

		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.PARENT_ID, ParamUtil.getString(queryData, AppParams.S_PARTNER_ID));

		return resultMap;
	}

	public static Map getCategoryName() throws SQLException {

		Map inputParams = new LinkedHashMap<>();

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(1, OracleTypes.NUMBER);
		outputParamsTypes.put(2, OracleTypes.VARCHAR);
		outputParamsTypes.put(3, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(1, AppParams.RESULT_CODE);
		outputParamsNames.put(2, AppParams.RESULT_MSG);
		outputParamsNames.put(3, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.GET_CATEGORY_NAME, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(resultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map<String, String> categoryName = new HashMap<String, String>();

		for (Map data : resultDataList) {
			categoryName.put(ParamUtil.getString(data, AppParams.S_ID), ParamUtil.getString(data, AppParams.S_NAME));
		}

		return categoryName;
	}

	public static Map getCategoryNameCache() throws SQLException {
		Map redis = RedisService.getMap(RedisKeyEnum.CATEGORY_NAMES_MAP);
		if (redis == null || redis.isEmpty()) {
			redis = getCategoryNameDb();
		}
		return redis;
	}
	
	public static Map getCategoryNameDbByUserIdAndDomainID(String user_id, String domain_id) throws SQLException {
		LOGGER.info("user_id= " + user_id + " -- domain_id= " + domain_id);
		Map inputParams = new LinkedHashMap<>();
		inputParams.put(1, user_id);
		inputParams.put(2, domain_id);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_CATEGORY_NAME_BY_USER_ID_AND_DOMAIN, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

//		if (resultDataList.isEmpty()) {
//			throw new OracleException(
//					ParamUtil.getString(resultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
//		}

		Map<String, String> categoryName = new HashMap<String, String>();

		for (Map data : resultDataList) {
			categoryName.put(ParamUtil.getString(data, AppParams.S_ID), ParamUtil.getString(data, AppParams.S_NAME));
		}

		return categoryName;
	}

	private static Map getCategoryNameDb() throws SQLException {
		Map allCategoryNameMap = CategoryService.getCategoryName();

		return RedisService.persistMap(RedisKeyEnum.CATEGORY_NAMES_MAP, allCategoryNameMap);
	}

	/**
	 * @author liamle
	 * @param categoryObj
	 * @return CategoryObj
	 * @throws Exception
	 */
	public static CategoryObj save(CategoryObj categoryObj) throws Exception {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, categoryObj.getId());
		inputParams.put(2, categoryObj.getParentId());
		inputParams.put(3, categoryObj.getName());
		inputParams.put(4, categoryObj.getDesc());
		inputParams.put(5, categoryObj.getVisible());
		inputParams.put(6, categoryObj.getState());
		inputParams.put(7, categoryObj.getDomainId());
		inputParams.put(8, categoryObj.getDomain());
		inputParams.put(9, categoryObj.getUserId());
		inputParams.put(10, AppUtil.generateFriendlyUrl(categoryObj.getName()));
		inputParams.put(11, categoryObj.getPosition());

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, SAVE_CATEGORY, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		CategoryObj obj = CategoryObj.fromMap(resultDataList.get(0));

		LOGGER.fine("=> Category insert result: " + obj.toString());

		return obj;
	}

	/**
	 * @author liamle
	 * @param categoryId
	 * @return
	 * @throws SQLException
	 */
	public static void delete(String categoryId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, categoryId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DELETE, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_CATEGORY);
		}

		CategoryObj obj = CategoryObj.fromMap(resultDataList.get(0));

		if (!ResourceStates.DELETED.equalsIgnoreCase(obj.getState())) {
			throw new BadRequestException(SystemError.INTERNAL_SERVER_ERROR);
		}

		LOGGER.fine("=> Category deleted result: " + obj.toString());

	}

	/**
	 * @author liamle
	 * @param categoryId
	 * @return CategoryObj
	 * @throws SQLException
	 */
	public static CategoryObj findById(String categoryId, String userId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, categoryId);
		inputParams.put(2, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, FIND_BY_ID, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(resultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		CategoryObj obj = CategoryObj.fromMap(resultDataList.get(0));

		if (obj.getParentId() == null || obj.getParentId().isEmpty()) {
			obj.setSubCategories(findByParentId(obj.getId()));
		}

		LOGGER.fine("=> Category findById result: " + obj.toString());

		return obj;
	}

	/**
	 * @author liamle
	 * @param parentId
	 * @param userId
	 * @param domainId
	 * @return CategoryObj
	 * @throws SQLException
	 */
	private static List<CategoryObj> findByParentId(String parentId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, parentId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, FIND_BY_PARENT_ID, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		return resultDataList.stream().map(o -> CategoryObj.fromMap(o)).collect(Collectors.toList());

	}

	/**
	 * @author liamle
	 * @param categoryId
	 * @return CategoryObj
	 * @throws SQLException
	 */
//	public static CategoryObj findByDomainId(String categoryId) throws SQLException {
//
//		Map inputParams = new LinkedHashMap<Integer, String>();
//		inputParams.put(1, categoryId);
//
//		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
//		outputParamsTypes.put(2, OracleTypes.NUMBER);
//		outputParamsTypes.put(3, OracleTypes.VARCHAR);
//		outputParamsTypes.put(4, OracleTypes.CURSOR);
//
//		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
//		outputParamsNames.put(2, AppParams.RESULT_CODE);
//		outputParamsNames.put(3, AppParams.RESULT_MSG);
//		outputParamsNames.put(4, AppParams.RESULT_DATA);
//
//		Map resultMap = DBProcedureUtil.execute(dataSource, FIND_BY_ID, inputParams, outputParamsTypes,
//				outputParamsNames);
//
//		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
//
//		if (resultCode != HttpResponseStatus.OK.code()) {
//			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
//		}
//
//		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
//
//		if (resultDataList.isEmpty()) {
//			throw new OracleException(
//					ParamUtil.getString(resultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
//		}
//
//		CategoryObj obj = CategoryObj.fromMap(resultDataList.get(0));
//
//		LOGGER.fine("=> Category findById result: " + obj.toString());
//
//		return obj;
//	}

	public static List<CategoryObj> findByUserIdAndDomainId(String userId, String domainId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domainId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, FIND_LEVEL1_BY_USER_ID_AND_DOMAIN_ID, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<CategoryObj> objs = new ArrayList<CategoryObj>();

		for (Map resultdata : resultDataList) {
			CategoryObj c = CategoryObj.fromMap(resultdata);
			c.setSubCategories(findByParentId(c.getId()));
			objs.add(c);
		}

		return objs;
	}

	public static List<CategoryObj> findByUserIdAndDomainIdWithoutLeveling(String userId, String domainId)
			throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domainId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, FIND_BY_USER_ID_AND_DOMAIN_ID_WITHOUT_LEVELING, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<CategoryObj> objs = new ArrayList<CategoryObj>();

		for (Map resultdata : resultDataList) {
			CategoryObj c = CategoryObj.fromMap(resultdata);
			objs.add(c);
		}

		return objs;
	}

	public static void saveTagForCustomMenu(CategoryObj c) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, c.getId());
		inputParams.put(2, c.getParentId());
		inputParams.put(3, c.getPosition());

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, SAVE_TAG_FOR_CUSTOM_MENU, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

	}

	public static boolean isExistTag(String id, String name, String domainId, String userId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, name);
		inputParams.put(3, domainId);
		inputParams.put(4, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, CHECK_EXISTENCE_OF_A_TAG, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

//		int totalRow = ParamUtil.getInt(resultDataList.get(0), AppParams.N_TOTAL);
		int totalRow = Integer.parseInt(ParamUtil.getString(resultDataList.get(0), AppParams.N_TOTAL));
		return totalRow > 0;
	}

	public static void toggleShowOnMenuOption(String domainId, String userId, int currentState, int expectState)
			throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, userId);
		inputParams.put(3, currentState);
		inputParams.put(4, expectState);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, TOGGLE_SHOW_ON_MENU_OPTION, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

	}

	private static final Logger LOGGER = Logger.getLogger(CategoryService.class.getName());

}
