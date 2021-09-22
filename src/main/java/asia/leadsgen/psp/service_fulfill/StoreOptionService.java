package asia.leadsgen.psp.service_fulfill;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.StoreOptionObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;

public class StoreOptionService extends MasterService {

	private static final String GET_ATTRIBUTE_BY_STORE_ID = "{call PKG_FF_STORE_OPTION.get_attribute_by_store_id(?,?,?,?,?)}";
	static final String MERGE_ATTRIBUTE_STORE_OPTION = "{call PKG_FF_STORE_OPTION.merge_attribute_store_option(?)}";
	static final String MERGE_TAG_STORE_OPTION = "{call PKG_FF_STORE_OPTION.merge_tag_store_option(?)}";

	public static Map mergeAttributeV2(List<StoreOptionObj> listStoreMedia) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, java.sql.Array>();
		Map resultMap = null;
		try (Connection hikariCon = dataSource.getConnection()) {

			if (hikariCon.isWrapperFor(OracleConnection.class)) {

				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				StoreOptionObj[] arrayStoreMedia = new StoreOptionObj[listStoreMedia.size()];
				arrayStoreMedia = listStoreMedia.toArray(arrayStoreMedia);

				java.sql.Array listStoreMediaObj = con.createOracleArray("STORE_OPTION_T", arrayStoreMedia);
				try (CallableStatement cstmt = con.prepareCall(MERGE_ATTRIBUTE_STORE_OPTION);) {
					cstmt.setArray(1, listStoreMediaObj);
					cstmt.execute();
				}
			}
		}
		return resultMap;
	}

	public static Map mergeTag(List<StoreOptionObj> listStoreMedia) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, java.sql.Array>();
		Map resultMap = null;
		try (Connection hikariCon = dataSource.getConnection()) {
			if (hikariCon.isWrapperFor(OracleConnection.class)) {

				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				StoreOptionObj[] arrayStoreMedia = new StoreOptionObj[listStoreMedia.size()];
				arrayStoreMedia = listStoreMedia.toArray(arrayStoreMedia);

				java.sql.Array listStoreMediaObj = con.createOracleArray("STORE_OPTION_T", arrayStoreMedia);
				try (CallableStatement cstmt = con.prepareCall(MERGE_TAG_STORE_OPTION);) {
					cstmt.setArray(1, listStoreMediaObj);
					cstmt.execute();
				}
			}
		}
		return resultMap;
	}

	public static List<Map> lookUp(String storeId, String type) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, type);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_ATTRIBUTE_BY_STORE_ID, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		LOGGER.info("=> Get attribute by store id result: " + resultDataList.toString());

		return resultDataList;
	}

	private static StoreOptionObj format(Map resultItem) throws SQLException {

		StoreOptionObj storeOption = new StoreOptionObj();

		String id = ParamUtil.getString(resultItem, AppParams.S_ID);
		String storeId = ParamUtil.getString(resultItem, AppParams.S_STORE_ID);
		String type = ParamUtil.getString(resultItem, AppParams.S_TYPE);
		String attributeId = ParamUtil.getString(resultItem, AppParams.S_OPTION_ID);
		String attributeName = ParamUtil.getString(resultItem, AppParams.S_OPTION_NAME);
		String option = ParamUtil.getString(resultItem, AppParams.S_BGP_OPTION);
		String term = ParamUtil.getString(resultItem, AppParams.S_TERMS);
		String state = ParamUtil.getString(resultItem, AppParams.S_STATE);

		storeOption.setS_id(id);
		storeOption.setS_store_id(storeId);
		storeOption.setS_type(type);
		storeOption.setS_option_id(attributeId);
		storeOption.setS_option_name(attributeName);
		storeOption.setS_bgp_option(option);
		storeOption.setS_terms(term);
		storeOption.setS_state(state);

		return storeOption;
	}

//	public static List<StoreOptionObj> lookUp(String storeId, String type) throws SQLException {
//		List<StoreOptionObj> result = new ArrayList<>();
//		List<Map> resultList = searchAll(GET_ATTRIBUTE_BY_STORE_ID, new Object[] { storeId, type });
//		if (resultList.size() > 0) {
//			resultList.forEach(resultMap -> {
//				try {
//					result.add(format(resultMap));
//				} catch (SQLException e) {
//					e.printStackTrace();
//				}
//			});
//		}
//		logger.info("=> result store option lookup: " + result.toString());
//		return result;
//	}

	private static final Logger LOGGER = Logger.getLogger(StoreOptionService.class.getName());
}
