package asia.leadsgen.psp.server.handler.campaign_v2;

import java.io.Serializable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DesignModel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6492551275301815366L;

	@SerializedName("type")
	@Expose
	private String type;
	
	@SerializedName("url")
	@Expose
	private String url;
	
	@SerializedName("thumb_url")
	@Expose
	private String thumb_url;
	
	@SerializedName("width")
	@Expose
	private String width;
	
	@SerializedName("height")
	@Expose
	private String height;
	
	@SerializedName("crop_geometry")
	@Expose
	private String crop_geometry;
	
	@SerializedName("printable_top")
	@Expose
	private String printable_top;
	
	@SerializedName("printable_left")
	@Expose
	private String printable_left;
	
	@SerializedName("printable_width")
	@Expose
	private String printable_width;
	
	@SerializedName("printable_height")
	@Expose
	private String printable_height;
	
	@SerializedName("zIndex")
	@Expose
	private String zIndex;
	
	@SerializedName("main")
	@Expose
	private boolean isMain;
	
	@SerializedName("custom_texts")
	@Expose
	private String custom_texts;

	@Override
	public String toString() {
		return "DesignModel [type=" + type + ", url=" + url + ", thumb_url=" + thumb_url + ", width=" + width
				+ ", height=" + height + ", crop_geometry=" + crop_geometry + ", printable_top=" + printable_top
				+ ", printable_left=" + printable_left + ", printable_width=" + printable_width + ", printable_height="
				+ printable_height + ", zIndex=" + zIndex + ", isMain=" + isMain + ", custom_texts=" + custom_texts
				+ "]";
	}
	
}
