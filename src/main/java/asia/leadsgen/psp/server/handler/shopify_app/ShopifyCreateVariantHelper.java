package asia.leadsgen.psp.server.handler.shopify_app;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.SystemException;
import asia.leadsgen.psp.server.handler.campaign_v2.CampaignModel;
import asia.leadsgen.psp.server.handler.campaign_v2.ColorModel;
import asia.leadsgen.psp.server.handler.campaign_v2.ProductModel;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.Base2dClassificationUtil;
import asia.leadsgen.psp.util.JSONStringToMapUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ShopifyCreateVariantHelper {
	
	private static String ispPrefixTshirt;
	private static String ispPrefixOtherBase;

	public static void setIspPrefixTshirt(String ispPrefixTshirt) {
		ShopifyCreateVariantHelper.ispPrefixTshirt = ispPrefixTshirt;
	}

	public static void setIspPrefixOtherBase(String ispPrefixOtherBase) {
		ShopifyCreateVariantHelper.ispPrefixOtherBase = ispPrefixOtherBase;
	}

	public static void createNewVariant(String campaignId, String productId, CampaignModel campModel, ProductModel productModel, String userId) 
			throws SQLException, ParseException, JsonProcessingException, UnsupportedEncodingException, UnirestException {
		
		List<ColorModel> colorModels = productModel.getColors();
		ObjectMapper mapper = new ObjectMapper();
		List<Map> requestColorList = colorModels.stream().map(o -> mapper.convertValue(o, Map.class)).collect(Collectors.toList());
		List<String> requestColorIds = requestColorList.stream().map(o -> ParamUtil.getString(o, AppParams.ID)).collect(Collectors.toList());
		LOGGER.info("requestColorIds: " + requestColorIds.toString());
		
		String dbColors = ShopifyAppService.getProductColors(productId);
		List<String> dbColorIdList = Arrays.asList(dbColors.split(","));
		LOGGER.info("dbColorIdList: " + dbColorIdList.toString());
		dbColorIdList = new ArrayList<>(dbColorIdList);

		List<String> launchingColorIds = new ArrayList<>();	
		for (String colorId : requestColorIds) {																							
			if (!dbColors.contains(colorId)) {									
				launchingColorIds.add(colorId);
				dbColorIdList.add(colorId);
			}									
		}
		
		HashMap<String, List<String>> productIdColorIds = new HashMap<String, List<String>>();
		if (CollectionUtils.isNotEmpty(launchingColorIds)) {
			
			Map updateColor = ShopifyAppService.updateColors(productId, String.join(",", dbColorIdList), "");
			String colorIds = ParamUtil.getString(updateColor, AppParams.S_COLORS);
			List<String> colorIdList = Arrays.asList(colorIds.split(","));
			productIdColorIds.put(productId, colorIdList);
			ISPHelper ispHelper = new ISPHelper(campaignId, productIdColorIds);
			
			Map campaignInfo = CampaignService.getV2(campaignId);
			
			List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);
			Map campaignProduct = campaignProductList.stream().filter(m -> (m.get(AppParams.ID)).equals(productId))
					.findFirst().get();
			List<Map> productColorList = ParamUtil.getListData(campaignProduct, AppParams.COLORS);
			boolean productDefault = ParamUtil.getBoolean(campaignProduct, AppParams.DEFAULT);
			
			List<Map> launchingColorList = productColorList.stream().filter(o -> launchingColorIds.contains(ParamUtil.getString(o, AppParams.ID)))
					.collect(Collectors.toList());
			LOGGER.info("launchingColorList: " + launchingColorList.toString());
			
			List<Map> productDesignList = ParamUtil.getListData(campaignProduct, AppParams.DESIGNS);
			Map baseMap = ParamUtil.getMapData(campaignProduct, AppParams.BASE);
			
			List<Map> ispRequestList = new ArrayList<>();
			Map ispProduct = new LinkedHashMap<>();
			ispProduct.put(AppParams.ID, productId);
			ispProduct.put(AppParams.BASE, baseMap);
			ispProduct.put(AppParams.DESIGNS, productDesignList);
			ispProduct.put(AppParams.DEFAULT, productDefault);
			ispProduct.put(AppParams.COLORS, launchingColorList);
			
			ispRequestList.add(ispProduct);
			LOGGER.info("ispRequestList: " + ispRequestList.toString());
			formatIspRequestVariantMap(ispRequestList, baseMap, campaignProductList, userId, campaignId, ispHelper, productId, dbColors);
		}
	}
	
	private static void formatIspRequestVariantMap(List<Map> ispRequestList, Map baseMap, List<Map> campaignProductList,
			String userId, String campaignId, ISPHelper ispHelper, String productId, String dbColors)
			throws JsonProcessingException, UnsupportedEncodingException, UnirestException, ParseException, SQLException {
		
		List<Map> shirtMainDesign = new ArrayList<>();
		List<Map> shirtMainBase = new ArrayList<>();
		List<Map> phoneCaseMainDesign = new ArrayList<>();
		List<Map> phoneCaseMainBase = new ArrayList<>();
		
		String baseId = ParamUtil.getString(baseMap, AppParams.ID);
		String designGroup = ParamUtil.getString(baseMap, AppParams.DESIGN_GROUP);
		String designType = ParamUtil.getString(baseMap, AppParams.DESIGN_TYPE);
		String group = Base2dClassificationUtil.classify(baseId);
		
		if (designGroup.equalsIgnoreCase("shirt")) {
			findShirtMainDesign(campaignProductList, designGroup, shirtMainDesign);
			
			if (!CollectionUtils.isEmpty(shirtMainDesign)) {
				generateVariantsMockUpWithBaseInfo(userId, campaignId, ispRequestList, ispPrefixTshirt + "2d-tshirt", shirtMainDesign, shirtMainBase, ispHelper, productId, dbColors);
			}
		}
		
		if (designType.equalsIgnoreCase("phonecase")) {
			findPhoneMainDesignAndBase(campaignProductList, designType, phoneCaseMainDesign, phoneCaseMainBase);
			
			if (!CollectionUtils.isEmpty(phoneCaseMainDesign) && !CollectionUtils.isEmpty(phoneCaseMainBase)) {
				generateVariantsMockUpWithBaseInfo(userId, campaignId, ispRequestList, ispPrefixOtherBase + "phonecase", phoneCaseMainDesign, phoneCaseMainBase, ispHelper, productId, dbColors);
			}
		}
		
		if (group.equalsIgnoreCase("mugs") || group.equalsIgnoreCase("poster") || group.equalsIgnoreCase("canvas")) {
			generateVariantsMockUpNotBaseInfo(userId, campaignId, ispRequestList, ispPrefixOtherBase + group, ispHelper, productId, dbColors);
		}
	}
	
	private static void findShirtMainDesign(List<Map> campaignProductList, String designShirt, List<Map> shirtMainDesign) {
		
		for (Map product : campaignProductList) {
			String productName = ParamUtil.getString(product, AppParams.PRODUCT_NAME);
			LOGGER.info("product name: " + productName);
			Map<String, Object> base = ParamUtil.getMapData(product, AppParams.BASE);
			String designGroup = ParamUtil.getString(base, AppParams.DESIGN_GROUP);
			boolean isMainDesign = false;
			if (designGroup.equalsIgnoreCase(designShirt)) {
				List<Map> designs = ParamUtil.getListData(product, AppParams.DESIGNS);
				for (Map designMap : designs) {
					isMainDesign = ParamUtil.getBoolean(designMap, AppParams.MAIN);
					if (isMainDesign) {
						designMap.put(AppParams.BASE, base);
						shirtMainDesign.add(designMap);
					}
				}
			}
			if (isMainDesign) {
				break;
			}
		}
	}
	
	private static void findPhoneMainDesignAndBase(List<Map> campaignProductList, String designPhone,
			List<Map> phoneCaseMainDesign, List<Map> phoneCaseMainBase) {
		
		for (Map product : campaignProductList) {
			
			Map<String, Object> base = ParamUtil.getMapData(product, AppParams.BASE);
			String designType = ParamUtil.getString(base, AppParams.DESIGN_TYPE);
			boolean isMainDesign = false;
			if (designType.equalsIgnoreCase(designPhone)) {
				List<Map> designs = ParamUtil.getListData(product, AppParams.DESIGNS);
				for (Map designMap : designs) {
					isMainDesign = ParamUtil.getBoolean(designMap, AppParams.MAIN);
					if (isMainDesign) {
						phoneCaseMainDesign.add(designMap);
						phoneCaseMainBase.add(base);
					}
				}
			}
			if (isMainDesign) {
				break;
			}
		}
	}
	
	private static void generateVariantsMockUpWithBaseInfo(String userId, String campaignId, List<Map> products,
			String ispUri, List<Map> designs, @Nullable List<Map> bases, ISPHelper ispHelper, String productId, String dbColors)
			throws JsonProcessingException, UnirestException, ParseException, UnsupportedEncodingException, SQLException {

		Map ispRequestBodyMap = new LinkedHashMap();
		ispRequestBodyMap.put(AppParams.USER_ID, userId);
		ispRequestBodyMap.put(AppParams.CAMPAIGN_ID, campaignId);
		ispRequestBodyMap.put(AppParams.PRODUCTS, products);
		ispRequestBodyMap.put(AppParams.DESIGNS, designs);
		ispRequestBodyMap.put(AppParams.BASES, bases);
		ObjectMapper mapper = new ObjectMapper();
		String jsonBody = mapper.writeValueAsString(ispRequestBodyMap);
		LOGGER.info("shopify-app create variant | gen mockup with base info | isp response = " + jsonBody);

		HttpResponse<JsonNode> response = Unirest.post(ispUri).body(jsonBody).asJson();

		ispResponseHandler(ispUri, campaignId, response, ispHelper, productId, dbColors);
	}
	
	private static void generateVariantsMockUpNotBaseInfo(String userId, String campaignId, List<Map> ispRequestList,
			String group, ISPHelper ispHelper, String productId, String dbColors) 
			throws UnirestException, JsonProcessingException, UnsupportedEncodingException, SQLException, ParseException {

		Map ispRequestBodyMap = new LinkedHashMap();
		ispRequestBodyMap.put(AppParams.USER_ID, userId);
		ispRequestBodyMap.put(AppParams.CAMPAIGN_ID, campaignId);
		ispRequestBodyMap.put(AppParams.PRODUCTS, ispRequestList);
		ObjectMapper mapper = new ObjectMapper();
		String jsonBody = mapper.writeValueAsString(ispRequestBodyMap);
		LOGGER.info("shopify-app create variant | gen mockup not base info | isp response = " + jsonBody);

		HttpResponse<JsonNode> response = Unirest.post(group).body(jsonBody).asJson();

		ispResponseHandler(group, campaignId, response, ispHelper, productId, dbColors);
	}
	
	private static void ispResponseHandler(String ispUri, String campaignId, HttpResponse<JsonNode> response, ISPHelper ispHelper, String productId, String dbColors) 
			throws UnsupportedEncodingException, SQLException, ParseException {
		
		int responseCode = response.getStatus();
		LOGGER.info(ispUri + " response code : " + responseCode);
		if (responseCode != HttpResponseStatus.CREATED.code()) {
//			CampaignService.updateState(campaignId, ResourceStates.DRAFT);
			if (StringUtils.isNotEmpty(productId) && StringUtils.isNotEmpty(dbColors)) {
				ShopifyAppService.updateColors(productId, dbColors, "");
			}
			throw new SystemException(GENERATE_MOCKUP_FAILED);
		}
		
		Map<String, Object> map = JSONStringToMapUtil.toMap(response.getBody().getObject());
		LOGGER.info(ispUri + " response body : " + map.toString());
		List<Map> productList = ParamUtil.getListData(map, AppParams.PRODUCTS);
		
		String productIdNew, baseId, baseName, frontDesignId = "", backDesignId = "";
		Map productBaseInfoMap;
		List<Map> ispVariants, productDesignList;
		List<String> productColorIds;
		
		LOGGER.info("ISPHelper: " + ispHelper.campaignId + " - " + ispHelper.productIdColorIds);
		for (Map product : productList) {
			
			productIdNew = ParamUtil.getString(product, AppParams.ID);
			ispVariants = ParamUtil.getListData(product, AppParams.VARIANTS);
			productColorIds = ispHelper.productIdColorIds.get(productIdNew);
			LOGGER.info("productColorIds: " + productColorIds.toString());
			
			productBaseInfoMap = ParamUtil.getMapData(product, AppParams.BASE);
			baseId = ParamUtil.getString(productBaseInfoMap, AppParams.ID);
			baseName = ParamUtil.getString(productBaseInfoMap, AppParams.NAME);

			productDesignList = ParamUtil.getListData(product, AppParams.DESIGNS);
			
			if (!CollectionUtils.isEmpty(productDesignList)) {

				String designId, designType;

				for (Map productDesign : productDesignList) {
					designId = ParamUtil.getString(productDesign, AppParams.ID);
					designType = ParamUtil.getString(productDesign, AppParams.TYPE);
					if (designType.equals(AppConstants.DESIGN_TYPE_FRONT)) {
						frontDesignId = designId;
					} else if (designType.equals(AppConstants.DESIGN_TYPE_BACK)) {
						backDesignId = designId;
					}
				}
			}
			
			if (CollectionUtils.isEmpty(ispVariants) == false) {
				insertNewProductVariants(campaignId, productIdNew, ispVariants, baseId, baseName, frontDesignId, backDesignId, productColorIds);
			}
		}
		
		CampaignService.updateDefaultImages(campaignId);
	}
	
	private static void insertNewProductVariants(String campaignId, String productId, List<Map> requestVariants,
			String baseId, String baseName, String frontDesignId, String backDesignId, List<String> productColorIds) throws SQLException {
		
    	Map variantColor;
		String colorId, colorValue, colorName, variantName, frontImageUrl, backImageUrl;
		boolean defaultVariant;
		int nOrder;

		for (Map productVariant : requestVariants) {
			variantColor = ParamUtil.getMapData(productVariant, AppParams.COLOR);
			colorId = ParamUtil.getString(variantColor, AppParams.ID);
			colorValue = ParamUtil.getString(variantColor, AppParams.VALUE);
			colorName = ParamUtil.getString(variantColor, AppParams.NAME);
			variantName = baseName + " - " + colorName;
			frontImageUrl = ParamUtil.getString(productVariant, AppParams.URLFRONT);
			backImageUrl = ParamUtil.getString(productVariant, AppParams.URLBACK);
			defaultVariant = ParamUtil.getBoolean(variantColor, AppParams.DEFAULT);
			nOrder = productColorIds.indexOf(colorId);
			
			ProductVariantService.insert(variantName, productId, baseId, colorId, colorValue, frontDesignId,
					backDesignId, frontImageUrl, backImageUrl, defaultVariant, nOrder);
		}	
	}
	
	public static void createNewCampaignProduct(String campaignId, String productId, String userId, boolean defaultProduct) 
			throws SQLException, ParseException, JsonProcessingException, UnsupportedEncodingException, UnirestException {
		
		Map campaignInfo = CampaignService.getV2(campaignId);
		
		List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);
		Map campaignProduct = campaignProductList.stream().filter(m -> (m.get(AppParams.ID)).equals(productId))
				.findFirst().get();
		
		List<Map> productDesignList = ParamUtil.getListData(campaignProduct, AppParams.DESIGNS);
		List<Map> productColorList = ParamUtil.getListData(campaignProduct, AppParams.COLORS);
		List<String> colorIdList = productColorList.stream().map(o -> ParamUtil.getString(o, AppParams.ID)).collect(Collectors.toList());
		HashMap<String, List<String>> productIdColorIds = new HashMap<String, List<String>>();
		productIdColorIds.put(productId, colorIdList);
		ISPHelper ispHelper = new ISPHelper(campaignId, productIdColorIds);
		LOGGER.info("ISPHelper: " + ispHelper.campaignId + " - " + ispHelper.productIdColorIds);
		
		
		Map baseMap = ParamUtil.getMapData(campaignProduct, AppParams.BASE);
		
		List<Map> ispRequestList = new ArrayList<>();
		Map ispProduct = new LinkedHashMap<>();
		ispProduct.put(AppParams.ID, productId);
		ispProduct.put(AppParams.BASE, baseMap);
		ispProduct.put(AppParams.DESIGNS, productDesignList);
		ispProduct.put(AppParams.DEFAULT, defaultProduct);
		ispProduct.put(AppParams.COLORS, productColorList);
		
		ispRequestList.add(ispProduct);
		LOGGER.info("ispRequestList: " + ispRequestList.toString());
		
		formatIspRequestVariantMap(ispRequestList, baseMap, campaignProductList, userId, campaignId, ispHelper, "", "");
	}
	
	private static final SystemError GENERATE_MOCKUP_FAILED = new SystemError("GENERATE_MOCKUP_FAILED",
			"Generate mockup failed. Please try again later!", "", "http://developer.30usd.com/errors/400.html");
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyCreateVariantHelper.class.getName());
	
}

class ISPHelper {
	String campaignId;
	HashMap<String, List<String>> productIdColorIds;
	
	ISPHelper(String campaignId, HashMap<String, List<String>> productIdColorIds){
		this.campaignId = campaignId;
		this.productIdColorIds = productIdColorIds;
	}
}
