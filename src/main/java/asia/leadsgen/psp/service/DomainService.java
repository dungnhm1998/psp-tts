package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.VolumeDiscount;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 4/1/17.
 */
public class DomainService {

	private static DataSource dataSource;
	public static final String DOMAIN_URL_UPDATE_V2 = "{call PKG_DOMAIN_URL.url_update_v2(?,?,?,?,?,?)}";
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static Map domainSearch(String userId, String domainName) throws SQLException {

		LOGGER.fine("Domain search with userId=" + userId + "and domainName=" + domainName);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domainName);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(formatDomainMap(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.DOMAINS, dataList);

//		LOGGER.fine("=> Search result: " + resultTotalRow);
		return resultMap;
	}

	public static Map domainSearch(String userId, String domainName, Set<String> accessibleDomains)
			throws SQLException {

		LOGGER.fine("Domain search with userId=" + userId + "and domainName=" + domainName);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domainName);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

//		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);
		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {
			if (accessibleDomains.contains(ParamUtil.getString(resultDataMap, AppParams.S_NAME))) {
				dataList.add(formatDomainMap(resultDataMap));
			}
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, dataList.size());
		resultMap.put(AppParams.DOMAINS, dataList);

		return resultMap;
	}

	public static Map getDefaultDomain(String host) throws SQLException {

		Map defaultDomainInfoMap = new LinkedHashMap();

		List<Map> domainList = ParamUtil.getListData(domainSearch("", ""), AppParams.DOMAINS);

		for (Map domainInfo : domainList) {

			if (ParamUtil.getString(domainInfo, AppParams.NAME).equalsIgnoreCase(host)) {
				defaultDomainInfoMap = domainInfo;
				break;
			}
		}

		return defaultDomainInfoMap;
	}

	public static void updateDomainUri(String domainId, String uri, String uriType, String uriReference)
			throws SQLException {
		String correctUri = uri.contains(StringPool.FORWARD_SLASH)
				? uri.substring(uri.indexOf(StringPool.FORWARD_SLASH) + 1)
				: uri;
		List<Map> uriSearchList = ParamUtil.getListData(domainUriSearch("", "", correctUri, ""), AppParams.URIS);
		if (uriSearchList.size() > 0) {
			String currentReference = ParamUtil.getString(uriSearchList.get(0), AppParams.REFERENCE);
			if (!currentReference.equalsIgnoreCase(uriReference)) {
				throw new BadRequestException(SystemError.DUPLICATE_URI);
			}
		} else {
			List<Map> uriReferenceSearch = ParamUtil.getListData(domainUriSearch("", "", "", uriReference),
					AppParams.URIS);
			if (uriReferenceSearch.size() <= 0) {
				domainUrlInsert("", uriType, correctUri, uriReference);
			} else {
				String uriId = ParamUtil.getString(uriReferenceSearch.get(0), AppParams.ID);
				domainUrlUpdateV2(uriId,domainId,correctUri);
			}
		}
	}

	public static Map domainUriSearch(String domainId, String type, String uri, String reference) throws SQLException {

		LOGGER.fine("URL search with domainId=" + domainId + ", type=" + type + ", uri=" + uri + ", reference="
				+ reference);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, type);
		inputParams.put(3, uri);
		inputParams.put(4, reference);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_URL_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(uriFormat(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.URIS, dataList);

		LOGGER.fine("=> Search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map domainUriCheck(String domainId, String type, String uri, String reference) throws SQLException {

		LOGGER.fine("URL search with domainId=" + domainId + ", type=" + type + ", uri=" + uri + ", reference="
				+ reference);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, type);
		inputParams.put(3, uri);
		inputParams.put(4, reference);

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

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_URI_CHECK, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		List<Map> dataList = new ArrayList<>();

		for (Map resultDataMap : resultDataList) {

			dataList.add(uriFormat(resultDataMap));
		}

		Map resultMap = new LinkedHashMap();

		resultMap.put(AppParams.TOTAL, resultTotalRow);
		resultMap.put(AppParams.URIS, dataList);

		LOGGER.fine("=> Search result: " + resultTotalRow);

		return resultMap;
	}

	public static Map domainUrlInsert(String domainId, String type, String uri, String reference) throws SQLException {

		LOGGER.info("Domain uri insert with domainId=" + domainId + ", type=" + type + ", uri=" + uri + ", reference="
				+ reference);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, type);
		inputParams.put(3, uri);
		inputParams.put(4, reference);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_URL_INSERT, inputParams,
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

		Map resultMap = uriFormat(resultDataList.get(0));

		LOGGER.fine("=> Domain uri insert result: " + resultMap.toString());

		return resultMap;
	}

	public static Map domainUrlUpdate(String id, String uri) throws SQLException {

		LOGGER.fine("Domain uri update with id=" + id + ", uri=" + uri);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, uri);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_URL_UPDATE, inputParams,
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

		Map resultMap = uriFormat(resultDataList.get(0));

		LOGGER.fine("=> Domain uri update result: " + resultMap.toString());

		return resultMap;
	}
	
	public static Map domainUrlUpdateV2(String id,String domainId, String uri) throws SQLException {

		LOGGER.info("Domain uri update with id=" + id + ", domainId"+domainId + ", uri=" + uri);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, domainId);
		inputParams.put(3, uri);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource,DOMAIN_URL_UPDATE_V2, inputParams,
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

		Map resultMap = uriFormat(resultDataList.get(0));

		LOGGER.fine("=> Domain uri update result: " + resultMap.toString());

		return resultMap;
	}

	public static boolean updateSaleEvent(String userId, String domainId, Boolean countDown, Boolean discount)
			throws SQLException {

		LOGGER.fine("domainSaleEventChange=" + domainId);

		Map resultMap = new LinkedHashMap<>();
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, domainId);
		inputParams.put(3, countDown);
		inputParams.put(4, discount);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_SALE_EVENT_CHANGE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		return resultCode == HttpResponseStatus.OK.code();

	}

	public static Map domainSearch(String domainName) throws SQLException {

		LOGGER.fine("Domain search with domainName=" + domainName);

		Map resultMap = new LinkedHashMap<>();
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainName);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		outputParamsNames.put(5, AppParams.PROMOTION);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_SEARCH_V2, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (!resultDataList.isEmpty()) {
			resultMap = info(resultDataList.get(0));
		}

		List<Map> resultPromotions = ParamUtil.getListData(searchResultMap, AppParams.PROMOTION);
		Map promotion = Collections.EMPTY_MAP;
		if (!resultPromotions.isEmpty()) {
			promotion = formatPromotion(resultPromotions.get(0));
		}
		resultMap.put(AppParams.PROMOTION, promotion);
		List<VolumeDiscount> volumeDiscounts = PromotionService.getVolumeDiscounts(domainName);
		resultMap.put("volume_discounts", volumeDiscounts);

		LOGGER.fine("=> Domain search result: " + resultMap.toString());

		return resultMap;

	}

	private static Map formatPromotion(Map promo) {
		Map el = new LinkedHashMap<>();
		el.put(AppParams.ID, ParamUtil.getString(promo, AppParams.S_ID));
		el.put(AppParams.STATE, ParamUtil.getString(promo, AppParams.S_STATE));
		el.put(AppParams.EXPIRE, ParamUtil.getString(promo, AppParams.D_EXPIRE));
		el.put(AppParams.DOMAIN, ParamUtil.getString(promo, AppParams.S_DOMAIN));
		el.put(AppParams.DOMAIN_ID, ParamUtil.getString(promo, AppParams.S_DOMAIN_ID));
		el.put(AppParams.THRESHOLD, ParamUtil.getFormatedDouble(promo, AppParams.S_THRESHOLD, 2));
		el.put(AppParams.TYPE, ParamUtil.getString(promo, AppParams.S_TYPE));
		return el;
	}

	public static Map search(String domainName) throws SQLException {

		LOGGER.info("Domain search with domainName=" + domainName);

		Map resultMap = new LinkedHashMap<>();
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainName);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.SEARCH_DOMAIN, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		if (!resultDataList.isEmpty()) {
			resultMap = search(resultDataList.get(0));
		}

		LOGGER.info("=> Domain search result: " + resultMap.toString());

		return resultMap;

	}

	private static Map uriFormat(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		Map uriDomainInfoMap = new LinkedHashMap();
		uriDomainInfoMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_DOMAIN_ID));
		uriDomainInfoMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_DOMAIN_NAME));

		resultMap.put(AppParams.DOMAIN, uriDomainInfoMap);
		resultMap.put(AppParams.TYPE, ParamUtil.getString(queryData, AppParams.S_TYPE));
		resultMap.put(AppParams.URI, ParamUtil.getString(queryData, AppParams.S_URI));
		resultMap.put(AppParams.REFERENCE, ParamUtil.getString(queryData, AppParams.S_REF));
		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

		return resultMap;
	}

	private static Map format(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_DOMAIN_ID));

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

		return resultMap;
	}

	public static Map createDomain(String userId, String siteName, String fbUri, String twUri, String igUri,
			String bannerUrl, String logoUrl, String highlightColor, String seoTitle, String seoDesc,
			String seoImageCover) throws SQLException {

		LOGGER.log(Level.FINE,
				"Create domain for user {0} : site name = {1} , fbUri = {2} , twUri={3}, igUri={4}, bannerUrl={5}, logoUrl={6}, highlightColor={7}",
				new Object[] { userId, siteName, fbUri, twUri, igUri, bannerUrl, logoUrl, highlightColor });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, siteName);
		inputParams.put(3, fbUri);
		inputParams.put(4, twUri);
		inputParams.put(5, igUri);
		inputParams.put(6, bannerUrl);
		inputParams.put(7, logoUrl);
		inputParams.put(8, highlightColor);
		inputParams.put(9, seoTitle);
		inputParams.put(10, seoDesc);
		inputParams.put(11, seoImageCover);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_CREATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		Map domainMap = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			domainMap = formatDomainMap(resultDataList.get(0));
		}
		return domainMap;
	}

	public static Map registerDomain(String domainId, String name, String state, String regEmail, String regName, 
			String regPostcode, String regStreet, String regCity, String regCountryCode, String regCountryName) throws SQLException {

		LOGGER.log(Level.FINE,
				"Register domain id={0} : name={1}, description={2}, state={3}, "
				+ "regEmail={3}, regName={4}, regPostcode={5}, regStreet={6}, regCity={7}, regCity={8}, regCountryCode={9}, regCountryName={10}",
				new Object[] { domainId, name, "", state, regEmail, regName, 
						regPostcode, regStreet, regCity, regCountryCode, regCountryName });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, name);
		inputParams.put(3, state);
		inputParams.put(4, regEmail);
		inputParams.put(5, regName);
		inputParams.put(6, regPostcode);
		inputParams.put(7, regStreet);
		inputParams.put(8, regCity);
		inputParams.put(9, regCountryCode);
		inputParams.put(10, regCountryName);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.VARCHAR);
		outputParamsTypes.put(13, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(11, AppParams.RESULT_CODE);
		outputParamsNames.put(12, AppParams.RESULT_MSG);
		outputParamsNames.put(13, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_REGISTER, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		Map domainMap = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			domainMap = formatDomainMap(resultDataList.get(0));
		}
		return domainMap;
	}

	public static Map updateState(String domainId, String state) throws SQLException {

		LOGGER.log(Level.INFO, "Update domain id={0} : state = {1} ", new Object[] { domainId, state });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_UPDATE_STATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		Map domainMap = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			domainMap = formatDomainMap(resultDataList.get(0));
		}
		return domainMap;
	}

	public static Map updateDNSState(String domainId, String state) throws SQLException {

		LOGGER.log(Level.INFO, "Update domain id={0} : dns state = {1} ", new Object[] { domainId, state });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_UPDATE_DNS_STATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		Map domainMap = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			domainMap = formatDomainMap(resultDataList.get(0));
		}
		return domainMap;
	}

	public static Map listDomains(String userId) throws SQLException {

		LOGGER.log(Level.INFO, "List domains of user id= " + userId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_LIST, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		Map domainMap = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			domainMap = formatDomainsMap(resultDataList, null);
		}
		return domainMap;
	}

	public static Map listDomains(String userId, Set<String> accessibleDomains) throws SQLException {

		LOGGER.log(Level.INFO, "List domains of user id= " + userId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_LIST, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		Map domainMap = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			domainMap = formatDomainsMap(resultDataList, accessibleDomains);
		}
		return domainMap;
	}

	public static Map lookup(String domainId, String userId) throws SQLException {
		LOGGER.log(Level.INFO, "Domain lookup with id = " + domainId);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_LOOKUP, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			throw new BadRequestException(SystemError.DATA_NOT_FOUND);
		}
		return formatDomainMap(resultDataList.get(0));
	}

	public static Map updateDomain(String domainId, String name, String fbUri, String twUri, String igUri,
			String banner, String logo, String color, String seoTitle, String seoDesc, String seoImageCover, String officeAddress)
			throws SQLException {
		LOGGER.log(Level.FINE,
				"Update domain for domain {0} : site name = {1} , fbUri = {2} , twUri={3}, igUri={4}, bannerUrl={5}, logoUrl={6}, highlightColor={7}",
				new Object[] { domainId, name, fbUri, twUri, igUri, banner, logo, color });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, name);
		inputParams.put(3, fbUri);
		inputParams.put(4, twUri);
		inputParams.put(5, igUri);
		inputParams.put(6, banner);
		inputParams.put(7, logo);
		inputParams.put(8, color);
		inputParams.put(9, seoTitle);
		inputParams.put(10, seoDesc);
		inputParams.put(11, seoImageCover);
		inputParams.put(12, officeAddress);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(13, OracleTypes.NUMBER);
		outputParamsTypes.put(14, OracleTypes.VARCHAR);
		outputParamsTypes.put(15, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(13, AppParams.RESULT_CODE);
		outputParamsNames.put(14, AppParams.RESULT_MSG);
		outputParamsNames.put(15, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_UPDATE, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		Map domainMap = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			domainMap = formatDomainMap(resultDataList.get(0));
		}
		return domainMap;
	}

	private static Map formatDomainsMap(List<Map> dataList, Set<String> accessibleDomains) {
		Map domainMap = new LinkedHashMap<>();
		List<Map> domains = new ArrayList<>();
		for (Map data : dataList) {
			String name = ParamUtil.getString(data, AppParams.S_NAME);
			if (accessibleDomains == null || accessibleDomains.contains(name)) {
				domains.add(formatDomainMap(data));
			}
		}
		domainMap.put(AppParams.DATA, domains);
		return domainMap;
	}

	private static Map info(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		resultMap.put(AppParams.SITE_NAME, ParamUtil.getString(queryData, AppParams.S_SITE_NAME));

		resultMap.put(AppParams.FB_URI, ParamUtil.getString(queryData, AppParams.S_FB_URI));

		resultMap.put(AppParams.TW_URI, ParamUtil.getString(queryData, AppParams.S_TW_URI));

		resultMap.put(AppParams.IG_URI, ParamUtil.getString(queryData, AppParams.S_IG_URI));

		resultMap.put(AppParams.BANNER_URL, ParamUtil.getString(queryData, AppParams.S_BANNER_URL));

		resultMap.put(AppParams.LOGO_URL, ParamUtil.getString(queryData, AppParams.S_LOGO_URL));

		resultMap.put(AppParams.HIGHLIGHT_COLOR, ParamUtil.getString(queryData, AppParams.S_HIGHLIGHT_COLOR));

		resultMap.put(AppParams.DOMAIN_ID, ParamUtil.getString(queryData, AppParams.S_DOMAIN_ID));

		resultMap.put(AppParams.DOMAIN_NAME, ParamUtil.getString(queryData, AppParams.S_DOMAIN_NAME));

		resultMap.put(AppParams.REGISTER_EMAIL, ParamUtil.getString(queryData, AppParams.S_REGISTER_EMAIL));

		resultMap.put(AppParams.REGISTER_NAME, ParamUtil.getString(queryData, AppParams.S_REGISTER_NAME));

		resultMap.put(AppParams.REGISTER_STREET, ParamUtil.getString(queryData, AppParams.S_REGISTER_STREET));

		resultMap.put(AppParams.REGISTER_POSTCODE, ParamUtil.getString(queryData, AppParams.S_REGISTER_POSTCODE));

		resultMap.put(AppParams.REGISTER_CITY, ParamUtil.getString(queryData, AppParams.S_REGISTER_CITY));

		resultMap.put(AppParams.REGISTER_COUNTRY_CODE,
				ParamUtil.getString(queryData, AppParams.S_REGISTER_COUNTRY_CODE));

		resultMap.put(AppParams.REGISTER_COUNTRY_NAME,
				ParamUtil.getString(queryData, AppParams.S_REGISTER_COUNTRY_NAME));

		resultMap.put(AppParams.FB_PIXEL, ParamUtil.getString(queryData, AppParams.S_FB_PIXEL));

		resultMap.put(AppParams.GG_ADWORD_ID, ParamUtil.getString(queryData, AppParams.S_GG_ADWORD_ID));

		resultMap.put(AppParams.GG_ANALYTICS_ID, ParamUtil.getString(queryData, AppParams.S_GG_ANALYTICS_ID));

		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

		resultMap.put(AppParams.SEO_TITLE, ParamUtil.getString(queryData, AppParams.S_SEO_TITLE));
		resultMap.put(AppParams.SEO_DESC, ParamUtil.getString(queryData, AppParams.S_SEO_DESC));
		resultMap.put(AppParams.SEO_IMAGE_COVER, ParamUtil.getString(queryData, AppParams.S_SEO_IMAGE_COVER));

		return resultMap;
	}

	private static Map search(Map queryData) throws SQLException {

		Map resultMap = new LinkedHashMap<>();

		resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));

		resultMap.put(AppParams.USER_ID, ParamUtil.getString(queryData, AppParams.S_USER_ID));

		resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));

		resultMap.put(AppParams.LOGO, ParamUtil.getString(queryData, AppParams.S_LOGO_URL));

		resultMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_STATE));

		return resultMap;
	}

	public static Map updateTracking(String domainId, String userId, String facebookPixelId, String googleAdwordsId,
			String googleAnalyticsId, String googleSiteVerification, Boolean active_countdown,
			Boolean active_auto_discount, Boolean activeSearchBar,
			Boolean activeCustomerLogin, String googleAccountId, String facebookAccountId,
			String googleTagManagerId, String googleConversionTrackingId) throws SQLException {

		LOGGER.log(Level.FINE,
				"Update domain for domain {0} : facebookPixelId = {1} , googleAdwordsId = {2} , googleAnalyticsId = {3} , googleSiteVerification = {4} , "
				+ "googleTagManagerId = {5} , googleConversionTrackingId = {6}",
				new Object[] { domainId, facebookPixelId, googleAdwordsId, googleAnalyticsId, googleSiteVerification, 
						googleTagManagerId, googleConversionTrackingId });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, userId);
		inputParams.put(3, facebookPixelId);
		inputParams.put(4, googleAdwordsId);
		inputParams.put(5, googleAnalyticsId);
		inputParams.put(6, googleSiteVerification);

		inputParams.put(7, active_countdown);
		inputParams.put(8, active_auto_discount);
		inputParams.put(9, activeSearchBar);
		
		inputParams.put(10, activeCustomerLogin);
		inputParams.put(11, googleAccountId);
		inputParams.put(12, facebookAccountId);
		
		inputParams.put(13, googleTagManagerId);
		inputParams.put(14, googleConversionTrackingId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(15, OracleTypes.NUMBER);
		outputParamsTypes.put(16, OracleTypes.VARCHAR);
		outputParamsTypes.put(17, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(15, AppParams.RESULT_CODE);
		outputParamsNames.put(16, AppParams.RESULT_MSG);
		outputParamsNames.put(17, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_UPDATE_TRACKING, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		Map domainMap = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			domainMap = formatDomainMap(resultDataList.get(0));
		}
		return domainMap;
	}

	public static void updateCertificate(String domainId, String certificateID, String issuedDate, String expireDate,
			String state) throws SQLException {
		LOGGER.log(Level.FINE,
				"Update certificate for domain {0} : certificateID = {1} , issuedDate = {2} , expireDate={3}, state={4}",
				new Object[] { domainId, certificateID, issuedDate, expireDate, state });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, certificateID);
		inputParams.put(3, issuedDate);
		inputParams.put(4, expireDate);
		inputParams.put(5, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_UPDATE_CERTIFICATE,
				inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

	}

	private static Map formatDomainMap(Map raw) {
		Map domainMap = new LinkedHashMap<>();
		domainMap.put(AppParams.ID, ParamUtil.getString(raw, AppParams.S_ID));
		domainMap.put(AppParams.SITE_NAME, ParamUtil.getString(raw, AppParams.S_SITE_NAME));
		domainMap.put(AppParams.NAME, ParamUtil.getString(raw, AppParams.S_NAME));
		domainMap.put(AppParams.FACEBOOK, ParamUtil.getString(raw, AppParams.S_FB_URI));
		domainMap.put(AppParams.TWITTER, ParamUtil.getString(raw, AppParams.S_TW_URI));
		domainMap.put(AppParams.INSTAGRAM, ParamUtil.getString(raw, AppParams.S_IG_URI));
		domainMap.put(AppParams.BANNER, ParamUtil.getString(raw, AppParams.S_BANNER_URL));
		domainMap.put(AppParams.LOGO, ParamUtil.getString(raw, AppParams.S_LOGO_URL));
		domainMap.put(AppParams.COLOR, ParamUtil.getString(raw, AppParams.S_HIGHLIGHT_COLOR));
		domainMap.put(AppParams.CREATE_TIME, ParamUtil.getString(raw, AppParams.D_CREATE));
		domainMap.put(AppParams.STATE, ParamUtil.getString(raw, AppParams.S_STATE));
		domainMap.put(AppParams.SEO_TITLE, ParamUtil.getString(raw, AppParams.S_SEO_TITLE));
		domainMap.put(AppParams.SEO_DESC, ParamUtil.getString(raw, AppParams.S_SEO_DESC));
		domainMap.put(AppParams.SEO_IMAGE_COVER, ParamUtil.getString(raw, AppParams.S_SEO_IMAGE_COVER));
		domainMap.put(AppParams.REGISTER_DATE, ParamUtil.getString(raw, AppParams.D_REGISTER));
		domainMap.put(AppParams.EXPIRE_DATE, ParamUtil.getString(raw, AppParams.D_EXPIRE));

		domainMap.put(AppParams.ACTIVE_COUNTDOWN, ParamUtil.getBoolean(raw, AppParams.N_ACTIVE_COUNTDOWN));
		domainMap.put(AppParams.ACTIVE_AUTO_DISCOUNT, ParamUtil.getBoolean(raw, AppParams.N_ACTIVE_AUTO_DISCOUNT));
		
		domainMap.put("office_address", ParamUtil.getString(raw, "S_OFFICE_ADDRESS"));

		Map trackingTags = new LinkedHashMap<>();
		trackingTags.put(AppParams.FB_PIXEL, ParamUtil.getString(raw, AppParams.S_FB_PIXEL));
		trackingTags.put(AppParams.GG_ADWORD_ID, ParamUtil.getString(raw, AppParams.S_GG_ADWORD_ID));
		trackingTags.put(AppParams.GG_ANALYTICS_ID, ParamUtil.getString(raw, AppParams.S_GG_ANALYTICS_ID));
		trackingTags.put(AppParams.GG_SITE_VERIFY, ParamUtil.getString(raw, AppParams.S_GG_SITE_VERIFY));
		trackingTags.put(AppParams.GG_TAG_MANAGER_ID, ParamUtil.getString(raw, AppParams.S_GG_TAG_MANAGER_ID));
		trackingTags.put(AppParams.GG_CONVERSION_TRACKING_ID, ParamUtil.getString(raw, AppParams.S_GG_CONVERSION_TRACKING_ID));
		domainMap.put("tracking_tags", trackingTags);

		domainMap.put(AppParams.CUSTOM_HEADER, ParamUtil.getString(raw, AppParams.S_CUSTOM_HEADER));
		domainMap.put(AppParams.CUSTOM_BODY, ParamUtil.getString(raw, AppParams.S_CUSTOM_BODY));
		domainMap.put(AppParams.USER_ID, ParamUtil.getString(raw, AppParams.S_USER_ID));
		domainMap.put(AppParams.ACTIVE_SEARCH_BAR, ParamUtil.getBoolean(raw, AppParams.N_ACTIVE_SEARCH_BAR));
		domainMap.put(AppParams.ACTIVE_CUSTOMER_LOGIN, ParamUtil.getBoolean(raw, AppParams.N_ACTIVE_CUSTOMER_LOGIN));
		domainMap.put(AppParams.GG_ACCOUNT_ID, ParamUtil.getString(raw, AppParams.S_GG_ACCOUNT_ID));
		domainMap.put(AppParams.FB_ACCOUNT_ID, ParamUtil.getString(raw, AppParams.S_FB_ACCOUNT_ID));
		
		Map registerInfo = new LinkedHashMap<>();
		registerInfo.put(AppParams.REGISTER_EMAIL, ParamUtil.getString(raw, AppParams.S_REGISTER_EMAIL));
		registerInfo.put(AppParams.REGISTER_NAME, ParamUtil.getString(raw, AppParams.S_REGISTER_NAME));
		registerInfo.put(AppParams.REGISTER_STREET, ParamUtil.getString(raw, AppParams.S_REGISTER_STREET));
		registerInfo.put(AppParams.REGISTER_POSTCODE, ParamUtil.getString(raw, AppParams.S_REGISTER_POSTCODE));
		registerInfo.put(AppParams.REGISTER_CITY, ParamUtil.getString(raw, AppParams.S_REGISTER_CITY));
		registerInfo.put(AppParams.REGISTER_COUNTRY_CODE, ParamUtil.getString(raw, AppParams.S_REGISTER_COUNTRY_CODE));
		registerInfo.put(AppParams.REGISTER_COUNTRY_NAME, ParamUtil.getString(raw, AppParams.S_REGISTER_COUNTRY_NAME));
		domainMap.put("register_info", registerInfo);

		return domainMap;
	}

	public static Map getTracking(String domainName, String userId) throws SQLException {

		LOGGER.fine("Get tracking domain with domainName=" + domainName);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainName);
		inputParams.put(2, userId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_TRACKING, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
		}

		int resultTotalRow = ParamUtil.getInt(searchResultMap, AppParams.RESULT_TOTAL);

		List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

		Map resultMap = new LinkedHashMap();

		if (!resultDataList.isEmpty()) {
			resultMap = (resultDataList.get(0));
		}

		Map domainMap = new LinkedHashMap<>();

		domainMap.put("facebook_pixel_id", ParamUtil.getString(resultMap, AppParams.S_FB_PIXEL));
		domainMap.put("google_adwords_id", ParamUtil.getString(resultMap, AppParams.S_GG_ADWORD_ID));
		domainMap.put("google_analytics_id", ParamUtil.getString(resultMap, AppParams.S_GG_ANALYTICS_ID));

		LOGGER.fine("=> Get tracking domain result: " + resultDataList);

		return domainMap;
	}

	public static Map getCustomDomains(String userId, String state) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, userId);
		inputParams.put(2, state);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);
		outputParamsTypes.put(6, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);
		outputParamsNames.put(6, AppParams.RESULT_TOTAL);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_LIST_CUSTOM, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		Map response = new LinkedHashMap<>();

		response.put(AppParams.TOTAL, ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL));

		return response;
	}

	public static void addCodeDomain(String domainId, String userId, String customHeader, String customBody)
			throws SQLException {

		LOGGER.log(Level.FINE, "Add code for domain {0} : customHeader = {1} , customBody = {2}",
				new Object[] { domainId, customHeader, customBody });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, userId);
		inputParams.put(3, customHeader);
		inputParams.put(4, customBody);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_ADD_CODE, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

	}

	public static Map domainExpireTimeUpdate(String id, int years) throws SQLException {

		LOGGER.fine("Domain expire time update with id=" + id + ", years=" + years);

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, years);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.RENEW_DOMAIN, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

//		if (resultDataList.isEmpty()) {
//			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
//		}
//		Map resultMap = format(resultDataList.get(0));
//		LOGGER.fine("=> Domain expire time update result: " + resultMap.toString());
		return new LinkedHashMap<>();
	}

//	public static Map insertDomainRenew(String userId, String siteName, String fbUri, String twUri, String igUri,
//			String bannerUrl, String logoUrl, String highlightColor, String seoTitle, String seoDesc,
//			String seoImageCover) throws SQLException {
//
//		Map inputParams = new LinkedHashMap<Integer, String>();
//		inputParams.put(1, userId);
//		inputParams.put(2, siteName);
//
//		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
//		outputParamsTypes.put(3, OracleTypes.NUMBER);
//		outputParamsTypes.put(4, OracleTypes.VARCHAR);
//		outputParamsTypes.put(5, OracleTypes.CURSOR);
//
//		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
//		outputParamsNames.put(3, AppParams.RESULT_CODE);
//		outputParamsNames.put(4, AppParams.RESULT_MSG);
//		outputParamsNames.put(5, AppParams.RESULT_DATA);
//
//		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool., inputParams, outputParamsTypes, outputParamsNames);
//		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
//		if (resultCode != HttpResponseStatus.OK.code()) {
//			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
//		}
//
//		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
//		Map domainMap = new LinkedHashMap<>();
//		if (!resultDataList.isEmpty()) {
//			domainMap = formatDomainMap(resultDataList.get(0));
//		}
//		return domainMap;
//	}
	
	public static void activeSearchBarDomain(String domainId, String userId, Boolean activeSearchBar) throws SQLException {
		
		LOGGER.fine("activeSearchDomain: " + domainId);
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, userId);
		inputParams.put(3, activeSearchBar);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		
		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_ACTIVE_SEARCH, inputParams,
				outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static Map updateDomainInfoToRegister(String domainId, String domainName, String state, String regEmail, String regName, 
			String regPostcode, String regStreet, String regCity, String regCountryCode, String regCountryName) throws SQLException {

		LOGGER.log(Level.FINE,
				"Update Domain Info to Register domain id={0} : name={1}, description={2}, state={3}, "
				+ "regEmail={4}, regName={5}, regPostcode={6}, regStreet={7}, regCity={8}, regCity={9}, regCountryCode={10}, regCountryName={11}",
				new Object[] { domainId, domainName, "", state, regEmail, regName, 
						regPostcode, regStreet, regCity, regCountryCode, regCountryName });

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, domainId);
		inputParams.put(2, domainName);
		inputParams.put(3, state);
		inputParams.put(4, regEmail);
		inputParams.put(5, regName);
		inputParams.put(6, regPostcode);
		inputParams.put(7, regStreet);
		inputParams.put(8, regCity);
		inputParams.put(9, regCountryCode);
		inputParams.put(10, regCountryName);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(11, OracleTypes.NUMBER);
		outputParamsTypes.put(12, OracleTypes.VARCHAR);
		outputParamsTypes.put(13, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(11, AppParams.RESULT_CODE);
		outputParamsNames.put(12, AppParams.RESULT_MSG);
		outputParamsNames.put(13, AppParams.RESULT_DATA);

		Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DOMAIN_UPDATE_INFO_TO_REGISTER, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		Map domainMap = new LinkedHashMap<>();
		if (!resultDataList.isEmpty()) {
			domainMap = formatDomainMap(resultDataList.get(0));
		}
		return domainMap;
	}
	
	private static final Logger LOGGER = Logger.getLogger(DomainService.class.getName());

}
