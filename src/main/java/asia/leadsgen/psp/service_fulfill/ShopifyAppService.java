package asia.leadsgen.psp.service_fulfill;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.HashedMap;

import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.ShopifyImageObj;
import asia.leadsgen.psp.obj.ShopifyOptionObj;
import asia.leadsgen.psp.obj.ShopifyProductObj;
import asia.leadsgen.psp.obj.ShopifyProductPullObj;
import asia.leadsgen.psp.obj.ShopifySyncedProductObj;
import asia.leadsgen.psp.obj.ShopifyVariantObj;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.ShopifyProduct;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.ShopifyProductImage;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.ShopifyProductOption;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.ShopifyProductVariant;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.BasePhoneCaseUtil;
import asia.leadsgen.psp.util.CampaignUtil;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;

public class ShopifyAppService {
	
	private static DataSource dataSource;
	
	static final String SHOPIFY_INSERT_PRODUCT = "{call PKG_SHOPIFY_APP.insert_fetched_product(?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String SHOPIFY_INSERT_PRODUCT_VARIANT = "{call PKG_SHOPIFY_APP.insert_product_variant(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String SHOPIFY_INSERT_PRODUCT_OPTION = "{call PKG_SHOPIFY_APP.insert_product_option(?,?,?,?,?,?,?,?)}";
	static final String SHOPIFY_INSERT_PRODUCT_IMAGE = "{call PKG_SHOPIFY_APP.insert_product_image(?,?,?,?,?,?,?,?,?,?)}";
	static final String SHOPIFY_CHECK_FETCHED_PRODUCT = "{call PKG_SHOPIFY_APP.check_fetched_product(?,?,?,?)}";
	static final String SHOPIFY_SEARCH_PRODUCT = "{call PKG_SHOPIFY_APP.search_product(?,?,?,?,?,?,?,?,?,?,?)}";
	static final String SHOPIFY_LOOKUP_PRODUCT = "{call PKG_SHOPIFY_APP.lookup_product(?,?,?,?)}";
	static final String SHOPIFY_GET_PRODUCT_VARIANTS = "{call PKG_SHOPIFY_APP.get_product_variants(?,?,?,?)}";
	static final String SHOPIFY_GET_SYNCED_PRODUCT_VARIANT = "{call PKG_SHOPIFY_APP.get_synced_product_variant(?,?,?,?)}";
	static final String SHOPIFY_SYNC_PRODUCT_VARIANT = "{call PKG_SHOPIFY_APP.sync_product_variant(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String SHOPIFY_GET_VARIANT_IMAGE = "{call PKG_SHOPIFY_APP.get_variant_image(?,?,?,?)}";
	static final String SHOPIFY_UPDATE_STATE_PRODUCT_VARIANT = "{call PKG_SHOPIFY_APP.update_state_synced_product_variant(?,?,?,?)}";
	static final String SHOPIFY_UPDATE_PRODUCT_OPTIONS = "{call PKG_SHOPIFY_APP.update_product_options(?,?,?,?,?,?,?,?)}";
	
	
	static final String SHOPIFY_UPDATE_REF_ID_TO_SYNCED_VARIANT = "{call PKG_SHOPIFY_APP.update_ref_id_to_synced_variant(?,?,?,?,?,?,?)}";
	static final String SHOPIFY_UPDATE_DESC_SHOPIFY_PRODUCT = "{call PKG_SHOPIFY_APP.update_desc_shopify_product(?,?,?,?,?,?)}";
	static final String SHOPIFY_UPDATE_PRODUCT_VARIANT_PRICES = "{call PKG_SHOPIFY_APP.update_prices_product_variant(?,?,?,?,?,?,?,?)}";
	static final String SHOPIFY_UPDATE_STATE_PRODUCT = "{call PKG_SHOPIFY_APP.update_state_product(?,?,?,?)}";
	static final String SHOPIFY_UPDATE_N_DEFAULT_SHOPIFY_IMAGE = "{call PKG_SHOPIFY_APP.update_n_default_shopify_image(?,?,?)}";
	static final String PRODUCT_GET_COLORS = "{call PKG_PRODUCT.product_get_colors(?,?,?,?)}";
	static final String SHOPIFY_GET_POSITION_SHOPIFY_IMAGE = "{call PKG_SHOPIFY_APP.get_position_shopify_image(?,?,?,?)}";
	static final String SHOPIFY_DELETE_PRODUCT_IMAGE = "{call PKG_SHOPIFY_APP.delete_product_images(?,?,?,?)}";
	static final String SHOPIFY_GET_IMAGE_ID_FROM_SYNCED_VARIANT = "{call PKG_SHOPIFY_APP.get_image_id_from_synced_variant(?,?,?,?)}";
	static final String SHOPIFY_GET_SIZE_IDS_FROM_SYNCED_VARIANT = "{call PKG_SHOPIFY_APP.get_size_ids_from_synced_variant(?,?,?,?)}";
	static final String SHOPIFY_UPDATE_SYNCED_PRODUCT_VARIANT = "{call PKG_SHOPIFY_APP.update_synced_product_variant(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	static final String SHOPIFY_UPDATE_PRODUCT = "{call PKG_SHOPIFY_APP.update_shopify_product(?,?,?,?,?,?)}";
	static final String SHOPIFY_UPDATE_N_SYNCED_PRODUCT_VARIANT = "{call PKG_SHOPIFY_APP.update_n_synced_product_variant(?,?,?,?)}";
	static final String SHOPIFY_DELETE_SYNCED_VARIANT = "{call PKG_SHOPIFY_APP.delete_synced_variant(?,?,?,?)}";
	
	static final String SHOPIFY_DELETE_PRODUCT = "{call PKG_SHOPIFY_APP.delete_product(?,?,?,?)}";
	static final String SHOPIFY_GET_PRODUCT_VARIANT_SYNC = "{call PKG_SHOPIFY_APP.get_product_variant_sync(?,?,?,?,?,?)}";
	static final String SHOPIFY_UPDATE_PRODUCT_COLORS = "{call PKG_SHOPIFY_APP.product_update_colors(?,?,?,?,?,?)}";
	static final String CREATE_GRPC_WEBHOOKS = "{call PKG_GRPC_WEBHOOKS.create_grpc_webhooks(?,?,?,?)}";
	static final String GET_SHOPIFY_PRODUCT_OPTION = "{call PKG_SHOPIFY_APP.get_shopify_product_option(?,?,?,?)}";
	static final String GET_EXITS_VARIANT_BY_BASE_ID = "{call PKG_SHOPIFY_APP.get_exits_variant_by_base_id(?,?,?,?,?)}";
	static final String SHOPIFY_SEARCH_PRODUCT_STATE = "{call PKG_SHOPIFY_APP.search_product_state(?,?,?,?,?,?,?,?,?,?,?)}";
	static final String SHOPIFT_INSERT_RECEIVE_WEBHOOK = "{call PKG_SHOPIFY_APP.insert_receive_webhook(?,?,?,?,?,?)}";

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public static Map<String, Object> insertFetchedProduct(String storeId, ShopifyProductObj obj, String storeName, String state, String bodyHtmlEncode)
    		throws SQLException {
    	
    	Map inputParams = new LinkedHashMap<Integer, String>();
    	inputParams.put(1, storeId);
    	inputParams.put(2, obj.getId());
    	inputParams.put(3, obj.getTitle());
    	inputParams.put(4, bodyHtmlEncode);
    	inputParams.put(5, obj.getProductType());
    	inputParams.put(6, obj.getTags());
    	inputParams.put(7, obj.getHandle());
    	inputParams.put(8, storeName);
    	inputParams.put(9, state);
    	
    	Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
    	outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.VARCHAR);
		outputParamsTypes.put(12, OracleTypes.CURSOR);
		
    	Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
    	outputParamsNames.put(10, AppParams.RESULT_CODE);
		outputParamsNames.put(11, AppParams.RESULT_MSG);
		outputParamsNames.put(12, AppParams.RESULT_DATA);
		
		Map insertResultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_INSERT_PRODUCT,
				inputParams, outputParamsTypes, outputParamsNames);
		
		LOGGER.info("insertFetchedProductResultMap= " + insertResultMap.toString());
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = resultDataList.get(0);

		LOGGER.fine("=> Shopify fetched product insert result: " + resultMap.toString());

		return resultMap;   	
    }
    
	public static Map<String, Object> insertProductVariant(ShopifyVariantObj obj, int synced) throws SQLException {
	    	
    	Map inputParams = new LinkedHashMap<Integer, String>();
    	inputParams.put(1, obj.getId());
    	inputParams.put(2, obj.getProductId());
    	inputParams.put(3, obj.getTitle());
    	inputParams.put(4, obj.getPrice());
    	inputParams.put(5, obj.getSku());
    	inputParams.put(6, obj.getPosition());
    	inputParams.put(7, obj.getOption1());
    	inputParams.put(8, obj.getOption2());
    	inputParams.put(9, obj.getOption3());
    	inputParams.put(10, obj.getImageId());
    	inputParams.put(11, synced);
    	
    	Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
    	outputParamsTypes.put(12, OracleTypes.NUMBER);
		outputParamsTypes.put(13, OracleTypes.VARCHAR);
		outputParamsTypes.put(14, OracleTypes.CURSOR);
		
    	Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
    	outputParamsNames.put(12, AppParams.RESULT_CODE);
		outputParamsNames.put(13, AppParams.RESULT_MSG);
		outputParamsNames.put(14, AppParams.RESULT_DATA);
		
		Map insertResultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_INSERT_PRODUCT_VARIANT,
				inputParams, outputParamsTypes, outputParamsNames);
		
//		LOGGER.info("=> Shopify product variant insert result: " + insertResultMap.toString());
		
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = resultDataList.get(0);

		LOGGER.fine("=> Shopify product variant insert result: " + resultMap.toString());

		return resultMap;
    }
	
	public static Map<String, Object> insertProductOption(ShopifyOptionObj obj) throws SQLException {
    	
    	Map inputParams = new LinkedHashMap<Integer, String>();
    	inputParams.put(1, obj.getId());
    	inputParams.put(2, obj.getProductId());
    	inputParams.put(3, obj.getName());
    	inputParams.put(4, obj.getPosition());
    	inputParams.put(5, String.join(",", obj.getValues()));
    	
    	Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
    	outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);
		
    	Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
    	outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);
		
		Map insertResultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_INSERT_PRODUCT_OPTION,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = resultDataList.get(0);

		LOGGER.fine("=> Shopify product option insert result: " + resultMap.toString());

		return resultMap;
    }
	
	public static Map<String, Object> insertProductImage(ShopifyImageObj obj, String ids) throws SQLException {
    	
    	Map inputParams = new LinkedHashMap<Integer, String>();
    	inputParams.put(1, obj.getId());
    	inputParams.put(2, obj.getProductId());
    	inputParams.put(3, obj.getPosition());
    	inputParams.put(4, obj.getSrc());
    	inputParams.put(5, obj.getWidth());
    	inputParams.put(6, obj.getHeight());
    	inputParams.put(7, ids);
    	
    	Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
    	outputParamsTypes.put(8, OracleTypes.NUMBER);
		outputParamsTypes.put(9, OracleTypes.VARCHAR);
		outputParamsTypes.put(10, OracleTypes.CURSOR);
		
    	Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
    	outputParamsNames.put(8, AppParams.RESULT_CODE);
		outputParamsNames.put(9, AppParams.RESULT_MSG);
		outputParamsNames.put(10, AppParams.RESULT_DATA);
		
		Map insertResultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_INSERT_PRODUCT_IMAGE,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = resultDataList.get(0);

		LOGGER.fine("=> Shopify product image insert result: " + resultMap.toString());

		return resultMap;
    }
	
	public static boolean isExistProduct(Long productId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_TOTAL);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_CHECK_FETCHED_PRODUCT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		return ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL, 0) > 0;	
	}
	
	public static Map searchProduct(String storeName, String content, String status, int page, int pageSize) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeName);
		inputParams.put(2, content);
		inputParams.put(3, status);
		inputParams.put(4, page);
		inputParams.put(5, pageSize);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);
		outputParamsTypes.put(9, OracleTypes.NUMBER);
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);
		outputParamsNames.put(9, AppParams.RESULT_TOTAL);
		outputParamsNames.put(10, "rs_synced");
		outputParamsNames.put(11, "rs_not_synced");
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_SEARCH_PRODUCT, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);
		int resultSynced = ParamUtil.getInt(resultMap, "rs_synced");
		int resultNotSynced = ParamUtil.getInt(resultMap, "rs_not_synced");
		
		List<Map> formatList = new ArrayList<Map>();
		for (Map resultDataItem : resultDataList) {	
			formatList.add(format(resultDataItem));
		}
		
		resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.TOTAL, resultTotal);
		resultMap.put("rs_synced", resultSynced);
		resultMap.put("rs_not_synced", resultNotSynced);
		resultMap.put(AppParams.DATA, formatList);

		LOGGER.fine("=> searchProduct result: " + resultMap.toString());

		return resultMap;	
	}
    
    private static Map format(Map resultDataItem) throws SQLException {
    	Map resultMap = new LinkedHashMap<>();
    	
    	String storeId = ParamUtil.getString(resultDataItem, AppParams.S_STORE_ID);  	
    	Map storeMap = DropShipStoreService.lookUp(storeId);                
		String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
		
    	String productRefId = Long.toString((ParamUtil.getLong(resultDataItem, AppParams.S_PRODUCT_REF_ID)));
    	resultMap.put(AppParams.PRODUCT_REF_ID, productRefId);
    	resultMap.put(AppParams.TITLE, ParamUtil.getString(resultDataItem, AppParams.S_TITLE));
    	resultMap.put(AppParams.TAGS, ParamUtil.getString(resultDataItem, AppParams.S_TAGS));
    	resultMap.put(AppParams.STATE, ParamUtil.getString(resultDataItem, AppParams.S_STATE));
    	resultMap.put(AppParams.IMAGE, ParamUtil.getString(resultDataItem, AppParams.S_IMAGE));
    	resultMap.put(AppParams.DESCRIPTION, ParamUtil.getString(resultDataItem, AppParams.S_DESC));
    	resultMap.put(AppParams.DOMAIN, domain);
    	resultMap.put(AppParams.HANDLE, ParamUtil.getString(resultDataItem, AppParams.S_HANDLE));
//    	String productId = ParamUtil.getString(resultDataItem, AppParams.S_PRODUCT_ID);
//    	resultMap.put(AppParams.PRODUCT_ID, productId);
    	String campaignId = ParamUtil.getString(resultDataItem, AppParams.S_CAMPAIGN_ID);
    	resultMap.put(AppParams.CAMPAIGN_ID, campaignId);
    	resultMap.put(AppParams.BASE_GROUP_ID, ParamUtil.getString(resultDataItem, AppParams.S_BASE_GROUP_ID));
    	resultMap.put(AppParams.TOTAL_VARIANT, ParamUtil.getInt(resultDataItem, AppParams.N_TOTAL_VARIANT));
    	resultMap.put(AppParams.SYNCED, ParamUtil.getInt(resultDataItem, AppParams.N_SYNCED));	
    	resultMap.put(AppParams.NOT_SYNCED, ParamUtil.getInt(resultDataItem, AppParams.N_TOTAL_VARIANT)
    			- ParamUtil.getInt(resultDataItem, AppParams.N_SYNCED));
//    	resultMap.put(AppParams.DESIGN_SIDES, ParamUtil.getInt(resultDataItem, AppParams.S_DESIGN_SIDES));
    	resultMap.put(AppParams.CREATE_DATE, ParamUtil.getString(resultDataItem, AppParams.D_CREATE));
    	resultMap.put(AppParams.UPDATE_DATE, ParamUtil.getString(resultDataItem, AppParams.D_UPDATE));
   	
		return resultMap;
	}
    
    public static Map lookup(Long productId) throws SQLException, ParseException {
    	
    	Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_LOOKUP_PRODUCT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		Map result = format(resultDataList.get(0));	
		Map getVariant = getVariant(productId);
		List<Map> variants = ParamUtil.getListData(getVariant, AppParams.VARIANTS);
		result.put(AppParams.VARIANTS, variants);
			
		LOGGER.fine("=> lookupProduct result: " + resultMap.toString());
		
		return result;	
    }
    
    public static Map getVariant(Long productId) throws SQLException, ParseException {
    	
    	Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_GET_PRODUCT_VARIANTS, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		List<Map> formatList = new ArrayList<Map>();

		for (Map resultDataItem : resultDataList) {
			Map formatVariantMap = formatVariant(resultDataItem);
//			LOGGER.info("=> formatVariantMap result: " + formatVariantMap.toString());
			if (formatVariantMap != null && formatVariantMap.isEmpty() == false) {
				formatList.add(formatVariantMap);
			}		
		}
		
		Map result = new LinkedHashMap<>();
		result.put(AppParams.VARIANTS, formatList);
		return result;
    }

	private static Map formatVariant(Map resultDataItem) throws SQLException, ParseException {
		Map resultMap = new LinkedHashMap<>();
		Long variantRefId = ParamUtil.getLong(resultDataItem, AppParams.S_VARIANT_REF_ID);
    	resultMap.put(AppParams.REFERENCE_ID, variantRefId);
    	resultMap.put(AppParams.TITLE, ParamUtil.getString(resultDataItem, AppParams.S_VARIANT_TITLE));

    	
    	resultMap.put(AppParams.POSITION, ParamUtil.getInt(resultDataItem, AppParams.N_POSITION));
    	resultMap.put(AppParams.SKU_REF, ParamUtil.getString(resultDataItem, AppParams.S_SKU_REF));
    	Double retailPrice = ParamUtil.getDouble(resultDataItem, AppParams.S_PRICE);
    	resultMap.put(AppParams.RETAIL_PRICE, retailPrice);
    	
    	Long imageId = ParamUtil.getLong(resultDataItem, AppParams.S_IMAGE_ID);	
    	if (imageId != null && imageId != 0) {
    		Map imageMap = getVariantImage(imageId);
    		resultMap.put(AppParams.IMAGE_ID, imageId);
        	resultMap.put(AppParams.IMAGE, ParamUtil.getString(imageMap, AppParams.S_IMAGE));
    	}
    	
    	int synced = ParamUtil.getInt(resultDataItem, AppParams.N_SYNCED);
    	resultMap.put(AppParams.SYNCED, synced);
    		
    	if (synced != 0) {
    		Map syncedVariantMap = getSyncedProductVariant(variantRefId);
//    		LOGGER.info("=> syncedVariantMap : " + syncedVariantMap.toString());
    		if (syncedVariantMap != null && syncedVariantMap.isEmpty() == false) {
    			syncedVariantMap.put(AppParams.RETAIL_PRICE, retailPrice);
        		resultMap.put(AppParams.SYNCED_VARIANT, syncedVariantMap);
    		} else {
    			resultMap.put(AppParams.SYNCED_VARIANT, Collections.EMPTY_MAP);
    		}		
    	}
    	
		return resultMap;
	}
	
	public static Map getSyncedProductVariant(Long variantId) throws SQLException, ParseException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, variantId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_GET_SYNCED_PRODUCT_VARIANT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		
		Map result = new LinkedHashMap<>();
		result = formatSyncedVariant(resultDataList.get(0));
	
		return result;	
	}

	private static Map formatSyncedVariant(Map resultData) throws SQLException {
		Map resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.VARIANT_SYNC_ID, ParamUtil.getString(resultData, AppParams.S_BGP_VARIANT_ID));
		
		String baseId = ParamUtil.getString(resultData, AppParams.S_BASE_ID);
		Map baseMap = BaseService.get(baseId);
		
		List<Map> baseSizePhoneCase = new ArrayList<Map>();
		String basePhoneCaseIds = "";
		List<String> basePhoneCaseIdList = new ArrayList<>();
		
		if (BasePhoneCaseUtil.isPhoneCase(baseId)) {
			basePhoneCaseIds = BasePhoneCaseUtil.getBasePhoneCaseIds();
			basePhoneCaseIdList = Arrays.asList(basePhoneCaseIds.trim().split(","));
			for (String basePhoneCaseId : basePhoneCaseIdList) {
				List<Map> baseSize = BaseService.listBaseSizeAndPriceAndDesignPhoneCaseInfo(basePhoneCaseId, false);
				baseSizePhoneCase.addAll(baseSize);
			}
			baseMap.put("base_size", baseSizePhoneCase);
		} else {
			List<Map> baseSizeList = BaseService.listBaseSizeAndPriceAndDesignPhoneCaseInfo(baseId, false);
			baseMap.put("base_size", baseSizeList);
		}
		resultMap.put(AppParams.BASE, baseMap);
		
		resultMap.put(AppParams.BASE_NAME, ParamUtil.getString(resultData, AppParams.S_BASE_NAME));
		resultMap.put(AppParams.IMG_URL, ParamUtil.getString(resultData, AppParams.S_IMG_URL));
		String sizeId = ParamUtil.getString(resultData, AppParams.S_SIZE_ID);
		resultMap.put(AppParams.SIZE_ID, sizeId);
		resultMap.put(AppParams.SIZE_NAME, ParamUtil.getString(resultData, AppParams.S_SIZE_NAME));
		resultMap.put(AppParams.COLOR_ID, ParamUtil.getString(resultData, AppParams.S_COLOR_ID));
		resultMap.put(AppParams.COLOR_NAME, ParamUtil.getString(resultData, AppParams.S_COLOR_NAME));
		resultMap.put(AppParams.COLOR, ParamUtil.getString(resultData, AppParams.S_COLOR));
		resultMap.put(AppParams.MEDIA_ID, ParamUtil.getString(resultData, AppParams.S_MEDIA_ID));
		resultMap.put(AppParams.STATE, ParamUtil.getString(resultData, AppParams.S_STATE));
		resultMap.put(AppParams.PRODUCT_TYPE, ParamUtil.getString(resultData, AppParams.S_PRODUCT_TYPE));
		String productId = ParamUtil.getString(resultData, AppParams.S_PRODUCT_ID);
		resultMap.put(AppParams.PRODUCT_ID, productId);
		String campaignId = ParamUtil.getString(resultData, AppParams.S_CAMPAIGN_ID);
    	Map designInfo = CampaignUtil.getMapProductDesigns(campaignId);
    	List<Map> designInfoList = new ArrayList<>();
    	
    	if (designInfo != null && designInfo.isEmpty() == false) {
    		designInfoList = ParamUtil.getListData(designInfo, productId);
    		for (Map m : designInfoList) {
    			String type = ParamUtil.getString(m, AppParams.TYPE);
    			if (type.equalsIgnoreCase("full")) {
    				m.replace(AppParams.TYPE, AppParams.FRONT);
    			}
    		}
    	}
    	resultMap.put("design_info", designInfoList);
		
		Map designMap = new LinkedHashMap<>();
		String mockup_front_url = ParamUtil.getString(resultData, AppParams.S_FRONT_IMG_URL) + "?";
		String mockup_back_url = ParamUtil.getString(resultData, AppParams.S_BACK_IMG_URL);
		String design_front_url = ParamUtil.getString(resultData, AppParams.S_DESIGN_FRONT_URL);
		String design_back_url = ParamUtil.getString(resultData, AppParams.S_DESIGN_BACK_URL);	
		designMap.put(AppParams.MOCKUP_FRONT_URL, mockup_front_url);
		designMap.put(AppParams.MOCKUP_BACK_URL, mockup_back_url);
		designMap.put(AppParams.DESIGN_FRONT_URL, design_front_url);
		designMap.put(AppParams.DESIGN_BACK_URL, design_back_url);
		resultMap.put(AppParams.DESIGNS, designMap);
		resultMap.put(AppParams.DESIGN_SIDES, ParamUtil.getInt(resultData, AppParams.S_DESIGN_SIDES));
			
		Map price = BaseSizeService.getBasePriceByProductIdAndSizeId(baseId, sizeId);
		resultMap.put(AppParams.DROPSHIP_BASE_COST, ParamUtil.getDouble(price, AppParams.DROPSHIP_BASE_COST));
		resultMap.put(AppParams.SECOND_SIDE_PRICE, ParamUtil.getDouble(price, AppParams.SECOND_SIDE_PRICE));
		
		resultMap.put(AppParams.CURRENCY, ParamUtil.getString(resultData, AppParams.S_CURRENCY));
		resultMap.put(AppParams.SALE_EXPECTED, ParamUtil.getInt(resultData, AppParams.N_SALE_EXPECTED));
		
		return resultMap;
	}
	
	public static Map syncProductVariant(ShopifySyncedProductObj obj) throws SQLException {
		
//		LOGGER.info("ShopifySyncedProductObj= " + obj.toString());
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, obj.getBgpVariantId());
		inputParams.put(2, obj.getBaseId());
		inputParams.put(3, obj.getSizeId());
		inputParams.put(4, obj.getColorId());
		inputParams.put(5, obj.getImageId());
		inputParams.put(6, obj.getFrontDesign());
		inputParams.put(7, obj.getFrontMockup());
		inputParams.put(8, obj.getBackDesign());
		inputParams.put(9, obj.getBackMockup());
		inputParams.put(10, obj.getBgpProductId());
		inputParams.put(11, obj.getSalePrice());
		inputParams.put(12, obj.getCurrency());
		inputParams.put(13, obj.getSaleExpected());
		inputParams.put(14, obj.getProductRefId());
		inputParams.put(15, obj.getVariantRefId());
		inputParams.put(16, obj.getSku());
		inputParams.put(17, obj.getColorName());
		inputParams.put(18, obj.getSizeName());
		inputParams.put(19, obj.getColorValue());
		inputParams.put(20, obj.getSkuRef());
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(21, OracleTypes.NUMBER);
		outputParamsTypes.put(22, OracleTypes.VARCHAR);
		outputParamsTypes.put(23, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(21, AppParams.RESULT_CODE);
		outputParamsNames.put(22, AppParams.RESULT_MSG);
		outputParamsNames.put(23, AppParams.RESULT_DATA);
		
		Map insertResultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_SYNC_PRODUCT_VARIANT, inputParams,
				outputParamsTypes, outputParamsNames);
//		LOGGER.info("insertResultMap " + insertResultMap.toString());
		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);
		
		Map resultMap = formatSyncedVariant(resultDataList.get(0));
		
//		LOGGER.info("=> Shopify sync product result: " + resultMap.toString());
		
		return resultMap;		
	}
	
	public static Map getVariantImage(Long imageId) throws SQLException, ParseException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, imageId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultList = DBProcedureUtil.execute(dataSource, SHOPIFY_GET_VARIANT_IMAGE, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultList, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultList, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(resultList, AppParams.RESULT_DATA);
		if (resultDataList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		
		Map resultMap = resultDataList.get(0);
		
		return resultMap;	
	}
	
	public static void updateStateSyncedProductVariant(Long id, String state) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, state);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_STATE_PRODUCT_VARIANT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static void updateProductOptions(ShopifyOptionObj obj) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
    	inputParams.put(1, obj.getId());
    	inputParams.put(2, obj.getProductId());
    	inputParams.put(3, obj.getPosition());
    	inputParams.put(4, obj.getName());
    	inputParams.put(5, String.join(",", obj.getValues()));
    	
    	Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
    	outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);
		
    	Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
    	outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_PRODUCT_OPTIONS,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static void updateRefIdToSyncedVariant(ShopifyVariantObj vObj) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, vObj.getProductId());
		inputParams.put(2, vObj.getId());
		inputParams.put(3, vObj.getImageId());
		inputParams.put(4, vObj.getSku());
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		outputParamsTypes.put(7, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		outputParamsNames.put(7, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_REF_ID_TO_SYNCED_VARIANT,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}	
	}
	
	public static void updateDescShopifyProduct(Long productRefId, String desc, String title) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productRefId);
		inputParams.put(2, desc);
		inputParams.put(3, title);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_DESC_SHOPIFY_PRODUCT,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}	
	}
	
	public static void updatePricesProductVariant(Long variantRefId, Long productRefId, String productId, String sizeId, String salePrice) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, variantRefId);
		inputParams.put(2, productRefId);
		inputParams.put(3, productId);
		inputParams.put(4, sizeId);
		inputParams.put(5, salePrice);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_PRODUCT_VARIANT_PRICES,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}	
	}
	
	public static void updateStateProduct(Long id, String state) throws SQLException {
		
		LOGGER.info("update State ProductId: " + id + " - " + state);
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, id);
		inputParams.put(2, state);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_STATE_PRODUCT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static void updateNdefaultShopifyImg(String productId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_N_DEFAULT_SHOPIFY_IMAGE, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static String getProductColors(String productId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, PRODUCT_GET_COLORS, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(resultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map result = resultDataList.get(0);
		
		String colors = ParamUtil.getString(result, AppParams.S_COLORS);
		
		return colors;
	}
	
	public static Integer getPositionShopifyImage(Long productRefId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productRefId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_GET_POSITION_SHOPIFY_IMAGE, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		Map result = resultDataList.get(0);
		
		return ParamUtil.getInt(result, AppParams.N_POSITION);
	}
	
	public static void deleteProductImage(Long productRefId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productRefId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_DELETE_PRODUCT_IMAGE, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static Long getImageIdFromSyncedVariant(String variantId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, variantId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_GET_IMAGE_ID_FROM_SYNCED_VARIANT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		if (resultDataList.isEmpty()) {
			return null;
		}
		
		Map result = resultDataList.get(0);
		
		return ParamUtil.getLong(result, AppParams.S_IMAGE_ID);
	}
	
	public static String getSizeIdsFromSyncedVariant(String variantId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, variantId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_GET_SIZE_IDS_FROM_SYNCED_VARIANT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		if (resultDataList.isEmpty()) {
			return null;
		}
		
		Map result = resultDataList.get(0);
		
		return ParamUtil.getString(result, AppParams.S_SIZES);
	}
	
	public static Map updateSyncedProductVariant(ShopifySyncedProductObj obj) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, obj.getBgpVariantId());
		inputParams.put(2, obj.getSizeId());
		inputParams.put(3, obj.getColorId());
		inputParams.put(4, obj.getFrontMockup());
		inputParams.put(5, obj.getBackMockup());
		inputParams.put(6, obj.getVariantRefId());
		inputParams.put(7, obj.getSku());
		inputParams.put(8, obj.getColorName());
		inputParams.put(9, obj.getSizeName());
		inputParams.put(10, obj.getBaseId());
		inputParams.put(11, obj.getFrontDesign());
		inputParams.put(12, obj.getBackDesign());
		inputParams.put(13, obj.getBgpProductId());
		inputParams.put(14, obj.getColorValue());
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(15, OracleTypes.NUMBER);
		outputParamsTypes.put(16, OracleTypes.VARCHAR);
		outputParamsTypes.put(17, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(15, AppParams.RESULT_CODE);
		outputParamsNames.put(16, AppParams.RESULT_MSG);
		outputParamsNames.put(17, AppParams.RESULT_DATA);
		
		Map updateMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_SYNCED_PRODUCT_VARIANT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(updateMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultDataList = ParamUtil.getListData(updateMap, AppParams.RESULT_DATA);
		
		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(updateMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}
		
		Map resultMap = resultDataList.get(0);
		
		return resultMap;		
	}
	
	public static void updateShopifyProduct(Long productRefId, String campaignId, String collectionIds) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productRefId);
    	inputParams.put(2, campaignId);
    	inputParams.put(3, collectionIds);
    	
    	Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_PRODUCT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static void deleteShopifyProduct(String storeName, Long productRefId) throws SQLException {
		LOGGER.info("store_name= " + storeName + " --- productRefId= " + productRefId);
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeName);
		inputParams.put(2, productRefId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_DELETE_PRODUCT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static void mergeProduct(String storeId, String store_name, ShopifyProductPullObj obj)  throws UnirestException, SQLException, ParseException {
		LOGGER.info("mergeProduct()...");
		List<ShopifyProduct> lines_product = new ArrayList<ShopifyProduct>();
		List<ShopifyProductImage> lines_image = new ArrayList<ShopifyProductImage>();
		List<ShopifyProductOption> lines_option = new ArrayList<ShopifyProductOption>();
		List<ShopifyProductVariant> lines_variant = new ArrayList<ShopifyProductVariant>();
		List<ShopifyProductObj> lstProduct = new ArrayList<ShopifyProductObj>();
		if(obj.getProduct() != null) {
			lstProduct.add(obj.getProduct());
		} else if(obj.getProducts() != null && obj.getProducts().size() > 0) {
			lstProduct = obj.getProducts();
		}
		
		for (ShopifyProductObj shopifyProductObj : lstProduct) {
			lines_product.add(new ShopifyProduct(storeId, shopifyProductObj.getId(), shopifyProductObj.getTitle(),
					StringUtil.urlEncode(shopifyProductObj.getBodyHtml()), shopifyProductObj.getProductType(), shopifyProductObj.getTags(),
					shopifyProductObj.getHandle(), store_name));
			for (ShopifyImageObj shopifyImageObj : shopifyProductObj.getImages()) {
				String variant_ids = "";
				if(shopifyImageObj.getVariantIds() != null && shopifyImageObj.getVariantIds().size() > 0) {
					variant_ids = String.join(", ", shopifyImageObj.getVariantIds().toString());
				}
				lines_image.add(new ShopifyProductImage(shopifyImageObj.getId(), shopifyImageObj.getProductId(),
						shopifyImageObj.getPosition().longValue(), shopifyImageObj.getSrc(),
						shopifyImageObj.getWidth().longValue(), shopifyImageObj.getHeight().longValue(),
						variant_ids));
			}
			
			
			for (ShopifyOptionObj shopifyOptionObj : shopifyProductObj.getOptions()) {
				lines_option.add(new ShopifyProductOption(shopifyOptionObj.getId(), shopifyOptionObj.getProductId(),
						shopifyOptionObj.getName(), shopifyOptionObj.getPosition().longValue(),
						String.join(", ", shopifyOptionObj.getValues())));
			}

			
			for (ShopifyVariantObj shopifyVariantObj : shopifyProductObj.getVariants()) {
				Long nImageId = 0L;
				if (shopifyVariantObj.getImageId() != null) {
					nImageId = shopifyVariantObj.getImageId();
				}
				lines_variant.add(new ShopifyProductVariant(shopifyVariantObj.getId(), shopifyVariantObj.getProductId(),
						shopifyVariantObj.getTitle(), shopifyVariantObj.getPrice(), shopifyVariantObj.getSku(),
						shopifyVariantObj.getPosition().longValue(), shopifyVariantObj.getOption1(),
						shopifyVariantObj.getOption2(), shopifyVariantObj.getOption3(), nImageId));
//				LOGGER.info("lines_variant: " + lines_variant.toString());
			}
		}
		
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
//				LOGGER.info("lines_product= " + lines_product.toString());
//				LOGGER.info("lines_image= " + lines_image.toString());
//				LOGGER.info("lines_option= " + lines_option.toString());
//				LOGGER.info("lines_variant= " + lines_variant.toString());
				
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				ShopifyProduct[] arrShopifyProduct = new  ShopifyProduct[lines_product.size()];
				arrShopifyProduct = lines_product.toArray(arrShopifyProduct);
				
				ShopifyProductImage[] arrShopifyProductImage = new  ShopifyProductImage[lines_image.size()];
				arrShopifyProductImage = lines_image.toArray(arrShopifyProductImage);
				
				
				ShopifyProductOption[] arrShopifyProductOption = new  ShopifyProductOption[lines_option.size()];
				arrShopifyProductOption = lines_option.toArray(arrShopifyProductOption);
				
				ShopifyProductVariant[] arrShopifyProductVariant = new  ShopifyProductVariant[lines_variant.size()];
				arrShopifyProductVariant = lines_variant.toArray(arrShopifyProductVariant);
				
				java.sql.Array array_to_product = con.createOracleArray("SHOPIFY_PRODUCT_T", arrShopifyProduct);
				java.sql.Array array_to_product_image = con.createOracleArray("SHOPIFY_PRODUCT_IMAGE_T", arrShopifyProductImage);
				java.sql.Array array_to_product_option = con.createOracleArray("SHOPIFY_PRODUCT_OPTION_T", arrShopifyProductOption);
				java.sql.Array array_to_product_variant = con.createOracleArray("SHOPIFY_PRODUCT_VARIANT_T", arrShopifyProductVariant);
				
				try (CallableStatement cstmt = con.prepareCall("{call PKG_SHOPIFY_APP.merge_product_variant(?,?,?,?)}");) {
					cstmt.setArray(1, array_to_product); // Set input parameter
					cstmt.setArray(2, array_to_product_variant); // Set input parameter
					cstmt.setArray(3, array_to_product_image); // Set input parameter
					cstmt.setArray(4, array_to_product_option); // Set input parameter
	
					cstmt.execute();
				}
			}
		}
	}

	public static Map<String, Object> getShopifyProductVariantSync(String store_id, Long product_id, Long variant_id) throws SQLException {
		LOGGER.info("store_id= " + store_id + " --- product_id= " + product_id + " --- variant_id= " + variant_id);
		
    	Map inputParams = new LinkedHashMap<Integer, String>();
    	inputParams.put(1, store_id);
    	inputParams.put(2, product_id);
    	inputParams.put(3, variant_id);
    	
    	Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
    	outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);
		outputParamsTypes.put(6, OracleTypes.CURSOR);
		
    	Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
    	outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		outputParamsNames.put(6, AppParams.RESULT_DATA);
		
		Map insertResultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_GET_PRODUCT_VARIANT_SYNC,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = resultDataList.get(0);

//		LOGGER.info("=> Shopify product variant sync result: " + resultMap.toString());

		return resultMap;
    }
	
	public static void updateNSyncedProductVariant(Long productRefId, Long variantRefId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productRefId);
		inputParams.put(2, variantRefId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_N_SYNCED_PRODUCT_VARIANT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static void deletedSyncedVariant(String variantId, String sizeId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, variantId);
		inputParams.put(2, sizeId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_DELETE_SYNCED_VARIANT, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static Map updateColors(String id, String colors, String defaultColorId) throws SQLException {
        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, id);
        inputParams.put(2, colors);
        inputParams.put(3, defaultColorId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(4, OracleTypes.NUMBER);
        outputParamsTypes.put(5, OracleTypes.VARCHAR);
        outputParamsTypes.put(6, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(4, AppParams.RESULT_CODE);
        outputParamsNames.put(5, AppParams.RESULT_MSG);
        outputParamsNames.put(6, AppParams.RESULT_DATA);

        Map updateResultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_UPDATE_PRODUCT_COLORS, inputParams, outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
        }
        
        List<Map> resultDataList = ParamUtil.getListData(updateResultMap, AppParams.RESULT_DATA);
		
		Map resultMap = resultDataList.get(0);

		return resultMap;	
    }
	
	public static String checkSku(String baseId, String sizeId, String colorName) throws SQLException {
		
		String sku = "";
		Set<String> skuList = new LinkedHashSet<String>();
		Set<String> partnerList = new LinkedHashSet<String>();
		List<Map> skuListMap = BaseSKUService.getSkuByBaseIdSizeIdColorName(baseId, sizeId, colorName);
		if (skuListMap.isEmpty()) {
			skuListMap = BaseSKUService.getSkuByBaseIdSizeIdColorName(baseId, sizeId, "");
		}
		
		if (skuListMap.isEmpty()) {
			return sku;
		}		
		
		if (skuListMap.size() > 1) {
			for (Map skuMap : skuListMap) {
				String partnerId = ParamUtil.getString(skuMap, AppParams.S_PARTNER_ID);
				partnerList.add(partnerId);		
			}

			if (partnerList.contains("feeiFVNPgjmnzB8I")) {
				for (Map skuMap : skuListMap) {
					String partnerId = ParamUtil.getString(skuMap, AppParams.S_PARTNER_ID);
					
					if (partnerId.equalsIgnoreCase("feeiFVNPgjmnzB8I")) {
						sku = ParamUtil.getString(skuMap, AppParams.S_SKU);
					}
				}
				
			} else {
				for (Map skuMap : skuListMap) {
					sku = ParamUtil.getString(skuMap, AppParams.S_SKU);
					skuList.add(sku);				
				}			
			}
			
		} else {
			sku = ParamUtil.getString(skuListMap.get(0), AppParams.S_SKU);
		}
			
		if (skuList.isEmpty() == false) {
			sku = String.join(",", skuList);
		}
//		LOGGER.info("sku: " + sku);
		
		return sku;
	}
	
	public static Map<String, Object> insertGrpcWebhooks(String payload)
    		throws SQLException {
    	
    	Map inputParams = new LinkedHashMap<Integer, String>();
    	inputParams.put(1, payload);
    	
    	Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
    	outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		
    	Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
    	outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map insertResultMap = DBProcedureUtil.execute(dataSource, CREATE_GRPC_WEBHOOKS,
				inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(insertResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG));
		}

		List<Map> resultDataList = ParamUtil.getListData(insertResultMap, AppParams.RESULT_DATA);

		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(insertResultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}

		Map resultMap = resultDataList.get(0);

		LOGGER.fine("=> Shopify fetched product insert result: " + resultMap.toString());

		return resultMap;   	
    }
	
	public static List<Map> getShopifyProductOption(Long productRefId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productRefId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, GET_SHOPIFY_PRODUCT_OPTION, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		if (resultDataList.isEmpty()) {
			throw new OracleException(
					ParamUtil.getString(resultMap, AppParams.RESULT_MSG, SystemError.DATA_NOT_FOUND.getName()));
		}
		
		return resultDataList;
	}
	
	public static List<Map> getExitsShopifyVariant(Long productRefId, String baseId) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, productRefId);
		inputParams.put(2, baseId);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);
		outputParamsTypes.put(5, OracleTypes.CURSOR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		outputParamsNames.put(5, AppParams.RESULT_DATA);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, GET_EXITS_VARIANT_BY_BASE_ID, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		LOGGER.info("resultDataList= " + resultDataList.toString());
		
		List<Map> formatList = new ArrayList<Map>();
		
		for (Map resultData : resultDataList) {
			Map format = new HashedMap<>();
			
			String sizeId = ParamUtil.getString(resultData, AppParams.S_SIZE_ID);
			String sizeName = ParamUtil.getString(resultData, AppParams.S_SIZE_NAME);
			
			String colorId = ParamUtil.getString(resultData, AppParams.S_COLOR_ID);
			String colorName = ParamUtil.getString(resultData, AppParams.S_COLOR_NAME);
			String colorValue = ParamUtil.getString(resultData, AppParams.S_COLOR);
			
			Map colorMap = new HashedMap<>();
			colorMap.put(AppParams.ID, colorId);
			colorMap.put(AppParams.NAME, colorName);
			colorMap.put(AppParams.VALUE, colorValue);
			
			if (CollectionUtils.isEmpty(formatList)) {
				
				List<Map> colorList = new ArrayList<Map>();
				colorList.add(colorMap);
				
				format.put(AppParams.SIZE_ID, sizeId);
				format.put(AppParams.SIZE_NAME, sizeName);
				format.put(AppParams.SIZE_ID, sizeId);
				format.put(AppParams.COLORS, colorList);
				
				formatList.add(format);
				LOGGER.info("formatList= " + formatList.toString());
				
			} else {
				LOGGER.info("sizeId= " + sizeId);
				Map exitsSizes = formatList.stream().filter(s -> ParamUtil.getString(s, AppParams.SIZE_ID).equalsIgnoreCase(sizeId)).findFirst().orElse(null);
				if (exitsSizes != null && exitsSizes.isEmpty() == false) {
					
					List<Map> exitsColorList = ParamUtil.getListData(exitsSizes, AppParams.COLORS);
					Set<Map> exitsColorSet = new HashSet<>(exitsColorList);
					exitsColorSet.add(colorMap);
					
					List<Map> colorList = new ArrayList<>(exitsColorSet);
					exitsSizes.replace(AppParams.COLORS, colorList);
					LOGGER.info("formatList= " + formatList.toString());
					
				} else {
					
					List<Map> colorList = new ArrayList<Map>();
					colorList.add(colorMap);
					
					format.put(AppParams.SIZE_ID, sizeId);
					format.put(AppParams.SIZE_NAME, sizeName);
					format.put(AppParams.SIZE_ID, sizeId);
					format.put(AppParams.COLORS, colorList);
					
					formatList.add(format);
					LOGGER.info("formatList= " + formatList.toString());
				}
			}
		}
		
		return formatList;
	}
	
	public static Map searchProductState(String storeName, String content, String status, int page, int pageSize) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeName);
		inputParams.put(2, content);
		inputParams.put(3, status);
		inputParams.put(4, page);
		inputParams.put(5, pageSize);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(6, OracleTypes.NUMBER);
		outputParamsTypes.put(7, OracleTypes.VARCHAR);
		outputParamsTypes.put(8, OracleTypes.CURSOR);
		outputParamsTypes.put(9, OracleTypes.NUMBER);
		outputParamsTypes.put(10, OracleTypes.NUMBER);
		outputParamsTypes.put(11, OracleTypes.NUMBER);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(6, AppParams.RESULT_CODE);
		outputParamsNames.put(7, AppParams.RESULT_MSG);
		outputParamsNames.put(8, AppParams.RESULT_DATA);
		outputParamsNames.put(9, AppParams.RESULT_TOTAL);
		outputParamsNames.put(10, "rs_approved");
		outputParamsNames.put(11, "rs_processing");
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFY_SEARCH_PRODUCT_STATE, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
		List<Map> resultDataList = ParamUtil.getListData(resultMap, AppParams.RESULT_DATA);
		
		int resultTotal = ParamUtil.getInt(resultMap, AppParams.RESULT_TOTAL);
		int resultApproved = ParamUtil.getInt(resultMap, "rs_approved");
		int resultProcessing = ParamUtil.getInt(resultMap, "rs_processing");
		
		List<Map> formatList = new ArrayList<Map>();
		for (Map resultDataItem : resultDataList) {
			Map formatMap = new LinkedHashMap<>();
			String productRefId = Long.toString((ParamUtil.getLong(resultDataItem, AppParams.S_PRODUCT_REF_ID)));
			formatMap.put(AppParams.PRODUCT_REF_ID, productRefId);
			formatMap.put(AppParams.STATE, ParamUtil.getString(resultDataItem, AppParams.S_STATE));
			formatList.add(formatMap);
		}
		
		resultMap = new LinkedHashMap<>();
		resultMap.put(AppParams.TOTAL, resultTotal);
		resultMap.put("rs_approved", resultApproved);
		resultMap.put("rs_processing", resultProcessing);
		resultMap.put(AppParams.DATA, formatList);
		
		return resultMap;
	}
	
	public static void insertReceiveWebhook(String storeId, String refId, String type, String jsonBody) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, storeId);
		inputParams.put(2, refId);
		inputParams.put(3, type);
		inputParams.put(4, jsonBody);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(5, OracleTypes.NUMBER);
		outputParamsTypes.put(6, OracleTypes.VARCHAR);
		
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(5, AppParams.RESULT_CODE);
		outputParamsNames.put(6, AppParams.RESULT_MSG);
		
		Map resultMap = DBProcedureUtil.execute(dataSource, SHOPIFT_INSERT_RECEIVE_WEBHOOK, inputParams,
				outputParamsTypes, outputParamsNames);
		int resultCode = ParamUtil.getInt(resultMap, AppParams.RESULT_CODE);
		LOGGER.info("resultCode= " + resultCode);
		LOGGER.info("result_msg= " + ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		if (resultCode != HttpResponseStatus.CREATED.code()) {
			throw new OracleException(ParamUtil.getString(resultMap, AppParams.RESULT_MSG));
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ShopifyAppService.class.getName());

}