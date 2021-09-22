package asia.leadsgen.psp.service_fulfill;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.util.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.CampaignUtil;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class DropShipStoreCampService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	private static final String UPDATE_STORE_CAMP_SYNC = "{call pkg_dropship_store_camp.update_store_camp_sync(?,?,?,?,?)}";
	
	static final String DROPSHIP_STORE_CAMP_INSERT = "{call pkg_dropship_store_camp.insert_store_camp(?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_STORE_CAMP_LIST_CAMPS = "{call pkg_dropship_store_camp.list_campaigns(?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_STORE_CAMP_LIST_PRODUCTS = "{call pkg_dropship_store_camp.list_products(?,?,?,?,?)}";
	static final String DROPSHIP_STORE_CAMP_UPDATE = "{call pkg_dropship_store_camp.update_store_camp(?,?,?,?,?,?)}";

	public static Map insertStoreCamp(String storeId, String campaignId, String productId, String referenceId,
			String state, String customVariantsData, String channel) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, campaignId);
		inputParams.put(3, productId);
		inputParams.put(4, referenceId);
		inputParams.put(5, state);
		inputParams.put(6, customVariantsData);
		inputParams.put(7, channel);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.VARCHAR);
		outputParamsTypes.put(10, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(8, AppParams.RESULT_CODE);
		outputParamsNames.put(9, AppParams.RESULT_MSG);
		outputParamsNames.put(10, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_CAMP_INSERT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = format(resultDataList.get(0));
		LOGGER.fine("=> mapStoreCamp result: " + resultMap.toString());
		return resultMap;
	}

	public static Map listCampaigns(String campaignId, String title, String userId, String storeId, int page,
			int pageSize, int uploadable) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, title);
		inputParams.put(3, userId);
		inputParams.put(4, storeId);
		inputParams.put(5, page);
		inputParams.put(6, pageSize);
		inputParams.put(7, uploadable);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.VARCHAR);
		outputParamsTypes.put(10, OracleTypes.CURSOR);
		outputParamsTypes.put(11, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(8, AppParams.RESULT_CODE);
		outputParamsNames.put(9, AppParams.RESULT_MSG);
		outputParamsNames.put(10, AppParams.RESULT_DATA);
		outputParamsNames.put(11, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_CAMP_LIST_CAMPS, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
//		if (resultDataList.isEmpty()) {
//			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
//		}
		List<Map> campaignsList = new ArrayList<>();
		resultDataList.forEach(rs -> {
			try {
				campaignsList.add(formatCampaign(rs));
			} catch (ParseException e) {
				LOGGER.severe(e.getMessage());
			}
		});
		LOGGER.fine("=> listCampaigns result: " + campaignsList.toString());

		Map data = new LinkedHashMap<>();

		data.put(AppParams.TOTAL, ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL));
		data.put(AppParams.DATA, campaignsList);

		return data;
	}

	public static Map listProducts(String storeId, String campaignId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, campaignId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_CAMP_LIST_PRODUCTS,
				inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		List<Map> productsList = new ArrayList<>();
		resultDataList.forEach(rs -> {
			try {
				productsList.add(formatProduct(rs));
			} catch (ParseException e) {
				LOGGER.severe(e.getMessage());
			}
		});
		LOGGER.fine("=> listProducts result: " + productsList.toString());

		Map data = new LinkedHashMap<>();

		data.put(AppParams.DATA, productsList);

		return data;
	}

	public static Map updateState(String storeId, String campaignId, String state)
			throws SQLException, UnsupportedEncodingException, ParseException {

		LOGGER.fine("Dropship Store Campaign update with id=" + campaignId + ", state=" + state);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, campaignId);
		inputParams.put(3, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_CAMP_UPDATE,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(updateResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> Dropship Store Campaign update result: " + resultMap.toString());

		return resultMap;
	}

	private static Map format(Map data) {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.STORE_ID, ParamUtil.getString(data, AppParams.S_STORE_ID));
		el.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(data, AppParams.S_CAMPAIGN_ID));
		el.put(AppParams.REFERENCE_ID, ParamUtil.getString(data, AppParams.S_REFERENCE_ID));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.UPDATE_DATE, ParamUtil.getString(data, AppParams.D_UPDATE));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		return el;
	}

	private static Map formatCampaign(Map data) throws ParseException {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.TITLE, ParamUtil.getString(data, AppParams.S_TITLE));
		el.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(data, AppParams.S_DESIGN_FRONT_URL));
		el.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(data, AppParams.S_DESIGN_BACK_URL));
		el.put(AppParams.STORE_ID, ParamUtil.getString(data, AppParams.S_STORE_ID));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.REMAINING, CampaignUtil.getTimeRemaining());
		el.put(AppParams.UPLOADABLE, ParamUtil.getBoolean(data, AppParams.N_UPLOADABLE));
		el.put(AppParams.REFERENCE_ID, ParamUtil.getString(data, AppParams.S_REFERENCE_ID));
		el.put(AppParams.REFERENCE_URL, ParamUtil.getString(data, AppParams.S_REFERENCE_URL));
		el.put(AppParams.PRODUCT_ID, ParamUtil.getString(data, AppParams.S_PRODUCT_ID));
		el.put(AppParams.PRODUCT_NAME, ParamUtil.getString(data, AppParams.S_NAME));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		return el;
	}

	private static Map formatProduct(Map data) throws ParseException {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.TITLE, ParamUtil.getString(data, AppParams.S_TITLE));
		el.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(data, AppParams.S_DESIGN_FRONT_URL));
		el.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(data, AppParams.S_DESIGN_BACK_URL));
		el.put(AppParams.STORE_ID, ParamUtil.getString(data, AppParams.S_STORE_ID));
		el.put(AppParams.CREATE_DATE, ParamUtil.getString(data, AppParams.D_CREATE));
		el.put(AppParams.REFERENCE_ID, ParamUtil.getString(data, AppParams.S_REFERENCE_ID));
		el.put(AppParams.REFERENCE_URL, ParamUtil.getString(data, AppParams.S_REFERENCE_URL));
		el.put(AppParams.PRODUCT_ID, ParamUtil.getString(data, AppParams.S_PRODUCT_ID));
		el.put(AppParams.PRODUCT_NAME, ParamUtil.getString(data, AppParams.S_NAME));
		return el;
	}

	public static Map getUserInfo(String id) throws SQLException {

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

		return formatUserInfo(resultDataList.get(0));
	}
	
	private static Map formatUserInfo(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		resultMap.put(AppParams.EMAIL, ParamUtil.getString(queryData, AppParams.S_EMAIL));

		return resultMap;
	}
	
	public static void updateStoreSyncedByStoreId(String store_id, int n_synced) throws SQLException {

		LOGGER.fine("Dropship Store Campaign update synced with store_id=" + store_id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, store_id);
		inputParams.put(2, n_synced);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map updateResultMap = DBProcedureUtil.execute(dataSource, UPDATE_STORE_CAMP_SYNC, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}
	}
	
	private static final Logger LOGGER = Logger.getLogger(DropShipStoreCampService.class.getName());
}
