package asia.leadsgen.psp.server.handler.dropship.order;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DropshipApiCancelOrderRequest {
	@SerializedName("order_id")
	@Expose
	private String orderId;
	@SerializedName("sandbox")
	@Expose
	private Boolean sandbox;
	@SerializedName("api_key")
	@Expose
	private String apiKey;
}
