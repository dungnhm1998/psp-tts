package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class WebhookService extends MasterService{
	
	private static final String WEBHOOK_INTERLOAN_UPDATE_HISTORY_STATE = "{call PKG_FF_WEBHOOK.webhook_interloan_update_history_state(?,?,?,?,?)}";;

	public static List<Map> interloanUpdateHistoryState(String refId, String state) throws SQLException {
		
		Object[] args = new Object[] {refId, state};
		
		List<Map> resultUpdate = update(WEBHOOK_INTERLOAN_UPDATE_HISTORY_STATE, args);
		return resultUpdate;
	}
	
}
