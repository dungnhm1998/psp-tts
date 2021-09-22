package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
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
public class EmailTemplateService {

    private static DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static Map search(String type, String state, boolean isGetContent) throws SQLException {

        LOGGER.fine("Email template search with type=" + type + " and state=" + state);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, type);
        inputParams.put(2, state);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(3, OracleTypes.NUMBER);
        outputParamsTypes.put(4, OracleTypes.VARCHAR);
        outputParamsTypes.put(5, OracleTypes.NUMBER);
        outputParamsTypes.put(6, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(3, AppParams.RESULT_CODE);
        outputParamsNames.put(4, AppParams.RESULT_MSG);
        outputParamsNames.put(5, AppParams.RESULT_TOTAL);
        outputParamsNames.put(6, AppParams.RESULT_DATA);
        
        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MAIL_TEMPLATE_SEARCH, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        int resultTotal = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

        List<Map> resultList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        List<Map> templateList = new ArrayList<>();

        for(Map resultMap : resultList){
            templateList.add(format(resultMap, isGetContent));
        }

        Map resultMap = new LinkedHashMap();
        resultMap.put(AppParams.TOTAL, resultTotal);
        resultMap.put(AppParams.TEMPLATES, templateList);

        LOGGER.fine("=> Email template search result: " + resultTotal);

        return resultMap;
    }

    public static Map get(String id) throws SQLException {

        LOGGER.fine("Email template lookup with id=" + id);

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

        Map lookupResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MAIL_TEMPLATE_GET, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(lookupResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(lookupResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(lookupResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            throw new BadRequestException(SystemError.INVALID_EMAIL_TEMPLATE);
        }

        LOGGER.fine("=> Email template lookup result: " + ParamUtil.getString(lookupResultMap, AppParams.RESULT_MSG));

        return format(resultDataList.get(0), true);
    }

    public static Map insert(String type, String name, String subject, String content) throws SQLException {

        LOGGER.fine("Email template insert with type=" + type + ", name=" + name + ", subject=" + subject + ", content=" + content);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, type);
        inputParams.put(2, name);
        inputParams.put(3, subject);
        inputParams.put(4, content);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(5, OracleTypes.NUMBER);
        outputParamsTypes.put(6, OracleTypes.VARCHAR);
        outputParamsTypes.put(7, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(5, AppParams.RESULT_CODE);
        outputParamsNames.put(6, AppParams.RESULT_MSG);
        outputParamsNames.put(7, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.MAIL_TEMPLATE_INSERT, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.CREATED.code()) {
            throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
        }

        LOGGER.fine("=> Email template insert result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

        return format(resultDataList.get(0), true);
    }

    private static Map format(Map queryData, boolean isGetContent) throws SQLException {

        Map resultMap = new LinkedHashMap<>();

        resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
        resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));
        resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));
        resultMap.put(AppParams.SUBJECT, ParamUtil.getString(queryData, AppParams.S_SUBJECT));
        if(isGetContent){
        	resultMap.put(AppParams.CONTENT, ParamUtil.getString(queryData, AppParams.C_CONTENT));
        }
        resultMap.put(AppParams.THUMBNAIL, ParamUtil.getString(queryData, AppParams.S_THUMBNAIL));
        resultMap.put(AppParams.COLUMN, ParamUtil.getInt(queryData, AppParams.N_COLUMN));
        resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

        return resultMap;
    }

    private static final Logger LOGGER = Logger.getLogger(EmailTemplateService.class.getName());
}
