package asia.leadsgen.psp.obj;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DropshipApiOrder {

	@SerializedName("reference_order_id")
	@Expose
	private String referenceOrderId;

	@SerializedName("shipping_name")
	@Expose
	private String shippingName;
	@SerializedName("shipping_address1")
	@Expose
	private String shippingAddress1;
	@SerializedName("shipping_address2")
	@Expose
	private String shippingAddress2;
	@SerializedName("shipping_city")
	@Expose
	private String shippingCity;
	@SerializedName("shipping_state")
	@Expose
	private String shippingState;
	@SerializedName("shipping_zip")
	@Expose
	private String shippingZip;
	@SerializedName("shipping_country")
	@Expose
	private String shippingCountry;
	@SerializedName("shipping_email")
	@Expose
	private String shippingEmail;
	@SerializedName("shipping_phone")
	@Expose
	private String shippingPhone;

	@SerializedName("sandbox")
	@Expose
	private Boolean sandbox;
	@SerializedName("api_key")
	@Expose
	private String apiKey;

	@SerializedName("ignore_address_check")
	@Expose
	private Boolean ignoreAddressCheck;

}
