package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DropshipStoreObj implements Serializable {
	private static final long serialVersionUID = -3643126821833823712L;

	private String id;

	private String channel;

	private String apiKey;

	private String apiPassword;

	private String sharedSecret;

	private String domain;

	private String state;

	private String dateCreate;

	private String dateUpdate;

	private String name;

	private String userId;

	private Integer connected;

	private Integer autoFulfill;

	private String locationId;
	
	private String clientId;

	public static DropshipStoreObj fromMap(Map<String, Object> input) {
		DropshipStoreObj obj = new DropshipStoreObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setChannel(ParamUtil.getString(input, AppParams.S_CHANNEL));
		obj.setApiKey(ParamUtil.getString(input, AppParams.S_API_KEY));
		obj.setApiPassword(ParamUtil.getString(input, AppParams.S_API_PASSWORD));
		obj.setSharedSecret(ParamUtil.getString(input, AppParams.S_SHARED_SECRET));
		obj.setDomain(ParamUtil.getString(input, AppParams.S_DOMAIN));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setDateCreate(ParamUtil.getString(input, AppParams.D_CREATE));
		obj.setDateUpdate(ParamUtil.getString(input, AppParams.D_UPDATE));
		obj.setName(ParamUtil.getString(input, AppParams.S_NAME));
		obj.setUserId(ParamUtil.getString(input, AppParams.S_USER_ID));
		obj.setConnected(ParamUtil.getInt(input, AppParams.N_CONNECTED, 0));
		obj.setAutoFulfill(ParamUtil.getInt(input, AppParams.N_AUTO_FULFILL, 0));
		obj.setLocationId(ParamUtil.getString(input, AppParams.S_LOCATION_ID));
		obj.setClientId(ParamUtil.getString(input, "S_CLIENT_ID"));
		return obj;
	}

}