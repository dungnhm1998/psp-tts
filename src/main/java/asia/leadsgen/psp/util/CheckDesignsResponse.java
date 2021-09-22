package asia.leadsgen.psp.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckDesignsResponse {

	@SerializedName("url")
	@Expose
	private String url;
	@SerializedName("url_laser")
	@Expose
	private String urlLaser;
	@SerializedName("has_laser")
	@Expose
	private Integer hasLaser;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrlLaser() {
		return urlLaser;
	}

	public void setUrlLaser(String urlLaser) {
		this.urlLaser = urlLaser;
	}

	public Integer getHasLaser() {
		return hasLaser;
	}

	public void setHasLaser(Integer hasLaser) {
		this.hasLaser = hasLaser;
	}
}
