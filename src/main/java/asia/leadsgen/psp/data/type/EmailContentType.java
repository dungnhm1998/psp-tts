package asia.leadsgen.psp.data.type;

public enum EmailContentType {
	
	IMAGE("image"),
	NO_IMAGE("no_image");
	
	private String value;

	private EmailContentType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
