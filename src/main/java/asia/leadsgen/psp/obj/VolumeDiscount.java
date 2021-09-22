package asia.leadsgen.psp.obj;

public class VolumeDiscount {
	private String id;
	private int quantity;
	private double value;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public VolumeDiscount() {
		super();
	}

	public VolumeDiscount(String id, int quantity, double value) {
		super();
		this.id = id;
		this.quantity = quantity;
		this.value = value;
	}

	@Override
	public String toString() {
		return "VolumeDiscount [id=" + id + ", quantity=" + quantity + ", value=" + value + "]";
	}

}
