package asia.leadsgen.psp.obj;

public class Shipping {

	String id;
	String name;
	String email;
	String phone;
	String line1;
	String line2;
	String city;
	String state;
	String postalCode;
	String countryCode;
	String countryName;
	int shipAsGift;
	
	public Shipping() {}
	
	public Shipping(String id, String name, String email, String phone, String line1, String line2, String city,
			String state, String postalCode, String countryCode, String countryName, int shipAsGift) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.line1 = line1;
		this.line2 = line2;
		this.city = city;
		this.state = state;
		this.postalCode = postalCode;
		this.countryCode = countryCode;
		this.countryName = countryName;
		this.shipAsGift = shipAsGift;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getLine1() {
		return line1;
	}

	public void setLine1(String line1) {
		this.line1 = line1;
	}

	public String getLine2() {
		return line2;
	}

	public void setLine2(String line2) {
		this.line2 = line2;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getCountryName() {
		return countryName;
	}

	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}

	public int getShipAsGift() {
		return shipAsGift;
	}

	public void setShipAsGift(int shipAsGift) {
		this.shipAsGift = shipAsGift;
	}

	@Override
	public String toString() {
		return "Shipping [id=" + id + ", name=" + name + ", email=" + email + ", phone=" + phone + ", line1=" + line1
				+ ", line2=" + line2 + ", city=" + city + ", state=" + state + ", postalCode=" + postalCode
				+ ", countryCode=" + countryCode + ", countryName=" + countryName + ", shipAsGift=" + shipAsGift + "]";
	}
	
}
