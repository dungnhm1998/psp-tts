package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import asia.leadsgen.psp.obj.DropshipCustomApiItem;
import asia.leadsgen.psp.obj.DropshipImportFileRowObj;

public class DropshipImportFileRowsService extends MasterService {
	private static final String GET_ROWS = "{call PKG_DROPSHIP_IMPORT_FILE_ROWS.get_rows(?,?,?)}";
	private static final String GET_ROW_BY_ID = "{call PKG_DROPSHIP_IMPORT_FILE_ROWS.get_row_by_id(?,?,?,?)}";
	private static final String UPDATE_ROWS = "{call PKG_DROPSHIP_IMPORT_FILE_ROWS.update_rows(?,?,?,?,?,?,?,?,?,?)}";
	private static final String INSERT_JSON_GATEWAY_API = "{call PKG_DROPSHIP_IMPORT_FILE_ROWS.insert_receive_json_gateway_api(?,?,?,?,?)}";
	private static final String INSERT_CUSTOM_API_ORDER = "{call PKG_DROPSHIP_IMPORT_FILE_ROWS.insert_custom_api_order(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	private static final String GET_ROWS_FILE_ID = "{call PKG_DROPSHIP_IMPORT_FILE_ROWS.get_rows_by_file_id(?,?,?,?)}";

	
	public static List<DropshipImportFileRowObj> getRowsNotProcess() throws SQLException {
		List<DropshipImportFileRowObj> results = new ArrayList<DropshipImportFileRowObj>();
		List<Map> result = searchAll(GET_ROWS, new Object[] {});
		for (Map map : result) {
			results.add(DropshipImportFileRowObj.fromMap(map));
		}
		return results;

	}

	public static DropshipImportFileRowObj getRowsById(String id) throws SQLException {
		DropshipImportFileRowObj result = null;
		List<Map> results = searchAll(GET_ROW_BY_ID, new Object[] { id });
		for (Map map : results) {
			return DropshipImportFileRowObj.fromMap(map);
		}
		return result;

	}

	public static void updateRow(String id, String status, String order_id, String source, String error_note,
			String order_product_id, int reprocess) throws SQLException {
		logger.info("DropshipImportFileRowsService()- updateRow: id= " + id + ", status= " + status + ", order_id= "
				+ order_id + ", source= " + source + ", error_note= " + error_note + ", order_product_id= "
				+ order_product_id + ", reprocess= " + reprocess);
		update(UPDATE_ROWS, new Object[] { id, status, order_id, source, error_note, order_product_id, reprocess });
	}

	public static DropshipImportFileRowObj insertJsonGatewayApi(String id, String json) throws SQLException {
		DropshipImportFileRowObj result = null;
		List<Map> results = update(INSERT_JSON_GATEWAY_API, new Object[] { id, json });
		for (Map map : results) {
			return DropshipImportFileRowObj.fromMap(map);
		}
		return result;

	}

	public static List<DropshipImportFileRowObj> lookUp(String fileId) throws SQLException {
		List<DropshipImportFileRowObj> result = new ArrayList<>();
		List<Map> listResult = searchAll(GET_ROWS_FILE_ID, new Object[] { fileId });
		for (Map map : listResult) {
			result.add(DropshipImportFileRowObj.fromMap(map));
		}
		return result;
	}
	
	public static void insertCustomApiOrder(String fileId, String userId, String storeId, String refOrder, String orderId, String shippingEmail, String shippingName, 
			String shippingAddress1, String shippingAddress2, String shippingCity, String shippingZip, String shippingState, String shippingCountry, String shippingPhone, 
			String addressCheck, DropshipCustomApiItem orderItem, String source, String groupColumn, String type, String fileName, String minifiedJson) throws SQLException {
		
		insert(INSERT_CUSTOM_API_ORDER, new Object[] { fileId, userId, storeId, refOrder, orderId, shippingEmail, shippingName, shippingAddress1, shippingAddress2, 
				 shippingCity, shippingZip, shippingState, shippingCountry, shippingPhone, addressCheck, orderItem.getQuantity(), orderItem.getSku(), 
				 orderItem.getDesignUrlFront(), orderItem.getDesignUrlBack(), orderItem.getMockupUrlFront(), orderItem.getMockupUrlBack(), 
				 source, groupColumn, type, fileName, minifiedJson });
	}

}
