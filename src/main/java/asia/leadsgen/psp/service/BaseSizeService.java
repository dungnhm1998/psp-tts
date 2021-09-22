package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import asia.leadsgen.psp.data.type.RedisKeyEnum;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
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
public class BaseSizeService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	static final String SEARCH_ALL_SIZE = "{call PKG_BASE_SIZE.search_all_size(?,?,?)}";
	static final String SEARCH_ALL_AVAILABLE_BASES_OF_SIZE = "{call PKG_BASE_SIZE.search_all_available_bases_of_size(?,?,?,?)}";

	public static Map search(String name, String state) throws SQLException {

		LOGGER.fine("Base size search with name=" + name + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, name);
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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.BASE_SIZE_SEARCH, inputParams,
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
		resultMap.put(AppParams.SIZES, dataList);

		LOGGER.fine("=> Base size search result: " + resultMap.toString());

		return resultMap;
	}

	public static Map list(String ids, String state) throws SQLException {

		LOGGER.fine("Base size list with ids=" + ids + ", state=" + state);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.BASE_SIZE_LIST, inputParams,
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
		resultMap.put(AppParams.SIZES, colorList);

		LOGGER.fine("=> Base size list result: " + resultMap.toString());

		return resultMap;
	}

	public static Map insert(String name, String desc, String width, String height, String sleeve, String unit,
			int position, String state) throws SQLException {

		LOGGER.fine("Base size insert with name=" + name + ", desc=" + desc + ", width=" + width + ", height=" + height
				+ ", sleeve=" + sleeve + ", unit=" + unit + ", position=" + position + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, name);
		inputParams.put(2, desc);
		inputParams.put(3, width);
		inputParams.put(4, height);
		inputParams.put(5, sleeve);
		inputParams.put(6, unit);
		inputParams.put(7, position);
		inputParams.put(8, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(9, OracleTypes.NUMBER);
		outputParamsTypes.put(10, OracleTypes.VARCHAR);
		outputParamsTypes.put(11, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(9, AppParams.RESULT_CODE);
		outputParamsNames.put(10, AppParams.RESULT_MSG);
		outputParamsNames.put(11, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.BASE_SIZE_INSERT, inputParams,
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

		LOGGER.fine("=> Base size insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Boolean checkAvailabilityForBase(String sizeId, String baseId) throws SQLException {
		List<String> bases = searchAllSize().get(sizeId);
		if (bases == null || bases.isEmpty() || !bases.contains(baseId)) {
			return false;
		}
		return true;
	}

	public static Map<String, List<String>> searchAllSize() throws SQLException {

		Map<String, List<String>> allSizeAndAvailableBases = RedisService
				.get(RedisKeyEnum.SIZES_AND_AVAILABLE_BASE_MAP.getValue());

		if (allSizeAndAvailableBases == null || allSizeAndAvailableBases.isEmpty()) {
			Map inputParams = new LinkedHashMap<Integer, String>();

			Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
			outputParamsTypes.put(1, OracleTypes.NUMBER);
			outputParamsTypes.put(2, OracleTypes.VARCHAR);
			outputParamsTypes.put(3, OracleTypes.CURSOR);

			Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
			outputParamsNames.put(1, AppParams.RESULT_CODE);
			outputParamsNames.put(2, AppParams.RESULT_MSG);
			outputParamsNames.put(3, AppParams.RESULT_DATA);

			Map resultMap = DBProcedureUtil.execute(dataSource, SEARCH_ALL_SIZE, inputParams, outputParamsTypes,
					outputParamsNames);

			int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

			if (resultCode != HttpResponseStatus.OK.code()) {
				throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
			}
			List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
			allSizeAndAvailableBases = new LinkedHashMap<String, List<String>>();
			for (Map resultData : resultDataList) {
				String sizeId = ParamUtil.getString(resultData, AppParams.S_ID);
				allSizeAndAvailableBases.put(sizeId, searchAllAvailableBasesOfSize(sizeId));
			}
		}
		return allSizeAndAvailableBases;
	}

	public static List<String> searchAllAvailableBasesOfSize(String sizeId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, sizeId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, SEARCH_ALL_AVAILABLE_BASES_OF_SIZE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		return resultDataList.stream().map(o -> ParamUtil.getString(o, AppParams.S_BASE_ID))
				.collect(Collectors.toList());

	}

	private static Map format(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		resultMap.put(AppParams.WIDTH, ParamUtil.getString(queryData, AppParams.S_WIDTH));

		resultMap.put(AppParams.HEIGHT, ParamUtil.getString(queryData, AppParams.S_HEIGHT));

		resultMap.put(AppParams.SLEEVE, ParamUtil.getString(queryData, AppParams.S_SLEEVE));

		resultMap.put(AppParams.UNIT, ParamUtil.getString(queryData, AppParams.S_UNIT));

		resultMap.put(AppParams.POSITION, ParamUtil.getString(queryData, AppParams.N_POSITION));

		return resultMap;
	}
	
	public static Map getBasePriceByProductIdAndSizeId(String baseId, String sizeId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, baseId);
		inputParams.put(2, sizeId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.GET_PRICE_BY_PRODUCT_ID_AND_SIZE_ID, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_PRODUCT);
		}
		
		Map resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.DROPSHIP_BASE_COST, ParamUtil.getFormatedDouble(resultDataList.get(0), AppParams.S_DROPSHIP_BASE_COST, 2));
		resultMap.put(AppParams.SECOND_SIDE_PRICE, ParamUtil.getFormatedDouble(resultDataList.get(0), AppParams.S_SECOND_SIDE_PRICE, 2));

		return resultMap;
	}

	private static final Logger LOGGER = Logger.getLogger(BaseSizeService.class.getName());
}
