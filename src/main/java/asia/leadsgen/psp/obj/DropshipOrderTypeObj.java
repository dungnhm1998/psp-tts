package asia.leadsgen.psp.obj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Clob;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DropshipOrderTypeObj implements SQLData {

	public static final String SQL_TYPE = "ORDER_TYPE";

	private String id;
	private String idPrefix;
	private double amount;
	private String currency;
	private String state;
	private String shippingId;
	private String trackingCode;
	private String note;
	private String channel;
	private double subAmount;
	private double shippingFee;
	private String storeId;
	private String userId;
	private String referenceOrder;
	private int totalItem;
	private String paymentFee;
	private String refundNote;
	private String adjustNote;
	private String refundedAmount;
	private String refFee;
	private int addrVerified;
	private String addrVerifiedNote;
	private String minifiedJson;
	private Clob minifiedJsonClob;
	private String sellerCost;
	private String produceCost;
	private String shippingCost;
	private String source;
	private String subState;
	private String originalId;
	private String shippingMethod;
	private String taxAmount;
	private String iossNumber;

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		id = stream.readString();
		idPrefix = stream.readString();
		amount = stream.readDouble();
		currency = stream.readString();
		state = stream.readString();
		shippingId = stream.readString();
		trackingCode = stream.readString();
		note = stream.readString();
		channel = stream.readString();
		subAmount = stream.readDouble();
		shippingFee = stream.readDouble();
		storeId = stream.readString();
		userId = stream.readString();
		referenceOrder = stream.readString();
		totalItem = stream.readInt();
		paymentFee = stream.readString();
		refundNote = stream.readString();
		adjustNote = stream.readString();
		refundedAmount = stream.readString();
		refFee = stream.readString();
		addrVerified = stream.readInt();
		addrVerifiedNote = stream.readString();
		minifiedJson = stream.readString();
		sellerCost = stream.readString();
		produceCost = stream.readString();
		shippingCost = stream.readString();
		source = stream.readString();
		subState = stream.readString();
		originalId = stream.readString();
		shippingMethod = stream.readString();
		taxAmount = stream.readString();
		iossNumber = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(id);
		stream.writeString(idPrefix);
		stream.writeDouble(amount);
		stream.writeString(currency);
		stream.writeString(state);
		stream.writeString(shippingId);
		stream.writeString(trackingCode);
		stream.writeString(note);
		stream.writeString(channel);
		stream.writeDouble(subAmount);
		stream.writeDouble(shippingFee);
		stream.writeString(storeId);
		stream.writeString(userId);
		stream.writeString(referenceOrder);
		stream.writeInt(totalItem);
		stream.writeString(paymentFee);
		stream.writeString(refundNote);
		stream.writeString(adjustNote);
		stream.writeString(refundedAmount);
		stream.writeString(refFee);
		stream.writeInt(addrVerified);
		stream.writeString(addrVerifiedNote);
		stream.writeClob(minifiedJsonClob);
		stream.writeString(sellerCost);
		stream.writeString(produceCost);
		stream.writeString(shippingCost);
		stream.writeString(source);
		stream.writeString(subState);
		stream.writeString(originalId);
		stream.writeString(shippingMethod);
		stream.writeString(taxAmount);
		stream.writeString(iossNumber);
	}

	@Override
	public String toString() {
		return "DropshipOrderTypeObj [id=" + id
				+ ",idPrefix=" + idPrefix
				+ ",amount=" + amount
				+ ",currency=" + currency
				+ ",state=" + state
				+ ",shippingId=" + shippingId
				+ ",trackingCode=" + trackingCode
				+ ",note=" + note
				+ ",channel=" + channel
				+ ",subAmount=" + subAmount
				+ ",shippingFee=" + shippingFee
				+ ",storeId=" + storeId
				+ ",userId=" + userId
				+ ",referenceOrder=" + referenceOrder
				+ ",totalItem=" + totalItem
				+ ",paymentFee=" + paymentFee
				+ ",refundNote=" + refundNote
				+ ",adjustNote=" + adjustNote
				+ ",refundedAmount=" + refundedAmount
				+ ",refFee=" + refFee
				+ ",addrVerified=" + addrVerified
				+ ",addrVerifiedNote=" + addrVerifiedNote
				+ ",minifiedJson=" + minifiedJson
				+ ",sellerCost=" + sellerCost
				+ ",produceCost=" + produceCost
				+ ",shippingCost=" + shippingCost
				+ ",source=" + source
				+ ",subState=" + subState
				+ ",originalId=" + originalId
				+ ",shippingMethod=" + shippingMethod
				+ ",taxAmount=" + taxAmount
				+ ",iossNumber=" + iossNumber
				+ "]";
	}
}
