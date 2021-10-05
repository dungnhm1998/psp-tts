package asia.leadsgen.psp.service_fulfill;

/**
* @author liamle
* @param id
* @return Base Information
* @throws SQLException
*/

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import asia.leadsgen.psp.data.type.RedisKeyEnum;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.BasePhoneCaseUtil;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.DimensionUnits;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class BaseService extends MasterService {

	static final String REDIS_PREFIX = "psp_base_";

	private static RedisTemplate redisTemplate;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static void setRedisTemplate(RedisTemplate redisTemplate) {
		BaseService.redisTemplate = redisTemplate;
	}

	private static Map<String, String> baseSizeMap;

	static final String colorsDropshipBaseIds = "LCV9OQEvNZqeuX3D,i8ja02GzNr57tTwZ,WWQ7xQ2aYSa0XUEC,EOCuz3dpn8DqSVPB,NIUJTUQ0BwZ473QF,wg1fgLbO77j2WKky,hUeboM3JbQW9lXRK,j1e2h3Wq4Gz5ksjs,-cwc847TMWTaZmVg,WS-CFAJxOx1edtrB,N8qXaylZ2nanzTim";

	static final String FF_COMPUTE_SIZEMAP = "{call PKG_FF_BASE_SIZE.compute_sizemap(?,?,?)}";
	static final String FF_GET_SIZE_AND_PRICE_BY_BASE_ID = "{call PKG_FF_BASE_SIZE.get_size_and_price_by_base_id(?,?,?,?)}";
	static final String FF_GET_DROPSHIP_BASE_COST = "{call PKG_FF_BASE.get_dropship_base_cost(?,?,?,?,?,?)}";
	static final String FF_GET_ALL_BASE_AND_BASE_GROUP_NAME = "{call PKG_FF_BASE.get_all_base_and_base_group_name(?,?,?)}";
	static final String DROPSHIP_BASE_GET = "{call PKG_FF_BASE.dropship_base_get(?,?,?,?)}";
	static final String BASE_GET = "{call PKG_BASE.base_get(?,?,?,?)}";
	static final String GET_BASE_BASE_TYPE_AND_BASE_GROUP_NAME = "{call PKG_BASE.get_base_base_type_and_base_group_name(?,?,?)}";
	static final String DROPSHIP_GET_BASE_BASE_TYPE_AND_BASE_GROUP_NAME = "{call PKG_FF_BASE.get_base_base_type_and_base_group_name(?,?,?)}";
	static final String FF_GET_SIZE_AND_PRICE_AND_DESIGN_INFO_BY_BASE_ID = "{call PKG_FF_BASE_SIZE.get_size_and_price_and_design_info_by_base_id(?,?,?,?,?)}";
	
	private static final String GET_CATALOGS = "{call pkg_ff_dropship_base.get_catalogs(?,?,?)}";
	private static final String GET_CATALOG_BY_BASE_ID = "{call pkg_ff_dropship_base.get_catalog_by_base_id(?,?,?,?)}";	
	private static final String GET_NEW_BASE = "{call pkg_ff_dropship_base.get_new_base(?,?,?)}";
	
	public static Map get(String id) throws SQLException {

		Map resultMap = RedisService.get(REDIS_PREFIX + id);
		if (resultMap == null || resultMap.isEmpty()) {
			List<Map> resultDataList = MasterService.excuteQuery(DROPSHIP_BASE_GET, new Object[] { id });
			if (resultDataList.isEmpty()) {
				resultDataList = MasterService.excuteQuery(BASE_GET, new Object[] { id });
				if (resultDataList.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_PRODUCT_BASE);
				}
			}
			resultMap = format(resultDataList.get(0));
			RedisService.persist(REDIS_PREFIX + id, resultMap); // cached in Redis with key like psp_base_*
		}

		return resultMap;
	}

	public static double getDropshipBaseCost(String base_id, String size_id, int isTwoDesigns) throws SQLException {

		LOGGER.fine("Base look up with base_id=" + base_id + "---size=" + size_id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, base_id);
		inputParams.put(2, size_id);
		inputParams.put(3, isTwoDesigns);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, FF_GET_DROPSHIP_BASE_COST, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_PRODUCT);
		}

		return ParamUtil.getFormatedDouble(resultDataList.get(0), AppParams.S_DROPSHIP_BASE_COST, 2);
	}

	private static Map formatBaseIdAndName(List<Map> resultDataList) {
		Map<String, String> keysValues = new LinkedHashMap<>();
		if (resultDataList != null && resultDataList.size() > 0) {
			for (Map data : resultDataList) {
				String key = ParamUtil.getString(data, AppParams.S_NAME);
				String value = ParamUtil.getString(data, AppParams.S_ID);
				keysValues.put(key, value);
			}
		}
		return keysValues;
	}

	private static Map format(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		String baseId = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, baseId);

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		Map typeMap = new LinkedHashMap();
		typeMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_TYPE_ID));
		typeMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_TYPE_NAME));
		resultMap.put(AppParams.TYPE, typeMap);

		resultMap.put(AppParams.DISPLAY_NAME, ParamUtil.getString(queryData, AppParams.S_DISPLAY_NAME));

		if (!ParamUtil.getString(queryData, AppParams.S_DESC).isEmpty()) {
			resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		}

		resultMap.put(AppParams.PRICE, ParamUtil.getString(queryData, AppParams.S_PRICE));

		resultMap.put(AppParams.CURRENCY, ParamUtil.getString(queryData, AppParams.S_CURRENCY));

		String baseSizeIds = ParamUtil.getString(queryData, AppParams.S_SIZES);
		resultMap.put(AppParams.SIZES, baseSizeIds);

		resultMap.put(AppParams.DESIGN_GROUP, ParamUtil.getString(queryData, AppParams.S_DESIGN_GROUP));

		String baseColorIds = ParamUtil.getString(queryData, AppParams.S_COLORS);
		List<Map> allBaseColor = new ArrayList<>();

		if (!baseColorIds.isEmpty()) {
			resultMap.put(AppParams.COLORS, ProductUtil.getBaseColorList(baseColorIds, ""));
		}

		resultMap.put(AppParams.SIZE_PRICE_EDITABLE, ParamUtil.getBoolean(queryData, AppParams.N_EDIT_SIZE_PRICE));

		Map baseImageInfoMap = new LinkedHashMap();

		baseImageInfoMap.put(AppParams.ICON, ParamUtil.getString(queryData, AppParams.S_ICON_IMG_URL));

		baseImageInfoMap.put(AppParams.FRONT, ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_URL));

		baseImageInfoMap.put(AppParams.BACK, ParamUtil.getString(queryData, AppParams.S_BACK_IMG_URL));

		baseImageInfoMap.put(AppParams.WIDTH, ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_WIDTH));

		baseImageInfoMap.put(AppParams.HEIGHT, ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_HEIGHT));

		baseImageInfoMap.put(AppParams.UNIT, ParamUtil.getString(queryData, AppParams.S_UNIT, DimensionUnits.PIXEL));

		baseImageInfoMap.put("display_image", ParamUtil.getString(queryData, "S_SHOPIFY_DISPLAY_IMG"));

		resultMap.put(AppParams.IMAGE, baseImageInfoMap);

		resultMap.put(AppParams.FULL_FILLMENT, ParamUtil.getBoolean(queryData, AppParams.N_FULFILLMENT));

		Map printableInfoMap = new LinkedHashMap();

		printableInfoMap.put(AppParams.FRONT_TOP, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_TOP));

		printableInfoMap.put(AppParams.FRONT_LEFT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_LEFT));

		printableInfoMap.put(AppParams.FRONT_WIDTH, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_WIDTH));

		printableInfoMap.put(AppParams.FRONT_HEIGHT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_HEIGHT));

		printableInfoMap.put(AppParams.BACK_TOP, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_TOP));

		printableInfoMap.put(AppParams.BACK_LEFT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_LEFT));

		printableInfoMap.put(AppParams.BACK_WIDTH, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_WIDTH));

		printableInfoMap.put(AppParams.BACK_HEIGHT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_HEIGHT));

		printableInfoMap.put(AppParams.UNIT, ParamUtil.getString(queryData, AppParams.S_UNIT, DimensionUnits.PIXEL));

		resultMap.put(AppParams.PRINTABLE, printableInfoMap);

		Map<String, Object> dimension = new HashMap<String, Object>();
		dimension.put(AppParams.WIDTH, ParamUtil.getString(queryData, AppParams.S_DIMENSION_WIDTH));
		dimension.put(AppParams.HEIGHT, ParamUtil.getString(queryData, AppParams.S_DIMENSION_HEIGHT));

		resultMap.put(AppParams.DIMENSION, dimension);

		resultMap.put(AppParams.POSITION, ParamUtil.getString(queryData, AppParams.N_POSITION));

		resultMap.put(AppParams.DESIGN_TYPE, ParamUtil.getString(queryData, AppParams.S_DESIGN_TYPE));

		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

		resultMap.put(AppParams.SHORT_CODE, ParamUtil.getString(queryData, AppParams.S_SHORT_CODE));

		resultMap.put("display", ParamUtil.getBoolean(queryData, "N_PHONECASE_DISPLAY"));
		
		resultMap.put(AppParams.HTML_DESC, ParamUtil.getString(queryData, AppParams.S_HTML_DESC));

		return resultMap;
	}

	public static Map<String, String> getBaseSizeMap() throws SQLException {
		Map redis = RedisService.getMap(RedisKeyEnum.BASE_SIZE_MAP);
		if (redis == null || redis.isEmpty()) {
			redis = computeSizeMap();
		}
		return redis;
	}

	public static Map<String, String> computeSizeMap() throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(1, OracleTypes.NUMBER);
		outputParamsTypes.put(2, OracleTypes.VARCHAR);
		outputParamsTypes.put(3, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(1, AppParams.RESULT_CODE);
		outputParamsNames.put(2, AppParams.RESULT_MSG);
		outputParamsNames.put(3, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, FF_COMPUTE_SIZEMAP, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		Map<String, String> sizeMap = new HashMap<String, String>();

		List<Map> dataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		for (Map data : dataList) {
			sizeMap.put(ParamUtil.getString(data, AppParams.S_ID), ParamUtil.getString(data, AppParams.S_NAME));
		}

		return RedisService.persistMap(RedisKeyEnum.BASE_SIZE_MAP, sizeMap);
	}

	public static List<Map> getBaseBaseTypeAndBaseGroupName(boolean toCreateOrderDropship) throws SQLException {
		
		if (!toCreateOrderDropship) {
			List<Map> rsList = searchAll(GET_BASE_BASE_TYPE_AND_BASE_GROUP_NAME, new Object[] {});
			
			return rsList.stream().map(o -> {
				Map m = new LinkedHashMap<>();
				m.put(AppParams.GROUP_ID, ParamUtil.getString(o, AppParams.S_GROUP_ID));
				m.put("base_group_name", ParamUtil.getString(o, "S_BASE_GROUP_NAME"));
				m.put(AppParams.BASE_ID, ParamUtil.getString(o, AppParams.S_BASE_ID));
				m.put(AppParams.BASE_NAME, ParamUtil.getString(o, AppParams.S_BASE_NAME));
				return m;
			}).collect(Collectors.toList());
			
		} else {
			List<Map> rsList = searchAll(DROPSHIP_GET_BASE_BASE_TYPE_AND_BASE_GROUP_NAME, new Object[] {});
			
			return rsList.stream().map(o -> {
				Map m = new LinkedHashMap<>();
				m.put(AppParams.GROUP_ID, ParamUtil.getString(o, AppParams.S_GROUP_ID));
				m.put("base_group_name", ParamUtil.getString(o, "S_BASE_GROUP_NAME"));
				m.put(AppParams.BASE_ID, ParamUtil.getString(o, AppParams.S_BASE_ID));
				m.put(AppParams.BASE_NAME, ParamUtil.getString(o, AppParams.S_BASE_NAME));
				return m;
			}).collect(Collectors.toList());
		}
		
	}

	public static Map getBaseBaseTypeAndBaseGroupDb(boolean toCreateOrderDropship) throws SQLException {
		
		List<Map> allBaseGroup = new ArrayList<Map>();
		
		List<Map> listBase = getBaseBaseTypeAndBaseGroupName(toCreateOrderDropship);
		
		Set<String> listGroupId = listBase.stream().map(s -> ParamUtil.getString(s, AppParams.GROUP_ID)).collect(Collectors.toSet());

		for (String baseGroupId : listGroupId) {
			
			Map baseMap = new LinkedHashMap<>();
			baseMap.put("id", baseGroupId);
			List<Map> childrenList = listBase.stream().filter(s -> ParamUtil.getString(s, AppParams.GROUP_ID).equals(baseGroupId)).map(s -> {
				Map o = s;
				o.remove(AppParams.GROUP_ID);
				return o;
			}).collect(Collectors.toList());
			baseMap.put("name", childrenList.get(0).get("base_group_name"));
			
			List<Map> formatChildrenList = new ArrayList<Map>();
			for (Map children : childrenList) {
				Map formatChildren = new LinkedHashMap<>();
				String baseId = ParamUtil.getString(children, AppParams.BASE_ID);
				formatChildren = BaseService.get(baseId);
				
				List<Map> baseSizePhoneCase = new ArrayList<Map>();
				String basePhoneCaseIds = "";
				List<String> basePhoneCaseIdList = new ArrayList<>();
				
				if (toCreateOrderDropship) {
					if (BasePhoneCaseUtil.isPhoneCaseDropship(baseId)) {
						basePhoneCaseIds = BasePhoneCaseUtil.getBasePhoneCaseDropshipIds();
						basePhoneCaseIdList = Arrays.asList(basePhoneCaseIds.trim().split(","));
						for (String basePhoneCaseId : basePhoneCaseIdList) {
							List<Map> baseSize = listBaseSizeAndPriceAndDesignPhoneCaseInfo(basePhoneCaseId, toCreateOrderDropship);
							baseSizePhoneCase.addAll(baseSize);
						}
						formatChildren.put("base_size", baseSizePhoneCase);
					} else {
						List<Map> baseSizeList = listBaseSizeAndPriceAndDesignPhoneCaseInfo(baseId, toCreateOrderDropship);
						formatChildren.put("base_size", baseSizeList);
					}
				} else {
					if (BasePhoneCaseUtil.isPhoneCase(baseId)) {
						basePhoneCaseIds = BasePhoneCaseUtil.getBasePhoneCaseIds();
						basePhoneCaseIdList = Arrays.asList(basePhoneCaseIds.trim().split(","));
						for (String basePhoneCaseId : basePhoneCaseIdList) {
							List<Map> baseSize = listBaseSizeAndPriceAndDesignPhoneCaseInfo(basePhoneCaseId, toCreateOrderDropship);
							baseSizePhoneCase.addAll(baseSize);
						}
						formatChildren.put("base_size", baseSizePhoneCase);
					} else {
						List<Map> baseSizeList = listBaseSizeAndPriceAndDesignPhoneCaseInfo(baseId, toCreateOrderDropship);
						formatChildren.put("base_size", baseSizeList);
					}
				}
				
				formatChildrenList.add(formatChildren);
			}

			baseMap.put("childrens", formatChildrenList);
			allBaseGroup.add(baseMap);
		}
		
		Map baseGroups = new HashMap<>();
		baseGroups.put(AppParams.BASE_GROUPS, allBaseGroup);
		
		if (toCreateOrderDropship) {
			return RedisService.persistMap(RedisKeyEnum.BASE_GROUPS_MAP_ORDER, baseGroups);
		} else {
			return RedisService.persistMap(RedisKeyEnum.BASE_GROUPS_MAP, baseGroups);
		}
	}

	public static List<Map> getBasePhone() throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(1, OracleTypes.NUMBER);
		outputParamsTypes.put(2, OracleTypes.VARCHAR);
		outputParamsTypes.put(3, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(1, AppParams.RESULT_CODE);
		outputParamsNames.put(2, AppParams.RESULT_MSG);
		outputParamsNames.put(3, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_BASE_PHONE_CASES, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
		List<Map> result = new ArrayList<>();
		List<Map> dataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		for (Map data : dataList) {
			result.add(data);
		}

		return result;
	}

	private static final String GET_BASE_PHONE_CASES = "{call PKG_BASE_TYPE.get_all_base_phone(?,?,?)}";

	public static List<Map> listBaseSizeAndPrice(String baseId) throws SQLException {

		List<Map> rsList = searchAll(FF_GET_SIZE_AND_PRICE_BY_BASE_ID, new Object[] { baseId });
		return rsList.stream().map(o -> {
			Map m = new HashedMap<>();
			m.put(AppParams.ID, ParamUtil.getString(o, AppParams.S_SIZE_ID));
			m.put(AppParams.NAME, ParamUtil.getString(o, AppParams.S_SIZE_NAME));
			m.put(AppParams.DROPSHIP_BASE_COST, ParamUtil.getFormatedDouble(o, AppParams.S_DROPSHIP_BASE_COST, 2));
			m.put(AppParams.BASE_COST, ParamUtil.getFormatedDouble(o, AppParams.S_BASE_COST, 2));
			m.put(AppParams.SECOND_SIDE_PRICE, ParamUtil.getFormatedDouble(o, AppParams.S_SECOND_SIDE_PRICE, 2));
			m.put("default_profit", ParamUtil.getFormatedDouble(o, "S_DEFAULT_PROFIT", 2));
			return m;
		}).collect(Collectors.toList());
	}

	public static List<Map> listBaseSizeAndPriceAndDesignPhoneCaseInfo(String baseId, boolean toCreateOrderDropship) throws SQLException {
		
		List<Map> rsList = new ArrayList<Map>();
		if (!toCreateOrderDropship) {
			rsList = searchAll(FF_GET_SIZE_AND_PRICE_AND_DESIGN_INFO_BY_BASE_ID, new Object[] { baseId, 0 });
		} else {
			rsList = searchAll(FF_GET_SIZE_AND_PRICE_AND_DESIGN_INFO_BY_BASE_ID, new Object[] { baseId, 1 });
		}
		
		return rsList.stream().map(o -> {
			Map m = new HashedMap<>();
			m.put(AppParams.ID, ParamUtil.getString(o, AppParams.S_SIZE_ID));
			m.put(AppParams.NAME, ParamUtil.getString(o, AppParams.S_SIZE_NAME));
			m.put(AppParams.DROPSHIP_BASE_COST, ParamUtil.getFormatedDouble(o, AppParams.S_DROPSHIP_BASE_COST, 2));
			m.put(AppParams.BASE_COST, ParamUtil.getFormatedDouble(o, AppParams.S_BASE_COST, 2));
			m.put(AppParams.SECOND_SIDE_PRICE, ParamUtil.getFormatedDouble(o, AppParams.S_SECOND_SIDE_PRICE, 2));
			m.put(AppParams.BASE_ID, baseId);
			m.put(AppParams.IMAGE, ParamUtil.getMapData(o, AppParams.IMAGE));
			m.put(AppParams.PRINTABLE, ParamUtil.getMapData(o, AppParams.PRINTABLE));
			m.put(AppParams.DIMENSION, ParamUtil.getMapData(o, AppParams.DIMENSION));		
			m.put(AppParams.DESIGN_GROUP, ParamUtil.getString(o, AppParams.S_DESIGN_GROUP));
			m.put(AppParams.DESIGN_TYPE, ParamUtil.getString(o, AppParams.S_DESIGN_TYPE));
			
			Map typeMap = new LinkedHashMap();
			typeMap.put(AppParams.ID, ParamUtil.getString(o, AppParams.S_TYPE_ID));
			typeMap.put(AppParams.NAME, ParamUtil.getString(o, AppParams.S_TYPE_NAME));
			m.put(AppParams.TYPE, typeMap);
			
			Map baseImageInfoMap = new LinkedHashMap();
			baseImageInfoMap.put(AppParams.ICON, ParamUtil.getString(o, AppParams.S_ICON_IMG_URL));
			baseImageInfoMap.put(AppParams.FRONT, ParamUtil.getString(o, AppParams.S_FRONT_IMG_URL));
			baseImageInfoMap.put(AppParams.BACK, ParamUtil.getString(o, AppParams.S_BACK_IMG_URL));
			baseImageInfoMap.put(AppParams.WIDTH, ParamUtil.getString(o, AppParams.S_FRONT_IMG_WIDTH));
			baseImageInfoMap.put(AppParams.HEIGHT, ParamUtil.getString(o, AppParams.S_FRONT_IMG_HEIGHT));
			baseImageInfoMap.put(AppParams.UNIT, ParamUtil.getString(o, AppParams.S_UNIT, DimensionUnits.PIXEL));
			baseImageInfoMap.put("display_image", ParamUtil.getString(o, "S_SHOPIFY_DISPLAY_IMG"));
			m.put(AppParams.IMAGE, baseImageInfoMap);
			
			Map printableInfoMap = new LinkedHashMap();
			printableInfoMap.put(AppParams.FRONT_TOP, ParamUtil.getString(o, AppParams.S_PRINTABLE_FRONT_TOP));
			printableInfoMap.put(AppParams.FRONT_LEFT, ParamUtil.getString(o, AppParams.S_PRINTABLE_FRONT_LEFT));
			printableInfoMap.put(AppParams.FRONT_WIDTH, ParamUtil.getString(o, AppParams.S_PRINTABLE_FRONT_WIDTH));
			printableInfoMap.put(AppParams.FRONT_HEIGHT, ParamUtil.getString(o, AppParams.S_PRINTABLE_FRONT_HEIGHT));
			printableInfoMap.put(AppParams.BACK_TOP, ParamUtil.getString(o, AppParams.S_PRINTABLE_BACK_TOP));
			printableInfoMap.put(AppParams.BACK_LEFT, ParamUtil.getString(o, AppParams.S_PRINTABLE_BACK_LEFT));
			printableInfoMap.put(AppParams.BACK_WIDTH, ParamUtil.getString(o, AppParams.S_PRINTABLE_BACK_WIDTH));
			printableInfoMap.put(AppParams.BACK_HEIGHT, ParamUtil.getString(o, AppParams.S_PRINTABLE_BACK_HEIGHT));
			printableInfoMap.put(AppParams.UNIT, ParamUtil.getString(o, AppParams.S_UNIT, DimensionUnits.PIXEL));
			m.put(AppParams.PRINTABLE, printableInfoMap);
			
			Map<String, Object> dimension = new HashMap<String, Object>();
			dimension.put(AppParams.WIDTH, ParamUtil.getString(o, AppParams.S_DIMENSION_WIDTH));
			dimension.put(AppParams.HEIGHT, ParamUtil.getString(o, AppParams.S_DIMENSION_HEIGHT));
			m.put(AppParams.DIMENSION, dimension);

			return m;
		}).collect(Collectors.toList());
	}
	
	public static Map getAllBaseGroupCache(boolean toCreateOrderDropship) throws SQLException {
		
		Map redis = new HashMap<>();
		if (toCreateOrderDropship) {
			redis = RedisService.getMap(RedisKeyEnum.BASE_GROUPS_MAP_ORDER);
			if (redis == null || redis.isEmpty()) {
				redis = getBaseBaseTypeAndBaseGroupDb(toCreateOrderDropship);
			}
		} else {
			redis = RedisService.getMap(RedisKeyEnum.BASE_GROUPS_MAP);
			if (redis == null || redis.isEmpty()) {
				redis = getBaseBaseTypeAndBaseGroupDb(toCreateOrderDropship);
			}
		}
		 
		return redis;
	}

	public static Map getAllBaseCache() throws SQLException {

		Map redis = RedisService.getMap(RedisKeyEnum.BASES_MAP);
		if (redis == null || redis.isEmpty()) {
			redis = getAllBaseDb();
		}
		return redis;
	}

	public static Map getAllBaseDb() throws SQLException {

		List<Map> listBase = listAllBaseAndBaseGroupName();

		Map baseMap = new HashMap<>();

		for (String baseGroupName : listBase.stream().map(s -> ParamUtil.getString(s, "base_group_name")).collect(Collectors.toSet())) {
			baseMap.put(baseGroupName, listBase.stream().filter(s -> ParamUtil.getString(s, "base_group_name").equals(baseGroupName)).map(s -> {
				Map o = s;
				String baseId = ParamUtil.getString(o, AppParams.BASE_ID);
				LOGGER.info("baseId: " + baseId);
				List<Map> baseColorList = new ArrayList<Map>();
				try {
					baseColorList = BaseColorService.getBaseColorForDropship(baseId);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				o.put(AppParams.COLORS, baseColorList);
				List<Map> baseSizeList = new ArrayList<Map>();
				try {
					baseSizeList = listBaseSizeAndPrice(baseId);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				o.put(AppParams.SIZES, baseSizeList);
				o.remove("base_group_name");
				return o;
			}).collect(Collectors.toList()));
		}

		List<Map> apparelList = ParamUtil.getListData(baseMap, "Apparel");
		List<Map> apparelIphone = new ArrayList<Map>();
		List<Map> apparelSamsung = new ArrayList<Map>();
		List<Map> apparelMugs = new ArrayList<Map>();
		List<Map> apparelPoster = new ArrayList<Map>();
		List<Map> apparelTee = new ArrayList<Map>();
		for (Map apparel : apparelList) {
			String baseName = ParamUtil.getString(apparel, AppParams.BASE_NAME);
			if (baseName.contains("iPhone")) {
				apparelIphone.add(apparel);
			} else if (baseName.contains("Samsung")) {
				apparelSamsung.add(apparel);
			} else if (baseName.contains("Mugs")) {
				apparelMugs.add(apparel);
			} else if (baseName.contains("Poster")) {
				apparelPoster.add(apparel);
			} else {
				apparelTee.add(apparel);
			}
		}
		List<Map> apparelFilter = new ArrayList<Map>();
		apparelFilter.addAll(apparelTee);
		apparelFilter.addAll(apparelMugs);
		apparelFilter.addAll(apparelPoster);
		apparelFilter.addAll(apparelIphone);
		apparelFilter.addAll(apparelSamsung);
		baseMap.replace("Apparel", apparelFilter);

		return RedisService.persistMap(RedisKeyEnum.BASES_MAP, baseMap);
	}

	public static List<Map> listAllBaseAndBaseGroupName() throws SQLException {

		List<Map> rsList = searchAll(FF_GET_ALL_BASE_AND_BASE_GROUP_NAME, new Object[] {});
		return rsList.stream().map(o -> {

			Map m = new HashedMap<>();

			m.put("base_group_name", ParamUtil.getString(o, "S_BASE_GROUP_NAME"));
			m.put(AppParams.BASE_GROUP_ID, ParamUtil.getString(o, AppParams.S_BASE_GROUP_ID));
			m.put(AppParams.BASE_ID, ParamUtil.getString(o, AppParams.S_BASE_ID));
			m.put(AppParams.BASE_NAME, ParamUtil.getString(o, "S_NAME"));
			m.put(AppParams.RESOLUTION, ParamUtil.getString(o, "S_RESOLUTION_REQUIRE"));

			Map baseImageInfoMap = new LinkedHashMap();
			baseImageInfoMap.put(AppParams.ICON, ParamUtil.getString(o, AppParams.S_ICON_IMG_URL));
			baseImageInfoMap.put(AppParams.FRONT, ParamUtil.getString(o, AppParams.S_FRONT_IMG_URL));
			baseImageInfoMap.put(AppParams.BACK, ParamUtil.getString(o, AppParams.S_BACK_IMG_URL));
			baseImageInfoMap.put(AppParams.WIDTH, ParamUtil.getString(o, AppParams.S_FRONT_IMG_WIDTH));
			baseImageInfoMap.put(AppParams.HEIGHT, ParamUtil.getString(o, AppParams.S_FRONT_IMG_HEIGHT));
			baseImageInfoMap.put(AppParams.UNIT, ParamUtil.getString(o, AppParams.S_UNIT, DimensionUnits.PIXEL));
			m.put(AppParams.IMAGE, baseImageInfoMap);

			Map printableInfoMap = new LinkedHashMap();
			printableInfoMap.put(AppParams.FRONT_TOP, ParamUtil.getString(o, AppParams.S_PRINTABLE_FRONT_TOP));
			printableInfoMap.put(AppParams.FRONT_LEFT, ParamUtil.getString(o, AppParams.S_PRINTABLE_FRONT_LEFT));
			printableInfoMap.put(AppParams.FRONT_WIDTH, ParamUtil.getString(o, AppParams.S_PRINTABLE_FRONT_WIDTH));
			printableInfoMap.put(AppParams.FRONT_HEIGHT, ParamUtil.getString(o, AppParams.S_PRINTABLE_FRONT_HEIGHT));
			printableInfoMap.put(AppParams.BACK_TOP, ParamUtil.getString(o, AppParams.S_PRINTABLE_BACK_TOP));
			printableInfoMap.put(AppParams.BACK_LEFT, ParamUtil.getString(o, AppParams.S_PRINTABLE_BACK_LEFT));
			printableInfoMap.put(AppParams.BACK_WIDTH, ParamUtil.getString(o, AppParams.S_PRINTABLE_BACK_WIDTH));
			printableInfoMap.put(AppParams.BACK_HEIGHT, ParamUtil.getString(o, AppParams.S_PRINTABLE_BACK_HEIGHT));
			printableInfoMap.put(AppParams.UNIT, ParamUtil.getString(o, AppParams.S_UNIT, DimensionUnits.PIXEL));
			m.put(AppParams.PRINTABLE, printableInfoMap);

			return m;
		}).collect(Collectors.toList());
	}
	
	public static List<Map> getListCatalogs() throws SQLException {
		Object[] args = new Object[]{};
		
		List<Map> result = searchAll(GET_CATALOGS, args);
		return result;
	}
	
	public static List<Map> getCatalogDetail(String baseId) throws SQLException {
		Object[] args = new Object[]{baseId};
		List<Map> result = searchAll(GET_CATALOG_BY_BASE_ID, args);
		return result;
	}
	
	public static Map getNewProductCache() throws SQLException {
		
		Map redis = RedisService.getMap(RedisKeyEnum.BASE_NEW_PRODUCT);
		if (redis == null || redis.isEmpty()) {
			redis = getNewProductDb();
		}
		return redis;
	}
	
	public static Map getNewProductDb() throws SQLException {
		
		List<Map> rsList = searchAll(GET_NEW_BASE, new Object[] {});
		
		List<Map> newProductList = new ArrayList<>();
		
		for (Map m : rsList) {
			newProductList.add(formatV2(m));
		}
		
		Map newProduct = new HashMap<>();
		newProduct.put("new_product", newProductList);
		
		return  RedisService.persistMap(RedisKeyEnum.BASE_NEW_PRODUCT, newProduct);
	}
	
	private static Map formatV2(Map queryData) throws SQLException {
		
		Map resultMap = new LinkedHashMap<>();
		 
		String baseId = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, baseId);

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		Map typeMap = new LinkedHashMap();
		typeMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_TYPE_ID));
		typeMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_TYPE_NAME));
		resultMap.put(AppParams.TYPE, typeMap);

		resultMap.put(AppParams.DISPLAY_NAME, ParamUtil.getString(queryData, AppParams.S_DISPLAY_NAME));

		if (!ParamUtil.getString(queryData, AppParams.S_DESC).isEmpty()) {
			resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		}

		resultMap.put(AppParams.CURRENCY, ParamUtil.getString(queryData, AppParams.S_CURRENCY));
		
		resultMap.put(AppParams.DESIGN_GROUP, ParamUtil.getString(queryData, AppParams.S_DESIGN_GROUP));
		
		String baseColorIds = ParamUtil.getString(queryData, AppParams.S_COLORS);
		List<Map> allBaseColor = new ArrayList<>();

		if (!baseColorIds.isEmpty()) {
			resultMap.put(AppParams.COLORS, ProductUtil.getBaseColorList(baseColorIds, ""));
		}
		
		resultMap.put(AppParams.SIZE_PRICE_EDITABLE, ParamUtil.getBoolean(queryData, AppParams.N_EDIT_SIZE_PRICE));
		
		resultMap.put(AppParams.FULL_FILLMENT, ParamUtil.getBoolean(queryData, AppParams.N_FULFILLMENT));

		Map printAbleFront = new LinkedHashMap();
		
		printAbleFront.put(AppParams.TYPE, AppParams.FRONT);
	 	printAbleFront.put(AppParams.PRINTABLE_TOP, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_TOP));
        printAbleFront.put(AppParams.PRINTABLE_LEFT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_LEFT));
        printAbleFront.put(AppParams.PRINTABLE_WIDTH, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_WIDTH));
        printAbleFront.put(AppParams.PRINTABLE_HEIGHT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_FRONT_HEIGHT));
        printAbleFront.put(AppParams.IMG_URL, ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_URL));
        printAbleFront.put("canvas_width", ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_WIDTH));
        printAbleFront.put("canvas_height", ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_HEIGHT));
        printAbleFront.put(AppParams.UNIT, ParamUtil.getString(queryData, AppParams.S_UNIT, DimensionUnits.PIXEL));
		
        Map printAbleBack = new LinkedHashMap();
        printAbleBack.put(AppParams.TYPE, AppParams.BACK);
        printAbleBack.put(AppParams.PRINTABLE_TOP, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_TOP));
        printAbleBack.put(AppParams.PRINTABLE_LEFT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_LEFT));
        printAbleBack.put(AppParams.PRINTABLE_WIDTH, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_WIDTH));
        printAbleBack.put(AppParams.PRINTABLE_HEIGHT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_HEIGHT));
        printAbleBack.put(AppParams.IMG_URL, ParamUtil.getString(queryData, AppParams.S_BACK_IMG_URL));
        printAbleBack.put("canvas_width", ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_WIDTH));
        printAbleBack.put("canvas_height", ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_HEIGHT));
        printAbleBack.put(AppParams.UNIT, ParamUtil.getString(queryData, AppParams.S_UNIT, DimensionUnits.PIXEL));
        
        //handle for women racerback
        if (StringUtils.isEmpty(ParamUtil.getString(queryData, AppParams.S_PRINTABLE_BACK_TOP))) {
            printAbleBack = Collections.emptyMap();
        }

        List<Map> printAble = new ArrayList<>();
        printAble.add(printAbleFront);
        printAble.add(printAbleBack);
        resultMap.put(AppParams.PRINTABLE, printAble);
        
        Map<String, Object> dimension = new HashMap<String, Object>();
		dimension.put(AppParams.WIDTH, ParamUtil.getString(queryData, AppParams.S_DIMENSION_WIDTH));
		dimension.put(AppParams.HEIGHT, ParamUtil.getString(queryData, AppParams.S_DIMENSION_HEIGHT));

		resultMap.put(AppParams.DIMENSION, dimension);
		
		resultMap.put(AppParams.POSITION, ParamUtil.getString(queryData, AppParams.N_POSITION));

		resultMap.put(AppParams.DESIGN_TYPE, ParamUtil.getString(queryData, AppParams.S_DESIGN_TYPE));

		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		
		resultMap.put(AppParams.BASE_SHORT_CODE, ParamUtil.getString(queryData, AppParams.S_SHORT_CODE));
		
		resultMap.put(AppParams.DISPLAY, ParamUtil.getBoolean(queryData, "N_DISPLAY"));
		
		resultMap.put("display_image", ParamUtil.getString(queryData, "S_SHOPIFY_DISPLAY_IMG"));
		
		resultMap.put("mockup_image_url", ParamUtil.getString(queryData, AppParams.S_BASE_MOCKUP));
		
		resultMap.put(AppParams.RESOLUTION, ParamUtil.getString(queryData, "S_RESOLUTION_REQUIRE"));
        
		resultMap.put("catalog_name", ParamUtil.getString(queryData, "S_CATALOG_NAME"));
		
		resultMap.put(AppParams.PROCESSING_TIME, ParamUtil.getString(queryData, AppParams.S_PROCESSING_TIME));
		
		resultMap.put("is_personalize", ParamUtil.getBoolean(queryData, "N_IS_PERSONALIZE"));
		
		resultMap.put(AppParams.HTML_DESC, ParamUtil.getString(queryData, AppParams.S_HTML_DESC));
		
		List<Map> baseSizeList = new ArrayList<Map>();
		if (BasePhoneCaseUtil.isPhoneCaseDropship(baseId)) {
			String basePhoneCaseIds = BasePhoneCaseUtil.getBasePhoneCaseDropshipIds();
			List<String> basePhoneCaseIdList = Arrays.asList(basePhoneCaseIds.trim().split(","));
			for (String basePhoneCaseId : basePhoneCaseIdList) {
				List<Map> baseSize = listBaseSizeAndPrice(basePhoneCaseId);
				baseSizeList.addAll(baseSize);
			}
			resultMap.put("base_size", baseSizeList);
		} else {
			baseSizeList = listBaseSizeAndPrice(baseId);
			resultMap.put("base_size", baseSizeList);
		}
		
		resultMap.put(AppParams.PRICE, getMinPrice(baseSizeList));
		resultMap.put(AppParams.BASE_COST, getMinPrice(baseSizeList));
		 
		return resultMap;
		
	}
	
    public static String getMinPrice(List<Map> baseSizeList) {
        ArrayList<Double> prices = new ArrayList<>();

        for (Map size : baseSizeList) {
            Double baseCost = ParamUtil.getDouble(size, "base_cost");
            prices.add(baseCost);
        }
        Double minPrice = prices.get(0);
        for (Double p : prices) {
            if (p <= minPrice) minPrice = p;
        }
        return minPrice.toString();
    }
	
	private static final Logger LOGGER = Logger.getLogger(BaseService.class.getName());
}