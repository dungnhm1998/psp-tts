package asia.leadsgen.psp.obj;

public class CustomerEmail {
	private String email;
	private String name;
	private String orderDate;
	private String campaignDomain;
	private String campaignUrl;
	private String campaignTags;
	private String orderQuantity;
	private String orderPrice;
	private String deviceType;
	private String country;
	private String state;
	
	public CustomerEmail() {

	}

	public CustomerEmail(String email, String name, String orderDate, String campaignDomain, String campaignUrl,
			String campaignTags, String orderQuantity, String orderPrice, String deviceType, String country,
			String state) {
		super();
		this.email = email;
		this.name = name;
		this.orderDate = orderDate;
		this.campaignDomain = campaignDomain;
		this.campaignUrl = campaignUrl;
		this.campaignTags = campaignTags;
		this.orderQuantity = orderQuantity;
		this.orderPrice = orderPrice;
		this.deviceType = deviceType;
		this.country = country;
		this.state = state;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(String orderDate) {
		this.orderDate = orderDate;
	}

	public String getCampaignDomain() {
		return campaignDomain;
	}

	public void setCampaignDomain(String campaignDomain) {
		this.campaignDomain = campaignDomain;
	}

	public String getCampaignUrl() {
		return campaignUrl;
	}

	public void setCampaignUrl(String campaignUrl) {
		this.campaignUrl = campaignUrl;
	}

	public String getCampaignTags() {
		return campaignTags;
	}

	public void setCampaignTags(String campaignTags) {
		this.campaignTags = campaignTags;
	}

	public String getOrderQuantity() {
		return orderQuantity;
	}

	public void setOrderQuantity(String orderQuantity) {
		this.orderQuantity = orderQuantity;
	}

	public String getOrderPrice() {
		return orderPrice;
	}

	public void setOrderPrice(String orderPrice) {
		this.orderPrice = orderPrice;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return "CustomerEmailObj [email=" + email + ", name=" + name + "]";
	}

}
