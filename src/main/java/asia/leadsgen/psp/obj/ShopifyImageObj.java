package asia.leadsgen.psp.obj;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Getter
@Setter
@ToString
public class ShopifyImageObj {
	
	@SerializedName("id")
	@Expose
	private Long id;
	@SerializedName("product_id")
	@Expose
	private Long productId;
	@SerializedName("position")
	@Expose
	private Integer position;
	@SerializedName("width")
	@Expose
	private Integer width;
	@SerializedName("height")
	@Expose
	private Integer height;
	@SerializedName("src")
	@Expose
	private String src;
	@SerializedName("variant_ids")
	@Expose
	private List<Long> variantIds;
}
