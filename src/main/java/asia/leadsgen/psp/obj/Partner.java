package asia.leadsgen.psp.obj;

public class Partner {

	private String id;
	private String name;
//	private String shop;
	private String addressLine1;
	private String city;
	private String state;
	private String country;
	private String duedate;
	private String bank;
	private String bankAccount;
	private String bankAddress;
	private String swiftCode;
	private String paypalEmail;
	private String paypalFirstName;
	private String paypalLastName;
	private String payoneerEmail;
	private String defaultPayment;

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
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

//	public String getShop() {
//		return shop;
//	}
//
//	public void setShop(String shop) {
//		this.shop = shop;
//	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getDuedate() {
		return duedate;
	}

	public void setDuedate(String duedate) {
		this.duedate = duedate;
	}

	public String getBank() {
		return bank;
	}

	public void setBank(String bank) {
		this.bank = bank;
	}

	public String getBankAccount() {
		return bankAccount;
	}

	public void setBankAccount(String bankAccount) {
		this.bankAccount = bankAccount;
	}

	public String getBankAddress() {
		return bankAddress;
	}

	public void setBankAddress(String bankAddress) {
		this.bankAddress = bankAddress;
	}

	public String getSwiftCode() {
		return swiftCode;
	}

	public void setSwiftCode(String swiftCode) {
		this.swiftCode = swiftCode;
	}

	public String getPaypalEmail() {
		return paypalEmail;
	}

	public void setPaypalEmail(String paypalEmail) {
		this.paypalEmail = paypalEmail;
	}

	public String getPaypalFirstName() {
		return paypalFirstName;
	}

	public void setPaypalFirstName(String paypalFirstName) {
		this.paypalFirstName = paypalFirstName;
	}

	public String getPaypalLastName() {
		return paypalLastName;
	}

	public void setPaypalLastName(String paypalLastName) {
		this.paypalLastName = paypalLastName;
	}

	public String getPayoneerEmail() {
		return payoneerEmail;
	}

	public void setPayoneerEmail(String payoneerEmail) {
		this.payoneerEmail = payoneerEmail;
	}

	public String getDefaultPayment() {
		return defaultPayment;
	}

	public void setDefaultPayment(String defaultPayment) {
		this.defaultPayment = defaultPayment;
	}

	public Partner() {

	}

	public Partner(String id, String name, String addressLine1, String city, String state, String country,
			String duedate, String bank, String bankAccount, String bankAddress, String swiftCode, String paypalEmail,
			String paypalFirstName, String paypalLastName, String payoneerEmail, String defaultPayment) {
		super();
		this.id = id;
		this.name = name;
		this.addressLine1 = addressLine1;
		this.city = city;
		this.state = state;
		this.country = country;
		this.duedate = duedate;
		this.bank = bank;
		this.bankAccount = bankAccount;
		this.bankAddress = bankAddress;
		this.swiftCode = swiftCode;
		this.paypalEmail = paypalEmail;
		this.paypalFirstName = paypalFirstName;
		this.paypalLastName = paypalLastName;
		this.payoneerEmail = payoneerEmail;
		this.defaultPayment = defaultPayment;
	}

	static public Partner champyPrint() {
		Partner partner = new Partner("qKWWXsDnKbBJgaIe", "Champy", "", "", "", "", "", "", "", "", "", "", "", "", "",
				"");
		return partner;
	}

}
