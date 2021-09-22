package asia.leadsgen.psp.obj;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StoreOptionObj implements SQLData {
	public static final String SQL_TYPE = "STORE_OPTION_TYPE";
	private String s_id;
	private String s_store_id;
	private String s_type;
	private String s_option_id;
	private String s_option_name;
	private String s_bgp_option;
	private String s_terms;
	private String s_state;

	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		s_id = stream.readString();
		s_store_id = stream.readString();
		s_type = stream.readString();
		s_option_id = stream.readString();
		s_option_name = stream.readString();
		s_bgp_option = stream.readString();
		s_terms = stream.readString();
		s_state = stream.readString();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(s_id);
		stream.writeString(s_store_id);
		stream.writeString(s_type);
		stream.writeString(s_option_id);
		stream.writeString(s_option_name);
		stream.writeString(s_bgp_option);
		stream.writeString(s_terms);
		stream.writeString(s_state);
	}

}