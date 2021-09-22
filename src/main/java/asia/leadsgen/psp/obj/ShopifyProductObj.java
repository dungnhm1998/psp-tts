package asia.leadsgen.psp.obj;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopifyProductObj {
	
	@SerializedName("id")
	@Expose
	private Long id;
	@SerializedName("title")
	@Expose
	private String title;
	@SerializedName("body_html")
	@Expose
	private String bodyHtml;
	@SerializedName("product_type")
	@Expose
	private String productType;
	@SerializedName("handle")
	@Expose
	private String handle;
	@SerializedName("tags")
	@Expose
	private String tags;
	@SerializedName("variants")
	@Expose
	private List<ShopifyVariantObj> variants = null;
	@SerializedName("options")
	@Expose
	private List<ShopifyOptionObj> options = null;
	@SerializedName("images")
	@Expose
	private List<ShopifyImageObj> images = null;
	@SerializedName("variant")
	@Expose
	private ShopifyVariantObj variant = null;
	@SerializedName("image")
	@Expose
	private ShopifyImageObj image = null;
	
//	@Override
//	public String toString() {
//		return "ShopifyProductObj [id=" + id + ", title=" + title + ", productType=" + productType + ", handle="
//				+ handle + ", tags=" + tags + ", variants=" + variants + ", options=" + options + ", images=" + images
//				+ ", variant=" + variant + ", image=" + image + "]";
//	}
	
}
