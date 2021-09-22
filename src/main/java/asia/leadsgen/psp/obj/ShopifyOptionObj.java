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
public class ShopifyOptionObj {
	
	@SerializedName("id")
	@Expose
	private Long id;
	@SerializedName("product_id")
	@Expose
	private Long productId;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("position")
	@Expose
	private Integer position;
	@SerializedName("values")
	@Expose
	private List<String> values;
}
