package asia.leadsgen.psp.obj;

import java.util.Map;

import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ShippingFeeObj {
	static final String _2D_GROUP = "83311958385839942518";
	static final String _3D_GROUP = "22571365878458529108";

	private String id;
	private String baseId;
	private String baseName;
	private String countryCode;
	private Double price;
	private Double addingPrice;
	private String currency;
	private String state;
	private String groupId;
	private Double dropshipPrice;
	private Double dropshipAddingPrice;
	private Double expressPrice;
	private Double expressAddingPrice;
	private String taxAmount = "0";

	public static ShippingFeeObj fromMap(Map<String, Object> data) {
		ShippingFeeObj obj = new ShippingFeeObj();
		obj.setId(ParamUtil.getString(data, AppParams.S_ID));
		obj.setBaseId(ParamUtil.getString(data, AppParams.S_BASE_ID));
		obj.setBaseName(ParamUtil.getString(data, AppParams.S_BASE_NAME));
		obj.setCountryCode(ParamUtil.getString(data, AppParams.S_COUNTRY_CODE));
		obj.setPrice(ParamUtil.getDouble(data, AppParams.S_PRICE, AppConstants.DEFAULT_SHIPPING_FEE_VALUE_DOUBLE));
		obj.setAddingPrice(ParamUtil.getDouble(data, AppParams.S_ADDING_PRICE, AppConstants.DEFAULT_ADDING_SHIPPING_FEE_VALUE_DOUBLE));
		obj.setCurrency(ParamUtil.getString(data, AppParams.S_CURRENCY));
		obj.setState(ParamUtil.getString(data, AppParams.S_STATE));
		obj.setGroupId(ParamUtil.getString(data, AppParams.S_GROUP_ID));
		obj.setDropshipPrice(ParamUtil.getDouble(data, AppParams.S_DROPSHIP_PRICE, AppConstants.DEFAULT_SHIPPING_FEE_VALUE_DOUBLE));
		obj.setDropshipAddingPrice(ParamUtil.getDouble(data, AppParams.S_DROPSHIP_ADDING_PRICE, AppConstants.DEFAULT_ADDING_SHIPPING_FEE_VALUE_DOUBLE));
		obj.setExpressPrice(ParamUtil.getDouble(data, AppParams.S_DROPSHIP_EXPRESS_PRICE, AppConstants.DEFAULT_SHIPPING_FEE_VALUE_DOUBLE));
		obj.setExpressAddingPrice(ParamUtil.getDouble(data, AppParams.S_DROPSHIP_EXPRESS_ADDING_PRICE, AppConstants.DEFAULT_ADDING_SHIPPING_FEE_VALUE_DOUBLE));
//		obj.setTaxAmount(ParamUtil.getString(data, AppParams.S_TAX_AMOUNT));
		return obj;
	}

	@Override
	public String toString() {
		return "ShippingFeeObj"
				+ " [id=" + id
				+ ", baseId=" + baseId
				+ ", baseName=" + baseName
				+ ", countryCode=" + countryCode
				+ ", price=" + price
				+ ", addingPrice=" + addingPrice
				+ ", currency=" + currency
				+ ", state=" + state
				+ ", groupId=" + groupId
				+ ", dropshipPrice=" + dropshipPrice
				+ ", dropshipAddingPrice=" + dropshipAddingPrice
				+ ", expressPrice=" + expressPrice
				+ ", expressAddingPrice=" + expressAddingPrice
				+ ", taxAmount=" + taxAmount
				+ "]";
	}

}