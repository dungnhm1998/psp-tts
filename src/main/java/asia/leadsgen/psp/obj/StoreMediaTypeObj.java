package asia.leadsgen.psp.obj;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StoreMediaTypeObj implements SQLData {
	public static final String SQL_TYPE = "STORE_MEDIA_TYPE";
	private String id;
	private String storeId;
	private String type;
	private String optionId;
	private String optionName;
	private String bgpOption;
	private String terms;
	private String state;

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		id = stream.readString();
		storeId = stream.readString();
		type = stream.readString();
		optionId = stream.readString();
		optionName = stream.readString();
		bgpOption = stream.readString();
		terms = stream.readString();
		state = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(id);
		stream.writeString(storeId);
		stream.writeString(type);
		stream.writeString(optionId);
		stream.writeString(optionName);
		stream.writeString(bgpOption);
		stream.writeString(terms);
		stream.writeString(state);
	}

}