package asia.leadsgen.psp.service_fulfill;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.google.protobuf.TextFormat.ParseException;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.MediaObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;

public class MediaService extends MasterService {
	private static final String GET_LIST_BY_USER = "{call PKG_MEDIA.get_list_by_user(?,?,?,?,?,?,?,?,?)}";
	private static final String GET_MEDIA_BY_ID = "{call PKG_MEDIA.get_media_by_id(?,?,?,?,?)}";
	private static final String CREATE_MEDIA = "{call PKG_MEDIA.create_media(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	private static final String UPDATE_MEDIA_BY_ID = "{call PKG_MEDIA.update_media_by_id(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	private static final String DELETE_MEDIA_BY_ID = "{call PKG_MEDIA.delete_media_by_id(?,?,?,?,?)}";
	private static final String FIND_BY_SKU = "call pkg_media.get_media_by_sku(?,?,?,?,?)";
	// Create List Media
	private static final String CREATE_LIST_MEDIA_F = "{call PKG_MEDIA.create_list_media(?)}";
	// Check upload file trung ten
	private static final String CHECK_NAME_IS_EXITS = "{call PKG_MEDIA.check_name_is_exits(?,?,?,?,?,?)}";

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static asia.leadsgen.psp.server.handler.dropship.order.MediaObj getMediaBySku(String user_id, String sku)
			throws SQLException {
		Map result = searchOne(FIND_BY_SKU, new Object[] { user_id, sku });
		return (result == null) ? null : asia.leadsgen.psp.server.handler.dropship.order.MediaObj.fromMap(result);
	}

	public static Map getListMediaByUserId(String user_id, int page, int page_size, String text, String type)
			throws SQLException {

		LOGGER.info("Base look up with user_id=" + user_id + " ---- page=" + page + " ---- page_size=" + page_size
				+ " ---- text=" + text + " ---- type=" + type);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, user_id);
		inputParams.put(2, page);
		inputParams.put(3, page_size);
		inputParams.put(4, text);
		inputParams.put(5, type);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_LIST_BY_USER, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
		int result_total = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultMap = new ArrayList<Map>();
		for (Map item : resultDataList) {
			resultMap.add(format(item));
		}
		Map result = new LinkedHashMap<>();
		result.put(AppParams.TOTAL, result_total);
		result.put(AppParams.DATA, resultMap);
		LOGGER.info("=> Base look up result: " + result.toString());

		return result;
	}

	public static Map getMediaById(String user_id, String id) throws SQLException {

		LOGGER.info("Base look up with user_id=" + user_id + " --- id=" + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, user_id);
		inputParams.put(2, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_MEDIA_BY_ID, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_PRODUCT);
		}

		Map resultMap = null;
		if (!resultDataList.isEmpty()) {
			resultMap = format(resultDataList.get(0));
		}
		LOGGER.info("=> Base look up result: " + resultMap.toString());
		return resultMap;
	}

	public static Map createMedia(String user_id, String type, String tags, String base_id, String url, String state,
			String name, String size, String resolution, String thumb_url, String md5) throws SQLException {

		LOGGER.info("Base look up with user_id=" + user_id + " --- type=" + type + " --- tags=" + tags + " --- base_id="
				+ base_id + " --- url=" + url + " --- state=" + state + " --- name=" + name + " --- size=" + size
				+ " --- resolution=" + resolution + " --- thumb_url=" + thumb_url + " --- md5=" + md5);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, user_id);
		inputParams.put(2, url);
		inputParams.put(3, tags);
		inputParams.put(4, type);
		inputParams.put(5, base_id);
		inputParams.put(6, state);
		inputParams.put(7, name);
		inputParams.put(8, size);
		inputParams.put(9, resolution);
		inputParams.put(10, thumb_url);
		inputParams.put(11, md5);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, CREATE_MEDIA, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_PRODUCT);
		}

		Map resultMap = null;
		if (!resultDataList.isEmpty()) {
			resultMap = format(resultDataList.get(0));
		}
		LOGGER.info("=> Base look up result: " + resultMap.toString());
		return resultMap;
	}

	public static Map updateMediaById(String user_id, String id, String type, String tags, String base_id, String url,
			String state, String name, String size, String resolution, String thumb_url, String md5)
			throws SQLException {

		LOGGER.info("Base look up with user_id=" + user_id + " --- id=" + id + " --- type=" + type + " --- tags=" + tags
				+ " --- base_id=" + base_id + " --- url=" + url + " --- state=" + state + " --- name=" + name
				+ " --- size=" + size + " --- resolution=" + resolution + " --- thumb_url=" + thumb_url + " --- md5="
				+ md5);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, user_id);
		inputParams.put(2, id);
		inputParams.put(3, url);
		inputParams.put(4, tags);
		inputParams.put(5, type);
		inputParams.put(6, base_id);
		inputParams.put(7, state);
		inputParams.put(8, name);
		inputParams.put(9, size);
		inputParams.put(10, resolution);
		inputParams.put(11, thumb_url);
		inputParams.put(12, thumb_url);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);
		outputParamsTypes.put(15, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);
		outputParamsNames.put(15, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, UPDATE_MEDIA_BY_ID, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_PRODUCT);
		}

		Map resultMap = null;
		if (!resultDataList.isEmpty()) {
			resultMap = format(resultDataList.get(0));
		}
		LOGGER.info("=> Base look up result: " + resultMap.toString());
		return resultMap;
	}

	public static Map deleteMediaById(String user_id, String id) throws SQLException {

		LOGGER.info("Base look up with user_id=" + user_id + " --- id=" + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, user_id);
		inputParams.put(2, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DELETE_MEDIA_BY_ID, inputParams, outputParamsTypes,
				outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_PRODUCT);
		}

		Map resultMap = null;
		if (!resultDataList.isEmpty()) {
			resultMap = format(resultDataList.get(0));
		}
		LOGGER.info("=> Base look up result: " + resultMap.toString());
		return resultMap;
	}

	private static Map format(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));
		resultMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_URL));
		resultMap.put(AppParams.TAGS, ParamUtil.getString(queryData, AppParams.S_TAGS));
		resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));
		resultMap.put(AppParams.BASE_ID, ParamUtil.getString(queryData, AppParams.S_BASE_ID));
		resultMap.put(AppParams.CREATE_DATE, ParamUtil.getString(queryData, AppParams.D_CREATE));
		resultMap.put(AppParams.UPDATE_DATE, ParamUtil.getString(queryData, AppParams.D_UPDATE));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));
		resultMap.put(AppParams.SIZE, ParamUtil.getString(queryData, AppParams.S_SIZE));
		resultMap.put(AppParams.RESOLUTION, ParamUtil.getString(queryData, AppParams.S_RESOLUTION));
		resultMap.put(AppParams.THUMB_URL, ParamUtil.getString(queryData, AppParams.S_THUMB_URL));
		resultMap.put(AppParams.BASE_ID, ParamUtil.getString(queryData, AppParams.S_BASE_ID));
		resultMap.put(AppParams.MD5, ParamUtil.getString(queryData, AppParams.S_MD5));

		return resultMap;
	}

	public static void createListMedia(List<MediaObj> listData) throws UnirestException, SQLException, ParseException {

		LOGGER.info("insert list: ...");

		try (Connection hikariCon = dataSource.getConnection()) {

			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				LOGGER.info("Connect success");
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				MediaObj[] arrayMedia = new MediaObj[listData.size()];
				arrayMedia = listData.toArray(arrayMedia);

				java.sql.Array array_media = con.createOracleArray("CREATE_LIST_MEDIA_T", arrayMedia);

				LOGGER.info("arrayMedia= " + arrayMedia);

				try (CallableStatement cstmt = con.prepareCall(CREATE_LIST_MEDIA_F);) {
					cstmt.setArray(1, array_media); // Set input parameter
	
					cstmt.execute();
				}
			}
		}
	}

	public static boolean checkNameMediaExists(String user_id, String type, String name) throws SQLException {

		LOGGER.info("Ckeck List Media user_id =" + user_id + " --- type =" + type + " --- name =" + name);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, user_id);
		inputParams.put(2, type);
		inputParams.put(3, name);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_TOTAL);
		Map searchResultMap = DBProcedureUtil.execute(dataSource, CHECK_NAME_IS_EXITS, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
		int result_total = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultMap = new ArrayList<Map>();
		LOGGER.info("Result " + resultMap.size());
		if (result_total > 0) {
			return true;
		}

		return false;
	}

	private static final Logger LOGGER = Logger.getLogger(MediaService.class.getName());
}