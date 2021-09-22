package asia.leadsgen.psp.service_fulfill;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.http.ParseException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DropshipOrderServiceV2 extends MasterService {

	static final String DROPSHIP_DASHBOARD_CONVERSION_OVERVIEW = "{call PKG_FF_DROPSHIP_ORDER.get_info_dropship_dashboard_overview(?,?,?,?,?,?)}";
    static final String DROPSHIP_DASHBOARD_CONVERSION_DETAIL = "{call PKG_FF_DROPSHIP_ORDER.get_info_dropship_dashboard_detail(?,?,?,?,?,?,?,?)}";
    static final String GET_NEWEST_ETSY_ORDER_ID = "{call pkg_dropship_order.get_newest_etsy_order_id(?,?,?,?)}";
    
    public static Map getDashboardConversionOverview(String orderId)
            throws SQLException, ParseException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, orderId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(2, OracleTypes.NUMBER);
        outputParamsTypes.put(3, OracleTypes.VARCHAR);
        outputParamsTypes.put(4, OracleTypes.NUMBER);
        outputParamsTypes.put(5, OracleTypes.NUMBER);
        outputParamsTypes.put(6, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(2, AppParams.RESULT_CODE);
        outputParamsNames.put(3, AppParams.RESULT_MSG);
        outputParamsNames.put(4, AppParams.UNIT_SALES);
        outputParamsNames.put(5, AppParams.REVENUE);
        outputParamsNames.put(6, AppParams.RESULT_DATA);

        Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_DASHBOARD_CONVERSION_OVERVIEW, inputParams,
                outputParamsTypes, outputParamsNames);
        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList =ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

        Double unitSale = ParamUtil.getDouble(resultMap, AppParams.UNIT_SALES);
        
        Double revenue = ParamUtil.getDouble(resultMap, AppParams.REVENUE);
        
        Map result = new HashedMap<>();

        result.put(AppParams.RESULT_DATA, resultDataList.get(0));
        result.put(AppParams.UNIT_SALES, unitSale);
        result.put(AppParams.REVENUE, revenue);
//
        LOGGER.info("=> lookUp result: " + result.toString());

        return result;
    }
    
    public static List<Map>  getDashboardConversionDetail(String userId, int page, int pageSize, String startDate, String endDate)
            throws SQLException, ParseException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, userId);
        inputParams.put(2, page);
        inputParams.put(3, pageSize);
        inputParams.put(4, startDate);
        inputParams.put(5, endDate);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(6, OracleTypes.NUMBER);
        outputParamsTypes.put(7, OracleTypes.VARCHAR);
        outputParamsTypes.put(8, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(6, AppParams.RESULT_CODE);
        outputParamsNames.put(7, AppParams.RESULT_MSG);
        outputParamsNames.put(8, AppParams.RESULT_DATA);

        Map resultMap = DBProcedureUtil.execute(dataSource, DROPSHIP_DASHBOARD_CONVERSION_DETAIL, inputParams,
                outputParamsTypes, outputParamsNames);
        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

        LOGGER.fine("=> lookUp result: " + resultDataList.toString());

        return resultDataList;
    }

    public static String getNewestOrderIdFromDB(String storeId) throws SQLException {
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

        Map insertResultMap = DBProcedureUtil.execute(dataSource, GET_NEWEST_ETSY_ORDER_ID,
                inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        if (resultCode != io.netty.handler.codec.http.HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDatalist = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

        if (!resultDatalist.isEmpty()) {
            String newestId = ParamUtil.getString(resultDatalist.get(0), AppParams.ORDER_ID.toUpperCase());
            return newestId;
        }
        return null;
    }

    private static final Logger LOGGER = Logger.getLogger(DropshipOrderServiceV2.class.getName());

}
