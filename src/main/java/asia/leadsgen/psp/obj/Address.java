package asia.leadsgen.psp.obj;

import java.util.HashMap;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;

public class Address {
	private String name;
	private String line1;
	private String line2;
	private String city;
	private String state;
	private String zip;
	private String country;
	private String phone;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Address() {
	}

	public Address(String name, String line1, String line2, String city, String state, String zip, String country,
			String phone) {
		super();
		this.name = name;
		this.line1 = line1;
		this.line2 = line2;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.country = country;
		this.phone = phone;
	}

	@Override
	public String toString() {
		return "Address [name=" + name + ", line1=" + line1 + ", line2=" + line2 + ", city=" + city + ", state=" + state
				+ ", zip=" + zip + ", country=" + country + ", phone=" + phone + "]";
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put(AppParams.LINE1, this.line1);
		map.put(AppParams.LINE2, this.line2);
		map.put(AppParams.CITY, this.city);
		map.put(AppParams.STATE, this.state);
		map.put(AppParams.POSTAL_CODE, this.zip);
		map.put(AppParams.COUNTRY, this.country);
		map.put(AppParams.PHONE, this.phone);
		return map;
	}
}
