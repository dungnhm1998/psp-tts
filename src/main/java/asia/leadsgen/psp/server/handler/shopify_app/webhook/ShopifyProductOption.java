package asia.leadsgen.psp.server.handler.shopify_app.webhook;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class ShopifyProductOption implements SQLData {
	public static final String SQL_TYPE = "SHOPIFY_PRODUCT_OPTION";

	private Long refId;
	private Long productId;
	private String name;
	private Long position;
	private String values;

	public ShopifyProductOption() {
	}

	public ShopifyProductOption(Long refId, Long productId, String name, Long position, String values) {
		super();
		this.refId = refId; 
		this.productId = productId; 
		this.name = name; 
		this.position = position; 
		this.values = values; 
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		refId = stream.readLong();
		productId = stream.readLong();
		name = stream.readString();
		position = stream.readLong();
		values = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeLong(refId);
		stream.writeLong(productId);
		stream.writeString(name);
		stream.writeLong(position);
		stream.writeString(values);
	}

	@Override
	public String toString() {
		return "ShopifyProductOption [refId=" + refId + ", productId=" + productId + ", name=" + name
				+ ", position=" + position + ", values=" + values + "]";
	}

	
}