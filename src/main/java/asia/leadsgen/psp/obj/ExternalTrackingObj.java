package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;

public class ExternalTrackingObj implements Serializable {
	private static final long serialVersionUID = -5451591172852138661L;
	private String id;
	private String packageId;
	private String referenceId;
	private String vendor;
	private String state;
	
	public ExternalTrackingObj() {}
	
	public ExternalTrackingObj(String id, String packageId, String referenceId, String vendor, String state) {
		super();
		this.id = id;
		this.packageId = packageId;
		this.referenceId = referenceId;
		this.vendor = vendor;
		this.state = state;
	}

	public static ExternalTrackingObj fromMap(Map<String, Object> input) {
		ExternalTrackingObj obj = new ExternalTrackingObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setPackageId(ParamUtil.getString(input, AppParams.S_PACKAGE_ID));
		obj.setReferenceId(ParamUtil.getString(input, AppParams.S_REFERENCE_ID));
		obj.setVendor(ParamUtil.getString(input, AppParams.S_VENDOR));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		return obj;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void setPackageId(String packageId) {
		this.packageId = packageId;
	}

	public String getPackageId() {
		return this.packageId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public String getReferenceId() {
		return this.referenceId;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getVendor() {
		return this.vendor;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getState() {
		return this.state;
	}
}
