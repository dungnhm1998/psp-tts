package asia.leadsgen.psp.obj;

import java.io.Serializable;

public class ShippingOwnerObj implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;
	private String company;
	private String phone;
	private String country;
	private String addState;
	private String postalCode;
	private String city;
	private String addLine1;
	private String addLine2;
	private String state;
	private String dCreate;
	private String dUpdate;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getAddState() {
		return addState;
	}

	public void setAddState(String addState) {
		this.addState = addState;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAddLine1() {
		return addLine1;
	}

	public void setAddLine1(String addLine1) {
		this.addLine1 = addLine1;
	}

	public String getAddLine2() {
		return addLine2;
	}

	public void setAddLine2(String addLine2) {
		this.addLine2 = addLine2;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getdCreate() {
		return dCreate;
	}

	public void setdCreate(String dCreate) {
		this.dCreate = dCreate;
	}

	public String getdUpdate() {
		return dUpdate;
	}

	public void setdUpdate(String dUpdate) {
		this.dUpdate = dUpdate;
	}

	@Override
	public String toString() {
		return "ShippingOwnerObj [id=" + id + ", company=" + company + ", phone=" + phone + ", country=" + country
				+ ", addState=" + addState + ", postalCode=" + postalCode + ", city=" + city + ", addLine1=" + addLine1
				+ ", addLine2=" + addLine2 + ", state=" + state + ", dCreate=" + dCreate + ", dUpdate=" + dUpdate + "]";
	}

}
