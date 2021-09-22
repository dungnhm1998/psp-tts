package asia.leadsgen.psp.obj;

public class DomainObj {

	private String id;
	private String sld;
	private String tld;
	private String name;
	private String logo;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSld() {
		return name != null && !name.isEmpty() ? name.substring(0, name.lastIndexOf('.')) : null;
	}

	public void setSld(String sld) {
		this.sld = sld;
	}

	public String getTld() {
		return name != null && !name.isEmpty() ? name.substring(name.lastIndexOf('.'), name.length()) : null;
	}

	public void setTld(String tld) {
		this.tld = tld;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	@Override
	public String toString() {
		return "DomainObj [id=" + id + ", sld=" + sld + ", tld=" + tld + ", name=" + name + ", logo=" + logo + "]";
	}

}
