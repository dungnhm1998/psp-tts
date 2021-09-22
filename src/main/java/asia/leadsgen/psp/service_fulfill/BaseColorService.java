package asia.leadsgen.psp.service_fulfill;

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

import asia.leadsgen.psp.data.type.RedisKeyEnum;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class BaseColorService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	static final String FF_GET_COLOR_FOR_DROPSHIP = "{call PKG_FF_BASE_COLOR.get_color_for_dropship(?,?,?,?)}";

	public static Map getAllBaseColorsCache() throws SQLException {
		Map redis = RedisService.getMap(RedisKeyEnum.BASES_COLORS_MAP);
		if (redis == null || redis.isEmpty()) {
			redis = getAllBaseColorsDb();
		}
		return redis;
	}

	public static Map getAllBaseColorsDb() throws SQLException {

		List<Map> baseColorsList = BaseColorService.getBaseColorsList("");
		
		Map baseColorsMap = new HashMap<>();
		
		for (String baseId : baseColorsList.stream().map(s -> ParamUtil.getString(s, AppParams.BASE_ID))
				.collect(Collectors.toSet())) {
			baseColorsMap.put(baseId, baseColorsList.stream()
					.filter(s -> ParamUtil.getString(s, AppParams.BASE_ID).equals(baseId)).map(s -> {
						Map o = s;
						o.remove(AppParams.BASE_ID);
						return o;
					}).collect(Collectors.toList()));
		}

		return RedisService.persistMap(RedisKeyEnum.BASES_COLORS_MAP, baseColorsMap);

	}

	public static List<Map> getBaseColorsList(String baseId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, baseId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.GET_ALL_BASES_COLORS, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_LIST;
		}

		List<Map> baseColorList = formatBaseColorsList(resultDataList);

		return baseColorList;
	}

	private static List<Map> formatBaseColorsList(List<Map> resultDataList) {
		List<Map> baseColorList = new ArrayList<>();
		for (Map resultData : resultDataList) {
			Map el = new LinkedHashMap<>();
			el.put(AppParams.ID, ParamUtil.getString(resultData, AppParams.S_ID));
			el.put(AppParams.NAME, ParamUtil.getString(resultData, AppParams.S_NAME));
			el.put(AppParams.VALUE, ParamUtil.getString(resultData, AppParams.S_VALUE));
			el.put(AppParams.POSITION, ParamUtil.getString(resultData, AppParams.N_POSITION));
			el.put(AppParams.BASE_ID, ParamUtil.getString(resultData, AppParams.S_BASE_ID));
			baseColorList.add(el);
		}
		return baseColorList;
	}

	public static Map getDefault() throws SQLException {

		LOGGER.fine("Base color default look up");

		Map inputParams = new LinkedHashMap<Integer, String>();

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(1, OracleTypes.NUMBER);
		outputParamsTypes.put(2, OracleTypes.VARCHAR);
		outputParamsTypes.put(3, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(1, AppParams.RESULT_CODE);
		outputParamsNames.put(2, AppParams.RESULT_MSG);
		outputParamsNames.put(3, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.COLOR_GET_DEFAULT, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> Base color default look up result: " + resultMap.toString());

		return resultMap;
	}

	public static Map list(String ids, String state) throws SQLException {

		LOGGER.fine("Base color list with ids=" + ids + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, ids);
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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.BASE_COLOR_LIST, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> colorList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {
			colorList.add(format(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.COLORS, colorList);

		LOGGER.fine("=> Base color list result: " + resultMap.toString());

		return resultMap;
	}

	private static Map format(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		resultMap.put(AppParams.VALUE, ParamUtil.getString(queryData, AppParams.S_VALUE));

		resultMap.put(AppParams.POSITION, ParamUtil.getString(queryData, AppParams.N_POSITION));

		return resultMap;
	}
	
	public static List<Map> getBaseColorForDropship(String baseId) throws SQLException {
		
		LOGGER.info("getBaseColorForDropship by baseId="+ baseId);
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, baseId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, FF_GET_COLOR_FOR_DROPSHIP, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		
		List<Map> baseColorDropship = formatBaseColorsDropship(resultDataList);

		return baseColorDropship;	
	}

	private static List<Map> formatBaseColorsDropship(List<Map> resultDataList) {
		List<Map> baseColorDropship = new ArrayList<>();
		for (Map resultData : resultDataList) {
			Map el = new LinkedHashMap<>();
			el.put(AppParams.ID, ParamUtil.getString(resultData, AppParams.S_ID));
			el.put(AppParams.NAME, ParamUtil.getString(resultData, AppParams.S_NAME));
			el.put(AppParams.VALUE, ParamUtil.getString(resultData, AppParams.S_VALUE));
			el.put(AppParams.POSITION, ParamUtil.getString(resultData, AppParams.N_POSITION));
			baseColorDropship.add(el);
		}
		return baseColorDropship;
	}

	private static final Logger LOGGER = Logger.getLogger(BaseColorService.class.getName());
}
