package asia.leadsgen.psp.server.handler.shopify_app.webhook;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class ShopifyProduct implements SQLData {
	public static final String SQL_TYPE = "SHOPIFY_PRODUCT";

	private String storeId;
	private Long refId;
	private String title;
	private String bodyHtml;
	private String productType;
	private String tags;
	private String handle;
	private String storeName;

	public ShopifyProduct() {
	}

	public ShopifyProduct(String storeId, Long refId, String title, String bodyHtml, String productType, String tags, String handle, String storeName) {
		super();
		this.storeId = storeId; 
		this.refId = refId; 
		this.title = title; 
		this.bodyHtml = bodyHtml; 
		this.productType = productType; 
		this.tags = tags; 
		this.handle = handle; 
		this.storeName = storeName; 
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		storeId = stream.readString();
		refId = stream.readLong();
		title = stream.readString();
		bodyHtml = stream.readString();
		productType = stream.readString();
		tags = stream.readString();
		handle = stream.readString();
		storeName = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(storeId);
		stream.writeLong(refId);
		stream.writeString(title);
		stream.writeString(bodyHtml);
		stream.writeString(productType);
		stream.writeString(tags);
		stream.writeString(handle);
		stream.writeString(storeName);
	}

	@Override
	public String toString() {
		return "ShopifyProduct [storeId=" + storeId + ", refId=" + refId + ", title=" + title + ", bodyHtml=" + bodyHtml
				+ ", productType=" + productType + ", tags=" + tags + ", handle=" + handle + ", storeName=" + storeName
				+ "]";
	}
	
	
}
