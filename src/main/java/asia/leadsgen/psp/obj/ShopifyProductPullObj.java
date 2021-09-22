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
public class ShopifyProductPullObj {
	@SerializedName("products")
	@Expose
	private List<ShopifyProductObj> products = null;
	
	@SerializedName("product")
	@Expose
	private ShopifyProductObj product = null;

	@Override
	public String toString() {
		return "ShopifyProductPullObj [products=" + products + ", product=" + product + "]";
	}
	
	
}
