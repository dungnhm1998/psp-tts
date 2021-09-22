package asia.leadsgen.psp.server.handler.campaign_v2;

import java.math.BigDecimal;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class AllProductSizesDBType implements SQLData {
	public static final String SQL_TYPE = "ALL_PRODUCT_SIZES_TYPE";
	
	private String productId;
	private String baseId;
	private int saleExpected;
	private String sizeId;
	private BigDecimal salePrice;
	
	public AllProductSizesDBType() {
	}
	
	public AllProductSizesDBType(String productId, String baseId, int saleExpected, String sizeId, BigDecimal salePrice) {
		super();
		this.productId = productId;
		this.baseId = baseId;
		this.saleExpected = saleExpected;
		this.sizeId = sizeId;
		this.salePrice = salePrice;
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		productId = stream.readString();
		baseId = stream.readString();
		saleExpected = stream.readInt();
		sizeId = stream.readString();
		salePrice = stream.readBigDecimal();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(productId);
		stream.writeString(baseId);
		stream.writeInt(saleExpected);
		stream.writeString(sizeId);
		stream.writeBigDecimal(salePrice);
	}

}
