package asia.leadsgen.psp.obj;

import java.sql.Date;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FulfillmentDetailObj implements SQLData {
	
	public static final String SQL_TYPE = "FULFILLMENT_DETAIL_TYPE";
	
	private String id;
	private String fulfillmentId;
	private String campaignId;
	private String campaignTitle;
	private String baseId;
	private String orderId;
	private String trackingCode;
	private String productId;
	private String productName;
	private String productFrontImgUrl;
	private String productBackImgUrl;
	private String variantId;
	private String size;
	private String color;
	private String colorName;
	private int quantity;
	private String frontDesignId;
	private String frontImageId;
	private String frontImageUrl;
	private String backDesignId;
	private String backImageId;
	private String backImageUrl;
	private String shippingId;
	private int sendEmail;
	private Date printExpire;
	private String partnerId;
	private String barcodeUrl;
	private String shippingCarrier;
	private String shippingLableUrl;
	private String shippingTrackingCode;
	private String shippingTrackingUrl;
	private Date create;
	private Date update;
	private String state;
	private int payoutProcessed;
	private String tariffNumber;
	private String shippingValue;
	private int fulfilled;
	private String source;
	private String lineItemId;
	private String baseWeight;
	private String packageWeight;
	private Date createShippingCode;
	private String label;
	private String sku;
	private String invoice;
	private int runningInvoice;
	private String errorNote;
	private int tscProd;
	private String printCost;
	private String shippingCost;
	private String productCost;
	private String packingCost;
	private int accountingProcessed;
	private String shippingState;
	private String packageId;
	private Date assigned;
	private Date accepted;
	private String refOrderId;
	private String shippingService;
	private Date delivered;
	private String printType;
	private String colorId;
	private String sizeId;
	private String orderProductId;
	private String shippingMethod;
	
	//transient
	private String shippingCountryCode;
	private String dropshipSource;
	private String seq;
	private boolean rosaChangeToChampy = false;
	private boolean valid = true;
	
	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		id = stream.readString();
		fulfillmentId = stream.readString();
		campaignId = stream.readString();
		campaignTitle = stream.readString();
		baseId = stream.readString();
		orderId = stream.readString();
		trackingCode = stream.readString();
		productId = stream.readString();
		productName = stream.readString();
		productFrontImgUrl = stream.readString();
		productBackImgUrl = stream.readString();
		variantId = stream.readString();
		size = stream.readString();
		color = stream.readString();
		colorName = stream.readString();
		quantity = stream.readInt();
		frontDesignId = stream.readString();
		frontImageId = stream.readString();
		frontImageUrl = stream.readString();
		backDesignId = stream.readString();
		backImageId = stream.readString();
		backImageUrl = stream.readString();
		shippingId = stream.readString();
		sendEmail = stream.readInt();
		printExpire = stream.readDate();
		partnerId = stream.readString();
		barcodeUrl = stream.readString();
		shippingCarrier = stream.readString();
		shippingLableUrl = stream.readString();
		shippingTrackingCode = stream.readString();
		shippingTrackingUrl = stream.readString();
		create = stream.readDate();
		update = stream.readDate();
		state = stream.readString();
		payoutProcessed = stream.readInt();
		tariffNumber = stream.readString();
		shippingValue = stream.readString();
		fulfilled = stream.readInt();
		source = stream.readString();
		lineItemId = stream.readString();
		baseWeight = stream.readString();
		packageWeight = stream.readString();
		createShippingCode = stream.readDate();
		label = stream.readString();
		sku = stream.readString();
		invoice = stream.readString();
		runningInvoice = stream.readInt();
		errorNote = stream.readString();
		tscProd = stream.readInt();
		printCost = stream.readString();
		shippingCost = stream.readString();
		productCost = stream.readString();
		packingCost = stream.readString();
		accountingProcessed = stream.readInt();
		shippingState = stream.readString();
		packageId = stream.readString();
		assigned = stream.readDate();
		accepted = stream.readDate();
		refOrderId = stream.readString();
		shippingService = stream.readString();
		delivered = stream.readDate();
		printType = stream.readString();
		colorId = stream.readString();
		sizeId = stream.readString();
		orderProductId = stream.readString();
		shippingMethod = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(id);
		stream.writeString(fulfillmentId);
		stream.writeString(campaignId);
		stream.writeString(campaignTitle);
		stream.writeString(baseId);
		stream.writeString(orderId);
		stream.writeString(trackingCode);
		stream.writeString(productId);
		stream.writeString(productName);
		stream.writeString(productFrontImgUrl);
		stream.writeString(productBackImgUrl);
		stream.writeString(variantId);
		stream.writeString(size);
		stream.writeString(color);
		stream.writeString(colorName);
		stream.writeInt(quantity);
		stream.writeString(frontDesignId);
		stream.writeString(frontImageId);
		stream.writeString(frontImageUrl);
		stream.writeString(backDesignId);
		stream.writeString(backImageId);
		stream.writeString(backImageUrl);
		stream.writeString(shippingId);
		stream.writeInt(sendEmail);
		stream.writeDate(printExpire);
		stream.writeString(partnerId);
		stream.writeString(barcodeUrl);
		stream.writeString(shippingCarrier);
		stream.writeString(shippingLableUrl);
		stream.writeString(shippingTrackingCode);
		stream.writeString(shippingTrackingUrl);
		stream.writeDate(create);
		stream.writeDate(update);
		stream.writeString(state);
		stream.writeInt(payoutProcessed);
		stream.writeString(tariffNumber);
		stream.writeString(shippingValue);
		stream.writeInt(fulfilled);
		stream.writeString(source);
		stream.writeString(lineItemId);
		stream.writeString(baseWeight);
		stream.writeString(packageWeight);
		stream.writeDate(createShippingCode);
		stream.writeString(label);
		stream.writeString(sku);
		stream.writeString(invoice);
		stream.writeInt(runningInvoice);
		stream.writeString(errorNote);
		stream.writeInt(tscProd);
		stream.writeString(printCost);
		stream.writeString(shippingCost);
		stream.writeString(productCost);
		stream.writeString(packingCost);
		stream.writeInt(accountingProcessed);
		stream.writeString(shippingState);
		stream.writeString(packageId);
		stream.writeDate(assigned);
		stream.writeDate(accepted);
		stream.writeString(refOrderId);
		stream.writeString(shippingService);
		stream.writeDate(delivered);
		stream.writeString(printType);
		stream.writeString(colorId);
		stream.writeString(sizeId);
		stream.writeString(orderProductId);
		stream.writeString(shippingMethod);
	}
	
	public static FulfillmentDetailObj fromMap(Map<String, Object> input) {
		FulfillmentDetailObj obj = new FulfillmentDetailObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setFulfillmentId(ParamUtil.getString(input, AppParams.S_FULFILLMENT_ID));
		obj.setCampaignId(ParamUtil.getString(input, AppParams.S_CAMPAIGN_ID));
		obj.setCampaignTitle(ParamUtil.getString(input, AppParams.S_CAMPAIGN_TITLE));
		obj.setBaseId(ParamUtil.getString(input, AppParams.S_BASE_ID));
		obj.setOrderId(ParamUtil.getString(input, AppParams.S_ORDER_ID));
		obj.setTrackingCode(ParamUtil.getString(input, AppParams.S_TRACKING_CODE));
		obj.setProductId(ParamUtil.getString(input, AppParams.S_PRODUCT_ID));
		obj.setProductName(ParamUtil.getString(input, AppParams.S_PRODUCT_NAME));
		obj.setProductFrontImgUrl(ParamUtil.getString(input, AppParams.S_PRODUCT_FRONT_IMG_URL));
		obj.setProductBackImgUrl(ParamUtil.getString(input, AppParams.S_PRODUCT_BACK_IMG_URL));
		obj.setVariantId(ParamUtil.getString(input, AppParams.S_VARIANT_ID));
		obj.setSize(ParamUtil.getString(input, AppParams.S_SIZE));
		obj.setSizeId(ParamUtil.getString(input, AppParams.S_SIZE_ID));
		obj.setColorId(ParamUtil.getString(input, AppParams.S_COLOR_ID));
		obj.setColor(ParamUtil.getString(input, AppParams.S_COLOR));
		obj.setColorName(ParamUtil.getString(input, AppParams.S_COLOR_NAME));
		obj.setQuantity(ParamUtil.getInt(input, AppParams.N_QUANTITY, 0));
		obj.setFrontDesignId(ParamUtil.getString(input, AppParams.S_FRONT_DESIGN_ID));
		obj.setFrontImageId(ParamUtil.getString(input, "S_FRONT_IMAGE_ID"));
		obj.setFrontImageUrl(ParamUtil.getString(input, "S_FRONT_IMAGE_URL"));
		obj.setBackDesignId(ParamUtil.getString(input, AppParams.S_BACK_DESIGN_ID));
		obj.setBackImageId(ParamUtil.getString(input, "S_BACK_IMAGE_ID"));
		obj.setBackImageUrl(ParamUtil.getString(input, "S_BACK_IMAGE_URL"));
		obj.setShippingId(ParamUtil.getString(input, AppParams.S_SHIPPING_ID));
		obj.setSendEmail(ParamUtil.getInt(input, AppParams.N_SEND_EMAIL, 0));
		obj.setPartnerId(ParamUtil.getString(input, AppParams.S_PARTNER_ID));
		obj.setBarcodeUrl(ParamUtil.getString(input, AppParams.S_BARCODE_URL));
		obj.setShippingCarrier(ParamUtil.getString(input, AppParams.S_SHIPPING_CARRIER));
		obj.setShippingLableUrl(ParamUtil.getString(input, AppParams.S_SHIPPING_LABLE_URL));
		obj.setShippingTrackingCode(ParamUtil.getString(input, AppParams.S_SHIPPING_TRACKING_CODE));
		obj.setShippingTrackingUrl(ParamUtil.getString(input, AppParams.S_SHIPPING_TRACKING_URL));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setPayoutProcessed(ParamUtil.getInt(input, AppParams.N_PAYOUT_PROCESSED, 0));
		obj.setTariffNumber(ParamUtil.getString(input, AppParams.S_TARIFF_NUMBER));
		obj.setShippingValue(ParamUtil.getString(input, AppParams.S_SHIPPING_VALUE));
		obj.setFulfilled(ParamUtil.getInt(input, "N_FULFILLED", 0));
		obj.setSource(ParamUtil.getString(input, AppParams.S_SOURCE));
		obj.setLineItemId(ParamUtil.getString(input, AppParams.S_LINE_ITEM_ID));
		obj.setBaseWeight(ParamUtil.getString(input, "S_BASE_WEIGHT"));
		obj.setPackageWeight(ParamUtil.getString(input, AppParams.S_PACKAGE_WEIGHT));
		obj.setLabel(ParamUtil.getString(input, "S_LABEL"));
		obj.setSku(ParamUtil.getString(input, AppParams.S_SKU));
		obj.setInvoice(ParamUtil.getString(input, "S_INVOICE"));
		obj.setRunningInvoice(ParamUtil.getInt(input, "N_RUNNING_INVOICE", 0));
		obj.setErrorNote(ParamUtil.getString(input, AppParams.S_ERROR_NOTE));
		obj.setTscProd(ParamUtil.getInt(input, "N_TSC_PROD", 0));
		obj.setPrintCost(ParamUtil.getString(input, "S_PRINT_COST"));
		obj.setShippingCost(ParamUtil.getString(input, "S_SHIPPING_COST"));
		obj.setProductCost(ParamUtil.getString(input, "S_PRODUCT_COST"));
		obj.setPackingCost(ParamUtil.getString(input, "S_PACKING_COST"));
		obj.setAccountingProcessed(ParamUtil.getInt(input, "N_ACCOUNTING_PROCESSED", 0));
		obj.setShippingState(ParamUtil.getString(input, AppParams.S_SHIPPING_STATE));
		obj.setPackageId(ParamUtil.getString(input, AppParams.S_PACKAGE_ID));
		obj.setShippingService(ParamUtil.getString(input, "S_SHIPPING_SERVICE"));
		obj.setShippingCountryCode(ParamUtil.getString(input, AppParams.S_SHIPPING_COUNTRY_CODE));
		obj.setDropshipSource(ParamUtil.getString(input, "S_DROPSHIP_SOURCE"));
		obj.setShippingMethod(ParamUtil.getString(input, AppParams.S_SHIPPING_METHOD));
		return obj;
	}

	public FulfillmentDetailObj(String id, String campaignId, String campaignTitle, int quantity, String partnerId) {
		super();
		this.id = id;
		this.campaignId = campaignId;
		this.campaignTitle = campaignTitle;
		this.quantity = quantity;
		this.partnerId = partnerId;
	}
	
}
