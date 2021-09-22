package asia.leadsgen.psp.service_fulfill;

import java.math.BigDecimal;
import java.sql.CallableStatement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.server.handler.campaign_v2.AllProductSizesDBType;
import asia.leadsgen.psp.server.handler.campaign_v2.CampaignDBType;
import asia.leadsgen.psp.server.handler.campaign_v2.CampaignModel;
import asia.leadsgen.psp.server.handler.campaign_v2.ColorModel;
import asia.leadsgen.psp.server.handler.campaign_v2.ProductDBType;
import asia.leadsgen.psp.server.handler.campaign_v2.ProductModel;
import asia.leadsgen.psp.server.handler.campaign_v2.SizeDBType;
import asia.leadsgen.psp.server.handler.campaign_v2.SizeModel;
import asia.leadsgen.psp.server.handler.campaign_v2.UpsellCampaignDBType;
import asia.leadsgen.psp.server.handler.campaign_v2.UpsellCampaignModel;
import asia.leadsgen.psp.server.handler.campaign_v2.VariantDBType;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.CampaignUtil;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.internal.OraclePreparedStatement;

public class CampaignCreateServiceV2 extends MasterService {
	private static DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public static final String CAMP_V2_INSERT_CAMP = "{call PKG_FF_CAMPAIGN_V2.insert_campaign(?,?,?,?,?,?,?,?)}";
	
	public static final String CAMP_V2_INSERT_PRODUCT = "{call PKG_FF_CAMPAIGN_V2.insert_product(?,?,?,?,?)}";
	
	public static final String CAMP_V2_INSERT_PRODUCT_SIZE = "{call PKG_FF_CAMPAIGN_V2.insert_product_size(?,?,?,?)}";
	
	public static final String CAMP_V2_INSERT_UPSELL_CAMP = "{call PKG_FF_CAMPAIGN_V2.insert_upsell_campaign(?,?)}";
	
	public static final String CAMP_V2_UPDATE_CAMP_INFO = "{call PKG_FF_CAMPAIGN_V2.update_campaign_info(?,?,?,?,?)}";
	
	public static final String CAMP_V2_INSERT_ALL_PRODUCT = "{call PKG_FF_CAMPAIGN_V2.insert_all_product(?,?)}";
	
	public static final String CAMP_V2_GET_PRODUCT_IDS_WITH_BASE_ID = "{call PKG_FF_CAMPAIGN_V2.camp_get_product_ids_with_base_id(?,?,?,?)}";
	
	public static final String CAMP_V2_INSERT_ALL_PRODUCT_SIZES = "{call PKG_FF_CAMPAIGN_V2.insert_all_product_sizes(?)}";
	
	public static final String CAMP_V2_INSERT_ALL_PRODUCT_VARIANTS = "{call PKG_FF_CAMPAIGN_V2.insert_all_product_variants(?)}";
	
	public static final String CAMP_V2_UPDATE_CAMP_DESIGN_VERSION = "{call PKG_FF_CAMPAIGN_V2.update_campaign_design_version(?,?,?,?)}";
	
	public static Map insertCampaign(CampaignModel campModel, String designVersion) throws ClassNotFoundException, ParseException, SQLException {
		
		Map resultMap = new LinkedHashMap<>();
		
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				
//				LOGGER.info("campModel: " + campModel.toString());
				
				CampaignDBType cObj = new CampaignDBType(campModel.getUserId(), campModel.getBaseGroupId(), campModel.getTitle(), campModel.getDescription(),
						campModel.getDomain(), campModel.getDomainId(), campModel.isPrivate(), campModel.getCategories(),
						campModel.getGgPixel(), campModel.getFbPixel(), campModel.getSeoDesc(), campModel.getSeoImageCover(), campModel.getSeoTitle(), campModel.getState());
				
				SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

				String startTime = dateFormat.format(new Date());

				int campaignLength = 1;

				String endTime = dateFormat.format(CampaignUtil.getCampaignEndDate(new Date(), campaignLength));

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);
				LocalDateTime formatDateTime = LocalDateTime.parse(startTime, formatter);

				if (dateFormat.parse(endTime).before(dateFormat.parse(startTime)) || formatDateTime.getHour() >= 22) {
					endTime = dateFormat.format(DateTimeUtil.addDays(dateFormat.parse(endTime), 1));
				}
				
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				
				Map map = con.getTypeMap();
				map.put("CAMPAIGN_TYPE", asia.leadsgen.psp.server.handler.campaign_v2.CampaignDBType.class);
				
				con.setTypeMap(map);
				
				try (CallableStatement cstmt = con.prepareCall(CAMP_V2_INSERT_CAMP);) {
					cstmt.setObject(1, cObj);
					cstmt.setInt(2, campaignLength);
					cstmt.setString(3, startTime);
					cstmt.setString(4, endTime);
					cstmt.setString(5, designVersion);
	
					cstmt.registerOutParameter(6, OracleTypes.NUMBER);
					cstmt.registerOutParameter(7, OracleTypes.VARCHAR);
					cstmt.registerOutParameter(8, OracleTypes.CURSOR);
					
					cstmt.execute();
	
					try (ResultSet resultSet = (ResultSet) cstmt.getObject(8);) {
					
						if (resultSet != null) {
							while(resultSet.next()) {
								resultMap.put(AppParams.CAMPAIGN_ID, resultSet.getString(1));
							}
						} else {
							throw new BadRequestException(SystemError.DATA_NOT_FOUND);
						}
						LOGGER.info("result: " + resultMap.toString());
					}
				}
			}
		}
		return resultMap;
	}

	public static Map insertProduct(String campaignId, ProductModel productModel, int position, String currency) throws SQLException, ClassNotFoundException {
		
		Map resultMap = new LinkedHashMap<>();
		
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				
				List<String> sizeIdList = new ArrayList<String>();
				List<Map> sizeList = new ArrayList<>();
				
				List<SizeModel> sizeModels = productModel.getSizes();
				for (SizeModel sModel : sizeModels) {
					
					String sizeId = sModel.getId();
					if (!sizeId.isEmpty()) {
						sizeIdList.add(sizeId);
					}
					
					Map sizeMap = new HashMap<>();
					sizeMap.put("id", sizeId);
					sizeMap.put("sale_price", sModel.getSale_price());
					
					sizeList.add(sizeMap);			
				}

				String sizes = "";
				if (sizeIdList != null && sizeIdList.isEmpty() == false) {
					sizes = String.join(",", sizeIdList);
				}
				
				List<String> colorIdList = new ArrayList<String>();
				List<ColorModel> colorModels = productModel.getColors();
				String defaultColorId = "";
				
				for (ColorModel clModel : colorModels) {
					
					String colorId = clModel.getId();
					boolean defaultColor = clModel.isDefault();
					
					if (!colorId.isEmpty()) {
						colorIdList.add(colorId);
					}
					
					if (defaultColor) {
						defaultColorId = colorId;
					}
				}
				String colors = "";
				if (colorIdList != null && colorIdList.isEmpty() == false) {
					colors = String.join(",", colorIdList);
				}
				
				ProductDBType pObj = new ProductDBType(productModel.getBaseId(), position, productModel.isBackView(), productModel.isDefault(), 
						sizes, colors, defaultColorId, productModel.getSale_expected(), ResourceStates.APPROVED);
				
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				
				Map map = con.getTypeMap();
				map.put("PRODUCT_CAMP_TYPE", asia.leadsgen.psp.server.handler.campaign_v2.ProductDBType.class);
				con.setTypeMap(map);
				
				try (CallableStatement cstmt = con.prepareCall(CAMP_V2_INSERT_PRODUCT);) {
					cstmt.setString(1, campaignId);
					cstmt.setObject(2, pObj);
					
					cstmt.registerOutParameter(3, OracleTypes.NUMBER);
					cstmt.registerOutParameter(4, OracleTypes.VARCHAR);
					cstmt.registerOutParameter(5, OracleTypes.CURSOR);
					
					cstmt.execute();
					
					try (ResultSet resultSet = (ResultSet) cstmt.getObject(5);) {
					
						if (resultSet != null) {
							while(resultSet.next()) {
								resultMap.put(AppParams.ID, resultSet.getString(1));
							}
						}
					}
				}
				
				String productId = ParamUtil.getString(resultMap, AppParams.ID);
				
				insertSizes(productModel.getSizes(), productId, productModel.getBaseId(), productModel.getSale_expected(), currency);
				
				resultMap.put(AppParams.SIZES, sizeList);
			}
		}
		
		return resultMap;
	}
	
	private static void insertSizes(List<SizeModel> sizesList, String productId, String baseId, int saleExpected, String currency) 
			throws SQLException, ClassNotFoundException {
		
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
							
				SizeDBType[] sizeT = new SizeDBType[sizesList.size()];
				
				for (int i = 0; i < sizesList.size(); i++) {				
					SizeModel sModel = sizesList.get(i);
					if (currency.isEmpty() || currency.equalsIgnoreCase("USD")) {
						SizeDBType obj = new SizeDBType(sModel.getId(), BigDecimal.valueOf((sModel.getSale_price())));
						sizeT[i] = obj;	
					} else {
						SizeDBType obj = new SizeDBType(sModel.getId(), null);
						sizeT[i] = obj;
					}							
				}
				
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				
				java.sql.Array array_to_pass = con.createOracleArray("PRODUCT_SIZE_T", sizeT);

				try (OraclePreparedStatement cstmt = (OraclePreparedStatement)con.prepareCall(CAMP_V2_INSERT_PRODUCT_SIZE);) {
					cstmt.setString(1, productId);
					cstmt.setString(2, baseId);
					cstmt.setInt(3, saleExpected);
					cstmt.setArray(4, array_to_pass);
	
					cstmt.execute();
				}
			}
		}
	}
	
	public static Map insertPhoneCaseProduct(String campaignId, ProductModel productModel, SizeModel sModel, boolean isDefault, 
			int position, String currency, String baseId) 
			throws SQLException, ClassNotFoundException {
		
		Map resultMap = new LinkedHashMap<>();
		
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				
				String sizeId = sModel.getId();
				List<Map> sizeList = new ArrayList<>();
				
				Map sizeMap = new HashMap<>();
				sizeMap.put("id", sizeId);
				sizeMap.put("sale_price", sModel.getSale_price());
				
				sizeList.add(sizeMap);	
				
				List<String> colorIdList = new ArrayList<String>();
				List<ColorModel> colorModels = productModel.getColors();
				String defaultColorId = "";
				
				for (ColorModel clModel : colorModels) {
					
					String colorId = clModel.getId();
					boolean defaultColor = clModel.isDefault();
					
					if (!colorId.isEmpty()) {
						colorIdList.add(colorId);
					}
					
					if (defaultColor) {
						defaultColorId = colorId;
					}
				}
				String colors = "";
				if (colorIdList != null && colorIdList.isEmpty() == false) {
					colors = String.join(",", colorIdList);
				}
				
				ProductDBType pObj = new ProductDBType(baseId, position, productModel.isBackView(), isDefault, 
						sizeId, colors, defaultColorId, productModel.getSale_expected(), ResourceStates.APPROVED);
				
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				
				Map map = con.getTypeMap();
				map.put("PRODUCT_CAMP_TYPE", asia.leadsgen.psp.server.handler.campaign_v2.ProductDBType.class);
				con.setTypeMap(map);
				
				try (CallableStatement cstmt = con.prepareCall(CAMP_V2_INSERT_PRODUCT);) {
					cstmt.setString(1, campaignId);
					cstmt.setObject(2, pObj);
					
					cstmt.registerOutParameter(3, OracleTypes.NUMBER);
					cstmt.registerOutParameter(4, OracleTypes.VARCHAR);
					cstmt.registerOutParameter(5, OracleTypes.CURSOR);
					
					cstmt.execute();
					
					try (ResultSet resultSet = (ResultSet) cstmt.getObject(5);) {
					
						if (resultSet != null) {
							while(resultSet.next()) {
								resultMap.put(AppParams.ID, resultSet.getString(1));
							}
						}
					}
				}
				
				String productId = ParamUtil.getString(resultMap, AppParams.ID);
				
				List<SizeModel> sizeModels = new ArrayList<SizeModel>();
				sizeModels.add(sModel);
				
				insertSizes(sizeModels, productId, baseId, productModel.getSale_expected(), currency);
				
				resultMap.put(AppParams.SIZES, sizeList);
			}			
		}
				
		return resultMap;		
	}
	
	public static void insertUpsellCampaign(String campaignId, List<UpsellCampaignModel> upsellCampaigns) throws SQLException, ClassNotFoundException {
		
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				
				UpsellCampaignDBType[] upsellCampT = new UpsellCampaignDBType[upsellCampaigns.size()];
				
				for (int i = 0; i < upsellCampaigns.size(); i++) {
					UpsellCampaignModel upsellCampModel = upsellCampaigns.get(i);
					UpsellCampaignDBType obj = new UpsellCampaignDBType(upsellCampModel.getUpsellCampaignId(), upsellCampModel.getUpsellCampaignName(),
							upsellCampModel.getType(), upsellCampModel.getUpsellVariantId(), upsellCampModel.getUpsellVariantName(), upsellCampModel.getUpsellVariantUrl(),
							upsellCampModel.getUpsellDiscountType(), upsellCampModel.getUpsellDiscountValue());
					upsellCampT[i] = obj;
				}
				
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				
				java.sql.Array array_to_pass = con.createOracleArray("UPSELL_CAMP_T", upsellCampT);
				
				try (OraclePreparedStatement cstmt = (OraclePreparedStatement)con.prepareCall(CAMP_V2_INSERT_UPSELL_CAMP);) {
					cstmt.setString(1, campaignId);
					cstmt.setArray(2, array_to_pass);
					cstmt.execute();
				}
			}
			
		}
	}
	
	public static void updateCampInfo(String campaignId, double salePrice, int isBackview) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, salePrice);
		inputParams.put(3, isBackview);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(4, OracleTypes.NUMBER);
		outputParamsTypes.put(5, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(4, AppParams.RESULT_CODE);
		outputParamsNames.put(5, AppParams.RESULT_MSG);
		
		Map updateResultMap = DBProcedureUtil.execute(dataSource, CAMP_V2_UPDATE_CAMP_INFO, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}
	}
	
	public static void insertAllProduct(String campaignId, List<ProductModel> productModels) throws SQLException {
		
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				
				ProductDBType[] productCampT = new ProductDBType[productModels.size()];
				
				for (int i = 0; i < productModels.size(); i++) {
					ProductModel pModel = productModels.get(i);
					ProductDBType obj = new ProductDBType(pModel.getBaseId(), pModel.getPosition(), pModel.isBackView(), pModel.isDefault(), pModel.getAllSizes(),
							pModel.getAllColors(), pModel.getDefaultColorId(), pModel.getSale_expected(), ResourceStates.APPROVED);
					productCampT[i] = obj;
					
				}
				
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				
				java.sql.Array array_to_pass = con.createOracleArray("PRODUCT_CAMP_T", productCampT);
				
				try (OraclePreparedStatement cstmt = (OraclePreparedStatement)con.prepareCall(CAMP_V2_INSERT_ALL_PRODUCT);) {
					cstmt.setString(1, campaignId);
					cstmt.setArray(2, array_to_pass);
					cstmt.execute();
				}

			}
		}
	}
	
	public static List<Map> getCampaignProductIdWithBaseId(String campaignId) throws SQLException {

		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);

		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(2, OracleTypes.NUMBER);
		outputParamsTypes.put(3, OracleTypes.VARCHAR);
		outputParamsTypes.put(4, OracleTypes.CURSOR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(2, AppParams.RESULT_CODE);
		outputParamsNames.put(3, AppParams.RESULT_MSG);
		outputParamsNames.put(4, AppParams.RESULT_DATA);

		Map campApprovedProductQueryResultMap = DBProcedureUtil.execute(dataSource,
				CAMP_V2_GET_PRODUCT_IDS_WITH_BASE_ID, inputParams, outputParamsTypes, outputParamsNames);

		int resultCode = ParamUtil.getInt(campApprovedProductQueryResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(campApprovedProductQueryResultMap, AppParams.RESULT_MSG));
		}

		List<Map> queryDataList = ParamUtil.getListData(campApprovedProductQueryResultMap, AppParams.RESULT_DATA);

		return queryDataList;
	}
	
	public static void insertAllSize(List<Map> allProductSizes, String currency) throws SQLException {
		
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				
				AllProductSizesDBType[] allProductSizesT = new AllProductSizesDBType[allProductSizes.size()];
				
				for (Map productSizes : allProductSizes) {
					int i = allProductSizes.indexOf(productSizes);
					if (currency.isEmpty() || currency.equalsIgnoreCase("USD")) {
						AllProductSizesDBType obj = new AllProductSizesDBType(
								ParamUtil.getString(productSizes, AppParams.PRODUCT_ID),
								ParamUtil.getString(productSizes, AppParams.BASE_ID),
								ParamUtil.getInt(productSizes, AppParams.SALE_EXPECTED),
								ParamUtil.getString(productSizes, AppParams.ID),
								BigDecimal.valueOf(ParamUtil.getDouble(productSizes, AppParams.SALE_PRICE)));
						allProductSizesT[i] = obj;
					} else {
						AllProductSizesDBType obj = new AllProductSizesDBType(
								ParamUtil.getString(productSizes, AppParams.PRODUCT_ID),
								ParamUtil.getString(productSizes, AppParams.BASE_ID),
								ParamUtil.getInt(productSizes, AppParams.SALE_EXPECTED),
								ParamUtil.getString(productSizes, AppParams.ID),
								null);
						allProductSizesT[i] = obj;
					}
				}
				
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				
				java.sql.Array array_to_pass = con.createOracleArray("ALL_PRODUCT_SIZES_T", allProductSizesT);
				
				try (OraclePreparedStatement cstmt = (OraclePreparedStatement)con.prepareCall(CAMP_V2_INSERT_ALL_PRODUCT_SIZES);) {
					cstmt.setArray(1, array_to_pass);
					cstmt.execute();
				}
			}
		}
	}
	
	public static void insertAllVariant(List<Map> allProductVariant) throws SQLException {
		
		try (Connection hikariCon = dataSource.getConnection()) {
			
			if (hikariCon.isWrapperFor(OracleConnection.class)) {
				
				VariantDBType[] productVariantsT = new VariantDBType[allProductVariant.size()];
				
				for (Map productVariant : allProductVariant) {
					int i = allProductVariant.indexOf(productVariant);
					VariantDBType obj = new VariantDBType(
							ParamUtil.getString(productVariant, AppParams.PRODUCT_ID),
							ParamUtil.getString(productVariant, AppParams.COLOR_ID),
							ParamUtil.getString(productVariant, AppParams.COLOR_VALUE),
							ParamUtil.getString(productVariant, AppParams.URLFRONT),
							ParamUtil.getString(productVariant, AppParams.URLBACK),
							ParamUtil.getString(productVariant, AppParams.FRONT_DESIGN_ID),
							ParamUtil.getString(productVariant, AppParams.BACK_DESIGN_ID),
							ParamUtil.getString(productVariant, AppParams.VARIANT_NAME),
							ParamUtil.getString(productVariant, AppParams.BASE_ID),
							ParamUtil.getBoolean(productVariant, AppParams.N_DEFAULT),
							ParamUtil.getInt(productVariant, AppParams.N_ORDER));
					productVariantsT[i] = obj;
				}
				
				OracleConnection con = hikariCon.unwrap(OracleConnection.class);
				
				java.sql.Array array_to_pass = con.createOracleArray("PRODUCT_VARIANTS_T", productVariantsT);
				
				try (OraclePreparedStatement cstmt = (OraclePreparedStatement)con.prepareCall(CAMP_V2_INSERT_ALL_PRODUCT_VARIANTS);) {
					cstmt.setArray(1, array_to_pass);
					cstmt.execute();
				}
			}
		}
	}
	
	public static void updateCampDesignVersion(String campaignId, String version) throws SQLException {
		
		Map inputParams = new LinkedHashMap<Integer, String>();
		inputParams.put(1, campaignId);
		inputParams.put(2, version);
		
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		outputParamsTypes.put(3, OracleTypes.NUMBER);
		outputParamsTypes.put(4, OracleTypes.VARCHAR);

		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		outputParamsNames.put(3, AppParams.RESULT_CODE);
		outputParamsNames.put(4, AppParams.RESULT_MSG);
		
		Map updateResultMap = DBProcedureUtil.execute(dataSource, CAMP_V2_UPDATE_CAMP_DESIGN_VERSION, inputParams,
				outputParamsTypes, outputParamsNames);
		
		int resultCode = ParamUtil.getInt(updateResultMap, AppParams.RESULT_CODE);

		if (resultCode != HttpResponseStatus.OK.code()) {
			throw new OracleException(ParamUtil.getString(updateResultMap, AppParams.RESULT_MSG));
		}
	}

	private static final Logger LOGGER = Logger.getLogger(CampaignCreateServiceV2.class.getName());

}
