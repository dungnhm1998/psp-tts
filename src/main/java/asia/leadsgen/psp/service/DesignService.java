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
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class DesignService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	private static final String ADD_CUSTOM_TEXTS = "{call PKG_DESIGN.add_design_custom_texts(?,?,?,?)}";

	public static Map get(String id) throws SQLException {

		LOGGER.fine("Design look up with ID=" + id);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DESIGN_GET, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		Map resultMap = format(resultDataList.get(0));

		LOGGER.fine("=> Design look up result: " + resultMap.toString());

		return resultMap;
	}

	/**
	 * 
	 * @param type
	 * @param imageId
	 * @param imagePosition
	 * @param artId
	 * @param artPosition
	 * @param textId
	 * @param textPosition
	 * @param price
	 * @param priceType
	 * @return
	 * @throws SQLException
	 */
	public static Map insert(String type, String imageId, int imagePosition, String artId, int artPosition,
			String textId, int textPosition, String price, String priceType) throws SQLException {

		LOGGER.fine("Design insert with type=" + type + ", imageId=" + imageId + ", artId=" + artId + ", textId="
				+ textId + ", price=" + price + ", priceType=" + priceType);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, type);
		inputParams.put(2, imageId);
		inputParams.put(3, imagePosition);
		inputParams.put(4, artId);
		inputParams.put(5, artPosition);
		inputParams.put(6, textId);
		inputParams.put(7, textPosition);
		inputParams.put(8, price);
		inputParams.put(9, priceType);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.VARCHAR);
		outputParamsTypes.put(12, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(10, AppParams.RESULT_CODE);
		outputParamsNames.put(11, AppParams.RESULT_MSG);
		outputParamsNames.put(12, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DESIGN_INSERT, inputParams,
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

		LOGGER.fine("=> Design insert result: " + resultMap.toString());

		return resultMap;
	}

	public static void delete(String id) throws SQLException {

		LOGGER.fine("Delete design: " + id);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);

		Map deleteResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DESIGN_DELETE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
		}

		LOGGER.fine("=> Design delete result: " + resultCode);
	}

	public static String alloverInsert(String newProductId, String designType, String newImageId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, newProductId);
		inputParams.put(2, designType);
		inputParams.put(3, newImageId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ALL_OVER_INSERT_DESIGN, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
		
		return ParamUtil.getString(resultDataList.get(0), AppParams.S_ID);
	}

	public static void alloverUpdate(String designId, String imageId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, designId);
		inputParams.put(2, imageId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.ALL_OVER_UPDATE_DESIGN, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
	}

	private static Map format(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
		resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));
		resultMap.put(AppParams.ART_ID, ParamUtil.getString(queryData, AppParams.S_ART_ID));
		resultMap.put(AppParams.ART_PRICE_TYPE, ParamUtil.getString(queryData, AppParams.S_ART_PRICE_TYPE));
		resultMap.put(AppParams.ART_PRICE, ParamUtil.getString(queryData, AppParams.S_ART_PRICE));
		resultMap.put(AppParams.IMAGE_ID, ParamUtil.getString(queryData, AppParams.S_IMAGE_ID));

		if (!ParamUtil.getString(queryData, AppParams.S_IMAGE_ID).isEmpty()) {

			Map imageInfoMap = new LinkedHashMap();

			imageInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_IMAGE_ID));
			imageInfoMap.put(AppParams.POSITION, ParamUtil.getString(queryData, AppParams.N_IMAGE_POSITION));
			imageInfoMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_URL));
			imageInfoMap.put(AppParams.THUMB_URL, ParamUtil.getString(queryData, AppParams.S_PREVIEW));
			imageInfoMap.put(AppParams.WIDTH, ParamUtil.getString(queryData, AppParams.S_WIDTH));
			imageInfoMap.put(AppParams.HEIGHT, ParamUtil.getString(queryData, AppParams.S_HEIGHT));
			imageInfoMap.put(AppParams.CROP_GEOMETRY, ParamUtil.getString(queryData, AppParams.S_CROP_GEOMETRY));
			imageInfoMap.put(AppParams.PRINTABLE_TOP, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_TOP));
			imageInfoMap.put(AppParams.PRINTABLE_LEFT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_LEFT));
			imageInfoMap.put(AppParams.PRINTABLE_WIDTH, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_WIDTH));
			imageInfoMap.put(AppParams.PRINTABLE_HEIGHT, ParamUtil.getString(queryData, AppParams.S_PRINTABLE_HEIGHT));

			resultMap.put(AppParams.IMAGE, imageInfoMap);
		}

		return resultMap;
	}
	
	public static void addCustomTexts(String designId, String customTexts) throws SQLException{
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, designId);
		inputParams.put(2, customTexts);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		
		Map updateResultMap = DBProcedureUtil.execute(dataSource, ADD_CUSTOM_TEXTS, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}
	}

	private static final Logger LOGGER = Logger.getLogger(DesignService.class.getName());

}
