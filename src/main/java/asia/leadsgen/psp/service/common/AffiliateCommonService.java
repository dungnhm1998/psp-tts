package asia.leadsgen.psp.service.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class AffiliateCommonService {

	public static Boolean isQueryForDataExecutedSuccessFully(Map queryResult) {

		int resultCode = ParamUtil.getInt(queryResult, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(queryResult, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(queryResult, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
//			throw new OracleException(ParamUtil.getString(queryResult, AppParams.RESULT_MSG));
		}

		return true;
	}

	public static Map formatQueryData(Map queryResult) {

		Map summaryData = formatSummaryDataWithoutJavaCalculation(queryResult);

		List<Map> listData = ParamUtil.getListData(queryResult, AppParams.RESULT_DATA);
		listData = formatListData(listData);
		int total = ParamUtil.getInt(queryResult, AppParams.RESULT_TOTAL);

		Map formattedData = new LinkedHashMap<>();
		formattedData.put(AppParams.TOTAL, total);
		formattedData.put(AppParams.SUMMARY_DATA, summaryData);
		formattedData.put(AppParams.DATA, listData);

		return formattedData;
	}
	
	public static Map formatProductReport(Map queryResult) {

		Map summaryData = formatSummaryData(queryResult);

		List<Map> listData = ParamUtil.getListData(queryResult, AppParams.RESULT_DATA);
		listData = formatListProductReport(listData);
		int total = ParamUtil.getInt(queryResult, AppParams.RESULT_TOTAL);

		Map formattedData = new LinkedHashMap<>();
		formattedData.put(AppParams.TOTAL, total);
		formattedData.put(AppParams.SUMMARY_DATA, summaryData);
		formattedData.put(AppParams.DATA, listData);

		return formattedData;
	}
	
	public static Map formatProductReportByLevel(Map queryResult, String optionLv3, String checkId) {

		Map summaryData = formatSummaryData(queryResult);

		List<Map> listData = ParamUtil.getListData(queryResult, AppParams.RESULT_DATA);
		listData = formatListProductReportByLevel(listData, optionLv3, checkId);
		int total = ParamUtil.getInt(queryResult, AppParams.RESULT_TOTAL);

		Map formattedData = new LinkedHashMap<>();
		formattedData.put(AppParams.TOTAL, total);
		formattedData.put(AppParams.SUMMARY_DATA, summaryData);
		formattedData.put(AppParams.DATA, listData);

		return formattedData;
	}

	private static Map formatSummaryDataWithoutJavaCalculation(Map queryResult) {

		Map summaryData = new LinkedHashMap<>();
		int totalOrders = ParamUtil.getInt(queryResult, AppParams.ORDERS_TOTAL);
		int totalUnits = ParamUtil.getInt(queryResult, AppParams.UNITS_TOTAL);
		double totalProfits = ParamUtil.getDouble(queryResult, AppParams.PROFITS_TOTAL);
		int totalVisits = ParamUtil.getInt(queryResult, AppParams.VISITS_TOTAL);
		double totalRate = ParamUtil.getDouble(queryResult, AppParams.RATE_TOTAL);
		summaryData.put(AppParams.ORDERS_TOTAL, totalOrders);
		summaryData.put(AppParams.UNITS_TOTAL, totalUnits);
		summaryData.put(AppParams.PROFITS_TOTAL, totalProfits);
		summaryData.put(AppParams.VISITS_TOTAL, totalVisits);
		summaryData.put(AppParams.RATE_TOTAL, totalRate);
		
		return summaryData;
	}
	
	private static Map formatSummaryData(Map queryResult) {

		Map summaryData = new LinkedHashMap<>();
		int totalOrders = ParamUtil.getInt(queryResult, AppParams.ORDERS_TOTAL);
		int totalUnits = ParamUtil.getInt(queryResult, AppParams.UNITS_TOTAL);
		double totalProfits = ParamUtil.getDouble(queryResult, AppParams.PROFITS_TOTAL);
		int totalVisits = ParamUtil.getInt(queryResult, AppParams.VISITS_TOTAL);
		double totalRate = totalVisits > 0 ? (((double)totalOrders) / totalVisits) * 100 : 500;
		summaryData.put(AppParams.ORDERS_TOTAL, totalOrders);
		summaryData.put(AppParams.UNITS_TOTAL, totalUnits);
		summaryData.put(AppParams.PROFITS_TOTAL, totalProfits);
		summaryData.put(AppParams.VISITS_TOTAL, totalVisits);
		summaryData.put(AppParams.RATE_TOTAL, totalRate);
		
		return summaryData;
	}

	private static List<Map> formatListData(List<Map> listData) {
		List<Map> formatedListData = new ArrayList<>();
		int count = 0;

		for (Map data : listData) {

			count++;
			String name = ParamUtil.getString(data, AppParams.S_NAME);
			String orders = ParamUtil.getString(data, AppParams.N_ORDERS);
			String percentageChange = ParamUtil.getString(data, AppParams.N_PERCENTAGE);
			String units = ParamUtil.getString(data, AppParams.N_UNIT_SALES);
			String profit = ParamUtil.getString(data, AppParams.N_PROFIT);
			String conversionRate = ParamUtil.getString(data, AppParams.N_CONV_RATE);
			String visits = ParamUtil.getString(data, AppParams.N_VISITS);

			Map formatedData = new LinkedHashMap<>();
			formatedData.put(AppParams.ID, count);
			formatedData.put(AppParams.NAME, name);
			formatedData.put(AppParams.ORDER, orders);
			formatedData.put(AppParams.PERCENT_CHANGE, percentageChange);
			formatedData.put(AppParams.UNIT, units);
			formatedData.put(AppParams.PROFIT, profit);
			formatedData.put(AppParams.CONV_RATE, conversionRate);
			formatedData.put(AppParams.VISITS, visits);
			
			formatedListData.add(formatedData);
		}
		return formatedListData;
	}
	
	private static List<Map> formatListProductReport(List<Map> listData) {
		List<Map> formatedListData = new ArrayList<>();

		for (Map data : listData) {

			Map formatedData = new LinkedHashMap<>();
			
			formatedData.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
			formatedData.put(AppParams.PRODUCT_ID, ParamUtil.getString(data, AppParams.S_BASE_ID));
			formatedData.put(AppParams.NAME, ParamUtil.getString(data, AppParams.S_NAME));
			formatedData.put(AppParams.COUNTRY_NAME, ParamUtil.getString(data, AppParams.S_COUNTRY));
			formatedData.put(AppParams.ORDER, ParamUtil.getInt(data, AppParams.N_ORDERS));
			formatedData.put(AppParams.VISITS, ParamUtil.getInt(data, AppParams.N_VISITS));
			formatedData.put(AppParams.UNIT, ParamUtil.getString(data, AppParams.N_UNIT_SALES));
			formatedData.put(AppParams.PROFIT, ParamUtil.getDouble(data, AppParams.N_PROFIT));
	        
	        double convertionRate = ParamUtil.getDouble(data, AppParams.N_CONV_RATE);
	        formatedData.put(AppParams.CONV_RATE, convertionRate);
			
			formatedListData.add(formatedData);
		}
		return formatedListData;
	}
	
	private static List<Map> formatListProductReportByLevel(List<Map> listData, String optionLv3, String checkId) {
		List<Map> formatedListData = new ArrayList<>();

		int count = 0;
		for (Map data : listData) {  

			Map formatedData = new LinkedHashMap<>();
			
			count++;
			formatedData.put(AppParams.ID, count);
			formatedData.put(AppParams.PRODUCT_ID, ParamUtil.getString(data, AppParams.S_BASE_ID));
			formatedData.put(AppParams.NAME, ParamUtil.getString(data, AppParams.S_NAME));
			formatedData.put(AppParams.COUNTRY_NAME, ParamUtil.getString(data, AppParams.S_COUNTRY));
			formatedData.put(AppParams.ORDER, ParamUtil.getInt(data, AppParams.N_ORDERS));
			formatedData.put(AppParams.VISITS, ParamUtil.getInt(data, AppParams.N_VISITS));
			formatedData.put(AppParams.UNIT, ParamUtil.getString(data, AppParams.N_UNIT_SALES));
			formatedData.put(AppParams.PROFIT, ParamUtil.getDouble(data, AppParams.N_PROFIT));
	        
	        double convertionRate = ParamUtil.getDouble(data, AppParams.N_CONV_RATE);
	        formatedData.put(AppParams.CONV_RATE, convertionRate);
	        //For client side check condition
	        boolean group1 = true, group2 = true;
	        if (StringUtils.isEmpty(optionLv3)) {
				group1 = true;
				group2 = false;
			}
	        formatedData.put("group_1", group1);
	        formatedData.put("group_2", group2);
	        formatedData.put("check_id", checkId);
			formatedListData.add(formatedData);
		}
		return formatedListData;
	}
	
	public static Map formatQueryDataForCountry(Map queryResult, String checkId) {

		List<Map> listData = ParamUtil.getListData(queryResult, AppParams.RESULT_DATA);
		listData = formatListDataForCountry(listData, checkId);
		int total = ParamUtil.getInt(queryResult, AppParams.RESULT_TOTAL);

		Map formattedData = new LinkedHashMap<>();
		formattedData.put(AppParams.DATA, listData);

		return formattedData;
	}
	
	private static List<Map> formatListDataForCountry(List<Map> listData, String checkId) {
		List<Map> formatedListData = new ArrayList<>();

		for (Map data : listData) {

			String name = ParamUtil.getString(data, AppParams.S_NAME);
			String orders = ParamUtil.getString(data, AppParams.N_ORDERS);
			String percentageChange = ParamUtil.getString(data, AppParams.N_PERCENTAGE);
			String units = ParamUtil.getString(data, AppParams.N_UNIT_SALES);
			String profit = ParamUtil.getString(data, AppParams.N_PROFIT);
			String conversionRate = ParamUtil.getString(data, AppParams.N_CONV_RATE);
			String visits = ParamUtil.getString(data, AppParams.N_VISITS);

			Map formatedData = new LinkedHashMap<>();
			formatedData.put(AppParams.COUNTRY_ADD, true);
			formatedData.put(AppParams.CHECK_ID, checkId);
			formatedData.put(AppParams.NAME, name);
			formatedData.put(AppParams.ORDER, orders);
			formatedData.put(AppParams.PERCENT_CHANGE, percentageChange);
			formatedData.put(AppParams.UNIT, units);
			formatedData.put(AppParams.PROFIT, profit);
			formatedData.put(AppParams.CONV_RATE, conversionRate);
			formatedData.put(AppParams.VISITS, visits);
			
			formatedListData.add(formatedData);
		}
		return formatedListData;
	}

	public static Map createInputParams(String userId, String campaignIds, String nameFilter, String showbyName, String startDate,
			String endDate, int page, int pageSize, String orderby, String orderDriection) {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, campaignIds);
		inputParams.put(3, nameFilter);
		inputParams.put(4, showbyName);
		inputParams.put(5, startDate);
		inputParams.put(6, endDate);
		inputParams.put(7, page);
		inputParams.put(8, pageSize);
		inputParams.put(9, orderby);
		inputParams.put(10, orderDriection);

		return inputParams;
	}

	public static Map createOutputParamTypes() {
		Map<Integer, Integer> outputParamTypes = new LinkedHashMap<>();

		outputParamTypes.put(11, OracleTypes.NUMBER);
		outputParamTypes.put(12, OracleTypes.NUMBER);
		outputParamTypes.put(13, OracleTypes.NUMBER);
		outputParamTypes.put(14, OracleTypes.NUMBER);
		outputParamTypes.put(15, OracleTypes.NUMBER);
		outputParamTypes.put(16, OracleTypes.NUMBER);
		outputParamTypes.put(17, OracleTypes.VARCHAR);
		outputParamTypes.put(18, OracleTypes.NUMBER);
		outputParamTypes.put(19, OracleTypes.CURSOR);

		return outputParamTypes;
	}

	public static Map createOutputParamNames() {

		Map<Integer, String> outputParamNames = new LinkedHashMap<>();

		outputParamNames.put(11, AppParams.ORDERS_TOTAL);
		outputParamNames.put(12, AppParams.UNITS_TOTAL);
		outputParamNames.put(13, AppParams.PROFITS_TOTAL);
		outputParamNames.put(14, AppParams.RATE_TOTAL);
		outputParamNames.put(15, AppParams.VISITS_TOTAL);
		outputParamNames.put(16, AppParams.RESULT_CODE);
		outputParamNames.put(17, AppParams.RESULT_MSG);
		outputParamNames.put(18, AppParams.RESULT_TOTAL);
		outputParamNames.put(19, AppParams.RESULT_DATA);

		return outputParamNames;
	}
	
}
