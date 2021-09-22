package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;

public class PartnerPayoutObj implements Serializable {
	private static final long serialVersionUID = -9119912970502910294L;

	private String id;

	private String partnerId;

	private String dateCreate;

	private String dateUpdate;

	private String dateStart;

	private String dateEnd;

	private String desc;

	private String amount;

	private String invoiceUrl;

	private String state;

	private String paidUserId;

	private String paidUserName;

	private String datePaid;

	private String payType;

	private String invoiceNumber;

	private String adjustment;

	private String adjustmentDesc;

	private String dateLastRun;

	public static PartnerPayoutObj fromMap(Map<String, Object> input) {
		PartnerPayoutObj obj = new PartnerPayoutObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setPartnerId(ParamUtil.getString(input, AppParams.S_PARTNER_ID));
		obj.setDateCreate(ParamUtil.getString(input, AppParams.D_CREATE));
		obj.setDateUpdate(ParamUtil.getString(input, AppParams.D_UPDATE));
		obj.setDateStart(ParamUtil.getString(input, AppParams.D_START));
		obj.setDateEnd(ParamUtil.getString(input, AppParams.D_END));
		obj.setDesc(ParamUtil.getString(input, AppParams.S_DESC));
		obj.setAmount(ParamUtil.getString(input, AppParams.S_AMOUNT));
		obj.setInvoiceUrl(ParamUtil.getString(input, AppParams.S_INVOICE_URL));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setPaidUserId(ParamUtil.getString(input, AppParams.S_PAID_USER_ID));
		obj.setPaidUserName(ParamUtil.getString(input, AppParams.S_PAID_USER_NAME));
		obj.setDatePaid(ParamUtil.getString(input, AppParams.D_PAID));
		obj.setPayType(ParamUtil.getString(input, AppParams.S_PAY_TYPE));
		obj.setInvoiceNumber(ParamUtil.getString(input, AppParams.S_INVOICE_NUMBER));
		obj.setAdjustment(ParamUtil.getString(input, AppParams.S_ADJUSTMENT));
		obj.setAdjustmentDesc(ParamUtil.getString(input, AppParams.S_ADJUSTMENT_DESC));
		obj.setDateLastRun(ParamUtil.getString(input, AppParams.D_LAST_RUN));
		return obj;
	}

	public String getId() {
		return id;
	}

	public String getPartnerId() {
		return partnerId;
	}

	public String getDateCreate() {
		return dateCreate;
	}

	public String getDateUpdate() {
		return dateUpdate;
	}

	public String getDateStart() {
		return dateStart;
	}

	public String getDateEnd() {
		return dateEnd;
	}

	public String getDesc() {
		return desc;
	}

	public String getAmount() {
		return amount;
	}

	public String getInvoiceUrl() {
		return invoiceUrl;
	}

	public String getState() {
		return state;
	}

	public String getPaidUserId() {
		return paidUserId;
	}

	public String getPaidUserName() {
		return paidUserName;
	}

	public String getDatePaid() {
		return datePaid;
	}

	public String getPayType() {
		return payType;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public String getAdjustment() {
		return adjustment;
	}

	public String getAdjustmentDesc() {
		return adjustmentDesc;
	}

	public String getDateLastRun() {
		return dateLastRun;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}

	public void setDateCreate(String dateCreate) {
		this.dateCreate = dateCreate;
	}

	public void setDateUpdate(String dateUpdate) {
		this.dateUpdate = dateUpdate;
	}

	public void setDateStart(String dateStart) {
		this.dateStart = dateStart;
	}

	public void setDateEnd(String dateEnd) {
		this.dateEnd = dateEnd;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public void setInvoiceUrl(String invoiceUrl) {
		this.invoiceUrl = invoiceUrl;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setPaidUserId(String paidUserId) {
		this.paidUserId = paidUserId;
	}

	public void setPaidUserName(String paidUserName) {
		this.paidUserName = paidUserName;
	}

	public void setDatePaid(String datePaid) {
		this.datePaid = datePaid;
	}

	public void setPayType(String payType) {
		this.payType = payType;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public void setAdjustment(String adjustment) {
		this.adjustment = adjustment;
	}

	public void setAdjustmentDesc(String adjustmentDesc) {
		this.adjustmentDesc = adjustmentDesc;
	}

	public void setDateLastRun(String dateLastRun) {
		this.dateLastRun = dateLastRun;
	}

	@Override
	public String toString() {
		return "PartnerPayoutObj [id=" + id + ", partnerId=" + partnerId + ", dateCreate=" + dateCreate
				+ ", dateUpdate=" + dateUpdate + ", dateStart=" + dateStart + ", dateEnd=" + dateEnd + ", desc=" + desc
				+ ", amount=" + amount + ", invoiceUrl=" + invoiceUrl + ", state=" + state + ", paidUserId="
				+ paidUserId + ", paidUserName=" + paidUserName + ", datePaid=" + datePaid + ", payType=" + payType
				+ ", invoiceNumber=" + invoiceNumber + ", adjustment=" + adjustment + ", adjustmentDesc="
				+ adjustmentDesc + ", dateLastRun=" + dateLastRun + "]";
	}

}