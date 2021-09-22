package asia.leadsgen.psp.obj;

public class CMSCreateLabelResult {

	private boolean isSuccess;
	private String id;
	private String carrier;
	private String trackingCode;
	private String trackingUrl;
	private String url;
	private String ffDetailId;

	private LabelFileObj labelFile;

	public LabelFileObj getLabelFile() {
		return labelFile;
	}

	public void setLabelFile(LabelFileObj labelFile) {
		this.labelFile = labelFile;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCarrier() {
		return carrier;
	}

	public void setCarrier(String carrier) {
		this.carrier = carrier;
	}

	public String getTrackingCode() {
		return trackingCode;
	}

	public void setTrackingCode(String trackingCode) {
		this.trackingCode = trackingCode;
	}

	public String getTrackingUrl() {
		return trackingUrl;
	}

	public void setTrackingUrl(String trackingUrl) {
		this.trackingUrl = trackingUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFfDetailId() {
		return ffDetailId;
	}

	public void setFfDetailId(String ffDetailId) {
		this.ffDetailId = ffDetailId;
	}

	@Override
	public String toString() {
		return "CMSCreateLabelResult [isSuccess=" + isSuccess + ", id=" + id + ", carrier=" + carrier
				+ ", trackingCode=" + trackingCode + ", trackingUrl=" + trackingUrl + ", url=" + url + "]";
	}

}
