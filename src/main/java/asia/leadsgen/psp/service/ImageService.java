package asia.leadsgen.psp.service;

import java.sql.SQLException;
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
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.obj.Image;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class ImageService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public static Image getImage(String id) throws SQLException {
		Map imageMap = null;
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

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.IMAGE_GET, inputParams,
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
		return Image.fromMap(resultDataList.get(0));
	}
	
	public static Map get(String id) throws SQLException {

		LOGGER.fine("Image look up with id=" + id);

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

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.IMAGE_GET, inputParams,
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

		LOGGER.fine("=> Image look up result: " + resultMap.toString());

		return resultMap;
	}

	public static Map insert(String type, String name, String desc, String url, String width, String height,
			String printableTop, String printableLeft, String printableWidth, String printableHeight, String thumbUrl,
			String colors, int totalColors, int dpi) throws SQLException {

		LOGGER.fine("Image insert with type=" + type + ", name=" + name + ", width=" + width + ", height=" + height
				+ ", thumbUrl=" + thumbUrl + ", dpi= " + dpi);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, type);
		inputParams.put(2, name);
		inputParams.put(3, desc);
		inputParams.put(4, url);
		inputParams.put(5, width);
		inputParams.put(6, height);
		inputParams.put(7, printableTop);
		inputParams.put(8, printableLeft);
		inputParams.put(9, printableWidth);
		inputParams.put(10, printableHeight);
		inputParams.put(11, thumbUrl);
		inputParams.put(12, colors);
		inputParams.put(13, totalColors);
		inputParams.put(14, dpi);
		inputParams.put(15, ResourceStates.APPROVED);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(16, OracleTypes.NUMBER);
		outputParamsTypes.put(17, OracleTypes.VARCHAR);
		outputParamsTypes.put(18, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(16, AppParams.RESULT_CODE);
		outputParamsNames.put(17, AppParams.RESULT_MSG);
		outputParamsNames.put(18, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.IMAGE_INSERT, inputParams,
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

		LOGGER.fine("=> Image insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Map update(String id, String width, String height, String printableTop, String printableLeft,
			String printableWidth, String printableHeight, String ppi, String cropGeometry, String zIndex) throws SQLException {

		LOGGER.fine("Image update with id=" + id + ", width=" + width + ", height=" + height + ", printableTop=" + printableTop
				+ ", printableLeft=" + printableLeft + ", printableWidth=" + printableWidth + ", printableHeight=" + printableHeight
				+ ", ppi=" + ppi + ", cropGeometry=" + cropGeometry + ", zIndex=" + zIndex);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, width);
		inputParams.put(3, height);
		inputParams.put(4, printableTop);
		inputParams.put(5, printableLeft);
		inputParams.put(6, printableWidth);
		inputParams.put(7, printableHeight);
		inputParams.put(8, ppi);
		inputParams.put(9, cropGeometry);
		inputParams.put(10, zIndex);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.VARCHAR);
		outputParamsTypes.put(13, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(11, AppParams.RESULT_CODE);
		outputParamsNames.put(12, AppParams.RESULT_MSG);
		outputParamsNames.put(13, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.IMAGE_UPDATE, inputParams,
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

		LOGGER.fine("=> Image update result: " + resultMap.toString());

		return resultMap;
	}

	public static void delete(String id) throws SQLException {

		LOGGER.fine("Delete image: " + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.IMAGE_DELETE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Image delete result: " + resultCode);
	}

	public static void alloverUpdate(String imageId, String imgurl, String width, String height, String dpi)
			throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, imageId);
		inputParams.put(2, imgurl);
		inputParams.put(3, width);
		inputParams.put(4, height);
		inputParams.put(5, dpi);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.IMAGE_ALLOVER_UPDATE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}
	}

	private static Map format(Map queryData) {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));
		resultMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_URL));
		resultMap.put(AppParams.WIDTH, ParamUtil.getString(queryData, AppParams.S_WIDTH));
		resultMap.put(AppParams.HEIGHT, ParamUtil.getString(queryData, AppParams.S_HEIGHT));
		resultMap.put(AppParams.CROP_GEOMETRY, ParamUtil.getString(queryData, AppParams.S_CROP_GEOMETRY));
		resultMap.put(AppParams.PRINTABLE_TOP, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_TOP));
		resultMap.put(AppParams.PRINTABLE_LEFT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_LEFT));
		resultMap.put(AppParams.PRINTABLE_WIDTH, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_WIDTH));
		resultMap.put(AppParams.PRINTABLE_HEIGHT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_HEIGHT));
		resultMap.put(AppParams.THUMB_URL, ParamUtil.getString(queryData, AppParams.S_PREVIEW));
		resultMap.put(AppParams.ZINDEX, ParamUtil.getString(queryData, AppParams.S_Z_INDEX));

		return resultMap;
	}

	private static final Logger LOGGER = Logger.getLogger(ImageService.class.getName());

}
