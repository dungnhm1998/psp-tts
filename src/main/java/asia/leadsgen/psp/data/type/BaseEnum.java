package asia.leadsgen.psp.data.type;

public enum BaseEnum {

	SHIRT_LONG_SLEEVE("---2BfBws3P6wKDO"), 
	SHIRT_BASIC_TEE("-cwc847TMWTaZmVg"), 
	SHIRT_SWEATSHIRT("0rtJZfvO_eqaNYEg"),
	SHIRT_WOMENS_V_NECK("3t3H83mHJ-_1yQUz"), 
	SHIRT_V_NECK("8d_QKhXD-86igKPP"),
	SHIRT_WOMENS_TANK_TOP("9BESdMiawozq3yyX"), 
	SHIRT_TANK_TOP("DpyfFZQ04G-jN_pL"), 
	SHIRT_HOODIE("KfXL7aOF8wUOJZyH"),
	SHIRT_WOMENS_PREMIUM_TEE("N289LB_XPeJhkWt4"), 
	SHIRT_PREMIUM_TEE("Nhy-R_esvYxrhdCX"),
	SHIRT_KIDS_BASIC_TEE("WS-CFAJxOx1edtrB"), 
	SHIRT_KIDS_HOODIE("aneISamhDEoT39oy"),
	SHIRT_WOMENS_BASIC_TEE("tL9HoLtLPWpM1Mz4"), 
	SHIRT_KIDS_SWEATSHIRT("zBSADWJIqNXuc-HB");

	private String value;

	private BaseEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
