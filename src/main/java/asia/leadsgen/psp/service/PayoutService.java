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

public class PayoutService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static Map get(String userId, String type, String startDate, String endDate, int page, int pageSize)
			throws SQLException {

		LOGGER.fine("Payout look up with userId=" + userId + " type=" + type);

		Map inputParams = new LinkedHashMap<>();
		inputParams.put(1, userId);
		inputParams.put(2, type);
		inputParams.put(3, startDate);
		inputParams.put(4, endDate);
		inputParams.put(5, page);
		inputParams.put(6, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.VARCHAR);
		outputParamsTypes.put(9, OracleTypes.NUMBER);
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(7, AppParams.RESULT_CODE);
		outputParamsNames.put(8, AppParams.RESULT_MSG);
		outputParamsNames.put(9, AppParams.TOTAL_AMOUNT);
		outputParamsNames.put(10, AppParams.TOTAL_AMOUNT_REF);
		outputParamsNames.put(11, AppParams.TOTAL_PROFIT);
		outputParamsNames.put(12, AppParams.RESULT_TOTAL);
		outputParamsNames.put(13, AppParams.W8);
		outputParamsNames.put(14, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYOUT_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		double totalAmount = ParamUtil.getDouble(searchResultMap, AppParams.TOTAL_AMOUNT);

		double totalAmountRef = ParamUtil.getDouble(searchResultMap, AppParams.TOTAL_AMOUNT_REF);

		double totalProfit = ParamUtil.getDouble(searchResultMap, AppParams.TOTAL_PROFIT);

		double resultTotal = ParamUtil.getDouble(searchResultMap, AppParams.RESULT_TOTAL);

		int w8 = ParamUtil.getInt(searchResultMap, AppParams.W8);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		Map resultResponse = new LinkedHashMap<>();

		if (!resultDataList.isEmpty()) {
			Map resultMap = format(resultDataList);
			List<String> payoutList = new ArrayList<String>(resultMap.values());
			resultResponse.put(AppParams.PAYOUT, payoutList);
		}

		resultResponse.put(AppParams.TOTAL_AMOUNT, totalAmount);
		resultResponse.put(AppParams.TOTAL_AMOUNT_REF, totalAmountRef);
		resultResponse.put(AppParams.TOTAL_PROFIT, totalProfit);
		resultResponse.put(AppParams.RESULT_TOTAL, resultTotal);
		resultResponse.put(AppParams.W8, w8);

		LOGGER.fine("=> Payout look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return resultResponse;
	}

	public static Map getPayoutUnApproved(String userId) throws SQLException {

		LOGGER.fine("Payout getPayoutUnApproved with userId=" + userId);

		Map inputParams = new LinkedHashMap<>();
		inputParams.put(1, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYOUT_UNAPPROVED, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		Map resultResponse = new LinkedHashMap<>();

		if (!resultDataList.isEmpty()) {
			Map resultMap = format(resultDataList);
			resultResponse.put(AppParams.PAYOUT, resultMap);
		}

		LOGGER.fine(
				"=> Payout getPayoutUnApproved result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return resultResponse;
	}

	private static Map format(List<Map> queryData) {

		Map resultMap = new LinkedHashMap<>();
		int count = 0;
		for (Map map : queryData) {
			Map info = new LinkedHashMap<>();
			info.put(AppParams.ID, ParamUtil.getString(map, AppParams.S_ID));
			info.put(AppParams.ORDER_ID, ParamUtil.getString(map, AppParams.S_ORDER_ID));
			info.put(AppParams.USER_ID, ParamUtil.getString(map, AppParams.S_USER_ID));
			info.put(AppParams.TYPE, ParamUtil.getString(map, AppParams.S_TYPE));
			info.put(AppParams.CREATE_TIME, ParamUtil.getString(map, AppParams.S_CREATE));
			info.put(AppParams.UPDATE_TIME, ParamUtil.getString(map, AppParams.D_UPDATE));
			info.put(AppParams.DESC, ParamUtil.getString(map, AppParams.S_DESC));
			info.put(AppParams.AMOUNT, ParamUtil.getString(map, AppParams.S_AMOUNT));
			info.put(AppParams.STATE, ParamUtil.getString(map, AppParams.S_STATE));
			resultMap.put(count++, info);
		}

		return resultMap;
	}

	private static Map format(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.ORDER_ID, ParamUtil.getString(queryData, AppParams.S_ORDER_ID));
		resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));
		resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));
		resultMap.put(AppParams.CREATE_TIME, ParamUtil.getString(queryData, AppParams.D_CREATE));
		resultMap.put(AppParams.UPDATE_TIME, ParamUtil.getString(queryData, AppParams.D_UPDATE));
		resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		resultMap.put(AppParams.AMOUNT, ParamUtil.getString(queryData, AppParams.S_AMOUNT));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

		return resultMap;
	}

	public static Map insert(String userId, String payoutMethod, String amount, String transferDesc)
			throws SQLException {

		LOGGER.fine("Payout insert with userId=" + userId + ", amount=" + amount + ", payoutMethod=" + payoutMethod
				+ ", transferDesc=" + transferDesc);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, payoutMethod);
		inputParams.put(3, amount);
		inputParams.put(4, transferDesc);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYOUT_WITHDRAW, inputParams,
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

		LOGGER.fine("=> User insert result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static Map update(String userId, String id) throws SQLException {

		LOGGER.fine("Payout update with userId=" + userId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYOUT_CONFIRM, inputParams,
				outputParamsTypes, outputParamsNames);

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

		LOGGER.fine("=> Payout updat result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static void delete() throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(1, OracleTypes.NUMBER);
		outputParamsTypes.put(2, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(1, AppParams.RESULT_CODE);
		outputParamsNames.put(2, AppParams.RESULT_MSG);

		Map orders = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYOUT_DELETE, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(orders, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(orders, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Payout delete result: " + resultCode);
	}

	private static final Logger LOGGER = Logger.getLogger(PayoutService.class.getName());
}
