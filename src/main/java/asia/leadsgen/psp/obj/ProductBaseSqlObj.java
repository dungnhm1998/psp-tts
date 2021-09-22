package asia.leadsgen.psp.obj;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

@Getter
@Setter
@AllArgsConstructor
public class ProductBaseSqlObj implements SQLData {
    public static final String SQL_TYPE = "BASE_DETAIL";

    private String camp_id;
    private String base_id;
    private String base_name;
    private String base_cost;
    private String sale_price;
    private int sale_expected;
    private String sizes;
    private String colors;
    private String mock_up_url;
    private String default_color_id;

    public ProductBaseSqlObj() {
    }


    @Override
    public String getSQLTypeName() throws SQLException {
        return SQL_TYPE;
    }

    @Override
    public void readSQL(SQLInput stream, String typeName) throws SQLException {
        camp_id = stream.readString();
        base_id = stream.readString();
        base_name = stream.readString();
        base_cost = stream.readString();
        sale_price = stream.readString();
        sizes = stream.readString();
        colors = stream.readString();
        mock_up_url = stream.readString();
        default_color_id = stream.readString();

        sale_expected = stream.readInt();
    }

    @Override
    public void writeSQL(SQLOutput stream) throws SQLException {
        stream.writeString(camp_id);
        stream.writeString(base_id);
        stream.writeString(base_name);
        stream.writeString(base_cost);
        stream.writeString(sale_price);
        stream.writeString(sizes);
        stream.writeString(colors);
        stream.writeString(mock_up_url);
        stream.writeString(default_color_id);

        stream.writeInt(sale_expected);
    }

}
