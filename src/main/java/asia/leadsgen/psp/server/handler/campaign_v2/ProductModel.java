package asia.leadsgen.psp.server.handler.campaign_v2;

import java.io.Serializable;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4360902945655818480L;

	@SerializedName("base_id")
	@Expose
	private String baseId;
	
	@SerializedName("id")
	@Expose
	private String id;
	
	@SerializedName("ref_id")
	@Expose
	private String ref_id;
	
//	@SerializedName("name")
//	@Expose
//	private String name;
	
	@SerializedName("position")
	@Expose
	private int position;
	
	@SerializedName("back_view")
	@Expose
	private boolean isBackView;
	
	@SerializedName("default")
	@Expose
	private boolean isDefault;
	
	@SerializedName("sizes")
	@Expose
	private List<SizeModel> sizes;
	
	@SerializedName("allSizes")
	@Expose
	private String allSizes;
	
	@SerializedName("colors")
	@Expose
	private List<ColorModel> colors;
	
	@SerializedName("allColors")
	@Expose
	private String allColors;
	
	@SerializedName("defaultColorId")
	@Expose
	private String defaultColorId;
	
	@SerializedName("sale_expected")
	@Expose
	private int sale_expected;
	
	@SerializedName("sale_price")
	@Expose
	private double sale_price;
	
	@SerializedName("designs")
	@Expose
	private List<DesignModel> designs;

	@Override
	public String toString() {
		return "ProductModel [baseId=" + baseId + ", id=" + id + ", ref_id=" + ref_id + ", isBackView=" + isBackView
				+ ", isDefault=" + isDefault + ", sale_expected=" + sale_expected + ", sale_price=" + sale_price
				+ ", designs=" + designs + "]";
	}
	
}
