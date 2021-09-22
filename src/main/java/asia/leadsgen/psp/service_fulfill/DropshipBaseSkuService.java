package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.DropshipBaseSkuObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class DropshipBaseSkuService extends MasterService {

	static final String GET_DROPSHIP_BASE_BY_SKU = "{call PKG_FF_BASE_SKU.get_dropship_base_by_sku(?,?,?,?)}";

	public static DropshipBaseSkuObj getBySku(String sku) throws SQLException {
		logger.info("get DropshipBaseSkuObj by sku = "+sku);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, sku);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, GET_DROPSHIP_BASE_BY_SKU, inputParams, outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		return CollectionUtils.isNotEmpty(resultDataList) ? DropshipBaseSkuObj.fromMap(resultDataList.get(0)) : null;
	}

}

