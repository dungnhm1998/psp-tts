package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BaseSKUObj implements Serializable {
	private static final long serialVersionUID = -2983600627331475796L;
	private String id;
	private String partnerId;
	private String baseId;
	private String sizeId;

	private String baseName;
	private String sizeName;

	private String vendorColorName;
	private String sku;
	private String desc;
	private String price;
	private String secondPrice;
	private String addPrice;
	private int backPrint;
	private String state;
	private String dateCreate;
	private String dateUpdate;
	private Double shippingUS;
	private Double shippingWW;
	private Double shippingExtra;
	private Double sellerBaseCost;
	private Double sellerBaseCostSecondPrice;
	private String baseShortCode;
	private String colorId;
	private String bgpColorName;
	private String bgpColorValue;

	public static BaseSKUObj fromMap(Map<String, Object> input) {
		BaseSKUObj obj = new BaseSKUObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setPartnerId(ParamUtil.getString(input, AppParams.S_PARTNER_ID));
		obj.setBaseId(ParamUtil.getString(input, AppParams.S_BASE_ID));
		obj.setSizeId(ParamUtil.getString(input, AppParams.S_SIZE_ID));
		obj.setColorId(ParamUtil.getString(input, AppParams.S_COLOR_ID));
		obj.setBaseName(ParamUtil.getString(input, AppParams.S_BASE_NAME));
		obj.setSizeName(ParamUtil.getString(input, AppParams.S_SIZE_NAME));
		obj.setBgpColorName(ParamUtil.getString(input, AppParams.S_BGP_COLOR_NAME));
		obj.setVendorColorName(ParamUtil.getString(input, AppParams.S_VENDOR_COLOR_NAME));
		obj.setSku(ParamUtil.getString(input, AppParams.S_SKU));
		obj.setDesc(ParamUtil.getString(input, AppParams.S_DESC));
		obj.setPrice(ParamUtil.getString(input, AppParams.S_PRICE));
		obj.setSecondPrice(ParamUtil.getString(input, AppParams.S_2ND_PRICE));
		obj.setAddPrice(ParamUtil.getString(input, AppParams.S_ADD_PRICE));
		obj.setBackPrint(ParamUtil.getInt(input, AppParams.N_BACK_PRINT, 0));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setDateCreate(ParamUtil.getString(input, AppParams.D_CREATE));
		obj.setDateUpdate(ParamUtil.getString(input, AppParams.D_UPDATE));
		obj.setShippingUS(ParamUtil.getDouble(input, AppParams.S_SHIPPING_US, 0));
		obj.setShippingWW(ParamUtil.getDouble(input, AppParams.S_SHIPPING_WW, 0));
		obj.setShippingExtra(ParamUtil.getDouble(input, AppParams.S_SHIPPING_EXTRA, 0));
		obj.setSellerBaseCost(ParamUtil.getDouble(input, AppParams.S_SELLER_BASE_COST, 0));
		obj.setSellerBaseCostSecondPrice(ParamUtil.getDouble(input, AppParams.S_SELLER_BASE_COST_SECOND_PRICE, 0));
		obj.setBaseShortCode(ParamUtil.getString(input, AppParams.S_BASE_SHORT_CODE));
		obj.setBgpColorValue(ParamUtil.getString(input, AppParams.S_BGP_COLOR_VALUE));
		return obj;
	}

	@Override
	public String toString() {
		return "BaseSKUObj [id=" + id + ", partnerId=" + partnerId + ", baseId=" + baseId + ", sizeId=" + sizeId
				+ ", baseName=" + baseName + ", sizeName=" + sizeName + ", vendorColorName=" + vendorColorName
				+ ", sku=" + sku + ", desc=" + desc + ", price=" + price + ", secondPrice=" + secondPrice
				+ ", addPrice=" + addPrice + ", backPrint=" + backPrint + ", state=" + state + ", dateCreate="
				+ dateCreate + ", dateUpdate=" + dateUpdate + ", shippingUS=" + shippingUS + ", shippingWW="
				+ shippingWW + ", shippingExtra=" + shippingExtra + ", sellerBaseCost=" + sellerBaseCost
				+ ", sellerBaseCostSecondPrice=" + sellerBaseCostSecondPrice + ", baseShortCode=" + baseShortCode
				+ ", colorId=" + colorId + ", bgpColorName=" + bgpColorName + ", bgpColorValue=" + bgpColorValue + "]";
	}
	
	
}
