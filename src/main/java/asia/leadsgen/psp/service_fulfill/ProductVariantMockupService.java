package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class ProductVariantMockupService {
	 private static DataSource dataSource;
	    
	    public void setDataSource(DataSource dataSource) {
	        this.dataSource = dataSource;
	    }
	    static final String GET_MOCKUP_BY_VARIANT_ID = "{call PKG_FF_PRODUCT_VARIANT_MOCKUP.get_mockup_by_variant_id(?,?,?,?,?)}";
	    
	    public static List<String> getMockupByVariantId(String variantId, String campaignId) throws SQLException {
			
			Map inputParams = new LinkedHashMap<Integer, String>();
			inputParams.put(1, variantId);
			inputParams.put(2, campaignId);

			Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
			outputParamsTypes.put(3, OracleTypes.NUMBER);
			outputParamsTypes.put(4, OracleTypes.VARCHAR);
			outputParamsTypes.put(5, OracleTypes.CURSOR);

			Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
			outputParamsNames.put(3, AppParams.RESULT_CODE);
			outputParamsNames.put(4, AppParams.RESULT_MSG);
			outputParamsNames.put(5, AppParams.RESULT_DATA);
			
			Map searchResultMap = DBProcedureUtil.execute(dataSource, GET_MOCKUP_BY_VARIANT_ID, inputParams,
					outputParamsTypes, outputParamsNames);

			int resultCode = ParamUtil.getInt(searchResultMap, AppParams.RESULT_CODE);

			if (resultCode != HttpResponseStatus.OK.code()) {
				throw new OracleException(ParamUtil.getString(searchResultMap, AppParams.RESULT_MSG));
			}

			List<Map> resultDataList = ParamUtil.getListData(searchResultMap, AppParams.RESULT_DATA);
			
			return resultDataList.stream().map(o -> ParamUtil.getString(o, AppParams.S_IMAGE_URL))
					.collect(Collectors.toList());
		}
}
