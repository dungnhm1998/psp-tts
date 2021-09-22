package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

public class ProductPriceService {

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static Map updatePriceAndSaleExpected(String id, double salePrice, int saleExpected) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, salePrice);
		inputParams.put(3, saleExpected);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource,
				DBProcedurePool.PRODUCT_PRICE_UPDATE_PRICE_AND_SALE_EXPECTED, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		resultMap = format(resultDataList.get(0));
		LOGGER.fine("=> updatePriceAndSaleExpected result: " + resultMap.toString());
		return resultMap;
	}

	public static Map updatePriceAndSaleExpected(String id, double salePrice, int saleExpected, String campaignId)
			throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, salePrice);
		inputParams.put(3, saleExpected);
		inputParams.put(4, campaignId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);
		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_PRICE_UPDATE_DEFAULT, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		resultMap = format(resultDataList.get(0));
		LOGGER.fine("=> updatePriceAndSaleExpected result: " + resultMap.toString());
		return resultMap;
	}

	public static Map getPricesMapByCampaignId(String campaignId) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_PRICE_GET_BY_CAMP_ID, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		Set<String> productIds = resultDataList.stream().map(o -> ParamUtil.getString(o, AppParams.S_PRODUCT_ID))
				.collect(Collectors.toSet());
		Map<String, Object> productPricesMap = new LinkedHashMap<>();
		for (String productId : productIds) {
			productPricesMap.put(productId,
					resultDataList.stream()
							.filter(o -> productId.equals(ParamUtil.getString(o, AppParams.S_PRODUCT_ID)))
							.map(o -> format(o)).collect(Collectors.toList()));
		}

		return productPricesMap;
	}

	public static List<Map> getPrices(String productId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.PRODUCT_PRICE_GET_PRICES, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		return format(resultDataList);
	}

	private static List<Map> format(List<Map> resultDataList) {
		List<Map> prices = new ArrayList<>();
		for (Map el : resultDataList) {
			prices.add(format(el));
		}
		return prices;
	}

	public static Map format(Map data) {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_ID));
		el.put(AppParams.PRODUCT_ID, ParamUtil.getString(data, AppParams.S_PRODUCT_ID));
		el.put(AppParams.SIZE_ID, ParamUtil.getString(data, AppParams.S_SIZE_ID));
		el.put(AppParams.SIZE_NAME, ParamUtil.getString(data, AppParams.S_SIZE_NAME));
		el.put(AppParams.BASE_ID, ParamUtil.getString(data, AppParams.S_BASE_ID));
		el.put(AppParams.BASE_COST, ParamUtil.getString(data, AppParams.S_BASE_COST));
		el.put(AppParams.DROPSHIP_BASE_COST, ParamUtil.getString(data, AppParams.S_DROPSHIP_BASE_COST));
		el.put(AppParams.PRINT_PRICE_FRONT, ParamUtil.getString(data, AppParams.S_PRINT_PRICE_FRONT));
		el.put(AppParams.PRINT_PRICE_BACK, ParamUtil.getString(data, AppParams.S_PRINT_PRICE_BACK));
		el.put(AppParams.TWO_SIDES_PRICE, ParamUtil.getString(data, AppParams.S_TWO_SIDES_PRICE));
		el.put(AppParams.PRICE, ParamUtil.getString(data, AppParams.S_SALE_PRICE));
		el.put(AppParams.CURRENCY, ParamUtil.getString(data, AppParams.S_CURRENCY));
		el.put(AppParams.SALE_EXPECTED, ParamUtil.getString(data, AppParams.N_SALE_EXPECTED));
		el.put(AppParams.STATE, ParamUtil.getString(data, AppParams.S_STATE));
		return el;
	}

	private static final Logger LOGGER = Logger.getLogger(ProductPriceService.class.getName());
}
