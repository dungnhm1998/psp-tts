package asia.leadsgen.psp.server.handler.campaign_v2;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class ProductDBType implements SQLData {
	public static final String SQL_TYPE = "PRODUCT_CAMP_TYPE";
	
	private String baseId;
//	private String name;
	private int position;
	private boolean isBackView;
	private boolean isDefault;
	private String sizes;
	private String colors;
	private String defaultColorId;
	private int saleExpected;
//	private BigDecimal salePrice;
	private String state;

	public ProductDBType() {
	}

	public ProductDBType(String baseId, int position, boolean isBackView, boolean isDefault, String sizes, String colors, String defaultColorId, int saleExpected, String state) {
		super();
		this.baseId = baseId;
//		this.name = name;
		this.position = position;
		this.isBackView = isBackView;
		this.isDefault = isDefault;
		this.sizes = sizes;
		this.colors = colors;
		this.defaultColorId = defaultColorId;
		this.saleExpected = saleExpected;
//		this.salePrice = salePrice;
		this.state = state;
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		baseId = stream.readString();
//		name = stream.readString();
		position = stream.readInt();
		isBackView = stream.readBoolean();
		isDefault = stream.readBoolean();
		sizes = stream.readString();
		colors = stream.readString();
		defaultColorId = stream.readString();
		saleExpected = stream.readInt();
//		salePrice = stream.readBigDecimal();	
		state = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(baseId);
//		stream.writeString(name);
		stream.writeInt(position);
		stream.writeBoolean(isBackView);
		stream.writeBoolean(isDefault);
		stream.writeString(sizes);
		stream.writeString(colors);
		stream.writeString(defaultColorId);
		stream.writeInt(saleExpected);
//		stream.writeBigDecimal(salePrice);
		stream.writeString(state);
	}

	@Override
	public String toString() {
		return String.format(
	        "baseId:    %s\nposition:	  %d\nisBackView:   %s\nisDefault:   %s\nsizes:	%s\ncolors:   %s\ndefaultColorId:	%s\nsaleExpected:   %d",
	        baseId,
	        position,
	        isBackView,
	        isDefault,
	        sizes,
	        colors,
	        defaultColorId,
	        saleExpected
	    );
	}
	
}
