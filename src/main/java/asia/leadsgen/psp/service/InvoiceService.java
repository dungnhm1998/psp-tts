/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 *
 * @author HIEPHV
 */
public class InvoiceService {
	static final String INVOICE_UPDATE_STATE_BY_ID = "{call PKG_INVOICE.INVOICE_UPDATE_STATE_BY_ID(?,?,?,?)}";
	static final String INVOICE_GET_ORDER_ID = "{call PKG_INVOICE.invoice_get_order_id(?,?,?,?)}";
	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static boolean UpdateStateId(String id, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, state);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, INVOICE_UPDATE_STATE_BY_ID,
				inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		return resultCode == HttpResponseStatus.OK.code();

	}

	public static String getOrderId(String id) throws SQLException {

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

		Map resultMap = DBProcedureUtil.execute(dataSource, INVOICE_GET_ORDER_ID, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		String orderId = null;
		if (CollectionUtils.isNotEmpty(ParamUtil.getListData(resultMap, AppParams.RESULT_DATA))) {
			orderId = ParamUtil.getString((Map) ParamUtil.getListData(resultMap, AppParams.RESULT_DATA).get(0),
					AppParams.S_ORDER_ID);
		}
		return orderId;
	}

}
