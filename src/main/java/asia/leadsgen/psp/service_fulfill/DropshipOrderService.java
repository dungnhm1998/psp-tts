package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.server.handler.dropship.order.OrderCancellationCheck;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.PaymentService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class DropshipOrderService extends MasterService {

	static final String DROPSHIP_ORDER_LOOKUP = "{call pkg_dropship_order.look_up(?,?,?,?)}";
	static final String DROPSHIP_ORDER_DUPLICATE = "{call pkg_dropship_order.duplicate_order(?,?,?,?,?)}";
		static final String DROPSHIP_ORDER_FIND_BY_REFERENCE = "{call pkg_dropship_order.find_by_reference_order(?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_UPDATE_STATE = "{call pkg_dropship_order.update_state(?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_UPDATE = "{call pkg_dropship_order.update_order(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_SEARCH = "{call pkg_dropship_order.search_order(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	
	static final String DROPSHIP_ORDER_INSERT = "{call pkg_ff_dropship_order.insert_dropship_order(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_CHECK_CONFLICT = "{call pkg_dropship_order.order_csv_check_exist(?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_VALIDATION_SOURCE = "{call pkg_dropship_order.validation_source_order_by_variant_and_user_id(?,?,?,?)}";
	static final String DROPSHIP_ORDER_DELETE_BY_ID_CSV_IMPORT = "{call pkg_dropship_order.order_csv_delete_by_order_id(?,?,?)}";
	static final String DROPSHIP_ORDER_CHECK_IF_ADDED_ORDER = "{call pkg_dropship_order.check_if_added_order(?,?,?,?,?)}";
	static final String UPDATE_ORDER_REFUNDED_AMOUNT = "{call pkg_dropship_order.update_order_refunded_amount(?,?,?,?,?)}";
	
	static final String DROPSHIP_ORDER_SEARCH_V2 = "{call pkg_ff_dropship_order.search_order(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_LOOKUP_V2 = "{call pkg_ff_dropship_order.look_up(?,?,?,?)}";
	static final String DROPSHIP_ORDER_UPDATE_V2 = "{call pkg_ff_dropship_order.update_order(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_UPDATE_STATE_V2 = "{call pkg_ff_dropship_order.update_state(?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_DELETE = "{call pkg_ff_dropship_order.delete_order(?,?,?)}";
	static final String DROPSHIP_ORDER_PAYALL_QUEUED_ORDER = "{call pkg_ff_dropship_order.payall_queued_order(?,?,?,?,?,?,?)}";
	
	static final String DROPSHIP_ORDER_ADJUST = "{call pkg_dropship_order.dropship_order_adjust(?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_PRD_TRACKING_LOOKUP = "{call pkg_dropship_order_product.order_prd_tracking_lookup(?,?,?,?)}";
	
	static final String CHECK_DUPLICATE_ORDER_ID = "{call pkg_dropship_order.check_duplicate_order_id(?,?,?,?)}";
	static final String IS_EXIST_STORE_ID_REFERENCE_ORDER_ID_SOURCE = "{call pkg_ff_dropship_order.is_exist_store_id_reference_order_id_source(?,?,?,?,?,?)}";
	static final String GET_ORDER_BY_STORE_ID_REFERENCE_ORDER_ID_SOURCE = "{call pkg_dropship_order.get_order_by_store_id_reference_order_id_source(?,?,?,?,?,?)}";
	
	static final String GET_ORDER_BY_ORIGINAL_ID = "{call pkg_dropship_order.get_order_by_original_id(?,?,?,?)}";
	static final String UPDATE_QUANTITY_AMOUNT_ADDRESS_CHECK = "{call pkg_ff_dropship_order.update_quantity_amount_address_check(?,?,?,?,?,?,?,?,?,?,?)}";
	static final String ORDER_CANCELLATION_CHECK = "{call pkg_dropship_order.check_for_api_cancellation(?,?,?,?)}";
	static final String CANCEL_ORDER_API = "{call pkg_dropship_order.cancel_order_api(?,?,?,?,?)}";
	static final String GET_ORDER_DROPSHIP_STATE = "{call pkg_dropship_order.get_dropship_order_state(?,?,?,?)}";
	static final String GET_ORDER_DROPSHIP_TRACKINGS = "{call pkg_dropship_order.get_dropship_order_trackings(?,?,?,?)}";
	static final String DROPSHIP_ORDER_UPDATE_BY_PAYPAL_SALE_ID = "{call pkg_dropship_order.update_state_by_paypal_sale_id(?,?,?,?)}";
	
	static final String DROPSHIP_ORDER_UPDATE_STATE_BY_ORIGINAL_ID = "{call pkg_ff_dropship_order.update_state_order_by_original_id(?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_UPDATE_EXTRA_FEE_AND_STATE = "{call pkg_ff_dropship_order.update_extra_fee_and_state(?,?,?,?,?,?,?)}";
	
	static final String DROPSHIP_ORDER_BALANCE = "{call pkg_wallet.get_current_balance(?,?,?,?)}";
	static final String DROPSHIP_ORDER_TOP_UP_HISTORY = "{call pkg_topup_history.get_current_topup_history(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_SEARCH_SUB_ACCOUNT = "{call pkg_ff_dropship_order.search_order_sub_account(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_PAYALL_QUEUED_ORDER_SUB_ACCOUNT = "{call pkg_ff_dropship_order.subaccount_payall_queued_order(?,?,?,?,?,?,?)}";
	
	static final String DROPSHIP_ORDER_SEARCH_TRACKING = "{call pkg_ff_dropship_order.search_tracking_order(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_ORDER_SEARCH_TRACKING_SUB_ACCOUNT = "{call pkg_ff_dropship_order.search_tracking_order_sub_account(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	/**
	 *
	 * @param orderId
	 * @param itemList
	 * @param campaignInfo
	 * @param paymentInfo
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 */
	public static Map lookUp(String orderId, boolean itemList, boolean campaignInfo, boolean paymentInfo)
			throws SQLException, ParseException {

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

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_LOOKUP, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		resultMap = formatV2(resultDataList.get(0), itemList, campaignInfo, paymentInfo);

		LOGGER.fine("=> lookUp result: " + resultMap.toString());

		return resultMap;
	}

	/**
	 *
	 * @param sourceOrderId
	 * @param trackingCode
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 */
	public static Map duplicate(String sourceOrderId, String trackingCode) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, sourceOrderId);
		inputParams.put(2, trackingCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_DUPLICATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatV2(resultDataList.get(0), true, false, false);
		LOGGER.fine("=> duplicate result: " + resultMap.toString());
		return resultMap;
	}

	public static Map findByReferenceOrder(String storeId, String shopifyOrderId) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, shopifyOrderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_FIND_BY_REFERENCE,
				inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return new LinkedHashMap<>();
		}

		resultMap = formatV2(resultDataList.get(0), true, false, true);

		LOGGER.fine("=> findByReferenceOrder result: " + resultMap.toString());

		return resultMap;
	}

	public static Map deleteOrder(String orderId) throws SQLException, ParseException {
		return updateState(orderId, ResourceStates.DELETED);
	}

	public static Map updateState(String orderId, String state) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_UPDATE_STATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatV2(resultDataList.get(0), false, false, false);

		LOGGER.fine("=> updateState result: " + resultMap.toString());

		return resultMap;
	}

	public static boolean UpdateStateByPaypalSaleId(String id, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, state);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);

		Map insertResultMap = DBProcedureUtil.execute(dataSource,
				DROPSHIP_ORDER_UPDATE_BY_PAYPAL_SALE_ID, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		return resultCode == HttpResponseStatus.OK.code();

	}

	public static Map updateOrder(String orderId, String orderAmount, String orderCurrency, String state,
			String shippingId, String storeId, String referenceOrderId, int totalItems, int addrVerified,
			String addrVerifiedNote) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, orderAmount);
		inputParams.put(3, orderCurrency);
		inputParams.put(4, state);
		inputParams.put(5, shippingId);
		inputParams.put(6, storeId);
		inputParams.put(7, referenceOrderId);
		inputParams.put(8, totalItems);
		inputParams.put(9, addrVerified);
		inputParams.put(10, addrVerifiedNote);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.VARCHAR);
		outputParamsTypes.put(13, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(11, AppParams.RESULT_CODE);
		outputParamsNames.put(12, AppParams.RESULT_MSG);
		outputParamsNames.put(13, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_UPDATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatV2(resultDataList.get(0), true, false, false);
		LOGGER.fine("=>  result: " + resultMap.toString());
		return resultMap;
	}

	public static void updateQuantityAmountAddressCheck(String orderId, double orderAmount, double orderSubamount,
			double shippingFee, int totalItems, int addressVerified, String addressVerifiedNote, String taxAmount) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, String.format("%.2f", orderAmount));
		inputParams.put(3, String.format("%.2f", orderSubamount));
		inputParams.put(4, String.format("%.2f", shippingFee));
		inputParams.put(5, totalItems);
		inputParams.put(6, addressVerified);
		inputParams.put(7, addressVerifiedNote);
		inputParams.put(8, taxAmount);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(9, OracleTypes.NUMBER);
		outputParamsTypes.put(10, OracleTypes.VARCHAR);
		outputParamsTypes.put(11, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(9, AppParams.RESULT_CODE);
		outputParamsNames.put(10, AppParams.RESULT_MSG);
		outputParamsNames.put(11, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, UPDATE_QUANTITY_AMOUNT_ADDRESS_CHECK, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}

	public static Map searchOrder(String userId, String storeId, String channel, String state, String startDate,
			String endDate, int page, int pageSize, String sort, String dir, String orderId)
			throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, storeId);
		inputParams.put(3, channel);
		inputParams.put(4, state);
		inputParams.put(5, startDate);
		inputParams.put(6, endDate);
		inputParams.put(7, page);
		inputParams.put(8, pageSize);
		inputParams.put(9, sort);
		inputParams.put(10, dir);
		inputParams.put(11, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.CURSOR);
		outputParamsTypes.put(15, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_DATA);
		outputParamsNames.put(15, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);

		List<Map> formatList = new ArrayList<>();
		for (Map resultDataItem : resultDataList) {
			formatList.add(format(resultDataItem, true));
		}

		resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.TOTAL, resultTotal);
		resultMap.put(AppParams.DATA, formatList);

		LOGGER.fine("=> searchOrder result: " + resultMap.toString());

		return resultMap;
	}

	public static boolean CheckConflict(String dopshipOrderId, String userId, String storeId)
			throws SQLException, ParseException {
		boolean flag = false;
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, dopshipOrderId);
		inputParams.put(2, userId);
		inputParams.put(3, storeId);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_CHECK_CONFLICT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode == HttpResponseStatus.CONFLICT.code()) {
			flag = true;
		}
		if (resultCode == HttpResponseStatus.NOT_FOUND.code()) {
			flag = false;
		}
		return flag;
	}

	public static boolean validationSourceOrderByVariantAndUserId(String variantId, String userId)
			throws SQLException, ParseException {
		boolean flag = false;
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, variantId);
		inputParams.put(2, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_VALIDATION_SOURCE,
				inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode == HttpResponseStatus.OK.code()) {
			flag = true;
		}
		if (resultCode == HttpResponseStatus.NOT_FOUND.code()) {
			flag = false;
		}
		return flag;
	}

	public static Map tracking(String orderId) throws SQLException {

		LOGGER.fine("Dropship Order product tracking with orderId=" + orderId);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PRD_TRACKING_LOOKUP,
				inputParams, outputParamsTypes, outputParamsNames);

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

		resultMap.put(AppParams.TRACKING, dataList);

		LOGGER.fine("=> Dropship Order product tracking result: "
				+ ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static void deleteByIdCSVImport(String orderId) throws SQLException {

		LOGGER.info("Dropship Delete order products with orderId=: " + orderId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource,
				DROPSHIP_ORDER_DELETE_BY_ID_CSV_IMPORT, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.info("=>Dropship Order delete result: " + resultCode);
	}

	private static Map format(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.VARIANT_NAME, ParamUtil.getString(queryData, AppParams.S_VARIANT_NAME));
		resultMap.put(AppParams.SIZE_NAME, ParamUtil.getString(queryData, AppParams.S_SIZE_NAME));
		resultMap.put(AppParams.TRACKING_CODE, ParamUtil.getString(queryData, AppParams.S_SHIPPING_TRACKING_CODE));
		resultMap.put("carrier", ParamUtil.getString(queryData, AppParams.S_SHIPPING_CARRIER));
		resultMap.put(AppParams.TRACKING_URL, ParamUtil.getString(queryData, AppParams.S_SHIPPING_TRACKING_URL));

		return resultMap;

	}

	private static Map format(Map data, boolean tracking) throws SQLException, ParseException {
		Map el = new LinkedHashMap<>();
		String id = ParamUtil.getString(data, AppParams.S_ID);
		el.put(AppParams.ID, id);
		el.put(AppParams.CURRENCY, ParamUtil.getString(data, AppParams.S_CURRENCY));
		el.put(AppParams.SUB_AMOUNT, ParamUtil.getString(data, AppParams.S_SUB_AMOUNT));
		el.put(AppParams.SHIPPING_FEE, ParamUtil.getString(data, AppParams.S_SHIPPING_FEE));
		el.put(AppParams.TAX_AMOUNT, ParamUtil.getString(data, AppParams.S_TAX_AMOUNT));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		el.put(AppParams.QUANTITY, ParamUtil.getString(data, AppParams.N_QUANTITY));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		el.put(AppParams.TRACKING_CODE, ParamUtil.getString(data, AppParams.S_TRACKING_CODE));
		el.put(AppParams.ORDER_DATE, ParamUtil.getString(data, AppParams.D_ORDER));
		el.put(AppParams.NOTE, ParamUtil.getString(data, AppParams.S_NOTE));
		el.put(AppParams.CHANNEL, ParamUtil.getString(data, AppParams.S_CHANNEL));
		el.put(AppParams.USER_ID, ParamUtil.getString(data, AppParams.S_USER_ID));
		el.put(AppParams.STORE_ID, ParamUtil.getString(data, AppParams.S_STORE_ID));
		el.put(AppParams.STORE_NAME, ParamUtil.getString(data, AppParams.S_STORE_NAME));
		el.put(AppParams.FULFILL_STATE, calculateFulfillState(data));
		el.put(AppParams.SHIPPING, ShippingService.get(ParamUtil.getString(data, AppParams.S_SHIPPING_ID)));
		el.put(AppParams.STORE_DOMAIN, ParamUtil.getString(data, AppParams.S_STORE_DOMAIN));
		el.put(AppParams.REFERENCE_ID, ParamUtil.getString(data, AppParams.S_REFERENCE_ORDER));

		Double amount = ParamUtil.getDouble(data, AppParams.S_AMOUNT, 0);
		Double refundedAmount = ParamUtil.getDouble(data, AppParams.S_REFUNDED_AMOUNT, 0);
		String amountExceptRefunded = String.format("%.2f", Math.abs(amount - refundedAmount));
		el.put(AppParams.AMOUNT, amountExceptRefunded);

		if (tracking) {
			List<Map> trackingData = ParamUtil.getListData(DropshipOrderService.tracking(id), AppParams.TRACKING);
			List<Map> trackingList = new ArrayList<Map>();
			String trackingCode;
			for (Map trackingMap : trackingData) {
				trackingCode = ParamUtil.getString(trackingMap, AppParams.TRACKING_CODE);
				if (!trackingCode.isEmpty()) {
					trackingList.add(trackingMap);
				}
			}
			el.put(AppParams.TRACKING, trackingList);

		}

		return el;
	}

	private static String calculateFulfillState(Map data) {
		String fulfillState = "";
		int totalItems = ParamUtil.getInt(data, "N_TOTAL_ITEM", 0);
		int fulfilledItems = ParamUtil.getInt(data, "N_FULFILLED_ITEM", 0);

		if (fulfilledItems == 0) {
			fulfillState = "Unfulfilled";
		} else if (totalItems <= fulfilledItems) {
			fulfillState = "Fulfilled";
		} else if (fulfilledItems < totalItems) {
			fulfillState = "Partially fulfilled";
		}

		return fulfillState;
	}

	public static boolean checkIfAddedOrder(String storeId, String referenceOrderId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, referenceOrderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_CHECK_IF_ADDED_ORDER,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		boolean isAdded = false;
		if (CollectionUtils.isNotEmpty(ParamUtil.getListData(deleteResultMap, AppParams.RESULT_DATA))) {
			isAdded = ParamUtil.getBoolean((Map) ParamUtil.getListData(deleteResultMap, AppParams.RESULT_DATA).get(0),
					AppParams.N_ADDED);
		}
		return isAdded;
	}

	public static void updateOrderRefundedAmount(String orderId, String refundedAmount) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, refundedAmount);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, UPDATE_ORDER_REFUNDED_AMOUNT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

	}

	public static Map searchOrderV2(String userId, String storeId, String channel, String state, String startDate,
			String endDate, int page, int pageSize, String sort, String dir, String orderId, String source)
			throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, storeId);
		inputParams.put(3, channel);
		inputParams.put(4, state);
		inputParams.put(5, startDate);
		inputParams.put(6, endDate);
		inputParams.put(7, page);
		inputParams.put(8, pageSize);
		inputParams.put(9, sort);
		inputParams.put(10, dir);
		inputParams.put(11, orderId);
		inputParams.put(12, source);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);
		outputParamsTypes.put(15, OracleTypes.CURSOR);
		outputParamsTypes.put(16, OracleTypes.NUMBER);
		outputParamsTypes.put(17, OracleTypes.NUMBER);
		outputParamsTypes.put(18, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);
		outputParamsNames.put(15, AppParams.RESULT_DATA);
		outputParamsNames.put(16, AppParams.RESULT_TOTAL);
		outputParamsNames.put(17, "result_all_orders");
		outputParamsNames.put(18, "result_fulfilled");

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_SEARCH_V2, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);
		int resultAllOrders = ParamUtil.getInt(resultMap, "result_all_orders");
		int resultFulfilled = ParamUtil.getInt(resultMap, "result_fulfilled");
		int resultUnfulfilled = resultAllOrders - resultFulfilled;

		List<Map> formatList = new ArrayList<>();
		for (Map resultDataItem : resultDataList) {
			formatList.add(formatV2(resultDataItem, true));
		}

		resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.TOTAL, resultTotal);
		resultMap.put(AppParams.DATA, formatList);
		resultMap.put("all_orders", resultAllOrders);
		resultMap.put("fulfilled", resultFulfilled);
		resultMap.put("unfulfilled", resultUnfulfilled);

		LOGGER.info("=> DropshipOrderService.searchOrderV2()-  result: " + resultMap.toString());

		return resultMap;
	}
	
	public static Map searchOrderSubAccount(String userId, String storeId, String channel, String state, String startDate,
			String endDate, int page, int pageSize, String sort, String dir, String orderId, String source)
			throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, storeId);
		inputParams.put(3, channel);
		inputParams.put(4, state);
		inputParams.put(5, startDate);
		inputParams.put(6, endDate);
		inputParams.put(7, page);
		inputParams.put(8, pageSize);
		inputParams.put(9, sort);
		inputParams.put(10, dir);
		inputParams.put(11, orderId);
		inputParams.put(12, source);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);
		outputParamsTypes.put(15, OracleTypes.CURSOR);
		outputParamsTypes.put(16, OracleTypes.NUMBER);
		outputParamsTypes.put(17, OracleTypes.NUMBER);
		outputParamsTypes.put(18, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);
		outputParamsNames.put(15, AppParams.RESULT_DATA);
		outputParamsNames.put(16, AppParams.RESULT_TOTAL);
		outputParamsNames.put(17, "result_all_orders");
		outputParamsNames.put(18, "result_fulfilled");

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_SEARCH_SUB_ACCOUNT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);
		int resultAllOrders = ParamUtil.getInt(resultMap, "result_all_orders");
		int resultFulfilled = ParamUtil.getInt(resultMap, "result_fulfilled");
		int resultUnfulfilled = resultAllOrders - resultFulfilled;

		List<Map> formatList = new ArrayList<>();
		for (Map resultDataItem : resultDataList) {
			formatList.add(formatV2(resultDataItem, true));
		}

		resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.TOTAL, resultTotal);
		resultMap.put(AppParams.DATA, formatList);
		resultMap.put("all_orders", resultAllOrders);
		resultMap.put("fulfilled", resultFulfilled);
		resultMap.put("unfulfilled", resultUnfulfilled);

		LOGGER.fine("=> searchOrderSubAccount result: " + resultMap.toString());

		return resultMap;
	}

	private static Map formatV2(Map data, boolean tracking) throws SQLException, ParseException {
		Map el = new LinkedHashMap<>();
		String id = ParamUtil.getString(data, AppParams.S_ID);
		el.put(AppParams.ID, id);
		el.put(AppParams.CURRENCY, ParamUtil.getString(data, AppParams.S_CURRENCY));
		el.put(AppParams.SUB_AMOUNT, ParamUtil.getString(data, AppParams.S_SUB_AMOUNT));
		el.put(AppParams.SHIPPING_FEE, ParamUtil.getString(data, AppParams.S_SHIPPING_FEE));
		el.put(AppParams.TAX_AMOUNT, ParamUtil.getString(data, AppParams.S_TAX_AMOUNT));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		el.put(AppParams.QUANTITY, ParamUtil.getString(data, AppParams.N_QUANTITY));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		el.put(AppParams.TRACKING_CODE, ParamUtil.getString(data, AppParams.S_TRACKING_CODE));
		el.put(AppParams.ORDER_DATE, ParamUtil.getString(data, AppParams.D_ORDER));
		el.put(AppParams.NOTE, ParamUtil.getString(data, AppParams.S_NOTE));
		el.put(AppParams.CHANNEL, ParamUtil.getString(data, AppParams.S_CHANNEL));
		el.put(AppParams.USER_ID, ParamUtil.getString(data, AppParams.S_USER_ID));
		el.put(AppParams.STORE_ID, ParamUtil.getString(data, AppParams.S_STORE_ID));
		el.put(AppParams.STORE_NAME, ParamUtil.getString(data, AppParams.S_STORE_NAME));
		el.put(AppParams.SHIPPING, ShippingService.get(ParamUtil.getString(data, AppParams.S_SHIPPING_ID)));
		el.put(AppParams.STORE_DOMAIN, ParamUtil.getString(data, AppParams.S_STORE_DOMAIN));
		el.put(AppParams.REFERENCE_ID, ParamUtil.getString(data, AppParams.S_REFERENCE_ORDER));
		el.put(AppParams.SOURCE, ParamUtil.getString(data, AppParams.S_SOURCE));
		el.put(AppParams.REQUIRE_REFUND, ParamUtil.getInt(data, AppParams.N_REQUIRE_REFUND));
		el.put(AppParams.FULFILL_STATE, calculateFulfillState(data));

		Double amount = ParamUtil.getDouble(data, AppParams.S_AMOUNT, 0);
		Double extra_fee = ParamUtil.getDouble(data, AppParams.S_EXTRA_FEE, 0);
		Double refundedAmount = ParamUtil.getDouble(data, AppParams.S_REFUNDED_AMOUNT, 0);
		String amountExceptRefunded = String.format("%.2f", Math.abs((amount + extra_fee) - refundedAmount));
		el.put(AppParams.AMOUNT, amountExceptRefunded);

		if (tracking) {
			List<Map> trackingData = ParamUtil.getListData(DropshipOrderService.tracking(id), AppParams.TRACKING);
			List<Map> trackingList = new ArrayList<Map>();
			String trackingCode;
			for (Map trackingMap : trackingData) {
				trackingCode = ParamUtil.getString(trackingMap, AppParams.TRACKING_CODE);
				if (!trackingCode.isEmpty()) {
					trackingList.add(trackingMap);
				}
			}
			el.put(AppParams.TRACKING, trackingList);

		}
		
		el.put(AppParams.SHIPPING_METHOD, ParamUtil.getString(data, AppParams.S_SHIPPING_METHOD));
		List<String> listProductName = Arrays.asList( ParamUtil.getString(data, AppParams.S_PRODUCT_NAME).split(","));
		el.put(AppParams.PRODUCT_NAME, listProductName);
		el.put(AppParams.TAX_AMOUNT, ParamUtil.getString(data, AppParams.S_TAX_AMOUNT));

		return el;
	}

	public static Map lookUpV2(String orderId, boolean itemList, boolean campaignInfo, boolean paymentInfo)
			throws SQLException, ParseException {

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

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_LOOKUP_V2, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		resultMap = formatV2(resultDataList.get(0), itemList, campaignInfo, paymentInfo);

		LOGGER.fine("=> lookUp result: " + resultMap.toString());

		return resultMap;
	}

	public static Map updateOrderV2(String orderId, String orderAmount, String orderCurrency, String state,
			String shippingId, String storeId, String referenceOrderId, int totalItems, int addrVerified,
			String addrVerifiedNote, String taxAmount, double shippingFee ) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, orderAmount);
		inputParams.put(3, orderCurrency);
		inputParams.put(4, state);
		inputParams.put(5, shippingId);
		inputParams.put(6, storeId);
		inputParams.put(7, referenceOrderId);
		inputParams.put(8, totalItems);
		inputParams.put(9, addrVerified);
		inputParams.put(10, addrVerifiedNote);
		inputParams.put(11, taxAmount);
		inputParams.put(12, shippingFee);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);
		outputParamsTypes.put(15, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);
		outputParamsNames.put(15, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_UPDATE_V2, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatV2(resultDataList.get(0), true, false, false);
		LOGGER.fine("=>  result: " + resultMap.toString());
		return resultMap;
	}

	private static Map formatV2(Map data, boolean itemList, boolean campaignInfo, boolean paymentInfo)
			throws SQLException, ParseException {
		Map el = new LinkedHashMap<>();
		String id = ParamUtil.getString(data, AppParams.S_ID);
		el.put(AppParams.ID, id);
		el.put(AppParams.CURRENCY, ParamUtil.getString(data, AppParams.S_CURRENCY));

		el.put(AppParams.SUB_AMOUNT, ParamUtil.getString(data, AppParams.S_SUB_AMOUNT));
		el.put(AppParams.SHIPPING_FEE, ParamUtil.getString(data, AppParams.S_SHIPPING_FEE));
		el.put(AppParams.TAX_AMOUNT, ParamUtil.getString(data, AppParams.S_TAX_AMOUNT));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		el.put(AppParams.QUANTITY, ParamUtil.getString(data, AppParams.N_QUANTITY));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		el.put(AppParams.TRACKING_CODE, ParamUtil.getString(data, AppParams.S_TRACKING_CODE));
		el.put(AppParams.ORDER_DATE, ParamUtil.getString(data, AppParams.D_ORDER));
		el.put(AppParams.NOTE, ParamUtil.getString(data, AppParams.S_NOTE));
		el.put(AppParams.CHANNEL, ParamUtil.getString(data, AppParams.S_CHANNEL));
		el.put(AppParams.USER_ID, ParamUtil.getString(data, AppParams.S_USER_ID));
		el.put(AppParams.STORE_ID, ParamUtil.getString(data, AppParams.S_STORE_ID));
		el.put(AppParams.STORE_NAME, ParamUtil.getString(data, AppParams.S_STORE_NAME));
		el.put(AppParams.SHIPPING_ID, ParamUtil.getString(data, AppParams.S_SHIPPING_ID));
		el.put(AppParams.ORIGINAL_ID, ParamUtil.getString(data, AppParams.S_ORIGINAL_ID));
		el.put(AppParams.SOURCE, ParamUtil.getString(data, AppParams.S_SOURCE));
		el.put(AppParams.SHIPPING_METHOD, ParamUtil.getString(data, AppParams.S_SHIPPING_METHOD));
		el.put(AppParams.FULFILL_STATE, calculateFulfillState(data));

		Map shipping = ShippingService.get(ParamUtil.getString(data, AppParams.S_SHIPPING_ID));
		if (shipping != null && shipping.isEmpty() == false) {
			Map address = ParamUtil.getMapData(shipping, AppParams.ADDRESS);
			address.put(AppParams.ADDR_VERIFIED, ParamUtil.getBoolean(data, AppParams.N_ADDR_VERIFIED));
			address.put(AppParams.ADDR_VERIFIED_NOTE, ParamUtil.getString(data, AppParams.S_ADDR_VERIFIED_NOTE));
			shipping.replace(AppParams.ADDRESS, address);

			el.put(AppParams.SHIPPING, shipping);
		} else {
			el.put(AppParams.SHIPPING, Collections.EMPTY_MAP);
		}
		
		el.put(AppParams.STORE_DOMAIN, ParamUtil.getString(data, AppParams.S_STORE_DOMAIN));
		el.put(AppParams.REFERENCE_ID, ParamUtil.getString(data, AppParams.S_REFERENCE_ORDER));
		el.put(AppParams.SOURCE, ParamUtil.getString(data, AppParams.S_SOURCE));
		el.put(AppParams.REQUIRE_REFUND, ParamUtil.getInt(data, AppParams.N_REQUIRE_REFUND));

		Double amount = ParamUtil.getDouble(data, AppParams.S_AMOUNT, 0);
		Double extra_fee = ParamUtil.getDouble(data, AppParams.S_EXTRA_FEE, 0);
		Double refundedAmount = ParamUtil.getDouble(data, AppParams.S_REFUNDED_AMOUNT, 0);
		String amountExceptRefunded = String.format("%.2f", Math.abs((amount + extra_fee) - refundedAmount));
		el.put(AppParams.EXTRA_FEE, extra_fee);
		el.put(AppParams.AMOUNT, amountExceptRefunded);

		if (itemList) {
			List<Map> orderItemList = ParamUtil
					.getListData(DropshipOrderProductService.search(id, ResourceStates.APPROVED), AppParams.ITEMS);
			el.put(AppParams.ITEMS, orderItemList);

			if (campaignInfo) {
				List<String> campaignIds = orderItemList.stream()
						.map(o -> ParamUtil.getString(ParamUtil.getMapData(o, AppParams.CAMPAIGN), AppParams.ID))
						.collect(Collectors.toList());

				List<Map> capaignList = new ArrayList<>();
				for (String campaignId : campaignIds) {
					capaignList.add(CampaignService.get(campaignId, true, false, true, false, true));
				}

				el.put(AppParams.CAMPAIGNS, capaignList);
			}
		}

		if (paymentInfo) {
			Map payments = PaymentService.search_by_order(id, ResourceStates.APPROVED, false);
			List<Map> paymentMaps = ParamUtil.getListData(payments, AppParams.PAYMENTS);
			if (!CollectionUtils.isEmpty(paymentMaps)) {
				el.put(AppParams.PAYMENT, paymentMaps.get(0));
			}
		}

		return el;
	}

	public static Map ignoreOrder(String orderId) throws SQLException, ParseException {
		return updateStateV2(orderId, ResourceStates.IGNORED);
	}

	public static Map updateStateV2(String orderId, String state) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_UPDATE_STATE_V2, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatV2(resultDataList.get(0), false, false, false);

		LOGGER.fine("=> updateState result: " + resultMap.toString());

		return resultMap;
	}
	
	public static Map updateExtraFeeAndState(String orderId, String state, int is_caculate_extra_fee, String method) throws SQLException, ParseException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, state);
		inputParams.put(3, is_caculate_extra_fee);
		inputParams.put(4, method);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_UPDATE_EXTRA_FEE_AND_STATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatV2(resultDataList.get(0), false, false, false);
		
		LOGGER.fine("=> updateState result: " + resultMap.toString());
		
		return resultMap;
	}

	public static void deleteOrderV2(String orderId) throws SQLException, ParseException {

		LOGGER.fine("Dropship Delete order products with orderId: " + orderId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_DELETE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=>Dropship Order delete result: " + resultCode);
	}

	public static Map payallQueuedOrder(String userId, String storeId) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, storeId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);
		outputParamsNames.put(6, AppParams.RESULT_TOTAL);
		outputParamsNames.put(7, "total_amount");

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PAYALL_QUEUED_ORDER,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);
		Double resultTotalAmount = ParamUtil.getDouble(resultMap, "total_amount");

		LOGGER.fine("=>Dropship Order amount result: " + resultTotalAmount);

		List<Map> formatList = new ArrayList<>();
		for (Map resultDataItem : resultDataList) {
			formatList.add(formatQueuedOrder(resultDataItem));
		}

		resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.TOTAL, resultTotal);
		resultMap.put(AppParams.DATA, formatList);
		resultMap.put("total_amount", resultTotalAmount);

		return resultMap;

	}

	private static Map formatQueuedOrder(Map data) {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		Double amount = ParamUtil.getDouble(data, AppParams.S_AMOUNT, 0);
		el.put(AppParams.AMOUNT, AppConstants.DEFAULT_AMOUNT_FORMAT.format(amount));
		return el;
	}

	public static boolean isExistOrderId(String orderId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, CHECK_DUPLICATE_ORDER_ID, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		return ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL, 0) > 0;
	}

	public static boolean isExistStoreIdReferenceOrderIdSource(String storeId, String referenceOrderId, String source)
			throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, referenceOrderId);
		inputParams.put(3, source);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, IS_EXIST_STORE_ID_REFERENCE_ORDER_ID_SOURCE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		return ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL, 0) > 0;
	}
	
//	public static Map getOrderByStoreIdReferenceOrderIdSource(String storeId, String referenceOrderId, String source)
//			throws SQLException, ParseException {
//		LOGGER.info("getOrderByStoreIdReferenceOrderIdSource: storeId= " + storeId + " - referenceOrderId= " + referenceOrderId + " - source= " + source);
//		Map inputParams = new LinkedHashMap<Integer, String>();
//		inputParams.put(1, storeId);
//		inputParams.put(2, referenceOrderId);
//		inputParams.put(3, source);
//		
//		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
//		outputParamsTypes.put(4, OracleTypes.NUMBER);
//		outputParamsTypes.put(5, OracleTypes.VARCHAR);
//		outputParamsTypes.put(6, OracleTypes.CURSOR);
//		
//		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
//		outputParamsNames.put(4, AppParams.RESULT_CODE);
//		outputParamsNames.put(5, AppParams.RESULT_MSG);
//		outputParamsNames.put(6, AppParams.RESULT_DATA);
//		
//		
//		Map resultMap = DBProcedureUtil.execute(dataSource, GET_ORDER_BY_ORIGINAL_ID, inputParams,
//				outputParamsTypes, outputParamsNames);
//		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
//		if (resultCode != HttpResponseStatus.OK.code()) {
//			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
//		}
//		
//		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
//		if (resultDataList.isEmpty()) {
//			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
//		}
//		resultMap = formatV2(resultDataList.get(0), true, false, false);
//		LOGGER.fine("=>  result: " + resultMap.toString());
//		return resultMap;
//	}
	
	public static Map getOrderByOriginalId(String original_id)
			throws SQLException, ParseException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, original_id);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		
		Map resultMap = DBProcedureUtil.execute(dataSource, GET_ORDER_BY_ORIGINAL_ID, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatV2(resultDataList.get(0), true, false, false);
		LOGGER.info("=>  result: " + resultMap.toString());
		return resultMap;
	}

	public static OrderCancellationCheck checkForApiCancellation(String orderId) throws SQLException {
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

		Map resultMap = DBProcedureUtil.execute(dataSource, ORDER_CANCELLATION_CHECK, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		OrderCancellationCheck check = null;

		List<Map> resultDaList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (CollectionUtils.isNotEmpty(resultDaList)) {
			check = OrderCancellationCheck.fromMap(resultDaList.get(0));
		}

		return check;
	}

	public static Boolean cancel(String orderId, Boolean isRequireRefund) throws SQLException {

		int requireRefundInt = isRequireRefund == null ? 0 : (isRequireRefund ? 1 : 0);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, requireRefundInt);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, CANCEL_ORDER_API, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			return false;
		}

		Boolean isSuccess = false;
		String state = ParamUtil.getString(resultDataList.get(0), AppParams.S_STATE);
		int requireRefund = ParamUtil.getInt(resultDataList.get(0), AppParams.N_REQUIRE_REFUND);

		return "DELETED".equalsIgnoreCase(state) || 1 == requireRefund;
	}
	
	public static void adjust(String trackingCode, String note) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, trackingCode);
		inputParams.put(2, note);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_ADJUST, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

	}

	public static String getDropshipOrderState(String orderId) throws SQLException {
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

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_ORDER_DROPSHIP_STATE, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList != null && resultDataList.isEmpty() == false) {
			return ParamUtil.getString(resultDataList.get(0), AppParams.S_STATE);
		}

		return "unknown";
	}

	public static List<Map> getDropshipOrderTrackings(String orderId) throws SQLException {
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

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_ORDER_DROPSHIP_TRACKINGS, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		Set<Map> set = resultDataList.stream().map(o -> {
			Map a = new LinkedHashMap<>();
			Map b = new LinkedHashMap<>();
			b.put(AppParams.CARRIER, ParamUtil.getString(o, AppParams.S_SHIPPING_CARRIER));
			b.put(AppParams.CODE, ParamUtil.getString(o, AppParams.S_SHIPPING_TRACKING_CODE));
			b.put(AppParams.URL, ParamUtil.getString(o, AppParams.S_SHIPPING_TRACKING_URL));
			a.put(ParamUtil.getString(o, AppParams.S_PACKAGE_ID), b);
			return a;
		}).collect(Collectors.toSet());
		List result = new ArrayList<Map>();
		result.addAll(set);
		return result;
	}
	
	public static Map updateStateOrderByOriginalId(String storeId, String originalId, String state) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, originalId);
		inputParams.put(3, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_UPDATE_STATE_BY_ORIGINAL_ID, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatV2(resultDataList.get(0), false, false, false);

		LOGGER.fine("=> updateState result: " + resultMap.toString());

		return resultMap;
	}
	
	public static Map insertDropshipOrder(DropshipOrderObj obj) throws SQLException, ParseException {
		LOGGER.info("=> obj= " + obj.toString());
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, obj.getOrderIdPrefix());
		inputParams.put(2, obj.getOrderAmount());
		inputParams.put(3, obj.getOrderCurrency());
		inputParams.put(4, obj.getState());
		inputParams.put(5, obj.getShippingId());
		inputParams.put(6, obj.getTrackingNumber());
		inputParams.put(7, obj.getNote());
		inputParams.put(8, obj.getChannel());
		inputParams.put(9, obj.getSubAmount());
		inputParams.put(10, obj.getShippingFee());
		inputParams.put(11, obj.getStoreId());
		inputParams.put(12, obj.getUserId());
		inputParams.put(13, obj.getReferenceOrderId());
		inputParams.put(14, obj.getTotalItems());
		inputParams.put(15, obj.getMinifiedJson());
		inputParams.put(16, obj.getSource());
		inputParams.put(17, obj.getAddrVerified());
		inputParams.put(18, obj.getShippingMethod());
		inputParams.put(19, obj.getOriginalId());
		inputParams.put(20, obj.getTaxAmount());

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(21, OracleTypes.NUMBER);
		outputParamsTypes.put(22, OracleTypes.VARCHAR);
		outputParamsTypes.put(23, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(21, AppParams.RESULT_CODE);
		outputParamsNames.put(22, AppParams.RESULT_MSG);
		outputParamsNames.put(23, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_INSERT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			// throw new OracleException(ParamUtil.getString(resultMap,
			// AppParams.RESULT_MSG));
			return Collections.EMPTY_MAP;
		}
		resultMap = formatV2(resultDataList.get(0), true, true, false);
		LOGGER.info("=> insertDropshipOrder result: " + resultMap.toString());
		return resultMap;
	}

	public static Map getBalance(String userId) throws SQLException {

		LOGGER.info("Dropship Order product Balance with orderId=" + userId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_BALANCE,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();
		if(resultDataList.size() > 0) {
			searchResultMap = formatBalance(resultDataList.get(0));
		} else {
			searchResultMap = new LinkedHashMap<>();

			searchResultMap.put(AppParams.CURRENT_BALANCE, 0);
			searchResultMap.put(AppParams.FULFILLMENT_COST_PAID, 0);
			searchResultMap.put(AppParams.FULFILLMENT_COST_PENDING, 0);
			searchResultMap.put(AppParams.PENDING_DEPOSIT, 0);
		}
		return searchResultMap;
	}

	private static Map formatBalance(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.CURRENT_BALANCE, ParamUtil.getDouble(queryData, AppParams.N_CURRENT_BALANCE));
		resultMap.put(AppParams.FULFILLMENT_COST_PAID, ParamUtil.getDouble(queryData, AppParams.N_FULFILLMENT_COST_PAID));
		resultMap.put(AppParams.FULFILLMENT_COST_PENDING, ParamUtil.getDouble(queryData, AppParams.N_FULFILLMENT_COST_PENDING));
		resultMap.put(AppParams.PENDING_DEPOSIT, ParamUtil.getDouble(queryData, AppParams.N_PENDING_DEPOSIT));

		return resultMap;

	}

	public static Map getTopUpHistory(String userId, String text, String state,int page, int pageSize, String startDate,
			String endDate, String sort, String dir) throws SQLException {

		LOGGER.info("Dropship Order product top up history  with orderId=" + userId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, text);
		inputParams.put(3, state);
		inputParams.put(4, page);
		inputParams.put(5, pageSize);
		inputParams.put(6, startDate);
		inputParams.put(7, endDate);
		inputParams.put(8, sort);
		inputParams.put(9, dir);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.VARCHAR);
		outputParamsTypes.put(12, OracleTypes.CURSOR);
		outputParamsTypes.put(13, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(10, AppParams.RESULT_CODE);
		outputParamsNames.put(11, AppParams.RESULT_MSG);
		outputParamsNames.put(12, AppParams.RESULT_DATA);
		outputParamsNames.put(13, AppParams.RESULT_TOTAL);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_TOP_UP_HISTORY,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();
		int total = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		for (Map resultDataMap : resultDataList) {

			dataList.add(formatTopUpHistory(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOP_UP_HISTORY, dataList);
		resultMap.put(AppParams.TOTAL, total);

		LOGGER.fine("=> Dropship Order product top up history result: "
				+ ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	private static Map formatTopUpHistory(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));
		resultMap.put(AppParams.TRANSACTION_ID, ParamUtil.getString(queryData, AppParams.S_TRANSACTION_ID));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.NOTE, ParamUtil.getString(queryData, AppParams.S_NOTE));
		resultMap.put(AppParams.AMOUNT, ParamUtil.getString(queryData, AppParams.N_AMOUNT));
		resultMap.put(AppParams.CREATE_DATE, ParamUtil.getString(queryData, AppParams.D_CREATE));
		resultMap.put(AppParams.METHOD, ParamUtil.getString(queryData, AppParams.S_METHOD));
		resultMap.put(AppParams.EXTRA_FEE, ParamUtil.getDouble(queryData, AppParams.N_EXTRA_FEE));

		return resultMap;

	}
	
	public static Map payallQueuedOrderSubAccount(String userId, String listStoreId) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, listStoreId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);
		outputParamsNames.put(6, AppParams.RESULT_TOTAL);
		outputParamsNames.put(7, "total_amount");

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_PAYALL_QUEUED_ORDER_SUB_ACCOUNT,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);
		Double resultTotalAmount = ParamUtil.getDouble(resultMap, "total_amount");

		LOGGER.fine("=>Dropship Order amount result: " + resultTotalAmount);

		List<Map> formatList = new ArrayList<>();
		for (Map resultDataItem : resultDataList) {
			formatList.add(formatQueuedOrder(resultDataItem));
		}

		resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.TOTAL, resultTotal);
		resultMap.put(AppParams.DATA, formatList);
		resultMap.put("total_amount", resultTotalAmount);

		return resultMap;

	}
	
	public static Map searchTrackingOrder(String userId, String storeId, String channel, String state, String startDate,
			String endDate, int page, int pageSize, String sort, String dir, String orderId, String source)
			throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, storeId);
		inputParams.put(3, channel);
		inputParams.put(4, state);
		inputParams.put(5, startDate);
		inputParams.put(6, endDate);
		inputParams.put(7, page);
		inputParams.put(8, pageSize);
		inputParams.put(9, sort);
		inputParams.put(10, dir);
		inputParams.put(11, orderId);
		inputParams.put(12, source);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);
		outputParamsTypes.put(15, OracleTypes.CURSOR);
		outputParamsTypes.put(16, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);
		outputParamsNames.put(15, AppParams.RESULT_DATA);
		outputParamsNames.put(16, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_SEARCH_TRACKING, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);

		List<Map> formatList = new ArrayList<>();
		for (Map resultDataItem : resultDataList) {
			formatList.add(formatTracking(resultDataItem));
		}

		resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.TOTAL, resultTotal);
		resultMap.put(AppParams.DATA, formatList);

		LOGGER.info("=> DropshipOrderService.searchTrackingOrder()-  result: " + resultMap.toString());

		return resultMap;
	}
	
	private static Map formatTracking(Map data) throws SQLException, ParseException {
		Map el = new LinkedHashMap<>();
		String id = ParamUtil.getString(data, AppParams.S_ID);
		el.put(AppParams.ID, id);
		el.put(AppParams.CURRENCY, ParamUtil.getString(data, AppParams.S_CURRENCY));
		el.put(AppParams.SUB_AMOUNT, ParamUtil.getString(data, AppParams.S_SUB_AMOUNT));
		el.put(AppParams.SHIPPING_FEE, ParamUtil.getString(data, AppParams.S_SHIPPING_FEE));
		el.put(AppParams.TAX_AMOUNT, ParamUtil.getString(data, AppParams.S_TAX_AMOUNT));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		el.put(AppParams.QUANTITY, ParamUtil.getString(data, AppParams.N_QUANTITY));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		el.put(AppParams.TRACKING_CODE, ParamUtil.getString(data, AppParams.S_TRACKING_CODE));
		el.put(AppParams.ORDER_DATE, ParamUtil.getString(data, AppParams.D_ORDER));
		el.put(AppParams.NOTE, ParamUtil.getString(data, AppParams.S_NOTE));
		el.put(AppParams.CHANNEL, ParamUtil.getString(data, AppParams.S_CHANNEL));
		el.put(AppParams.USER_ID, ParamUtil.getString(data, AppParams.S_USER_ID));
		el.put(AppParams.STORE_ID, ParamUtil.getString(data, AppParams.S_STORE_ID));
		el.put(AppParams.STORE_NAME, ParamUtil.getString(data, AppParams.S_STORE_NAME));
		el.put(AppParams.SHIPPING, ShippingService.get(ParamUtil.getString(data, AppParams.S_SHIPPING_ID)));
		el.put(AppParams.STORE_DOMAIN, ParamUtil.getString(data, AppParams.S_STORE_DOMAIN));
		el.put(AppParams.REFERENCE_ID, ParamUtil.getString(data, AppParams.S_REFERENCE_ORDER));
		el.put(AppParams.SOURCE, ParamUtil.getString(data, AppParams.S_SOURCE));
		el.put(AppParams.REQUIRE_REFUND, ParamUtil.getInt(data, AppParams.N_REQUIRE_REFUND));
		el.put(AppParams.FULFILL_STATE, calculateFulfillState(data));

		Double amount = ParamUtil.getDouble(data, AppParams.S_AMOUNT, 0);
		Double extra_fee = ParamUtil.getDouble(data, AppParams.S_EXTRA_FEE, 0);
		Double refundedAmount = ParamUtil.getDouble(data, AppParams.S_REFUNDED_AMOUNT, 0);
		String amountExceptRefunded = String.format("%.2f", Math.abs((amount + extra_fee) - refundedAmount));
		el.put(AppParams.EXTRA_FEE, extra_fee);
		el.put(AppParams.AMOUNT, amountExceptRefunded);

		List<String> listProductName = Arrays.asList( ParamUtil.getString(data, AppParams.S_PRODUCT_NAME).split(","));
		el.put(AppParams.PRODUCT_NAME, listProductName);
		el.put(AppParams.SHIPPING_METHOD, ParamUtil.getString(data, AppParams.S_SHIPPING_METHOD));
		el.put(AppParams.PACKAGE_ID, ParamUtil.getString(data, AppParams.S_PACKAGE_ID));
		el.put(AppParams.CARRIER, ParamUtil.getString(data, AppParams.S_SHIPPING_CARRIER));
		el.put(AppParams.CODE, ParamUtil.getString(data, AppParams.S_SHIPPING_TRACKING_CODE));
		el.put(AppParams.URL, ParamUtil.getString(data, AppParams.S_SHIPPING_TRACKING_URL));
		el.put(AppParams.LABEL, ParamUtil.getString(data, AppParams.S_SHIPPING_LABLE_URL));

		return el;
	}
	
	public static Map searchTrackingOrderSubAccount(String userId, String storeId, String channel, String state, String startDate,
			String endDate, int page, int pageSize, String sort, String dir, String orderId, String source)
			throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, storeId);
		inputParams.put(3, channel);
		inputParams.put(4, state);
		inputParams.put(5, startDate);
		inputParams.put(6, endDate);
		inputParams.put(7, page);
		inputParams.put(8, pageSize);
		inputParams.put(9, sort);
		inputParams.put(10, dir);
		inputParams.put(11, orderId);
		inputParams.put(12, source);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);
		outputParamsTypes.put(15, OracleTypes.CURSOR);
		outputParamsTypes.put(16, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);
		outputParamsNames.put(15, AppParams.RESULT_DATA);
		outputParamsNames.put(16, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_ORDER_SEARCH_TRACKING_SUB_ACCOUNT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);

		List<Map> formatList = new ArrayList<>();
		for (Map resultDataItem : resultDataList) {
			formatList.add(formatTracking(resultDataItem));
		}

		resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.TOTAL, resultTotal);
		resultMap.put(AppParams.DATA, formatList);

		LOGGER.info("=> DropshipOrderService.searchTrackingOrderSubAccount()-  result: " + resultMap.toString());

		return resultMap;
	}
	
	private static final Logger LOGGER = Logger.getLogger(DropshipOrderService.class.getName());
}