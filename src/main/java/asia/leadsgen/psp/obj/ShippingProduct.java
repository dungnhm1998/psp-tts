package asia.leadsgen.psp.obj;

import java.io.Serializable;

public class ShippingProduct implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;
	private String name;
	private String baseId;
	private String size;
	private double value;

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

	public String getBaseId() {
		return baseId;
	}

	public void setBaseId(String baseId) {
		this.baseId = baseId;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "ShippingProduct [id=" + id + ", name=" + name + ", baseId=" + baseId + ", size=" + size + ", value="
				+ value + "]";
	}
}
