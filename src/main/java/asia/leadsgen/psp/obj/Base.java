package asia.leadsgen.psp.obj;

public class Base {
	private String id;
	private String name;
	private String type;
	private String code;

	public Base(String name, String type, String code) {
		super();
		this.name = name;
		this.type = type;
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
