package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.internal.OracleTypes;

/**
 * 
 * @author nathan
 *
 */
public class DropshipOrderRefundService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	static final String DROPSHIP_ORDER_REFUND_GET_REFUNDED_SHIPPING = "{call PKG_DROPSHIP_ORDER_REFUND.get_refunded_shipping(?,?,?,?)}";
	static final String DROPSHIP_ORDER_SAVE_REFUND = "{call PKG_DROPSHIP_ORDER_REFUND.save_refund(?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_UPDATE_ORDER_STATE = "{call PKG_DROPSHIP_ORDER_REFUND.update_order_state(?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_REFUND_SHIPPING = "{call PKG_DROPSHIP_ORDER_REFUND.refund_shipping(?,?,?,?,?)}";
	static final String DROPSHIP_REFUND_FULL_ORDER = "{call pkg_dropship_order_refund.refund_full_order(?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_REFUND_INSERT = "{call PKG_DROPSHIP_ORDER_REFUND.insert_refund(?,?,?,?,?,?,?,?,?,?)}";

	public static Double getRefundedShipping(String orderId) throws SQLException {

		Double refundedShipping = 0d;

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_REFUND_GET_REFUNDED_SHIPPING,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode == HttpResponseStatus.OK.code()) {
			List<Map> dataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
			for (Map data : dataList) {
				refundedShipping += ParamUtil.getDouble(data, AppParams.S_AMOUNT);
			}
		}

		return refundedShipping;
	}

	public static void saveRefund(String orderId, String variantId, String sizeId, int refundQty, String amount)
			throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, variantId);
		inputParams.put(3, sizeId);
		inputParams.put(4, refundQty);
		inputParams.put(5, amount);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_SAVE_REFUND, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

	}

	public static void updateOrderState(String orderId, String orderState) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, orderState);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_UPDATE_ORDER_STATE,
				inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

	}

	public static void refundShipping(String orderId, String refundShippingAmount) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, refundShippingAmount);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_REFUND_SHIPPING, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

	}

	public static void refundFullOrder(String orderId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_REFUND_FULL_ORDER, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

	}

	private static Map format(Map data) {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.ORDER_ID, ParamUtil.getString(data, AppParams.S_ORDER_ID));
		el.put(AppParams.VARIANT_ID, ParamUtil.getString(data, AppParams.S_VARIANT_ID));
		el.put(AppParams.SIZE_ID, ParamUtil.getString(data, AppParams.S_SIZE_ID));
		el.put(AppParams.QUANTITY, ParamUtil.getString(data, AppParams.N_QUANTITY));
		el.put(AppParams.AMOUNT, ParamUtil.getString(data, AppParams.S_AMOUNT));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		return el;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderRefundService.class.getName());

}
