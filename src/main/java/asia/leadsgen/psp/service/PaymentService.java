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
import asia.leadsgen.psp.obj.TopupHistoryObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.HashMap;
import java.util.logging.Level;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class PaymentService {

    private static DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static final String UPDATE_WALLET_BY_USER_ID = "{call pkg_topup_history.update_wallet_by_user_id(?,?,?,?,?)}";
    
    public static Map search(String txnId, String tokenId, String payerId, String state, boolean getOrderInfo)
            throws SQLException {

        LOGGER.fine("Payment search with txnId=" + txnId + ", tokenId=" + tokenId + ", payerId=" + payerId + ", state="
                + state);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, txnId);
        inputParams.put(2, tokenId);
        inputParams.put(3, payerId);
        inputParams.put(4, state);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(5, OracleTypes.NUMBER);
        outputParamsTypes.put(6, OracleTypes.VARCHAR);
        outputParamsTypes.put(7, OracleTypes.NUMBER);
        outputParamsTypes.put(8, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(5, AppParams.RESULT_CODE);
        outputParamsNames.put(6, AppParams.RESULT_MSG);
        outputParamsNames.put(7, AppParams.RESULT_TOTAL);
        outputParamsNames.put(8, AppParams.RESULT_DATA);

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_SEARCH, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        List<Map> dataList = new ArrayList<>();

        for (Map resultDataMap : resultDataList) {

            dataList.add(format(resultDataMap, getOrderInfo));
        }

        Map resultMap = new LinkedHashMap();

        resultMap.put(AppParams.TOTAL, resultTotalRow);
        resultMap.put(AppParams.PAYMENTS, dataList);

        LOGGER.fine("=> Payment search result: " + resultTotalRow);

        return resultMap;
    }

    public static Map search_by_order(String orderId, String state, boolean getOrderInfo) throws SQLException {

        LOGGER.fine("Payment search with orderId=" + orderId + ", state=" + state);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, orderId);
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

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_SEARCH_BY_ORDER, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        List<Map> dataList = new ArrayList<>();

        for (Map resultDataMap : resultDataList) {

            dataList.add(format(resultDataMap, getOrderInfo));
        }

        Map resultMap = new LinkedHashMap();

        resultMap.put(AppParams.TOTAL, resultTotalRow);
        resultMap.put(AppParams.PAYMENTS, dataList);

        LOGGER.fine("=> Payment search result: " + resultTotalRow);

        return resultMap;
    }

    public static Map get(String id) throws SQLException {

        LOGGER.fine("Payment look up with id=" + id);

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

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_GET, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            throw new BadRequestException(SystemError.DATA_NOT_FOUND);
        }

        Map resultMap = format(resultDataList.get(0), false);

        LOGGER.fine("=> Payment look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

        return resultMap;
    }

    public static Map getPaymentByOrderId(String id) throws SQLException {

        LOGGER.fine("Payment look up with order id=" + id);

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

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_GET_BY_ORDER_ID, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            throw new BadRequestException(SystemError.DATA_NOT_FOUND);
        }

        Map resultMap = paymentByOrderFormat(resultDataList.get(0));

        LOGGER.fine("=> Payment look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

        return resultMap;
    }

    /**
     *
     * @param type
     * @param orderId
     * @param method
     * @param amount
     * @param currency
     * @return
     * @throws SQLException
     */
    public static Map insert(String type, String orderId, String method, String amount, String currency)
            throws SQLException {

        LOGGER.fine("Payment insert with type=" + type + ", orderId=" + orderId + ", method=" + method + ", amount="
                + amount + ", currency=" + currency);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, type);
        inputParams.put(2, orderId);
        inputParams.put(3, method);
        inputParams.put(4, amount);
        inputParams.put(5, currency);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(6, OracleTypes.NUMBER);
        outputParamsTypes.put(7, OracleTypes.VARCHAR);
        outputParamsTypes.put(8, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(6, AppParams.RESULT_CODE);
        outputParamsNames.put(7, AppParams.RESULT_MSG);
        outputParamsNames.put(8, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_INSERT, inputParams,
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

        Map resultMap = format(resultDataList.get(0), false);

        LOGGER.fine("=> Payment insert result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

        return resultMap;
    }

    public static boolean payPalCreateInvoice(String orderId, String state, String reference, String amount,
            String currency, String payerId, String method, String invoiceNumber) throws SQLException {

        Map inputParams = new LinkedHashMap<>();
        inputParams.put(1, orderId);
        inputParams.put(2, state);
        inputParams.put(3, reference);
        inputParams.put(4, amount);
        inputParams.put(5, currency);
        inputParams.put(6, payerId);
        inputParams.put(7, method);
        inputParams.put(8, invoiceNumber);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(9, OracleTypes.NUMBER);
        outputParamsTypes.put(10, OracleTypes.VARCHAR);
        outputParamsTypes.put(11, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(9, AppParams.RESULT_CODE);
        outputParamsNames.put(10, AppParams.RESULT_MSG);
        outputParamsNames.put(11, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_PAYPAL_INSERT, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        return resultCode == HttpResponseStatus.CREATED.code();
    }

    public static boolean paypalSentInvoiceUpdate(String id, String saleId, String paymentName) throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, saleId);
        inputParams.put(3, paymentName);
        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(4, OracleTypes.NUMBER);
        outputParamsTypes.put(5, OracleTypes.VARCHAR);
        outputParamsTypes.put(6, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(4, AppParams.RESULT_CODE);
        outputParamsNames.put(5, AppParams.RESULT_MSG);
        outputParamsNames.put(6, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_PAYPAL_SENT_INVOICE_UPDATE, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        return resultCode == HttpResponseStatus.OK.code();

    }

    public static boolean paypalRemoveByInvoiceNumber(String id) throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(2, OracleTypes.NUMBER);
        outputParamsTypes.put(3, OracleTypes.VARCHAR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(2, AppParams.RESULT_CODE);
        outputParamsNames.put(3, AppParams.RESULT_MSG);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_REMOVE_BY_INVOICE_NUMBER,
                inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        return resultCode == HttpResponseStatus.OK.code();

    }

    public static boolean paypalUpdateStateBySaleId(String id, String transactionId,String state,String amount) throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, transactionId);
        inputParams.put(3, state);
        inputParams.put(4, amount);
        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(5, OracleTypes.NUMBER);
        outputParamsTypes.put(6, OracleTypes.VARCHAR);
        outputParamsTypes.put(7, OracleTypes.CURSOR);
        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(5, AppParams.RESULT_CODE);
        outputParamsNames.put(6, AppParams.RESULT_MSG);
        outputParamsNames.put(7, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_UPDATE_BY_PAYPAL_SALE_ID,
                inputParams, outputParamsTypes, outputParamsNames);
        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        return resultCode == HttpResponseStatus.OK.code();

    }
    
    public static boolean paypalUpdateStateBySaleIdOtherDropship(String id, String transactionId,String state) throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, transactionId);
        inputParams.put(3, state);
        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(4, OracleTypes.NUMBER);
        outputParamsTypes.put(5, OracleTypes.VARCHAR);
        outputParamsTypes.put(6, OracleTypes.CURSOR);
        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(4, AppParams.RESULT_CODE);
        outputParamsNames.put(5, AppParams.RESULT_MSG);
        outputParamsNames.put(6, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_UPDATE_BY_PAYPAL_SALE_ID_OTHER_DROPSHIP,
                inputParams, outputParamsTypes, outputParamsNames);
        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        return resultCode == HttpResponseStatus.OK.code();

    }

    public static Map update(String id, String state, String reference, String payerId, String txnId, String tokenId,
            String info, String saleId, String invoiceNumber, String accountName) throws SQLException {

        LOGGER.info("Payment update with id=" + id + ", state=" + state + ", reference=" + reference + ", payerId="
                + payerId + ", txnId=" + txnId + ", tokenId=" + tokenId + ", info=" + info);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, state);
        inputParams.put(3, reference);
        inputParams.put(4, payerId);
        inputParams.put(5, txnId);
        inputParams.put(6, tokenId);
        inputParams.put(7, info);
        inputParams.put(8, saleId);
        inputParams.put(9, invoiceNumber);
        inputParams.put(10, accountName);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(11, OracleTypes.NUMBER);
        outputParamsTypes.put(12, OracleTypes.VARCHAR);
        outputParamsTypes.put(13, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(11, AppParams.RESULT_CODE);
        outputParamsNames.put(12, AppParams.RESULT_MSG);
        outputParamsNames.put(13, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PAYMENT_UPDATE, inputParams,
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

        Map resultMap = format(resultDataList.get(0), false);

        LOGGER.fine("=> Payment update result: " + ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));

        return resultMap;
    }

    private static Map format(Map queryData, boolean getOrderInfo) throws SQLException {

        Map resultMap = new LinkedHashMap<>();

        resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
        resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));
        resultMap.put(AppParams.TOKEN, ParamUtil.getString(queryData, AppParams.S_TOKEN_ID));
        resultMap.put(AppParams.REFERENCE, ParamUtil.getString(queryData, AppParams.S_REFERENCE));
        resultMap.put(AppParams.METHOD, ParamUtil.getString(queryData, AppParams.S_METHOD));
        resultMap.put(AppParams.TRANSACTION_ID, ParamUtil.getString(queryData, AppParams.S_TXN_ID));

        Map orderInfoMap = new LinkedHashMap<>();
        orderInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ORDER_ID));

        if (getOrderInfo) {
            orderInfoMap = OrderService.get(ParamUtil.getString(queryData, AppParams.S_ORDER_ID), true, false, false);
        }

        resultMap.put(AppParams.ORDER, orderInfoMap);
        resultMap.put(AppParams.AMOUNT, ParamUtil.getString(queryData, AppParams.S_AMOUNT));
        resultMap.put(AppParams.CURRENCY, ParamUtil.getString(queryData, AppParams.S_CURRENCY));

        String state = ParamUtil.getString(queryData, AppParams.S_STATE);

        resultMap.put(AppParams.STATE, state);

        resultMap.put(AppParams.CREATE_TIME, ParamUtil.getString(queryData, AppParams.D_CREATE));

        if (!ParamUtil.getString(queryData, AppParams.D_UPDATE).isEmpty()) {
            resultMap.put(AppParams.UPDATE_TIME, ParamUtil.getString(queryData, AppParams.D_UPDATE));
        }

        if (state.equalsIgnoreCase(ResourceStates.FAIL)) {
            resultMap.put(AppParams.REASON,
                    ParamUtil.getMapData(ParamUtil.getMapData(queryData, AppParams.S_INFO), AppParams.REASON));
        } else {
            resultMap.put(AppParams.INFORMATION, ParamUtil.getMapData(queryData, AppParams.S_INFO));
        }

        resultMap.put(AppParams.SALE_ID, ParamUtil.getString(queryData, AppParams.S_PAYPAL_SALE_ID));

        resultMap.put(AppParams.PAYMENT_NAME, ParamUtil.getString(queryData, AppParams.S_PAYMENT_NAME));

        return resultMap;
    }

    private static Map paymentByOrderFormat(Map queryData) throws SQLException {

        Map resultMap = new LinkedHashMap<>();
        resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
        resultMap.put(AppParams.METHOD, ParamUtil.getString(queryData, AppParams.S_METHOD));
        resultMap.put(AppParams.TRANSACTION_ID, ParamUtil.getString(queryData, AppParams.S_TXN_ID));
        resultMap.put(AppParams.SALE_ID, ParamUtil.getString(queryData, AppParams.S_PAYPAL_SALE_ID));
        resultMap.put(AppParams.AMOUNT, ParamUtil.getString(queryData, AppParams.S_AMOUNT));
        resultMap.put(AppParams.CURRENCY, ParamUtil.getString(queryData, AppParams.S_CURRENCY));
        resultMap.put(AppParams.PAYMENT_NAME, ParamUtil.getString(queryData, AppParams.S_PAYMENT_NAME));
        return resultMap;
    }
    
    public static Map insert(String cardId, String customerId, String state) throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, cardId);
        inputParams.put(2, customerId);
        inputParams.put(3, state);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(4, OracleTypes.NUMBER);
        outputParamsTypes.put(5, OracleTypes.VARCHAR);
        outputParamsTypes.put(6, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(4, AppParams.RESULT_CODE);
        outputParamsNames.put(5, AppParams.RESULT_MSG);
        outputParamsNames.put(6, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, PAYMENT_VERIFICATION_INSERT, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.CREATED.code()) {
            throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
        }

        Map resultMap = formatPayment(resultDataList.get(0));

        return resultMap;
    }
    
    private static Map formatPayment(Map queryData) throws SQLException {

        Map resultMap = new LinkedHashMap<>();
        resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
        resultMap.put(AppParams.CODE, ParamUtil.getString(queryData, AppParams.S_CODE));
        
        return resultMap;
    }
    
    public static Map checkCardVerificationCode(String id, String code) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, code);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, PAYMENT_VERIFICATION_CODE, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		Map<String, Object> response = new HashMap<>();
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (!resultDataList.isEmpty()) {
			response.put(AppParams.ID, ParamUtil.getString(resultDataList.get(0), AppParams.S_ID));
			response.put(AppParams.STATE, ParamUtil.getString(resultDataList.get(0), AppParams.S_STATE));
			response.put("total_failure", ParamUtil.getInt(resultDataList.get(0), "N_TOTAL_FAILURE"));

		}
		
		LOGGER.info("=> lookup result: " + resultMap.toString());
		return response;
	}
    
    public static Map getById(String id) throws SQLException {

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

		Map resultMap = DBProcedureUtil.execute(dataSource, PAYMENT_VERIFICATION_GET_BY_ID, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		Map<String, Object> response = new HashMap<>();
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (!resultDataList.isEmpty()) {
			response.put(AppParams.ID, ParamUtil.getString(resultDataList.get(0), AppParams.S_ID));
			response.put(AppParams.STATE, ParamUtil.getString(resultDataList.get(0), AppParams.S_STATE));
			response.put("total_failure", ParamUtil.getInt(resultDataList.get(0), "N_TOTAL_FAILURE"));
			response.put("expire_code", ParamUtil.getString(resultDataList.get(0), "D_EXPIRE_CODE"));
		}
		
		LOGGER.info("=> lookup getById result: " + resultMap.toString());
		return response;
	}
    
    
    public static int updateWalletBalanceByUserID(String user_id, Double amount) throws SQLException {
		LOGGER.info("updateWalletBalanceByUserID()- user_id= " + user_id + ", amount= " + amount);


		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, user_id);
		inputParams.put(2, amount);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, UPDATE_WALLET_BY_USER_ID, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		return resultCode;
	
	}
	
	public static final String PAYMENT_VERIFICATION_CODE = "{call PKG_PAYMENT.payment_verification_code(?,?,?,?,?)}";
    
    public static final String PAYMENT_VERIFICATION_INSERT = "{call PKG_PAYMENT.payment_verification_insert(?,?,?,?,?,?)}";
    
    public static final String PAYMENT_VERIFICATION_GET_BY_ID = "{call PKG_PAYMENT.payment_verification_get_by_id(?,?,?,?)}";

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());
}
