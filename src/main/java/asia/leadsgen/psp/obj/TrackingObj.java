package asia.leadsgen.psp.obj;

import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackingObj implements SQLData {
	
	public static final String SQL_TYPE = "UPDATE_TRACKING_ORDER";
	private String id;
	private String partnerId;
	private Clob tracking;
	private String groupId;
	private int code;
	private String message;
	private String state;
	private Date create;
	private Date update;
	
	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		id = stream.readString();
		partnerId = stream.readString();
		tracking = stream.readClob();
		groupId = stream.readString();
		code = stream.readInt();
		message = stream.readString();
		state = stream.readString();
		create = stream.readDate();
		update = stream.readDate();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(id);
		stream.writeString(partnerId);
		stream.writeClob(tracking);
		stream.writeString(groupId);
		stream.writeInt(code);
		stream.writeString(message);
		stream.writeString(state);
		stream.writeDate(create);
		stream.writeDate(update);
	}

	@Override
	public String toString() {
		return "TrackingObj [partnerId = " + partnerId + " , tracking = " + tracking + ""
				+ " , groupId = " + groupId + " , message = " + message + " , state = " + state + ""
				+ " , create = " + create + " , update = " + update + "]";
	}
	
}
