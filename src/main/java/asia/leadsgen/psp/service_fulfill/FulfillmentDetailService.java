package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.Map;

import asia.leadsgen.psp.obj.FulfillmentDetailObj;

public class FulfillmentDetailService extends MasterService {

	static final String GET_BY_ID = "{call PKG_FULFILLMENT_NEW.get_fufillment_detail_by_id(?,?,?,?)}";
	
	public static FulfillmentDetailObj getById(String id) throws SQLException {
		Map result = searchOne(GET_BY_ID, new Object[] { id });
		FulfillmentDetailObj item = null;
		if (result != null) {
			item = FulfillmentDetailObj.fromMap(result);
		}
		return item;
	}
	
}
