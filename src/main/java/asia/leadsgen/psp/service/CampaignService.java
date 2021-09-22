package asia.leadsgen.psp.service;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.BasePhoneCaseUtil;
import asia.leadsgen.psp.util.CampaignUtil;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.email.MailUtil;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.driver.OracleSQLException;

/**
 * Created by hungdx on 4/1/17.
 */
public class CampaignService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private static HttpServiceConfig aspServiceConfig;
	
	public void setIspServiceConfig(HttpServiceConfig aspServiceConfig) {
		this.aspServiceConfig = aspServiceConfig;
	}

	public static final String CAMP_GET_V2 = "{call PKG_CAMPAIGN_NEW.seller_camp_get(?,?,?,?,?,?,?,?,?)}";
	public static final String CAMP_UPDATE_INFO_V2 = "{call PKG_CAMPAIGN_NEW.CAMP_UPDATE_INFO(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	
	private static final String DROPSHIP_ORDER_GET_CAMP_SEARCH = "{call PKG_FF_CAMPAIGN_V2.camp_search(?,?,?,?,?,?,?,?,?,?)}";
	private static final String GET_CAMPAIGN_DROPSHIP = "{call PKG_FF_CAMPAIGN.get_campaigns_dropship(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	/**
	 * 
	 * @param id
	 * @param productBaseInfo
	 * @param productColorInfo
	 * @param productSizeInfo
	 * @param productDesignInfo
	 * @param productVariantInfo
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 */
	public static Map get(String id, boolean productBaseInfo, boolean productColorInfo, boolean productSizeInfo,
			boolean productDesignInfo, boolean productVariantInfo) throws SQLException, ParseException {

		LOGGER.fine("Campaign lookup with id=" + id);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Campaign look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (!resultDataList.isEmpty()) {
			return format(resultDataList.get(0), productBaseInfo, productColorInfo, productSizeInfo, productDesignInfo,
					productVariantInfo);
		} else {
			return Collections.EMPTY_MAP;
		}
	}

	public static Map getUnfinishedCampaign(String userId) throws SQLException, ParseException {

		LOGGER.fine("Get newest unfinished Campaign with userId=" + userId);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.UNFINISHED_CAMP_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Get newest unfinished Campaign result: "
				+ ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (!resultDataList.isEmpty()) {
			return format(resultDataList.get(0), true, false, true, false, true);
		} else {
			return new LinkedHashMap();
		}
	}

	public static Map search(CampaignSearchParams params)
			throws SQLException, UnsupportedEncodingException, ParseException {

		LOGGER.info("Campaign search with " + params.toString());

		String query = params.isIncludeDropship() ? DROPSHIP_ORDER_GET_CAMP_SEARCH
				: DBProcedurePool.CAMP_SEARCH;

		Map searchResultMap = DBProcedureUtil.execute(dataSource, query, params.getInputParamsMap(),
				params.getOutputParamsTypesMap(), params.getOutputParamsNamesMap());

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(formatV2(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.CAMPAIGNS, dataList);

		LOGGER.fine("=> Campaign search result: " + resultMap.toString());

		return resultMap;
	}

	public static Map searchV2(String domain, String title, String categories, String tags, String startTime,
			String endTime, int privateValue, String state, int page, int pageSize, String orderby)
			throws SQLException, UnsupportedEncodingException, ParseException {

		LOGGER.info("Campaign search with domain=" + domain + ", title=" + title + ", categories=" + categories
				+ ", tags=" + tags + ", startTime=" + startTime + ", endTime=" + endTime + ", private=" + privateValue
				+ ", state=" + state + ", page=" + page + ", pageSize=" + pageSize + ", orderby=" + orderby);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domain);
		inputParams.put(2, title);
		inputParams.put(3, categories);
		inputParams.put(4, tags);
		inputParams.put(5, startTime);
		inputParams.put(6, endTime);
		inputParams.put(7, privateValue);
		inputParams.put(8, state);
		inputParams.put(9, page);
		inputParams.put(10, pageSize);
		inputParams.put(11, orderby);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.NUMBER);
		outputParamsTypes.put(15, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_TOTAL);
		outputParamsNames.put(15, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_SEARCH_V2, inputParams,
				outputParamsTypes, outputParamsNames);

//		LOGGER.info("searchResultMap=" + searchResultMap.toString());

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {
			dataList.add(formatCampV2(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.CAMPAIGNS, dataList);

		LOGGER.fine("=> Campaign search result: " + resultMap.toString());

		return resultMap;
	}

	private static Map formatCampV2(Map data) throws ParseException {

		Map el = new LinkedHashMap<>();

		el.put(AppParams.TITLE, ParamUtil.getString(data, AppParams.S_TITLE));
		el.put(AppParams.REMAINING, CampaignUtil.getTimeRemaining());
		el.put(AppParams.PRICE, ParamUtil.getFormatedDouble(data, AppParams.S_SALE_PRICE, 2));
		el.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(data, AppParams.S_DESIGN_FRONT_URL));
		el.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(data, AppParams.S_DESIGN_BACK_URL));
		el.put(AppParams.BACK_VIEW, ParamUtil.getInt(data, AppParams.N_BACK_VIEW));
		el.put(AppParams.URI, ParamUtil.getString(data, AppParams.S_URI));

		return el;
	}

	public static Map search(String userId, String domain, String state, int page, int pageSize)
			throws SQLException, UnsupportedEncodingException, ParseException {

		LOGGER.info("Campaign get new products with userId=" + userId + ", state=" + state + ", page=" + page
				+ ", pageSize=" + pageSize);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domain);
		inputParams.put(3, state);
		inputParams.put(4, page);
		inputParams.put(5, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_TOTAL);
		outputParamsNames.put(9, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_GET_NEW_PRODUCTS, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {
			dataList.add(formatCampV2(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.PRODUCTS, dataList);

		LOGGER.fine("=> Campaign get new products result: " + resultMap.toString());

		return resultMap;
	}

	public static Map insert(String userId, String title, String desc, String categories, String tags, String startTime,
			String endTime, boolean autoRestart, boolean privateCamp, String baseGroups)
			throws SQLException, UnsupportedEncodingException, ParseException {

		LOGGER.fine("Campaign insert with userId=" + userId + ", title=" + title + ", desc=" + desc + ", startTime="
				+ startTime + ", endTime=" + endTime + ", baseGroups=" + baseGroups);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, title);
		inputParams.put(3, desc);
		inputParams.put(4, categories);
		inputParams.put(5, tags);
		inputParams.put(6, startTime);
		inputParams.put(7, endTime);
		inputParams.put(8, autoRestart);
		inputParams.put(9, privateCamp);
		inputParams.put(10, baseGroups);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.VARCHAR);
		outputParamsTypes.put(13, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(11, AppParams.RESULT_CODE);
		outputParamsNames.put(12, AppParams.RESULT_MSG);
		outputParamsNames.put(13, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_INSERT, inputParams,
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

		Map resultMap = format(resultDataList.get(0), true, true, false, true, false);

		LOGGER.fine("=> Campaign insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Map<String, Object> allOverInsert(String userId, String baseGroupId, String version)
			throws SQLException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, baseGroupId);
		inputParams.put(3, version);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_ALLOVER_INSERT, inputParams,
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

		Map resultMap = format(resultDataList.get(0), true, true, false, true, false);

		LOGGER.fine("=> Campaign insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Map update(String id, String userId, String domainId, String domainName, String title, String desc,
			String categories, String tags, String startTime, String endTime, int length, boolean autoRelaunch,
			boolean privateCamp, String fbPixel, String ggPixel, String state, double sale_price, String artIds,
			String seoTitle, String seoDesc, String seoImageCover)
			throws SQLException, UnsupportedEncodingException, ParseException {

		LOGGER.log(Level.FINE,
				"Campaign update with id= {0}, domainId ={1}, domainName = {2}, title = {3}, desc = {4}, categories = {5}, tags = {6},\r\n"
						+ "startTime={7}, endTime={8}, length={9}, autoRelaunch={10}, privateCamp={11}, String fbPixel={12},\r\n"
						+ " ggPixel={13}, String state={14}, sale_price={15}, artIds={16} ",
				new Object[] { id, userId, domainId, domainName, title, desc, categories, tags, startTime, endTime,
						length, autoRelaunch, privateCamp, fbPixel, ggPixel, state, sale_price, artIds });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, userId);
		inputParams.put(3, domainId);
		inputParams.put(4, domainName);
		inputParams.put(5, title);
		inputParams.put(6, desc);
		inputParams.put(7, categories);
		inputParams.put(8, tags);
		inputParams.put(9, startTime);
		inputParams.put(10, endTime);
		inputParams.put(11, length);
		inputParams.put(12, autoRelaunch);
		inputParams.put(13, privateCamp);
		inputParams.put(14, fbPixel);
		inputParams.put(15, ggPixel);
		inputParams.put(16, state);
		inputParams.put(17, sale_price);
		inputParams.put(18, artIds);
		inputParams.put(19, seoTitle);
		inputParams.put(20, seoDesc);
		inputParams.put(21, seoImageCover);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(22, OracleTypes.NUMBER);
		outputParamsTypes.put(23, OracleTypes.VARCHAR);
		outputParamsTypes.put(24, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(22, AppParams.RESULT_CODE);
		outputParamsNames.put(23, AppParams.RESULT_MSG);
		outputParamsNames.put(24, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_UPDATE, inputParams,
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

		Map resultMap = format(resultDataList.get(0), true, true, false, true, true);

		LOGGER.fine("=> Campaign update result: " + resultMap.toString());

		return resultMap;
	}

	public static Map<String, Object> updateInfo(String id, String domainId, String domainName, String title,
			String desc, String categories, String tags, boolean privateCamp, String fbPixel, String ggPixel,
			String seoTitle, String seoDesc, String seoImageCover)
			throws SQLException, UnsupportedEncodingException, ParseException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, domainId);
		inputParams.put(3, domainName);
		inputParams.put(4, title);
		inputParams.put(5, desc);
		inputParams.put(6, categories);
		inputParams.put(7, tags);
		inputParams.put(8, (privateCamp==true?1:0));
		inputParams.put(9, fbPixel);
		inputParams.put(10, ggPixel);
		inputParams.put(11, seoTitle);
		inputParams.put(12, seoDesc);
		inputParams.put(13, seoImageCover);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(14, OracleTypes.NUMBER);
		outputParamsTypes.put(15, OracleTypes.VARCHAR);
		outputParamsTypes.put(16, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(14, AppParams.RESULT_CODE);
		outputParamsNames.put(15, AppParams.RESULT_MSG);
		outputParamsNames.put(16, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, CAMP_UPDATE_INFO_V2, inputParams,
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

		Map resultMap = format(resultDataList.get(0), true, true, false, true, true);

		LOGGER.fine("=> Campaign update result: " + resultMap.toString());

		return resultMap;
	}

	public static Map updateState(String id, String state)
			throws SQLException, UnsupportedEncodingException, ParseException {

		LOGGER.fine("Campaign update with id=" + id + ", state=" + state);

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

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_UPDATE_STATE, inputParams,
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

		Map resultMap = format(resultDataList.get(0), true, true, false, true, false);

		LOGGER.fine("=> Campaign update result: " + resultMap.toString());

		return resultMap;
	}

	public static Map analysis(String userId, String domainName, String title, String categories, String tags,
			String startDate, String endDate, int privateValue, String state, int page, int pageSize, String orderby,
			String sortDirection, String filterColumn, boolean isOwner, boolean viewProfit, boolean viewStats,
			boolean addModifyCamp) throws SQLException, UnsupportedEncodingException, ParseException {

//		LOGGER.info("Campaign analysis with userId=" + userId + ", title=" + title + ", categories=" + categories
//				+ ", tags=" + tags + ", startTime=" + startDate + ", endTime=" + endDate + ", private="
//				+ privateValue + ", state=" + state + ", page=" + page + ", pageSize=" + pageSize + ", sort="
//				+ orderby + " filterColumn=" + filterColumn);

		Map resultMap = new LinkedHashMap();

		if (isOwner || viewProfit || viewStats || addModifyCamp) {
			Map analysisData = getAnalysisDetailsData(userId, domainName, title, categories, tags, startDate, endDate,
					privateValue, state, page, pageSize, orderby, sortDirection, filterColumn, isOwner);

			int resultCode = ParamUtil.getInt(analysisData, AppParams.RESULT_CODE);

			if (resultCode != HttpResponseStatus.OK.code()) {
				return new LinkedHashMap<>();
			}

			int resultTotalRow = ParamUtil.getInt(analysisData, AppParams.RESULT_TOTAL);

			List<Map> resultDataList = ParamUtil.getListData(analysisData, AppParams.RESULT_DATA);

			List<Map> dataList = new ArrayList<>();

			for (Map resultDataMap : resultDataList) {
				dataList.add(formatCampaignAnalysis(resultDataMap, isOwner, viewProfit, viewStats, addModifyCamp));
			}

			Map sumTotalData = getSumTotalData(userId, domainName, title, categories, tags, startDate, endDate,
					privateValue, state, filterColumn, isOwner, viewProfit, viewStats, addModifyCamp);

			resultMap.put(AppParams.TOTAL, resultTotalRow);
			resultMap.put(AppParams.SUM_TOTAL, sumTotalData);
			resultMap.put(AppParams.CAMPAIGNS, dataList);

		} else {
			Map sumTotalData = new LinkedHashMap<>();
			sumTotalData.put(AppParams.CAMPAIGN_TOTAL_ORDER, AppConstants.NOT_APPLICABLE);
			sumTotalData.put(AppParams.CAMPAIGN_UNIT_SALES, AppConstants.NOT_APPLICABLE);
			sumTotalData.put(AppParams.CAMPAIGN_N_VISITS, AppConstants.NOT_APPLICABLE);
			sumTotalData.put(AppParams.CONV_RATE, AppConstants.NOT_APPLICABLE);
			sumTotalData.put(AppParams.CAMPAIGN_REVENUE, AppConstants.NOT_APPLICABLE);

			resultMap.put(AppParams.TOTAL, 0);
			resultMap.put(AppParams.SUM_TOTAL, sumTotalData);
			resultMap.put(AppParams.CAMPAIGNS, Collections.EMPTY_LIST);

		}

//		LOGGER.fine("=> Campaign analysis result: " + resultMap.toString());

		return resultMap;
	}

	private static Map getSumTotalData(String userId, String domainName, String title, String categories, String tags,
			String startDate, String endDate, int privateValue, String state, String filterColumn, boolean isOwner,
			boolean viewProfit, boolean viewStats, boolean addModifyCamp) throws SQLException {

//		Map sumTotalQueryData = querySumTotalData(userId, domainName, title, categories, tags, startDate, endDate,
//				privateValue, state, filterColumn);

		Map sumTotalData = new LinkedHashMap();
		if (isOwner || viewStats || viewProfit) {

			Map inputParams = new LinkedHashMap<Integer, String>();

			inputParams.put(1, userId);
			inputParams.put(2, domainName);
			inputParams.put(3, title);
			inputParams.put(4, categories);
			inputParams.put(5, tags);
			inputParams.put(6, startDate);
			inputParams.put(7, endDate);
			inputParams.put(8, privateValue);
			inputParams.put(9, state);
			inputParams.put(10, filterColumn);
			inputParams.put(11, isOwner ? 1 : 0);

			Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
			outputParamsTypes.put(12, OracleTypes.NUMBER);
			outputParamsTypes.put(13, OracleTypes.VARCHAR);
			outputParamsTypes.put(14, OracleTypes.CURSOR);

			Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
			outputParamsNames.put(12, AppParams.RESULT_CODE);
			outputParamsNames.put(13, AppParams.RESULT_MSG);
			outputParamsNames.put(14, AppParams.RESULT_DATA);

			Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.AFF_CAMP_ANALYSIS_TOTAL,
					inputParams, outputParamsTypes, outputParamsNames);

			int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

			if (resultCode != HttpResponseStatus.OK.code()) {
				return new LinkedHashMap<>();
			}

			List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

			if (resultDataList != null && resultDataList.size() > 0) {
				Map sumTotalQueryData = resultDataList.get(0);
				sumTotalData.put(AppParams.CAMPAIGN_TOTAL_ORDER,
						ParamUtil.getString(sumTotalQueryData, AppParams.N_TOTAL_ORDERS));
				sumTotalData.put(AppParams.CAMPAIGN_UNIT_SALES,
						ParamUtil.getString(sumTotalQueryData, AppParams.N_UNIT_SALES));
				sumTotalData.put(AppParams.CAMPAIGN_N_VISITS,
						ParamUtil.getString(sumTotalQueryData, AppParams.N_VISITS));
				sumTotalData.put(AppParams.CONV_RATE, ParamUtil.getString(sumTotalQueryData, AppParams.N_CONV_RATE));
				if (isOwner || viewProfit) {
					sumTotalData.put(AppParams.CAMPAIGN_REVENUE,
							ParamUtil.getString(sumTotalQueryData, AppParams.N_REVENUE));
				} else {
					sumTotalData.put(AppParams.CAMPAIGN_REVENUE, AppConstants.NOT_APPLICABLE);
				}
			}
		} else {
			sumTotalData.put(AppParams.CAMPAIGN_TOTAL_ORDER, AppConstants.NOT_APPLICABLE);
			sumTotalData.put(AppParams.CAMPAIGN_UNIT_SALES, AppConstants.NOT_APPLICABLE);
			sumTotalData.put(AppParams.CAMPAIGN_N_VISITS, AppConstants.NOT_APPLICABLE);
			sumTotalData.put(AppParams.CONV_RATE, AppConstants.NOT_APPLICABLE);
			sumTotalData.put(AppParams.CAMPAIGN_REVENUE, AppConstants.NOT_APPLICABLE);
		}

		return sumTotalData;

	}

	private static Map getAnalysisDetailsData(String userId, String domainName, String title, String categories,
			String tags, String startDate, String endDate, int privateValue, String state, int page, int pageSize,
			String orderby, String sortDirection, String filterColumn, boolean isOwner) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();

		inputParams.put(1, userId);
		inputParams.put(2, domainName);
		inputParams.put(3, title);
		inputParams.put(4, categories);
		inputParams.put(5, tags);
		inputParams.put(6, startDate);
		inputParams.put(7, endDate);
		inputParams.put(8, privateValue);
		inputParams.put(9, state);
		inputParams.put(10, page);
		inputParams.put(11, pageSize);
		inputParams.put(12, extractSortColumn(orderby));
		inputParams.put(13, sortDirection);
		inputParams.put(14, filterColumn);
		inputParams.put(15, isOwner);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(16, OracleTypes.NUMBER);
		outputParamsTypes.put(17, OracleTypes.VARCHAR);
		outputParamsTypes.put(18, OracleTypes.NUMBER);
		outputParamsTypes.put(19, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(16, AppParams.RESULT_CODE);
		outputParamsNames.put(17, AppParams.RESULT_MSG);
		outputParamsNames.put(18, AppParams.RESULT_TOTAL);
		outputParamsNames.put(19, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.AFF_CAMP_ANALYSIS, inputParams,
				outputParamsTypes, outputParamsNames);
		return searchResultMap;
	}

	private static Map formatCampaignAnalysis(Map resultDataMap, boolean isOwner, boolean viewProfit, boolean viewStats,
			boolean addModifyCamp) {

//		String id = ;

		Map dataMap = new LinkedHashMap<>();

		dataMap.put(AppParams.ID, ParamUtil.getString(resultDataMap, AppParams.S_ID));
		dataMap.put(AppParams.URL, ParamUtil.getString(resultDataMap, AppParams.S_URI));
		dataMap.put(AppParams.DOMAIN_ID, ParamUtil.getString(resultDataMap, AppParams.S_DOMAIN_ID));
		dataMap.put(AppParams.DOMAIN, ParamUtil.getString(resultDataMap, AppParams.S_DOMAIN));
		dataMap.put(AppParams.TITLE, ParamUtil.getString(resultDataMap, AppParams.S_TITLE));
		dataMap.put(AppParams.STATE, ParamUtil.getString(resultDataMap, AppParams.S_STATE));
		dataMap.put(AppParams.PRIVATE, ParamUtil.getBoolean(resultDataMap, AppParams.N_PRIVATE));
		dataMap.put(AppParams.BACK_VIEW, ParamUtil.getBoolean(resultDataMap, AppParams.N_BACK_VIEW));
		dataMap.put(AppParams.CAMPAIGN_FRONT_IMAGE, ParamUtil.getString(resultDataMap, AppParams.S_FRONT_IMG_URL));
		dataMap.put(AppParams.CAMPAIGN_BACK_IMAGE, ParamUtil.getString(resultDataMap, AppParams.S_BACK_IMG_URL));
		dataMap.put(AppParams.FAVORITE, ParamUtil.getInt(resultDataMap, AppParams.N_FAVORITE));
		dataMap.put(AppParams.ARCHIVE, ParamUtil.getInt(resultDataMap, AppParams.N_ARCHIVE));
		dataMap.put(AppParams.BASE_GROUP_ID, ParamUtil.getString(resultDataMap, AppParams.S_BASE_GROUP_ID));
		dataMap.put(AppParams.CREATE_DATE, ParamUtil.getString(resultDataMap, AppParams.D_CREATE));
		dataMap.put(AppParams.VERSION, ParamUtil.getString(resultDataMap, AppParams.S_DESIGN_VERSION));

		String revenue = AppConstants.NOT_APPLICABLE;
		String orders = AppConstants.NOT_APPLICABLE;
		String percent = AppConstants.NOT_APPLICABLE;
		String sales = AppConstants.NOT_APPLICABLE;
		String visits = AppConstants.NOT_APPLICABLE;
		String rate = AppConstants.NOT_APPLICABLE;
		if (!(addModifyCamp && !(isOwner || viewProfit || viewStats))) {
			if (isOwner || viewProfit) {
				revenue = ParamUtil.getString(resultDataMap, AppParams.N_REVENUE);
			}
			orders = ParamUtil.getString(resultDataMap, AppParams.N_TOTAL_ORDERS);
			percent = ParamUtil.getString(resultDataMap, AppParams.N_PERCENTAGE);
			sales = ParamUtil.getString(resultDataMap, AppParams.N_UNIT_SALES);
			visits = ParamUtil.getString(resultDataMap, AppParams.N_VISITS);
			rate = ParamUtil.getString(resultDataMap, AppParams.N_CONV_RATE);
		}
		dataMap.put(AppParams.CAMPAIGN_REVENUE, revenue);
		dataMap.put(AppParams.CAMPAIGN_TOTAL_ORDER, orders);
		dataMap.put(AppParams.PERCENT_CHANGE, percent);
		dataMap.put(AppParams.CAMPAIGN_UNIT_SALES, sales);
		dataMap.put(AppParams.CAMPAIGN_N_VISITS, visits);
		dataMap.put(AppParams.CONV_RATE, rate);

		return dataMap;
	}

	public static Map duplicate(String userId, String campaignSourceId)
			throws SQLException, UnsupportedEncodingException, ParseException {

		LOGGER.info("Duplicating canpaign : id = " + campaignSourceId);

		Map inputParams = new LinkedHashMap<Integer, String>();

		inputParams.put(1, userId);
		inputParams.put(2, campaignSourceId);
		inputParams.put(3, AppParams.CAMP_COPY_SUFFIX);
		inputParams.put(4, AppConstants.DEFAULT_COPY_CAMPAIGN_TITLE_MAX_LENGTHS);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_DUPLICATE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		Map resultMap = new LinkedHashMap();
		if (resultDataList != null && resultDataList.size() > 0) {

			String campaignId = ParamUtil.getString(resultDataList.get(0), AppParams.S_ID);
			resultMap.put(AppParams.ID, campaignId);

			LOGGER.fine("=>Done duplicating || A new campaign was created with id= " + campaignId);
		}

		return resultMap;
	}

	private static String extractSortColumn(String orderby) {
		if (StringUtils.isNotEmpty(orderby)) {

			switch (orderby) {

			case AppParams.TITLE:
				orderby = AppParams.S_TITLE;
				break;

			case AppParams.CAMPAIGN_TOTAL_ORDER:
				orderby = AppParams.N_TOTAL_ORDERS;
				break;

			case AppParams.CAMPAIGN_UNIT_SALES:
				orderby = AppParams.N_UNIT_SALES;
				break;

			case AppParams.CAMPAIGN_N_VISITS:
				orderby = AppParams.N_VISITS;
				break;
			case AppParams.CONV_RATE:
				orderby = AppParams.N_CONV_RATE;
				break;

			case AppParams.CAMPAIGN_REVENUE:
				orderby = AppParams.N_REVENUE;
				break;

			case AppParams.CREATE_DATE:
				orderby = AppParams.D_CREATE;

			default:
				break;
			}

		}
		return orderby;
	}

	public static List<String> getCampaignProductIdList(String campaignId) throws SQLException {

		LOGGER.fine("Campaign approved product query with campaignId= " + campaignId);

		List<String> resultList = new ArrayList<>();

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map campApprovedProductQueryResultMap = DBProcedureUtil.execute(dataSource,
				DBProcedurePool.CAMP_GET_PRODUCT_IDS, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(campApprovedProductQueryResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(campApprovedProductQueryResultMap, AppParams.RESULT_MSG));
		}

		List<Map> queryDataList = ParamUtil.getListData(campApprovedProductQueryResultMap, AppParams.RESULT_DATA);

		for (Map queryData : queryDataList) {
			resultList.add(ParamUtil.getString(queryData, AppParams.S_ID));
		}

		LOGGER.fine("=> Campaign approved product query result: " + resultList.size());

		return resultList;
	}

	private static Map format(Map queryData, boolean productBaseInfo, boolean productColorInfo, boolean productSizeInfo,
			boolean productDesignInfo, boolean productVariantInfo) throws SQLException, ParseException {

		Map resultMap = new LinkedHashMap<>();

		String campaignId = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));
		resultMap.put(AppParams.TITLE, ParamUtil.getString(queryData, AppParams.S_TITLE));
//		resultMap.put(AppParams.DESC, StringUtil.urlEncode(ParamUtil.getString(queryData, AppParams.S_DESC)));
		resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		resultMap.put(AppParams.DOMAIN_ID, ParamUtil.getString(queryData, AppParams.S_DOMAIN_ID));
		resultMap.put(AppParams.DOMAIN, ParamUtil.getString(queryData, AppParams.S_DOMAIN));
		resultMap.put(AppParams.BASE_GROUP_ID, ParamUtil.getString(queryData, AppParams.S_BASE_GROUP_ID));

		resultMap.put(AppParams.SEO_TITLE, ParamUtil.getString(queryData, AppParams.S_SEO_TITLE));
		resultMap.put(AppParams.SEO_DESC, ParamUtil.getString(queryData, AppParams.S_SEO_DESC));
		resultMap.put(AppParams.SEO_IMAGE_COVER, ParamUtil.getString(queryData, AppParams.S_SEO_IMAGE_COVER));

		resultMap.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_FRONT_URL));
		resultMap.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_BACK_URL));
		resultMap.put(AppParams.BACK_VIEW, ParamUtil.getBoolean(queryData, AppParams.N_BACK_VIEW));

		String campaignUri = ParamUtil.getString(queryData, AppParams.S_URI);

//		if (campaignUri != null && !campaignUri.isEmpty() && !campaignUri.startsWith(StringPool.FORWARD_SLASH)) {
//			campaignUri = StringPool.FORWARD_SLASH + campaignUri;
//		}

		resultMap.put(AppParams.URL, campaignUri);
		resultMap.put(AppParams.TAGS, ParamUtil.getString(queryData, AppParams.S_TAGS));
		resultMap.put(AppParams.CATEGORIES, ParamUtil.getString(queryData, AppParams.S_CATEGORY_IDS));
		resultMap.put(AppParams.STORES, ParamUtil.getString(queryData, AppParams.S_STORES));
		resultMap.put(AppParams.PRIVATE, ParamUtil.getBoolean(queryData, AppParams.N_PRIVATE));

		if (!ParamUtil.getString(queryData, AppParams.D_START).isEmpty()) {
			resultMap.put(AppParams.START_TIME, ParamUtil.getString(queryData, AppParams.D_START));
		}

		if (!ParamUtil.getString(queryData, AppParams.D_START).isEmpty()) {
			resultMap.put(AppParams.END_TIME, ParamUtil.getString(queryData, AppParams.D_END));
		}

		if (ResourceStates.LAUNCHING.equals(ParamUtil.getString(queryData, AppParams.S_STATE))) {
			resultMap.put(AppParams.REMAINING, CampaignUtil.getTimeRemaining());
		}

		resultMap.put(AppParams.RELAUNCH, ParamUtil.getBoolean(queryData, AppParams.N_AUTO_RELAUNCH));
		resultMap.put(AppParams.FB_PIXEL, ParamUtil.getString(queryData, AppParams.S_FB_PIXEL));
		resultMap.put(AppParams.GG_PIXEL, ParamUtil.getString(queryData, AppParams.S_GG_PIXEL));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.VERSION, ParamUtil.getString(queryData, AppParams.S_DESIGN_VERSION));

		Map designsInfo = productDesignInfo == true ? CampaignUtil.getMapProductDesigns(campaignId) : null;
		Map variantsInfo = productVariantInfo == true ? CampaignUtil.getMapProductVariants(campaignId) : null;
		Map mockupsInfo = productVariantInfo == true ? CampaignUtil.getCampaignMockups(campaignId) : null;
		Map productPriceInfoMap = ProductPriceService.getPricesMapByCampaignId(campaignId);

		List<Map> campaignProductList = ParamUtil.getListData(ProductService.search(campaignId, productBaseInfo,
				productColorInfo, designsInfo, variantsInfo, mockupsInfo, productPriceInfoMap), AppParams.PRODUCTS);

		resultMap.put(AppParams.PRODUCTS, campaignProductList);

		return resultMap;
	}

	private static Map formatV2(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		String campaignId = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));
		resultMap.put(AppParams.TITLE, ParamUtil.getString(queryData, AppParams.S_TITLE));
//		resultMap.put(AppParams.DESC, StringUtil.urlEncode(ParamUtil.getString(queryData, AppParams.S_DESC)));
		resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		resultMap.put(AppParams.DOMAIN_ID, ParamUtil.getString(queryData, AppParams.S_DOMAIN_ID));
		resultMap.put(AppParams.DOMAIN, ParamUtil.getString(queryData, AppParams.S_DOMAIN));
		resultMap.put(AppParams.BASE_GROUP_ID, ParamUtil.getString(queryData, AppParams.S_BASE_GROUP_ID));

		resultMap.put(AppParams.SEO_TITLE, ParamUtil.getString(queryData, AppParams.S_SEO_TITLE));
		resultMap.put(AppParams.SEO_DESC, ParamUtil.getString(queryData, AppParams.S_SEO_DESC));
		resultMap.put(AppParams.SEO_IMAGE_COVER, ParamUtil.getString(queryData, AppParams.S_SEO_IMAGE_COVER));

		resultMap.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_FRONT_URL));
		resultMap.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_BACK_URL));
		resultMap.put(AppParams.BACK_VIEW, ParamUtil.getBoolean(queryData, AppParams.N_BACK_VIEW));

		String campaignUri = ParamUtil.getString(queryData, AppParams.S_URI);

//		if (campaignUri != null && !campaignUri.isEmpty() && !campaignUri.startsWith(StringPool.FORWARD_SLASH)) {
//			campaignUri = StringPool.FORWARD_SLASH + campaignUri;
//		}

		resultMap.put(AppParams.URL, campaignUri);
		resultMap.put(AppParams.TAGS, ParamUtil.getString(queryData, AppParams.S_TAGS));
		resultMap.put(AppParams.CATEGORIES, ParamUtil.getString(queryData, AppParams.S_CATEGORY_IDS));
		resultMap.put(AppParams.STORES, ParamUtil.getString(queryData, AppParams.S_STORES));
		resultMap.put(AppParams.PRIVATE, ParamUtil.getBoolean(queryData, AppParams.N_PRIVATE));

		if (!ParamUtil.getString(queryData, AppParams.D_START).isEmpty()) {
			resultMap.put(AppParams.START_TIME, ParamUtil.getString(queryData, AppParams.D_START));
		}

		if (!ParamUtil.getString(queryData, AppParams.D_START).isEmpty()) {
			resultMap.put(AppParams.END_TIME, ParamUtil.getString(queryData, AppParams.D_END));
		}

		if (ResourceStates.LAUNCHING.equals(ParamUtil.getString(queryData, AppParams.S_STATE))) {
			resultMap.put(AppParams.REMAINING, CampaignUtil.getTimeRemaining());
		}

		resultMap.put(AppParams.RELAUNCH, ParamUtil.getBoolean(queryData, AppParams.N_AUTO_RELAUNCH));
		resultMap.put(AppParams.FB_PIXEL, ParamUtil.getString(queryData, AppParams.S_FB_PIXEL));
		resultMap.put(AppParams.GG_PIXEL, ParamUtil.getString(queryData, AppParams.S_GG_PIXEL));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.VERSION, ParamUtil.getString(queryData, AppParams.S_DESIGN_VERSION));

//		Map designsInfo = productDesignInfo == true ? CampaignUtil.getMapProductDesigns(campaignId) : null;
//		Map variantsInfo = productVariantInfo == true ? CampaignUtil.getMapProductVariants(campaignId) : null;
//		Map mockupsInfo = productVariantInfo == true ? CampaignUtil.getCampaignMockups(campaignId) : null;
//		Map productPriceInfoMap = ProductPriceService.getPricesMapByCampaignId(campaignId);
//
//		List<Map> campaignProductList = ParamUtil.getListData(ProductService.search(campaignId, productBaseInfo,
//				productColorInfo, designsInfo, variantsInfo, mockupsInfo, productPriceInfoMap), AppParams.PRODUCTS);
//
//		resultMap.put(AppParams.PRODUCTS, campaignProductList);

		return resultMap;
	}

	public static Map updateDefaultImages(String campaignId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_UPDATE_DEFAULT_IMAGES, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
//		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
//		if (resultDataList.isEmpty()) {
//			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
//		}
//		resultMap = format(resultDataList.get(0));
		LOGGER.fine("=> updateDefaultImages result: " + resultMap.toString());
		return resultMap;
	}

//	public static void updateCampaignDefaultImages(String campaignId, String frontImageUrl, String backImageUrl)
//			throws SQLException {
//		LOGGER.info(
//				"Campaign " + campaignId + " : frontImageUrl = " + frontImageUrl + ", backImageUrl = " + backImageUrl);
//		Map inputParams = new LinkedHashMap<Integer, String>();
//		inputParams.put(1, campaignId);
//		inputParams.put(2, frontImageUrl);
//		inputParams.put(3, backImageUrl);
//
//		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
//		outputParamsTypes.put(4, OracleTypes.NUMBER);
//		outputParamsTypes.put(5, OracleTypes.VARCHAR);
//
//		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
//		outputParamsNames.put(4, AppParams.RESULT_CODE);
//		outputParamsNames.put(5, AppParams.RESULT_MSG);
//
//		Map campaignResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_UPDATE_DEFAULT_IMAGES,
//				inputParams, outputParamsTypes, outputParamsNames);
//
//		int resultCode = ParamUtil.getInt(campaignResultMap, AppParams.RESULT_CODE);
//
//		if (resultCode != HttpResponseStatus.OK.code()) {
//			throw new OracleException(ParamUtil.getString(campaignResultMap, AppParams.RESULT_MSG));
//		}
//
//	}

	public static Map insert(String campaignUrl, String firsName, String lastName, String addLine1, String addLine2,
			String state, String country, String email, String phone, String ownerType, String ownerIp, String details,
			String urlOriginal, String additional, String violation, String type, String domain)
			throws SQLException, UnsupportedEncodingException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignUrl);
		inputParams.put(2, firsName);
		inputParams.put(3, lastName);
		inputParams.put(4, addLine1);
		inputParams.put(5, addLine2);
		inputParams.put(6, state);
		inputParams.put(7, country);
		inputParams.put(8, email);
		inputParams.put(9, phone);
		inputParams.put(10, ownerType);
		inputParams.put(11, ownerIp);
		inputParams.put(12, details);
		inputParams.put(13, urlOriginal);
		inputParams.put(14, additional);
		inputParams.put(15, violation);
		inputParams.put(16, type);
		inputParams.put(17, domain);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(18, OracleTypes.NUMBER);
		outputParamsTypes.put(19, OracleTypes.VARCHAR);
		outputParamsTypes.put(20, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(18, AppParams.RESULT_CODE);
		outputParamsNames.put(19, AppParams.RESULT_MSG);
		outputParamsNames.put(20, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.REPORT_CAMP_INSERT, inputParams,
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

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> Report Campaign insert result: " + resultMap.toString());

		return resultMap;
	}

	private static Map format(Map resultDataMap) {
		Map dataMap = new LinkedHashMap<>();

		dataMap.put(AppParams.ID, ParamUtil.getString(resultDataMap, AppParams.S_ID));
		dataMap.put(AppParams.TYPE, ParamUtil.getString(resultDataMap, AppParams.S_TYPE));
		dataMap.put(AppParams.CAMPAIGN_URL, ParamUtil.getString(resultDataMap, AppParams.S_CAMPAIGN_URL));
		dataMap.put(AppParams.REPORTER_FIRST_NAME, ParamUtil.getString(resultDataMap, AppParams.S_REPORTER_FIRST_NAME));
		dataMap.put(AppParams.REPORTER_LAST_NAME, ParamUtil.getString(resultDataMap, AppParams.S_REPORTER_LAST_NAME));
		dataMap.put(AppParams.REPORTER_ADD_LINE1, ParamUtil.getString(resultDataMap, AppParams.S_REPORTER_ADD_LINE1));
		dataMap.put(AppParams.REPORTER_ADD_LINE2, ParamUtil.getString(resultDataMap, AppParams.S_REPORTER_ADD_LINE2));
		dataMap.put(AppParams.STATE, ParamUtil.getString(resultDataMap, AppParams.S_REPORTER_STATE));
		dataMap.put(AppParams.COUNTRY, ParamUtil.getString(resultDataMap, AppParams.S_REPORTER_COUNTRY));
		dataMap.put(AppParams.EMAIL, ParamUtil.getString(resultDataMap, AppParams.S_REPORTER_EMAIL));
		dataMap.put(AppParams.PHONE, ParamUtil.getString(resultDataMap, AppParams.S_REPORTER_PHONE));
		dataMap.put(AppParams.OWNER_TYPE, ParamUtil.getString(resultDataMap, AppParams.S_OWNER_TYPE));
		dataMap.put(AppParams.OWNER_IP, ParamUtil.getString(resultDataMap, AppParams.S_OWNER_IP));
		dataMap.put(AppParams.DETAILS, ParamUtil.getString(resultDataMap, AppParams.S_DETAIL));
		dataMap.put(AppParams.CAMPAIGN_URL_ORIGINAL,
				ParamUtil.getString(resultDataMap, AppParams.S_CAMPAIGN_URL_ORIGINAL));
		dataMap.put(AppParams.ADDITIONAL, ParamUtil.getString(resultDataMap, AppParams.S_ADDITIONAL));
		dataMap.put(AppParams.VIOLATION, ParamUtil.getString(resultDataMap, AppParams.S_VIOLATION));
		dataMap.put(AppParams.DOMAIN, ParamUtil.getString(resultDataMap, AppParams.S_DOMAIN));

		return dataMap;
	}

	public static List<Map> shopifyGetCampInfo(String campaignId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map report = DBProcedureUtil.execute(dataSource, DBProcedurePool.DROPSHIP_GET_CAMP_INFO, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(report, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleSQLException();
		}

		return ParamUtil.getListData(report, AppParams.RESULT_DATA);

	}

	private static Map formatProducts(Map resultDataMap) {
		Map dataMap = new LinkedHashMap<>();

		dataMap.put(AppParams.ID, ParamUtil.getString(resultDataMap, AppParams.S_ID));

		dataMap.put(AppParams.TITLE, ParamUtil.getString(resultDataMap, AppParams.S_TITLE));

		dataMap.put(AppParams.DESC, ParamUtil.getString(resultDataMap, AppParams.S_DESC));

		dataMap.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(resultDataMap, AppParams.S_DESIGN_FRONT_URL));

		dataMap.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(resultDataMap, AppParams.S_DESIGN_BACK_URL));

		dataMap.put(AppParams.URI, ParamUtil.getString(resultDataMap, AppParams.S_URI));

		dataMap.put(AppParams.STATE, ParamUtil.getString(resultDataMap, AppParams.S_REPORTER_STATE));

		return dataMap;
	}

	private static Map formatCampaignArt(Map resultDataMap) {
		Map dataMap = new LinkedHashMap<>();

		dataMap.put(AppParams.ID, ParamUtil.getString(resultDataMap, AppParams.S_ID));

		dataMap.put(AppParams.TITLE, ParamUtil.getString(resultDataMap, AppParams.S_TITLE));

		dataMap.put(AppParams.ART_ID, ParamUtil.getString(resultDataMap, AppParams.S_ART_ID));

		dataMap.put(AppParams.ART_URL, ParamUtil.getString(resultDataMap, AppParams.S_ART_URL));

		dataMap.put(AppParams.ICON_URL, ParamUtil.getString(resultDataMap, AppParams.S_ICON_URL));

		dataMap.put(AppParams.ORDER, ParamUtil.getString(resultDataMap, AppParams.N_ORDERS));

		dataMap.put(AppParams.PROFIT, ParamUtil.getString(resultDataMap, AppParams.N_PROFIT));

		dataMap.put(AppParams.START_TIME, ParamUtil.getString(resultDataMap, AppParams.D_START));

		dataMap.put(AppParams.END_TIME, ParamUtil.getString(resultDataMap, AppParams.D_END));

		return dataMap;
	}

	public static Map search(String userId, String title, String tags, String startTime, String endTime, int page,
			int pageSize) throws SQLException, ParseException {

		LOGGER.fine("Campaign Art search with userId=" + userId + " title=" + title);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, title);
		inputParams.put(3, tags);
		inputParams.put(4, startTime);
		inputParams.put(5, endTime);
		inputParams.put(6, page);
		inputParams.put(7, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.VARCHAR);
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(8, AppParams.RESULT_CODE);
		outputParamsNames.put(9, AppParams.RESULT_MSG);
		outputParamsNames.put(10, AppParams.RESULT_TOTAL);
		outputParamsNames.put(11, AppParams.RESULT_DATA);

		Map campaignResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_ART_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(campaignResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(campaignResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(campaignResultMap, AppParams.RESULT_TOTAL);

		LOGGER.fine("Campaign Art search result: " + ParamUtil.getString(campaignResultMap, AppParams.RESULT_MSG));

		List<Map> resultDataList = ParamUtil.getListData(campaignResultMap, AppParams.RESULT_DATA);
		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {
			dataList.add(formatCampaignArt(resultDataMap));
		}
		Map resultMap = new LinkedHashMap();
		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.CAMPAIGN_ART, dataList);

		return resultMap;
	}

	public static List<Map> getInfoForEmailMarketing(String emailCampId, String campaignIds, String prCode,
			boolean isPreview) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignIds);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_GET_INFO_FOR_EMAIL_MARKETING,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_LIST;
		}

		resultDataList = formatCampaignEmail(emailCampId, resultDataList, prCode, isPreview);

		LOGGER.fine("=> getInfoForEmailMarketing result: " + resultDataList.toString());

		return resultDataList;

	}

	private static List<Map> formatCampaignEmail(String emailCampId, List<Map> resultDataList, String prCode,
			boolean isPreview) {
		List<Map> camps = new ArrayList<>();

		for (Map data : resultDataList) {
			Map el = new LinkedHashMap<>();
			el.put(AppParams.TITLE, ParamUtil.getString(data, AppParams.S_TITLE));
			el.put(AppParams.DESC, ParamUtil.getString(data, AppParams.S_DESC));
			double price = ParamUtil.getFormatedDouble(data, AppParams.S_SALE_PRICE, 2);
			el.put(AppParams.PRICE, price);
			el.put(AppParams.COMPARE_AT_PRICE, GetterUtil.formatDouble(price * 120 / 100, 2));
			el.put(AppParams.IMG_URL, ParamUtil.getString(data, AppParams.S_IMG_URL));
			if (!isPreview) {
				String url = MailUtil.getEmailTrackingUrl() + "?action=click&mail_camp=" + emailCampId + "&url="
						+ ParamUtil.getString(data, AppParams.S_URL) + "?mail_camp=" + emailCampId;
				if (StringUtils.isNotEmpty(prCode)) {
					url += "&pr=" + prCode;
				}
				el.put(AppParams.URL, url);
			}
			camps.add(el);
		}

		return camps;

	}

	public static void update(String campaignId, String url, String thumbUrl, boolean isCheckDesignSize,
			String basePrintableWidth, String basePrintableHeight, String width, String height, String designType,
			String colors, int colorsCount, String printPrice) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, url);
		inputParams.put(3, thumbUrl);
		inputParams.put(4, basePrintableWidth);
		inputParams.put(5, basePrintableHeight);
		inputParams.put(6, width);
		inputParams.put(7, height);
		inputParams.put(8, designType);
		inputParams.put(9, colors);
		inputParams.put(10, colorsCount);
		inputParams.put(11, printPrice);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_DATA);

		String procedureCallStr = DBProcedurePool.CAMP_PATCH_DESIGN;

		if (!isCheckDesignSize) {
			procedureCallStr = DBProcedurePool.CAMP_PATCH_UNCHECK_DESIGN;
		}

		Map updateResultMap = DBProcedureUtil.execute(dataSource, procedureCallStr, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(updateResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

	}

	public static Map update(String campaignId, String seoTitle, String seoDesc, String seoImageCover)
			throws SQLException, UnsupportedEncodingException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, seoTitle);
		inputParams.put(3, seoDesc);
		inputParams.put(4, seoImageCover);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_UPDATE_SEO, inputParams,
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

		Map resultMap = formatSEO(resultDataList.get(0));

		return resultMap;
	}

	public static Map get(String id) throws SQLException, ParseException {

		LOGGER.fine("Campaign SEO lookup with id=" + id);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_GET_SEO, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (!resultDataList.isEmpty()) {
			return formatSEO(resultDataList.get(0));
		} else {
			return Collections.EMPTY_MAP;
		}
	}

	private static Map formatSEO(Map resultDataMap) {
		Map dataMap = new LinkedHashMap<>();

		dataMap.put(AppParams.ID, ParamUtil.getString(resultDataMap, AppParams.S_ID));
		dataMap.put(AppParams.SEO_TITLE, ParamUtil.getString(resultDataMap, AppParams.S_SEO_TITLE));
		dataMap.put(AppParams.SEO_DESC, ParamUtil.getString(resultDataMap, AppParams.S_SEO_DESC));
		dataMap.put(AppParams.SEO_IMAGE_COVER, ParamUtil.getString(resultDataMap, AppParams.S_SEO_IMAGE_COVER));

		return dataMap;
	}

	public static Map searchUri(String campaignId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_GET_URI, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (!resultDataList.isEmpty()) {
			return resultDataList.get(0);
		} else {
			return Collections.EMPTY_MAP;
		}
	}

	public static Map insert(String campaignId, String takedownTime, String createBy, String campaignType)
			throws SQLException, UnsupportedEncodingException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, takedownTime);
		inputParams.put(3, createBy);
		inputParams.put(4, campaignType);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_TEAKEDOWN_INSERT, inputParams,
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

		Map resultMap = formatCampTakedown(resultDataList.get(0));

		LOGGER.info("=> Campaign Takedown insert result: " + resultMap.toString());

		return resultMap;

	}

	public static Map getTakedownCampaigns(String token)
			throws SQLException, UnsupportedEncodingException, ParseException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, token);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMP_TAKEDOWN_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(formatCampTakedown(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.CAMPAIGNS_TAKEDOWN, dataList);

		LOGGER.info("=> Campaigns takedown get result: " + resultMap.toString());

		return resultMap;
	}

	private static Map formatCampTakedown(Map resultDataMap) throws ParseException {
		Map dataMap = new LinkedHashMap<>();

		dataMap.put(AppParams.ID, ParamUtil.getString(resultDataMap, AppParams.S_ID));
		dataMap.put(AppParams.CAMPAIGN_TITLE, ParamUtil.getString(resultDataMap, AppParams.S_TITLE));
		dataMap.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(resultDataMap, AppParams.S_DESIGN_FRONT_URL));
		dataMap.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(resultDataMap, AppParams.S_DESIGN_BACK_URL));
		dataMap.put(AppParams.CREATE_TIME, ParamUtil.getString(resultDataMap, AppParams.D_CREATE));
		dataMap.put(AppParams.END_TIME, ParamUtil.getString(resultDataMap, AppParams.D_END));
		dataMap.put(AppParams.TOKEN, ParamUtil.getString(resultDataMap, AppParams.S_TOKEN));
		dataMap.put(AppParams.EMAIL, ParamUtil.getString(resultDataMap, AppParams.S_EMAIL));
		dataMap.put(AppParams.NAME, ParamUtil.getString(resultDataMap, AppParams.S_NAME));
		dataMap.put(AppParams.URI, ParamUtil.getString(resultDataMap, AppParams.S_URI));
		dataMap.put(AppParams.DOMAIN, ParamUtil.getString(resultDataMap, AppParams.S_DOMAIN));
		dataMap.put(AppParams.USER_ID, ParamUtil.getString(resultDataMap, AppParams.S_USER_ID));

		SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);
		long endTime = dateFormat.parse(ParamUtil.getString(resultDataMap, AppParams.D_END)).getTime();
		Calendar calendar = Calendar.getInstance();
		long remaining = endTime - calendar.getTime().getTime();
		dataMap.put(AppParams.REMAINING, remaining);

		return dataMap;
	}

	public static List<Map> getReceiveTakedownEmail(String userId, String domain) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domain);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map report = DBProcedureUtil.execute(dataSource, DBProcedurePool.RECEIVE_TAKEDOWN_EMAIL, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(report, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleSQLException();
		}
		LOGGER.info("=> ReceiveTakedownEmail result: " + ParamUtil.getListData(report, AppParams.RESULT_DATA));
		return ParamUtil.getListData(report, AppParams.RESULT_DATA);
	}

	public static Map detailCampaign(String userId, String domainName, String title, String categories, String tags,
			String startDate, String endDate, int privateValue, String state, int page, int pageSize, String orderby,
			String sortDirection, String filterColumn, boolean isOwner, boolean viewStats, boolean viewProfit,
			boolean addModifyCamp) throws SQLException, UnsupportedEncodingException, ParseException {

		Map resultMap = new LinkedHashMap();

		if (isOwner || viewStats || viewProfit || addModifyCamp) {
			Map detailData = getCampaignDetailData(userId, domainName, title, categories, tags, startDate, endDate,
					privateValue, state, page, pageSize, orderby, sortDirection, filterColumn, isOwner);

			int resultCode = ParamUtil.getInt(detailData, AppParams.RESULT_CODE);

			if (resultCode != HttpResponseStatus.OK.code()) {
				return new LinkedHashMap<>();
			}

			int resultTotalRow = ParamUtil.getInt(detailData, AppParams.RESULT_TOTAL);

			List<Map> resultDataList = ParamUtil.getListData(detailData, AppParams.RESULT_DATA);

			List<Map> dataList = new ArrayList<>();
//			Map allCategoryName = CategoryService.getCategoryNameCache();
			Map allCategoryName = CategoryService.getCategoryNameDbByUserIdAndDomainID(userId, domainName);

			for (Map resultDataMap : resultDataList) {
				dataList.add(formatCampaignDetail(resultDataMap, allCategoryName));
			}

			resultMap.put(AppParams.TOTAL, resultTotalRow);
			resultMap.put(AppParams.CAMPAIGNS, dataList);

		} else {
			resultMap.put(AppParams.TOTAL, 0);
			resultMap.put(AppParams.CAMPAIGNS, Collections.EMPTY_LIST);
		}

		return resultMap;
	}

	private static Map formatCampaignDetail(Map resultDataMap, Map allCategoryName) throws SQLException {

		Map dataMap = new LinkedHashMap<>();

		dataMap.put(AppParams.ID, ParamUtil.getString(resultDataMap, AppParams.S_ID));
		dataMap.put(AppParams.URL, ParamUtil.getString(resultDataMap, AppParams.S_URI));
		dataMap.put(AppParams.DOMAIN_ID, ParamUtil.getString(resultDataMap, AppParams.S_DOMAIN_ID));
		dataMap.put(AppParams.DOMAIN, ParamUtil.getString(resultDataMap, AppParams.S_DOMAIN));
		dataMap.put(AppParams.TITLE, ParamUtil.getString(resultDataMap, AppParams.S_TITLE));
		dataMap.put(AppParams.TAGS, ParamUtil.getString(resultDataMap, AppParams.S_TAGS));

		String categoryIds = ParamUtil.getString(resultDataMap, AppParams.S_CATEGORY_IDS);
		String[] categoryIdArr = categoryIds.split(",");

		String categoryName = new String();
		for (int i = 0; i < categoryIdArr.length; i++) {
			if (allCategoryName.containsKey(categoryIdArr[i])) {
				categoryName += allCategoryName.get(categoryIdArr[i]);
				if (i < categoryIdArr.length - 1) {
					categoryName += ",";
				}
			}
		}
		dataMap.put(AppParams.CATEGORIES, categoryName);

		dataMap.put(AppParams.STATE, ParamUtil.getString(resultDataMap, AppParams.S_STATE));
		dataMap.put(AppParams.PRIVATE, ParamUtil.getBoolean(resultDataMap, AppParams.N_PRIVATE));
		dataMap.put(AppParams.BACK_VIEW, ParamUtil.getBoolean(resultDataMap, AppParams.N_BACK_VIEW));
		dataMap.put(AppParams.CAMPAIGN_FRONT_IMAGE, ParamUtil.getString(resultDataMap, AppParams.S_FRONT_IMG_URL));
		dataMap.put(AppParams.CAMPAIGN_BACK_IMAGE, ParamUtil.getString(resultDataMap, AppParams.S_BACK_IMG_URL));
		dataMap.put(AppParams.FAVORITE, ParamUtil.getInt(resultDataMap, AppParams.N_FAVORITE));
		dataMap.put(AppParams.ARCHIVE, ParamUtil.getInt(resultDataMap, AppParams.N_ARCHIVE));
		dataMap.put(AppParams.BASE_GROUP_ID, ParamUtil.getString(resultDataMap, AppParams.S_BASE_GROUP_ID));
		dataMap.put(AppParams.CREATE_DATE, ParamUtil.getString(resultDataMap, AppParams.D_CREATE));
		dataMap.put(AppParams.VERSION, ParamUtil.getString(resultDataMap, AppParams.S_DESIGN_VERSION));
		dataMap.put(AppParams.UNITS, ParamUtil.getString(resultDataMap, AppParams.N_UNIT_SALES));

		return dataMap;
	}

	private static Map getCampaignDetailData(String userId, String domainName, String title, String categories,
			String tags, String startDate, String endDate, int privateValue, String state, int page, int pageSize,
			String orderby, String sortDirection, String filterColumn, boolean isOwner) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();

		inputParams.put(1, userId);
		inputParams.put(2, domainName);
		inputParams.put(3, title);
		inputParams.put(4, categories);
		inputParams.put(5, tags);
		inputParams.put(6, startDate);
		inputParams.put(7, endDate);
		inputParams.put(8, privateValue);
		inputParams.put(9, state);
		inputParams.put(10, page);
		inputParams.put(11, pageSize);
		inputParams.put(12, extractSortColumn(orderby));
		inputParams.put(13, sortDirection);
		inputParams.put(14, filterColumn);
		inputParams.put(15, isOwner);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(16, OracleTypes.NUMBER);
		outputParamsTypes.put(17, OracleTypes.VARCHAR);
		outputParamsTypes.put(18, OracleTypes.NUMBER);
		outputParamsTypes.put(19, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(16, AppParams.RESULT_CODE);
		outputParamsNames.put(17, AppParams.RESULT_MSG);
		outputParamsNames.put(18, AppParams.RESULT_TOTAL);
		outputParamsNames.put(19, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.GET_CAMP_DETAIL, inputParams,
				outputParamsTypes, outputParamsNames);
		return searchResultMap;
	}

	public static String getCampaignState(String campaignId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();

		inputParams.put(1, campaignId);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.GET_CAMPAIGN_STATE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleSQLException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		String campaignState = null;
		if (CollectionUtils.isNotEmpty(ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA))) {
			campaignState = ParamUtil.getString(
					(Map) ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA).get(0), AppParams.S_STATE);
		}
		return campaignState;
	}
	
	public static int isTrademark(String content) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, content);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_TOTAL);

		Map getResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CAMPAIGN_CHECK_TRADEMARK, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(getResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(getResultMap, AppParams.RESULT_MSG));
		}
		
		return ParamUtil.getInt(getResultMap, AppParams.RESULT_TOTAL);
	}

	
	public static Map getV2(String id) throws SQLException, ParseException {
		LOGGER.fine("Campaign lookup with id=" + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);
		outputParamsTypes.put(9, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		outputParamsNames.put(5, AppParams.RESULT_DESIGN);
		outputParamsNames.put(6, AppParams.RESULT_VARIANT);
		outputParamsNames.put(7, AppParams.RESULT_MOCKUP);
		outputParamsNames.put(8, AppParams.RESULT_PRODUCT_PRICE);
		outputParamsNames.put(9, AppParams.RESULT_PRODUCT_LIST);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, CAMP_GET_V2, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Campaign look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if(!resultDataList.isEmpty()) {
			Map mapProductDesigns = new LinkedHashMap<>();
			List<Map> resultDesign = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DESIGN);
			resultDesign = resultDesign.stream().map(o -> ProductDesignService.format(o)).collect(Collectors.toList());
			Set<String> productDesignIds = resultDesign.stream().map(o -> ParamUtil.getString(o, AppParams.PRODUCT_ID))
					.collect(Collectors.toSet());
			for (String productId : productDesignIds) {
				mapProductDesigns.put(productId,
						resultDesign.stream().filter(o -> productId.equals(ParamUtil.getString(o, AppParams.PRODUCT_ID)))
								.collect(Collectors.toList()));
			}
			

			List<Map> resultVariant = ParamUtil.getListData(searchResultMap, AppParams.RESULT_VARIANT);
			List<Map> resultVariantMap = new ArrayList<>();
			for (Map variant : resultVariant) {
				resultVariantMap.add(ProductVariantService.format(variant));
			}

			Set<String> productVariantIds = resultVariantMap.stream().map(o -> ParamUtil.getString(o, AppParams.PRODUCT_ID))
					.collect(Collectors.toSet());
			Map mapProductVariants = new LinkedHashMap<>();
			for (String productId : productVariantIds) {
				mapProductVariants.put(productId,
						resultVariantMap.stream().parallel()
								.filter(o -> productId.equals(ParamUtil.getString(o, AppParams.PRODUCT_ID)))
								.collect(Collectors.toList()));
			}

			List<Map> resultMockup = ParamUtil.getListData(searchResultMap, AppParams.RESULT_MOCKUP);
			resultMockup = resultMockup.stream().map(o -> MockupService.format(o)).collect(Collectors.toList());
			
			Set<String> mockupIds = resultMockup.stream().map(o -> ParamUtil.getString(o, AppParams.VARIANT_ID))
					.collect(Collectors.toSet());
			Map mapVariantMockups = new LinkedHashMap<>();

			for (String mockupId : mockupIds) {
				mapVariantMockups.put(mockupId,
						resultMockup.stream().filter(o -> mockupId.equals(ParamUtil.getString(o, AppParams.VARIANT_ID)))
								.collect(Collectors.toList()));
			}

			List<Map> resultProductPrice = ParamUtil.getListData(searchResultMap, AppParams.RESULT_PRODUCT_PRICE);
			Set<String> productPriceIds = resultProductPrice.stream()
					.map(o -> ParamUtil.getString(o, AppParams.S_PRODUCT_ID)).collect(Collectors.toSet());
			Map<String, Object> productPricesMap = new LinkedHashMap<>();
			for (String productId : productPriceIds) {
				productPricesMap.put(productId, resultProductPrice.stream()
						.filter(o -> productId.equals(ParamUtil.getString(o, AppParams.S_PRODUCT_ID))).map(o -> {
							return ProductPriceService.format(o);
						}).collect(Collectors.toList()));
			}

			
			List<Map> resultProductList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_PRODUCT_LIST);
			List<Map> productList = new ArrayList<>();
			for (Map resultDataMap : resultProductList) {
				Map productMap = formatProduct(mapProductDesigns, mapProductVariants, productPricesMap, mapVariantMockups, resultDataMap);
				
				productList.add(productMap);
			}
			if (!resultDataList.isEmpty()) {
				return formatV2(resultDataList.get(0), productList);
			} else {
				return Collections.EMPTY_MAP;
			}
		} else {
			return Collections.EMPTY_MAP;
		}
		
	}

	public static Map formatProduct(Map mapProductDesigns, Map mapProductVariants, Map productPricesMap, Map mockupVariantMap, Map resultDataMap) throws SQLException {
		Map productMap = new LinkedHashMap<>();

		String productId = ParamUtil.getString(resultDataMap, AppParams.S_ID);
		productMap.put(AppParams.ID, productId);
		productMap.put(AppParams.POSITION, ParamUtil.getString(resultDataMap, AppParams.N_POSITION));
		productMap.put(AppParams.BACK_VIEW, ParamUtil.getBoolean(resultDataMap, AppParams.N_BACK_VIEW));
		productMap.put(AppParams.DEFAULT, ParamUtil.getBoolean(resultDataMap, AppParams.N_DEFAULT));
		productMap.put(AppParams.STATE, ParamUtil.getString(resultDataMap, AppParams.S_STATE));
        productMap.put(AppParams.CURRENCY, ParamUtil.getString(resultDataMap, AppParams.S_CURRENCY));
        productMap.put(AppParams.SALE_EXPECTED, ParamUtil.getString(resultDataMap, AppParams.N_SALE_EXPECTED));
        productMap.put(AppParams.PRODUCT_NAME, ParamUtil.getString(resultDataMap, AppParams.S_NAME));
        productMap.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(resultDataMap, AppParams.S_CAMPAIGN_ID));
        productMap.put(AppParams.PRODUCT_TYPE, ParamUtil.getString(resultDataMap, AppParams.S_PRODUCT_TYPE));

		List<Map> productPricesList = productPricesMap == null || productPricesMap.isEmpty()
				? ProductPriceService.getPrices(productId)
				: ParamUtil.getListData(productPricesMap, productId);
		productMap.put(AppParams.PRICES, productPricesList);

		Map artMap = new LinkedHashMap<>();
		artMap.put(AppParams.ART_ID_FRONT, ParamUtil.getString(resultDataMap, AppParams.S_ART_ID_FRONT));
		artMap.put(AppParams.ART_ID_FRONT, ParamUtil.getString(resultDataMap, AppParams.S_ART_ID_FRONT));
		artMap.put(AppParams.ART_PRICE_FRONT, ParamUtil.getString(resultDataMap, AppParams.S_ART_PRICE_FRONT));
		artMap.put(AppParams.ART_PRICE_TYPE_FRONT, ParamUtil.getString(resultDataMap, AppParams.S_ART_PRICE_TYPE_FRONT));

		productMap.put(AppParams.ART, artMap);
		String baseId = ParamUtil.getString(resultDataMap, AppParams.S_BASE_ID);
		productMap.put("display", true);

		boolean isPhoneCaseDisplay = ParamUtil.getBoolean(resultDataMap, "N_PHONECASE_DISPLAY");
		if (BasePhoneCaseUtil.isPhoneCase(baseId)) {
			if (!isPhoneCaseDisplay) {
				productMap.replace("display", false);
			}
		}

		productMap.put(AppParams.BASE, CampaignUtil.getBaseInfo(resultDataMap));
        
		String productColorIds = ParamUtil.getString(resultDataMap, AppParams.S_COLORS);
		String defaultColorId = ParamUtil.getString(resultDataMap, AppParams.S_DEFAULT_COLOR_ID);
		productMap.put(AppParams.COLORS, CampaignUtil.getBaseColorList(baseId, productColorIds, defaultColorId));
		productMap.put(AppParams.DESIGNS, mapProductDesigns == null ? Collections.EMPTY_LIST : ParamUtil.getListData(mapProductDesigns, productId));

		if (mapProductVariants != null && !mapProductVariants.isEmpty()) {
			List<Map> variantMapList = ParamUtil.getListData(mapProductVariants, productId);
			for (Map variantMap : variantMapList) {
				List<Map> mockupList = ParamUtil.getListData(mockupVariantMap,
						ParamUtil.getString(variantMap, AppParams.ID));
				if (CollectionUtils.isNotEmpty(mockupList)) {
					variantMap.put(AppParams.MOCKUPS, mockupList);
				}
			}
			productMap.put(AppParams.VARIANTS, variantMapList);
		}
		return productMap;
	}
	

	private static Map formatV2(Map queryData, List<Map> campaignProductList) throws SQLException, ParseException {

		Map resultMap = new LinkedHashMap<>();

		String campaignId = ParamUtil.getString(queryData, AppParams.S_ID);

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));
		resultMap.put(AppParams.TITLE, ParamUtil.getString(queryData, AppParams.S_TITLE));
//		resultMap.put(AppParams.DESC, StringUtil.urlEncode(ParamUtil.getString(queryData, AppParams.S_DESC)));
		resultMap.put(AppParams.DESC, ParamUtil.getString(queryData, AppParams.S_DESC));
		resultMap.put(AppParams.DOMAIN_ID, ParamUtil.getString(queryData, AppParams.S_DOMAIN_ID));
		resultMap.put(AppParams.DOMAIN, ParamUtil.getString(queryData, AppParams.S_DOMAIN));
		resultMap.put(AppParams.BASE_GROUP_ID, ParamUtil.getString(queryData, AppParams.S_BASE_GROUP_ID));

		resultMap.put(AppParams.SEO_TITLE, ParamUtil.getString(queryData, AppParams.S_SEO_TITLE));
		resultMap.put(AppParams.SEO_DESC, ParamUtil.getString(queryData, AppParams.S_SEO_DESC));
		resultMap.put(AppParams.SEO_IMAGE_COVER, ParamUtil.getString(queryData, AppParams.S_SEO_IMAGE_COVER));

		resultMap.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_FRONT_URL));
		resultMap.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_BACK_URL));
		resultMap.put(AppParams.BACK_VIEW, ParamUtil.getBoolean(queryData, AppParams.N_BACK_VIEW));

		String campaignUri = ParamUtil.getString(queryData, AppParams.S_URI);

//		if (campaignUri != null && !campaignUri.isEmpty() && !campaignUri.startsWith(StringPool.FORWARD_SLASH)) {
//			campaignUri = StringPool.FORWARD_SLASH + campaignUri;
//		}

		resultMap.put(AppParams.URL, campaignUri);
		resultMap.put(AppParams.TAGS, ParamUtil.getString(queryData, AppParams.S_TAGS));
		resultMap.put(AppParams.CATEGORIES, ParamUtil.getString(queryData, AppParams.S_CATEGORY_IDS));
		resultMap.put(AppParams.STORES, ParamUtil.getString(queryData, AppParams.S_STORES));
		resultMap.put(AppParams.PRIVATE, ParamUtil.getBoolean(queryData, AppParams.N_PRIVATE));

		if (!ParamUtil.getString(queryData, AppParams.D_START).isEmpty()) {
			resultMap.put(AppParams.START_TIME, ParamUtil.getString(queryData, AppParams.D_START));
		}

		if (!ParamUtil.getString(queryData, AppParams.D_START).isEmpty()) {
			resultMap.put(AppParams.END_TIME, ParamUtil.getString(queryData, AppParams.D_END));
		}

		if (ResourceStates.LAUNCHING.equals(ParamUtil.getString(queryData, AppParams.S_STATE))) {
			resultMap.put(AppParams.REMAINING, CampaignUtil.getTimeRemaining());
		}
		resultMap.put(AppParams.RELAUNCH, ParamUtil.getBoolean(queryData, AppParams.N_AUTO_RELAUNCH));
		resultMap.put(AppParams.FB_PIXEL, ParamUtil.getString(queryData, AppParams.S_FB_PIXEL));
		resultMap.put(AppParams.GG_PIXEL, ParamUtil.getString(queryData, AppParams.S_GG_PIXEL));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.VERSION, ParamUtil.getString(queryData, AppParams.S_DESIGN_VERSION));

		resultMap.put(AppParams.PRODUCTS, campaignProductList);
		return resultMap;
	}
	
	public static Map getCampaignDropship(String userId, String domainName, String title, String startDate, String endDate, String state, int page, int pageSize,
			String orderby, String orderDriection, boolean isOwner) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domainName);
		inputParams.put(3, title);
		inputParams.put(4, startDate);
		inputParams.put(5, endDate);
		inputParams.put(6, state);
		inputParams.put(7, page);
		inputParams.put(8, pageSize);
		inputParams.put(9, orderby);
		inputParams.put(10, orderDriection);
		inputParams.put(11, isOwner);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.NUMBER);
		outputParamsTypes.put(15, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_TOTAL);
		outputParamsNames.put(15, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_CAMPAIGN_DROPSHIP, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();
//		Map allCategoryName = CategoryService.getCategoryNameCache();
		Map allCategoryName = CategoryService.getCategoryNameDbByUserIdAndDomainID(userId, domainName);

		for (Map resultDataMap : resultDataList) {
			dataList.add(formatCampaignDetail(resultDataMap, allCategoryName));
		}
		
		Map resultMap = new LinkedHashMap();
		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.CAMPAIGNS, dataList);
		
		return resultMap;
	}
	
	private static final Logger LOGGER = Logger.getLogger(CampaignService.class.getName());
}
