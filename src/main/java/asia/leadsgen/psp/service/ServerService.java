package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

public class ServerService {

	static ArrayList<String> serverList = null;

	public static ArrayList<String> getServerList() throws SQLException {

		serverList = (serverList == null) ? getDbServerList() : serverList;
		return serverList;

	}

	private static ArrayList<String> getDbServerList() throws SQLException {

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(1, OracleTypes.NUMBER);
		outputParamsTypes.put(2, OracleTypes.VARCHAR);
		outputParamsTypes.put(3, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(1, AppParams.RESULT_CODE);
		outputParamsNames.put(2, AppParams.RESULT_MSG);
		outputParamsNames.put(3, AppParams.RESULT_DATA);

		Map resultMap = DBProcedureUtil.execute(dataSource, DBProcedurePool.GET_SERVER_LIST,
				Collections.EMPTY_MAP, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		ArrayList<String> dbServerList = new ArrayList<>();

		for (Map rs : resultDataList) {
			dbServerList.add(ParamUtil.getString(rs, AppParams.S_URL));
		}

		return dbServerList;
	}

	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

}
