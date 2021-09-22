package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DropshipImportFileRowObj implements Serializable {
	private static final long serialVersionUID = -2983600627331475796L;
	private String id;
	private String fileId;
	private String userId;
	private String storeId;
	private String referenceOrder;
	private String orderId;
	private String email;
	private String shippingName;
	private String shippingStreet;
	private String shippingAddress1;
	private String shippingAddress2;
	private String shippingCompany;
	private String shippingCity;
	private String shippingZip;
	private String shippingProvince;
	private String shippingCountry;
	private String shippingPhone;
	private String notes;
	private String financialStatus;
	private String lineitemQuantity;
	private String lineitemName;
	private String lineitemSku;
	private String designFrontUrl;
	private String designBackUrl;
	private String mockupFrontUrl;
	private String mockupBackUrl;
	private String byPassCheckAdress;
	private String source;
	private String status;
	private String orderProductId;
	private int reprocess;
	private String receiveJson;

	public static DropshipImportFileRowObj fromMap(Map<String, Object> input) {
		DropshipImportFileRowObj obj = new DropshipImportFileRowObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setFileId(ParamUtil.getString(input, AppParams.S_FILE_ID));
		obj.setUserId(ParamUtil.getString(input, AppParams.S_USER_ID));
		obj.setStoreId(ParamUtil.getString(input, AppParams.S_STORE_ID));
		obj.setReferenceOrder(ParamUtil.getString(input, AppParams.S_REFERENCE_ORDER));
		obj.setShippingName(ParamUtil.getString(input, AppParams.S_SHIPPING_NAME));
		obj.setShippingStreet(ParamUtil.getString(input, AppParams.S_SHIPPING_STREET));
		obj.setEmail(ParamUtil.getString(input, AppParams.S_EMAIL));
		obj.setShippingAddress1(ParamUtil.getString(input, AppParams.S_SHIPPING_ADDRESS1));
		obj.setShippingAddress2(ParamUtil.getString(input, AppParams.S_SHIPPING_ADDRESS2));
		obj.setShippingCompany(ParamUtil.getString(input, AppParams.S_SHIPPING_COMPANY));
		obj.setShippingCity(ParamUtil.getString(input, AppParams.S_SHIPPING_CITY));
		obj.setShippingZip(ParamUtil.getString(input, AppParams.S_SHIPPING_ZIP));
		obj.setShippingProvince(ParamUtil.getString(input, AppParams.S_SHIPPING_PROVINCE));
		obj.setShippingCountry(ParamUtil.getString(input, AppParams.S_SHIPPING_COUNTRY));
		obj.setShippingPhone(ParamUtil.getString(input, AppParams.S_SHIPPING_PHONE));
		obj.setNotes(ParamUtil.getString(input, AppParams.S_NOTES));
		obj.setFinancialStatus(ParamUtil.getString(input, AppParams.S_FINANCIAL_STATUS));
		obj.setLineitemQuantity(ParamUtil.getString(input, AppParams.S_LINEITEM_QUANTITY));
		obj.setLineitemName(ParamUtil.getString(input, AppParams.S_LINEITEM_NAME));
		obj.setLineitemSku(ParamUtil.getString(input, AppParams.S_LINEITEM_SKU));
		obj.setDesignFrontUrl(ParamUtil.getString(input, AppParams.S_DESIGN_FRONT_URL));
		obj.setDesignBackUrl(ParamUtil.getString(input, AppParams.S_DESIGN_BACK_URL));
		obj.setMockupFrontUrl(ParamUtil.getString(input, AppParams.S_MOCKUP_FRONT_URL));
		obj.setMockupBackUrl(ParamUtil.getString(input, AppParams.S_MOCKUP_BACK_URL));
		obj.setByPassCheckAdress(ParamUtil.getString(input, AppParams.S_BY_PASS_CHECK_ADRESS));
		obj.setSource(ParamUtil.getString(input, AppParams.S_SOURCE));
		obj.setStatus(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setOrderProductId(ParamUtil.getString(input, AppParams.S_ORDER_PRODUCT_ID));
		obj.setReprocess(ParamUtil.getInt(input, AppParams.N_REPROCESS));
		obj.setOrderId(ParamUtil.getString(input, AppParams.S_ORDER_ID));
		obj.setReceiveJson(ParamUtil.getString(input, "S_RECEIVE_JSON"));
		return obj;
	}
}
