package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections.MapUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class FulfillmentReviewService extends MasterService {
	
	private static final String GET_FULFILLMENT_REVIEW_BY_ID = "{call PKG_FULFILLMENT_REVIEW.get_fulfillment_review_by_id(?,?,?,?)}";
	private static final String UPDATE_DESIGN_PRINT_FULFILLMENT_REVIEW = "{call PKG_FULFILLMENT_REVIEW.update_design_print_fulfillment_review(?,?,?,?,?,?,?)}";
	
	public static Map getFulfillmentReviewById(String id) throws SQLException, ParseException {
		LOGGER.fine(" FulfillmentReviewService getFulfillmentReviewById() id =" + id);
		Object[] args = new Object[] {id};
		Map queryMap = searchOne(GET_FULFILLMENT_REVIEW_BY_ID, args);
		Map result = null;
		if (MapUtils.isNotEmpty(queryMap)) {
			result = formatFulfillmentReview(queryMap);
		}
		return result;
	}
	
	public static Map updateDesignPrint(String s_id, String url_print_front, String url_print_back, String print_type) throws SQLException, ParseException {
		LOGGER.fine("s_id=" + s_id);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, s_id);
		inputParams.put(2, url_print_front);
		inputParams.put(3, url_print_back);
		inputParams.put(4, print_type);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map resultUpdate = DBProcedureUtil.execute(dataSource, UPDATE_DESIGN_PRINT_FULFILLMENT_REVIEW, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultUpdate, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultUpdate, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultUpdate, AppParams.RESULT_DATA);
		Map result = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			result = formatFulfillmentReview(resultDataList.get(0));
		}

		LOGGER.fine("=> result: " + result);
		return result;
	}
	
	private static Map formatFulfillmentReview(Map queryData) throws SQLException, ParseException {
		Map resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));
		resultMap.put(AppParams.PRINT_DETAIL, ParamUtil.getString(queryData, AppParams.S_PRINT_DETAIL));
		resultMap.put(AppParams.DESIGN_FRONT_ID, ParamUtil.getString(queryData, AppParams.S_DESIGN_FRONT_ID));
		resultMap.put(AppParams.URL_DESIGN_FRONT, ParamUtil.getString(queryData, AppParams.S_URL_DESIGN_FRONT));
		resultMap.put(AppParams.DESIGN_BACK_ID, ParamUtil.getString(queryData, AppParams.S_DESIGN_BACK_ID));
		resultMap.put(AppParams.URL_DESIGN_BACK, ParamUtil.getString(queryData, AppParams.S_URL_DESIGN_BACK));
		resultMap.put(AppParams.BASE_ID, ParamUtil.getString(queryData, AppParams.S_BASE_ID));
		resultMap.put(AppParams.URL_PRINT_FRONT, ParamUtil.getString(queryData, AppParams.S_URL_PRINT_FRONT));
		resultMap.put(AppParams.URL_PRINT_BACK, ParamUtil.getString(queryData, AppParams.S_URL_PRINT_BACK));
		resultMap.put(AppParams.COLOR_ID, ParamUtil.getString(queryData, AppParams.S_COLOR_ID));
		resultMap.put(AppParams.COLOR_NAME, ParamUtil.getString(queryData, AppParams.S_COLOR_NAME));
		resultMap.put(AppParams.COLOR_VALUE, ParamUtil.getString(queryData, AppParams.S_COLOR_VALUE));
		resultMap.put(AppParams.SKU, ParamUtil.getString(queryData, AppParams.S_SKU));
		resultMap.put(AppParams.PRINT_TYPE, ParamUtil.getString(queryData, AppParams.S_PRINT_TYPE));
		resultMap.put(AppParams.ADJUST_TYPE, ParamUtil.getString(queryData, AppParams.S_ADJUST_TYPE));
		return resultMap;
	}
	
	private static final Logger LOGGER = Logger.getLogger(FulfillmentReviewService.class.getName());
	
}
