package asia.leadsgen.psp.server.handler.campaign_v2;

import java.io.Serializable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SizeModel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1360810704661715609L;

	@SerializedName("id")
	@Expose
	private String id;
	
	@SerializedName("name")
	@Expose
	private String name;
	
	@SerializedName("sale_price")
	@Expose
	private double sale_price;

}
