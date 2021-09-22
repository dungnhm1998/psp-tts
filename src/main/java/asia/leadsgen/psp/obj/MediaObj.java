package asia.leadsgen.psp.obj;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class MediaObj implements SQLData {
	  public static final String SQL_TYPE = "CREATE_LIST_MEDIA";
	
	String sUserId;
	String sType;
	String sTags;
	String sBaseId;
	String sUrl;
	String sState;
	String sName;
	String sSize;
	String sResolution;
	String sThumbUrl;
	String smd5;



	public String getsUserId() {
		return sUserId;
	}

	public void setsUserId(String sUserId) {
		this.sUserId = sUserId;
	}

	public String getsType() {
		return sType;
	}

	public void setsType(String sType) {
		this.sType = sType;
	}

	public String getsTags() {
		return sTags;
	}

	public void setsTags(String sTags) {
		this.sTags = sTags;
	}

	public String getsBaseId() {
		return sBaseId;
	}

	public void setsBaseId(String sBaseId) {
		this.sBaseId = sBaseId;
	}

	public String getsUrl() {
		return sUrl;
	}

	public void setsUrl(String sUrl) {
		this.sUrl = sUrl;
	}


	public String getsState() {
		return sState;
	}

	public void setsState(String sState) {
		this.sState = sState;
	}

	public String getsName() {
		return sName;
	}

	public void setsName(String sName) {
		this.sName = sName;
	}

	public String getsSize() {
		return sSize;
	}

	public void setsSize(String sSize) {
		this.sSize = sSize;
	}

	public String getsResolution() {
		return sResolution;
	}

	public void setsResolution(String sResolution) {
		this.sResolution = sResolution;
	}

	public String getsThumbUrl() {
		return sThumbUrl;
	}

	public void setsThumbUrl(String sThumbUrl) {
		this.sThumbUrl = sThumbUrl;
	}

	public String getSmd5() {
		return smd5;
	}

	public void setSmd5(String smd5) {
		this.smd5 = smd5;
	}

	@Override
	  public String getSQLTypeName() throws SQLException {
	    return SQL_TYPE;
	  }

	  @Override
	  public void readSQL(SQLInput stream, String typeName) throws SQLException {
	   
	  sUserId= stream.readString();
	sType= stream.readString();
	sTags= stream.readString();
	sBaseId= stream.readString();
	sUrl= stream.readString();
	sState= stream.readString();
	sName= stream.readString();
	sSize= stream.readString();
	sResolution= stream.readString();
	sThumbUrl= stream.readString();
	smd5= stream.readString();

	  }

	  @Override
	  public void writeSQL(SQLOutput stream) throws SQLException {
	    
	stream.writeString(sUserId);
	stream.writeString(sType);
	stream.writeString(sTags);
	stream.writeString(sBaseId);
	stream.writeString(sUrl);
	stream.writeString(sState);
	stream.writeString(sName);
	stream.writeString(sSize);
	stream.writeString(sResolution);
	stream.writeString(sThumbUrl);
	stream.writeString(smd5);

	  }

}
