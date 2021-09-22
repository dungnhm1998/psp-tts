package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.PartnerPayoutObj;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class PartnerPayoutService {

	static final String GET_BY_INVOICE_NUMER = "{call PKG_PARTNER_PAYOUT.get_by_invoice_number(?,?,?,?)}";

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static PartnerPayoutObj getByInvoiceNumber(String invoiceNumber) throws SQLException {
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, invoiceNumber);
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map data = DBProcedureUtil.execute(dataSource, GET_BY_INVOICE_NUMER, inputParams, outputParamsTypes,
				outputParamsNames);
		int resultCode = ParamUtil.getInt(data, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(data, AppParams.RESULT_MSG));
		}

		PartnerPayoutObj obj = null;
		List<Map> resultDataList = ParamUtil.getListData(data, AppParams.RESULT_DATA);

		if (CollectionUtils.isNotEmpty(resultDataList)) {
			obj = PartnerPayoutObj.fromMap(resultDataList.get(0));
		}
		return obj;

	}

	private static final Logger LOGGER = Logger.getLogger(PartnerPayoutService.class.getName());

}
