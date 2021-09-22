package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.DomainObj;
import asia.leadsgen.psp.obj.OrderItemObj;
import asia.leadsgen.psp.obj.OrderObj;
import asia.leadsgen.psp.obj.PaymentObj;
import asia.leadsgen.psp.obj.ShippingAddressObj;
import asia.leadsgen.psp.obj.ShippingObj;
import asia.leadsgen.psp.util.AppConstants;
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
public class OrderService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static OrderObj findByTrackingCode(String trackingCode) throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, trackingCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_FIND_BY_TRACKING_CODE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		OrderObj orderObj = formatOrderObj(resultDataList.get(0));

		LOGGER.info("=> findByTrackingCode result: " + orderObj.toString());

		return orderObj;
	}

	private static OrderObj formatOrderObj(Map resultData) throws ParseException {
		OrderObj orderObj = new OrderObj();
		orderObj.setId(ParamUtil.getString(resultData, AppParams.S_ORDER_ID));
		orderObj.setTrackingCode(ParamUtil.getString(resultData, AppParams.S_TRACKING_CODE));
		orderObj.setOrderAmount(ParamUtil.getString(resultData, AppParams.S_AMOUNT));

		ShippingObj shippingObj = new ShippingObj();
		shippingObj.setId(ParamUtil.getString(resultData, AppParams.S_SHIPPING_ID));
		shippingObj.setCustomerName(ParamUtil.getString(resultData, AppParams.S_SHIPPING_NAME));
		shippingObj.setCustomerPhone(ParamUtil.getString(resultData, AppParams.S_SHIPPING_PHONE));
		shippingObj.setCustomerEmail(ParamUtil.getString(resultData, AppParams.S_SHIPPING_EMAIL));

		ShippingAddressObj shippingAddressObj = new ShippingAddressObj();
		shippingAddressObj.setLine1(ParamUtil.getString(resultData, AppParams.S_SHIPPING_LINE1));
		shippingAddressObj.setLine2(ParamUtil.getString(resultData, AppParams.S_SHIPPING_LINE2));
		shippingAddressObj.setCity(ParamUtil.getString(resultData, AppParams.S_SHIPPING_CITY));
		shippingAddressObj.setState(ParamUtil.getString(resultData, AppParams.S_SHIPPING_STATE));
		shippingAddressObj.setPostalCode(ParamUtil.getString(resultData, AppParams.S_SHIPPING_POSTAL_CODE));
		shippingAddressObj.setCountry(ParamUtil.getString(resultData, AppParams.S_SHIPPING_COUNTRY_CODE));

		shippingObj.setShippingAddress(shippingAddressObj);
		orderObj.setShipping(shippingObj);

		DomainObj domain = new DomainObj();
		domain.setName(ParamUtil.getString(resultData, AppParams.S_DOMAIN_NAME));
		domain.setLogo(ParamUtil.getString(resultData, AppParams.S_DOMAIN_LOGO_URL));
		orderObj.setDomainObj(domain);

		PaymentObj paymentObj = new PaymentObj();
		paymentObj.setMethod(ParamUtil.getString(resultData, AppParams.S_PAYMENT_METHOD));

		String paymentCreateDate = ParamUtil.getString(resultData, AppParams.D_CREATE_PAYMENT);
		SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);
		Date payDate = dateFormat.parse(paymentCreateDate);
		dateFormat = new SimpleDateFormat("EEEE MMM dd, yyyy");
		paymentObj.setCreateDate(dateFormat.format(payDate));

		orderObj.setPaymentObj(paymentObj);

		return orderObj;
	}

	public static List<OrderItemObj> searchItems(String orderId) throws SQLException {

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

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_SEARCH_ITEMS, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<OrderItemObj> orderItems = Collections.EMPTY_LIST;

		if (!resultDataList.isEmpty()) {
			orderItems = formatOrderItems(resultDataList);
		}

		LOGGER.fine("=> searchShippingItem result: " + resultMap.toString());

		return orderItems;
	}

	public static List<OrderItemObj> searchShippingItem(String orderId) throws SQLException {

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

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_SEARCH_SHIPPING_ITEM, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<OrderItemObj> orderItems = Collections.EMPTY_LIST;

		if (!resultDataList.isEmpty()) {
			orderItems = formatOrderItems(resultDataList);
		}

		LOGGER.fine("=> searchShippingItem result: " + resultMap.toString());

		return orderItems;
	}

	public static List<OrderItemObj> searchItemByFfDetailIds(String ffDetailIds) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, ffDetailIds);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_SEARCH_ITEMS_BY_FF_DETAIL_IDS,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<OrderItemObj> orderItems = Collections.EMPTY_LIST;

		if (!resultDataList.isEmpty()) {
			orderItems = formatOrderItems(resultDataList);
		}

		LOGGER.fine("=> searchShippingItem result: " + resultMap.toString());

		return orderItems;
	}

	private static List<OrderItemObj> formatOrderItems(List<Map> resultDataList) {
		List<OrderItemObj> itemObjs = new ArrayList<>();
		for (Map resultData : resultDataList) {
			OrderItemObj itemObj = new OrderItemObj();
			itemObj.setName(ParamUtil.getString(resultData, AppParams.S_NAME));
			itemObj.setFfDetailId(ParamUtil.getString(resultData, AppParams.S_FULFILLMENT_DETAIL_ID));
			itemObj.setImageUrl(ParamUtil.getString(resultData, AppParams.S_IMAGE_URL));
			itemObj.setName(ParamUtil.getString(resultData, AppParams.S_NAME));
			itemObj.setAmount(ParamUtil.getString(resultData, AppParams.S_AMOUNT));
			itemObj.setShippingPrice(ParamUtil.getString(resultData, AppParams.S_SHIPPING_FEE));
			itemObj.setPrice(ParamUtil.getString(resultData, AppParams.S_PRICE));
			itemObj.setSizeName(ParamUtil.getString(resultData, AppParams.S_SIZE_NAME));
			itemObj.setQuantity(ParamUtil.getInt(resultData, AppParams.N_QUANTITY));
			itemObj.setState(ParamUtil.getString(resultData, AppParams.S_STATE));
			itemObjs.add(itemObj);
		}
		return itemObjs;
	}

	public static Map<String, Object> get(String id, boolean itemList, boolean fulfillmentInfo, boolean refundInfo)
			throws SQLException {

		LOGGER.fine("Order lookup with id=" + id);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		Map<String, Object> resultMap = format(resultDataList.get(0), itemList, fulfillmentInfo, refundInfo);

		LOGGER.fine("=> Order look up result: " + resultMap.toString());

		return resultMap;
	}

	public static Map<String, Object> orderDetail(String id, String userId) throws SQLException {

		LOGGER.fine("orderDetail lookup with id=" + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_PRD_GET_BY_ORDER_ID,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			resultDataList = new ArrayList<>();
		}

		Map resultMap = formatOrderDetailData(resultDataList, shippingOrder(id), orderRefundDetail(id, userId));

		return resultMap;
	}

	public static List<Map> orderRefundDetail(String id, String userId) throws SQLException {

		LOGGER.fine("order refund lookup with id=" + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_PRD_REFUND_GET_BY_ORDER_ID,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			resultDataList = new ArrayList<>();
		}
		return resultDataList;
	}

	public static Map<String, Object> shippingOrder(String id) throws SQLException {

		LOGGER.fine("shipping with id=" + id);

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

		Map executeResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.SHIPPING_GET_BY_ORDER_ID,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(executeResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(executeResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(executeResultMap, AppParams.RESULT_DATA);

		if (!resultDataList.isEmpty()) {
			return resultDataList.get(0);
		}
		return new LinkedHashMap();
	}

	public static Map<String, Object> listOrdersOfAnUser(String userId, String domain, String startTime, String endTime,
			String email, String orderStatus, String fulfillment, String country, int page, int pageSize)
			throws SQLException {

		LOGGER.fine("Order user id:" + userId + ", domain:" + domain + ", startTime:" + startTime + ", endTime:"
				+ endTime + ", orderStatus:" + orderStatus + ", fulfillment:" + fulfillment + ", country:" + country);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domain);
		inputParams.put(3, startTime);
		inputParams.put(4, endTime);
		inputParams.put(5, email);
		inputParams.put(6, orderStatus);
		inputParams.put(7, fulfillment);
		inputParams.put(8, country);

		inputParams.put(9, page);
		inputParams.put(10, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.VARCHAR);
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(11, AppParams.RESULT_CODE);
		outputParamsNames.put(12, AppParams.RESULT_MSG);
		outputParamsNames.put(13, AppParams.RESULT_TOTAL);
		outputParamsNames.put(14, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_OVERVIEW, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {
			dataList.add(formatOrderOverview(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.ORDERS, dataList);
		LOGGER.fine("=> Order overview look up result: " + resultMap.toString());
		return resultMap;

	}

	public static Map copyToNewOrder(String orderId, String trackingNumber) throws SQLException {
		LOGGER.fine("Order lookup with id=" + orderId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, trackingNumber);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_CLONE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		Map resultMap = format(resultDataList.get(0), false, false, false);

		LOGGER.fine("=> Order look up result: " + resultMap.toString());

		return resultMap;
	}

	public static Map updateMailCampId(String orderId, String mailCampId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, mailCampId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_UPDATE_MAIL_CAMP_ID, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		return Collections.EMPTY_MAP;
	}

	public static Map track(String trackingCode) throws SQLException {

		LOGGER.fine("Order lookup with trackingCode=" + trackingCode);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, trackingCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_TRACKING, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		Map resultMap = trackingFormat(resultDataList.get(0));

		LOGGER.fine("=> Order look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static String findFirstCampaignOfOrder(String orderId) throws SQLException {

		LOGGER.info("=> findFirstCampaignOfOrder : " + orderId);

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

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.FIND_FIRST_CAMPAIGN_OF_ORDER, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		String campaignId = ParamUtil.getString(resultDataList.get(0), AppParams.S_CAMPAIGN_ID);

		LOGGER.fine("=> findFirstCampaignOfOrder result: " + campaignId);

		return campaignId;
	}

	public static Map insert(String orderIdPrefix, String userId, String currency, String shippingId,
			String trackingNumber, String source, String medium, String campaign, String content, String utm_source,
			String utm_term, String utm_title, String sourceIp, String countryCode, String countryName, String city,
			String device, String os, String browser, String userAgent, String osVersion, String browserVersion,
			String deviceBrand, String deviceModel, String stateRegion, String domainName) throws SQLException {

		logInsertion(userId, currency, shippingId, trackingNumber, source, medium, campaign, content, countryCode, city,
				device, os, browser);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderIdPrefix);
		inputParams.put(2, userId);
		inputParams.put(3, currency);
		inputParams.put(4, shippingId);
		inputParams.put(5, trackingNumber);
		inputParams.put(6, source);
		inputParams.put(7, medium);
		inputParams.put(8, campaign);
		inputParams.put(9, content);
		inputParams.put(10, utm_source);
		inputParams.put(11, utm_term);
		inputParams.put(12, utm_title);
		inputParams.put(13, sourceIp);
		inputParams.put(14, countryCode);
		inputParams.put(15, countryName);
		inputParams.put(16, city);
		inputParams.put(17, device);
		inputParams.put(18, os);
		inputParams.put(19, browser);
		inputParams.put(20, userAgent);
		inputParams.put(21, osVersion);
		inputParams.put(22, browserVersion);
		inputParams.put(23, deviceBrand);
		inputParams.put(24, deviceModel);
		inputParams.put(25, stateRegion);
		inputParams.put(26, domainName);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(27, OracleTypes.NUMBER);
		outputParamsTypes.put(28, OracleTypes.VARCHAR);
		outputParamsTypes.put(29, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(27, AppParams.RESULT_CODE);
		outputParamsNames.put(28, AppParams.RESULT_MSG);
		outputParamsNames.put(29, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_INSERT, inputParams,
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

		Map resultMap = format(resultDataList.get(0), false, false, false);

		LOGGER.fine("=> Order insert result: " + resultMap.toString());

		return resultMap;
	}

	private static void logInsertion(String userId, String currency, String shippingId, String trackingNumber,
			String source, String medium, String campaign, String content, String country, String city, String device,
			String os, String browser) {
		String logHolder = "Order insert with userId= %s, currency= %s, shippingId=%s, trackingNumber=%s, source=%s, medium=%s, campaign=%s, content=%s, country=%s, city=%s, device=%s, os=%s, browser=%s";
		String logStr = String.format(logHolder, userId, currency, shippingId, trackingNumber, source, medium, campaign,
				content, country, city, device, os, browser);
		LOGGER.info(logStr);
	}

	public static Map update(String id, String amount, String currency, String state, String shippingId, String source,
			String medium, String campaign, String content, String country, String city, int totalItem)
			throws SQLException {

		LOGGER.fine("Order update with id=" + id + ", amount=" + amount + ", currency=" + currency + ", state=" + state
				+ ", shippingId=" + shippingId + ", totalItem=" + totalItem);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, amount);
		inputParams.put(3, currency);
		inputParams.put(4, state);
		inputParams.put(5, shippingId);
		inputParams.put(6, source);
		inputParams.put(7, medium);
		inputParams.put(8, campaign);
		inputParams.put(9, content);
		inputParams.put(10, country);
		inputParams.put(11, city);
		inputParams.put(12, totalItem);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);
		outputParamsTypes.put(15, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);
		outputParamsNames.put(15, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_UPDATE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(updateResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = format(resultDataList.get(0), true, false, false);

		LOGGER.fine("=> Order update result: " + ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static Map updateState(String id, String state) throws SQLException {

		LOGGER.fine("Order update state with id=" + id + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_UPDATE_STATE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(updateResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = format(resultDataList.get(0), true, false, false);

		LOGGER.fine("=> Order update result: " + ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	private static Map<String, Object> format(Map queryData, boolean itemList, boolean fulfillmentInfo,
			boolean refundInfo) throws SQLException {

		Map<String, Object> resultMap = new LinkedHashMap<>();

		String id = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, id);
		resultMap.put(AppParams.DOMAIN, ParamUtil.getString(queryData, AppParams.S_DOMAIN));
		resultMap.put(AppParams.AMOUNT, ParamUtil.getString(queryData, AppParams.S_AMOUNT));
		resultMap.put(AppParams.CURRENCY, ParamUtil.getString(queryData, AppParams.S_CURRENCY));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.TRACKING, ParamUtil.getString(queryData, AppParams.S_TRACKING_CODE));
		resultMap.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));

		Map<String, Object> shippingInfoMap = new LinkedHashMap<>();

		shippingInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_SHIPPING_ID));
		shippingInfoMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));
		shippingInfoMap.put(AppParams.EMAIL, ParamUtil.getString(queryData, AppParams.S_EMAIL));
		shippingInfoMap.put(AppParams.PHONE, ParamUtil.getString(queryData, AppParams.S_PHONE));
		shippingInfoMap.put(AppParams.GIFT, ParamUtil.getBoolean(queryData, AppParams.N_GIFT));

		Map<String, Object> addressMap = new LinkedHashMap<>();
		addressMap.put(AppParams.LINE1, ParamUtil.getString(queryData, AppParams.S_ADD_LINE1));
		addressMap.put(AppParams.LINE2, ParamUtil.getString(queryData, AppParams.S_ADD_LINE2));
		addressMap.put(AppParams.CITY, ParamUtil.getString(queryData, AppParams.S_ADD_CITY));
		addressMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_ADD_STATE));
		addressMap.put(AppParams.POSTAL_CODE, ParamUtil.getString(queryData, AppParams.S_POSTAL_CODE));
		addressMap.put(AppParams.COUNTRY, ParamUtil.getString(queryData, AppParams.S_COUNTRY_CODE));
		addressMap.put(AppParams.COUNTRY_NAME, ParamUtil.getString(queryData, AppParams.S_COUNTRY_NAME));

		shippingInfoMap.put(AppParams.ADDRESS, addressMap);

		resultMap.put(AppParams.SHIPPING, shippingInfoMap);

		resultMap.put(AppParams.IP, ParamUtil.getString(queryData, AppParams.S_SOURCE_IP));

		String billingAddressId = ParamUtil.getString(queryData, AppParams.S_BILLING_ADDRESS_ID);

		if (StringUtils.isNotEmpty(billingAddressId)) {
			String billingStatus = ParamUtil.getString(queryData, AppParams.S_BILLING_STATUS);
			if (ResourceStates.APPROVED.equals(billingStatus)) {
				Map<String, Object> billingInfoMap = new LinkedHashMap<>();

				billingInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_BILLING_ADDRESS_ID));
				billingInfoMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_BILLING_NAME));
				billingInfoMap.put(AppParams.EMAIL, ParamUtil.getString(queryData, AppParams.S_BILLING_EMAIL));
				billingInfoMap.put(AppParams.PHONE, ParamUtil.getString(queryData, AppParams.S_BILLING_PHONE));

				Map<String, Object> billingAddress = new LinkedHashMap<>();
				billingAddress.put(AppParams.LINE1, ParamUtil.getString(queryData, AppParams.S_BILLING_ADD_LINE1));
				billingAddress.put(AppParams.LINE2, ParamUtil.getString(queryData, AppParams.S_BILLING_ADD_LINE2));
				billingAddress.put(AppParams.CITY, ParamUtil.getString(queryData, AppParams.S_BILLING_ADD_CITY));
				billingAddress.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_BILLING_ADD_STATE));
				billingAddress.put(AppParams.POSTAL_CODE,
						ParamUtil.getString(queryData, AppParams.S_BILLING_POSTAL_CODE));
				billingAddress.put(AppParams.COUNTRY, ParamUtil.getString(queryData, AppParams.S_BILLING_COUNTRY_CODE));
				billingAddress.put(AppParams.COUNTRY_NAME, ParamUtil.getString(queryData, AppParams.S_COUNTRY_NAME));

				billingInfoMap.put(AppParams.ADDRESS, billingAddress);

				resultMap.put(AppParams.BILLING, billingInfoMap);
			}
		}

		if (itemList) {

			List<Map<String, Object>> orderItemList = ParamUtil.getListData(
					OrderProductService.search(id, fulfillmentInfo, ResourceStates.APPROVED), AppParams.ITEMS);

			resultMap.put(AppParams.ITEMS, orderItemList);

			if (refundInfo) {
				Map refund = OrderProductService.searchRefundedItems(id);
				List<Map> refundList = ParamUtil.getListData(refund, AppParams.DATA);
				if (CollectionUtils.isNotEmpty(refundList)) {
					for (Map rf : refundList) {

						Iterator<Map<String, Object>> itemItt = orderItemList.iterator();
						while (itemItt.hasNext()) {
							Map item = itemItt.next();
							if (matchItem(item, rf)) {
								int rfQuantity = ParamUtil.getInt(rf, AppParams.QUANTITY, 0);
								int itemQty = ParamUtil.getInt(item, AppParams.QUANTITY, 0);
								int remain = itemQty - rfQuantity;
								if (remain == 0) {
									itemItt.remove();
								} else {
									item.put(AppParams.QUANTITY, remain);
									double price = ParamUtil.getDouble(item, AppParams.PRICE);
									double amount = price * remain;
									item.put(AppParams.AMOUNT, String.valueOf(amount));
								}
							}
						}
					}
				}

				resultMap.put(AppParams.REFUND, refund);

			}

		}

		return resultMap;
	}

	private static Map formatOrderOverview(Map queryData) {

		Map<String, Object> resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.DOMAIN, ParamUtil.getString(queryData, AppParams.S_DOMAIN));
		resultMap.put(AppParams.SOURCE, ParamUtil.getString(queryData, AppParams.S_SOURCE));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.ORDER_DATE, ParamUtil.getString(queryData, AppParams.D_ORDER));
		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));
		resultMap.put(AppParams.EMAIL, ParamUtil.getString(queryData, AppParams.S_EMAIL));
		resultMap.put(AppParams.COUNTRY_NAME, ParamUtil.getString(queryData, AppParams.S_COUNTRY_NAME));
		resultMap.put(AppParams.FULFILL_STATE, ParamUtil.getString(queryData, AppParams.S_FULFILLMENT_STATE));
		resultMap.put(AppParams.PROFIT, ParamUtil.getString(queryData, AppParams.N_PROFIT));
		resultMap.put(AppParams.SEQUENCE, ParamUtil.getString(queryData, AppParams.N_SEQ));
		resultMap.put(AppParams.TRACKING, ParamUtil.getString(queryData, AppParams.S_TRACKING));
		resultMap.put(AppParams.PAYOUT_PROCESSED, ParamUtil.getInt(queryData, AppParams.N_PAYOUT_PROCESSED));

		return resultMap;
	}

	private static boolean matchItem(Map item, Map refund) {

		String i_product_id, rf_product_id, i_variant_id, rf_variant_id, i_color, rf_color, i_size_id, rf_size_id;
		i_product_id = ParamUtil.getString(item, AppParams.PRODUCT_ID);
		i_variant_id = ParamUtil.getString(item, AppParams.VARIANT_ID);
		i_color = ParamUtil.getString(item, AppParams.COLOR);
		i_size_id = ParamUtil.getString(item, AppParams.SIZE_ID);

		rf_product_id = ParamUtil.getString(refund, AppParams.PRODUCT_ID);
		rf_variant_id = ParamUtil.getString(refund, AppParams.VARIANT_ID);
		rf_color = ParamUtil.getString(refund, AppParams.COLOR);
		rf_size_id = ParamUtil.getString(refund, AppParams.SIZE_ID);

		boolean match = i_product_id.equals(rf_product_id) && i_variant_id.equals(rf_variant_id)
				&& i_color.equals(rf_color) && i_size_id.equals(rf_size_id);

		return match;
	}

	private static Map trackingFormat(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		String id = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, id);
		resultMap.put(AppParams.AMOUNT, ParamUtil.getString(queryData, AppParams.S_AMOUNT));
		resultMap.put(AppParams.CURRENCY, ParamUtil.getString(queryData, AppParams.S_CURRENCY));
		resultMap.put(AppParams.TAX, ParamUtil.getString(queryData, AppParams.S_TAX_AMOUNT));
		resultMap.put(AppParams.DISCOUNT_AMOUNT, ParamUtil.getString(queryData, AppParams.S_DISCOUNT_AMOUNT));
		resultMap.put(AppParams.REFUND_AMOUNT, ParamUtil.getString(queryData, AppParams.S_REFUND_AMOUNT));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

		Map shippingInfoMap = new LinkedHashMap<>();

		shippingInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_SHIPPING_ID));
		shippingInfoMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));
		shippingInfoMap.put(AppParams.EMAIL, ParamUtil.getString(queryData, AppParams.S_EMAIL));
		shippingInfoMap.put(AppParams.PHONE, ParamUtil.getString(queryData, AppParams.S_PHONE));
		shippingInfoMap.put(AppParams.GIFT, ParamUtil.getBoolean(queryData, AppParams.N_GIFT));

		Map addressMap = new LinkedHashMap<>();
		addressMap.put(AppParams.LINE1, ParamUtil.getString(queryData, AppParams.S_ADD_LINE1));
		addressMap.put(AppParams.LINE2, ParamUtil.getString(queryData, AppParams.S_ADD_LINE2));
		addressMap.put(AppParams.CITY, ParamUtil.getString(queryData, AppParams.S_ADD_CITY));
		addressMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_ADD_STATE));
		addressMap.put(AppParams.POSTAL_CODE, ParamUtil.getString(queryData, AppParams.S_POSTAL_CODE));
		addressMap.put(AppParams.COUNTRY, ParamUtil.getString(queryData, AppParams.S_COUNTRY_CODE));

		shippingInfoMap.put(AppParams.ADDRESS, addressMap);

		resultMap.put(AppParams.SHIPPING, shippingInfoMap);

		Map paymentInfoMap = new LinkedHashMap<>();
		paymentInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_PAYMENT_ID));
		paymentInfoMap.put(AppParams.METHOD, ParamUtil.getString(queryData, AppParams.S_PAYMENT_METHOD));
		paymentInfoMap.put(AppParams.REFERENCE, ParamUtil.getString(queryData, AppParams.S_PAYMENT_REFERENCE));
		paymentInfoMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_PAYMENT_STATE));
		paymentInfoMap.put(AppParams.PAY_TIME, ParamUtil.getString(queryData, AppParams.D_PAYMENT));
		paymentInfoMap.put(AppParams.REFUNDS, new ArrayList<Map>());

		resultMap.put(AppParams.PAYMENT, paymentInfoMap);

		List<Map> orderItemList = ParamUtil.getListData(OrderProductService.tracking(id), AppParams.ITEMS);

		resultMap.put(AppParams.ITEMS, orderItemList);

		return resultMap;
	}

	private static Map formatOrderDetailData(List<Map> orderProducts, Map shippingInfo, List<Map> orderRefund)
			throws SQLException {

		List<Map> orderProductsMap = new ArrayList<>();

		for (Map resultProductMap : orderProducts) {
			Map product = new LinkedHashMap<>();
			product.put(AppParams.ID, ParamUtil.getString(resultProductMap, AppParams.S_PRODUCT_ID));
			product.put(AppParams.PRICE, ParamUtil.getString(resultProductMap, AppParams.S_PRICE));
			product.put(AppParams.TAX, ParamUtil.getString(resultProductMap, AppParams.S_TAX));
			product.put(AppParams.TAX_AMOUNT, ParamUtil.getString(resultProductMap, AppParams.S_TAX_AMOUNT));
			product.put(AppParams.FULFILLMENT, ParamUtil.getString(resultProductMap, AppParams.S_FULFILLMENTS));
			product.put(AppParams.DISCOUNT_AMOUNT, ParamUtil.getString(resultProductMap, AppParams.S_DISCOUNT_AMOUNT));
			product.put(AppParams.BASE_COST, ParamUtil.getString(resultProductMap, AppParams.S_BASE_COST));
			product.put(AppParams.SOURCE, ParamUtil.getString(resultProductMap, AppParams.S_SOURCE));
			product.put(AppParams.CURRENCY, ParamUtil.getString(resultProductMap, AppParams.S_CURRENCY));
			product.put(AppParams.QUANTITY, ParamUtil.getInt(resultProductMap, AppParams.N_QUANTITY));
			product.put(AppParams.STATE, ParamUtil.getString(resultProductMap, AppParams.S_STATE));
			product.put(AppParams.PRODUCT_STATE, ResourceStates.PAID);
			product.put(AppParams.VARIANT_NAME, ParamUtil.getString(resultProductMap, AppParams.S_VARIANT_NAME));
			product.put(AppParams.IMG_URL, ParamUtil.getString(resultProductMap, AppParams.S_FRONT_IMG_URL));
			product.put(AppParams.COLOR, ParamUtil.getString(resultProductMap, AppParams.S_COLOR_VALUE));
			product.put(AppParams.SIZE_NAME, ParamUtil.getString(resultProductMap, AppParams.S_SIZE_NAME));
			product.put(AppParams.COLOR, ParamUtil.getString(resultProductMap, AppParams.S_COLOR_VALUE));
			product.put(AppParams.CAMPAIGN_TITLE, ParamUtil.getString(resultProductMap, AppParams.S_TITLE));
			product.put(AppParams.SHIPPING_FEE, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_FEE));
			product.put(AppParams.IMG_URL, ParamUtil.getString(resultProductMap, AppParams.S_FRONT_IMG_URL));
			product.put(AppParams.PROFIT, ParamUtil.getString(resultProductMap, AppParams.N_PROFIT));
			product.put(AppParams.CUSTOM_DATA, ParamUtil.getString(resultProductMap, AppParams.S_CUSTOM_DATA));

			Map brandMap = new LinkedHashMap<>();
			String heat_press = ParamUtil.getString(resultProductMap, AppParams.N_HEAT_PRESS);
			if(StringUtils.isNotEmpty(heat_press)) {
				Map heatPressMap = new LinkedHashMap<>();
				heatPressMap.put(AppParams.TITLE, "Heat press");
				heatPressMap.put(AppParams.TOTAL, heat_press);
				brandMap.put(AppParams.HEAT_PRESS, heatPressMap);
			}
			
			String brand_box = ParamUtil.getString(resultProductMap, AppParams.N_BRAND_BOX);
			if(StringUtils.isNotEmpty(brand_box)) {
				Map brandBoxMap = new LinkedHashMap<>();
				brandBoxMap.put(AppParams.TITLE, "Box with brand name");
				brandBoxMap.put(AppParams.TOTAL, brand_box);
				brandMap.put(AppParams.BRAND_BOX, brandBoxMap);
			}
			
			String shipping_tag = ParamUtil.getString(resultProductMap, AppParams.N_SHIPPING_TAG);
			if(StringUtils.isNotEmpty(shipping_tag)) {
				Map shippingTagMap = new LinkedHashMap<>();
				shippingTagMap.put(AppParams.TITLE, "Shipping tag");
				shippingTagMap.put(AppParams.TOTAL, shipping_tag);
				brandMap.put(AppParams.SHIPPING_TAG, shippingTagMap);
			
			}
			String thankyou_tag = ParamUtil.getString(resultProductMap, AppParams.N_THANKYOU_TAG);
			if(StringUtils.isNotEmpty(thankyou_tag)) {
				Map thankyouTagMap = new LinkedHashMap<>();
				thankyouTagMap.put(AppParams.TITLE, "Thank you tag");
				thankyouTagMap.put(AppParams.TOTAL, thankyou_tag);
				brandMap.put(AppParams.THANKYOU_TAG, thankyouTagMap);
			}
			
			String giftcard = ParamUtil.getString(resultProductMap, AppParams.N_GIFTCARD);
			if(StringUtils.isNotEmpty(giftcard)) {
				Map giftcardMap = new LinkedHashMap<>();
				giftcardMap.put(AppParams.TITLE, "Gift card");
				giftcardMap.put(AppParams.TOTAL, giftcard);
				brandMap.put(AppParams.GIFTCARD, giftcardMap);
			}
			
			product.put(AppParams.BRAND, brandMap);
			

			Map tracking = new LinkedHashMap<>();
			tracking.put(AppParams.CARRIER, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_CARRIER));
			tracking.put(AppParams.LABEL, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_LABLE_URL));
			tracking.put(AppParams.CODE, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_TRACKING_CODE));
			tracking.put(AppParams.URL, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_TRACKING_URL));
			product.put(AppParams.TRACKING, tracking);
			orderProductsMap.add(product);

		}

		List<Map> orderProductsRefundMap = new ArrayList<>();

		for (Map resultProductMap : orderRefund) {
			Map product = new LinkedHashMap<>();
			product.put(AppParams.ID, ParamUtil.getString(resultProductMap, AppParams.S_PRODUCT_ID));
			product.put(AppParams.PRICE, ParamUtil.getString(resultProductMap, AppParams.S_PRICE));
			product.put(AppParams.TAX, ParamUtil.getString(resultProductMap, AppParams.S_TAX));
			product.put(AppParams.TAX_AMOUNT, ParamUtil.getString(resultProductMap, AppParams.S_TAX_AMOUNT));
			product.put(AppParams.FULFILLMENT, ParamUtil.getString(resultProductMap, AppParams.S_FULFILLMENTS));
			product.put(AppParams.DISCOUNT_AMOUNT, ParamUtil.getString(resultProductMap, AppParams.S_DISCOUNT_AMOUNT));
			product.put(AppParams.BASE_COST, ParamUtil.getString(resultProductMap, AppParams.S_BASE_COST));
			product.put(AppParams.SOURCE, ParamUtil.getString(resultProductMap, AppParams.S_SOURCE));
			product.put(AppParams.CURRENCY, ParamUtil.getString(resultProductMap, AppParams.S_CURRENCY));
			product.put(AppParams.QUANTITY, ParamUtil.getInt(resultProductMap, AppParams.N_QUANTITY));
			product.put(AppParams.STATE, ParamUtil.getString(resultProductMap, AppParams.S_STATE));
			product.put(AppParams.PRODUCT_STATE, ResourceStates.REFUND);
			product.put(AppParams.VARIANT_NAME, ParamUtil.getString(resultProductMap, AppParams.S_VARIANT_NAME));
			product.put(AppParams.IMG_URL, ParamUtil.getString(resultProductMap, AppParams.S_FRONT_IMG_URL));
			product.put(AppParams.COLOR, ParamUtil.getString(resultProductMap, AppParams.S_COLOR_VALUE));
			product.put(AppParams.SIZE_NAME, ParamUtil.getString(resultProductMap, AppParams.S_SIZE_NAME));
			product.put(AppParams.COLOR, ParamUtil.getString(resultProductMap, AppParams.S_COLOR_VALUE));
			product.put(AppParams.CAMPAIGN_TITLE, ParamUtil.getString(resultProductMap, AppParams.S_TITLE));
			product.put(AppParams.SHIPPING_FEE, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_FEE));
			product.put(AppParams.IMG_URL, ParamUtil.getString(resultProductMap, AppParams.S_FRONT_IMG_URL));
			product.put(AppParams.PROFIT, ParamUtil.getString(resultProductMap, AppParams.N_PROFIT));

			Map tracking = new LinkedHashMap<>();
			tracking.put(AppParams.CARRIER, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_CARRIER));
			tracking.put(AppParams.LABEL, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_LABLE_URL));
			tracking.put(AppParams.CODE, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_TRACKING_CODE));
			tracking.put(AppParams.URL, ParamUtil.getString(resultProductMap, AppParams.S_SHIPPING_TRACKING_URL));
			product.put(AppParams.TRACKING, tracking);
			orderProductsRefundMap.add(product);

		}

		Map shippingInfoMap = new LinkedHashMap<>();
		// shippingInfoMap.put(AppParams.ID, ParamUtil.getString(shippingInfo,
		// AppParams.S_SHIPPING_ID));
		shippingInfoMap.put(AppParams.NAME, ParamUtil.getString(shippingInfo, AppParams.S_NAME));
		shippingInfoMap.put(AppParams.EMAIL, ParamUtil.getString(shippingInfo, AppParams.S_EMAIL));
		shippingInfoMap.put(AppParams.PHONE, ParamUtil.getString(shippingInfo, AppParams.S_PHONE));
		shippingInfoMap.put(AppParams.GIFT, ParamUtil.getBoolean(shippingInfo, AppParams.N_GIFT));

		shippingInfoMap.put(AppParams.LINE1, ParamUtil.getString(shippingInfo, AppParams.S_ADD_LINE1));
		shippingInfoMap.put(AppParams.LINE2, ParamUtil.getString(shippingInfo, AppParams.S_ADD_LINE2));
		shippingInfoMap.put(AppParams.CITY, ParamUtil.getString(shippingInfo, AppParams.S_ADD_CITY));
		shippingInfoMap.put(AppParams.STATE, ParamUtil.getString(shippingInfo, AppParams.S_ADD_STATE));
		shippingInfoMap.put(AppParams.POSTAL_CODE, ParamUtil.getString(shippingInfo, AppParams.S_POSTAL_CODE));
		shippingInfoMap.put(AppParams.COUNTRY, ParamUtil.getString(shippingInfo, AppParams.S_COUNTRY_CODE));

		Map billingMap = new LinkedHashMap<>();
		// billingMap.put(AppParams.ID, ParamUtil.getString(shippingInfo,
		// AppParams.S_SHIPPING_ID));
		billingMap.put(AppParams.NAME, ParamUtil.getString(shippingInfo, AppParams.S_BILLING_NAME));
		billingMap.put(AppParams.EMAIL, ParamUtil.getString(shippingInfo, AppParams.S_BILLING_EMAIL));
		billingMap.put(AppParams.PHONE, ParamUtil.getString(shippingInfo, AppParams.S_BILLING_PHONE));
		billingMap.put(AppParams.LINE1, ParamUtil.getString(shippingInfo, AppParams.S_BILLING_ADD_LINE1));
		billingMap.put(AppParams.LINE2, ParamUtil.getString(shippingInfo, AppParams.S_BILLING_ADD_LINE2));
		billingMap.put(AppParams.CITY, ParamUtil.getString(shippingInfo, AppParams.S_BILLING_ADD_CITY));
		billingMap.put(AppParams.STATE, ParamUtil.getString(shippingInfo, AppParams.S_BILLING_ADD_STATE));
		billingMap.put(AppParams.POSTAL_CODE, ParamUtil.getString(shippingInfo, AppParams.S_BILLING_POSTAL_CODE));
		billingMap.put(AppParams.COUNTRY, ParamUtil.getString(shippingInfo, AppParams.S_BILLING_COUNTRY_CODE));

		Map orderDetail = new LinkedHashMap<>();
		orderDetail.put(AppParams.PRODUCTS, orderProductsMap);
		orderDetail.put(AppParams.REFUNDS, orderProductsRefundMap);
		orderDetail.put(AppParams.SHIPPING, shippingInfoMap);
		orderDetail.put(AppParams.BILLING, billingMap);

		return orderDetail;
	}

	public static Map checkIfCreatedLabel(String trackingCode) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, trackingCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.LABEL);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map queryResult = DBProcedureUtil.execute(dataSource, DBProcedurePool.CHECK_IF_CREATED_LABEL, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(queryResult, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(queryResult, AppParams.RESULT_MSG));
		}

		boolean createdLabel = ParamUtil.getBoolean(queryResult, AppParams.LABEL);

		Map result = new HashMap<String, Object>();
		result.put(AppParams.LABEL, createdLabel);

		List<Map> shippingResultList = ParamUtil.getListData(queryResult, AppParams.RESULT_DATA);
		if (shippingResultList != null && shippingResultList.isEmpty() == false) {
			Map shippingResult = shippingResultList.get(0);
			Map shipping = new LinkedHashMap<>();
			shipping.put(AppParams.ID, ParamUtil.getString(shippingResult, AppParams.S_ID));
			shipping.put(AppParams.COUNTRY, ParamUtil.getString(shippingResult, AppParams.S_COUNTRY_CODE));
			shipping.put(AppParams.PHONE, ParamUtil.getString(shippingResult, AppParams.S_PHONE));
			result.put(AppParams.SHIPPING, shipping);
		}

		return result;
	}

	public static void updateShipping(String shippingId, String name, String line1, String line2, String city,
			String state, String zip) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, shippingId);
		inputParams.put(2, name);
		inputParams.put(3, line1);
		inputParams.put(4, line2);
		inputParams.put(5, city);
		inputParams.put(6, state);
		inputParams.put(7, zip);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.VARCHAR);
		outputParamsTypes.put(10, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(8, AppParams.RESULT_CODE);
		outputParamsNames.put(9, AppParams.RESULT_MSG);
		outputParamsNames.put(10, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.UPDATE_SHIPPING, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

	}

	public static boolean isValidTrackingCode(String trackingCode) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, trackingCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.FIND_BY_TRACKING_CODE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return false;
		}

		return true;
	}

	public static void stripeAdjust(String chargeKey, String note) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, chargeKey);
		inputParams.put(2, note);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_ADJUST_BY_STRIPE_CHARGE_KEY,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

	}

	private static final Logger LOGGER = Logger.getLogger(OrderService.class.getName());

}
