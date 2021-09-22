package asia.leadsgen.psp.server.handler.campaign_v2;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsellCampaignModel {
	
	@SerializedName("upsell_campaign_id")
	@Expose
	private String upsellCampaignId;
	
	@SerializedName("upsell_campaign_name")
	@Expose
	private String upsellCampaignName;
	
	@SerializedName("type")
	@Expose
	private String type;
	
	@SerializedName("upsell_variant_id")
	@Expose
	private String upsellVariantId;
	
	@SerializedName("upsell_variant_name")
	@Expose
	private String upsellVariantName;
	
	@SerializedName("upsell_variant_url")
	@Expose
	private String upsellVariantUrl;
	
	@SerializedName("upsell_discount_type")
	@Expose
	private String upsellDiscountType;
	
	@SerializedName("upsell_discount_value")
	@Expose
	private int upsellDiscountValue;
	
	public JsonObject toJson() {
		
		JsonObject i = new JsonObject();
		i.put("upsell_campaign_id", this.upsellCampaignId);
		i.put("upsell_campaign_name", this.upsellCampaignName);
		i.put("type", this.type);
		i.put("upsell_variant_id", this.upsellVariantId);
		i.put("upsell_variant_name", this.upsellVariantName);
		i.put("upsell_variant_url", this.upsellVariantUrl);
		i.put("upsell_discount_type", this.upsellDiscountType);
		i.put("upsell_discount_value", this.upsellDiscountValue);
		return i;
	}

}
