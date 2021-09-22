package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.email.MailUtil;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class EmailCampaignsService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static Map lookUpEmailList(String userId, String id) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);
		outputParamsTypes.put(6, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);
		outputParamsNames.put(6, AppParams.PENDING);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_LOOK_UP_EMAIL_LIST, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		int pending = ParamUtil.getInt(resultMap, AppParams.PENDING);
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		Map reponseDataMap = Collections.EMPTY_MAP;
		if (!resultDataList.isEmpty()) {
			reponseDataMap = formatEmailList(resultDataList.get(0));
			reponseDataMap.put(AppParams.PENDING, pending);
		}

		LOGGER.info("=> lookUpCampaign result: " + reponseDataMap.toString());
		return reponseDataMap;
	}

	public static Map unsubscribeEmail(String domainId, String email) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, email);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_UNSUBSCRIBE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		return Collections.EMPTY_MAP;
	}

	public static Map searchEmailList(String userId, String name, String state, int page, int pageSize)
			throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, name);
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

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_SEARCH_EMAIL_LIST, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		Map response = new LinkedHashMap<>();
		List<Map> emailist = new ArrayList<Map>();
		response.put(AppParams.TOTAL, ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL));
		for (Map rs : resultDataList) {
			emailist.add(formatEmailList(rs));
		}
		response.put(AppParams.DATA, emailist);
		LOGGER.fine("=> searchEmailList result: " + response.toString());
		return response;
	}

	public static Map lookupEmailCampaign(String userId, String id) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_LOOK_UP_EMAIL_CAMPAIGN,
				inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		resultMap = formatEmailCampaign(resultDataList.get(0));
		LOGGER.fine("=> lookupEmailCampaign result: " + resultMap.toString());
		return resultMap;
	}

	public static Map searchEmailCampaign(String userId, String filter, String state, int page, int pageSize,
			String startDate, String endDate) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, filter);
		inputParams.put(3, state);
		inputParams.put(4, page);
		inputParams.put(5, pageSize);
		inputParams.put(6, startDate);
		inputParams.put(7, endDate);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.VARCHAR);
		outputParamsTypes.put(10, OracleTypes.CURSOR);
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(8, AppParams.RESULT_CODE);
		outputParamsNames.put(9, AppParams.RESULT_MSG);
		outputParamsNames.put(10, AppParams.RESULT_DATA);
		outputParamsNames.put(11, AppParams.RESULT_TOTAL);
		outputParamsNames.put(12, AppParams.RESULT_OVERVIEW);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_SEARCH_EMAIL_CAMPAIGN,
				inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		Map response = new LinkedHashMap<>();
		response.put(AppParams.TOTAL, ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL));
		List<Map> campOverview = ParamUtil.getListData(resultMap, AppParams.RESULT_OVERVIEW);
		Map overviewData = campOverview.isEmpty() && campOverview.size() > 0 ? Collections.EMPTY_MAP
				: formatCampaignsOverview(campOverview.get(0));
		response.put(AppParams.OVERVIEW, overviewData);

		List<Map> emailist = new ArrayList<Map>();
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		for (Map rs : resultDataList) {
			emailist.add(formatEmailCampaign(rs));
		}
		response.put(AppParams.DATA, emailist);

		LOGGER.fine("=> searchEmailCampaign result: " + response.toString());
		return response;
	}

	public static Map searchAddCampaigns(String userId, String title, int isPrivate, int page, int pageSize)
			throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, title);
		inputParams.put(3, isPrivate);
		inputParams.put(4, page);
		inputParams.put(5, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_SEARCH_ADD_CAMPAIGNS,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		Map resultData = formatListAddCampaigns(resultDataList);

		LOGGER.fine("=> searchAddCampaigns result: " + resultData.toString());

		return resultData;
	}

	private static Map formatListAddCampaigns(List<Map> resultDataList) {
		Map resultData = new LinkedHashMap<>();
		resultData.put(AppParams.TOTAL, resultDataList.size());

		List<Map> camps = new ArrayList<>();
		for (Map data : resultDataList) {
			Map el = new LinkedHashMap<>();
			el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
			el.put(AppParams.TITLE, ParamUtil.getString(data, AppParams.S_TITLE));
			el.put(AppParams.DOMAIN, ParamUtil.getString(data, AppParams.S_DOMAIN));
			el.put(AppParams.ORDER, ParamUtil.getInt(data, AppParams.N_ORDER));
			el.put(AppParams.IMG_URL, ParamUtil.getString(data, AppParams.S_IMG_URL));
			camps.add(el);
		}
		resultData.put(AppParams.DATA, camps);
		return resultData;
	}

	private static Map formatCampaignsOverview(Map overview) {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.TOTAL_SENT_CAMP, ParamUtil.getString(overview, AppParams.N_TOTAL_SENT_CAMP));
		el.put(AppParams.OPEN_RATE, ParamUtil.getFormatedDouble(overview, AppParams.N_OPEN_RATE, 2));
		el.put(AppParams.CLICK_RATE, ParamUtil.getFormatedDouble(overview, AppParams.N_CLICK_RATE, 2));
		el.put(AppParams.TOTAL_PROFIT, ParamUtil.getFormatedDouble(overview, AppParams.N_TOTAL_PROFIT, 2));
		return el;
	}

	public static void updateOpenCountForCampaign(String id) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_UPDATE_OPEN_COUNT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}

	public static void updateCLickCountForCampaign(String id) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_UPDATE_CLICK_COUNT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}

	public static Map insertEmailCampaign(String name, String scheduleTime, String title, String description,
			String domainId, String domainName, String templateId, String userId, String campaignIds, String prId,
			String prCode, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, name);
		inputParams.put(2, scheduleTime);
		inputParams.put(3, title);
		inputParams.put(4, description);
		inputParams.put(5, domainId);
		inputParams.put(6, domainName);
		inputParams.put(7, templateId);
		inputParams.put(8, userId);
		inputParams.put(9, campaignIds);
		inputParams.put(10, prId);
		inputParams.put(11, prCode);
		inputParams.put(12, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);
		outputParamsTypes.put(15, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);
		outputParamsNames.put(15, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_INSERT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatEmailCampaign(resultDataList.get(0));
		LOGGER.info("=> insertEmailCampaign result: " + resultMap.toString());
		return resultMap;
	}

	public static Map updateEmailCampaign(String id, String name, String scheduleTime, String title, String description,
			String domainId, String domainName, String templateId, String userId, String campaignIds,
			String emailListId, String prId, String prCode) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, name);
		inputParams.put(3, scheduleTime);
		inputParams.put(4, title);
		inputParams.put(5, description);
		inputParams.put(6, domainId);
		inputParams.put(7, domainName);
		inputParams.put(8, templateId);
		inputParams.put(9, userId);
		inputParams.put(10, campaignIds);
		inputParams.put(11, emailListId);
		inputParams.put(12, prId);
		inputParams.put(13, prCode);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(14, OracleTypes.NUMBER);
		outputParamsTypes.put(15, OracleTypes.VARCHAR);
		outputParamsTypes.put(16, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(14, AppParams.RESULT_CODE);
		outputParamsNames.put(15, AppParams.RESULT_MSG);
		outputParamsNames.put(16, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_UPDATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatEmailCampaign(resultDataList.get(0));
		LOGGER.info("=> updateEmailCampaign result: " + resultMap.toString());
		return resultMap;

	}

	public static Map insertEmailCampaignDetail(String emailCampId, String campaignIds, String templateId,
			String domainId, String userId, String title, String description, String prCode) throws SQLException {

		LOGGER.info("Lookup domain userId=" + userId + ", domainId=" + domainId);
		Map domainInfo = DomainService.lookup(domainId, userId);
		String domain = ParamUtil.getString(domainInfo, AppParams.NAME);
		String banner = ParamUtil.getString(domainInfo, AppParams.BANNER);
		String logo = ParamUtil.getString(domainInfo, AppParams.LOGO);

		Map emailTemplate = EmailTemplateService.get(templateId);
		String templateContent = ParamUtil.getString(emailTemplate, AppParams.CONTENT);
		int templateColumn = ParamUtil.getInt(emailTemplate, AppParams.COLUMN);

		String mailContent = MailUtil.processMarketingEmailCampaignContent(emailCampId, domainId, domain, banner, logo,
				title, description, campaignIds, templateContent, templateColumn, prCode, false);

		return saveEmailCampaignDetailToDatabase(emailCampId, campaignIds, templateId, title, mailContent, logo, banner,
				ResourceStates.APPROVED);

	}

	private static Map saveEmailCampaignDetailToDatabase(String emailCampId, String campaignIds, String templateId,
			String templateSubject, String mailContent, String logo, String banner, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, emailCampId);
		inputParams.put(2, campaignIds);
		inputParams.put(3, templateId);
		inputParams.put(4, templateSubject);
		inputParams.put(5, mailContent);
		inputParams.put(6, logo);
		inputParams.put(7, banner);
		inputParams.put(8, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(9, OracleTypes.NUMBER);
		outputParamsTypes.put(10, OracleTypes.VARCHAR);
		outputParamsTypes.put(11, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(9, AppParams.RESULT_CODE);
		outputParamsNames.put(10, AppParams.RESULT_MSG);
		outputParamsNames.put(11, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_INSERT_DETAIL, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = formatEmailCampDetail(resultDataList.get(0));
		LOGGER.fine("=> insertEmailCampaignDetail result: " + resultMap.toString());
		return resultMap;
	}

	public static Map deleteEmailList(String userId, String id) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_DELETE_EMAIL_LIST, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		return Collections.EMPTY_MAP;
	}

	public static Map deleteEmailCampaign(String userId, String id) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_DELETE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		return Collections.EMPTY_MAP;
	}

	private static Map formatEmailList(Map data) {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.USER_ID, ParamUtil.getString(data, AppParams.S_USER_ID));
		el.put(AppParams.NAME, ParamUtil.getString(data, AppParams.S_NAME));
		el.put(AppParams.TOTAL_EMAIL, ParamUtil.getInt(data, AppParams.N_TOTAL_EMAIL));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		el.put(AppParams.LAST_SENT, ParamUtil.getString(data, AppParams.D_LAST_SENT));
		el.put(AppParams.COMPLAINTS, ParamUtil.getInt(data, AppParams.N_COMPLAINTS));
		el.put(AppParams.BOUNCES, ParamUtil.getInt(data, AppParams.N_BOUNCES));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		el.put(AppParams.SOURCE, ParamUtil.getString(data, AppParams.S_SOURCE));
		el.put(AppParams.DESC, ParamUtil.getString(data, AppParams.S_DESC));
		return el;
	}

	private static Map formatEmailCampaign(Map data) {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.NAME, ParamUtil.getString(data, AppParams.S_NAME));
		el.put(AppParams.SCHEDULE_TIME, ParamUtil.getString(data, AppParams.D_SCHEDULE_TIME));
		el.put(AppParams.TITLE, ParamUtil.getString(data, AppParams.S_TITLE));
		el.put(AppParams.DESC, ParamUtil.getString(data, AppParams.S_DESC));
		el.put(AppParams.DOMAIN_ID, ParamUtil.getString(data, AppParams.S_DOMAIN_ID));
		el.put(AppParams.DOMAIN, ParamUtil.getString(data, AppParams.S_DOMAIN));
		el.put(AppParams.TEMPLATE_ID, ParamUtil.getString(data, AppParams.S_TEMPLATE_ID));
		el.put(AppParams.TOTAL_EMAIL, ParamUtil.getInt(data, AppParams.N_TOTAL_EMAIL));
		int totalSent = ParamUtil.getInt(data, AppParams.N_TOTAL_SENT);
		el.put(AppParams.TOTAL_SENT, totalSent);
		el.put(AppParams.TOTAL_MAKE_SPAM, ParamUtil.getInt(data, AppParams.N_TOTAL_MAKE_SPAM));
		el.put(AppParams.TOTAL_INVALID, ParamUtil.getInt(data, AppParams.N_TOTAL_INVALID));
		int totalOpen = ParamUtil.getInt(data, AppParams.N_TOTAL_OPEN);
		el.put(AppParams.TOTAL_OPEN, totalOpen);
		el.put(AppParams.OPEN_RATE, totalSent == 0 ? 0 : GetterUtil.formatDouble((double) totalOpen / totalSent, 2));

		int totalClick = ParamUtil.getInt(data, AppParams.N_TOTAL_CLICK);
		el.put(AppParams.TOTAL_CLICK, totalClick);
		el.put(AppParams.CLICK_RATE, totalSent == 0 ? 0 : GetterUtil.formatDouble((double) totalClick / totalSent, 2));

		el.put(AppParams.TOTAL_PROFIT, ParamUtil.getString(data, AppParams.S_TOTAL_PROFIT));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		el.put(AppParams.USER_ID, ParamUtil.getString(data, AppParams.S_USER_ID));
		el.put(AppParams.EMAIL_LIST_ID, ParamUtil.getString(data, AppParams.S_EMAIL_LIST_ID));
		el.put(AppParams.CAMPAIGNS, ParamUtil.getString(data, AppParams.S_CAMPAIGN_IDS));
		el.put(AppParams.PROMOTION_ID, ParamUtil.getString(data, AppParams.S_PR_ID));
		el.put(AppParams.PROFIT, ParamUtil.getFormatedDouble(data, AppParams.N_PROFIT, 2));
		return el;
	}

	private static Map formatEmailCampDetail(Map data) {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.EMAIL_CAMPAIGN_ID, ParamUtil.getString(data, AppParams.S_EMAIL_CAMPAIGN_ID));
		el.put(AppParams.CAMPAIGN_IDS, ParamUtil.getString(data, AppParams.S_CAMPAIGN_IDS));
		el.put(AppParams.TEMPLATE_ID, ParamUtil.getString(data, AppParams.S_TEMPLATE_ID));
		el.put(AppParams.EMAIL_SUBJECT, ParamUtil.getString(data, AppParams.S_EMAIL_SUBJECT));
		el.put(AppParams.EMAIL_BODY, ParamUtil.getString(data, AppParams.S_EMAIL_BODY));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		el.put(AppParams.TEMPLATE_LOGO, ParamUtil.getString(data, AppParams.S_TEMPLATE_LOGO));
		el.put(AppParams.TEMPLATE_BANNER, ParamUtil.getString(data, AppParams.S_TEMPLATE_BANNER));
		return el;
	}

	public static void scheduleSending(String userId, String emailCampId, int totalEmail, int freeRemaining, int buyRemaining) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, emailCampId);
		inputParams.put(3, totalEmail);
		inputParams.put(4, freeRemaining);
		inputParams.put(5, buyRemaining);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.EMAIL_CAMP_SCHEDULE_SENDING, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}

	private static final Logger LOGGER = Logger.getLogger(EmailCampaignsService.class.getName());

}
