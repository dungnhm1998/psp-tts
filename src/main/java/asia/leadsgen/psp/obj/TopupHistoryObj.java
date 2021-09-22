package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class TopupHistoryObj implements Serializable {
	private static final long serialVersionUID = -2983600627331475796L;
	private String id;
	private String type;
	private String userId;
	private String method;
	private String transactionID;
	private String state;
	private String note;
	private String refID;
	private String email;
	private Double amount;

	public static TopupHistoryObj fromMap(Map<String, Object> input) {
		TopupHistoryObj obj = new TopupHistoryObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setUserId(ParamUtil.getString(input, AppParams.S_USER_ID));
		obj.setType(ParamUtil.getString(input, AppParams.S_TYPE));
		obj.setMethod(ParamUtil.getString(input, AppParams.S_METHOD));
		obj.setTransactionID(ParamUtil.getString(input, AppParams.S_TRANSACTION_ID));
		obj.setEmail(ParamUtil.getString(input, AppParams.S_EMAIL));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setNote(ParamUtil.getString(input, AppParams.S_NOTE));
		obj.setRefID(ParamUtil.getString(input, AppParams.S_REF_ID));
		obj.setAmount(ParamUtil.getDouble(input, AppParams.N_AMOUNT));
		return obj;
	}
}
