package asia.leadsgen.psp.service;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.data.type.RedisKeyEnum;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.ShippingOwnerObj;
import asia.leadsgen.psp.obj.ShippingProduct;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * Created by hungdx on 6/6/17.
 */
public class ShippingService {
	
	private static final String SHIPPING_GET_COUNTRY_BY_STATE = "{call PKG_SHIPPING.shipping_get_country_by_state(?,?,?,?)}";
    private static final String GET_ALL_SHIPPING_GROUP = "{call PKG_SHIPPING.get_all_shipping_group(?,?,?)}";
	
	public static Map<String, String> mexicoStateAlpha2Code = initMap();
	
//	private static Map<String, String> initMap() { 
//		Map<String, String> map = new HashMap<>();
//		map.put("Aguascalientes","AG");
//		map.put("Baja California","BN");
//		map.put("Baja California Sur","BS");
//		map.put("Campeche","CM");
//		map.put("Chiapas","CP");
//		map.put("Chihuahua","CH");
//		map.put("Coahuila","CA");
//		map.put("Colima","CL");
//		map.put("Distrito Federal","DF");
//		map.put("Durango","DU");
//		map.put("Estado de México","MX");
//		map.put("Guanajuato","GT");
//		map.put("Guerrero","GR");
//		map.put("Hidalgo","HI");
//		map.put("Jalisco","JA");
//		map.put("Michoacán","MC");
//		map.put("Morelos","MR");
//		map.put("Nayarit","NA");
//		map.put("Nuevo León","NL");
//		map.put("Oaxaca","OA");
//		map.put("Puebla","PU");
//		map.put("Querétaro","QE");
//		map.put("Quintana Roo","QR");
//		map.put("San Luis Potosí","SL");
//		map.put("Sinaloa","SI");
//		map.put("Sonora","SO");
//		map.put("Tabasco","TB");
//		map.put("Tamaulipas","TM");
//		map.put("Tlaxcala","TL");
//		map.put("Veracruz","VE");
//		map.put("Yucatán","YU");
//		map.put("Zacatecas","ZA");
//		return map;
//	}
	
	private static Map<String, String> initMap() { 
		Map<String, String> map = new HashMap<>();
		map.put("Aguascalientes","AG");
		map.put("Baja California","BC");
		map.put("Baja California Sur","BS");
		map.put("Campeche","CM");
		map.put("Chiapas","CS");
		map.put("Chihuahua","CH");
		map.put("Coahuila","CO");
		map.put("Colima","CL");
		map.put("Mexico City","DF");
		map.put("Durango","DG");
		map.put("Guanajuato","GT");
		map.put("Guerrero","GR");
		map.put("Hidalgo","HG");
		map.put("Jalisco","JA");
		map.put("México","EM");
		map.put("Michoacán","MI");
		map.put("Morelos","MO");
		map.put("Nayarit","NA");
		map.put("Nuevo León","NL");
		map.put("Oaxaca","OA");
		map.put("Puebla","PU");
		map.put("Querétaro","QT");
		map.put("Quintana Roo","QR");
		map.put("San Luis Potosí","SL");
		map.put("Sinaloa","SI");
		map.put("Sonora","SO");
		map.put("Tabasco","TB");
		map.put("Tamaulipas","TM");
		map.put("Tlaxcala","TL");
		map.put("Veracruz","VE");
		map.put("Yucatán","YU");
		map.put("Zacatecas","ZA");
		return map;
	}
	
    private static DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static Map get(String id) throws SQLException {

        LOGGER.fine("Shipping lookup with id=" + id);

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

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.SHIPPING_GET, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
//            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
            return null;
        }

        LOGGER.fine("=> Shipping info look up result: " + ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));

        return format(resultDataList.get(0));
    }
    
    
    public static String getShipingCountryByState(String state) throws SQLException {

        LOGGER.fine("Shipping lookup with state=" + state);
        String country_name = "";
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, state);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(2, OracleTypes.NUMBER);
        outputParamsTypes.put(3, OracleTypes.VARCHAR);
        outputParamsTypes.put(4, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(2, AppParams.RESULT_CODE);
        outputParamsNames.put(3, AppParams.RESULT_MSG);
        outputParamsNames.put(4, AppParams.RESULT_DATA);

        Map searchResultMap = DBProcedureUtil.execute(dataSource, SHIPPING_GET_COUNTRY_BY_STATE, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            return null;
        } else {
        	country_name = ParamUtil.getString(resultDataList.get(0), AppParams.S_NAME);
        }

        return country_name;
    }

//	public static ShippingProduct getByBaseAndSize(String baseId, String size) throws SQLException {
//
//		LOGGER.log(Level.INFO, "Get base weight for baseId : {0}, size : {1} ", new Object[] { baseId, size });
//
//		final String redisKey = RedisKeyEnum.BASES_WEIGHT.getValue();
//		Map baseWeight = RedisService.get(redisKey);
//
//		if (baseWeight == null || baseWeight.isEmpty()) {
//			List<ShippingProduct> shippingProducts = getBaseWeight();
//			baseWeight = new HashMap<>();
//			for (ShippingProduct prd : shippingProducts) {
//				baseWeight.put(String.format("%s-%s", prd.getBaseId(), prd.getSize()), prd);
//			}
//			RedisService.persist(redisKey, baseWeight);
//		}
//
//		ShippingProduct sPrd = (ShippingProduct) baseWeight.get(String.format("%s-%s", baseId, size));
//
//		return sPrd;
//
//	}
    public static List<ShippingProduct> getBaseWeight() throws SQLException {

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(1, OracleTypes.NUMBER);
        outputParamsTypes.put(2, OracleTypes.VARCHAR);
        outputParamsTypes.put(3, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(1, AppParams.RESULT_CODE);
        outputParamsNames.put(2, AppParams.RESULT_MSG);
        outputParamsNames.put(3, AppParams.RESULT_DATA);

        Map searchResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.SHIPPING_GET_SHIPPING_PRODUCT,
                Collections.EMPTY_MAP, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);

        List<ShippingProduct> products = new ArrayList<>();

        if (!resultDataList.isEmpty()) {
            products = formatShippingProducts(resultDataList);
        }

        return products;
    }

    private static List<ShippingProduct> formatShippingProducts(List<Map> resultDataList) {

        List<ShippingProduct> products = new ArrayList<>();

        for (Map data : resultDataList) {
            ShippingProduct prd = new ShippingProduct();
            prd.setId(ParamUtil.getString(data, AppParams.S_ID));
            prd.setBaseId(ParamUtil.getString(data, AppParams.S_BASE_ID));
            prd.setSize(ParamUtil.getString(data, AppParams.S_SIZE));
            prd.setValue(ParamUtil.getDouble(data, AppParams.N_VALUE));

            products.add(prd);
        }

        return products;
    }

    public static Map insert(String name, String email, String phone, String line1, String line2, String city,
            String state, String postalCode, String country, String countryName) throws SQLException {

        LOGGER.fine("Shipping info insert with name=" + name + ", email=" + email + ", city=" + city + ", state="
                + state + ", postalCode=" + postalCode + ", country=" + country);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, name);
        inputParams.put(2, email);
        inputParams.put(3, phone);
        inputParams.put(4, line1);
        inputParams.put(5, line2);
        inputParams.put(6, city);
        inputParams.put(7, state);
        inputParams.put(8, postalCode);
        inputParams.put(9, country);
        inputParams.put(10, countryName);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(11, OracleTypes.NUMBER);
        outputParamsTypes.put(12, OracleTypes.VARCHAR);
        outputParamsTypes.put(13, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(11, AppParams.RESULT_CODE);
        outputParamsNames.put(12, AppParams.RESULT_MSG);
        outputParamsNames.put(13, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.SHIPPING_INSERT, inputParams,
                outputParamsTypes, outputParamsNames);
        LOGGER.info("=> Shipping info insertResultMap: " + insertResultMap.toString());

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

        LOGGER.fine("=> Shipping info insert result: " + resultMap.toString());

        return resultMap;
    }

    public static Map update(String id, String name, String email, String phone, String line1, String line2,
            String city, String state, String postalCode, String country, String countryName, boolean shipAsGift)
            throws SQLException {

        LOGGER.fine("Shipping info insert with id=" + id + ", email=" + email + ", city=" + city + ", state=" + state
                + ", postalCode=" + postalCode + ", country=" + country + ", shipAsGift=" + shipAsGift);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, name);
        inputParams.put(3, email);
        inputParams.put(4, phone);
        inputParams.put(5, line1);
        inputParams.put(6, line2);
        inputParams.put(7, city);
        inputParams.put(8, state);
        inputParams.put(9, postalCode);
        inputParams.put(10, country);
        inputParams.put(11, countryName);
        inputParams.put(12, shipAsGift);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(13, OracleTypes.NUMBER);
        outputParamsTypes.put(14, OracleTypes.VARCHAR);
        outputParamsTypes.put(15, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(13, AppParams.RESULT_CODE);
        outputParamsNames.put(14, AppParams.RESULT_MSG);
        outputParamsNames.put(15, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.SHIPPING_UPDATE, inputParams,
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

        Map resultMap = format(resultDataList.get(0));

        LOGGER.fine("=> Shipping info update result: " + resultMap.toString());

        return resultMap;
    }

    private static Map format(Map queryData) throws SQLException {

        Map resultMap = new LinkedHashMap<>();

        resultMap.put(AppParams.ID, ParamUtil.getString(queryData, AppParams.S_ID));
        resultMap.put(AppParams.NAME, ParamUtil.getString(queryData, AppParams.S_NAME));
        resultMap.put(AppParams.EMAIL, ParamUtil.getString(queryData, AppParams.S_EMAIL));
        resultMap.put(AppParams.PHONE, ParamUtil.getString(queryData, AppParams.S_PHONE));
        resultMap.put(AppParams.GIFT, ParamUtil.getBoolean(queryData, AppParams.N_GIFT));

        Map addressMap = new LinkedHashMap<>();

        addressMap.put(AppParams.LINE1, ParamUtil.getString(queryData, AppParams.S_ADD_LINE1));
        addressMap.put(AppParams.LINE2, ParamUtil.getString(queryData, AppParams.S_ADD_LINE2));
        addressMap.put(AppParams.CITY, ParamUtil.getString(queryData, AppParams.S_ADD_CITY));
        addressMap.put(AppParams.STATE, ParamUtil.getString(queryData, AppParams.S_ADD_STATE));
        addressMap.put(AppParams.POSTAL_CODE, ParamUtil.getString(queryData, AppParams.S_POSTAL_CODE));
        addressMap.put(AppParams.COUNTRY, ParamUtil.getString(queryData, AppParams.S_COUNTRY_CODE));
        addressMap.put(AppParams.COUNTRY_NAME, ParamUtil.getString(queryData, AppParams.S_COUNTRY_NAME));

        resultMap.put(AppParams.ADDRESS, addressMap);

        return resultMap;
    }

    public static ShippingOwnerObj getShippingOwner()
            throws SQLException, IllegalAccessException, InvocationTargetException {

        ShippingOwnerObj shippingOwner = (ShippingOwnerObj) RedisService
                .getObject(RedisKeyEnum.SHIPPING_OWNER.getValue());

        if (shippingOwner == null) {
            Map inputParams = new LinkedHashMap<Integer, String>();

            Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
            outputParamsTypes.put(1, OracleTypes.NUMBER);
            outputParamsTypes.put(2, OracleTypes.VARCHAR);
            outputParamsTypes.put(3, OracleTypes.CURSOR);

            Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
            outputParamsNames.put(1, AppParams.RESULT_CODE);
            outputParamsNames.put(2, AppParams.RESULT_MSG);
            outputParamsNames.put(3, AppParams.RESULT_DATA);

            Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.GET_SHIPPING_OWNER, inputParams,
                    outputParamsTypes, outputParamsNames);

            int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

            if (resultCode != HttpResponseStatus.OK.code()) {
                throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
            }

            List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

            if (!resultDataList.isEmpty()) {
                shippingOwner = formatShippingOwner(resultDataList.get(0));
                RedisService.persist(RedisKeyEnum.SHIPPING_OWNER.getValue(), shippingOwner);
            }

        }
        return shippingOwner;
    }

    private static ShippingOwnerObj formatShippingOwner(Map data) {

        ShippingOwnerObj shippingOwner = new ShippingOwnerObj();

        shippingOwner.setId(ParamUtil.getString(data, AppParams.S_ID));
        shippingOwner.setCompany(ParamUtil.getString(data, AppParams.S_COMPANY));
        shippingOwner.setPhone(ParamUtil.getString(data, AppParams.S_PHONE));
        shippingOwner.setCountry(ParamUtil.getString(data, AppParams.S_COUNTRY));
        shippingOwner.setAddState(ParamUtil.getString(data, AppParams.S_ADD_STATE));
        shippingOwner.setPostalCode(ParamUtil.getString(data, AppParams.S_POSTAL_CODE));
        shippingOwner.setCity(ParamUtil.getString(data, AppParams.S_CITY));
        shippingOwner.setAddLine1(ParamUtil.getString(data, AppParams.S_ADD_LINE1));
        shippingOwner.setAddLine2(ParamUtil.getString(data, AppParams.S_ADD_LINE2));
        shippingOwner.setState(ParamUtil.getString(data, AppParams.S_STATE));
        shippingOwner.setdCreate(ParamUtil.getString(data, AppParams.D_CREATE));
        shippingOwner.setdUpdate(ParamUtil.getString(data, AppParams.D_UPDATE));

        return shippingOwner;
    }

    public static void deleteByIdCSVImport(String orderId) throws SQLException {

        LOGGER.fine("Dropship Delete order products with orderId=: " + orderId);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, orderId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(2, OracleTypes.NUMBER);
        outputParamsTypes.put(3, OracleTypes.VARCHAR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(2, AppParams.RESULT_CODE);
        outputParamsNames.put(3, AppParams.RESULT_MSG);

        Map deleteResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.SHIPPING_DELETE_BY_ID_CSV_IMPORT,
                inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(deleteResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(deleteResultMap, AppParams.RESULT_MSG));
        }

        LOGGER.fine("=>Shipping delete result: " + resultCode);

    }

    public static Map updateDropshipOrder(String id, String name, String email, String phone, String line1, String line2,
            String city, String state, String postalCode, String country, String countryName, boolean shipAsGift)
            throws SQLException {

        LOGGER.fine("Shipping info insert with id=" + id + ", email=" + email + ", city=" + city + ", state=" + state
                + ", postalCode=" + postalCode + ", country=" + country + ", shipAsGift=" + shipAsGift);

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, name);
        inputParams.put(3, email);
        inputParams.put(4, phone);
        inputParams.put(5, line1);
        inputParams.put(6, line2);
        inputParams.put(7, city);
        inputParams.put(8, state);
        inputParams.put(9, postalCode);
        inputParams.put(10, country);
        inputParams.put(11, countryName);
        inputParams.put(12, shipAsGift);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(13, OracleTypes.NUMBER);
        outputParamsTypes.put(14, OracleTypes.VARCHAR);
        outputParamsTypes.put(15, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(13, AppParams.RESULT_CODE);
        outputParamsNames.put(14, AppParams.RESULT_MSG);
        outputParamsNames.put(15, AppParams.RESULT_DATA);

        Map insertResultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.DROPSHIP_SHIPPING_UPDATE, inputParams,
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

        Map resultMap = format(resultDataList.get(0));

        LOGGER.fine("=> Shipping info update result: " + resultMap.toString());

        return resultMap;

    }

    public static Map getAllShippingGroup(){
        Map itemGroupQuantity = new HashMap<>();
        int qty = 0;

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(1, OracleTypes.NUMBER);
        outputParamsTypes.put(2, OracleTypes.VARCHAR);
        outputParamsTypes.put(3, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(1, AppParams.RESULT_CODE);
        outputParamsNames.put(2, AppParams.RESULT_MSG);
        outputParamsNames.put(3, AppParams.RESULT_DATA);

        Map insertResultMap = null;
        try {
            insertResultMap = DBProcedureUtil.execute(dataSource, GET_ALL_SHIPPING_GROUP, Collections.EMPTY_MAP, outputParamsTypes, outputParamsNames);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
        }

        List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

        if (resultDataList.isEmpty()) {
            throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
        }

        for (Map data : resultDataList) {
            String groupId = ParamUtil.getString(data, AppParams.S_ID);
            itemGroupQuantity.put(groupId, qty);
        }

        return itemGroupQuantity;
    }

    private static final Logger LOGGER = Logger.getLogger(ShippingService.class.getName());
}
