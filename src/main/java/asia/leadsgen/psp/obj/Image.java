package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class Image implements Serializable {
	private static final long serialVersionUID = -2273477240841761897L;
	private String id;
	private String type;
	private String name;
	private String desc;
	private String url;
	private String width;
	private String height;
	private String ppi;
	private String cropGeometry;
	private String printableTop;
	private String printableLeft;
	private String printableWidth;
	private String printableHeight;
	private String state;
	private String dateCreate;
	private String dateUpdate;
	private String preview;
	private String colors;
	private int colorsCount;
	private int dpi;

	private String colorName;
	private String colorValue;
	private JsonObject customData;

	public static Image fromMap(Map<String, Object> input) {
		Image obj = new Image();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setType(ParamUtil.getString(input, AppParams.S_TYPE));
		obj.setName(ParamUtil.getString(input, AppParams.S_NAME));
		obj.setDesc(ParamUtil.getString(input, AppParams.S_DESC));
		obj.setUrl(ParamUtil.getString(input, AppParams.S_URL));
		obj.setWidth(ParamUtil.getString(input, AppParams.S_WIDTH));
		obj.setHeight(ParamUtil.getString(input, AppParams.S_HEIGHT));
		obj.setPpi(ParamUtil.getString(input, AppParams.S_PPI));
		obj.setCropGeometry(ParamUtil.getString(input, AppParams.S_CROP_GEOMETRY));
		obj.setPrintableTop(ParamUtil.getString(input, AppParams.S_PRINTABLE_TOP));
		obj.setPrintableLeft(ParamUtil.getString(input, AppParams.S_PRINTABLE_LEFT));
		obj.setPrintableWidth(ParamUtil.getString(input, AppParams.S_PRINTABLE_WIDTH));
		obj.setPrintableHeight(ParamUtil.getString(input, AppParams.S_PRINTABLE_HEIGHT));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setDateCreate(ParamUtil.getString(input, AppParams.D_CREATE));
		obj.setDateUpdate(ParamUtil.getString(input, AppParams.D_UPDATE));
		obj.setPreview(ParamUtil.getString(input, AppParams.S_PREVIEW));
		obj.setColors(ParamUtil.getString(input, AppParams.S_COLORS));
		obj.setColorsCount(ParamUtil.getInt(input, AppParams.N_COLORS_COUNT, 0));
		obj.setDpi(ParamUtil.getInt(input, AppParams.N_DPI, 0));
		obj.setColorName(ParamUtil.getString(input, AppParams.S_COLOR_NAME));
		obj.setColorValue(ParamUtil.getString(input, AppParams.S_COLOR_VALUE));
		return obj;
	}

	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put(AppParams.URL, this.url);
		json.put(AppParams.PRINTABLE_TOP, this.printableTop);
		json.put(AppParams.PRINTABLE_LEFT, this.printableLeft);
		json.put(AppParams.PRINTABLE_WIDTH, this.printableWidth);
		json.put(AppParams.PRINTABLE_HEIGHT, this.printableHeight);
		json.put(AppParams.WIDTH, this.width);
		json.put(AppParams.HEIGHT, this.height);
		return json;
	}

	public JsonObject toJsonObject(String baseType) {
		JsonObject json = new JsonObject();
		json.put(AppParams.URL, this.url);
		json.put(AppParams.PRINTABLE_TOP, this.printableTop);
		json.put(AppParams.PRINTABLE_LEFT, this.printableLeft);
		json.put(AppParams.PRINTABLE_WIDTH, this.printableWidth);
		json.put(AppParams.PRINTABLE_HEIGHT, this.printableHeight);
		json.put(AppParams.WIDTH, this.width);
		json.put(AppParams.HEIGHT, this.height);
		json.put(AppParams.COLOR_NAME, this.colorName);
		json.put(AppParams.COLOR_VALUE, this.colorValue);
		json.put(AppParams.BASE_TYPE, baseType);
		return json;
	}

}
