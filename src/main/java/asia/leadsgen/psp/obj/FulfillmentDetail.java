package asia.leadsgen.psp.obj;

public class FulfillmentDetail {
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
	private boolean isSendEmail;
	private String datePrintExpire;
	private String partnerId;
	private String barcodeUrl;
	private String shippingCarrier;
	private String shippingLabelUrl;
	private String shippingTrackingCode;
	private String shippingTrackingUrl;
	private String dateCreate;
	private String dateUpdate;
	private String state;
	private boolean isProcessedPayout;
	private String tariffNumber;
	private String shippingValue;
	private String fullfilSyncState;
	private String source;
	private String lineItemId;
	private double baseWeight;
	private double packageWeight;

	private Shipping shipping;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFulfillmentId() {
		return fulfillmentId;
	}

	public void setFulfillmentId(String fulfillmentId) {
		this.fulfillmentId = fulfillmentId;
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}

	public String getCampaignTitle() {
		return campaignTitle;
	}

	public void setCampaignTitle(String campaignTitle) {
		this.campaignTitle = campaignTitle;
	}

	public String getBaseId() {
		return baseId;
	}

	public void setBaseId(String baseId) {
		this.baseId = baseId;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getTrackingCode() {
		return trackingCode;
	}

	public void setTrackingCode(String trackingCode) {
		this.trackingCode = trackingCode;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getProductFrontImgUrl() {
		return productFrontImgUrl;
	}

	public void setProductFrontImgUrl(String productFrontImgUrl) {
		this.productFrontImgUrl = productFrontImgUrl;
	}

	public String getProductBackImgUrl() {
		return productBackImgUrl;
	}

	public void setProductBackImgUrl(String productBackImgUrl) {
		this.productBackImgUrl = productBackImgUrl;
	}

	public String getVariantId() {
		return variantId;
	}

	public void setVariantId(String variantId) {
		this.variantId = variantId;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getColorName() {
		return colorName;
	}

	public void setColorName(String colorName) {
		this.colorName = colorName;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getFrontDesignId() {
		return frontDesignId;
	}

	public void setFrontDesignId(String frontDesignId) {
		this.frontDesignId = frontDesignId;
	}

	public String getFrontImageId() {
		return frontImageId;
	}

	public void setFrontImageId(String frontImageId) {
		this.frontImageId = frontImageId;
	}

	public String getFrontImageUrl() {
		return frontImageUrl;
	}

	public void setFrontImageUrl(String frontImageUrl) {
		this.frontImageUrl = frontImageUrl;
	}

	public String getBackDesignId() {
		return backDesignId;
	}

	public void setBackDesignId(String backDesignId) {
		this.backDesignId = backDesignId;
	}

	public String getBackImageId() {
		return backImageId;
	}

	public void setBackImageId(String backImageId) {
		this.backImageId = backImageId;
	}

	public String getBackImageUrl() {
		return backImageUrl;
	}

	public void setBackImageUrl(String backImageUrl) {
		this.backImageUrl = backImageUrl;
	}

	public String getShippingId() {
		return shippingId;
	}

	public void setShippingId(String shippingId) {
		this.shippingId = shippingId;
	}

	public boolean isSendEmail() {
		return isSendEmail;
	}

	public void setSendEmail(boolean isSendEmail) {
		this.isSendEmail = isSendEmail;
	}

	public String getDatePrintExpire() {
		return datePrintExpire;
	}

	public void setDatePrintExpire(String datePrintExpire) {
		this.datePrintExpire = datePrintExpire;
	}

	public String getPartnerId() {
		return partnerId;
	}

	public void setPartnerId(String partnerId) {
		this.partnerId = partnerId;
	}

	public String getBarcodeUrl() {
		return barcodeUrl;
	}

	public void setBarcodeUrl(String barcodeUrl) {
		this.barcodeUrl = barcodeUrl;
	}

	public String getShippingCarrier() {
		return shippingCarrier;
	}

	public void setShippingCarrier(String shippingCarrier) {
		this.shippingCarrier = shippingCarrier;
	}

	public String getShippingLabelUrl() {
		return shippingLabelUrl;
	}

	public void setShippingLabelUrl(String shippingLabelUrl) {
		this.shippingLabelUrl = shippingLabelUrl;
	}

	public String getShippingTrackingCode() {
		return shippingTrackingCode;
	}

	public void setShippingTrackingCode(String shippingTrackingCode) {
		this.shippingTrackingCode = shippingTrackingCode;
	}

	public String getShippingTrackingUrl() {
		return shippingTrackingUrl;
	}

	public void setShippingTrackingUrl(String shippingTrackingUrl) {
		this.shippingTrackingUrl = shippingTrackingUrl;
	}

	public String getDateCreate() {
		return dateCreate;
	}

	public void setDateCreate(String dateCreate) {
		this.dateCreate = dateCreate;
	}

	public String getDateUpdate() {
		return dateUpdate;
	}

	public void setDateUpdate(String dateUpdate) {
		this.dateUpdate = dateUpdate;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public boolean isProcessedPayout() {
		return isProcessedPayout;
	}

	public void setProcessedPayout(boolean isProcessedPayout) {
		this.isProcessedPayout = isProcessedPayout;
	}

	public String getTariffNumber() {
		return tariffNumber;
	}

	public void setTariffNumber(String tariffNumber) {
		this.tariffNumber = tariffNumber;
	}

	public String getShippingValue() {
		return shippingValue;
	}

	public void setShippingValue(String shippingValue) {
		this.shippingValue = shippingValue;
	}

	public String getFullfilSyncState() {
		return fullfilSyncState;
	}

	public void setFullfilSyncState(String fullfilSyncState) {
		this.fullfilSyncState = fullfilSyncState;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getLineItemId() {
		return lineItemId;
	}

	public void setLineItemId(String lineItemId) {
		this.lineItemId = lineItemId;
	}

	public double getBaseWeight() {
		return baseWeight;
	}

	public void setBaseWeight(double baseWeight) {
		this.baseWeight = baseWeight;
	}

	public double getPackageWeight() {
		return packageWeight;
	}

	public void setPackageWeight(double packageWeight) {
		this.packageWeight = packageWeight;
	}

	public Shipping getShipping() {
		return shipping;
	}

	public void setShipping(Shipping shipping) {
		this.shipping = shipping;
	}

	@Override
	public String toString() {
		return "FulfillmentDetail [id=" + id + ", fulfillmentId=" + fulfillmentId + ", campaignId=" + campaignId
				+ ", campaignTitle=" + campaignTitle + ", baseId=" + baseId + ", orderId=" + orderId + ", trackingCode="
				+ trackingCode + ", productId=" + productId + ", productName=" + productName + ", productFrontImgUrl="
				+ productFrontImgUrl + ", productBackImgUrl=" + productBackImgUrl + ", variantId=" + variantId
				+ ", size=" + size + ", color=" + color + ", colorName=" + colorName + ", quantity=" + quantity
				+ ", frontDesignId=" + frontDesignId + ", frontImageId=" + frontImageId + ", frontImageUrl="
				+ frontImageUrl + ", backDesignId=" + backDesignId + ", backImageId=" + backImageId + ", backImageUrl="
				+ backImageUrl + ", shippingId=" + shippingId + ", isSendEmail=" + isSendEmail + ", datePrintExpire="
				+ datePrintExpire + ", partnerId=" + partnerId + ", barcodeUrl=" + barcodeUrl + ", shippingCarrier="
				+ shippingCarrier + ", shippingLabelUrl=" + shippingLabelUrl + ", shippingTrackingCode="
				+ shippingTrackingCode + ", shippingTrackingUrl=" + shippingTrackingUrl + ", dateCreate=" + dateCreate
				+ ", dateUpdate=" + dateUpdate + ", state=" + state + ", isProcessedPayout=" + isProcessedPayout
				+ ", tariffNumber=" + tariffNumber + ", shippingValue=" + shippingValue + ", fullfilSyncState="
				+ fullfilSyncState + ", source=" + source + ", lineItemId=" + lineItemId + ", baseWeight=" + baseWeight
				+ ", packageWeight=" + packageWeight + ", shipping=" + shipping + "]";
	}

}
