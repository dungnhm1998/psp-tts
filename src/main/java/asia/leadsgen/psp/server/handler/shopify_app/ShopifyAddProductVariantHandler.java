package asia.leadsgen.psp.server.handler.shopify_app;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.data.type.RedisKeyEnum;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.obj.ShopifyImageObj;
import asia.leadsgen.psp.obj.ShopifyOptionObj;
import asia.leadsgen.psp.obj.ShopifyProductObj;
import asia.leadsgen.psp.obj.ShopifyProductPullObj;
import asia.leadsgen.psp.obj.ShopifySyncedProductObj;
import asia.leadsgen.psp.obj.ShopifyVariantObj;
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
import asia.leadsgen.psp.service.RedisService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
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

public class ShopifyAddProductVariantHandler implements Handler<RoutingContext> {
			
	@Override
	public void handle(RoutingContext routingContext) {
				
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			
			String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
			LOGGER.info("userId= " + userId);
			if (StringUtils.isEmpty(userId)) {
				throw new LoginException(SystemError.LOGIN_REQUIRED);
			}
			
			Map requestBodyMap = routingContext.getBodyAsJson().getMap();
			
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
			
			CampaignModel campModel = campaignPayload.getCampaign();
					
			String storeId = campaignPayload.getStoreId();
			LOGGER.info("storeId= " + storeId);
			if (StringUtils.isEmpty(storeId)) {
				throw new LoginException(SystemError.INVALID_DROPSHIP_STORE_ID);
			}
			
			Map storeMapDB = new LinkedHashMap<>();
			try {
				storeMapDB = DropShipStoreService.lookUp(storeId);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			String storeUserId = ParamUtil.getString(storeMapDB, AppParams.USER_ID);
			LOGGER.info("storeUserId= " + storeUserId);
			if (!storeUserId.equalsIgnoreCase(userId)) {
				throw new LoginException(SystemError.INVALID_USER);
			}
			
			try {		
				
				String campaignId = campModel.getId();
				String baseGroupId = campModel.getBaseGroupId();
				
				String consumerKey = ParamUtil.getString(storeMapDB, AppParams.API_KEY);
				String domain = ParamUtil.getString(storeMapDB, AppParams.DOMAIN);
				
				ProductModel productModel = campaignPayload.getProduct();
				
				String refId = routingContext.request().getParam(AppParams.ID);
				Long productRefId = Long.parseLong(refId);   	
				
				Map productVariantMap = ShopifyAppService.lookup(productRefId);
				int totalVariant = ParamUtil.getInt(productVariantMap, AppParams.TOTAL_VARIANT);			
				
				List<SizeModel> sizeModels = productModel.getSizes();
				List<ColorModel> colorModels = productModel.getColors();
				
				int maxShopifyVariant = sizeModels.size() * colorModels.size() + totalVariant;
				if (maxShopifyVariant > 100) {
					throw new BadRequestException(new SystemError
							("Invalid Request", "Shopify limit 100 variants per product", "", "http://developer.30usd.com/errors/400.html"));
				}
							
				String key = campaignId + "_" + refId + "_" + RedisKeyEnum.TASK_PROCESS_ADD_VARIANT_SHOPIFY.getValue();
				
				Map task = RedisService.get(key);
				
				if (task != null && !task.isEmpty()) {
					
					Map mapResult = new LinkedHashMap();
					mapResult.put(AppParams.RESULT_MSG, "This job is being processed, you must wait for this job to finish.");
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.SEE_OTHER.code());
					routingContext.put(AppParams.RESPONSE_MSG, "This job is being processed, you must wait for this job to finish.");
					routingContext.put(AppParams.RESPONSE_DATA, mapResult);
					
				} else {					
										 
					Map product = ParamUtil.getMapData(requestBodyMap, AppParams.PRODUCT);	
					List<Map> requestColorList = ParamUtil.getListData(product, AppParams.COLORS);
					List<Map> requestSizeList = ParamUtil.getListData(product, AppParams.SIZES);
					LOGGER.info("requestColorList: " + requestColorList.toString());
					LOGGER.info("requestSizeList: " + requestSizeList.toString());
										
					LOGGER.info("Add New Variant to Product in Shopify App --- UserId: " + userId + ", StoreId: " + storeId + ", CampaignId: " + campaignId + ", ProductRefId: " + refId);
					
					Map productAddMeta = new LinkedHashMap<>();									
					productAddMeta.put("id", productRefId);
					
//					List<Map> metafields = new ArrayList<>();
//					Map metafield = new LinkedHashMap<>();
//					metafield.put("key", "webhook_create_product");
//					metafield.put("value", "webhook_create_product");
//					metafield.put("value_type", "string");
//					metafield.put("namespace", "burgerprints");
//					metafields.add(metafield);
//					productAddMeta.put("metafields", metafields);
					
					Map productUpdate = new HashMap<>();
					productUpdate.put("product", productAddMeta);
					
					String putMetaUrl = String.format(ShopifyAPIEndpoints.PRODUCT_USING_TOKEN, domain, productRefId);
					String requestBody = new JsonObject(productUpdate).encode();
					LOGGER.info("putMetaUrl: " + putMetaUrl);
					LOGGER.info("requestBody: " + requestBody);
					
					HttpResponse<String> putResponse = Unirest.put(putMetaUrl).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
							.body(requestBody)
							.asString();
					LOGGER.info("put metafield response-status:" + putResponse.getStatus());
					LOGGER.info("data result text:" + putResponse.getStatusText());
					
					Map campaignInfo = CampaignService.getV2(campaignId);
					
					List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);
					
					String baseIdRequest = productModel.getBaseId();
					LOGGER.info("baseIdRequest: " + baseIdRequest);
					
					List<String> designTypeList = new ArrayList<>();
					Map storeMap = DropShipStoreService.lookUp(storeId);
					
					Map map = new LinkedHashMap<Integer, String>();
					map.put(AppParams.START_TIME, new Date());
					map.put(AppParams.PRODUCT_REF_ID, refId);
					map.put(AppParams.CAMPAIGN_ID, campaignId);
					
					LOGGER.info("save key: " + key);
					RedisService.save(key, map);
					Thread one = new Thread() {
						public void run() {
							try {
								
								Map syncProduct = new LinkedHashMap<>();
								int countCreatedProduct = 0;
								
								for (Map campProduct : campaignProductList) {
									
									Map base = ParamUtil.getMapData(campProduct, AppParams.BASE);
									String baseIdDb = ParamUtil.getString(base, AppParams.ID);
									String designType = ParamUtil.getString(base, AppParams.DESIGN_TYPE);
									designTypeList.add(designType);

									LOGGER.info("baseIdDb: " + baseIdDb);
									if (baseIdDb.equalsIgnoreCase(baseIdRequest)) {
										String productId = ParamUtil.getString(campProduct, AppParams.ID);
										LOGGER.info("Create new variant with baseId: " + baseIdDb);
										ShopifyCreateVariantHelper.createNewVariant(campaignId, productId, campModel, productModel, userId);
										Thread.sleep(1 * 1000);
										syncProduct = getProductToSync(campaignId, productId, requestColorList, requestSizeList, productRefId, storeMap);
										break;
									}
									countCreatedProduct++;
								}
								
								if (countCreatedProduct == campaignProductList.size()) {
									LOGGER.info("Create new product!");
									LOGGER.info("productModel= " + productModel.toString());
									productModel.setDefault(false);
									syncProduct = createNewCampProduct(campaignId, productModel, storeMap, designTypeList, countCreatedProduct, userId, requestColorList, requestSizeList,
											productRefId, routingContext, future);
								}
								
								LOGGER.info("syncProduct:" + syncProduct.toString());
								int totalAddNewVariant = 0;
								if (syncProduct != null && syncProduct.isEmpty() == false) {
										
									List<Map> variantList = ParamUtil.getListData(syncProduct, AppParams.VARIANTS);
									totalAddNewVariant = variantList.size();
									LOGGER.info("totalAddNewVariant= " + totalAddNewVariant);
									String productId = ParamUtil.getString(syncProduct, AppParams.PRODUCT_ID);
									
									for (Map syncVariant : variantList) {
										// Insert synced Variant to DB
										syncProductVariant(syncVariant, productId);
										
										/**
										 * Push synced Variant to Shopify
										 */
										pushVariantToShopifyStore(productRefId, syncVariant, storeMap);
									}
									
									/**
									 * Update new Product Option and Product Image to DB
									 */
									updateProductOptionAndImage(productRefId, storeMap);										
								}
								
							    LOGGER.info("delete key: " + key);
								RedisService.delete(key);
								
							} catch (Exception e) {
								e.printStackTrace();
								LOGGER.severe(e.toString());
								LOGGER.info("delete key: " + key);
								RedisService.delete(key);
							}
						}
					};
					one.start();
					
					Map mapResult = new LinkedHashMap();
					mapResult.put(AppParams.RESULT_MSG, "This job is being processed, you must wait for this job to finish.");
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, "This job is being processed, you must wait for this job to finish.");
					routingContext.put(AppParams.RESPONSE_DATA, mapResult);	
				}
							
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
	
	private Map createNewCampProduct(String campaignId, ProductModel pModel, Map storeMap, List<String> designTypeList, int position, String userId,
			List<Map> requestColorList, List<Map> requestSizeList, Long productRefId, RoutingContext routingContext, Future<Object> future) 
			throws SQLException, UnsupportedEncodingException, ClassNotFoundException, ParseException, InterruptedException, UnirestException, JsonProcessingException {
		
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
		Thread.sleep(1 * 5000);
		Map syncProduct = getProductToSync(campaignId, productId, requestColorList, requestSizeList, productRefId, storeMap);
		
		return syncProduct;
	}

	/**
	 * Return Formated Product Map to Sync
	 * @param campaignId
	 * @param product
	 * @param productRefId
	 * @param consumerKey
	 * @param domain
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 * @throws UnirestException
	 * @throws InterruptedException 
	 */
	private Map getProductToSync(String campaignId, String productId, List<Map> requestColorList, List<Map> requestSizeList, Long productRefId, Map storeMap) 
			throws SQLException, ParseException, UnirestException, InterruptedException {
		
		Map campaignInfo = CampaignService.getV2(campaignId);
		
		List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);	
			
		Map campaignProduct = campaignProductList.stream().filter(m -> (m.get(AppParams.ID)).equals(productId)).findFirst().get();
		
		List<String> colorIdList = requestColorList.stream().map(o -> ParamUtil.getString(o, AppParams.ID)).collect(Collectors.toList());
			
		List<Map> syncVariantList = new ArrayList<>();
		
		Map<String, String> baseSizeMap = BaseService.getBaseSizeMap();
		
		String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
		String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
		String currency = ParamUtil.getString(storeMap, AppParams.CURRENCY);
		
		boolean backView = ParamUtil.getBoolean(campaignProduct, AppParams.BACK_VIEW);
		LOGGER.info("backView= " + backView);
		
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
		
		int countNewColor = 0;
		
		for (String colorId : colorIdList) {
			LOGGER.info("colorId: " + colorIdList);
			/**
			 * Get Variant with this launchingColorId
			 */
			Map variantMap = checkExistVariant(campaignId, productId, colorId);		
			String variantId = ParamUtil.getString(variantMap, AppParams.ID);		
			LOGGER.info("variantId: " + variantId);
			/**
			 * Check imageId is existed with this variantId in tb_shopify_synced_product_variant
			 */
			Long imageId = ShopifyAppService.getImageIdFromSyncedVariant(variantId);
			LOGGER.info("imageId: " + imageId);
			String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
			
			if (imageId != null) {
						
				if (BasePhoneCaseUtil.isPhoneCase(baseId)) {
					List<Map> priceMapList = ParamUtil.getListData(campaignProduct, AppParams.PRICES);
					String phoneSizeId = ParamUtil.getString(priceMapList.get(0), AppParams.SIZE_ID);
					String phoneSizeName = ParamUtil.getString(priceMapList.get(0), AppParams.SIZE_NAME);
					Map pushVariantMap = formatSyncVariant(variantMap, campaignProduct, designFrontUrl, designBackUrl, 
							phoneSizeId, phoneSizeName, requestSizeList.get(0), currency, imageId);
					
					syncVariantList.add(pushVariantMap);
					
				} else {
					String sizeIds = ShopifyAppService.getSizeIdsFromSyncedVariant(variantId);
					LOGGER.info("sizeIds: " + sizeIds);
					
					for (Map sizeMap : requestSizeList) {
						String sizeId = ParamUtil.getString(sizeMap, AppParams.ID);
						String sizeName = baseSizeMap.get(sizeId);
						String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
						if (sizeIds.contains(sizeId)) {				
							LOGGER.info("Size: " + sizeName + " is selected with Color " + colorName);
							continue;				
						}
						Map pushVariantMap = formatSyncVariant(variantMap, campaignProduct, designFrontUrl, designBackUrl, 
								sizeId, sizeName, sizeMap, currency, imageId);						
						
						syncVariantList.add(pushVariantMap);
					}
				}			
				
			} else {
				
				/**
				 * If imageId is not existed, push new image to Shopify
				 */
				countNewColor++;
				
				Map image = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
				String mockup_front_url = ParamUtil.getString(image, AppParams.FRONT);				
				String mockup_back_url = ParamUtil.getString(image, AppParams.BACK);
				
				String imgPush = mockup_front_url;		
				List<String> allImagesProduct = new ArrayList<>();

				if (mockup_back_url != null && mockup_back_url .isEmpty() == false) {
					if (!backView) {
						allImagesProduct.add(mockup_back_url);
					} else {
						imgPush = mockup_back_url;
						allImagesProduct.add(mockup_front_url);
					}			
				}
				
				Long newImageId = Long.parseLong(updateImageShopifyStore(productRefId, imgPush, countNewColor, consumerKey, domain));
				
				for (String url : allImagesProduct) {
					updateImageShopifyStore(productRefId, url, countNewColor, consumerKey, domain);
				}
				
				if (BasePhoneCaseUtil.isPhoneCase(baseId)) {
					List<Map> priceMapList = ParamUtil.getListData(campaignProduct, AppParams.PRICES);
					String phoneSizeId = ParamUtil.getString(priceMapList.get(0), AppParams.SIZE_ID);
					String phoneSizeName = ParamUtil.getString(priceMapList.get(0), AppParams.SIZE_NAME);
					Map pushVariantMap = formatSyncVariant(variantMap, campaignProduct, designFrontUrl, designBackUrl, 
							phoneSizeId, phoneSizeName, requestSizeList.get(0), currency, newImageId);
					
					syncVariantList.add(pushVariantMap);
					
				} else {
							
					for (Map sizeMap : requestSizeList) {
						String sizeId = ParamUtil.getString(sizeMap, AppParams.ID);
						String sizeName = baseSizeMap.get(sizeId);
						Map pushVariantMap = formatSyncVariant(variantMap, campaignProduct, designFrontUrl, designBackUrl, 
								sizeId, sizeName, sizeMap, currency, newImageId);		
						
						syncVariantList.add(pushVariantMap);
					}
				}
			}
		}
		
		Map syncProduct = new LinkedHashMap<>();	
		if (syncVariantList != null && syncVariantList.isEmpty() == false) {					
			syncProduct.put(AppParams.VARIANTS, syncVariantList);
			syncProduct.put(AppParams.PRODUCT_ID, productId);
		}
				
		return syncProduct;
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
	
	private Map formatSyncVariant(Map variantMap, Map campaignProduct, String designFrontUrl, String designBackUrl, 
			String sizeId, String sizeName, Map sizeMap, String currency, Long imageId) {
		
		Map pushVariantMap = new LinkedHashMap<>();
		pushVariantMap.put(AppParams.VARIANT_ID, ParamUtil.getString(variantMap, AppParams.ID));
		pushVariantMap.put(AppParams.BASE_ID, ParamUtil.getString(variantMap, AppParams.BASE_ID));
		pushVariantMap.put(AppParams.COLOR_ID, ParamUtil.getString(variantMap, AppParams.COLOR_ID));
		pushVariantMap.put(AppParams.COLOR_NAME, ParamUtil.getString(variantMap, AppParams.COLOR_NAME));
		pushVariantMap.put(AppParams.COLOR, ParamUtil.getString(variantMap, AppParams.COLOR));
			
		Map img = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
		pushVariantMap.put(AppParams.MOCKUP_FRONT_URL, ParamUtil.getString(img, AppParams.FRONT));
		pushVariantMap.put(AppParams.MOCKUP_BACK_URL, ParamUtil.getString(img, AppParams.BACK));

		pushVariantMap.put(AppParams.DESIGN_FRONT_URL, designFrontUrl);
		pushVariantMap.put(AppParams.DESIGN_BACK_URL, designBackUrl);
		
		pushVariantMap.put(AppParams.SALE_EXPECTED, ParamUtil.getString(campaignProduct, AppParams.SALE_EXPECTED));
		pushVariantMap.put(AppParams.PRODUCT_NAME, ParamUtil.getString(campaignProduct, AppParams.PRODUCT_NAME));
		
		pushVariantMap.put(AppParams.SIZE_ID, sizeId);
		pushVariantMap.put(AppParams.SIZE_NAME, sizeName);
		pushVariantMap.put(AppParams.PRICE, ParamUtil.getString(sizeMap, AppParams.SALE_PRICE));
		pushVariantMap.put(AppParams.CURRENCY, currency);
						
		pushVariantMap.put(AppParams.IMAGE_ID, imageId);
		
		return pushVariantMap;
	}
	
	/**
	 * Insert synced Variant to tb_shopify_synced_product_variant
	 * @param syncVariant
	 * @param productId
	 * @throws Exception 
	 */
	private void syncProductVariant(Map syncVariant, String productId) throws Exception {
		
		LOGGER.info("syncVariant: " + syncVariant.toString());
		String variantId = ParamUtil.getString(syncVariant, AppParams.VARIANT_ID);
		String baseId = ParamUtil.getString(syncVariant, AppParams.BASE_ID);
		String sizeId = ParamUtil.getString(syncVariant, AppParams.SIZE_ID);
		String sizeName = ParamUtil.getString(syncVariant, AppParams.SIZE_NAME);
		String colorId = ParamUtil.getString(syncVariant, AppParams.COLOR_ID);
		String colorName = ParamUtil.getString(syncVariant, AppParams.COLOR_NAME);
		String colorValue = ParamUtil.getString(syncVariant, AppParams.COLOR);
					
		String design_front_url = ParamUtil.getString(syncVariant, AppParams.DESIGN_FRONT_URL);
		String mockup_front_url = ParamUtil.getString(syncVariant, AppParams.MOCKUP_FRONT_URL);
		if (mockup_front_url == null || mockup_front_url.isEmpty()) {
			LOGGER.info("mockup_front_url: " + mockup_front_url);
			throw new BadRequestException(SystemError.INVALID_MOCKUP_IMAGE);
		}
		String design_back_url = ParamUtil.getString(syncVariant, AppParams.DESIGN_BACK_URL);	
		String mockup_back_url = ParamUtil.getString(syncVariant, AppParams.MOCKUP_BACK_URL);
		
		String salePrice = ParamUtil.getString(syncVariant, AppParams.PRICE);
		int saleExpected = ParamUtil.getInt(syncVariant, AppParams.SALE_EXPECTED);
		String currency = ParamUtil.getString(syncVariant, AppParams.CURRENCY);
		
		LOGGER.info("productId: " + productId);
		LOGGER.info("baseId: " + baseId);
		LOGGER.info("sizeId: " + sizeId);
		LOGGER.info("colorName: " + colorName);
		String sku = "";
		sku = ShopifyAppService.checkSku(baseId, sizeId, colorName);
		
		ShopifySyncedProductObj syncedProductObj = new ShopifySyncedProductObj();
		syncedProductObj.setBgpVariantId(variantId);
		syncedProductObj.setBaseId(baseId);
		syncedProductObj.setSizeId(sizeId);
		syncedProductObj.setColorId(colorId);
//		syncedProductObj.setImageId(null);
		syncedProductObj.setFrontDesign(design_front_url);
		syncedProductObj.setFrontMockup(mockup_front_url);
		syncedProductObj.setBackDesign(design_back_url);
		syncedProductObj.setBackMockup(mockup_back_url);
		syncedProductObj.setBgpProductId(productId);
		syncedProductObj.setSalePrice(salePrice);
		syncedProductObj.setCurrency(currency);
		syncedProductObj.setSaleExpected(saleExpected);
		syncedProductObj.setSku(sku);
		syncedProductObj.setColorName(colorName);
		syncedProductObj.setSizeName(sizeName);
		syncedProductObj.setColorValue(colorValue);
		
		Map result = ShopifyAppService.syncProductVariant(syncedProductObj);
//		LOGGER.info("result:" + result.toString());
	}
	
	private void pushVariantToShopifyStore(Long productRefId, Map syncVariant, Map storeMap) 
			throws SQLException, UnirestException, ParseException, Exception {
		
		String variantSyncId = ParamUtil.getString(syncVariant, AppParams.VARIANT_ID);
		
		LOGGER.info("Push Variant to Shopify Store --- ProductRefId: " + productRefId + ", BGP VariantId: " + variantSyncId);
		
		String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
		String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
		
		String sizeId = ParamUtil.getString(syncVariant, AppParams.SIZE_ID);
		String sizeName = ParamUtil.getString(syncVariant, AppParams.SIZE_NAME);
		String colorName = ParamUtil.getString(syncVariant, AppParams.COLOR_NAME);
		String productName = ParamUtil.getString(syncVariant, AppParams.PRODUCT_NAME);
			
		String productStoreId = Long.toString(productRefId);
		Long imageId = Long.parseLong(ParamUtil.getString(syncVariant, AppParams.IMAGE_ID));
					
		String price = ParamUtil.getString(syncVariant, AppParams.PRICE);
		double compare_at_price = Double.parseDouble(price) * 120 / 100;
		
		Map variantPush = new LinkedHashMap<>();
		Map variant =  new LinkedHashMap<>();
		
		variant.put("image_id", imageId);

		List<Map> shopifyProductOptionList = ShopifyAppService.getShopifyProductOption(productRefId);
		int countOption = shopifyProductOptionList.size();
		for (int i = 0; i < countOption; i++) {
			Map productOption = shopifyProductOptionList.get(i);
			String optionName = ParamUtil.getString(productOption, AppParams.S_NAME);
			String optionNumber = "option" + String.valueOf(i+1);
			if (optionName.equalsIgnoreCase("Name")) {
				variant.put(optionNumber, productName);
			} else if (optionName.equalsIgnoreCase("Color")) {
				variant.put(optionNumber, colorName);
			} else if (optionName.equalsIgnoreCase("Size")) {
				variant.put(optionNumber, sizeName);
			}
		}
	
		variant.put("price", price);
		variant.put("compare_at_price", compare_at_price);
		variant.put("sku", variantSyncId + "|" + sizeId);
		
		// Hardcode inventory_quantity
//		variant.put(AppParams.INVENTORY_QUANTITY, 9999);
		variant.put(AppParams.INVENTORY_MANAGEMENT, null);
//		variant.put(AppParams.FULFILLMENT_SERVICE, "BurgerPrints");
		variant.put(AppParams.INVENTORY_POLICY, AppParams.DENY);
		variantPush.put("variant", variant);
		LOGGER.info("variantPush: " + variantPush.toString());
		
		String requestBody = new JsonObject(variantPush).encode();
		
		String url = String.format(ShopifyAPIEndpoints.PRODUCT_VARIANT_USING_TOKEN, domain, productStoreId);
		
		HttpResponse<String> postResponse = Unirest.post(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.body(requestBody)
				.asString();
		
		if (postResponse.getStatus() != 200 && postResponse.getStatus() != 201) {
		    LOGGER.info("push variant to shopify store response-status: " + postResponse.getStatus());
		    LOGGER.info("data result text: " + postResponse.getStatusText());
		    LOGGER.info("message: " + postResponse.getBody());
		    ShopifyAppService.deletedSyncedVariant(variantSyncId, sizeId);
		    LOGGER.info("Deleted VariantSyncId: " + variantSyncId);
		} else {
			ShopifyProductObj productObj = null;
			productObj = new Gson().fromJson(postResponse.getBody().toString(), ShopifyProductObj.class);
			
			ShopifyVariantObj variantObj = productObj.getVariant();
			ShopifyAppService.insertProductVariant(variantObj, 1);
			ShopifyAppService.updateRefIdToSyncedVariant(variantObj);	
//			LOGGER.info("Shopify Store VariantId: " + variantObj.getId());
		}				
	}
	
	/**
	 * Push New Image to Shopify
	 * @param productRefId
	 * @param mockup_front_url
	 * @param consumerKey
	 * @param domain
	 * @return
	 * @throws UnirestException
	 * @throws SQLException
	 */
	private String updateImageShopifyStore(Long productRefId, String imgUrl, int countNewColor, String consumerKey, String domain) 
			throws UnirestException, SQLException {
		
		LOGGER.info("Update Image to Shopify Store...");
		LOGGER.info("url= " + imgUrl);
		
		String productStoreId = Long.toString(productRefId);
		Map image = new HashMap<>();
		Map imageSrc = new HashMap<>();
		imageSrc.put("src", imgUrl);	
		image.put("image", imageSrc);
		
		String requestPostBody = new JsonObject(image).encode();
		
		String url = String.format(ShopifyAPIEndpoints.PRODUCT_IMAGE_USING_TOKEN, domain, productStoreId);
		
		HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", "id")
				.body(requestPostBody)
				.asString();
		if (response.getStatus() != 200 && response.getStatus() != 201) {
		    LOGGER.info("update image to shopify store response-status: " + response.getStatus());
		    LOGGER.info("data result text: " + response.getStatusText());
		    LOGGER.info("message: " + response.getBody());
//		    ShopifyAppService.updateStateSyncedProductVariant(variantSyncId, ResourceStates.DELETED);
		    throw new BadRequestException(SystemError.INVALID_REQUEST);
		}
		
		Map responseMap = new JsonObject(response.getBody()).getMap();
		Map imageMap = ParamUtil.getMapData(responseMap, "image");
		String imgId = ParamUtil.getString(imageMap, "id");
				
		return imgId;	
	}

	private void updateProductOptionAndImage(Long productRefId, Map storeMap) throws UnirestException, SQLException {
		
		LOGGER.info("Update Product Option and Image...");
		
		String productStoreId = Long.toString(productRefId);
		
		String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
		String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
		
		String url = String.format(ShopifyAPIEndpoints.PRODUCTS_ONE_USING_TOKEN, domain, productStoreId);
		
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", "options,images")
				.asString();
		LOGGER.info("get product response-status:" + response.getStatus());
		
		ShopifyProductPullObj productPullObj = new Gson().fromJson(response.getBody().toString(), ShopifyProductPullObj.class);	
		
		ShopifyProductObj productObj = productPullObj.getProduct();
		
		List<ShopifyOptionObj> updateOptionList = productObj.getOptions();
		for (ShopifyOptionObj opObj : updateOptionList) {
			ShopifyAppService.updateProductOptions(opObj);
		}
				
		List<ShopifyImageObj> updateImageList = productObj.getImages();

		for (ShopifyImageObj imgObj : updateImageList) {
			List<String> variantIdList = imgObj.getVariantIds().stream().map(l -> String.valueOf(l)).collect(Collectors.toList());
			String variantIds = String.join(",", variantIdList);
			ShopifyAppService.insertProductImage(imgObj, variantIds);
		}
	}
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyAddProductVariantHandler.class.getName());

}