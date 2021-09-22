package asia.leadsgen.psp.server.handler.dropship.order;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCancellationCheck implements Serializable {
	private static final long serialVersionUID = -6721574759544598172L;

	private String id;
	private String minifiedJson;
	private String storeId;
	private Integer fulfillmentRecords;
	private String state;
	private String subState;
	private String source;
	private Double hoursDiffFromPurchasedTime;

	public static OrderCancellationCheck fromMap(Map<String, Object> input) {
		OrderCancellationCheck obj = new OrderCancellationCheck();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setMinifiedJson(ParamUtil.getString(input, AppParams.S_MINIFIED_JSON));
		obj.setStoreId(ParamUtil.getString(input, AppParams.S_STORE_ID));
		obj.setFulfillmentRecords(ParamUtil.getInt(input, AppParams.N_FULFILLMENT_RECORDS, 0));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setSubState(ParamUtil.getString(input, AppParams.S_SUB_STATE));
		obj.setSource(ParamUtil.getString(input, AppParams.S_SOURCE));
		obj.setHoursDiffFromPurchasedTime(ParamUtil.getDouble(input, AppParams.N_HOURS_DIFF));
		return obj;
	}

}
