package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class ShippingFeeService extends MasterService {

	public static final String SHIPPING_FEE_GET_BY_BASEID_AND_COUNTRY = "{call PKG_SHIPPING_FEE.get_by_baseid_and_country(?,?,?,?,?)}";
	private static final String EXPRESS_SHIPPING_FEE_GET_BY_BASEID_AND_COUNTRY = "{call PKG_SHIPPING_FEE.get_express_shipping_by_baseid_and_country(?,?,?,?,?)}";
	private static final String GET_INFO_FOR_SHIPPING_EXPRESS = "{call PKG_FF_SHIPPING_FEE.get_info_for_shipping_express_v2(?,?,?)}";

	public static Map get(String baseId, String countryCode) throws SQLException {

		LOGGER.fine("Get shipping fee with baseTypeId=" + baseId + ", countryCode=" + countryCode);

		Map shippingFeeSearchResultMap = search(countryCode, baseId, ResourceStates.APPROVED);

		List<Map> shippingFeeList = ParamUtil.getListData(shippingFeeSearchResultMap, AppParams.SHIPPING_FEES);

		Map shippingFee = shippingFeeList.size() > 0 ? shippingFeeList.get(0) : new LinkedHashMap();

		LOGGER.info("=> Shipping fee: " + shippingFee.toString());

		return shippingFee;
	}
	public static ShippingFeeObj getShippingFee(String baseId, String countryCode) throws SQLException {

		LOGGER.info("Get shipping fee with baseId=" + baseId + ", countryCode=" + countryCode);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, baseId);
		inputParams.put(2, countryCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, SHIPPING_FEE_GET_BY_BASEID_AND_COUNTRY, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
		ShippingFeeObj shippingFee = null;
		if (resultDataList.isEmpty() == false) {
			shippingFee = ShippingFeeObj.fromMap(resultDataList.get(0));
		}
		return shippingFee;
	}
	
	public static Map search(String countryCode, String baseId, String state) throws SQLException {

		LOGGER.fine(
				"Shipping fee search with countryCode=" + countryCode + ", baseTypeId=" + baseId + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, countryCode);
		inputParams.put(2, baseId);
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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.SHIPPING_FEE_SEARCH, inputParams,
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
		resultMap.put(AppParams.SHIPPING_FEES, dataList);

		LOGGER.fine("=> Shipping fee search result: " + resultMap.toString());

		return resultMap;
	}

	private static Map format(Map data) throws SQLException {

		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.BASE_ID, ParamUtil.getString(data, AppParams.S_BASE_ID));
		el.put(AppParams.BASE_NAME, ParamUtil.getString(data, AppParams.S_BASE_NAME));
		el.put(AppParams.COUNTRY_CODE, ParamUtil.getString(data, AppParams.S_COUNTRY_CODE));
		el.put(AppParams.PRICE, ParamUtil.getString(data, AppParams.S_PRICE));
		el.put(AppParams.ADDING_PRICE, ParamUtil.getString(data, AppParams.S_ADDING_PRICE));
		el.put(AppParams.CURRENCY, ParamUtil.getString(data, AppParams.S_CURRENCY));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		return el;
	}
	
	public static List<Map> getShippingExpressInfo() throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(1, OracleTypes.NUMBER);
		outputParamsTypes.put(2, OracleTypes.VARCHAR);
		outputParamsTypes.put(3, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(1, AppParams.RESULT_CODE);
		outputParamsNames.put(2, AppParams.RESULT_MSG);
		outputParamsNames.put(3, AppParams.RESULT_DATA);
		
		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_INFO_FOR_SHIPPING_EXPRESS, inputParams, outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultData = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
		
		return resultData;
		
	}

	private static final Logger LOGGER = Logger.getLogger(ShippingFeeService.class.getName());
}