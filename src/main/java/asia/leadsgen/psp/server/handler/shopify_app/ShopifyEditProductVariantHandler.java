package asia.leadsgen.psp.server.handler.shopify_app;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.obj.ShopifySyncedProductObj;
import asia.leadsgen.psp.server.handler.campaign_v2.CampaignModel;
import asia.leadsgen.psp.server.handler.campaign_v2.CampaignPayload;
import asia.leadsgen.psp.server.handler.campaign_v2.ColorModel;
import asia.leadsgen.psp.server.handler.campaign_v2.DesignCreateV2;
import asia.leadsgen.psp.server.handler.campaign_v2.DesignModel;
import asia.leadsgen.psp.server.handler.campaign_v2.ProductModel;
import asia.leadsgen.psp.server.handler.campaign_v2.SizeModel;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.service_fulfill.CampaignCreateServiceV2;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service.ProductDesignService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.BasePhoneCaseUtil;
import asia.leadsgen.psp.util.BaseSizePhoneCaseUtil;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyEditProductVariantHandler implements Handler<RoutingContext> {
	
	static final String ISP_PREIX_30USD = "https://isp.30usd.com/";
	static final String ISP_PREFIX_BG = "https://isp.burgerprints.com/";
	
	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		routingContext.vertx().executeBlocking(future -> {
			
			LOGGER.info("ShopifyEditProductVariantHandler");
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
			JsonObject jsonBody = routingContext.getBodyAsJson();
        	CampaignPayload campaignPayload = new CampaignPayload();
        	
        	try {
				campaignPayload = parsePostedPayload(jsonBody);
			}
			catch(NumberFormatException ex) {
				throw new BadRequestException(new SystemError("Invalid Payload", "Data is not well-formed", String.format("Expect numeric value but got %s", ex.getMessage()), ""));
			}
			catch(JsonSyntaxException ex ) {
				throw new BadRequestException(new SystemError("Invalid Payload", "Data is not well-formed", ex.getCause().getMessage(), ""));
			}
			catch(Exception ex) {
				throw new BadRequestException(new SystemError("Invalid Payload", "Data is not well-formed", ex.getCause().getMessage(), ""));
			}		
			
			Long variantRefId = Long.parseLong(routingContext.request().getParam(AppParams.ID));		
			
			CampaignModel campModel = campaignPayload.getCampaign();
			
			ProductModel productModel = campaignPayload.getProduct();
					
			String storeId = campaignPayload.getStoreId();
			LOGGER.info("storeId= " + storeId);
			if (StringUtils.isEmpty(storeId)) {
				throw new LoginException(SystemError.INVALID_DROPSHIP_STORE_ID);
			}
			
			Map storeMap = null;
			try {
				storeMap = DropShipStoreService.lookUp(storeId);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			String storeUserId = ParamUtil.getString(storeMap, AppParams.USER_ID);
			LOGGER.info("storeUserId= " + storeUserId);
			if (!storeUserId.equalsIgnoreCase(userId)) {
				throw new LoginException(SystemError.INVALID_USER);
			}
			
			try {
				
				String campaignId = campModel.getId();
				LOGGER.info("campaignId: " + campaignId);
				
				Long productRefId = Long.parseLong(productModel.getRef_id());
				String productId = productModel.getId();
				LOGGER.info("productId: " + productId);
				
				Map campaignInfo = CampaignService.getV2(campaignId);
				List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);
				
				String baseIdRequest = productModel.getBaseId();
				LOGGER.info("baseIdRequest: " + baseIdRequest);
				
				int countCreatedProduct = 0;
				
				List<String> designTypeList = new ArrayList<>();
				
				// Add new Variant
				for (Map campProduct : campaignProductList) {
					
					Map base = ParamUtil.getMapData(campProduct, AppParams.BASE);
					String baseIdDb = ParamUtil.getString(base, AppParams.ID);
					String designType = ParamUtil.getString(base, AppParams.DESIGN_TYPE);
					designTypeList.add(designType);

					LOGGER.info("baseIdDb: " + baseIdDb);
					if (baseIdDb.equalsIgnoreCase(baseIdRequest)) {
						productId = ParamUtil.getString(campProduct, AppParams.ID);
						LOGGER.info("Create new variant with productId: " + productId + " - baseId: " + baseIdDb);					
						ShopifyCreateVariantHelper.createNewVariant(campaignId, productId, campModel, productModel, userId);
						break;
					}
					
					countCreatedProduct++;
				}
				
				// Add new Product
				if (countCreatedProduct == campaignProductList.size()) {
					LOGGER.info("Create new product!");
					LOGGER.info("productModel= " + productModel.toString());
					productModel.setDefault(false);
					productId = createNewCampProduct(campaignId, productModel, storeMap, designTypeList, countCreatedProduct, userId,
							productRefId, routingContext, future);
				}
				
				Thread.sleep(1 * 5000);		
				/**
				 * Get Formated Variant to sync
				 */
				Map syncVariant = getVariantToSync(campaignId, productModel, variantRefId, productId);
				
				/**
				 * Update synced Variant to DB
				 */
				if (syncVariant != null && syncVariant.isEmpty() == false) {
					updateProductVariant(syncVariant);
				}
						
				Map productVariantMap = ShopifyAppService.lookup(productRefId);
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, productVariantMap);
				
				future.complete();
				
			} catch (Exception e) {
				routingContext.fail(e);
			}
			
		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
		
	}
	
	private CampaignPayload parsePostedPayload(JsonObject jsonBody) throws Exception {
    	Gson gson = new GsonBuilder().setDateFormat("yyyyMMdd'T'HH:mm:ss'Z'").create();
		CampaignPayload campaign = gson.fromJson(jsonBody.toString(), CampaignPayload.class);
		return campaign;
    }
	
	private String createNewCampProduct(String campaignId, ProductModel pModel, Map storeMap, List<String> designTypeList, int position, String userId, 
			Long productRefId, RoutingContext routingContext, Future<Object> future) 
			throws SQLException, ClassNotFoundException, ParseException, JsonProcessingException, UnsupportedEncodingException, UnirestException {
		
		List<SizeModel> sizeModels = pModel.getSizes();
		
		String currency = ParamUtil.getString(storeMap, AppParams.CURRENCY);
		
		String baseIdRequest = pModel.getBaseId();
		LOGGER.info("baseIdRequest: " + baseIdRequest);   
		
		Map baseMap = BaseService.get(baseIdRequest);
		String designType = ParamUtil.getString(baseMap, AppParams.DESIGN_TYPE);
		
		Map baseInfo = new LinkedHashMap<>();
		baseInfo.put(AppParams.ID, ParamUtil.getString(baseMap, AppParams.ID));
		baseInfo.put(AppParams.PRINTABLE, ParamUtil.getMapData(baseMap, AppParams.PRINTABLE));
		
		String productId = "";
		
		if (BasePhoneCaseUtil.isPhoneCase(baseIdRequest)) {
			
			Map<String, String> baseSizePhoneCaseMap = BaseSizePhoneCaseUtil.initMap();
			
			for (SizeModel sModel : sizeModels) {
		
				String sizeId = sModel.getId();
				String baseIdPhonecase = baseSizePhoneCaseMap.get(sizeId);
				
				if (baseIdPhonecase.equalsIgnoreCase(baseIdRequest)) {
					Map createProduct = CampaignCreateServiceV2.insertPhoneCaseProduct(campaignId, pModel, sModel, false, position, currency, baseIdPhonecase);
					LOGGER.info("createProduct: " + createProduct.toString());   
					
					productId = ParamUtil.getString(createProduct, AppParams.ID);
					List<DesignModel> designs = pModel.getDesigns();
					
					for (DesignModel dModel : designs) {  
						if (dModel.getType().equalsIgnoreCase("front")) {
							Map designInfoMap = DesignCreateV2.insert(campaignId, productId, dModel, baseInfo, ResourceStates.SHOPIFY_APP, true, 1);
							LOGGER.info("productId: " + productId + " ====> designMap: " + designInfoMap.toString()); 
						}							  
					}
				}							
			}
			
		} else {
			
			Map createProduct = CampaignCreateServiceV2.insertProduct(campaignId, pModel, position, currency);
			LOGGER.info("createProduct: " + createProduct.toString());   
			
			productId = ParamUtil.getString(createProduct, AppParams.ID);

			List<DesignModel> designs = pModel.getDesigns();
			LOGGER.info("designType: " + designType);
			LOGGER.info("designTypeList: " + designTypeList.toString());

			if (designTypeList.contains(designType)) {							
				
				ProductDesignService.updateProductDesigns(campaignId, productId);
											
			} else {
				
				for (DesignModel dModel : designs) {
					String url = dModel.getUrl();
					LOGGER.info("url: " + url);   
					if (!StringUtils.isEmpty(url)) {
						Map designInfoMap = DesignCreateV2.insert(campaignId, productId, dModel, baseInfo, ResourceStates.SHOPIFY_APP, true, designs.indexOf(dModel));
						LOGGER.info("productId: " + productId + " ====> designMap: " + designInfoMap.toString());
					} 
				}
			}						
		}
		
		ShopifyCreateVariantHelper.createNewCampaignProduct(campaignId, productId, userId, false);
		
		return productId;
	}
	
	/**
	 * Return Formated Variant Map to Sync
	 * @param campaignId
	 * @param product
	 * @param variantRefId
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 * @throws InterruptedException 
	 */
	private Map getVariantToSync(String campaignId, ProductModel productModel, Long variantRefId, String productId) throws SQLException, ParseException, InterruptedException {
		
		LOGGER.info("Get variant to sync...");
		LOGGER.info("productId= " + productId);
		
		List<ColorModel> colorModels = productModel.getColors();
		String launchingColorId = colorModels.get(0).getId();
		LOGGER.info("launchingColorId: " + launchingColorId);
		
		Map variantMap = checkExistVariant(campaignId, productId, launchingColorId);
		String variantId = ParamUtil.getString(variantMap, AppParams.ID);
		String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
		LOGGER.info("variantId: " + variantId);
		
		List<SizeModel> sizeModels = productModel.getSizes();
		String sizeId = sizeModels.get(0).getId();
		Map<String, String> baseSizeMap = BaseService.getBaseSizeMap();
		String sizeName = baseSizeMap.get(sizeId);
		
		Map syncedVariant = ShopifyAppService.getSyncedProductVariant(variantRefId);
		String syncedVariantSizeId = ParamUtil.getString(syncedVariant, AppParams.SIZE_ID);
		LOGGER.info("syncedVariantSizeId: " + syncedVariantSizeId);
		String syncedVariantColorId = ParamUtil.getString(syncedVariant, AppParams.COLOR_ID);
		LOGGER.info("syncedVariantColorId: " + syncedVariantColorId);
		
		Map syncVariantMap = new LinkedHashMap<>();
		if (syncedVariantColorId.equalsIgnoreCase(launchingColorId)
				&& syncedVariantSizeId.equalsIgnoreCase(sizeId)) {
			return syncVariantMap;
		}
		
		String sizeIds = ShopifyAppService.getSizeIdsFromSyncedVariant(variantId);
		LOGGER.info("sizeIds: " + sizeIds);
		
		if (sizeIds.contains(sizeId)) {		
			LOGGER.info("SizeId: " + sizeId + " is selected with ColorId: " + launchingColorId);
			String msg = "Size: " + sizeName + " is selected with Color: " + colorName;
			throw new BadRequestException(new SystemError("Invalid Variant", msg , "", "http://developer.30usd.com/errors/400.html"));		
		}

		Map campaignInfo = CampaignService.getV2(campaignId);
		
		List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);	
			
		Map campaignProduct = campaignProductList.stream().filter(m -> (m.get(AppParams.ID)).equals(productId)).findFirst().get();
		
		String designFrontUrl = "", designBackUrl = "";	
		List<Map> designList = ParamUtil.getListData(campaignProduct, AppParams.DESIGNS);
		if (designList != null && designList.isEmpty() == false) {
			for (Map design : designList) {
				Map productImage = ParamUtil.getMapData(design, AppParams.IMAGE);
				if ((ParamUtil.getString(design, AppParams.TYPE)).equalsIgnoreCase("front")) {
					designFrontUrl = ParamUtil.getString(productImage, AppParams.URL);
				}
				if ((ParamUtil.getString(design, AppParams.TYPE)).equalsIgnoreCase("back")) {
					designBackUrl = ParamUtil.getString(productImage, AppParams.URL);
				}
			}
		}
		
		syncVariantMap.put(AppParams.VARIANT_ID, variantId);
		syncVariantMap.put(AppParams.PRODUCT_ID, productId);
		syncVariantMap.put(AppParams.BASE_ID, ParamUtil.getString(variantMap, AppParams.BASE_ID));
		syncVariantMap.put(AppParams.COLOR_ID, ParamUtil.getString(variantMap, AppParams.COLOR_ID));
		syncVariantMap.put(AppParams.COLOR_NAME, colorName);
		syncVariantMap.put(AppParams.COLOR, ParamUtil.getString(variantMap, AppParams.COLOR));
		syncVariantMap.put(AppParams.SIZE_ID, sizeId);
		syncVariantMap.put(AppParams.SIZE_NAME, sizeName);
		
		Map img = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
		syncVariantMap.put(AppParams.MOCKUP_FRONT_URL, ParamUtil.getString(img, AppParams.FRONT));
		syncVariantMap.put(AppParams.MOCKUP_BACK_URL, ParamUtil.getString(img, AppParams.BACK));
		
		syncVariantMap.put(AppParams.DESIGN_FRONT_URL, designFrontUrl);
		syncVariantMap.put(AppParams.DESIGN_BACK_URL, designBackUrl);
		
		syncVariantMap.put(AppParams.VARIANT_REF_ID, variantRefId);
			
		return syncVariantMap;
	}
	
	private Map checkExistVariant(String campaignId, String productId, String launchingColorId) throws SQLException, ParseException, InterruptedException {
		
		Map variantMap = new HashMap<>();
		boolean checkExistVariant = false;	
		int count = 0;
		while(checkExistVariant == false) {		
			Map campaignInfo = CampaignService.getV2(campaignId);
			List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);
			Map campaignProduct = campaignProductList.stream().filter(m -> (m.get(AppParams.ID)).equals(productId)).findFirst().get();
			List<Map> variantMapList = ParamUtil.getListData(campaignProduct, AppParams.VARIANTS);
			
			Optional<Map> opt = variantMapList.stream().filter(m -> (m.get(AppParams.COLOR_ID)).equals(launchingColorId)).findFirst();
			if (opt.isPresent()) {
				variantMap = variantMapList.stream().filter(m -> (m.get(AppParams.COLOR_ID)).equals(launchingColorId)).findFirst().get();
				
				checkExistVariant = true;
			}
			LOGGER.info("checkExistVariant: " + checkExistVariant);
			count++;
			if (count == 10) {
				throw new BadRequestException(new SystemError("Invalid Variant", "Failed to create variant!" , "", "http://developer.30usd.com/errors/400.html"));
			}
			Thread.sleep(1 * 1000);
		}
		return variantMap;	
	}
	
	/**
	 * Update synced Variant to tb_shopify_synced_product_variant
	 * @param syncVariant
	 * @return
	 * @throws SQLException
	 */
	private void updateProductVariant(Map syncVariant) throws SQLException {
		
		String variantId = ParamUtil.getString(syncVariant, AppParams.VARIANT_ID);
		String productId = ParamUtil.getString(syncVariant, AppParams.PRODUCT_ID);
		String baseId = ParamUtil.getString(syncVariant, AppParams.BASE_ID);
		String colorId = ParamUtil.getString(syncVariant, AppParams.COLOR_ID);
		String sizeId = ParamUtil.getString(syncVariant, AppParams.SIZE_ID);
		String colorName = ParamUtil.getString(syncVariant, AppParams.COLOR_NAME);
		String sizeName = ParamUtil.getString(syncVariant, AppParams.SIZE_NAME);
		String colorValue = ParamUtil.getString(syncVariant, AppParams.COLOR);
		
		Long variantRefId = ParamUtil.getLong(syncVariant, AppParams.VARIANT_REF_ID);
							
		String mockup_front_url = ParamUtil.getString(syncVariant, AppParams.MOCKUP_FRONT_URL);
		if (mockup_front_url == null || mockup_front_url.isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_MOCKUP_IMAGE);
		}
		String mockup_back_url = ParamUtil.getString(syncVariant, AppParams.MOCKUP_BACK_URL);
		String design_front_url = ParamUtil.getString(syncVariant, AppParams.DESIGN_FRONT_URL);
		String design_back_url = ParamUtil.getString(syncVariant, AppParams.DESIGN_BACK_URL);	
		
		String sku = "";	
		sku = ShopifyAppService.checkSku(baseId, sizeId, colorName);
		
		ShopifySyncedProductObj syncedProductObj = new ShopifySyncedProductObj();
		syncedProductObj.setBgpVariantId(variantId);
		syncedProductObj.setBaseId(baseId);
		syncedProductObj.setSizeId(sizeId);
		syncedProductObj.setColorId(colorId);
		syncedProductObj.setFrontDesign(design_front_url);
		syncedProductObj.setFrontMockup(mockup_front_url);
		syncedProductObj.setBackDesign(design_back_url);
		syncedProductObj.setBackMockup(mockup_back_url);
		syncedProductObj.setBgpProductId(productId);
		syncedProductObj.setVariantRefId(variantRefId);
		syncedProductObj.setSku(sku);
		syncedProductObj.setColorName(colorName);
		syncedProductObj.setSizeName(sizeName);
		syncedProductObj.setColorValue(colorValue);
		
		ShopifyAppService.updateSyncedProductVariant(syncedProductObj);	
	}
	
	private static final SystemError GENERATE_MOCKUP_FAILED = new SystemError("GENERATE_MOCKUP_FAILED",
			"Generate mockup failed. Please try again later!", "", "http://developer.30usd.com/errors/400.html");
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyEditProductVariantHandler.class.getName());
}
