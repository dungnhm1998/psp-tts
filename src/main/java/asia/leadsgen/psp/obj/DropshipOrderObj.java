package asia.leadsgen.psp.obj;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Setter
@Getter
public class DropshipOrderObj {

	private String id;
	private String orderIdPrefix;
	private String orderCurrency;
	private String state;
	private String shippingId;
	private String trackingNumber;
	private String note;
	private String channel;
	private String storeId;
	private String userId;
	private String referenceOrderId;
	private double orderAmount = 0.00d;
	private double subAmount = 0.00d;
	private double shippingFee = 0.00d;
	private int totalItems;
	private int addrVerified;
	private String minifiedJson;
	private String source;
	private String substate;
	private String originalId;
	private String shippingMethod;
	private double taxAmount = 0.00d;
	private String iossNumber;

	public DropshipOrderObj() {
		super();
	}

	public DropshipOrderObj(String orderIdPrefix, String orderCurrency, String state, String shippingId,
							String trackingNumber, String note, String channel, String storeId, String userId,
							String referenceOrderId, double orderAmount, double subAmount, double shippingFee,
							int totalItems, int addrVerified, String minifiedJson, String source, String substate,
							String originalId, String shippingMethod, double taxAmount, String iossNumber) {
		this.orderIdPrefix = orderIdPrefix;
		this.orderCurrency = orderCurrency;
		this.state = state;
		this.shippingId = shippingId;
		this.trackingNumber = trackingNumber;
		this.note = note;
		this.channel = channel;
		this.storeId = storeId;
		this.userId = userId;
		this.referenceOrderId = referenceOrderId;
		this.orderAmount = orderAmount;
		this.subAmount = subAmount;
		this.shippingFee = shippingFee;
		this.totalItems = totalItems;
		this.addrVerified = addrVerified;
		this.minifiedJson = minifiedJson;
		this.source = source;
		this.substate = substate;
		this.originalId = originalId;
		this.shippingMethod = shippingMethod;
		this.taxAmount = taxAmount;
		this.iossNumber = iossNumber;
	}

	public static class Builder {
		private String orderIdPrefix;
		private String orderCurrency;
		private String state;
		private String shippingId;
		private String trackingNumber;
		private String note;
		private String channel;
		private String storeId;
		private String userId;
		private String referenceOrderId;
		private double orderAmount = 0.00d;
		private double subAmount = 0.00d;
		private double shippingFee = 0.00d;
		private int totalItems;
		private int addrVerified;
		private String minifiedJson;
		private String source;
		private String substate;
		private String originalId;
		private String shippingMethod;
		private double taxAmount = 0.00d;
		private String iossNumber;

		public Builder(String orderIdPrefix) {
			this.orderIdPrefix = orderIdPrefix;
		}

		public Builder orderCurrency(String orderCurrency) {
			this.orderCurrency = orderCurrency;
			return this;
		}

		public Builder state(String state) {
			this.state = state;
			return this;
		}

		public Builder shippingId(String shippingId) {
			this.shippingId = shippingId;
			return this;
		}

		public Builder trackingNumber(String trackingNumber) {
			this.trackingNumber = trackingNumber;
			return this;
		}

		public Builder note(String note) {
			this.note = note;
			return this;
		}

		public Builder channel(String channel) {
			this.channel = channel;
			return this;
		}

		public Builder storeId(String storeId) {
			this.storeId = storeId;
			return this;
		}

		public Builder userId(String userId) {
			this.userId = userId;
			return this;
		}

		public Builder referenceOrderId(String referenceOrderId) {
			this.referenceOrderId = referenceOrderId;
			return this;
		}

		public Builder orderAmount(double orderAmount) {
			this.orderAmount = orderAmount;
			return this;
		}

		public Builder subAmount(double subAmount) {
			this.subAmount = subAmount;
			return this;
		}

		public Builder shippingFee(double shippingFee) {
			this.shippingFee = shippingFee;
			return this;
		}

		public Builder totalItems(int totalItems) {
			this.totalItems = totalItems;
			return this;
		}

		public Builder addrVerified(int addrVerified) {
			this.addrVerified = addrVerified;
			return this;
		}

		public Builder minifiedJson(String minifiedJson) {
			this.minifiedJson = minifiedJson;
			return this;
		}

		public Builder source(String source) {
			this.source = source;
			return this;
		}

		public Builder substate(String substate) {
			this.substate = substate;
			return this;
		}

		public Builder originalId(String originalId) {
			this.originalId = originalId;
			return this;
		}

		public Builder shippingMethod(String shippingMethod) {
			this.shippingMethod = shippingMethod;
			return this;
		}

		public Builder taxAmount(double taxAmount) {
			this.taxAmount = taxAmount;
			return this;
		}

		public Builder iossNumber(String iossNumber) {
			this.iossNumber = iossNumber;
			return this;
		}

		public DropshipOrderObj build() {
			return new DropshipOrderObj(this.orderIdPrefix, this.orderCurrency, this.state, this.shippingId,
					this.trackingNumber, this.note, this.channel, this.storeId, this.userId, this.referenceOrderId,
					this.orderAmount, this.subAmount, this.shippingFee, this.totalItems, this.addrVerified,
					this.minifiedJson, this.source, this.substate, this.originalId, this.shippingMethod, this.taxAmount, this.iossNumber);
		}

	}

	@Override
	public String toString() {
		return "DropshipOrderObj [id=" + id + ", orderIdPrefix=" + orderIdPrefix + ", orderCurrency=" + orderCurrency
				+ ", state=" + state + ", shippingId=" + shippingId + ", trackingNumber=" + trackingNumber + ", note="
				+ note + ", channel=" + channel + ", storeId=" + storeId + ", userId=" + userId + ", referenceOrderId="
				+ referenceOrderId + ", orderAmount=" + orderAmount + ", subAmount=" + subAmount + ", shippingFee="
				+ shippingFee + ", totalItems=" + totalItems + ", addrVerified=" + addrVerified + ", minifiedJson="
				+ minifiedJson + ", source=" + source + ", substate=" + substate + ", originalId=" + originalId
				+ ", shippingMethod=" + shippingMethod + ", taxAmount=" + taxAmount + ", iossNumber=" + iossNumber + "]";
	}
}
