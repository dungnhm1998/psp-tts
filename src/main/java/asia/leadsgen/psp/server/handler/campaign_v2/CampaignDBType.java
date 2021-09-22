package asia.leadsgen.psp.server.handler.campaign_v2;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class CampaignDBType implements SQLData {
	public static final String SQL_TYPE = "CAMPAIGN_TYPE";
	
	private String userId;
	private String baseGroupId;
	private String title;
	private String description;
	private String domain;
	private String domainId;
//	private String url;
//	private String designFrontUrl;
//	private String designBackUrl;
//	private boolean isBackView;
	private boolean isPrivate;
//	private String stores;
	private String categories;
//	private String startTime;
//	private String endTime;
//	private boolean isAutoRelaunch;
	private String ggPixel;
	private String fbPixel;
	private String seoDesc;
	private String seoImageCover;
	private String seoTitle;
	private String state;
	
	public CampaignDBType() {
	}

	public CampaignDBType(String userId, String baseGroupId, String title, String description, String domain, String domainId,
			boolean isPrivate, String categories, String ggPixel, String fbPixel, String seoDesc, String seoImageCover, String seoTitle, String state) {
		super();
		this.userId = userId;
		this.baseGroupId = baseGroupId;
		this.title = title;
		this.description = description;
		this.domain = domain;
		this.domainId = domainId;
//		this.url = url;
//		this.designFrontUrl = designFrontUrl;
//		this.designBackUrl = designBackUrl;
//		this.isBackView = isBackView;
		this.isPrivate = isPrivate;
//		this.stores = stores;
		this.categories = categories;
//		this.startTime = startTime;
//		this.endTime = endTime;
//		this.isAutoRelaunch = isAutoRelaunch;
		this.ggPixel = ggPixel;
		this.fbPixel = fbPixel;
		this.seoDesc = seoDesc;
		this.seoImageCover = seoImageCover;
		this.seoTitle = seoTitle;
		this.state = state;
	}

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		userId = stream.readString();
		baseGroupId = stream.readString();
		title = stream.readString();
		description = stream.readString();
		domain = stream.readString();
		domainId = stream.readString();
//		url = stream.readString();
//		designFrontUrl = stream.readString();
//		designBackUrl = stream.readString();
//		isBackView = stream.readBoolean();
		isPrivate = stream.readBoolean();
//		stores = stream.readString();
		categories = stream.readString();
//		startTime = stream.readString();
//		endTime = stream.readString();
//		isAutoRelaunch = stream.readBoolean();
		ggPixel = stream.readString();
		fbPixel = stream.readString();
		seoDesc = stream.readString();
		seoImageCover = stream.readString();
		seoTitle = stream.readString();
		state =  stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(userId);
		stream.writeString(baseGroupId);
		stream.writeString(title);
		stream.writeString(description);
		stream.writeString(domain);
		stream.writeString(domainId);
//		stream.writeString(url);
//		stream.writeString(designFrontUrl);
//		stream.writeString(designBackUrl);
//		stream.writeBoolean(isBackView);
		stream.writeBoolean(isPrivate);
//		stream.writeString(stores);
		stream.writeString(categories);
//		stream.writeString(startTime);
//		stream.writeString(endTime);
//		stream.writeBoolean(isAutoRelaunch);
		stream.writeString(ggPixel);
		stream.writeString(fbPixel);
		stream.writeString(seoDesc);
		stream.writeString(seoImageCover);
		stream.writeString(seoTitle);
		stream.writeString(state);
	}

	@Override
	public String toString() {
		return String.format(
				"userId:        %s\nbaseGroupId:    %s\ntitle:       %s\ncategories:		%s",
				userId,
				baseGroupId,
				title,
				categories);
	}
	
}
