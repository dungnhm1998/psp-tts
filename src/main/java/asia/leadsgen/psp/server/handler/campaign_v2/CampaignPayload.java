package asia.leadsgen.psp.server.handler.campaign_v2;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampaignPayload {
	@SerializedName("campaign")
	@Expose
	private CampaignModel campaign;
	
	@SerializedName("products")
	@Expose
	private List<ProductModel> products;	
	
	@SerializedName("product")
	@Expose
	private ProductModel product;
	
	@SerializedName("store_id")
	@Expose
	private String storeId;
}
