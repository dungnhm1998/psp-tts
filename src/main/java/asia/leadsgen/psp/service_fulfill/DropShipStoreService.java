package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.DropshipStoreObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class DropShipStoreService extends MasterService{

	static final String DROPSHIP_STORE_CHECK_DUPLICATE_STORE = "{call pkg_dropship_store.check_duplicate_store(?,?,?,?,?,?)}";
	static final String DROPSHIP_STORE_CHECK_DUPLICATE_API_KEY = "{call pkg_dropship_store.check_duplicate_api_key(?,?,?,?)}";
	static final String DROPSHIP_STORE_FIND_BY_API_KEY = "{call pkg_dropship_store.find_by_api_key(?,?,?,?)}";
	static final String DROPSHIP_STORE_INSERT_SHOPIFY_APP_STORE = "{call pkg_dropship_store.insert_shopify_app_store(?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_STORE_CHECK_USER_AND_STORE_ID_IN_SHOPIFY_APP = "{call pkg_dropship_store.check_user_and_store_id_in_shopify_app(?,?,?,?,?)}";
	static final String DROPSHIP_STORE_MATCH_SHOPIFY_APP_STORE = "{call pkg_dropship_store.match_shopify_app_store(?,?,?,?,?,?)}";
	static final String DROPSHIP_STORE_UPDATE_CURRENCY = "{call pkg_dropship_store.update_shopify_store_currency(?,?,?,?,?)}";

	static final String FF_DROPSHIP_STORE_LOOKUP = "{call PKG_FF_DROPSHIP_STORE.get_store(?,?,?,?)}";
	static final String FF_DROPSHIP_STORE_GET = "{call PKG_FF_DROPSHIP_STORE.get_stores_v2(?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String FF_DROPSHIP_STORE_GET_BY_ID_AND_STATE = "{call PKG_FF_DROPSHIP_STORE.get_store_by_id_and_state(?,?,?,?,?)}";
	static final String FF_DROPSHIP_STATE_AND_NCONNECT_BY_ID = "{call PKG_FF_DROPSHIP_STORE.update_state_and_nconnected(?,?,?,?,?,?)}";

	static final String DROPSHIP_STORE_LOOKUP_BY_DOMAIN = "{call pkg_dropship_store.find_store_by_domain(?,?,?,?)}";
	static final String DROPSHIP_STORE_UPDATE_AUTO_FULFILL = "{call pkg_dropship_store.update_auto_fulfill(?,?,?,?,?)}";
	static final String DROPSHIP_STORE_FIND = "{call pkg_dropship_store.find_store(?,?,?,?,?)}";
	static final String DROPSHIP_STORE_INSERT = "{call pkg_dropship_store.insert_store(?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_WOO_STORE_INSERT = "{call pkg_dropship_store.insert_woo_store(?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_STORE_UPDATE = "{call pkg_dropship_store.update_store(?,?,?,?,?,?,?,?,?,?)}";
	static final String DROPSHIP_WOO_STORE_UPDATE = "{call pkg_dropship_store.update_woo_store(?,?,?,?,?,?,?)}";
	static final String DROPSHIP_STORE_UPDATE_STATE = "{call pkg_dropship_store.update_state(?,?,?,?,?)}";
	static final String DROPSHIP_STORE_UPDATE_TOKEN = "{call pkg_dropship_store.update_store_token(?,?,?,?,?)}";
	
	static final String FF_GET_DROPSHIP_STORE_APPROVED_AND_DISCONNECTED = "{call PKG_FF_DROPSHIP_STORE.get_store_approved_and_disconnected_by_id(?,?,?,?)}";

	static final String INSERT_ETSY_STORE = "{call pkg_dropship_store.insert_etsy_store(?,?,?,?,?,?,?,?,?)}";
	

	public static Map lookUp(String storeId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, FF_DROPSHIP_STORE_LOOKUP, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		resultMap = format(resultDataList.get(0));
//		LOGGER.fine("=> getStores result: " + resultMap.toString());
		return resultMap;
	}

	public static Map lookUpByDomain(String domain) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domain);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_LOOKUP_BY_DOMAIN, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		resultMap = format(resultDataList.get(0));
//		LOGGER.fine("=> getStores result: " + resultMap.toString());
		return resultMap;
	}

	public static Map updateAutoFulfill(String storeId, boolean autofulfill) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, autofulfill == true ? 1 : 0);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_UPDATE_AUTO_FULFILL, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		resultMap = format(resultDataList.get(0));
		LOGGER.fine("=> updateAutoFulfill result: " + resultMap.toString());
		return resultMap;
	}

	public static Map searchStores(String userId, String channel, String storeIds , String state, String clientId , String search, int page, int pageSize) throws SQLException {

		LOGGER.info("search stores userId =" + userId + ", channel=" + channel + ", storeId= " + storeIds  + ", state=" + state + ", clientId=" + clientId + ", search=" 
				+ search + ", page=" + page + ", pageSize=" + pageSize);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, channel);
		inputParams.put(3, storeIds);
		inputParams.put(4, state);
		inputParams.put(5, clientId);
		inputParams.put(6, search);
		inputParams.put(7, page);
		inputParams.put(8, pageSize);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(9, OracleTypes.NUMBER);
		outputParamsTypes.put(10, OracleTypes.VARCHAR);
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(9, AppParams.RESULT_CODE);
		outputParamsNames.put(10, AppParams.RESULT_MSG);
		outputParamsNames.put(11, AppParams.RESULT_TOTAL);
		outputParamsNames.put(12, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, FF_DROPSHIP_STORE_GET, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		Map response = new HashMap<>();
		List<Map> responseListMap = new ArrayList<>();
		resultDataList.forEach(rs -> {
			responseListMap.add(format(rs));
		});
		
		response.put(AppParams.TOTAL, ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL));
		response.put(AppParams.DATA, responseListMap);

		return response;
	}

	public static Map find(String userId, String domain) throws SQLException {

		LOGGER.info("Find store with userId =" + userId + ", domain=" + domain);

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

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_FIND, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		Map response = new HashMap<>();

		if (!resultDataList.isEmpty()) {
			response = format(resultDataList.get(0));
		}
		return response;
	}

	public static Map addStore(String userId, String channel, String name, String domain, String apikey, String apipass, String sharedSecret, String locationId, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, channel);
		inputParams.put(3, name);
		inputParams.put(4, domain);
		inputParams.put(5, apikey);
		inputParams.put(6, apipass);
		inputParams.put(7, sharedSecret);
		inputParams.put(8, locationId);
		inputParams.put(9, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.VARCHAR);
		outputParamsTypes.put(12, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(10, AppParams.RESULT_CODE);
		outputParamsNames.put(11, AppParams.RESULT_MSG);
		outputParamsNames.put(12, AppParams.RESULT_DATA);

		Map insertResult = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_INSERT, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResult, AppParams.RESULT_CODE);

		if (resultCode != 200) {
			throw new OracleException(ParamUtil.getString(insertResult, AppParams.RESULT_MSG));
		}

		List<Map> listData = ParamUtil.getListData(insertResult, AppParams.RESULT_DATA);

		if (listData.isEmpty()) {
			throw new OracleException(ParamUtil.getString(insertResult, AppParams.RESULT_MSG));
		}

		return format(listData.get(0));

	}

	public static Map addWooStore(String userId, String channel, String name, String domain, String locationId, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, channel);
		inputParams.put(3, name);
		inputParams.put(4, domain);
		inputParams.put(5, locationId);
		inputParams.put(6, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.VARCHAR);
		outputParamsTypes.put(9, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(7, AppParams.RESULT_CODE);
		outputParamsNames.put(8, AppParams.RESULT_MSG);
		outputParamsNames.put(9, AppParams.RESULT_DATA);

		Map insertResult = DBProcedureUtil.execute(dataSource, DROPSHIP_WOO_STORE_INSERT, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResult, AppParams.RESULT_CODE);

		if (resultCode == 200) {
			List<Map> listData = ParamUtil.getListData(insertResult, AppParams.RESULT_DATA);

			if (listData.isEmpty()) {
				throw new OracleException(ParamUtil.getString(insertResult, AppParams.RESULT_MSG));
			}

			return format(listData.get(0));

		}
		return insertResult;
	}

	public static Map update(String storeId, String name, String domain, String apiKey, String apiPassword, String sharedSecret, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, name);
		inputParams.put(3, domain);
		inputParams.put(4, apiKey);
		inputParams.put(5, apiPassword);
		inputParams.put(6, sharedSecret);
		inputParams.put(7, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.VARCHAR);
		outputParamsTypes.put(10, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(8, AppParams.RESULT_CODE);
		outputParamsNames.put(9, AppParams.RESULT_MSG);
		outputParamsNames.put(10, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_UPDATE, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = format(resultDataList.get(0));
		LOGGER.fine("=> update result: " + resultMap.toString());
		return resultMap;
	}

	public static boolean wooUpdate(String storeId, String apiKey, String sharedSecret, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, apiKey);
		inputParams.put(3, sharedSecret);
		inputParams.put(4, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_WOO_STORE_UPDATE, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> update result: " + resultMap.toString());

		return resultCode == HttpResponseStatus.OK.code();
	}

	public static Map updateStoreState(String storeId, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_UPDATE_STATE, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_STORE_ID);
		}

		resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> updateStoreState result: " + resultMap.toString());
		return resultMap;
	}

	private static Map format(Map store) {

		Map dsStore = new LinkedHashMap<>();

		String id = ParamUtil.getString(store, AppParams.S_ID);
		String channel = ParamUtil.getString(store, AppParams.S_CHANNEL);
		String apiKey = ParamUtil.getString(store, AppParams.S_API_KEY);
		String secret = ParamUtil.getString(store, AppParams.S_SHARED_SECRET);
		String domain = ParamUtil.getString(store, AppParams.S_DOMAIN);
		String state = ParamUtil.getString(store, AppParams.S_STATE);
		String createDate = ParamUtil.getString(store, AppParams.D_CREATE);
		String upadateDate = ParamUtil.getString(store, AppParams.D_UPDATE);
		String name = ParamUtil.getString(store, AppParams.S_NAME);
		String userId = ParamUtil.getString(store, AppParams.S_USER_ID);
		int nConnected = ParamUtil.getInt(store, AppParams.N_CONNECTED);
		int nAutoFulfill = ParamUtil.getInt(store, AppParams.N_AUTO_FULFILL);
		String currency = ParamUtil.getString(store, AppParams.S_CURRENCY);

		dsStore.put(AppParams.ID, id);
		dsStore.put(AppParams.NAME, name);
		dsStore.put(AppParams.CHANNEL, channel);
		dsStore.put(AppParams.API_KEY, apiKey);
		dsStore.put(AppParams.SECRET, secret);
		dsStore.put(AppParams.DOMAIN, domain);
		dsStore.put(AppParams.STATE, state);
		dsStore.put(AppParams.USER_ID, userId);
		dsStore.put(AppParams.CONNECTED, nConnected);
		dsStore.put(AppParams.AUTO_FULFILL, nAutoFulfill);
		dsStore.put(AppParams.CREATE_DATE, createDate);
		dsStore.put(AppParams.UPDATE_DATE, upadateDate);
		dsStore.put(AppParams.CURRENCY, currency);

		return dsStore;
	}

	public static Map update(String storeId, String apiKey) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, apiKey);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_UPDATE_TOKEN, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		resultMap = format(resultDataList.get(0));
		LOGGER.fine("=> update result: " + resultMap.toString());
		return resultMap;
	}

	public static boolean isDuplicateStore(String userId, String channel, String name) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, channel);
		inputParams.put(3, name);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_CHECK_DUPLICATE_STORE, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		return ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL, 0) > 0;
	}

	public static boolean isExistThisApiKey(String apikey) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, apikey);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_CHECK_DUPLICATE_API_KEY, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		return ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL, 0) > 0;
	}

	public static DropshipStoreObj findByApiKey(String apikey) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, apikey);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_FIND_BY_API_KEY, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		return CollectionUtils.isNotEmpty(resultDataList) ? DropshipStoreObj.fromMap(resultDataList.get(0)) : null;
	}

	public static Map addShopifyAppStore(String clientId, String channel, String name, String domain, String apikey, String apipass, String sharedSecret, String locationId, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, clientId);
		inputParams.put(2, channel);
		inputParams.put(3, name);
		inputParams.put(4, domain);
		inputParams.put(5, apikey);
		inputParams.put(6, apipass);
		inputParams.put(7, sharedSecret);
		inputParams.put(8, locationId);
		inputParams.put(9, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.VARCHAR);
		outputParamsTypes.put(12, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(10, AppParams.RESULT_CODE);
		outputParamsNames.put(11, AppParams.RESULT_MSG);
		outputParamsNames.put(12, AppParams.RESULT_DATA);

		Map insertResult = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_INSERT_SHOPIFY_APP_STORE, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResult, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResult, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResult, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(insertResult, AppParams.RESULT_MSG));
		}

		Map resultMap = format(resultDataList.get(0));
		return resultMap;
	}

	public static List<Map> checkUserAndStoreNameInShopifyApp(String clientId, String name) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, clientId);
		inputParams.put(2, name);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map result = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_CHECK_USER_AND_STORE_ID_IN_SHOPIFY_APP, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(result, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(result, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(result, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		List<Map> responseListMap = new ArrayList<>();
		for (Map rs : resultDataList) {
			responseListMap.add(format(rs));
		}

		return responseListMap;
	}

	public static Map matchShopifyAppStoreId(String userId, String clientId, String storeName) throws SQLException {

		LOGGER.info("matchShopifyAppStoreId: userId= " + userId + ", storeName= " + storeName);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, clientId);
		inputParams.put(3, storeName);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_MATCH_SHOPIFY_APP_STORE, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		Map result = format(resultDataList.get(0));
		LOGGER.fine("=> update result: " + result.toString());
		return result;
	}

	public static Map updateStoreCurrency(String storeId, String currency) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, currency);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_STORE_UPDATE_CURRENCY, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		Map result = format(resultDataList.get(0));
		LOGGER.fine("=> update result: " + result.toString());
		return result;
	}
	
	public static Map getStoreByIdAndState(String storeId,String state) throws SQLException {
		Object[] args = new Object[] {storeId,state};
		Map resultMap = searchOne(FF_DROPSHIP_STORE_GET_BY_ID_AND_STATE, args);
		if (resultMap != null) {
			Map result = format(resultMap);
			return result;
		}
		return null;
	}
	
	public static void updateStateAndNConnect(String storeId , String state, int nConnect) throws SQLException {
		Object[] args = new Object[] {storeId,state, nConnect};
		List<Map> resultData = update(FF_DROPSHIP_STATE_AND_NCONNECT_BY_ID, args);
		
	}
	
	public static Map getStoreApprovedAndDisconnectedById(String storeId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, FF_GET_DROPSHIP_STORE_APPROVED_AND_DISCONNECTED, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		resultMap = format(resultDataList.get(0));
		return resultMap;
	}

	public static Map createEtsyStore(String userId, String storeName, String token,
									  String tokenSecret, String storeId, String shopEmail) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, storeName);
		inputParams.put(3, token);
		inputParams.put(4, tokenSecret);
		inputParams.put(5, storeId);
		inputParams.put(6, shopEmail);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(7, OracleTypes.NUMBER);
		outputParamsTypes.put(8, OracleTypes.VARCHAR);
		outputParamsTypes.put(9, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(7, AppParams.RESULT_CODE);
		outputParamsNames.put(8, AppParams.RESULT_MSG);
		outputParamsNames.put(9, AppParams.RESULT_DATA);

		Map insertResult = DBProcedureUtil.execute(dataSource, INSERT_ETSY_STORE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResult, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResult, AppParams.RESULT_MSG));
		}

		List resultData = ParamUtil.getListData(insertResult, AppParams.RESULT_DATA);
		String status = ParamUtil.getString((Map) resultData.get(0), AppParams.STATUS.toUpperCase());
		Map result = new LinkedHashMap();
		result.put(AppParams.STATUS, status);
		return result;
	}
	
	private static final Logger LOGGER = Logger.getLogger(DropShipStoreService.class.getName());
}
