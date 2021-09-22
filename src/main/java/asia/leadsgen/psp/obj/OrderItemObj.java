package asia.leadsgen.psp.obj;

public class OrderItemObj {

	private String id;
	private String name;
	private String state;
	private String ffDetailId;
	private String imageUrl;
	private String price;
	private int quantity;
	private String amount;
	private String shippingPrice;
	private String sizeName;

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

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getFfDetailId() {
		return ffDetailId;
	}

	public void setFfDetailId(String ffDetailId) {
		this.ffDetailId = ffDetailId;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getShippingPrice() {
		return shippingPrice;
	}

	public void setShippingPrice(String shippingPrice) {
		this.shippingPrice = shippingPrice;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getSizeName() {
		return sizeName;
	}

	public void setSizeName(String sizeName) {
		this.sizeName = sizeName;
	}

	@Override
	public String toString() {
		return "OrderItemObj [id=" + id + ", name=" + name + ", state=" + state + ", ffDetailId=" + ffDetailId
				+ ", imageUrl=" + imageUrl + ", price=" + price + ", quantity=" + quantity + ", amount=" + amount
				+ ", shippingPrice=" + shippingPrice + ", sizeName=" + sizeName + "]";
	}

}
