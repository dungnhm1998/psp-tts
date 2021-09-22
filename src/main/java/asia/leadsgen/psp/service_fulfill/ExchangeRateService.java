package asia.leadsgen.psp.service_fulfill;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;

public class ExchangeRateService {
	private static DataSource dataSource;
	private static String fixerKey;
	
	final static String REDIS_KEY = "redis.map.currency.exchangerate";
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public void setFixerKey(String key) {
		this.fixerKey = key;
	}
	
	static final String GET_EXCHANGE_RATE_BY_DATE = "{call pkg_exchange_rate.get_rate_by_currency(?,?,?,?,?,?)}";
	static final String GET_LATEST_RATES_BY_DATE = "{call pkg_exchange_rate.get_latest_rates(?,?,?)}";
	
	public static String getRate(String toCurrencyCode) throws SQLException, UnirestException, ParseException {
		Map<String,String> redis = RedisService.get(REDIS_KEY);
		
		if (redis == null || redis.isEmpty()) {
			redis = syncLastestRatesToRedis();
		}
		
		return redis.get(toCurrencyCode);
	}
	
	public static Map<String,String> syncLastestRatesToRedis() throws SQLException {
		
		//try to sync data from fixerio, if any exceptions thow consider current data in database is latest
		try {
		
			syncDataFromFixerIO();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(ParseException e) {
			e.printStackTrace();
		}
		
		List<Map<String, String>> list = getLatestRatesFromDb();
//		LOGGER.info("getLatestRatesFromDb: " + list.toString());
		Map<String,String> map = list.stream().collect(Collectors.toMap(s -> s.get("to_currency"), s -> s.get("rate")));
//		LOGGER.info("map: " + map.toString());
		return RedisService.save(REDIS_KEY, map, 30L, TimeUnit.MINUTES);

	}
	
	public static void syncDataFromFixerIO() throws UnirestException, SQLException, ParseException {
		
		HttpResponse<String> response = Unirest.get(String.format("http://data.fixer.io/api/latest?base=USD&access_key=%s", fixerKey)).asString();
		JsonObject responseBodyJson = new JsonObject(response.getBody());
		
//		LOGGER.info("responseBodyJson= " + responseBodyJson);
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date parsed = format.parse(responseBodyJson.getString("date"));
		java.sql.Date sqlDate = new java.sql.Date(parsed.getTime());
		
		final String base = responseBodyJson.getString("base");
		List<ExchangeLine> lines = new ArrayList<ExchangeLine>();
		JsonObject ratesObj = responseBodyJson.getJsonObject("rates");
		
		Iterator<Map.Entry<String, Object>> iterator = ratesObj.iterator();
		while(iterator.hasNext()) {
			Map.Entry<String, Object> entry = iterator.next();
			lines.add(new ExchangeLine(sqlDate, base,entry.getKey(), new BigDecimal(ratesObj.getDouble(entry.getKey()))));
		}
//		LOGGER.info("lines= " + lines.toString());
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				ExchangeLine[] arr = new  ExchangeLine[lines.size()];
				arr = lines.toArray(arr);
				java.sql.Array array_to_pass = con.createOracleArray("EXCHANGE_LINE_TYPE_T", arr);
				
				try (CallableStatement cstmt = con.prepareCall("{call PKG_EXCHANGE_RATE.update_rates(?)}");) {
					cstmt.setArray(1, array_to_pass); // Set input parameter
	
					cstmt.execute();
				}
			}
		}
//		LOGGER.info("done?");
		
	}
	
	public static double getExchangeRateFromDb(String date, String fromCurrencyCode, String toCurrencyCode) throws SQLException {
		Map<Integer, Object> inputParams = new LinkedHashMap<Integer, Object>();
		inputParams.put(1, date);
		inputParams.put(2, fromCurrencyCode);
		inputParams.put(3, toCurrencyCode);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);
		
		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_EXCHANGE_RATE_BY_DATE, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);
		
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}
		
		final double rate = ParamUtil.getDouble(searchResultMap, AppParams.RESULT_DATA);
		
		return rate;
	}
	
	public static List<Map<String,String>> getLatestRatesFromDb() throws SQLException {
		Map<Integer, Object> inputParams = new LinkedHashMap<Integer, Object>();
				
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(1, OracleTypes.NUMBER);
		outputParamsTypes.put(2, OracleTypes.VARCHAR);
		outputParamsTypes.put(3, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(1, AppParams.RESULT_CODE);
		outputParamsNames.put(2, AppParams.RESULT_MSG);
		outputParamsNames.put(3, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_LATEST_RATES_BY_DATE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_LIST;
		}

		List<Map<String,String>> baseColorList = formatList(resultDataList);

		return baseColorList;
		
	}
	
	private static List<Map<String,String>> formatList(List<Map> resultDataList) {
		List<Map<String,String>> list = new ArrayList<>();
		
		Map<String, String> map = Stream.of(
				  new AbstractMap.SimpleEntry<>("D_APPLIED", "date"), 
				  new AbstractMap.SimpleEntry<>("S_FROM_CURRENCY", "from_currency"),
				  new AbstractMap.SimpleEntry<>("S_TO_CURRENCY", "to_currency"),
				  new AbstractMap.SimpleEntry<>("N_RATE", "rate")
				)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		
		for (Map<String,String> resultData : resultDataList) {
			Map<String,String> el = new LinkedHashMap<>();
			
			map.forEach((k,v) -> el.put(v, ParamUtil.getString(resultData,k)));
			
			list.add(el);
		}
		return list;
	}
	
	
	static class ExchangeLine implements SQLData {
			public static final String SQL_TYPE = "EXCHANGE_LINE_TYPE";

			private java.sql.Date exchangeDate;
			private String fromCurrency;
			private String toCurrency;
			
			private BigDecimal rate;

			public ExchangeLine() {
			}

			public ExchangeLine(java.sql.Date exchangeDate, String fromCurrency, String toCurrency,  BigDecimal discountValue) {
				super();
				this.exchangeDate = exchangeDate;
				this.fromCurrency = fromCurrency;
				this.toCurrency = toCurrency;
				this.rate = discountValue;
			}

			@Override
			public String getSQLTypeName() throws SQLException {
				return SQL_TYPE;
			}

			@Override
			public void readSQL(SQLInput stream, String typeName) throws SQLException {
				exchangeDate = stream.readDate();
				fromCurrency = stream.readString();
				toCurrency = stream.readString();
				rate = stream.readBigDecimal();
			}

			@Override
			public void writeSQL(SQLOutput stream) throws SQLException {
				stream.writeDate(exchangeDate);
				stream.writeString(fromCurrency);
				stream.writeString(toCurrency);
				stream.writeBigDecimal(rate);
			}
			
			@Override
			  public String toString()
			  {
			    return String.format("base %s exchagned to %s with rate %f", fromCurrency, toCurrency, rate);
			  }

			public String getToCurrency() {
				return toCurrency;
			}
			
			
		}
	
	private static final Logger LOGGER = Logger.getLogger(ExchangeRateService.class.getName());
}
