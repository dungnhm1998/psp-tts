package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.error.SystemError;
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
public class PreferencesService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static String get(String key) throws SQLException {

		LOGGER.fine("System preferences look up with key=" + key);

		String value = "";
		value = (String) RedisDatabase0Service.getObject(key);
		if (value == null || value.isEmpty()) {
			Map inputParams = new LinkedHashMap<Integer, String>();
			inputParams.put(1, key);

			Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
			outputParamsTypes.put(2, OracleTypes.NUMBER);
			outputParamsTypes.put(3, OracleTypes.VARCHAR);
			outputParamsTypes.put(4, OracleTypes.CURSOR);

			Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
			outputParamsNames.put(2, AppParams.RESULT_CODE);
			outputParamsNames.put(3, AppParams.RESULT_MSG);
			outputParamsNames.put(4, AppParams.RESULT_DATA);

			Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PREFERENCE_GET, inputParams,
					outputParamsTypes, outputParamsNames);

			int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

			if (resultCode != HttpResponseStatus.OK.code()) {
				throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
			}

			List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

			if (!resultDataList.isEmpty()) {

				value = ParamUtil.getString(resultDataList.get(0), AppParams.S_VALUE);

				RedisDatabase0Service.persist(key, value);
			}

			LOGGER.fine("=> value: " + value);
		}

		return value;
	}

	public static Map getInfo(String key) throws SQLException {

		LOGGER.fine("System preferences info with key=" + key);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, key);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PREFERENCE_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {

			throw new OracleException(SystemError.DATA_NOT_FOUND.getName());
		}

		return format(resultDataList.get(0));
	}

	public static Map search(String scope, String key, String state) throws SQLException {

		LOGGER.fine("System preferences search with scope=" + scope + ", key=" + key + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, scope);
		inputParams.put(2, key);
		inputParams.put(3, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_TOTAL);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PREFERENCE_SEARCH, inputParams,
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
		resultMap.put(AppParams.PREFERENCES, dataList);

		LOGGER.fine("=> Preferences search result: " + resultMap.toString());

		return resultMap;
	}

	public static Map insert(String scope, String key, String value, String state, boolean replaceExisting)
			throws SQLException {

		LOGGER.fine("System preference insert with groupId=" + scope + ", key=" + key + ", value=" + value + ", state="
				+ state);

		String existValue = get(key);

		if (!existValue.isEmpty()) {

			if (replaceExisting) {
				update(key, value);
			} else {
				throw new OracleException("Preference key value conflict!");
			}
		}

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, scope);
		inputParams.put(2, key);
		inputParams.put(3, value);
		inputParams.put(4, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PREFERENCE_INSERT, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> Preference insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Map update(String key, String value) throws SQLException {

		LOGGER.fine("System preference  with update with key=" + key + ", value=" + value);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, key);
		inputParams.put(2, value);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PREFERENCE_UPDATE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> Preference update result: " + resultMap.toString());

		return resultMap;
	}

	private static Map format(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		resultMap.put(AppParams.KEY, ParamUtil.getString(queryData, AppParams.S_KEY));

		resultMap.put(AppParams.VALUE, ParamUtil.getString(queryData, AppParams.S_VALUE));

		if (!ParamUtil.getString(queryData, AppParams.S_SCOPE).isEmpty()) {
			resultMap.put(AppParams.SCOPE, ParamUtil.getString(queryData, AppParams.S_SCOPE));
		}

		return resultMap;
	}

	private static final Logger LOGGER = Logger.getLogger(PreferencesService.class.getName());
}
