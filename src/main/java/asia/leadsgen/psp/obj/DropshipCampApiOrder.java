package asia.leadsgen.psp.obj;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DropshipCampApiOrder extends DropshipApiOrder {
	@SerializedName("items")
	@Expose
	private List<DropshipCustomApiItem> items = null;
}
