package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import asia.leadsgen.psp.obj.TopupHistoryObj;

public class TopupHistoryService extends MasterService {
	private static final String INSERT_TOPUP_HISTORY = "{call pkg_topup_history.insert_topup_history(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	private static final String UPDATE_STATE_BY_REF_ID = "{call pkg_topup_history.update_state_by_ref_id(?,?,?,?,?,?)}";
	private static final String UPDATE_STATE_BY_ID = "{call pkg_topup_history.update_state_by_id(?,?,?,?,?,?,?,?,?)}";
	private static final String CHECK_TRANSACTION_ID_EXISTS = "{call pkg_topup_history.check_transaction_id_exists(?,?,?,?)}";

	public static TopupHistoryObj insertTopupHistory(String type, String method, String transaction_id, String state,
			String note, String ref_id, String user_id, String email, Double amount, Double fee) throws SQLException {
		TopupHistoryObj obj = null;
		Map result = insert(INSERT_TOPUP_HISTORY,
				new Object[] { type, method, transaction_id, state, note, ref_id, user_id, email, amount, fee });
		if (!result.isEmpty()) {
			obj = TopupHistoryObj.fromMap(result);
		}
		return obj;

	}

	public static TopupHistoryObj updateStateByRefid(String ref_id, String state, String transaction_id) throws SQLException {
		logger.info("updateRow: ref_id= " + ref_id + ", state= " + state + ", transaction_id= " + transaction_id);
		TopupHistoryObj result = null;
		List<Map> results = update(UPDATE_STATE_BY_REF_ID, new Object[] { ref_id, state, transaction_id });
		for (Map map : results) {
			return TopupHistoryObj.fromMap(map);
		}
		return result;

	}

	public static TopupHistoryObj updateStateByID(String id, String state, String email, Double amount, Double extra_fee, String note) throws SQLException {
		logger.info("updateRow: id= " + id + ", state= " + state + ", email= " + email + ", amount= " + amount + ", extra_fee= " + extra_fee + ", note= " + note);
		TopupHistoryObj result = null;
		List<Map> results = update(UPDATE_STATE_BY_ID, new Object[] { id, email, state, amount, extra_fee, note});
		for (Map map : results) {
			return TopupHistoryObj.fromMap(map);
		}
		return result;
	}
	
	public static boolean checkTransactionExist(String transaction_id) throws SQLException {
		logger.info("checkTransactionExist: transaction_id= " + transaction_id);
		List<Map> results = searchAll(CHECK_TRANSACTION_ID_EXISTS, new Object[] { transaction_id });
		if(results.isEmpty() || results.size() <= 0)
			return false;
		return true;
	}

}
