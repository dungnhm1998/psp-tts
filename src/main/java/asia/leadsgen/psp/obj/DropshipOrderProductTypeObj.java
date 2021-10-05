package asia.leadsgen.psp.obj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
@ToString
public class DropshipOrderProductTypeObj implements SQLData {

	private String id;
	private String orderId;
	private String campaignId;
	private String productId;
	private String variantId;
	private String sizeId;
	private String price;
	private String shippingFee;
	private String currency;
	private int quantity;
	private String state;
	private String variantName;
	private String amount;
	private int status;
	private String baseCost;
	private String baseId;
	private String lineItemId;
	private int refundedItem;
	private String variantFrontUrl;
	private String variantBackUrl;
	private String colorId;
	private String colorValue;
	private String partnerSku;
	private String colorName;
	private String sizeName;
	private String shippingMethod;
	private String unitAmount;
	private int refundItem;
	private String printDetail;
	private String itemType;
	private String designBackUrl;
	private String designFrontUrl;
	private String partnerProperties;
	private String partnerOption;
	private String taxAmount;
	private String customData;
	private Clob partnerPropertiesClob;
	private Clob partnerOptionClob;
	private Clob printDetailClob;
	private Clob customDataClob;

	public static final String SQL_TYPE = "ORDER_PRODUCT_TYPE";

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		id = stream.readString();
		orderId = stream.readString();
		campaignId = stream.readString();
		productId = stream.readString();
		variantId = stream.readString();
		sizeId = stream.readString();
		price = stream.readString();
		shippingFee = stream.readString();
		currency = stream.readString();
		quantity = stream.readInt();
		state = stream.readString();
		variantName = stream.readString();
		amount = stream.readString();
		status = stream.readInt();
		baseCost = stream.readString();
		baseId = stream.readString();
		lineItemId = stream.readString();
		refundedItem = stream.readInt();
		variantFrontUrl = stream.readString();
		variantBackUrl = stream.readString();
		colorId = stream.readString();
		colorValue = stream.readString();
		partnerSku = stream.readString();
		colorName = stream.readString();
		sizeName = stream.readString();
		shippingMethod = stream.readString();
		unitAmount = stream.readString();
		refundItem = stream.readInt();
		printDetail = stream.readString();
		itemType = stream.readString();
		designBackUrl = stream.readString();
		designFrontUrl = stream.readString();
		partnerProperties = stream.readString();
		partnerOption = stream.readString();
		taxAmount = stream.readString();
		customData = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(id);
		stream.writeString(orderId);
		stream.writeString(campaignId);
		stream.writeString(productId);
		stream.writeString(variantId);
		stream.writeString(sizeId);
		stream.writeString(price);
		stream.writeString(shippingFee);
		stream.writeString(currency);
		stream.writeInt(quantity);
		stream.writeString(state);
		stream.writeString(variantName);
		stream.writeString(amount);
		stream.writeInt(status);
		stream.writeString(baseCost);
		stream.writeString(baseId);
		stream.writeString(lineItemId);
		stream.writeInt(refundedItem);
		stream.writeString(variantFrontUrl);
		stream.writeString(variantBackUrl);
		stream.writeString(colorId);
		stream.writeString(colorValue);
		stream.writeString(partnerSku);
		stream.writeString(colorName);
		stream.writeString(sizeName);
		stream.writeString(shippingMethod);
		stream.writeString(unitAmount);
		stream.writeInt(refundItem);
		stream.writeClob(printDetailClob);
		stream.writeString(itemType);
		stream.writeString(designBackUrl);
		stream.writeString(designFrontUrl);
		stream.writeClob(partnerPropertiesClob);
		stream.writeClob(partnerOptionClob);
		stream.writeString(taxAmount);
		stream.writeClob(customDataClob);

	}
}
