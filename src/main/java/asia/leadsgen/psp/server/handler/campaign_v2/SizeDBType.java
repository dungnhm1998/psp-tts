package asia.leadsgen.psp.server.handler.campaign_v2;

import java.math.BigDecimal;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class SizeDBType implements SQLData {
	public static final String SQL_TYPE = "PRODUCT_SIZE_TYPE";
	
	private String id;
	private BigDecimal salePrice;
	
	public SizeDBType() {
	}

	public SizeDBType(String id, BigDecimal salePrice) {
		super();
		this.id = id;
		this.salePrice = salePrice;
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		id = stream.readString();
		salePrice = stream.readBigDecimal();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(id);
		stream.writeBigDecimal(salePrice);
	}

	@Override
	public String toString() {
		return String.format(
		        "id:    %s\nsalePrice:	  %f",
		        id,
		        salePrice
		    );
	}
	
}
