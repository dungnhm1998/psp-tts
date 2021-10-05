package asia.leadsgen.psp.obj;

import java.sql.Date;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class FulfillmentReviewObj implements SQLData {
	
	public static final String SQL_TYPE = "FULFILLMENT_REVIEW_TYPE";
	
	private String id;
	private String campaignId;
	private String designFrontId;
	private String urlDesignFront;
	private String designBackId;
	private String urlDesignBack;
	private Date create;
	private Date update;
	private String state;
	private String userEmail;
	private String urlMockupFront;
	private String urlMockupBack;
	private String partnerId;
	private String baseId;
	private String baseName;
	private String note;
	private String urlPrintFront;
	private String urlPrintBack;
	private String colorId;
	private String colorName;
	private String colorValue;
	private String seq;
	private String sku;
	private String printType;
	private String source;
	private String printDetail;
	private Date assigned;
	private Date complete;
	private int baiduDownload;
	private String adjustType;
	
	private boolean designTypeFull;
	private String orderProductId;
	private String sizeName;
	private String typeId;
	private String sizeId;
	
	private boolean forInsert = false;
	private boolean forUpdate = false;
	
	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		id = stream.readString();
		campaignId = stream.readString();
		designFrontId = stream.readString();
		urlDesignFront = stream.readString();
		designBackId = stream.readString();
		urlDesignBack = stream.readString();
		create = stream.readDate();
		update = stream.readDate();
		state = stream.readString();
		userEmail = stream.readString();
		urlMockupFront = stream.readString();
		urlMockupBack = stream.readString();
		partnerId = stream.readString();
		baseId = stream.readString();
		baseName = stream.readString();
		note = stream.readString();
		urlPrintFront = stream.readString();
		urlPrintBack = stream.readString();
		colorId = stream.readString();
		colorName = stream.readString();
		colorValue = stream.readString();
		seq = stream.readString();
		sku = stream.readString();
		printType = stream.readString();
		source = stream.readString();
		printDetail = stream.readString();
		assigned = stream.readDate();
		complete = stream.readDate();
		baiduDownload = stream.readInt();
		adjustType = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(id);
		stream.writeString(campaignId);
		stream.writeString(designFrontId);
		stream.writeString(urlDesignFront);
		stream.writeString(designBackId);
		stream.writeString(urlDesignBack);
		stream.writeDate(create);
		stream.writeDate(update);
		stream.writeString(state);
		stream.writeString(userEmail);
		stream.writeString(urlMockupFront);
		stream.writeString(urlMockupBack);
		stream.writeString(partnerId);
		stream.writeString(baseId);
		stream.writeString(baseName);
		stream.writeString(note);
		stream.writeString(urlPrintFront);
		stream.writeString(urlPrintBack);
		stream.writeString(colorId);
		stream.writeString(colorName);
		stream.writeString(colorValue);
		stream.writeString(seq);
		stream.writeString(sku);
		stream.writeString(printType);
		stream.writeString(source);
		stream.writeString(printDetail);
		stream.writeDate(assigned);
		stream.writeDate(complete);
		stream.writeInt(baiduDownload);
		stream.writeString(adjustType);
	}
	
	public static FulfillmentReviewObj fromMap(Map map) {
		FulfillmentReviewObj obj = new FulfillmentReviewObj();
		obj.setId(ParamUtil.getString(map, AppParams.S_ID));
		obj.setCampaignId(ParamUtil.getString(map, AppParams.S_CAMPAIGN_ID));
		obj.setDesignFrontId(ParamUtil.getString(map, AppParams.S_DESIGN_FRONT_ID));
		obj.setUrlDesignFront(ParamUtil.getString(map, "S_URL_DESIGN_FRONT"));
		obj.setDesignBackId(ParamUtil.getString(map, "S_DESIGN_BACK_ID"));
		obj.setUrlDesignBack(ParamUtil.getString(map, "S_URL_DESIGN_BACK"));
		obj.setState(ParamUtil.getString(map, AppParams.S_STATE));
		obj.setUserEmail(ParamUtil.getString(map, "S_USER_EMAIL"));
		obj.setUrlMockupFront(ParamUtil.getString(map, "S_URL_MOCKUP_FRONT"));
		obj.setUrlMockupBack(ParamUtil.getString(map, "S_URL_MOCKUP_BACK"));
		obj.setPartnerId(ParamUtil.getString(map, AppParams.S_PARTNER_ID));
		obj.setBaseId(ParamUtil.getString(map, AppParams.S_BASE_ID));
		obj.setBaseName(ParamUtil.getString(map, AppParams.S_BASE_NAME));
		obj.setNote(ParamUtil.getString(map, AppParams.S_NOTE));
		obj.setUrlPrintFront(ParamUtil.getString(map, AppParams.S_URL_PRINT_FRONT));
		obj.setUrlPrintBack(ParamUtil.getString(map, "S_URL_PRINT_BACK"));
		obj.setColorId(ParamUtil.getString(map, AppParams.S_COLOR_ID));
		obj.setColorName(ParamUtil.getString(map, AppParams.S_COLOR_NAME));
		obj.setColorValue(ParamUtil.getString(map, AppParams.S_COLOR_VALUE));
		obj.setSeq(ParamUtil.getString(map, "S_SEQ"));
		obj.setSku(ParamUtil.getString(map, AppParams.S_SKU));
		obj.setPrintType(ParamUtil.getString(map, AppParams.S_PRINT_TYPE));
		obj.setSource(ParamUtil.getString(map, AppParams.S_SOURCE));
		obj.setPrintDetail(ParamUtil.getString(map, AppParams.S_PRINT_DETAIL));
		obj.setAdjustType(ParamUtil.getString(map, AppParams.S_ADJUST_TYPE));
		return obj;
	}
	
}
