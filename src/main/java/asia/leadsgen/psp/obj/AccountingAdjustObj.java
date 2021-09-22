package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;

public class AccountingAdjustObj implements Serializable {

	private static final long serialVersionUID = -1L;

	private String id;
	private String type;
	private String content;
	private String state;

	public AccountingAdjustObj() {
	};

	public AccountingAdjustObj(String type, String content, String state) {
		this.type = type;
		this.content = content;
		this.state = state;
	}

	public static AccountingAdjustObj fromMap(Map<String, Object> input) {
		AccountingAdjustObj obj = new AccountingAdjustObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setType(ParamUtil.getString(input, AppParams.S_TYPE));
		obj.setContent(ParamUtil.getString(input, AppParams.S_CONTENT));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		return obj;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getState() {
		return state;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "AccountingAdjustObj [id=" + id + ", type=" + type + ", content=" + content + ", state=" + state + "]";
	}

}
