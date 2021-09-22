package asia.leadsgen.psp.server.handler.campaign_v2;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampaignModel {
	
	@SerializedName("id")
	@Expose
	private String id;
	
	@SerializedName("user_id")
	@Expose
	private String userId;
	
	@SerializedName("base_group_id")
	@Expose
	private String baseGroupId;
	
	@SerializedName("title")
	@Expose
	private String title;
	
	@SerializedName("description")
	@Expose
	private String description;
	
	@SerializedName("domain")
	@Expose
	private String domain;
	
	@SerializedName("domain_id")
	@Expose
	private String domainId;
	
	@SerializedName("url")
	@Expose
	private String url;
	
//	@SerializedName("design_front_url")
//	@Expose
//	private String designFrontUrl;
//	
//	@SerializedName("design_back_url")
//	@Expose
//	private String designBackUrl;
	
//	@SerializedName("back_view")
//	@Expose
//	private boolean isBackView;
	
	@SerializedName("private")
	@Expose
	private boolean isPrivate;
	
	@SerializedName("stores")
	@Expose
	private String stores;
	
	@SerializedName("categories")
	@Expose
	private String categories;
	
	@SerializedName("tags")
	@Expose
	private String tags;
	
	@SerializedName("collections")
	@Expose
	private String collections;
	
//	@SerializedName("start_time")
//	@Expose
//	private String startTime;
//
//	@SerializedName("end_time")
//	@Expose
//	private String endTime;
//	
//	@SerializedName("relaunch")
//	@Expose
//	private boolean isAutoRelaunch;

	@SerializedName("gg_pixel")
	@Expose
	private String ggPixel;
	
	@SerializedName("fb_pixel")
	@Expose
	private String fbPixel;
	
	@SerializedName("seo_desc")
	@Expose
	private String seoDesc;
	
	@SerializedName("seo_image_cover")
	@Expose
	private String seoImageCover;
	
	@SerializedName("seo_title")
	@Expose
	private String seoTitle;
	
//	@SerializedName("timezone")
//	@Expose
//	private String timezone;
	
	@SerializedName("state")
	@Expose
	private String state;
	
	@SerializedName("upsell_campaigns")
	@Expose
	private List<UpsellCampaignModel> upsellCampaigns;

	@Override
	public String toString() {
		return "CampaignModel [id=" + id + ", userId=" + userId + ", baseGroupId=" + baseGroupId + ", title=" + title
				+ ", description=" + description + ", domain=" + domain + ", domainId=" + domainId + ", url=" + url
				+ ", isPrivate=" + isPrivate + ", stores=" + stores + ", categories=" + categories + ", ggPixel="
				+ ggPixel + ", fbPixel=" + fbPixel + ", seoDesc=" + seoDesc + ", seoImageCover=" + seoImageCover
				+ ", seoTitle=" + seoTitle + ", state=" + state + ", upsellCampaigns=" + upsellCampaigns + "]";
	}
	
	public JsonObject toJson() {
		
		JsonObject obj = new JsonObject();
		obj.put("id", this.id);
		obj.put("user_id", this.userId);
		obj.put("base_group_id", this.baseGroupId);
		obj.put("title", this.title);
		obj.put("description", this.description);
		obj.put("domain", this.domain);
		obj.put("domain_id", this.domainId);
		obj.put("url", this.url);
		obj.put("private", this.userId);
		obj.put("stores", this.stores);
		obj.put("categories", this.categories);
		obj.put("tags", this.tags);
		obj.put("collections", this.collections);
		obj.put("gg_pixel", this.ggPixel);
		obj.put("fb_pixel", this.fbPixel);
		obj.put("seo_desc", this.seoDesc);
		obj.put("seo_image_cover", this.seoImageCover);
		obj.put("seo_title", this.seoTitle);
		obj.put("state", this.state);
		
		JsonArray upCampArr = new JsonArray();
		for (UpsellCampaignModel i : this.upsellCampaigns) {
			upCampArr.add(i.toJson());
		}
		obj.put("upsell_campaigns", upCampArr);
		
		return obj;
	}
}

