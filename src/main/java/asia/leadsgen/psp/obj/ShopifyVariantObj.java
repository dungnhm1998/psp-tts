package asia.leadsgen.psp.obj;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Getter
@Setter
@ToString
public class ShopifyVariantObj {
	@SerializedName("id")
	@Expose
	private Long id;
	@SerializedName("product_id")
	@Expose
	private Long productId;
	@SerializedName("title")
	@Expose
	private String title;
	@SerializedName("price")
	@Expose
	private String price;
	@SerializedName("sku")
	@Expose
	private String sku;
	@SerializedName("position")
	@Expose
	private Integer position;
	@SerializedName("option1")
	@Expose
	private String option1;
	@SerializedName("option2")
	@Expose
	private String option2;
	@SerializedName("option3")
	@Expose
	private String option3;
	@SerializedName("image_id")
	@Expose
	private Long imageId;
}
