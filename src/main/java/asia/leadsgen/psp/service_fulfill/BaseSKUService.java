package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.BaseSKUObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class BaseSKUService extends MasterService {

	static final String IS_EXIST_THIS_SKU = "{call PKG_FF_BASE_SKU.is_exist_sku(?,?,?,?)}";
	static final String GET_BY_SKU = "{call PKG_FF_BASE_SKU.get_by_sku(?,?,?,?)}";
	static final String GET_BY_SKU_AND_BASE_ID = "{call PKG_FF_BASE_SKU.get_by_sku_and_base_id(?,?,?,?,?)}";

	static final String GET_LIST_BASE_BY_PARTNER_ID = "{call PKG_FF_BASE_SKU.get_list_base_by_partner_id(?,?,?,?)}";
	
	static final String POSTER_SKU_REGEX = "^matte-poster-(?:11x17|16x24|17x11|24x16|24x36|36x24)$";
	static final String POST_SKU_DEFAULT = "matte-poster|one|white";
	private static final String GET_SKU_BY_PARTER_ID_BASE_ID_COLOR_ID = "{call PKG_FF_BASE_SKU.get_sku_by_parter_id_base_id_color_id(?,?,?,?,?,?)}";
	private static final String GET_SKU_BY_BASE_ID_SIZE_ID_COLOR_NAME = "{call PKG_FF_BASE_SKU.get_sku_by_base_id_size_id_color_name(?,?,?,?,?,?)}";
	
	private static final String GET_BASE_SKU_BY_BASE_SIZE_COLOR_PARTNER_ID = "{call PKG_FF_BASE_SKU.get_base_sku_by_base_size_color_partner_id(?,?,?,?,?,?,?)}";

	public static HashMap<String, String> POSTER_BASES = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("matte-poster-24x36", "hUeboM3JbQW9lXRK");
			put("matte-poster-16x24", "wg1fgLbO77j2WKky");
			put("matte-poster-36x24", "i8ja02GzNr57tTwZ");
			put("matte-poster-24x16", "EOCuz3dpn8DqSVPB");
			put("matte-poster-11x17", "NIUJTUQ0BwZ473QF");
			put("matte-poster-17x11", "WWQ7xQ2aYSa0XUEC");
		}

	};

	public static boolean isExistThisSku(String sku) throws SQLException {

		if (sku.matches(POSTER_SKU_REGEX)) {
			return true;
		}

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, sku);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, IS_EXIST_THIS_SKU, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		return ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL, 0) > 0;
	}

	public static BaseSKUObj getBySku(String sku) throws SQLException {

		String findSkuQuery;

		Map inputParams = new LinkedHashMap<Integer, String>();
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();

		int idx = 2;
		if (sku.matches(POSTER_SKU_REGEX)) {
			findSkuQuery = GET_BY_SKU_AND_BASE_ID;
			String baseId = POSTER_BASES.get(sku);
			inputParams.put(1, POST_SKU_DEFAULT);
			inputParams.put(idx++, baseId);
		} else {
			inputParams.put(1, sku);
			findSkuQuery = GET_BY_SKU;
		}

		outputParamsTypes.put(idx, OracleTypes.NUMBER);
		outputParamsNames.put(idx++, AppParams.RESULT_CODE);

		outputParamsTypes.put(idx, OracleTypes.VARCHAR);
		outputParamsNames.put(idx++, AppParams.RESULT_MSG);

		outputParamsTypes.put(idx, OracleTypes.CURSOR);
		outputParamsNames.put(idx++, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, findSkuQuery, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		return CollectionUtils.isNotEmpty(resultDataList) ? BaseSKUObj.fromMap(resultDataList.get(0)) : null;
	}

	public static BaseSKUObj getSkuByParterIdBaseIdColorId(String partnerId, String baseId, String colorId)
			throws SQLException {
		Map result = searchOne(GET_SKU_BY_PARTER_ID_BASE_ID_COLOR_ID, new Object[] { partnerId, baseId, colorId });
		return (result == null) ? null : BaseSKUObj.fromMap(result);

	}

	public static List<Map> getSkuByBaseIdSizeIdColorName(String baseId, String sizeId, String colorName)
			throws SQLException {
		List<Map> result = searchAll(GET_SKU_BY_BASE_ID_SIZE_ID_COLOR_NAME, new Object[] { baseId, sizeId, colorName });
		return result;
	}

	public static List<Map> getListBaseByPartnerId(String partnerId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, partnerId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_LIST_BASE_BY_PARTNER_ID, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		logger.info("result list base by partner id " + partnerId + " :" + resultDataList);
//		Map result = new HashMap<>();
//		if (!resultDataList.isEmpty()) {
//			result = resultDataList.get(0);
//		}
		return resultDataList;
	}
	
	public static Map getBaseSkuByBaseIdAndSizeIdAndColorPartnerId(String baseId, String sizeId, String colorId,String partnerId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, baseId);
		inputParams.put(2, sizeId);
		inputParams.put(3, colorId);
		inputParams.put(4, partnerId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_BASE_SKU_BY_BASE_SIZE_COLOR_PARTNER_ID, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			logger.info("getBaseSkuByBaseIdAndSizeIdAndColorPartnerId Error input : baseId,size,color,partner= " + inputParams);
			logger.severe(
					"getBaseSkuByBaseIdAndSizeIdAndColorPartnerId Error msg " + ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultData = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (CollectionUtils.isEmpty(resultData)) {
			return null;
		}
		
		return resultData.get(0);
	}

}
