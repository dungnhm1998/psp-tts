package asia.leadsgen.psp.server.handler.shopify_app.webhook;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class ShopifyProductVariant implements SQLData {
	public static final String SQL_TYPE = "SHOPIFY_PRODUCT_VARIANT";

	private Long refId;
	private Long productId;
	private String title;
	private String price;
	private String sku;
	private Long position;
	private String option1;
	private String option2;
	private String option3;
	private Long imageId;

	public ShopifyProductVariant() {
	}

	public ShopifyProductVariant(Long refId, Long productId, String title, String price, String sku, Long position, String option1, String option2, String option3, Long imageId) {
		super();
		this.refId = refId; 
		this.productId = productId; 
		this.title = title; 
		this.price = price; 
		this.sku = sku; 
		this.position = position; 
		this.option1 = option1; 
		this.option2 = option2; 
		this.option3 = option3; 
		this.imageId = imageId; 
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		refId = stream.readLong();
		productId = stream.readLong();
		title = stream.readString();
		price = stream.readString();
		sku = stream.readString();
		position = stream.readLong();
		option1 = stream.readString();
		option2 = stream.readString();
		option3 = stream.readString();
		imageId = stream.readLong();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeLong(refId);
		stream.writeLong(productId);
		stream.writeString(title);
		stream.writeString(price);
		stream.writeString(sku);
		stream.writeLong(position);
		stream.writeString(option1);
		stream.writeString(option2);
		stream.writeString(option3);
		stream.writeLong(imageId);
	}

	@Override
	public String toString() {
		return "ShopifyProductVariant [refId=" + refId + ", productId=" + productId + ", title=" + title + ", price="
				+ price + ", sku=" + sku + ", position=" + position + ", option1=" + option1 + ", option2=" + option2
				+ ", option3=" + option3 + ", imageId=" + imageId + "]";
	}
	
}
