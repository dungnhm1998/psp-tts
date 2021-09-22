package asia.leadsgen.psp.service_fulfill;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class UploadFileService {
	public static final String FILE_UPLOAD_INSERT = "{call PKG_DROPSHIP_IMPORT_FILE.file_upload_insert(?,?,?,?,?,?,?,?,?)}";
    public static final String DROPSHIP_IMPORT_FILE_SEARCH = "{call pkg_dropship_import_file.get_list_dropship_import_file(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
    public static final String DROPSHIP_IMPORT_FILE_ROW_SEARCH = "{call pkg_dropship_import_file_rows.get_list_dropship_import_file_rows(?,?,?,?,?,?,?,?,?,?)}";
	private static DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public static Map insert(String fileName, String fileURL, String fileType, String fileUserId, String fileStoreId, String source) throws SQLException {

        LOGGER.info("data insert into database " + fileName + fileURL + fileType +  fileUserId + fileStoreId + source);
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, fileName);
        inputParams.put(2, fileURL);
        inputParams.put(3, fileType);
        inputParams.put(4, fileUserId);
        inputParams.put(5, fileStoreId);
        inputParams.put(6, source);


        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(7, OracleTypes.NUMBER);
        outputParamsTypes.put(8, OracleTypes.VARCHAR);
        outputParamsTypes.put(9, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(7, AppParams.RESULT_CODE);
        outputParamsNames.put(8, AppParams.RESULT_MSG);
        outputParamsNames.put(9, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, FILE_UPLOAD_INSERT, inputParams,
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

        LOGGER.fine("=> Dropship import file insert result: " + resultMap.toString());

        return resultMap;

    }

    public static Map getDropshipImportFile(String userId, String storeId, String state, String type, String source, String fileName, int page, int pageSize,
    		String startDate, String endDate, String sort, String dir)
            throws SQLException, ParseException {

        LOGGER.info("--------------------Start get list file csv---------------------------");
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, userId);
        inputParams.put(2, storeId);
        inputParams.put(3, state);
        inputParams.put(4, type);
        inputParams.put(5, source);
        inputParams.put(6, fileName);
        inputParams.put(7, page);
        inputParams.put(8, pageSize);
        inputParams.put(9, startDate);
		inputParams.put(10, endDate);
		inputParams.put(11, sort);
		inputParams.put(12, dir);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(13, OracleTypes.NUMBER);
        outputParamsTypes.put(14, OracleTypes.VARCHAR);
        outputParamsTypes.put(15, OracleTypes.CURSOR);
        outputParamsTypes.put(16, OracleTypes.NUMBER);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(13, AppParams.RESULT_CODE);
        outputParamsNames.put(14, AppParams.RESULT_MSG);
        outputParamsNames.put(15, AppParams.RESULT_DATA);
        outputParamsNames.put(16, AppParams.RESULT_TOTAL);

        Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_IMPORT_FILE_SEARCH, inputParams,
                outputParamsTypes, outputParamsNames);
        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }
        List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

        int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);

        List<Map> formatList = new ArrayList<>();
        for (Map resultDataItem : resultDataList) {
            formatList.add(format(resultDataItem));
        }

        resultMap = new LinkedHashMap<>();
        resultMap.put(AppParams.TOTAL, resultTotal);
        resultMap.put(AppParams.DATA, formatList);

        LOGGER.info("--------------------End get list file csv---------------------------");
        return resultMap;
    }


    public static Map getDropshipImportFileRow(String userId, String fileId, String text, String state, int page, int pageSize)
            throws SQLException, ParseException {

        LOGGER.info("--------------------Start get list file rows csv---------------------------");
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, userId);
        inputParams.put(2, fileId);
        inputParams.put(3, text);
        inputParams.put(4, state);
        inputParams.put(5, page);
        inputParams.put(6, pageSize);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(7, OracleTypes.NUMBER);
        outputParamsTypes.put(8, OracleTypes.VARCHAR);
        outputParamsTypes.put(9, OracleTypes.CURSOR);
        outputParamsTypes.put(10, OracleTypes.NUMBER);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(7, AppParams.RESULT_CODE);
        outputParamsNames.put(8, AppParams.RESULT_MSG);
        outputParamsNames.put(9, AppParams.RESULT_DATA);
        outputParamsNames.put(10, AppParams.RESULT_TOTAL);

        Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_IMPORT_FILE_ROW_SEARCH, inputParams,
                outputParamsTypes, outputParamsNames);
        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }
        List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

        int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);

        List<Map> formatList = new ArrayList<>();
        for (Map resultDataItem : resultDataList) {
            formatList.add(formatFileRows(resultDataItem));
        }

        resultMap = new LinkedHashMap<>();
        resultMap.put(AppParams.TOTAL, resultTotal);
        resultMap.put(AppParams.DATA, formatList);

        LOGGER.info("--------------------End get list file rows csv---------------------------");

        return resultMap;
    }

    private static Map format(Map queryData) throws SQLException {

        Map resultMap = new LinkedHashMap<>();

        resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

        resultMap.put(AppParams.FILE_NAME, ParamUtil.getString(queryData, AppParams.S_FILE_NAME));

        resultMap.put(AppParams.URL, ParamUtil.getString(queryData, AppParams.S_URL));

        resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));

        resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));

        resultMap.put(AppParams.STORE_ID, ParamUtil.getString(queryData, AppParams.S_STORE_ID));

        resultMap.put(AppParams.CREATE_TIME, ParamUtil.getString(queryData, AppParams.D_CREATE));

        resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

        resultMap.put("error_msg", ParamUtil.getString(queryData, "S_ERROR_MSG"));

        resultMap.put(AppParams.SOURCE, ParamUtil.getString(queryData, AppParams.S_SOURCE));


        return resultMap;
    }


    private static Map formatFileRows(Map queryData) throws SQLException {

        Map resultMap = new LinkedHashMap<>();

        resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

        resultMap.put(AppParams.FILE_NAME, ParamUtil.getString(queryData, AppParams.S_FILE_NAME));

        resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));

        resultMap.put(AppParams.STORE_ID, ParamUtil.getString(queryData, AppParams.S_STORE_ID));

        resultMap.put(AppParams.EMAIL, ParamUtil.getString(queryData, AppParams.S_EMAIL));

        resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

        resultMap.put(AppParams.ORDER_ID, ParamUtil.getString(queryData, AppParams.S_ORDER_ID));

        resultMap.put(AppParams.REFERENCE, ParamUtil.getString(queryData, AppParams.S_REFERENCE_ORDER));

        resultMap.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_FRONT_URL));

        resultMap.put(AppParams.DESIGN_BACK_URL, ParamUtil.getString(queryData, AppParams.S_DESIGN_BACK_URL));

        resultMap.put(AppParams.MOCKUP_FRONT_URL, ParamUtil.getString(queryData, AppParams.S_MOCKUP_FRONT_URL));

        resultMap.put(AppParams.MOCKUP_BACK_URL, ParamUtil.getString(queryData, AppParams.S_MOCKUP_BACK_URL));

        resultMap.put(AppParams.LINEITEM_NAME, ParamUtil.getString(queryData, AppParams.S_LINEITEM_NAME));

        resultMap.put(AppParams.LINEITEM_SKU, ParamUtil.getString(queryData, AppParams.S_LINEITEM_SKU));

        resultMap.put(AppParams.CREATE_DATE, ParamUtil.getString(queryData, AppParams.D_UPDATE));

        resultMap.put(AppParams.LINEITEM_QUANTITY, ParamUtil.getString(queryData, AppParams.S_LINEITEM_QUANTITY));

        resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));

        resultMap.put(AppParams.ERROR_NOTE, ParamUtil.getString(queryData, AppParams.S_ERROR_NOTE));
        resultMap.put(AppParams.REPROCESS, ParamUtil.getInt(queryData, AppParams.N_REPROCESS));

        return resultMap;
    }
    private static final Logger LOGGER = Logger.getLogger(UploadFileService.class.getName());
}