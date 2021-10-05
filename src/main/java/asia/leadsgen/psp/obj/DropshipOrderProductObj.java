package asia.leadsgen.psp.obj;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DropshipOrderProductObj {

	private String id;
	private String orderId;
	private String campaignId;
	private String productId;
	private String variantId;
	private String sizeId;
	private double price;
	private double shippingFee;
	private String currency;
	private int quantity;
	private String state;
	private String variantName;
	private double amount;
	private double baseCost;
	private String baseId;
	private String lineItemId;
	private String variantFrontUrl;
	private String variantBackUrl;
	private String colorId;
	private String colorValue;
	private String partnerSku;
	private String colorName;
	private String sizeName;
	private String shippingMethod;
	private String printDetail;
	private String itemType;
	private String partnerProperties;
	private String partnerOption;
	private String baseShortCode;
	private String designFrontUrl;
	private String designBackUrl;
	private String unitAmount;
	private String tax;
	private double taxAmount;

	public DropshipOrderProductObj(String orderId, String campaignId, String productId, String variantId, String sizeId, double price, double shippingFee, String currency, int quantity, String state, String variantName, double amount, double baseCost, String baseId, String lineItemId, String variantFrontUrl, String variantBackUrl, String colorId, String colorValue, String partnerSku, String colorName, String sizeName, String shippingMethod, String printDetail, String itemType, String partnerProperties, String partnerOption, String baseShortCode, String designFrontUrl, String designBackUrl, String unitAmount, String tax, double taxAmount) {
		this.orderId = orderId;
		this.campaignId = campaignId;
		this.productId = productId;
		this.variantId = variantId;
		this.sizeId = sizeId;
		this.price = price;
		this.shippingFee = shippingFee;
		this.currency = currency;
		this.quantity = quantity;
		this.state = state;
		this.variantName = variantName;
		this.amount = amount;
		this.baseCost = baseCost;
		this.baseId = baseId;
		this.lineItemId = lineItemId;
		this.variantFrontUrl = variantFrontUrl;
		this.variantBackUrl = variantBackUrl;
		this.colorId = colorId;
		this.colorValue = colorValue;
		this.partnerSku = partnerSku;
		this.colorName = colorName;
		this.sizeName = sizeName;
		this.shippingMethod = shippingMethod;
		this.printDetail = printDetail;
		this.itemType = itemType;
		this.partnerProperties = partnerProperties;
		this.partnerOption = partnerOption;
		this.baseShortCode = baseShortCode;
		this.designFrontUrl = designFrontUrl;
		this.designBackUrl = designBackUrl;
		this.unitAmount = unitAmount;
		this.tax = tax;
		this.taxAmount = taxAmount;
	}

	public static class Builder {
		private String orderId;
		private String campaignId;
		private String productId;
		private String variantId;
		private String sizeId;
		private double price;
		private double shippingFee;
		private String currency;
		private int quantity;
		private String state;
		private String variantName;
		private double amount;
		private double baseCost;
		private String baseId;
		private String lineItemId;
		private String variantFrontUrl;
		private String variantBackUrl;
		private String colorId;
		private String colorValue;
		private String partnerSku;
		private String colorName;
		private String sizeName;
		private String shippingMethod;
		private String printDetail;
		private String itemType;
		private String partnerProperties;
		private String partnerOption;
		private String baseShortCode;
		private String designFrontUrl;
		private String designBackUrl;
		private String unitAmount;
		private String tax;
		private double taxAmount;
		
		public Builder() {
			
		}
		
		public Builder(String orderId) {
			this.orderId = orderId;
		}

		public DropshipOrderProductObj.Builder orderId(String orderId) {
			this.orderId = orderId;
			return this;
		}

		public DropshipOrderProductObj.Builder campaignId(String campaignId) {
			this.campaignId = campaignId;
			return this;
		}

		public DropshipOrderProductObj.Builder productId(String productId) {
			this.productId = productId;
			return this;
		}

		public DropshipOrderProductObj.Builder variantId(String variantId) {
			this.variantId = variantId;
			return this;
		}

		public DropshipOrderProductObj.Builder sizeId(String sizeId) {
			this.sizeId = sizeId;
			return this;
		}

		public DropshipOrderProductObj.Builder price(double price) {
			this.price = price;
			return this;
		}

		public DropshipOrderProductObj.Builder shippingFee(double shippingFee) {
			this.shippingFee = shippingFee;
			return this;
		}

		public DropshipOrderProductObj.Builder currency(String currency) {
			this.currency = currency;
			return this;
		}

		public DropshipOrderProductObj.Builder quantity(int quantity) {
			this.quantity = quantity;
			return this;
		}

		public DropshipOrderProductObj.Builder state(String state) {
			this.state = state;
			return this;
		}

		public DropshipOrderProductObj.Builder variantName(String variantName) {
			this.variantName = variantName;
			return this;
		}

		public DropshipOrderProductObj.Builder amount(double amount) {
			this.amount = amount;
			return this;
		}

		public DropshipOrderProductObj.Builder baseCost(double baseCost) {
			this.baseCost = baseCost;
			return this;
		}

		public DropshipOrderProductObj.Builder baseId(String baseId) {
			this.baseId = baseId;
			return this;
		}

		public DropshipOrderProductObj.Builder lineItemId(String lineItemId) {
			this.lineItemId = lineItemId;
			return this;
		}

		public DropshipOrderProductObj.Builder variantFrontUrl(String variantFrontUrl) {
			this.variantFrontUrl = variantFrontUrl;
			return this;
		}

		public DropshipOrderProductObj.Builder variantBackUrl(String variantBackUrl) {
			this.variantBackUrl = variantBackUrl;
			return this;
		}

		public DropshipOrderProductObj.Builder colorId(String colorId) {
			this.colorId = colorId;
			return this;
		}

		public DropshipOrderProductObj.Builder colorValue(String colorValue) {
			this.colorValue = colorValue;
			return this;
		}

		public DropshipOrderProductObj.Builder partnerSku(String partnerSku) {
			this.partnerSku = partnerSku;
			return this;
		}

		public DropshipOrderProductObj.Builder colorName(String colorName) {
			this.colorName = colorName;
			return this;
		}

		public DropshipOrderProductObj.Builder sizeName(String sizeName) {
			this.sizeName = sizeName;
			return this;
		}

		public DropshipOrderProductObj.Builder shippingMethod(String shippingMethod) {
			this.shippingMethod = shippingMethod;
			return this;
		}

		public DropshipOrderProductObj.Builder printDetail(String printDetail) {
			this.printDetail = printDetail;
			return this;
		}

		public DropshipOrderProductObj.Builder itemType(String itemType) {
			this.itemType = itemType;
			return this;
		}

		public DropshipOrderProductObj.Builder partnerProperties(String partnerProperties) {
			this.partnerProperties = partnerProperties;
			return this;
		}

		public DropshipOrderProductObj.Builder partnerOption(String partnerOption) {
			this.partnerOption = partnerOption;
			return this;
		}

		public DropshipOrderProductObj.Builder baseShortCode(String baseShortCode) {
			this.baseShortCode = baseShortCode;
			return this;
		}

		public DropshipOrderProductObj.Builder designFrontUrl(String designFrontUrl) {
			this.designFrontUrl = designFrontUrl;
			return this;
		}

		public DropshipOrderProductObj.Builder designBackUrl(String designBackUrl) {
			this.designBackUrl = designBackUrl;
			return this;
		}

		public DropshipOrderProductObj.Builder unitAmount(String unitAmount) {
			this.unitAmount = unitAmount;
			return this;
		}

		public DropshipOrderProductObj.Builder tax(String tax) {
			this.tax = tax;
			return this;
		}

		public DropshipOrderProductObj.Builder taxAmount(double taxAmount) {
			this.taxAmount = taxAmount;
			return this;
		}

		public DropshipOrderProductObj build() {
			return new DropshipOrderProductObj(
					this.orderId, this.campaignId, this.productId, this.variantId, this.sizeId, this.price, this.shippingFee, this.currency, this.quantity, this.state, this.variantName, this.amount, this.baseCost, this.baseId, this.lineItemId, this.variantFrontUrl, this.variantBackUrl, this.colorId, this.colorValue, this.partnerSku, this.colorName, this.sizeName, this.shippingMethod, this.printDetail, this.itemType, this.partnerProperties, this.partnerOption, this.baseShortCode, this.designFrontUrl, this.designBackUrl, this.unitAmount, this.tax, this.taxAmount);
		}

	}

	@Override
	public String toString() {
		return "DropshipOrderProductObj"
				+ " [id=" + id
				+ ", orderId=" + orderId
				+ ", campaignId=" + campaignId
				+ ", productId=" + productId
				+ ", variantId=" + variantId
				+ ", sizeId=" + sizeId
				+ ", price=" + price
				+ ", shippingFee=" + shippingFee
				+ ", currency=" + currency
				+ ", quantity=" + quantity
				+ ", state=" + state
				+ ", variantName=" + variantName
				+ ", amount=" + amount
				+ ", baseCost=" + baseCost
				+ ", baseId=" + baseId
				+ ", lineItemId=" + lineItemId
				+ ", variantFrontUrl=" + variantFrontUrl
				+ ", variantBackUrl=" + variantBackUrl
				+ ", colorId=" + colorId
				+ ", colorValue=" + colorValue
				+ ", partnerSku=" + partnerSku
				+ ", colorName=" + colorName
				+ ", sizeName=" + sizeName
				+ ", shippingMethod=" + shippingMethod
				+ ", printDetail=" + printDetail
				+ ", itemType=" + itemType
				+ ", partnerProperties=" + partnerProperties
				+ ", partnerOption=" + partnerOption
				+ ", baseShortCode=" + baseShortCode
				+ ", designFrontUrl=" + designFrontUrl
				+ ", designBackUrl=" + designBackUrl
				+ ", unitAmount=" + unitAmount
				+ ", tax=" + tax
				+ ", taxAmount=" + taxAmount
				+ "]";
	}
}
