package asia.leadsgen.psp.server.handler.campaign_v2;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CustomTextModel {
	
	@SerializedName("text")
	@Expose
	private String text;
	
	@SerializedName("top")
	@Expose
	private String top;
	
	@SerializedName("left")
	@Expose
	private String left;
	
	@SerializedName("width")
	@Expose
	private String width;
	
	@SerializedName("height")
	@Expose
	private String height;
	
	@SerializedName("zIndex")
	@Expose
	private String zIndex;
	
	@SerializedName("rotation")
	@Expose
	private String rotation;
	
	@SerializedName("font")
	@Expose
	private String font;
	
	@SerializedName("fontFamily")
	@Expose
	private String fontFamily;
	
	@SerializedName("stroke_color")
	@Expose
	private String stroke_color;
	
	@SerializedName("fontSize")
	@Expose
	private String fontSize;
	
	@SerializedName("fontStyle")
	@Expose
	private String fontStyle;
	
	@SerializedName("fontWeight")
	@Expose
	private String fontWeight;
	
	@SerializedName("linethrough")
	@Expose
	private String linethrough;
	
	@SerializedName("textAlign")
	@Expose
	private String textAlign;
	
	@SerializedName("underline")
	@Expose
	private String underline;

	@Override
	public String toString() {
		return "CustomTextModel [text=" + text + ", top=" + top + ", left=" + left + ", width=" + width + ", height="
				+ height + ", zIndex=" + zIndex + ", rotation=" + rotation + ", font=" + font + ", fontFamily="
				+ fontFamily + ", stroke_color=" + stroke_color + ", fontSize=" + fontSize + ", fontStyle=" + fontStyle
				+ ", fontWeight=" + fontWeight + ", linethrough=" + linethrough + ", textAlign=" + textAlign
				+ ", underline=" + underline + "]";
	}

}
