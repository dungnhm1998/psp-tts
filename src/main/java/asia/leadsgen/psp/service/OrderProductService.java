package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class OrderProductService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static Map get(String id) throws SQLException {

		LOGGER.fine("Order product lookup with id=" + id);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_PRD_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		Map resultMap = format(resultDataList.get(0), false);

		LOGGER.fine("=> Order look up result: " + resultMap.toString());

		return resultMap;
	}

	public static Map activeFreeship(String itemId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, itemId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_PRD_ACTIVE_FREESHIP, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = format(resultDataList.get(0), false);
		LOGGER.fine("=> activeFreeship result: " + resultMap.toString());
		return resultMap;
	}

	public static Map search(String orderId, boolean fulfillmentInfo, String state) throws SQLException {

		LOGGER.fine("Order item search with orderId=" + orderId + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, fulfillmentInfo);
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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_PRD_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(format(resultDataMap, fulfillmentInfo));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.ITEMS, dataList);

		LOGGER.fine("=> Order items search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map searchRefundedItems(String orderId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		outputParamsNames.put(5, AppParams.RESULT_TOTAL);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.SEARCH_REFUNDED_ITEMS, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();

		for (Map rs : resultDataList) {
			dataList.add(format(rs, false));
		}

		Map result = new LinkedHashMap<String, Object>();
		result.put(AppParams.AMOUNT, ParamUtil.getString(searchResultMap, AppParams.RESULT_TOTAL));
		result.put(AppParams.DATA, dataList);
		
		return result;
	}

	public static Map tracking(String orderId) throws SQLException {

		LOGGER.fine("Order product tracking with orderId=" + orderId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_TOTAL);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_PRD_TRACKING, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(format(resultDataMap, false));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.ITEMS, dataList);

		LOGGER.fine("=> Order product tracking result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static Map insert(String orderId, String campaignId, String productId, String variantId, String variantName,
			String sizeId, double price, String currency, int quatity, String shippingFee, String amount, String prId,
			String prCode, String prType, String discountType, String discountValue, String discountAmount,
			String baseId, String baseCost, String upsellNote, String upsellAmount, String fulfillments, String tax,
			String taxAmount) throws SQLException {

		LOGGER.log(Level.INFO,
				"orderId={0}, campaignId={1}, productId={2}, variantId={3}, variantName={4}, sizeId={5}, price={6}, currency={7}, quatity={8}, shippingFee={9}, amount={10}, prId={11}, prCode={12}, prType={13}, discountType={14}, discountValue={15}, discountAmount={16}, baseId={17}, baseCost={18}, upsellNote={19}, upsellAmount={20}, fulfillments={21}, tax={22}, taxAmount={23}",
				new Object[] { orderId, campaignId, productId, variantId, variantName, sizeId, String.valueOf(price),
						currency, String.valueOf(quatity), shippingFee, amount, prId, prCode, prType, discountType,
						discountValue, discountAmount, baseId, baseCost, upsellNote, upsellAmount, fulfillments, tax,
						taxAmount });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);
		inputParams.put(2, campaignId);
		inputParams.put(3, productId);
		inputParams.put(4, variantId);
		inputParams.put(5, variantName);
		inputParams.put(6, sizeId);
		inputParams.put(7, price);
		inputParams.put(8, currency);
		inputParams.put(9, quatity);
		inputParams.put(10, shippingFee);
		inputParams.put(11, amount);
		inputParams.put(12, prId);
		inputParams.put(13, prCode);
		inputParams.put(14, prType);
		inputParams.put(15, discountType);
		inputParams.put(16, discountValue);
		inputParams.put(17, discountAmount);
		inputParams.put(18, baseId);
		inputParams.put(19, baseCost);
		inputParams.put(20, upsellNote);
		inputParams.put(21, upsellAmount);
		inputParams.put(22, fulfillments);
		inputParams.put(23, tax);
		inputParams.put(24, taxAmount);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(25, OracleTypes.NUMBER);
		outputParamsTypes.put(26, OracleTypes.VARCHAR);
		outputParamsTypes.put(27, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(25, AppParams.RESULT_CODE);
		outputParamsNames.put(26, AppParams.RESULT_MSG);
		outputParamsNames.put(27, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_PRD_INSERT, inputParams,
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

		Map resultMap = format(resultDataList.get(0), false);

		LOGGER.fine("=> Order product insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Map update(String id, String sizeId, int quantity, String shippingFee, String prId, String prCode,
			String prType, String discountType, String discountValue, String discountAmount, String amount,
			String state, String upsellNote, String upsellDiscountStr, String tax, String taxAmount)
			throws SQLException {

		LOGGER.fine("Order product update with id=" + id + ", sizeId=" + sizeId + ", quantity=" + quantity + ", prId="
				+ prId + ", prCode=" + prCode + ", discountType=" + discountType + ", discountAmount=" + discountAmount
				+ ", shippingFee=" + shippingFee + ", amount=" + amount + ", upsellDiscountStr=" + upsellDiscountStr);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, sizeId);
		inputParams.put(3, quantity);
		inputParams.put(4, shippingFee);
		inputParams.put(5, prId);
		inputParams.put(6, prCode);
		inputParams.put(7, prType);
		inputParams.put(8, discountType);
		inputParams.put(9, discountValue);
		inputParams.put(10, discountAmount);
		inputParams.put(11, amount);
		inputParams.put(12, state);
		inputParams.put(13, upsellNote);
		inputParams.put(14, upsellDiscountStr);
		inputParams.put(15, tax);
		inputParams.put(16, taxAmount);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(17, OracleTypes.NUMBER);
		outputParamsTypes.put(18, OracleTypes.VARCHAR);
		outputParamsTypes.put(19, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(17, AppParams.RESULT_CODE);
		outputParamsNames.put(18, AppParams.RESULT_MSG);
		outputParamsNames.put(19, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_PRD_UPDATE, inputParams,
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

		Map resultMap = format(resultDataList.get(0), false);

		LOGGER.fine("=> Order product update result: " + resultMap.toString());

		return resultMap;
	}

	public static void deleteByOrder(String orderId) throws SQLException {

		LOGGER.fine("Delete order products with orderId=: " + orderId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, orderId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ORDER_PRD_DELETE_ALL, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Order products delete result: " + resultCode);
	}

	private static Map format(Map queryData, boolean fulfillmentInfo) {

		Map resultMap = new LinkedHashMap<>();

		String id = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, id);

		Map campaignInfoMap = new LinkedHashMap<>();
		campaignInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));
		campaignInfoMap.put(AppParams.TITLE, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_TITLE));

		String campaignUrl = ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_URL);

//		if (!campaignUrl.startsWith(StringPool.FORWARD_SLASH)) {
//			campaignUrl = StringPool.FORWARD_SLASH + campaignUrl;
//		}

		resultMap.put(AppParams.CAMPAIGN_TITLE, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_TITLE));
		resultMap.put(AppParams.CAMPAIGN_URL, campaignUrl);
		resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));

		campaignInfoMap.put(AppParams.URL, campaignUrl);
		campaignInfoMap.put(AppParams.END_TIME, ParamUtil.getString(queryData, AppParams.D_CAMPAIGN_END));

		resultMap.put(AppParams.CAMPAIGN, campaignInfoMap);

		resultMap.put(AppParams.PRODUCT_ID, ParamUtil.getString(queryData, AppParams.S_PRODUCT_ID));

		if (!ParamUtil.getString(queryData, AppParams.S_PR_ID).isEmpty()
				|| ParamUtil.getDouble(queryData, AppParams.S_DISCOUNT_AMOUNT) > 0) {

			Map promotionInfoMap = new LinkedHashMap();
			promotionInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_PR_ID));
			promotionInfoMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_PR_TYPE));
			promotionInfoMap.put(AppParams.CODE, ParamUtil.getString(queryData, AppParams.S_PR_CODE));

			Map discountInfoMap = new LinkedHashMap();
			discountInfoMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_DISCOUNT_TYPE));
			discountInfoMap.put(AppParams.VALUE, ParamUtil.getString(queryData, AppParams.S_DISCOUNT_VALUE));
			discountInfoMap.put(AppParams.AMOUNT, ParamUtil.getString(queryData, AppParams.S_DISCOUNT_AMOUNT));

			promotionInfoMap.put(AppParams.DISCOUNT, discountInfoMap);

			resultMap.put(AppParams.PROMOTION, promotionInfoMap);

		}

		if (fulfillmentInfo) {
			Map<String, String> tracking = new HashMap<String, String>();
			tracking.put(AppParams.CARRIER, ParamUtil.getString(queryData, AppParams.S_SHIPPING_CARRIER));
			tracking.put(AppParams.CODE, ParamUtil.getString(queryData, AppParams.S_SHIPPING_TRACKING_CODE));
			tracking.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_SHIPPING_TRACKING_URL));
			resultMap.put(AppParams.TRACKING, tracking);
		}

		resultMap.put(AppParams.UPSELL_DISCOUNT_AMOUNT,
				ParamUtil.getString(queryData, AppParams.S_UPSELL_DISCOUNT_AMOUNT));

		resultMap.put(AppParams.VARIANT_ID, ParamUtil.getString(queryData, AppParams.S_VARIANT_ID));
		resultMap.put(AppParams.VARIANT_NAME, ParamUtil.getString(queryData, AppParams.S_VARIANT_NAME));
		resultMap.put(AppParams.COLOR, ParamUtil.getString(queryData, AppParams.S_COLOR_VALUE));
		resultMap.put(AppParams.SIZE_ID, ParamUtil.getString(queryData, AppParams.S_SIZE_ID));

		boolean backView = ParamUtil.getBoolean(queryData, AppParams.N_BACK_VIEW);

		String variantImageUrl = backView ? ParamUtil.getString(queryData, AppParams.S_BACK_IMG_URL)
				: ParamUtil.getString(queryData, AppParams.S_FRONT_IMG_URL);

		resultMap.put(AppParams.VARIANT_IMAGE, variantImageUrl);
		resultMap.put(AppParams.SIZE_NAME, ParamUtil.getString(queryData, AppParams.S_SIZE_NAME));
		resultMap.put(AppParams.PRICE, ParamUtil.getString(queryData, AppParams.S_PRICE));
		resultMap.put(AppParams.CURRENCY, ParamUtil.getString(queryData, AppParams.S_CURRENCY));
		resultMap.put(AppParams.QUANTITY, ParamUtil.getInt(queryData, AppParams.N_QUANTITY));
		resultMap.put(AppParams.SHIPPING_FEE, ParamUtil.getString(queryData, AppParams.S_SHIPPING_FEE));
		resultMap.put(AppParams.AMOUNT, ParamUtil.getString(queryData, AppParams.S_AMOUNT));
		resultMap.put(AppParams.TAX, ParamUtil.getString(queryData, AppParams.S_TAX));
		resultMap.put(AppParams.TAX_AMOUNT, ParamUtil.getString(queryData, AppParams.S_TAX_AMOUNT));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_PRODUCT_STATE));

		if (!ParamUtil.getString(queryData, AppParams.S_TRACKING_ID).isEmpty()) {
			Map trackingInfoMap = new LinkedHashMap<>();
			trackingInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_TRACKING_ID));
			trackingInfoMap.put(AppParams.CODE, ParamUtil.getString(queryData, AppParams.S_TRACKING_CODE));
			trackingInfoMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_TRACKING_URL));
			trackingInfoMap.put(AppParams.INFORMATION, ParamUtil.getString(queryData, AppParams.S_TRACKING_INFO));
			trackingInfoMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_TRACKING_STATE));

			resultMap.put(AppParams.TRACKING, trackingInfoMap);
		}

		resultMap.put(AppParams.FULFILLMENTS, ParamUtil.getString(queryData, AppParams.S_FULFILLMENTS));

		return resultMap;
	}

	private static final Logger LOGGER = Logger.getLogger(OrderProductService.class.getName());
}
