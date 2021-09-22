package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class UserService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 
	 * @param id - affiliate id
	 * @return
	 * @throws SQLException
	 */
	public static Map get(String id) throws SQLException {

		LOGGER.info("User lookup with id=" + id);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.USER_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		LOGGER.fine("=> User look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return format(resultDataList.get(0));
	}

//	public static Map lookUp(String aspUserId) throws SQLException {
//
//		LOGGER.fine("User lookup with aspUserId=" + aspUserId);
//
//		Map inputParams = new LinkedHashMap<Integer, String>();
//		inputParams.put(1, aspUserId);
//
//		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
//		outputParamsTypes.put(2, OracleTypes.NUMBER);
//		outputParamsTypes.put(3, OracleTypes.VARCHAR);
//		outputParamsTypes.put(4, OracleTypes.CURSOR);
//
//		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
//		outputParamsNames.put(2, AppParams.RESULT_CODE);
//		outputParamsNames.put(3, AppParams.RESULT_MSG);
//		outputParamsNames.put(4, AppParams.RESULT_DATA);
//
//		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.USER_LOOKUP, inputParams,
//				outputParamsTypes, outputParamsNames);
//
//		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);
//
//		if (resultCode != HttpResponseStatus.OK.code()) {
//			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
//		}
//
//		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
//
//		LOGGER.fine("=> User look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
//
//		if (resultDataList.isEmpty()) {
//			return new LinkedHashMap();
//		} else {
//			return format(resultDataList.get(0));
//		}
//	}

	public static Map insert(String aspId, String groupId, String parentId, String referrer, String name, String email,
			String mobile, String avatar, String languageId, String timezone, String country) throws SQLException {

		LOGGER.fine("User insert with aspId=" + aspId + ", groupId=" + groupId + ", parentId=" + parentId
				+ ", referrer=" + referrer + ", name=" + name + ", email=" + email);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, aspId);
		inputParams.put(2, groupId);
		inputParams.put(3, parentId);
		inputParams.put(4, referrer);
		inputParams.put(5, name);
		inputParams.put(6, email);
		inputParams.put(7, mobile);
		inputParams.put(8, avatar);
		inputParams.put(9, languageId);
		inputParams.put(10, timezone);
		inputParams.put(11, country);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.USER_INSERT, inputParams,
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

		LOGGER.fine("=> User insert result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static Map insert(String aspRef, String groupId, String parentId, String name, String email, String mobile,
			String languageId, String timezone, String state, String referrer, String avatar, String country,
			String countryCode, String website, String note) throws SQLException {

		LOGGER.fine("User insert with aspId=" + aspRef + ", groupId=" + groupId + ", parentId=" + parentId
				+ ", referrer=" + referrer + ", name=" + name + ", email=" + email);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, aspRef);
		inputParams.put(2, groupId);
		inputParams.put(3, parentId);
		inputParams.put(4, name);
		inputParams.put(5, email);
		inputParams.put(6, mobile);
		inputParams.put(7, languageId);
		inputParams.put(8, timezone);
		inputParams.put(9, state);
		inputParams.put(10, referrer);
		inputParams.put(11, avatar);
		inputParams.put(12, country);
		inputParams.put(13, countryCode);
		inputParams.put(14, website);
		inputParams.put(15, note);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(16, OracleTypes.NUMBER);
		outputParamsTypes.put(17, OracleTypes.VARCHAR);
		outputParamsTypes.put(18, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(16, AppParams.RESULT_CODE);
		outputParamsNames.put(17, AppParams.RESULT_MSG);
		outputParamsNames.put(18, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.USER_ASP_INSERT, inputParams,
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

		LOGGER.fine("=> User insert result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static Map update(String aspId, String groupId, String name, String email, String mobile, String avatar,
			String languageId, String timezone, String state, String country) throws SQLException {

		LOGGER.fine("User update with aspId=" + aspId + ", groupId=" + groupId + ", name=" + name + ", email=" + email
				+ ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, aspId);
		inputParams.put(2, groupId);
		inputParams.put(3, name);
		inputParams.put(4, email);
		inputParams.put(5, mobile);
		inputParams.put(6, avatar);
		inputParams.put(7, languageId);
		inputParams.put(8, timezone);
		inputParams.put(9, state);
		inputParams.put(10, country);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.VARCHAR);
		outputParamsTypes.put(13, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(11, AppParams.RESULT_CODE);
		outputParamsNames.put(12, AppParams.RESULT_MSG);
		outputParamsNames.put(13, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.USER_UPDATE, inputParams,
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

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> User update result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

		return resultMap;
	}

	public static Map findByToken(String token) throws SQLException {

		LOGGER.info("User lookup with token=" + token);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, token);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.USER_FIND_BY_TOKEN, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		LOGGER.fine("=> User look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

		return format(resultDataList.get(0));
	}

	public static Map searchReferrals(String userId, double profitPerUnit, String startDate, String endDate, int page,
			int pageSize) throws SQLException {

            
            
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, profitPerUnit);
		inputParams.put(3, startDate);
		inputParams.put(4, endDate);
		inputParams.put(5, page);
		inputParams.put(6, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.VARCHAR);
		outputParamsTypes.put(9, OracleTypes.CURSOR);
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(7, AppParams.RESULT_CODE);
		outputParamsNames.put(8, AppParams.RESULT_MSG);
		outputParamsNames.put(9, AppParams.RESULT_DATA);
		outputParamsNames.put(10, AppParams.TOTAL_REFERALS);
		outputParamsNames.put(11, AppParams.TOTAL_PROFIT);
		outputParamsNames.put(12, AppParams.TOTAL_SALE);
		outputParamsNames.put(13, AppParams.REFERRER);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.USER_GET_REFERRALS, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
		List<Map> responseDataList = new ArrayList<>();
		if (!resultDataList.isEmpty()) {
			for (Map data : resultDataList) {
				responseDataList.add(formatReferral(data));
			}
		}

		Map response = new LinkedHashMap<>();
		response.put(AppParams.TOTAL, ParamUtil.getString(searchResultMap, AppParams.TOTAL_REFERALS));
		response.put(AppParams.PROFIT, ParamUtil.getString(searchResultMap, AppParams.TOTAL_PROFIT));
		response.put(AppParams.SALE, ParamUtil.getString(searchResultMap, AppParams.TOTAL_SALE));
		response.put(AppParams.REFERRALS, responseDataList);
		response.put(AppParams.REFERRER, ParamUtil.getString(searchResultMap, AppParams.REFERRER));

		LOGGER.fine("=> Referrals result " + response.toString());

		return response;
	}

	private static Map formatReferral(Map data) {
		Map referral = new LinkedHashMap<>();
		referral.put(AppParams.EMAIL, ParamUtil.getString(data, AppParams.S_EMAIL));
		referral.put(AppParams.ACCEPT_DATE, ParamUtil.getString(data, AppParams.D_ACCEPT));
		referral.put(AppParams.PROFIT, ParamUtil.getString(data, AppParams.N_PROFIT));
		referral.put(AppParams.SALE, ParamUtil.getString(data, AppParams.N_SALE));
		return referral;
	}

	private static Map format(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		resultMap.put(AppParams.ASP_ID, ParamUtil.getString(queryData, AppParams.S_ASP_REF));

		if (!ParamUtil.getString(queryData, AppParams.S_PARENT_ID).isEmpty()) {
			resultMap.put(AppParams.PARENT_ID, ParamUtil.getString(queryData, AppParams.S_PARENT_ID));
		}

		if (!ParamUtil.getString(queryData, AppParams.S_GROUP_ID).isEmpty()) {
			Map userGroupInfoMap = new LinkedHashMap();

			userGroupInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_GROUP_ID));
			userGroupInfoMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_GROUP_NAME));

			resultMap.put(AppParams.GROUP, userGroupInfoMap);
		}

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		resultMap.put(AppParams.EMAIL, ParamUtil.getString(queryData, AppParams.S_EMAIL));

		if (!ParamUtil.getString(queryData, AppParams.S_MOBILE).isEmpty()) {
			resultMap.put(AppParams.MOBILE, ParamUtil.getString(queryData, AppParams.S_MOBILE));
		}

		if (!ParamUtil.getString(queryData, AppParams.S_AVATAR).isEmpty()) {
			resultMap.put(AppParams.AVATAR, ParamUtil.getString(queryData, AppParams.S_AVATAR));
		}

		resultMap.put(AppParams.TIMEZONE, ParamUtil.getString(queryData, AppParams.S_TIMEZONE));

		resultMap.put(AppParams.LANGUAGE, ParamUtil.getString(queryData, AppParams.S_LANGUAGE_ID));

		resultMap.put(AppParams.COUNTRY, ParamUtil.getString(queryData, AppParams.S_COUNTRY));

		resultMap.put(AppParams.API_TOKEN, ParamUtil.getString(queryData, AppParams.S_API_TOKEN));

		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

		resultMap.put(AppParams.TEST, ParamUtil.getBoolean(queryData, AppParams.N_IS_TEST));
		
		resultMap.put(AppParams.W8, ParamUtil.getInt(queryData, AppParams.N_IS_W8));
		
		resultMap.put(AppParams.W8_URL, ParamUtil.getString(queryData, AppParams.S_W8_URL));
                
        resultMap.put(AppParams.REF_OWNER, ParamUtil.getBoolean(queryData, AppParams.N_REF_OWNER));
        
        resultMap.put(AppParams.TOOL_SCRIPTS, ParamUtil.getInt(queryData, AppParams.N_TOOL_SCRIPTS));
        
        resultMap.put("private_camp", ParamUtil.getInt(queryData, "N_PRIVATE_CAMP"));
                
		return resultMap;
	}
	
	public static Map updateW8(String userId, String url) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, url);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.USER_UPDATE_W8, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(updateResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = format(resultDataList.get(0));

		return resultMap;
	}
	
	public static void markToolScripts(String userId, int flag) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, flag);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.USER_MARK_TOOL_SCRIPTS, inputParams,outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

	}
	
	public static void insertLoginHistory(String affId, String sourceIp, String countryName, String countryCode, String city, String stateRegion, String userAgent, String device, String os, String osVersion, String browsers, String browsersVersion) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, affId);
		inputParams.put(2, sourceIp);
		inputParams.put(3, countryName);
		inputParams.put(4, countryCode);
		inputParams.put(5, city);
		inputParams.put(6, stateRegion);
		inputParams.put(7, userAgent);
		inputParams.put(8, device);
		inputParams.put(9, os);
		inputParams.put(10, osVersion);
		inputParams.put(11, browsers);
		inputParams.put(12, browsersVersion);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.LOGIN_HISTORY_INSERT, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

	}

	private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

}
