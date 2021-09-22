package asia.leadsgen.psp.server.handler.shopify_app;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
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

public class ShopifySyncProductVariantHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			
			LOGGER.info("ShopifySyncProductVariantHandler");
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			LOGGER.info("userId= " + userId);
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
								
				String productId = "";
				LOGGER.info("campaignId: " + campaignId);
				
				Long productRefId = Long.parseLong(productModel.getRef_id());
				
				Map productVariantMap = ShopifyAppService.lookup(productRefId);
				
				LOGGER.info("productVariantMap: " + productVariantMap.toString());
				
				String baseGroupId = ParamUtil.getString(productVariantMap, AppParams.BASE_GROUP_ID);
				
				String title = ParamUtil.getString(productVariantMap, AppParams.TITLE);
				
				String desc = ParamUtil.getString(productVariantMap, AppParams.DESCRIPTION);
				
				String currency = ParamUtil.getString(storeMap, AppParams.CURRENCY);
						
				if (title != null && title.isEmpty() == false) {
					campModel.setTitle(title);
				} else {
					campModel.setTitle(null);
				}
				
				if (desc != null && desc.isEmpty() == false) {
					campModel.setDescription(desc);
				} else {
					campModel.setDescription(null);
				}
				
				Map createCamp = new HashMap<>();
				
				if (campaignId == null || campaignId.isEmpty()) {
					campModel.setState(ResourceStates.SHOPIFY_APP);
					campModel.setUserId(userId);
					
					createCamp = createNewCampaign(campModel, productModel, routingContext, future, currency, userId);
					campaignId = ParamUtil.getString(createCamp, AppParams.CAMPAIGN_ID);
					productId = ParamUtil.getString(createCamp, AppParams.PRODUCT_ID);
					
					ShopifyAppService.updateShopifyProduct(productRefId, campaignId, "");
					
				} else {
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
							LOGGER.info("Create new variant with product: " + productId + " - base: " + baseIdDb);					
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
				}
				
				Thread.sleep(1 * 2000);
				/**
				 *  Insert synced Variant to DB
				 */
				syncProductVariant(campaignId, productId, productModel, variantRefId, productRefId, currency);
				ShopifyAppService.updateNSyncedProductVariant(productRefId, variantRefId);
				
				productVariantMap = ShopifyAppService.lookup(productRefId);
				
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
	
	private Map createNewCampaign(CampaignModel campModel, ProductModel productModel, RoutingContext routingContext, Future<Object> future, String currency, String userId) 
			throws ClassNotFoundException, SQLException, UnsupportedEncodingException, ParseException, InterruptedException, JsonProcessingException, UnirestException {
		
		Map createCampaign = CampaignCreateServiceV2.insertCampaign(campModel, "7.0");
		LOGGER.info("createCampaign: " + createCampaign.toString());   
		
		String campaignId = ParamUtil.getString(createCampaign, AppParams.CAMPAIGN_ID);	
		String productId = "";
		
		productModel.setBackView(false);
		productModel.setDefault(true);
		productModel.setSale_expected(2);
		
		List<Map> productSizeList = new ArrayList<>();
									
		String productBaseId = productModel.getBaseId();
		if (BasePhoneCaseUtil.isPhoneCase(productBaseId)) {
			
			boolean isDefault = true;
			List<SizeModel> sizeModels = productModel.getSizes();
			SizeModel sModel = sizeModels.get(0);													

				Map createProduct = CampaignCreateServiceV2.insertPhoneCaseProduct(campaignId, productModel, sModel, isDefault, 1, currency, productBaseId);
				LOGGER.info("createProduct: " + createProduct.toString());   
				
				productId = ParamUtil.getString(createProduct, AppParams.ID);
						
				List<DesignModel> designs = productModel.getDesigns();
				
				for (DesignModel dModel : designs) {  
					if (dModel.getType().equalsIgnoreCase("front")) {
						Map designInfoMap = DesignCreateV2.insert(campaignId, productId, dModel, Collections.EMPTY_MAP, ResourceStates.SHOPIFY_APP, true, 1);
						LOGGER.info("productId: " + productId + " ====> designMap: " + designInfoMap.toString()); 
					}							  
				}
				
				productSizeList.add(createProduct);
					
		} else {
			
			Map createProduct = CampaignCreateServiceV2.insertProduct(campaignId, productModel, 1, currency);
			LOGGER.info("createProduct: " + createProduct.toString());   
			
			productId = ParamUtil.getString(createProduct, AppParams.ID);
					
			List<DesignModel> designs = productModel.getDesigns();
			
			for (DesignModel dModel : designs) {
				String url = dModel.getUrl();
				LOGGER.info("url: " + url);   
				if (!StringUtils.isEmpty(url)) {
					Map designInfoMap = DesignCreateV2.insert(campaignId, productId, dModel, Collections.EMPTY_MAP, ResourceStates.SHOPIFY_APP, productModel.isDefault(), designs.indexOf(dModel));
					LOGGER.info("productId: " + productId + " ====> designMap: " + designInfoMap.toString());
				}		   
			}
			
			productSizeList.add(createProduct);
		}					
		
		ShopifyCreateVariantHelper.createNewCampaignProduct(campaignId, productId, userId, true);
		
		Map result = new HashMap<>();
		result.put(AppParams.CAMPAIGN_ID, campaignId);
		result.put(AppParams.PRODUCT_ID, productId);
		return result;		
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
	
	private void syncProductVariant(String campaignId, String productId, ProductModel productModel, Long variantRefId, Long productRefId, String currency)
			throws SQLException, ParseException, InterruptedException {
		
		Map campaignInfo = CampaignService.getV2(campaignId);
		List<SizeModel> sizeModels = productModel.getSizes();
		List<ColorModel> colorModels = productModel.getColors();
		
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
		
		ColorModel clModel = colorModels.get(0);
		String colorId = clModel.getId();
		
		SizeModel sModel = sizeModels.get(0);
		Map<String, String> baseSizeMap = BaseService.getBaseSizeMap();
		String sizeId = sModel.getId();
		String sizeName = baseSizeMap.get(sizeId);
				
		Map variantMap = checkExistVariant(campaignId, productId, colorId);		
		String variantId = ParamUtil.getString(variantMap, AppParams.ID);
		String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
		LOGGER.info("variantId: " + variantId);
		
		String sizeIds = ShopifyAppService.getSizeIdsFromSyncedVariant(variantId);
		LOGGER.info("sizeIds: " + sizeIds);
		
		if (sizeIds.contains(sizeId)) {		
			LOGGER.info("SizeId: " + sizeId + " is selected with ColorId: " + colorId);
			String msg = "Size: " + sizeName + " is selected with Color: " + colorName;
			throw new BadRequestException(new SystemError("Invalid Variant", msg , "", "http://developer.30usd.com/errors/400.html"));		
		}
		
		Map shopifyProduct = ShopifyAppService.getVariant(productRefId);
		List<Map> shopifyVariantList = ParamUtil.getListData(shopifyProduct, AppParams.VARIANTS);
		Map shopifyVariantMap = shopifyVariantList.stream().filter(m -> (m.get(AppParams.REFERENCE_ID)).equals(variantRefId)).findFirst().get();
			
		String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
		String colorValue = ParamUtil.getString(variantMap, AppParams.COLOR);

		String skuRef = ParamUtil.getString(shopifyVariantMap, AppParams.SKU_REF); 
		String salePrice = ParamUtil.getString(shopifyVariantMap, AppParams.RETAIL_PRICE);
		int saleExpected = Integer.parseInt(ParamUtil.getString(campaignProduct, AppParams.SALE_EXPECTED));
		
		Map img = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
		String mockup_front_url = ParamUtil.getString(img, AppParams.FRONT);
		String mockup_back_url = ParamUtil.getString(img, AppParams.BACK);
	
		Long imageId = ParamUtil.getLong(shopifyVariantMap, AppParams.IMAGE_ID);
		
		String sku = "";	
		sku = ShopifyAppService.checkSku(baseId, sizeId, colorName);
		
		ShopifySyncedProductObj syncedProductObj = new ShopifySyncedProductObj();
		syncedProductObj.setBgpVariantId(variantId);
		syncedProductObj.setBaseId(baseId);
		syncedProductObj.setSizeId(sizeId);
		syncedProductObj.setColorId(colorId);
		syncedProductObj.setImageId(imageId);
		syncedProductObj.setFrontDesign(designFrontUrl);
		syncedProductObj.setFrontMockup(mockup_front_url);
		syncedProductObj.setBackDesign(designBackUrl);
		syncedProductObj.setBackMockup(mockup_back_url);
		syncedProductObj.setBgpProductId(productId);
		syncedProductObj.setSalePrice(salePrice);
		syncedProductObj.setCurrency(currency);
		syncedProductObj.setSaleExpected(saleExpected);
		syncedProductObj.setProductRefId(productRefId);
		syncedProductObj.setVariantRefId(variantRefId);
		syncedProductObj.setSku(sku);
		syncedProductObj.setColorName(colorName);
		syncedProductObj.setSizeName(sizeName);
		syncedProductObj.setColorValue(colorValue);
		syncedProductObj.setSkuRef(skuRef);
		
		ShopifyAppService.syncProductVariant(syncedProductObj);		
	}
	
	private Map checkExistVariant(String campaignId, String productId, String colorId) throws SQLException, ParseException, InterruptedException {
		
		Map variantMap = new HashMap<>();
		boolean checkExistVariant = false;	
		int count = 0;
		while(checkExistVariant == false) {		
			Map campaignInfo = CampaignService.getV2(campaignId);
			List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);
			Map campaignProduct = campaignProductList.stream().filter(m -> (m.get(AppParams.ID)).equals(productId)).findFirst().get();
			List<Map> variantMapList = ParamUtil.getListData(campaignProduct, AppParams.VARIANTS);
			
			Optional<Map> opt = variantMapList.stream().filter(m -> (m.get(AppParams.COLOR_ID)).equals(colorId)).findFirst();
			if (opt.isPresent()) {
				variantMap = variantMapList.stream().filter(m -> (m.get(AppParams.COLOR_ID)).equals(colorId)).findFirst().get();
				
				checkExistVariant = true;
			}
			LOGGER.info("checkExistVariant: " + checkExistVariant);
			count++;
			if (count == 10) {
				throw new BadRequestException(new SystemError("Invalid Variant", "Failed to create variant!" , "", "http://developer.30usd.com/errors/400.html"));
			}
			Thread.sleep(1 * 3000);
		}
		return variantMap;	
	}
		
	private static final Logger LOGGER = Logger.getLogger(ShopifySyncProductVariantHandler.class.getName());

}
