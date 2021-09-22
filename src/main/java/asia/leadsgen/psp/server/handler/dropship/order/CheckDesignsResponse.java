package asia.leadsgen.psp.server.handler.dropship.order;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckDesignsResponse {
	private int code;
	private String message;
	@SerializedName("is_valid")
	@Expose
	private Boolean isValid;

	@SerializedName("design_back")
	@Expose
	private DesignBack designBack;

	@SerializedName("design_front")
	@Expose
	private DesignFront designFront;

	@Getter
	@Setter
	class DesignFront {

		@SerializedName("is_valid")
		@Expose
		private Boolean isValid;
		@SerializedName("description")
		@Expose
		private String description;
		@SerializedName("is_success_download")
		@Expose
		private Boolean isSuccessDownload;
		@SerializedName("md5_checksum")
		@Expose
		private String md5Checksum;
		@SerializedName("url")
		@Expose
		private String url;

	}

	@Getter
	@Setter
	class DesignBack {

		@SerializedName("is_valid")
		@Expose
		private Boolean isValid;
		@SerializedName("description")
		@Expose
		private String description;
		@SerializedName("is_success_download")
		@Expose
		private Boolean isSuccessDownload;
		@SerializedName("md5_checksum")
		@Expose
		private String md5Checksum;
		@SerializedName("url")
		@Expose
		private String url;

	}
}
