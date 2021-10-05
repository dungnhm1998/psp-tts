package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.ExternalTrackingObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class ExternalTrackingService extends MasterService {

	private static final String DELETE_EXTERNAL_TRACKING_BY_PACKAGE_ID = "{call PKG_EXTERNAL_TRACKING.delete_by_package_id(?,?,?)}";

	public static ExternalTrackingObj insertTracking(ExternalTrackingObj obj) throws SQLException {
		Map rs = insert(DBProcedurePool.EXTERNAL_TRACKING_INSERT, new Object[] { obj.getPackageId(), obj.getReferenceId(), obj.getVendor() });
		return ExternalTrackingObj.fromMap(rs);
	}

	public static List<ExternalTrackingObj> getByReferenceAndVendor(String ref, String vendor) throws SQLException {
		List<Map> rsList = searchAll(DBProcedurePool.EXTERNAL_TRACKING_GET_BY_REFERENCE_AND_VENDOR, new Object[] { ref, vendor });
		return (List) rsList.stream().map(o -> ExternalTrackingObj.fromMap(o)).collect(Collectors.toList());
	}
	
	public static ExternalTrackingObj updatePackageState(String refId, String packageid, String state) throws SQLException {
		List<Map> rsList = update(DBProcedurePool.EXTERNAL_TRACKING_UPDATE_PACKAGE_STATE, new Object[] { refId, packageid, state });
		return (rsList != null && rsList.isEmpty() == false) ? ExternalTrackingObj.fromMap(rsList.get(0)) : null;
	}
	
	public static void deleteOtherCarriersDetected(String id, String packageid, String ref) throws SQLException {
		update(DBProcedurePool.EXTERNAL_TRACKING_DELETE_OTHER_CARRIERS_DETECTED, new Object[] { id, packageid, ref});
	}
	
	public static ExternalTrackingObj updateTrackingStatus(String id, String packageid, String state) throws SQLException {
		List<Map> rsList = update(DBProcedurePool.EXTERNAL_TRACKING_UPDATE_TRACKING_STATUS, new Object[] { id, packageid, state });
		return (rsList != null && rsList.isEmpty() == false) ? ExternalTrackingObj.fromMap(rsList.get(0)) : null;
	}
	

	public static void deleteExternalTrackingByPackageId(String packageId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, packageId);
		
		Map<Integer, Integer> outTypes = new LinkedHashMap<>();
		outTypes.put(2, OracleTypes.NUMBER);
		outTypes.put(3, OracleTypes.VARCHAR);

		Map<Integer, String> outNames = new LinkedHashMap<>();
		outNames.put(2, AppParams.RESULT_CODE);
		outNames.put(3, AppParams.RESULT_MSG);

		Map queryResult = DBProcedureUtil.execute(dataSource, DELETE_EXTERNAL_TRACKING_BY_PACKAGE_ID, inputParams, outTypes, outNames);

		int resultCode = ParamUtil.getInt(queryResult, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code() && resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(queryResult, AppParams.RESULT_MSG));
		}

	}
	
}
