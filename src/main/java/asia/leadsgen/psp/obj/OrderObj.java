package asia.leadsgen.psp.obj;

public class OrderObj {

	private String id;
	private String updateDate;
	private String state;
	private String trackingCode;
	private String orderAmount;
	private ShippingObj shipping;
	private DomainObj domainObj;
	private PaymentObj paymentObj;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(String updateDate) {
		this.updateDate = updateDate;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getTrackingCode() {
		return trackingCode;
	}

	public void setTrackingCode(String trackingCode) {
		this.trackingCode = trackingCode;
	}

	public ShippingObj getShipping() {
		return shipping;
	}

	public void setShipping(ShippingObj shipping) {
		this.shipping = shipping;
	}

	public DomainObj getDomainObj() {
		return domainObj;
	}

	public void setDomainObj(DomainObj domainObj) {
		this.domainObj = domainObj;
	}

	public PaymentObj getPaymentObj() {
		return paymentObj;
	}

	public void setPaymentObj(PaymentObj paymentObj) {
		this.paymentObj = paymentObj;
	}

	public String getOrderAmount() {
		return orderAmount;
	}

	public void setOrderAmount(String orderAmount) {
		this.orderAmount = orderAmount;
	}

	@Override
	public String toString() {
		return "OrderObj [id=" + id + ", updateDate=" + updateDate + ", state=" + state + ", trackingCode="
				+ trackingCode + ", orderAmount=" + orderAmount + ", shipping=" + shipping + ", domainObj=" + domainObj
				+ ", paymentObj=" + paymentObj + "]";
	}

}
