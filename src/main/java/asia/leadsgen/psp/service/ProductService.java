package asia.leadsgen.psp.service;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import asia.leadsgen.psp.obj.ProductBaseSqlObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.BasePhoneCaseUtil;
import asia.leadsgen.psp.util.CampaignUtil;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.PreferenceKeys;
import asia.leadsgen.psp.util.PreferenceValues;
import oracle.jdbc.OracleConnection;
import oracle.sql.TIMESTAMP;
import org.apache.commons.collections4.CollectionUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class ProductService {

    private static NumberFormat amountFormatter = new DecimalFormat("#0.00");

    private static DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     *
     * @param id
     * @param baseInfo
     * @param colorInfo
     * @param designInfo
     * @param variantInfo
     * @return
     * @throws SQLException
     */
    public static Map get(String id, boolean baseInfo, boolean colorInfo, boolean designInfo, boolean variantInfo)
            throws SQLException {

        LOGGER.fine("Product lookup with id=" + id);

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

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_GET, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        String campaignId = ParamUtil.getString(resultDataList.get(0), AppParams.S_CAMPAIGN_ID);
        Map designInfoMap = designInfo == true ? CampaignUtil.getMapProductDesigns(campaignId) : null;
        Map variantInfoMap = variantInfo == true ? CampaignUtil.getMapProductVariants(campaignId) : null;
        Map mockupInfoMap = variantInfo == true ? CampaignUtil.getCampaignMockups(campaignId) : null;

        Map resultMap = format(resultDataList.get(0), baseInfo, colorInfo, designInfoMap, variantInfoMap, mockupInfoMap,
                null);

        LOGGER.fine("=> Product look up result: " + resultMap.toString());

        return resultMap;
    }

    public static Map getV2(String id, boolean baseInfo, boolean colorInfo, boolean designInfo, boolean variantInfo)
            throws SQLException {

        LOGGER.fine("Product lookup with id=" + id);

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

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_GET, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
           return new LinkedHashMap<>();
        }

        String campaignId = ParamUtil.getString(resultDataList.get(0), AppParams.S_CAMPAIGN_ID);
        Map designInfoMap = designInfo == true ? CampaignUtil.getMapProductDesigns(campaignId) : null;
        Map variantInfoMap = variantInfo == true ? CampaignUtil.getMapProductVariants(campaignId) : null;
        Map mockupInfoMap = variantInfo == true ? CampaignUtil.getCampaignMockups(campaignId) : null;

        Map resultMap = format(resultDataList.get(0), baseInfo, colorInfo, designInfoMap, variantInfoMap, mockupInfoMap,
                null);

        LOGGER.fine("=> Product look up result: " + resultMap.toString());

        return resultMap;
    }

    public static Map getBaseInfoAndPrice(String productId, String sizeId) throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, productId);
        inputParams.put(2, sizeId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(3, OracleTypes.NUMBER);
        outputParamsTypes.put(4, OracleTypes.VARCHAR);
        outputParamsTypes.put(5, OracleTypes.CURSOR);
        outputParamsTypes.put(6, OracleTypes.NUMBER);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(3, AppParams.RESULT_CODE);
        outputParamsNames.put(4, AppParams.RESULT_MSG);
        outputParamsNames.put(5, AppParams.RESULT_DATA);
        outputParamsNames.put(6, AppParams.PRICE);

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_GET_BASE_INFO_AND_PRICE,
                inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        Map response = new LinkedHashMap<>();

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        if (!resultDataList.isEmpty()) {
            response = formatPrdPrice(resultDataList.get(0));
        }
        return response;
    }

    private static Map formatPrdPrice(Map baseSearchResult) {
        Map el = new LinkedHashMap<>();
        el.put(AppParams.BASE_ID, ParamUtil.getString(baseSearchResult, AppParams.S_BASE_ID));
        el.put(AppParams.BASE_COST, ParamUtil.getString(baseSearchResult, AppParams.S_BASE_COST));
        el.put(AppParams.DROPSHIP_BASE_COST, ParamUtil.getString(baseSearchResult, AppParams.S_DROPSHIP_BASE_COST));
        el.put(AppParams.FULFILLMENTS, ParamUtil.getString(baseSearchResult, AppParams.S_FULFILLMENTS));
        el.put(AppParams.BASE_SHORT_CODE, ParamUtil.getString(baseSearchResult, AppParams.S_BASE_SHORT_CODE));
        el.put(AppParams.PRICE, ParamUtil.getString(baseSearchResult, AppParams.S_SALE_PRICE));
        return el;
    }

    public static Map search(String campaignId, boolean baseInfo, boolean colorInfo, Map designsInfo, Map variantsInfo,
            Map mockupsInfo, Map productPriceInfo) throws SQLException {

        LOGGER.fine("Product search with campaignId=" + campaignId);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, campaignId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(2, OracleTypes.NUMBER);
        outputParamsTypes.put(3, OracleTypes.VARCHAR);
        outputParamsTypes.put(4, OracleTypes.NUMBER);
        outputParamsTypes.put(5, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(2, AppParams.RESULT_CODE);
        outputParamsNames.put(3, AppParams.RESULT_MSG);
        outputParamsNames.put(4, AppParams.RESULT_TOTAL);
        outputParamsNames.put(5, AppParams.RESULT_DATA);

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_SEARCH, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);
        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        List<Map> dataList = new ArrayList<>();
        for (Map resultDataMap : resultDataList) {
            dataList.add(format(resultDataMap, baseInfo, colorInfo, designsInfo, variantsInfo, mockupsInfo,
                    productPriceInfo));
        }

        Map resultMap = new LinkedHashMap();

        resultMap.put(AppParams.TOTAL, resultTotalRow);
        resultMap.put(AppParams.PRODUCTS, dataList);

        LOGGER.fine("=> Product search result: " + resultTotalRow);

        return resultMap;
    }

    /**
     *
     * @param campaignId
     * @param baseId
     * @param name
     * @param desc
     * @param baseCost
     * @param salePrice
     * @param saleExpected
     * @param currency
     * @param sizes
     * @param colors
     * @param backView
     * @param mockupImageURL
     * @param defaultProduct
     * @param defaultColor
     * @param position
     * @param state
     * @return
     * @throws SQLException
     */
    public static Map<String, Object> insert(String campaignId, String baseId, String name, String desc,
            double baseCost, double salePrice, int saleExpected, String currency, String sizes, String colors,
            boolean backView, String mockupImageURL, boolean defaultProduct, String defaultColor, int position,
            String state) throws SQLException {

        LOGGER.fine("Product insert with campaignId=" + campaignId + ", baseId=" + name + ", name=" + name
                + ", baseCost=" + baseCost + ", salePrice=" + salePrice + ", state=" + state);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, campaignId);
        inputParams.put(2, baseId);
        inputParams.put(3, name);
        inputParams.put(4, desc);
        inputParams.put(5, baseCost);
        inputParams.put(6, salePrice);
        inputParams.put(7, saleExpected);
        inputParams.put(8, currency);
        inputParams.put(9, sizes);
        inputParams.put(10, colors);
        inputParams.put(11, backView);
        inputParams.put(12, mockupImageURL);
        inputParams.put(13, defaultProduct);
        inputParams.put(14, defaultColor);
        inputParams.put(15, position);
        inputParams.put(16, state);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(17, OracleTypes.NUMBER);
        outputParamsTypes.put(18, OracleTypes.VARCHAR);
        outputParamsTypes.put(19, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(17, AppParams.RESULT_CODE);
        outputParamsNames.put(18, AppParams.RESULT_MSG);
        outputParamsNames.put(19, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_INSERT, inputParams,
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

        Map designInfoMap = CampaignUtil.getMapProductDesigns(campaignId);
        LOGGER.info(campaignId + " designInfoMap=" + designInfoMap.toString());
        Map resultMap = format(resultDataList.get(0), true, true, designInfoMap, null, null, null);

        LOGGER.fine("=> Product insert result: " + resultMap.toString());

        return resultMap;
    }

    public static Map<String, Object> alloverInsert(String campaignId, String baseGroupId, String baseId, String name,
            int saleExpected, String currency, String sizes, boolean defaultProduct, int position, String state)
            throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, campaignId);
        inputParams.put(2, baseGroupId);
        inputParams.put(3, baseId);
        inputParams.put(4, name);
        inputParams.put(5, saleExpected);
        inputParams.put(6, currency);
        inputParams.put(7, sizes);
        inputParams.put(8, defaultProduct);
        inputParams.put(9, position);
        inputParams.put(10, state);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(11, OracleTypes.NUMBER);
        outputParamsTypes.put(12, OracleTypes.VARCHAR);
        outputParamsTypes.put(13, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(11, AppParams.RESULT_CODE);
        outputParamsNames.put(12, AppParams.RESULT_MSG);
        outputParamsNames.put(13, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_ALLOVER_INSERT, inputParams,
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

        Map designInfoMap = CampaignUtil.getMapProductDesigns(campaignId);
        Map resultMap = format(resultDataList.get(0), true, true, designInfoMap, null, null, null);

        LOGGER.fine("=> Product insert result: " + resultMap.toString());

        return resultMap;
    }

    public static Map<String, Object> insertAndUpdateDesign(String campaignId, String baseId, String name, String desc,
            double baseCost, double salePrice, int saleExpected, String currency, String sizes, String colors,
            boolean backView, String mockupImageURL, boolean defaultProduct, String defaultColor, int position,
            String state) throws SQLException {

        LOGGER.fine("Product insert with campaignId=" + campaignId + ", baseId=" + name + ", name=" + name
                + ", baseCost=" + baseCost + ", salePrice=" + salePrice + ", state=" + state);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, campaignId);
        inputParams.put(2, baseId);
        inputParams.put(3, name);
        inputParams.put(4, desc);
        inputParams.put(5, baseCost);
        inputParams.put(6, salePrice);
        inputParams.put(7, saleExpected);
        inputParams.put(8, currency);
        inputParams.put(9, sizes);
        inputParams.put(10, colors);
        inputParams.put(11, backView);
        inputParams.put(12, mockupImageURL);
        inputParams.put(13, defaultProduct);
        inputParams.put(14, defaultColor);
        inputParams.put(15, position);
        inputParams.put(16, state);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(17, OracleTypes.NUMBER);
        outputParamsTypes.put(18, OracleTypes.VARCHAR);
        outputParamsTypes.put(19, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(17, AppParams.RESULT_CODE);
        outputParamsNames.put(18, AppParams.RESULT_MSG);
        outputParamsNames.put(19, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_INSERT_AND_UPDATE_DESIGN,
                inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.CREATED.code()) {
            throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            throw new OracleException(
                    ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
        }

        Map designInfoMap = CampaignUtil.getMapProductDesigns(campaignId);
        LOGGER.info(campaignId + " designInfoMap=" + designInfoMap.toString());
        Map resultMap = format(resultDataList.get(0), true, true, designInfoMap, null, null, null);

        LOGGER.fine("=> Product insert result: " + resultMap.toString());

        return resultMap;
    }

    public static Map update(String id, String baseCost, double salePrice, int saleExpected, String sizes,
            String colors, boolean backView, String mockupImageURL, int position, boolean defaultProduct,
            String defaultColor, String state) throws SQLException {

        LOGGER.fine("Product update with id=" + id + ", baseCost=" + baseCost + ", salePrice=" + salePrice
                + ", saleExpected=" + saleExpected + ", position=" + position + ", defaultProduct=" + defaultProduct
                + ", backView=" + backView + ", state=" + state);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, baseCost);
        inputParams.put(3, salePrice);
        inputParams.put(4, saleExpected);
        inputParams.put(5, sizes);
        inputParams.put(6, colors);
        inputParams.put(7, backView);
        inputParams.put(8, mockupImageURL);
        inputParams.put(9, position);
        inputParams.put(10, defaultProduct);
        inputParams.put(11, defaultColor);
        inputParams.put(12, state);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(13, OracleTypes.NUMBER);
        outputParamsTypes.put(14, OracleTypes.VARCHAR);
        outputParamsTypes.put(15, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(13, AppParams.RESULT_CODE);
        outputParamsNames.put(14, AppParams.RESULT_MSG);
        outputParamsNames.put(15, AppParams.RESULT_DATA);

        Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_UPDATE, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(updateResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            throw new OracleException(
                    ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
        }

        Map designInfoMap = CampaignUtil
                .getMapProductDesigns(ParamUtil.getString(resultDataList.get(0), AppParams.S_ID));

        Map resultMap = format(resultDataList.get(0), true, true, designInfoMap, null, null, null);

        LOGGER.fine("=> Product update result: " + resultMap.toString());

        return resultMap;
    }

    public static void delete(String id) throws SQLException {

        LOGGER.fine("Product delete with id=" + id);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(2, OracleTypes.NUMBER);
        outputParamsTypes.put(3, OracleTypes.VARCHAR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(2, AppParams.RESULT_CODE);
        outputParamsNames.put(3, AppParams.RESULT_MSG);

        Map updateResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_DELETE, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
        }

        LOGGER.fine("=> Product delete result: " + ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
    }

    public static Map updateArt(String productId, String designType, String artId, String artPriceType, String artPrice)
            throws SQLException {

        LOGGER.log(Level.INFO, " updateArt {0}, {1}, {2}, {3} , {4} ",
                new Object[]{productId, designType, artId, artPriceType, artPrice});
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, productId);
        inputParams.put(2, designType);
        inputParams.put(3, artId);
        inputParams.put(4, artPriceType);
        inputParams.put(5, artPrice);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(6, OracleTypes.NUMBER);
        outputParamsTypes.put(7, OracleTypes.VARCHAR);
        outputParamsTypes.put(8, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(6, AppParams.RESULT_CODE);
        outputParamsNames.put(7, AppParams.RESULT_MSG);
        outputParamsNames.put(8, AppParams.RESULT_DATA);

        Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_UDATE_ART, inputParams,
                outputParamsTypes, outputParamsNames);
        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }
        List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
        if (resultDataList.isEmpty()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }

        resultMap = format(resultDataList.get(0), false, false, null, null, null, null);

        LOGGER.fine("=> updateArt result: " + resultMap.toString());

        return resultMap;
    }

    public static void delete(String productId, String campaignId, String deleteByBaseId, boolean isDefault)
            throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, productId);
        inputParams.put(2, campaignId);
        inputParams.put(3, deleteByBaseId);
        inputParams.put(4, isDefault ? 1 : 0);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(5, OracleTypes.NUMBER);
        outputParamsTypes.put(6, OracleTypes.VARCHAR);
        outputParamsTypes.put(7, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(5, AppParams.RESULT_CODE);
        outputParamsNames.put(6, AppParams.RESULT_MSG);
        outputParamsNames.put(7, AppParams.RESULT_DATA);

        Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_DELETE_2, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }
    }

    public static void updateDefault(String id, String variantId, boolean backview, String campId) throws SQLException {
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, variantId);
        inputParams.put(3, backview);
        inputParams.put(4, campId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(5, OracleTypes.NUMBER);
        outputParamsTypes.put(6, OracleTypes.VARCHAR);
        outputParamsTypes.put(7, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(5, AppParams.RESULT_CODE);
        outputParamsNames.put(6, AppParams.RESULT_MSG);
        outputParamsNames.put(7, AppParams.RESULT_DATA);

        Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_SET_TO_DEFAULT, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }

    }

    private static Map format(Map queryData, boolean baseInfo, boolean colorInfo, Map designInfo, Map variantsInfo,
            Map mockupsInfo, Map productPricesInfo) throws SQLException {

        Map resultMap = new LinkedHashMap<>();

        String productId = ParamUtil.getString(queryData, AppParams.S_ID);

        resultMap.put(AppParams.ID, productId);
        resultMap.put(AppParams.POSITION, ParamUtil.getString(queryData, AppParams.N_POSITION));
        resultMap.put(AppParams.BACK_VIEW, ParamUtil.getBoolean(queryData, AppParams.N_BACK_VIEW));
        resultMap.put(AppParams.DEFAULT, ParamUtil.getBoolean(queryData, AppParams.N_DEFAULT));
        resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));
        resultMap.put(AppParams.CURRENCY, ParamUtil.getString(queryData, AppParams.S_CURRENCY));
        resultMap.put(AppParams.SALE_EXPECTED, ParamUtil.getString(queryData, AppParams.N_SALE_EXPECTED));
        resultMap.put(AppParams.PRODUCT_NAME, ParamUtil.getString(queryData, AppParams.S_NAME));
        resultMap.put(AppParams.CAMPAIGN_ID, ParamUtil.getString(queryData, AppParams.S_CAMPAIGN_ID));
        resultMap.put(AppParams.PRODUCT_TYPE, ParamUtil.getString(queryData, AppParams.S_PRODUCT_TYPE));

        List<Map> productPricesList = productPricesInfo == null || productPricesInfo.isEmpty()
                ? ProductPriceService.getPrices(productId)
                : ParamUtil.getListData(productPricesInfo, productId);
        resultMap.put(AppParams.PRICES, productPricesList);

        Map artMap = new LinkedHashMap<>();
        artMap.put(AppParams.ART_ID_FRONT, ParamUtil.getString(queryData, AppParams.S_ART_ID_FRONT));
        artMap.put(AppParams.ART_PRICE_FRONT, ParamUtil.getString(queryData, AppParams.S_ART_PRICE_FRONT));
        artMap.put(AppParams.ART_PRICE_TYPE_FRONT, ParamUtil.getString(queryData, AppParams.S_ART_PRICE_TYPE_FRONT));
        artMap.put(AppParams.ART_ID_FRONT, ParamUtil.getString(queryData, AppParams.S_ART_ID_FRONT));
        artMap.put(AppParams.ART_PRICE_FRONT, ParamUtil.getString(queryData, AppParams.S_ART_PRICE_FRONT));
        artMap.put(AppParams.ART_PRICE_TYPE_FRONT, ParamUtil.getString(queryData, AppParams.S_ART_PRICE_TYPE_FRONT));

        resultMap.put(AppParams.ART, artMap);

        String baseId = ParamUtil.getString(queryData, AppParams.S_BASE_ID);
        if (BasePhoneCaseUtil.isPhoneCase(baseId)) {
            resultMap.put("display", ParamUtil.getBoolean(queryData, "N_PHONECASE_DISPLAY"));
        } else {
            resultMap.put("display", true);
        }

        if (baseInfo) {
            resultMap.put(AppParams.BASE, CampaignUtil.getBaseInfo(queryData));
        }

        if (colorInfo) {
            String productColorIds = ParamUtil.getString(queryData, AppParams.S_COLORS);
//            LOGGER.info("ProductService productColorIds=" + productColorIds);
            // String baseId = ParamUtil.getString(queryData, AppParams.S_BASE_ID);
            String defaultColorId = ParamUtil.getString(queryData, AppParams.S_DEFAULT_COLOR_ID);
            resultMap.put(AppParams.COLORS, CampaignUtil.getBaseColorList(baseId, productColorIds, defaultColorId));
        }

        resultMap.put(AppParams.DESIGNS,
                designInfo == null ? Collections.EMPTY_LIST : ParamUtil.getListData(designInfo, productId));

        if (variantsInfo != null && !variantsInfo.isEmpty()) {
            List<Map> variantMapList = ParamUtil.getListData(variantsInfo, productId);
            for (Map variantMap : variantMapList) {
                List<Map> mockupList = ParamUtil.getListData(mockupsInfo,
                        ParamUtil.getString(variantMap, AppParams.ID));
                if (CollectionUtils.isNotEmpty(mockupList)) {
                    variantMap.put(AppParams.MOCKUPS, mockupList);
                }
            }
            resultMap.put(AppParams.VARIANTS, variantMapList);
        }

        return resultMap;
    }
    
    public static void updateColors(String id, String colors, String defaultColorId) throws SQLException {
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, colors);
        inputParams.put(3, defaultColorId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(4, OracleTypes.NUMBER);
        outputParamsTypes.put(5, OracleTypes.VARCHAR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(4, AppParams.RESULT_CODE);
        outputParamsNames.put(5, AppParams.RESULT_MSG);

        Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_UPDATE_COLORS, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }

    }
    
    public static void leatherUpdateDefault(String id, String variantId, boolean backview, String campId) throws SQLException {
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, variantId);
        inputParams.put(3, backview);
        inputParams.put(4, campId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(5, OracleTypes.NUMBER);
        outputParamsTypes.put(6, OracleTypes.VARCHAR);
        outputParamsTypes.put(7, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(5, AppParams.RESULT_CODE);
        outputParamsNames.put(6, AppParams.RESULT_MSG);
        outputParamsNames.put(7, AppParams.RESULT_DATA);

        Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_SET_TO_DEFAULT, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }

    }
    
    public static Map<String, Object> leatherInsert(String campaignId, String baseGroupId, String baseId, String name,
            int saleExpected, String currency, String sizes, boolean defaultProduct, int position, String state, String colors)
            throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, campaignId);
        inputParams.put(2, baseGroupId);
        inputParams.put(3, baseId);
        inputParams.put(4, name);
        inputParams.put(5, saleExpected);
        inputParams.put(6, currency);
        inputParams.put(7, sizes);
        inputParams.put(8, defaultProduct);
        inputParams.put(9, position);
        inputParams.put(10, state);
        inputParams.put(11, colors);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(12, OracleTypes.NUMBER);
        outputParamsTypes.put(13, OracleTypes.VARCHAR);
        outputParamsTypes.put(14, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(12, AppParams.RESULT_CODE);
        outputParamsNames.put(13, AppParams.RESULT_MSG);
        outputParamsNames.put(14, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_LEATHER_INSERT, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.CREATED.code()) {
            throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) { 
        	throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
        }

        Map designInfoMap = CampaignUtil.getMapProductDesigns(campaignId);
        Map resultMap = format(resultDataList.get(0), true, true, designInfoMap, null, null, null);

        LOGGER.info("=> Product insert result: " + resultMap.toString());

        return resultMap;
    }
    
    public static void updateDesignId(String id) throws SQLException {
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(2, OracleTypes.NUMBER);
        outputParamsTypes.put(3, OracleTypes.VARCHAR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(2, AppParams.RESULT_CODE);
        outputParamsNames.put(3, AppParams.RESULT_MSG);

        Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.UPDATE_DESIGN_ID, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }

    }
    
    public static void deleteProducts(String campaignId, String productIds) throws SQLException {
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, campaignId);
        inputParams.put(2, productIds);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(3, OracleTypes.NUMBER);
        outputParamsTypes.put(4, OracleTypes.VARCHAR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(3, AppParams.RESULT_CODE);
        outputParamsNames.put(4, AppParams.RESULT_MSG);

        Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_LEATHER_DELETE, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }

    }
    
    public static void deleteVariants(String campaignId, String productId) throws SQLException {
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, campaignId);
        inputParams.put(2, productId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(3, OracleTypes.NUMBER);
        outputParamsTypes.put(4, OracleTypes.VARCHAR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(3, AppParams.RESULT_CODE);
        outputParamsNames.put(4, AppParams.RESULT_MSG);

        Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.VARIANT_LEATHER_DELETE, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
        }

    }

    public static List<Map> insertListToTable_TB_Product(String campaignId, List<Map> listProduct) throws SQLException {
        List<Map> result = new ArrayList<>();
        List<ProductBaseSqlObj> listBase = new ArrayList<>();

        double defaultProfit = GetterUtil.getDouble(
                PreferencesService.get(PreferenceKeys.PRODUCT_PROFIT_PER_UNIT_DEFAULT),
                PreferenceValues.PRODUCT_PROFIT_PER_UNIT_DEFAULT);

        int defaultProductSaleExpected = GetterUtil.getInteger(
                PreferencesService.get(PreferenceKeys.PRODUCT_SALE_EXPECTED_DEFAULT),
                PreferenceValues.PRODUCT_SALE_EXPECTED_DEFAULT);

        for (Map product : listProduct) {
            listBase.add(formatToBaseDetailObj(product, defaultProfit, defaultProductSaleExpected, campaignId));
        }

        List<Object[]> objects = new ArrayList<>();
        for (ProductBaseSqlObj product : listBase) {
            objects.add(new Object[]{product.getCamp_id(), product.getBase_id(), product.getBase_name(), product.getBase_cost(),
                                    product.getSale_price(), product.getSale_expected(), product.getSizes(), product.getColors(),
                                    product.getMock_up_url(), product.getDefault_color_id()});
        }

        try {
            Connection connection = dataSource.getConnection();

            if (connection.isWrapperFor(OracleConnection.class)) {
                OracleConnection conn = connection.unwrap(OracleConnection.class);

                Struct[] structs = new Struct[objects.size()];
                for (int i=0; i< structs.length;  i++) {
                    structs[i] = connection.createStruct("BASE_DETAIL", objects.get(i));

                }
                Array arrayOfProduct = conn.createOracleArray("BASE_DETAIL_TABLE",structs);
                CallableStatement statement = conn.prepareCall(INSERT_LIST_PRODUCT);

                statement.setArray(1, arrayOfProduct);
                statement.registerOutParameter(2, OracleTypes.NUMBER);
                statement.registerOutParameter(3, OracleTypes.VARCHAR);
                statement.registerOutParameter(4, OracleTypes.CURSOR);

                statement.execute();

                int statusCode = statement.getInt(2);
                if (statusCode != 201) {
                    LOGGER.warning(statement.getString(3));
                    throw new OracleException(statement.getString(3));
                }
                List<Map> resultDataList = ParamUtil.getListData(getMapFromStatement(statement, 4), AppParams.RESULT_DATA);


                if (!resultDataList.isEmpty()) {
                    Map designInfoMap = CampaignUtil.getMapProductDesigns(campaignId);
                    LOGGER.info(campaignId + " designInfoMap=" + designInfoMap.toString());
                    for (Map data : resultDataList) {
                        result.add(format(data, true, true, designInfoMap, null, null, null));
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
        return result;
    }

    public static ProductBaseSqlObj formatToBaseDetailObj(Map defaultBaseInfoMap, double defaultProfit,
                                                          int defaultProductSaleExpected, String campaignId) throws SQLException {
        String baseId = ParamUtil.getString(defaultBaseInfoMap, AppParams.S_ID);
        String name = ParamUtil.getString(defaultBaseInfoMap, AppParams.S_NAME);
        double defaultBasePrice = GetterUtil.format(ParamUtil.getDouble(defaultBaseInfoMap, AppParams.PRICE), 2);
        double defaultProductSalePrice = GetterUtil.format(defaultBasePrice + defaultProfit, 2);

        String listColor = ParamUtil.getString(defaultBaseInfoMap, AppParams.S_COLORS);

        String defaultColorId;
        if (listColor.contains(WHITE)) {
            defaultColorId = WHITE;
        } else if (listColor.contains(COLORFUL)) {
            defaultColorId = COLORFUL;
        } else {
            List<String> listColors = Arrays.stream(listColor.split(","))
                    .collect(Collectors.toList());
            defaultColorId = listColors.get(0).trim();
        }

        String baseSizes = ParamUtil.getString(defaultBaseInfoMap, AppParams.SIZES);

        ProductBaseSqlObj obj = new ProductBaseSqlObj(campaignId, baseId, name, String.valueOf(defaultBasePrice),
                String.valueOf(defaultProductSalePrice), defaultProductSaleExpected,
                baseSizes, defaultColorId, "",  defaultColorId);
        return obj;
    }

    public static Map getMapFromStatement(CallableStatement callableStatement, int paramIndex) throws SQLException {
        ResultSet resultSet = (ResultSet) callableStatement.getObject(paramIndex);
        Map result = new HashMap();
        if (resultSet != null) {

            List<Map<String, Object>> modelList = new ArrayList<>();

            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            while (resultSet.next()) {

                Map<String, Object> modelInfoMap = new LinkedHashMap<>();

                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {

                    Object value = resultSet.getObject(i);

                    if (value instanceof TIMESTAMP) {
                        value = new java.util.Date(((TIMESTAMP) value).timestampValue().getTime());
                    }

                    if (value instanceof Clob) {
                        Clob clob = resultSet.getClob(i);
                        value = clob.getSubString(1, (int) clob.length());
                    }

                    modelInfoMap.put(resultSetMetaData.getColumnName(i), value);
                }

                modelList.add(modelInfoMap);
            }
            result.put(AppParams.RESULT_DATA, modelList);
        }
        return result;
    }

    //update lại cột s_color của tb_product = các s_color_id trong tb_product_variant
    public static void updateColorForProduct(String productId) throws SQLException {
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, productId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(2, OracleTypes.NUMBER);
        outputParamsTypes.put(3, OracleTypes.VARCHAR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(2, AppParams.RESULT_CODE);
        outputParamsNames.put(3, AppParams.RESULT_MSG);

        Map updateResultMap = DBProcedureUtil.execute(dataSource, UPDATE_COLOR_FOR_PRODUCT, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ProductService.class.getName());

    static final String INSERT_LIST_PRODUCT = "{call PKG_PRODUCT.insert_list_product(?,?,?,?)}";
    static final String UPDATE_COLOR_FOR_PRODUCT = "{call PKG_PRODUCT.update_color_for_product(?,?,?)}";
    static final String WHITE = "__oavw-PWXYZ0JMm";
    static final String COLORFUL = "Pjxb5HvW6MGlyxBA";
}
