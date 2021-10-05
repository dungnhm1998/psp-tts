package asia.leadsgen.psp.service_fulfill;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.TrackingObj;
import asia.leadsgen.psp.server.handler.warehouse.WareHouseCreateHandler;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.UUIDUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;
import oracle.net.aso.j;

public class UpdateTrackingService {
	
	private static final String INSERT_MULTI = "{call PKG_TRACKING.insert_multi(?)}";
	private static final String GET_DATA_BY_GROUP_ID = "{call PKG_TRACKING.get_by_group_id(?,?,?,?)}";
	
	private static String domain;
	
	private static DataSource dataSource;
	
	public void setDomain (String domain) {
		UpdateTrackingService.domain = domain;
	}
	
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public static boolean validateToken(String token) {
		if (StringUtils.isEmpty(token)) {
			return false;
		} else {
			String[] splitz = token.split(" ");
			if (splitz.length < 2 ) {
				return false;
			} else if (!"bearer".equalsIgnoreCase(splitz[0])) {
				return false;
			} else {
				try {
					byte[] decode = AppUtil.decodeBase64(splitz[1]);
					String decodeStr = new String(decode);
					JsonObject jsonToken = new JsonObject(decodeStr);
					LOGGER.info("TOKEN INFO " + jsonToken.toString());

					String apiKey = getMd5("H4Qah6MbqrkxPFTH"+domain);
					String apiKey2= getMd5("RosalindaAPI_v2"+domain);

					String requestToken = jsonToken.getString("signature");

					if ("H4Qah6MbqrkxPFTH".equalsIgnoreCase(jsonToken.getString("partner_id")) 
							&& (requestToken.equalsIgnoreCase(apiKey) || requestToken.equalsIgnoreCase(apiKey2))) {
						return true;
					}
				} catch (Exception e) {
					return false;
				}
			}
		}
		return false;
	}
	
	public static String insertData(JsonArray datas , String partnerId) throws SQLException {
		List<TrackingObj> listDataInsert = new ArrayList<TrackingObj>();
		LOGGER.info("Start Execute "+LocalTime.now());
		String groupId = UUIDUtil.getUuid();
		try (Connection hikariCon = dataSource.getConnection()) {
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				LOGGER.info("Insert Multi Data Into TB_UPDATE_TRACKING ...");
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				for (int i = 0 ; i<= datas.size()-1 ; i++) {
					TrackingObj obj = new TrackingObj();
					JsonObject data = datas.getJsonObject(i);
					Clob clob = con.createClob();
					clob.setString(1, data.toString());
					obj.setPartnerId(partnerId);
					obj.setCode(-1);
					obj.setMessage("Pending");
					obj.setTracking(clob);
					obj.setGroupId(groupId);
					obj.setState("pending");
					
					listDataInsert.add(obj);
				}
				LOGGER.info("Insert " + listDataInsert.size() + " Record");
				TrackingObj[] arr = new TrackingObj[listDataInsert.size()];
				arr = listDataInsert.toArray(arr);
				
				java.sql.Array orclArr = con.createOracleArray("UPDATE_TRACKING_ORDER_T", arr);
				try (CallableStatement cstmt = con.prepareCall(INSERT_MULTI);) {
					cstmt.setArray(1, orclArr);
					cstmt.execute();
				}
			}
		}
		LOGGER.info("End Execute "+LocalTime.now());
		LOGGER.info("Insert Success With Group Id = " + groupId);
		return groupId;
	}
	
	
	public static List<Map> getDataByGroupId(String groupId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, groupId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, GET_DATA_BY_GROUP_ID, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			LOGGER.severe("Get data Error");
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultData = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		return resultData;
	}
	
	public static Map format(List<Map> datas) {
		List<JsonObject> list = new ArrayList<JsonObject>();
		Map responseData = new LinkedHashMap<>();
		int pending = 0;
		int successed = 0;
		int failed = 0;
		for(Map data : datas) {
			String tracking = ParamUtil.getString(data, "S_TRACKING");
			JsonObject obj = new JsonObject(tracking);
			String message = ParamUtil.getString(data, "S_MESSAGE");
			int code = ParamUtil.getInt(data, "N_CODE");
			String state = ParamUtil.getString(data, AppParams.S_STATE);
			if ("pending".equalsIgnoreCase(state)) {
				pending++;
			} else if ("successed".equalsIgnoreCase(state)) {
				successed++;
			} else if ("failed".equalsIgnoreCase(state)) {
				failed++;
			}
			obj.put("code", code);
			obj.put("message", message);
			list.add(obj);
		}
		responseData.put("pending", pending);
		responseData.put("successed", successed);
		responseData.put("failed", failed);
		responseData.put("data", list);
		
		return responseData;
	}
	
	public static String getMd5(String input)
    {
        try {
  
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } 
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
	
	private static final Logger LOGGER = Logger.getLogger(UpdateTrackingService.class.getName());
}
