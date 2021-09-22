package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import asia.leadsgen.psp.obj.OrderToFinanceObj;
import asia.leadsgen.psp.util.DBProcedurePool;

public class OrderToFinanceService extends MasterService {
	public static OrderToFinanceObj save(OrderToFinanceObj obj) throws SQLException {
		OrderToFinanceObj savedObj = null;
		int dropship = (obj.getIsDropship() == null || obj.getIsDropship() == false) ? 0 : 1;

		List<Map> result = excuteQuery(DBProcedurePool.ORDER_TO_FINANCE_QUEUE_SAVE, new Object[] { obj.getId(),
				obj.getOrderId(), obj.getPayload(), obj.getResponse(), obj.getState(), dropship });
		if (CollectionUtils.isNotEmpty(result)) {
			savedObj = OrderToFinanceObj.fromMap(result.get(0));
		}
		return savedObj;

	}

}
