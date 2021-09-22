package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.AuthorizationException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class ClientService {

    private static DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static Map get(String id, boolean getKey) throws SQLException {

        LOGGER.fine("API Client get with id=" + id);

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

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.CLIENT_GET, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new AuthorizationException(SystemError.INVALID_CLIENT);
        }

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        if(resultDataList.isEmpty()){
            throw new AuthorizationException(SystemError.INVALID_CLIENT);
        }

        LOGGER.fine("=> Client get result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

        return format(resultDataList.get(0), getKey);
    }

    public static String getKey(String id) throws SQLException {
        return ParamUtil.getString(get(id, true), AppParams.KEY);
    }

    private static Map format(Map queryData, boolean getKey) {

        Map resultMap = new LinkedHashMap<>();

        resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

        resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

        if(getKey) {
            resultMap.put(AppParams.KEY, ParamUtil.getString(queryData, AppParams.S_KEY));
        }

        resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

        resultMap.put(AppParams.AUTH_CHECK, ParamUtil.getBoolean(queryData, AppParams.N_AUTH_CHECK));

        return resultMap;
    }

    private static final Logger LOGGER = Logger.getLogger(ClientService.class.getName());
}
