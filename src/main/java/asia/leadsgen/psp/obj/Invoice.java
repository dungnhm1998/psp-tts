package asia.leadsgen.psp.obj;

import java.util.List;

public class Invoice {

	private Partner partner;
	private String invoiceNumber;
	private String adjustReason;
	private Double subtotal;
	private Double adjustments;
	private Double total;
	private List<InvoiceItem> items;
	private List<InvoiceItem> groupedItems;
	private int totalItem;
	private String note;

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
		if (this.items != null && this.items.isEmpty() == false) {
			for (InvoiceItem item : items) {
				item.setInvoiceNumber(invoiceNumber);
			}

		}
	}

	public Double getSubtotal() {
		return subtotal;
	}

	public void setSubtotal(Double subtotal) {
		this.subtotal = subtotal;
	}

	public Double getAdjustments() {
		return adjustments;
	}

	public void setAdjustments(Double adjustments) {
		this.adjustments = adjustments;
	}

	public Double getTotal() {
		return total;
	}

	public void setTotal(Double total) {
		this.total = total;
	}

	public List<InvoiceItem> getItems() {
		return items;
	}

	public void setItems(List<InvoiceItem> items) {
		this.items = items;
	}

	public int getTotalItem() {
		return totalItem;
	}

	public void setTotalItem(int totalItem) {
		this.totalItem = totalItem;
	}

	public String getStartDate() {
		String date = "";
		if (this.items != null && this.items.isEmpty() == false) {
			date = this.items.get(0).getDate();
		}
		return date;
	}

	public String getEndDate() {
		String date = "";
		if (this.items != null && this.items.isEmpty() == false) {
			date = this.items.get(this.items.size() - 1).getDate();
		}
		return date;
	}

	public Partner getPartner() {
		return partner;
	}

	public void setPartner(Partner partner) {
		this.partner = partner;
		if (this.items != null && this.items.isEmpty() == false) {
			for (InvoiceItem i : this.items) {
				i.setPartnerId(partner.getId());
			}
		}
	}

	public List<InvoiceItem> getGroupedItems() {
		return groupedItems;
	}

	public void setGroupedItems(List<InvoiceItem> groupedItems) {
		this.groupedItems = groupedItems;
	}

	public String getAdjustReason() {
		return adjustReason;
	}

	public void setAdjustReason(String adjustReason) {
		this.adjustReason = adjustReason;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
	
}
