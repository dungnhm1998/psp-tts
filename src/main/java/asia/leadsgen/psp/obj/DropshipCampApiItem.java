package asia.leadsgen.psp.obj;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DropshipCampApiItem {
	@SerializedName("catalog_sku")
	@Expose
	private String sku;

	@SerializedName("quantity")
	@Expose
	private Integer quantity;

}
