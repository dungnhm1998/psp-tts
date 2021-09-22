package asia.leadsgen.psp.service_fulfill;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.protobuf.TextFormat.ParseException;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.CampaignUtil;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class StockService extends MasterService{
	private static final String GET_LIST_SKU_OUT_STOCK = "{call PKG_STOCK.get_list_sku_out_stock(?,?,?,?,?,?,?,?,?,?)}";
	
	public static Map getListSkuOutStock(String base_id, int page, int page_size, String baseName, String colorName, String sizeName)
			throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, base_id);
		inputParams.put(2, page);
		inputParams.put(3, page_size);
		inputParams.put(4, baseName);
		inputParams.put(5, colorName);
		inputParams.put(6, sizeName);

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
		

		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_LIST_SKU_OUT_STOCK, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
		int result_total = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);
		LOGGER.info("Total : " + result_total);
//		LOGGER.info("Result  : " + resultDataList);
		List<Map> resultMap = new ArrayList<Map>();
		for (Map item : resultDataList) {
			resultMap.add(format(item));
		}
		Map result = new LinkedHashMap<>();
		result.put(AppParams.TOTAL, result_total);
		result.put(AppParams.DATA, resultMap);
		LOGGER.info("=> Base look up result: " + result.toString());

		return result;
	}
	
	private static Map format(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.BASE_ID, ParamUtil.getString(queryData, AppParams.S_BASE_ID));
		resultMap.put(AppParams.BASE_NAME, ParamUtil.getString(queryData, AppParams.S_BASE_NAME));
		resultMap.put(AppParams.COLOR_ID, ParamUtil.getString(queryData, AppParams.S_COLOR_ID));
		resultMap.put(AppParams.COLOR_NAME, ParamUtil.getString(queryData, AppParams.S_COLOR_NAME));
		resultMap.put(AppParams.SIZE_ID, ParamUtil.getString(queryData, AppParams.S_SIZE_ID));
		resultMap.put(AppParams.SIZE_NAME, ParamUtil.getString(queryData, AppParams.S_SIZE_NAME));
		resultMap.put(AppParams.SKU, ParamUtil.getString(queryData, AppParams.S_SKU));
		resultMap.put(AppParams.STOCK, ParamUtil.getString(queryData, AppParams.N_STOCK));

		return resultMap;
	}

	
	private static final Logger LOGGER = Logger.getLogger(StockService.class.getName());
}
