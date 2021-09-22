package asia.leadsgen.psp.server.handler.campaign_v2;

import java.io.Serializable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColorModel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5198525177599903035L;

	@SerializedName("id")
	@Expose
	private String id;
	
	@SerializedName("default")
	@Expose
	private boolean isDefault;

}
