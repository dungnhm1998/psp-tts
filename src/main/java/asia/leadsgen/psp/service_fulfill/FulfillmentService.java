package asia.leadsgen.psp.service_fulfill;

import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.CMSCreateLabelResult;
import asia.leadsgen.psp.obj.Fulfillment;
import asia.leadsgen.psp.obj.FulfillmentDetail;
import asia.leadsgen.psp.obj.FulfillmentDetailObj;
import asia.leadsgen.psp.obj.FulfillmentObj;
import asia.leadsgen.psp.obj.PartnerObj;
import asia.leadsgen.psp.obj.Shipping;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.driver.OracleSQLException;

public class FulfillmentService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	static final String FF_DETAIL_GET_ORDERS_ROSALINDA = "{call PKG_FF_FULFILLMENT_DETAIL.rosalinda_get_orders(?,?,?,?,?,?,?,?)}";
	public static final String FULFILL_SCHEDULER_ASSIGN_PRODUCTS_TO_PARTNER = "{call PKG_FULFILL_SCHEDULER.assign_products_to_partner(?,?,?,?,?)}";
	private static final String INSERT_FULFILLMENT = "{call PKG_FULFILLMENT_NEW.insert_fulfillment(?)}";
	private static final String UPDATE_PRINT_URL_AND_SKU_AND_COST = "{call PKG_FULFILLMENT_NEW.update_print_url_and_sku_and_cost(?)}";
	
	
	public static Map searchByOrderId(String orderId) throws SQLException {

		Map inputParams;
		inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.FULFILLMENT_SEARCH_BY_ORDER_ID, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			resultMap = new LinkedHashMap<>();
		} else {
			resultMap = format(resultDataList.get(0));
		}

		LOGGER.fine("=> searchByOrderId result: " + resultMap.toString());

		return resultMap;
	}

	private static Map format(Map data) {

		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.FULFILLMENT_ID, ParamUtil.getString(data, AppParams.S_FULFILLMENT_ID));
		el.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(data, AppParams.S_CAMPAIGN_ID));
		el.put(AppParams.ORDER_ID, ParamUtil.getString(data, AppParams.S_ORDER_ID));
		return el;
	}

	public static Fulfillment searchFulfillmentDetail(String fulfillmentId)
			throws SQLException, IllegalAccessException, InvocationTargetException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, fulfillmentId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		outputParamsNames.put(5, AppParams.FULFILLMENT);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.FULFILLMENT_SEARCH_FF_DETAIL, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> ffDetails = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<FulfillmentDetail> fulfillmentDetails = new ArrayList<FulfillmentDetail>();

		if (!ffDetails.isEmpty()) {
			fulfillmentDetails = convertFulfillmentDetails(ffDetails);
		}

		PartnerObj partner = null;
		List<Map> fulfillments = ParamUtil.getListData(resultMap, AppParams.FULFILLMENT);

		String campId = null, campTitle = null;
		int total = 0;
		if (!fulfillments.isEmpty()) {
			partner = new PartnerObj();
			partner.setId(ParamUtil.getString(fulfillments.get(0), AppParams.S_PARTNER_ID));
			partner.setName(ParamUtil.getString(fulfillments.get(0), AppParams.S_NAME));
			partner.setEmail(ParamUtil.getString(fulfillments.get(0), AppParams.S_EMAIL));

			campId = ParamUtil.getString(fulfillments.get(0), AppParams.S_CAMPAIGN_ID);
			campTitle = ParamUtil.getString(fulfillments.get(0), AppParams.S_CAMPAIGN_TITLE);
			total = ParamUtil.getInt(fulfillments.get(0), AppParams.N_TOTAL);
		}

		Fulfillment fulfillment = new Fulfillment(fulfillmentId, campId, campTitle, total, partner, fulfillmentDetails);

		LOGGER.fine("=> fulfillment result: " + resultMap.toString());

		return fulfillment;
	}

	public static Fulfillment manualSearchFulfillmentDetail(String fulfillmentId)
			throws SQLException, IllegalAccessException, InvocationTargetException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, fulfillmentId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		outputParamsNames.put(5, AppParams.FULFILLMENT);

		String query = "{call PKG_FULFILLMENT.manuall_search_ff_detail_for_creating_label(?,?,?,?,?)}";

		Map resultMap = DBProcedureUtil.execute(dataSource, query, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> ffDetails = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<FulfillmentDetail> fulfillmentDetails = new ArrayList<FulfillmentDetail>();

		if (!ffDetails.isEmpty()) {
			fulfillmentDetails = convertFulfillmentDetails(ffDetails);
		}

		PartnerObj partner = null;
		List<Map> fulfillments = ParamUtil.getListData(resultMap, AppParams.FULFILLMENT);

		String campId = null, campTitle = null;
		int total = 0;
		if (!fulfillments.isEmpty()) {
			partner = new PartnerObj();
			partner.setId(ParamUtil.getString(fulfillments.get(0), AppParams.S_PARTNER_ID));
			partner.setName(ParamUtil.getString(fulfillments.get(0), AppParams.S_NAME));
			partner.setEmail(ParamUtil.getString(fulfillments.get(0), AppParams.S_EMAIL));

			campId = ParamUtil.getString(fulfillments.get(0), AppParams.S_CAMPAIGN_ID);
			campTitle = ParamUtil.getString(fulfillments.get(0), AppParams.S_CAMPAIGN_TITLE);
			total = ParamUtil.getInt(fulfillments.get(0), AppParams.N_TOTAL);
		}

		Fulfillment fulfillment = new Fulfillment(fulfillmentId, campId, campTitle, total, partner, fulfillmentDetails);

		LOGGER.fine("=> fulfillment result: " + resultMap.toString());

		return fulfillment;
	}

	private static List<FulfillmentDetail> convertFulfillmentDetails(List<Map> fulfillmentDetailMaps) {

		List<FulfillmentDetail> fulfillmentDetails = new ArrayList<FulfillmentDetail>();

		for (Map<String, Object> fulfillmentDetailMap : fulfillmentDetailMaps) {
			fulfillmentDetails.add(formatFulfillmentDetail(fulfillmentDetailMap));
		}

		return fulfillmentDetails;
	}

	public static void manuallUpdateShippingFulfillmentDetail(CMSCreateLabelResult lbResult) throws SQLException {

		manuallUpdateShippingFulfillmentDetail(lbResult.getFfDetailId(), lbResult.getCarrier(),
				lbResult.getTrackingCode(), lbResult.getTrackingUrl(), lbResult.getUrl());

	}

	public static void manuallUpdateShippingFulfillmentDetail(String id, String carrier, String code, String url,
			String labelUrl) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();

		inputParams.put(1, id);
		inputParams.put(2, carrier);
		inputParams.put(3, code);
		inputParams.put(4, url);
		inputParams.put(5, labelUrl);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MANUAL_UPDATE_SHIPPING, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

	}

	public static void updateShippingFulfillmentDetail(CMSCreateLabelResult lbResult) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();

		inputParams.put(1, lbResult.getFfDetailId());
		inputParams.put(2, lbResult.getCarrier());
		inputParams.put(3, lbResult.getTrackingCode());
		inputParams.put(4, lbResult.getTrackingUrl());
		inputParams.put(5, lbResult.getUrl());
		inputParams.put(6, ResourceStates.PACKED);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.VARCHAR);
		outputParamsTypes.put(9, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(7, AppParams.RESULT_CODE);
		outputParamsNames.put(8, AppParams.RESULT_MSG);
		outputParamsNames.put(9, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.FULFILLMENT_UPDATE_SHIPPING, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

	}

	private static FulfillmentDetail formatFulfillmentDetail(Map<String, Object> data) {

		FulfillmentDetail dt = new FulfillmentDetail();

		dt.setId(ParamUtil.getString(data, AppParams.S_ID));
		dt.setFulfillmentId(ParamUtil.getString(data, AppParams.S_FULFILLMENT_ID));
		dt.setCampaignId(ParamUtil.getString(data, AppParams.S_CAMPAIGN_ID));
		dt.setCampaignTitle(ParamUtil.getString(data, AppParams.S_CAMPAIGN_TITLE));
		dt.setBaseId(ParamUtil.getString(data, AppParams.S_BASE_ID));
		dt.setOrderId(ParamUtil.getString(data, AppParams.S_ORDER_ID));
		dt.setTrackingCode(ParamUtil.getString(data, AppParams.S_TRACKING_CODE));
		dt.setProductId(ParamUtil.getString(data, AppParams.S_PRODUCT_ID));
		dt.setProductName(ParamUtil.getString(data, AppParams.S_PRODUCT_NAME));
		dt.setVariantId(ParamUtil.getString(data, AppParams.S_VARIANT_ID));
		dt.setSize(ParamUtil.getString(data, AppParams.S_SIZE));
		dt.setColor(ParamUtil.getString(data, AppParams.S_COLOR));
		dt.setColorName(ParamUtil.getString(data, AppParams.S_COLOR_NAME));
		dt.setQuantity(ParamUtil.getInt(data, AppParams.N_QUANTITY));
		dt.setShippingId(ParamUtil.getString(data, AppParams.S_SHIPPING_ID));
		dt.setSendEmail(ParamUtil.getBoolean(data, AppParams.N_SEND_EMAIL));
		dt.setPartnerId(ParamUtil.getString(data, AppParams.S_PARTNER_ID));
		dt.setBarcodeUrl(ParamUtil.getString(data, AppParams.S_BARCODE_URL));
		dt.setShippingCarrier(ParamUtil.getString(data, AppParams.S_SHIPPING_CARRIER));
		dt.setShippingLabelUrl(ParamUtil.getString(data, AppParams.S_SHIPPING_LABLE_URL));
		dt.setShippingTrackingCode(ParamUtil.getString(data, AppParams.S_SHIPPING_TRACKING_CODE));
		dt.setShippingTrackingUrl(ParamUtil.getString(data, AppParams.S_SHIPPING_TRACKING_URL));
		dt.setDateCreate(ParamUtil.getString(data, AppParams.D_CREATE));
		dt.setState(ParamUtil.getString(data, AppParams.S_STATE));
		dt.setProcessedPayout(ParamUtil.getBoolean(data, AppParams.N_PAYOUT_PROCESSED));
		dt.setTariffNumber(ParamUtil.getString(data, AppParams.S_TARIFF_NUMBER));
		dt.setShippingValue(ParamUtil.getString(data, AppParams.S_SHIPPING_VALUE));
		dt.setFullfilSyncState(ParamUtil.getString(data, AppParams.S_FULFILL_SYNC_STATE));
		dt.setSource(ParamUtil.getString(data, AppParams.S_SOURCE));
		dt.setLineItemId(ParamUtil.getString(data, AppParams.S_LINE_ITEM_ID));
		dt.setPackageWeight(ParamUtil.getDouble(data, AppParams.S_PACKAGE_WEIGHT, 0.0));

		Shipping shipping = new Shipping();
		shipping.setName(ParamUtil.getString(data, AppParams.S_SHIPPING_NAME));
		shipping.setEmail(ParamUtil.getString(data, AppParams.S_SHIPPING_EMAIL));
		shipping.setPhone(ParamUtil.getString(data, AppParams.S_SHIPPING_PHONE));
		shipping.setLine1(ParamUtil.getString(data, AppParams.S_SHIPPING_ADD_LINE1));
		shipping.setLine2(ParamUtil.getString(data, AppParams.S_SHIPPING_ADD_LINE2));
		shipping.setCity(ParamUtil.getString(data, AppParams.S_SHIPPING_ADD_CITY));
		shipping.setState(ParamUtil.getString(data, AppParams.S_SHIPPING_ADD_STATE));
		shipping.setPostalCode(ParamUtil.getString(data, AppParams.S_SHIPPING_POSTAL_CODE));
		shipping.setCountryCode(ParamUtil.getString(data, AppParams.S_SHIPPING_COUNTRY_CODE));
		shipping.setCountryName(ParamUtil.getString(data, AppParams.S_SHIPPING_COUNTRY_NAME));
		shipping.setShipAsGift(ParamUtil.getInt(data, AppParams.N_SHIPPING_GIFT));

		dt.setShipping(shipping);

		return dt;
	}

	public static Map updateCreatingLabelState(String fulfillmentId, int isAutoCreating) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, fulfillmentId);
		inputParams.put(2, isAutoCreating);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.FULFILLMENT_UPDATE_CREATING_LABEL_STATE,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> updateCreatingLabelState result: " + resultMap.toString());

		return Collections.EMPTY_MAP;
	}

	public static Fulfillment getCreatedLabels(String fulfillmentId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, fulfillmentId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		outputParamsNames.put(5, AppParams.FULFILLMENT);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.FULFILLMENT_GET_CREATED_LABELS, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<FulfillmentDetail> fulfillmentDetails = new ArrayList<>();

		if (!resultDataList.isEmpty()) {
			fulfillmentDetails = convertFulfillmentDetails(resultDataList);
		}

		PartnerObj partner = null;
		List<Map> fulfillments = ParamUtil.getListData(resultMap, AppParams.FULFILLMENT);

		String campId = null, campTitle = null;
		int total = 0;
		if (!fulfillments.isEmpty()) {
			partner = new PartnerObj();
			partner.setId(ParamUtil.getString(fulfillments.get(0), AppParams.S_PARTNER_ID));
			partner.setName(ParamUtil.getString(fulfillments.get(0), AppParams.S_NAME));
			partner.setEmail(ParamUtil.getString(fulfillments.get(0), AppParams.S_EMAIL));

			campId = ParamUtil.getString(fulfillments.get(0), AppParams.S_CAMPAIGN_ID);
			campTitle = ParamUtil.getString(fulfillments.get(0), AppParams.S_CAMPAIGN_TITLE);
			total = ParamUtil.getInt(fulfillments.get(0), AppParams.N_TOTAL);
		}

		Fulfillment fulfillment = new Fulfillment(fulfillmentId, campId, campTitle, total, partner, fulfillmentDetails);

		LOGGER.fine("=> fulfillment result: " + resultMap.toString());

		return fulfillment;
	}

	public static List<FulfillmentDetail> splitFfDetailPackage(String ffDetailId, int itemPerPackage)
			throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, ffDetailId);
		inputParams.put(2, itemPerPackage);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.FULFILLMENT_SPLIT_PACKAGE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		List<FulfillmentDetail> resultDetails = new ArrayList<>();
		if (!resultDataList.isEmpty()) {
			resultDetails = convertFulfillmentDetails(resultDataList);
		}
		LOGGER.fine("=> splitFfDetailPackage result: " + resultMap.toString());
		return resultDetails;
	}

	public static List<Map> getApparelFulfillment(String startDate, String endDate, String fulfillmentIds)
			throws SQLException {

		String GET_APPAREL_FULFILLMENT = "{call PKG_FULFILLMENT.get_apparel_fulfillment(?,?,?,?,?,?)}";

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, startDate);
		inputParams.put(2, endDate);
		inputParams.put(3, fulfillmentIds);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map report = DBProcedureUtil.execute(dataSource, GET_APPAREL_FULFILLMENT, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(report, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleSQLException();
		}
		return ParamUtil.getListData(report, AppParams.RESULT_DATA);
	}

	public static List<Map> getApparelFulfillmentDetail(String fulfillmentId, String orderBy) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, fulfillmentId);
		inputParams.put(2, orderBy);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map report = DBProcedureUtil.execute(dataSource, DBProcedurePool.GET_APPAREL_FULFILLMENT_DETAIL, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(report, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleSQLException();
		}
		return ParamUtil.getListData(report, AppParams.RESULT_DATA);
	}
	
	public static List<Map> rosalindaGetOrders(int page, int pageSize, String date, String ref_type, String ref_value) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, page);
		inputParams.put(2, pageSize);
		inputParams.put(3, date);
		inputParams.put(4, ref_type);
		inputParams.put(5, ref_value);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, FF_DETAIL_GET_ORDERS_ROSALINDA, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		LOGGER.info("get orders rosalinda result: " + resultMap.toString());

		return resultDataList;
	}
	
	public static String assignItemToPartner(FulfillmentDetailObj item) throws SQLException {
		LOGGER.info(">>>>> assignItemToPartner item " + item.toString());
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, item.getPartnerId());
		inputParams.put(2, item.getId());
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource,FULFILL_SCHEDULER_ASSIGN_PRODUCTS_TO_PARTNER, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		String packageId = null;
		if (CollectionUtils.isNotEmpty(resultDataList)) {
			packageId = ParamUtil.getString(resultDataList.get(0), AppParams.S_ID);
		}
		return packageId;
	}
	
	public static void insertFulfillment(List<FulfillmentObj> items){
		LOGGER.info("insertFulfillment " + items.size() + " items");
		try (Connection hikariCon = dataSource.getConnection()) {
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				FulfillmentObj[] arr = new FulfillmentObj[items.size()];
				arr = items.toArray(arr);
				java.sql.Array orclArr = con.createOracleArray("FULFILLMENT_TYPE_T", arr);
				CallableStatement cstmt = con.prepareCall(INSERT_FULFILLMENT);
				cstmt.setArray(1, orclArr);
				cstmt.execute();
			}
		} catch (Exception e) {
			LOGGER.severe("insertFulfillment Error " + e.getMessage());
		}
	}
	
	public static void updatePrintUrlAndSkuAndCost(List<FulfillmentDetailObj> items) throws SQLException {
		try (Connection hikariCon = dataSource.getConnection()) {
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				FulfillmentDetailObj[] arr = new FulfillmentDetailObj[items.size()];
				arr = items.toArray(arr);
				java.sql.Array orclArr = con.createOracleArray("FULFILLMENT_DETAIL_TYPE_T", arr);
				CallableStatement cstmt = con.prepareCall(UPDATE_PRINT_URL_AND_SKU_AND_COST);
				cstmt.setArray(1, orclArr);
				cstmt.execute();
			}
		}
	}
	
	private static final Logger LOGGER = Logger.getLogger(FulfillmentService.class.getName());

}
