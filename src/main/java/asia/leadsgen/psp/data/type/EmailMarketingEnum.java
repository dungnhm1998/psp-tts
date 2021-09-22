package asia.leadsgen.psp.data.type;

public enum EmailMarketingEnum {
	
	FINISH_CREATE_LABEL("finish_create_label_notify"),
	ORDER_SHIPPING_JOB("order_shipping_job"),
	PLAIN_TEXT_ORDER_CONFIRM("plain_text_order_confirm"),
	PLAIN_TEXT_ORDER_SHIPPED("plain_text_order_shipped"),
	SHOPIFY_CHARGE_FAIL("shopify_charge_fail");
	
	private String value;

	private EmailMarketingEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
