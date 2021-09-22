package asia.leadsgen.psp.obj;

public class InvoiceItem {

	private String id;
	public String name;
	private String date;
	private int quantity;
	private int sides;
	private Double price;
	private Double amount;
	private String invoiceNumber;
	private String partnerId;
	private Double total;

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

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Double getAmount() {
		return price * quantity;
	}

	public int getSides() {
		return sides;
	}

	public void setSides(int sides) {
		this.sides = sides;
	}

	public InvoiceItem(String id, String name, String date, int quantity, int sides, Double price, Double amount) {
		super();
		this.id = id;
		this.name = name;
		this.date = date;
		this.quantity = quantity;
		this.sides = sides;
		this.price = price;
		this.amount = amount;
	}

	public InvoiceItem() {

	}

	public InvoiceItem(String id, String name, String date, int quantity, int sides, Double price, Double amount,
			String invoiceNumber, String partnerId) {
		super();
		this.id = id;
		this.name = name;
		this.date = date;
		this.quantity = quantity;
		this.sides = sides;
		this.price = price;
		this.amount = amount;
		this.invoiceNumber = invoiceNumber;
		this.partnerId = partnerId;
	}

	public void addQuantity(int quantity) {
		this.quantity += quantity;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public String getPartnerId() {
		return partnerId;
	}

	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}

	public Double getTotal() {
		return total;
	}

	public void setTotal(Double total) {
		this.total = total;
	}
	
}
