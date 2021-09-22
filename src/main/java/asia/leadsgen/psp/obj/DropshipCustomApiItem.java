package asia.leadsgen.psp.obj;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Setter;

import lombok.Getter;

@Getter
@Setter
public class DropshipCustomApiItem {
	@SerializedName("catalog_sku")
	@Expose
	private String sku;
	@SerializedName("design_url_front")
	@Expose
	private String designUrlFront;

	@SerializedName("mockup_url_front")
	@Expose
	private String mockupUrlFront;

	@SerializedName("design_url_back")
	@Expose
	private String designUrlBack;

	@SerializedName("mockup_url_back")
	@Expose
	private String mockupUrlBack;

	@SerializedName("quantity")
	@Expose
	private Integer quantity;
	
	private String md5CampaignId;

}
