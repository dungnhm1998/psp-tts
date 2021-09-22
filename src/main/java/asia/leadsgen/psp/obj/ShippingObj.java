package asia.leadsgen.psp.obj;

public class ShippingObj {
	private String id;
	private String customerName;
	private String customerPhone;
	private String customerEmail;
	private boolean gift;
	private ShippingAddressObj shippingAddress;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getCustomerPhone() {
		return customerPhone;
	}

	public void setCustomerPhone(String customerPhone) {
		this.customerPhone = customerPhone;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public void setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
	}

	public boolean isGift() {
		return gift;
	}

	public void setGift(boolean gift) {
		this.gift = gift;
	}

	public ShippingAddressObj getShippingAddress() {
		return shippingAddress;
	}

	public void setShippingAddress(ShippingAddressObj shippingAddress) {
		this.shippingAddress = shippingAddress;
	}

}
