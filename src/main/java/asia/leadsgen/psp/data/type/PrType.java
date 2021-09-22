package asia.leadsgen.psp.data.type;

public enum PrType {

	VOLUME_DISCOUNT("c1gs-6364-8888"), 
	COUPON_FREESHIP("FkvNUu9Zcab-O9UL"),
	COUPON_DISCOUNT("24yQLTr5bg5f5Cor"),
	THRESHOLD_FREESHIP("fac1-4374-8a37");

	private String value;

	private PrType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
