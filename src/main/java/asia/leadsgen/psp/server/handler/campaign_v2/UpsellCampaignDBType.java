package asia.leadsgen.psp.server.handler.campaign_v2;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class UpsellCampaignDBType implements SQLData {
	public static final String SQL_TYPE = "UPSELL_CAMP_TYPE";
	
	private String upsellCampaignId;
	private String upsellCampaignName;
	private String type;
	private String upsellVariantId;
	private String upsellVariantName;
	private String upsellVariantUrl;
	private String upsellDiscountType;
	private int upsellDiscountValue;
	
	public UpsellCampaignDBType() {	
	}
	
	public UpsellCampaignDBType(String upsellCampaignId, String upsellCampaignName, String type, String upsellVariantId,
			String upsellVariantName, String upsellVariantUrl, String upsellDiscountType, int upsellDiscountValue) {
		super();
		this.upsellCampaignId = upsellCampaignId;
		this.upsellCampaignName = upsellCampaignName;
		this.type = type;
		this.upsellVariantId = upsellVariantId;
		this.upsellVariantName = upsellVariantName;
		this.upsellVariantUrl = upsellVariantUrl;
		this.upsellDiscountType = upsellDiscountType;
		this.upsellDiscountValue = upsellDiscountValue;
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}
	
	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		upsellCampaignId = stream.readString();
		upsellCampaignName = stream.readString();
		type = stream.readString();
		upsellVariantId = stream.readString();
		upsellVariantName = stream.readString();
		upsellVariantUrl = stream.readString();
		upsellDiscountType = stream.readString();
		upsellDiscountValue = stream.readInt();
	}
	
	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(upsellCampaignId);
		stream.writeString(upsellCampaignName);
		stream.writeString(type);
		stream.writeString(upsellVariantId);
		stream.writeString(upsellVariantName);
		stream.writeString(upsellVariantUrl);
		stream.writeString(upsellDiscountType);
		stream.writeInt(upsellDiscountValue);
	}

	@Override
	public String toString() {
		return String.format(
				"upsellCampaignId:      %s\nupsellCampaignName:    %s\ntype:      %s\nupsellVariantId:    %s\nupsellVariantName:    %s\nupsellVariantUrl:		%s\nupsellDiscountType:		%s\nupsellDiscountValue:	%d",
				upsellCampaignId,
				upsellCampaignName,
				type,
				upsellVariantId,
				upsellVariantName,
				upsellVariantUrl,
				upsellDiscountType,
				upsellDiscountValue);
	}

}
