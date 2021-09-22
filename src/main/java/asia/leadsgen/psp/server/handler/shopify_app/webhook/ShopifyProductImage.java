package asia.leadsgen.psp.server.handler.shopify_app.webhook;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class ShopifyProductImage implements SQLData {
	public static final String SQL_TYPE = "SHOPIFY_PRODUCT_IMAGE";

	private Long refId;
	private Long productId;
	private Long position;
	private String src;
	private Long width;
	private Long height;
	private String variant_ids;

	public ShopifyProductImage() {
	}

	public ShopifyProductImage(Long refId, Long productId, Long position, String src, Long width, Long height, String variant_ids) {
		super();
		this.refId = refId; 
		this.productId = productId; 
		this.position = position; 
		this.src = src; 
		this.width = width; 
		this.height = height; 
		this.variant_ids = variant_ids; 
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		refId = stream.readLong();
		productId = stream.readLong();
		position = stream.readLong();
		src = stream.readString();
		width = stream.readLong();
		height = stream.readLong();
		variant_ids = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeLong(refId);
		stream.writeLong(productId);
		stream.writeLong(position);
		stream.writeString(src);
		stream.writeLong(width);
		stream.writeLong(height);
		stream.writeString(variant_ids);
	}

	@Override
	public String toString() {
		return "ShopifyProductImage [refId=" + refId + ", productId=" + productId + ", position="
				+ position + ", src=" + src + ", width=" + width + ", height=" + height + ", variant_ids=" + variant_ids
				+ "]";
	}
	
	
}