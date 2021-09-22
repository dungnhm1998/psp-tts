package asia.leadsgen.psp.server.handler.dropship.order;

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
public class MediaObj implements Serializable {
	private static final long serialVersionUID = -2983600627331475796L;
	private String id;
	private String userId;
	private String type;
	private String tags;
	private String baseId;
	private String url;
	private String create;
	private String update;
	private String state;
	private String name;
	private String size;
	private String resolution;
	private String thumbUrl;
	private String md5;
	
	

	public static MediaObj fromMap(Map<String, Object> input) {
		MediaObj obj = new MediaObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setUserId(ParamUtil.getString(input, AppParams.S_USER_ID));
		obj.setType(ParamUtil.getString(input, AppParams.S_TYPE));
		obj.setTags(ParamUtil.getString(input, AppParams.S_TAGS));
		obj.setBaseId(ParamUtil.getString(input, AppParams.S_BASE_ID));
		obj.setUrl(ParamUtil.getString(input, AppParams.S_URL));
		obj.setCreate(ParamUtil.getString(input, AppParams.D_CREATE));
		obj.setUpdate(ParamUtil.getString(input, AppParams.D_UPDATE));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setName(ParamUtil.getString(input, AppParams.S_NAME));
		obj.setSize(ParamUtil.getString(input, AppParams.S_SIZE));
		obj.setResolution(ParamUtil.getString(input, AppParams.S_RESOLUTION));
		obj.setThumbUrl(ParamUtil.getString(input, AppParams.S_THUMB_URL));
		obj.setMd5(ParamUtil.getString(input, AppParams.S_MD5));
		
		return obj;
	}

}
