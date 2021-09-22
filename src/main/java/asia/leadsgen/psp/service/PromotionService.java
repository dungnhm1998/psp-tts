package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import asia.leadsgen.psp.data.type.PrType;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.VolumeDiscount;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class PromotionService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static Map discountTypeSearch(String state) throws SQLException {

		LOGGER.fine("Promotion discount type search with state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, state);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_DISCOUNT_TYPE_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(typeFormat(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.TYPES, dataList);

		LOGGER.fine("=> Promotion discount type search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map activeFreeshipSearch(String domainName) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainName);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_ACTIVE_FREESHIP_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		resultMap = prFormat(resultDataList.get(0));

		LOGGER.fine("=> activeFreeshipSearch result: " + resultMap.toString());
		return resultMap;
	}

	public static Map prTypeSearch(String state) throws SQLException {

		LOGGER.fine("Promotion type search with state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, state);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_TYPE_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> coupons = new ArrayList<>();
		List<Map> aboveThreshold = new ArrayList<>();
		for (Map resultDataMap : resultDataList) {
			String type = ParamUtil.getString(resultDataMap, AppParams.S_TYPE);
			if ("coupon".equals(type)) {
				coupons.add(typeFormat(resultDataMap));
			} else if ("above_threshold".equals(type)) {
				aboveThreshold.add(typeFormat(resultDataMap));
			}
		}

		Map resultMap = new LinkedHashMap();

		Map types = new LinkedHashMap<>();
		types.put("above_threshold", aboveThreshold);
		types.put("coupon", coupons);

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.TYPES, types);

		LOGGER.fine("=> Promotion type search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map prTypeLookup(String typeId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, typeId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_TYPE_LOOKUP, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = typeFormat(resultDataList.get(0));
		LOGGER.fine("=> prTypeLookup result: " + resultMap.toString());
		return resultMap;
	}

	public static Map activeFreeShipping(String userId, String typeId, double minOrderAmount, String domainId,
			String domainName) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, typeId);
		inputParams.put(3, minOrderAmount);
		inputParams.put(4, domainId);
		inputParams.put(5, domainName);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_ACTIVE_FREE_SHIPPING, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = prFormat(resultDataList.get(0));
		LOGGER.fine("=> activeFreeShipping result: " + resultMap.toString());
		return resultMap;

	}

	public static Map prSearch(String domainName, String userId, String code, String state, int page, int pageSize)
			throws SQLException {

		LOGGER.fine("Promotion search with userId=" + userId + ", code=" + code + ", state=" + state + ", page=" + page
				+ ", pageSize=" + pageSize);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainName);
		inputParams.put(2, userId);
		inputParams.put(3, code);
		inputParams.put(4, state);
		inputParams.put(5, page);
		inputParams.put(6, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.VARCHAR);
		outputParamsTypes.put(9, OracleTypes.NUMBER);
		outputParamsTypes.put(10, OracleTypes.CURSOR);
		outputParamsTypes.put(11, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(7, AppParams.RESULT_CODE);
		outputParamsNames.put(8, AppParams.RESULT_MSG);
		outputParamsNames.put(9, AppParams.RESULT_TOTAL);
		outputParamsNames.put(10, AppParams.RESULT_DATA);
		outputParamsNames.put(11, AppParams.THRESHOLD);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(prFormat(resultDataMap));
		}

		List<Map> thresholdResultList = ParamUtil.getListData(searchResultMap, AppParams.THRESHOLD);

		Map resultMap = new LinkedHashMap();

		Map threshold = new LinkedHashMap<>();
		if (!thresholdResultList.isEmpty()) {
			threshold = prFormat(thresholdResultList.get(0));
		}

		resultMap.put("above_threshold", threshold);

		Map coupons = new LinkedHashMap();
		coupons.put(AppParams.TOTAL, resultTotalRow);
		coupons.put(AppParams.DATA, dataList);
		resultMap.put("coupon", coupons);

		LOGGER.fine("=> Promotion search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map get(String promotionId) throws SQLException {

		LOGGER.fine("Promotion lookup with promotionId=" + promotionId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, promotionId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_PROMOTION);
		}

		LOGGER.fine("=> Promotion lookup result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return prFormat(resultDataList.get(0));
	}

	public static Map prCampaignCheck(String domainName, String campaignId, String prCode) throws SQLException {

		LOGGER.fine("Promotion check up with campaignId=" + campaignId + " and promo code =" + prCode + " and domain ="
				+ domainName);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainName);
		inputParams.put(2, campaignId);
		inputParams.put(3, prCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_CAMPAIGN_CHECK, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.size() > 0) {
			return prFormat(resultDataList.get(0));
		} else {
			return new LinkedHashMap();
		}
	}

	public static Map prStoreCheck(String domainName, String store, String prCode) throws SQLException {

		LOGGER.fine(
				"Promotion check up with store=" + store + " and promo code =" + prCode + " and domain =" + domainName);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainName);
		inputParams.put(2, store);
		inputParams.put(3, prCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_STORE_CHECK, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.size() > 0) {
			return prFormat(resultDataList.get(0));
		} else {
			return new LinkedHashMap();
		}
	}

	public static Map insert(String domainName, String userId, String typeId, String code, String desc,
			String discountId, String discountValue, String expirationDate, String campaignId, String storeId)
			throws SQLException, BadRequestException {

		LOGGER.fine("Promotion insert with userId=" + userId + ", typeId=" + typeId + ", code=" + code + ", desc="
				+ desc + ", discountId=" + discountId + ", discountValue=" + discountValue + ", expirationDate="
				+ expirationDate);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainName);
		inputParams.put(2, userId);
		inputParams.put(3, typeId);
		inputParams.put(4, code);
		inputParams.put(5, desc);
		inputParams.put(6, discountId);
		inputParams.put(7, discountValue);
		inputParams.put(8, expirationDate);
		inputParams.put(9, campaignId);
		inputParams.put(10, storeId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.VARCHAR);
		outputParamsTypes.put(13, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(11, AppParams.RESULT_CODE);
		outputParamsNames.put(12, AppParams.RESULT_MSG);
		outputParamsNames.put(13, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_INSERT, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode == 409) {
			return null;
		}

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		LOGGER.fine("=> Promotion insert result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return prFormat(resultDataList.get(0));
	}

	public static Map update(String id, String typeId, String desc, String discountId, String discountValue,
			String expirationDate, String state) throws SQLException {

		LOGGER.fine("Promotion update with id=" + id + ", typeId=" + typeId + ", desc=" + desc + ", discountId="
				+ discountId + ", discountValue=" + discountValue + ", expirationDate=" + expirationDate + ", state="
				+ state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, typeId);
		inputParams.put(3, desc);
		inputParams.put(4, discountId);
		inputParams.put(5, discountValue);
		inputParams.put(6, expirationDate);
		inputParams.put(7, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.VARCHAR);
		outputParamsTypes.put(10, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(8, AppParams.RESULT_CODE);
		outputParamsNames.put(9, AppParams.RESULT_MSG);
		outputParamsNames.put(10, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_UPDATE, inputParams,
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

		LOGGER.fine("=> Promotion update result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return prFormat(resultDataList.get(0));
	}

	public static void delete(String prId) throws SQLException {

		LOGGER.fine("Promotion delete with prId=" + prId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, prId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_DELETE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Promotion delete result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
	}

	public static Map update_count(String id, String code) throws SQLException {

		LOGGER.fine("Promotion update with id=" + id + ", code=" + code);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, code);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_UPDATE_COUNT, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			return new LinkedHashMap<>();
			// throw new OracleException(ParamUtil.getString(insertResultMap,
			// AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		LOGGER.fine("=> Promotion update result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return prFormat(resultDataList.get(0));
	}

	private static Map prFormat(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));

		Map typeMap = new LinkedHashMap<>();
		typeMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_TYPE_ID));
		String typeName = ParamUtil.getString(queryData, AppParams.S_TYPE_NAME);
		String promotTypeCode = ParamUtil.getString(queryData, AppParams.S_TYPE);
		typeMap.put(AppParams.NAME, typeName);
		typeMap.put(AppParams.TYPE, promotTypeCode);

		resultMap.put(AppParams.TYPE, typeMap);

		if ("above_threshold".equals(promotTypeCode)) {
			resultMap.put(AppParams.THRESHOLD, ParamUtil.getFormatedDouble(queryData, "S_THRESHOLD", 2));
		} else {
			resultMap.put(AppParams.CODE, ParamUtil.getString(queryData, AppParams.S_CODE));
		}

		if (!ParamUtil.getString(queryData, AppParams.S_DISCOUNT_TYPE_ID).isEmpty()) {
			Map discountMap = new LinkedHashMap<>();
			discountMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_DISCOUNT_TYPE_ID));
			discountMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_DISCOUNT_TYPE));
			discountMap.put(AppParams.VALUE, ParamUtil.getString(queryData, AppParams.S_DISCOUNT_VALUE));

			resultMap.put(AppParams.DISCOUNT, discountMap);
		}

		resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));

		if (!ParamUtil.getString(queryData, AppParams.D_EXPIRE).isEmpty()) {
			resultMap.put(AppParams.EXPIRATION, ParamUtil.getString(queryData, AppParams.D_EXPIRE));
		}

		resultMap.put(AppParams.USED, ParamUtil.getInt(queryData, AppParams.N_USED_COUNT));

		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

		return resultMap;
	}

	public static Map updateFreeShipping(String prId, double threshold, int active) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, prId);
		inputParams.put(2, threshold);
		inputParams.put(3, active);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_UPDATE_FREE_SHIPPING, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = prFormat(resultDataList.get(0));
		LOGGER.fine("=> updateFreeShipping result: " + resultMap.toString());
		return resultMap;
	}

	public static List<VolumeDiscount> getVolumeDiscounts(String domainName) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainName);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_GET_VOLUME_DISCOUNT_BY_DOMAIN,
				inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		List<VolumeDiscount> volumeDiscounts = new ArrayList<>();

		if (resultDataList.isEmpty() == false) {
			String id;
			int quantity;
			double value;
			for (Map data : resultDataList) {
				id = ParamUtil.getString(data, AppParams.S_ID);
				quantity = ParamUtil.getInt(data, "N_VOLUME_DISCOUNT");
				value = ParamUtil.getDouble(data, "S_DISCOUNT_VALUE");

				volumeDiscounts.add(new VolumeDiscount(id, quantity, value));
			}
		}

		return volumeDiscounts;
	}

	public static Map<String, Object> toggleVolumeDiscount(String userId, String domainId, String domainName,
			boolean active) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, PrType.VOLUME_DISCOUNT.getValue());
		inputParams.put(3, domainId);
		inputParams.put(4, domainName);
		inputParams.put(5, active);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_TOGGLE_VOLUME_DISCOUNT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put(AppParams.DOMAIN_ID, domainId);
		
		if (resultDataList.isEmpty()) {
			result.put(AppParams.ACTIVE, false);
		} else {
			result.put(AppParams.ACTIVE, true);
			List<Map> prList = (List<Map>) resultDataList.stream().map(o -> volumeDiscountFormat(o))
					.collect(Collectors.toList());
			result.put(AppParams.DATA, prList);
		}

		return result;

	}

	public static void updateVolumeDiscount(String id, int quantity, String discount) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, quantity);
		inputParams.put(3, discount);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_UPDATE_VOLUME_DISCOUNT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}

	public static Map<String, Object> getVolumeDiscounts(String userId, String domainId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domainId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_GET_VOLUME_DISCOUNT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map<String, Object>> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put(AppParams.DOMAIN_ID, domainId);

		if (resultDataList.isEmpty()) {
			result.put(AppParams.ACTIVE, false);
		} else {
			result.put(AppParams.ACTIVE, true);
			List<Map<String, Object>> prList = resultDataList.stream().map(o -> volumeDiscountFormat(o))
					.collect(Collectors.toList());
			result.put(AppParams.DATA, prList);
		}

		return result;
	}

	public static Map prSearchByCampaignId(String userId, String campId, String state, int page, int pageSize)
			throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, campId);
		inputParams.put(3, state);
		inputParams.put(4, page);
		inputParams.put(5, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);
		outputParamsTypes.put(9, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);
		outputParamsNames.put(9, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_SEARCH_BY_CAMPAIGN_ID, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		int total = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);

		List<Map<String, Object>> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<Map> coupons = new ArrayList<>();

		for (Map resultData : resultDataList) {
			coupons.add(prFormat(resultData));
		}

		Map response = new HashMap<>();
		response.put(AppParams.TOTAL, total);
		response.put(AppParams.DATA, coupons);

		return response;

	}

	public static Map prSearchByStoreId(String userId, String storeId, String state, int page, int pageSize)
			throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, storeId);
		inputParams.put(3, state);
		inputParams.put(4, page);
		inputParams.put(5, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);
		outputParamsTypes.put(9, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);
		outputParamsNames.put(9, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PR_SEARCH_BY_STORE_ID, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		int total = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);

		List<Map<String, Object>> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		List<Map> coupons = new ArrayList<>();

		for (Map resultData : resultDataList) {
			coupons.add(prFormat(resultData));
		}

		Map response = new HashMap<>();
		response.put(AppParams.TOTAL, total);
		response.put(AppParams.DATA, coupons);

		return response;
	}

	private static Map<String, Object> volumeDiscountFormat(Map<String, Object> queryData) {

		Map<String, Object> resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.QUANTITY, ParamUtil.getInt(queryData, "N_VOLUME_DISCOUNT"));
		resultMap.put(AppParams.VALUE, ParamUtil.getInt(queryData, "S_DISCOUNT_VALUE"));

		return resultMap;
	}

	private static Map typeFormat(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));
		resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));

		return resultMap;
	}

	private static final Logger LOGGER = Logger.getLogger(PromotionService.class.getName());

}
