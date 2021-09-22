package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.interfaces.LoggerInterface;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleTypes;

/**
 * 
 * @author liamle
 *
 */
public class MasterService implements LoggerInterface {

	static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		MasterService.dataSource = dataSource;
	}

	protected static Map searchOne(String query, Object[] args) throws SQLException {

		List<Map> queryDataList = excuteQuery(query, args);
		Map result = null;
		if (queryDataList.isEmpty() == false) {
			result = queryDataList.get(0);
		}
		return result;
	}

	protected static List<Map> searchAll(String query, Object[] args) throws SQLException {
		List<Map> queryDataList = excuteQuery(query, args);
		return queryDataList;
	}

	protected static List<Map> update(String query, Object[] args) throws SQLException {
		return excuteQuery(query, args);
	}

	protected static Map insert(String query, Object[] args) throws SQLException {
		List<Map> queryList = excuteQuery(query, args);
		return (queryList != null && queryList.isEmpty() == false) ? queryList.get(0) : null;
	}

	public static List<Map> excuteQuery(String query, Object[] args) throws SQLException {
		int beginIdx = 0;
		Map inArgs = new LinkedHashMap<>();
		if (args != null && args.length > 0) {
			for (int i = 1; i <= args.length; i++) {
				inArgs.put(i, args[i - 1]);
			}
			beginIdx = args.length;
		}

		Map<Integer, Integer> outTypes = new LinkedHashMap<>();
		outTypes.put(beginIdx + 1, OracleTypes.NUMBER);
		outTypes.put(beginIdx + 2, OracleTypes.VARCHAR);
		outTypes.put(beginIdx + 3, OracleTypes.CURSOR);

		Map<Integer, String> outNames = new LinkedHashMap<>();
		outNames.put(beginIdx + 1, AppParams.RESULT_CODE);
		outNames.put(beginIdx + 2, AppParams.RESULT_MSG);
		outNames.put(beginIdx + 3, AppParams.RESULT_DATA);

		Map queryResult = DBProcedureUtil.execute(dataSource, query, inArgs, outTypes, outNames);

		int resultCode = ParamUtil.getInt(queryResult, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code() && resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(queryResult, AppParams.RESULT_MSG));
		}

		List<Map> queryDataList = ParamUtil.getListData(queryResult, AppParams.RESULT_DATA);

		return queryDataList;
	}

}
