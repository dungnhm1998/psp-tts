package asia.leadsgen.psp.server.handler.campaign_v2;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class VariantDBType implements SQLData {
	public static final String SQL_TYPE = "PRODUCT_VARIANTS_TYPE";
	
	private String productId;
	private String colorId;
	private String colorValue;
	private String frontImgUrl;
	private String backImgUrl;
	private String frontDesignId;
	private String backDesignId;
	private String variantName;
	private String baseId;
	private boolean isDefault;
	private int nOrder;
	
	public VariantDBType() {
	}
	
	public VariantDBType(String productId, String colorId, String colorValue, String frontImgUrl, String backImgUrl,
			String frontDesignId, String backDesignId, String variantName, String baseId, boolean isDefault, int nOrder) {
		super();
		this.productId = productId;
		this.colorId = colorId;
		this.colorValue = colorValue;
		this.frontImgUrl = frontImgUrl;
		this.backImgUrl = backImgUrl;
		this.frontDesignId = frontDesignId;
		this.backDesignId = backDesignId;
		this.variantName = variantName;
		this.baseId = baseId;
		this.isDefault = isDefault;
		this.nOrder = nOrder;
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		productId = stream.readString();
		colorId = stream.readString();
		colorValue = stream.readString();
		frontImgUrl = stream.readString();
		backImgUrl = stream.readString();
		frontDesignId = stream.readString();
		backDesignId = stream.readString();
		variantName = stream.readString();
		baseId = stream.readString();
		isDefault = stream.readBoolean();
		nOrder = stream.readInt();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(productId);
		stream.writeString(colorId);
		stream.writeString(colorValue);
		stream.writeString(frontImgUrl);
		stream.writeString(backImgUrl);
		stream.writeString(frontDesignId);
		stream.writeString(backDesignId);
		stream.writeString(variantName);
		stream.writeString(baseId);
		stream.writeBoolean(isDefault);
		stream.writeInt(nOrder);
	}

}
