package asia.leadsgen.psp.server.handler.shopify_app;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
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
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.RedisService;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.PartnerConst;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyAddProductHandler implements Handler<RoutingContext> {
	
	static final String FIELDS = "id,title,body_html,vendor,product_type,handle,tags,variants,options,images";

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
			
			String storeId = ParamUtil.getString(requestBodyMap, AppParams.STORE_ID);
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
				PartnerConst.initPartnerConst();
				String storeName = ParamUtil.getString(storeMap, AppParams.NAME);
            	String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
				String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
				String currency = ParamUtil.getString(storeMap, AppParams.CURRENCY);
				
				String campaignId = ParamUtil.getString(requestBodyMap, AppParams.CAMPAIGN_ID);
				List<Map> productList = ParamUtil.getListData(requestBodyMap, AppParams.PRODUCTS);
				String collections = ParamUtil.getString(requestBodyMap, AppParams.COLLECTION_ID);
				String tags = ParamUtil.getString(requestBodyMap, AppParams.TAGS);
				
				LOGGER.info("Add Product to Shopify App --- UserId: " + userId + ", StoreId: " + storeId + ", CampaignId: " + campaignId);
				
				/**
				 * Get Formated Product to sync
				 */
				List<Map> syncProductList = getProductListToSync(campaignId, productList, currency, collections, tags);
//				LOGGER.info("syncProductList= " + syncProductList.toString());							
				
				if (syncProductList.size() == 1) {
					
					LOGGER.info("syncProductList size= " + syncProductList.size());
					LOGGER.info("Push Campaign with 1 Product to Shopify Store!");
					
					Map syncProduct = syncProductList.get(0);
					List<Map> variantList = ParamUtil.getListData(syncProduct, AppParams.VARIANTS);					
					for (Map syncVariant : variantList) {
						/**
						 *  Insert synced Variant to DB
						 */
						syncProductVariant(syncVariant);
					}
					
					/**
					 * Push synced Product to Shopify
					 */
					pushCampOneProductToShopifyStore(campaignId, syncProduct, storeId, consumerKey, domain, storeName, routingContext, future);
					
				} else {
					
					LOGGER.info("syncProductList size= " + syncProductList.size());
					LOGGER.info("Push Campaign with Many Products to Shopify Store!");
					
					Map syncFirstProduct = syncProductList.get(0);
					List<Map> listVariantOfFirstProduct = ParamUtil.getListData(syncFirstProduct, AppParams.VARIANTS);					
					for (Map syncVariant : listVariantOfFirstProduct) {
						/**
						 *  Insert synced Variant to DB
						 */
						syncProductVariant(syncVariant);
					}
					
					/**
					 * Push synced Product to Shopify
					 */
					pushCampManyProductsToShopifyStore(campaignId, syncFirstProduct, storeId, consumerKey, domain, storeName, routingContext);
					
					String key = campaignId + "_" + RedisKeyEnum.TASK_PROCESS_ADD_PRODUCT_SHOPIFY.getValue();
					
					Map map = new LinkedHashMap<Integer, String>();
					map.put(AppParams.START_TIME, new Date());
					map.put(AppParams.STORE_ID, storeId);
					map.put(AppParams.CAMPAIGN_ID, campaignId);
					
					LOGGER.info("save key: " + key);
					RedisService.save(key, map);
					Thread one = new Thread() {
						public void run() {
							try {
								
								syncProductList.remove(0);
								for (Map syncProduct : syncProductList) {
									List<Map> variantList = ParamUtil.getListData(syncProduct, AppParams.VARIANTS);
									for (Map syncVariant : variantList) {
										/**
										 *  Insert synced Variant to DB
										 */
										syncProductVariant(syncVariant);
									}
									
									/**
									 * Push synced Product to Shopify
									 */
									pushCampManyProductsToShopifyStore(campaignId, syncProduct, storeId, consumerKey, domain, storeName, routingContext);
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
					
					Map productSearchResult = ShopifyAppService.searchProduct(storeId, "", "", 1, 10);
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, productSearchResult);
					
					future.complete();
					
				}
				
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
	
	/**
	 * Return Formated Product List to Sync
	 * @param campaignId
	 * @param productList: received from Api CampaignCreateV2Handler
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 */
	private List<Map> getProductListToSync(String campaignId, List<Map> productList, String currency, String collections, String tags) throws SQLException, ParseException {
		
		Map campaignInfo = CampaignService.getV2(campaignId);
		
		List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);
		
		String desc = ParamUtil.getString(campaignInfo, AppParams.DESC);
		
		String title = ParamUtil.getString(campaignInfo, AppParams.TITLE);
		
		List<Map> syncProductList = new ArrayList<>();
		
		for (Map campaignProduct : campaignProductList) {
			
			String productId = ParamUtil.getString(campaignProduct, AppParams.ID);
			
			String productName = ParamUtil.getString(campaignProduct, AppParams.PRODUCT_NAME);
			LOGGER.info("productName= " + productName);

			String productType = ParamUtil.getString(campaignProduct, AppParams.PRODUCT_TYPE);
						
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
			
			List<Map> variantMapList = ParamUtil.getListData(campaignProduct, AppParams.VARIANTS);
			
			/**
			 * Get Size List from productList
			 */
			Map pushProduct = productList.stream().filter(m -> (m.get(AppParams.ID)).equals(productId)).findFirst().get();
			List<Map> sizeList = ParamUtil.getListData(pushProduct, AppParams.SIZES);
			Map<String, String> baseSizeMap = BaseService.getBaseSizeMap();
			
			Map mappingVariantImageMap = new LinkedHashMap<>();
			List<String> mappingVariantDefaultImage = new ArrayList<>();
			List<Map> syncVariantList = new ArrayList<>();
			for (Map variantMap : variantMapList) {
				
				String variantId = ParamUtil.getString(variantMap, AppParams.ID);
				String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
				String colorId = ParamUtil.getString(variantMap, AppParams.COLOR_ID);
				String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
				String color = ParamUtil.getString(variantMap, AppParams.COLOR);
				boolean defaultVariant = ParamUtil.getBoolean(variantMap, AppParams.DEFAULT);
				int nOrder = ParamUtil.getInt(variantMap, AppParams.ORDER);
				Map img = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
				String imgFront = ParamUtil.getString(img, AppParams.FRONT);
				String imgBack = ParamUtil.getString(img, AppParams.BACK);
				
				if (!backView) {
					String imgSplit = StringUtils.substring(StringUtils.substring(imgFront, 0, StringUtils.lastIndexOf(imgFront, ".")), StringUtils.lastIndexOf(imgFront, "/") + 1);
					mappingVariantImageMap.put(productName + " / " + colorName, imgSplit);
				} else {
					String imgSplit = StringUtils.substring(StringUtils.substring(imgBack, 0, StringUtils.lastIndexOf(imgBack, ".")), StringUtils.lastIndexOf(imgBack, "/") + 1);
					mappingVariantImageMap.put(productName + " / " + colorName, imgSplit);
				}
				
				if (nOrder != 0 && defaultVariant) {
					String imgFrontDefaultSplit = StringUtils.substring(StringUtils.substring(imgFront, 0, StringUtils.lastIndexOf(imgFront, ".")), StringUtils.lastIndexOf(imgFront, "/") + 1);
					String imgBackDefaultSplit = StringUtils.substring(StringUtils.substring(imgBack, 0, StringUtils.lastIndexOf(imgBack, ".")), StringUtils.lastIndexOf(imgBack, "/") + 1);
					if (!backView) {
						mappingVariantDefaultImage.add(imgFrontDefaultSplit);
						if (StringUtils.isNotEmpty(imgBackDefaultSplit)) {
							mappingVariantDefaultImage.add(imgBackDefaultSplit);
						}
						
					} else {
						mappingVariantDefaultImage.add(imgBackDefaultSplit);
						mappingVariantDefaultImage.add(imgFrontDefaultSplit);
					}
				}
						
				for (Map sizeMap : sizeList) {
					
					Map pushVariantMap = new LinkedHashMap<>();
					pushVariantMap.put(AppParams.PRODUCT_ID, productId);
					pushVariantMap.put(AppParams.PRODUCT_NAME, productName);
					pushVariantMap.put(AppParams.VARIANT_ID, variantId);
					pushVariantMap.put(AppParams.BASE_ID, baseId);
					pushVariantMap.put(AppParams.COLOR_ID, colorId);
					pushVariantMap.put(AppParams.COLOR_NAME, colorName);
					pushVariantMap.put(AppParams.COLOR, color);
					pushVariantMap.put(AppParams.DEFAULT, defaultVariant);
					
					pushVariantMap.put(AppParams.MOCKUP_FRONT_URL, imgFront);
					pushVariantMap.put(AppParams.MOCKUP_BACK_URL, imgBack);			
					pushVariantMap.put(AppParams.DESIGN_FRONT_URL, designFrontUrl);
					pushVariantMap.put(AppParams.DESIGN_BACK_URL, designBackUrl);
					
					pushVariantMap.put(AppParams.SALE_EXPECTED, ParamUtil.getString(campaignProduct, AppParams.SALE_EXPECTED));
					
					String sizeId = ParamUtil.getString(sizeMap, AppParams.ID);
					pushVariantMap.put(AppParams.SIZE_ID, sizeId);
					String sizeName = baseSizeMap.get(sizeId);
//					LOGGER.info("sizeName= " + sizeName);
					pushVariantMap.put(AppParams.SIZE_NAME, sizeName);
					pushVariantMap.put(AppParams.PRICE, ParamUtil.getString(sizeMap, AppParams.SALE_PRICE));
					pushVariantMap.put(AppParams.CURRENCY, currency);
					pushVariantMap.put(AppParams.BACK_VIEW, backView);
					
					syncVariantList.add(pushVariantMap);
				}
			}
			
			/**
			 * Format Product to Sync
			 */
			Map syncProduct = new LinkedHashMap<>();
			syncProduct.put(AppParams.CAMPAIGN_ID, campaignId);
			syncProduct.put(AppParams.PRODUCT_TYPE, productType);
			syncProduct.put(AppParams.TITLE, title);		
			syncProduct.put(AppParams.TAGS, tags);
			syncProduct.put(AppParams.COLLECTION_ID, collections);
			syncProduct.put(AppParams.DESCRIPTION, desc);	
			syncProduct.put(AppParams.VARIANTS, syncVariantList);		
			syncProduct.put(AppParams.IMAGES, mappingVariantImageMap);
			syncProduct.put(AppParams.DEFAULT, mappingVariantDefaultImage);
			syncProductList.add(syncProduct);
		}
		
		return syncProductList;
	}
	
	/**
	 * Insert synced Variant to tb_shopify_synced_product_variant
	 * @param syncVariant
	 * @param productId
	 * @throws SQLException
	 */
	private void syncProductVariant(Map syncVariant) throws SQLException {
		
		String productId = ParamUtil.getString(syncVariant, AppParams.PRODUCT_ID);
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
//			LOGGER.info("mockup_front_url: " + mockup_front_url);
			throw new BadRequestException(SystemError.INVALID_MOCKUP_IMAGE);
		}
		String design_back_url = ParamUtil.getString(syncVariant, AppParams.DESIGN_BACK_URL);	
		String mockup_back_url = ParamUtil.getString(syncVariant, AppParams.MOCKUP_BACK_URL);
		
		String salePrice = ParamUtil.getString(syncVariant, AppParams.PRICE);
		int saleExpected = Integer.parseInt(ParamUtil.getString(syncVariant, AppParams.SALE_EXPECTED));
		String currency = ParamUtil.getString(syncVariant, AppParams.CURRENCY);
		
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
			
		ShopifyAppService.syncProductVariant(syncedProductObj);	
	}
	
	private void pushCampOneProductToShopifyStore(String campaignId, Map syncProduct, String storeId, String consumerKey, String domain, String storeName,
			RoutingContext routingContext, Future<Object> future) 
			throws SQLException, UnirestException, ParseException, UnsupportedEncodingException, InterruptedException {
		
		LOGGER.info("Push Product to Shopify Store --- CampaignId: " + campaignId);
		
		Map mappingVariantImageMap = ParamUtil.getMapData(syncProduct, AppParams.IMAGES);
		List<String> defaultImgUrl = ParamUtil.getListData(syncProduct, AppParams.DEFAULT);
		LOGGER.info("Default Image Url: " + defaultImgUrl);
		
		/**
		 * Create New Product with one Variant and Push to Shopify store form Map syncProduct
		 */	
		Map productInfo = createNewProductWithOneVariant(syncProduct, storeId, consumerKey, domain, storeName, mappingVariantImageMap);
		
		String key = campaignId + "_" + RedisKeyEnum.TASK_PROCESS_ADD_PRODUCT_SHOPIFY.getValue();
		
		Map map = new LinkedHashMap<Integer, String>();
		map.put(AppParams.START_TIME, new Date());
		map.put(AppParams.STORE_ID, storeId);
		map.put(AppParams.CAMPAIGN_ID, campaignId);
		
		LOGGER.info("save key: " + key);
		RedisService.save(key, map);
		Thread one = new Thread() {
			public void run() {
				try {
					
					String productStoreId = ParamUtil.getString(productInfo, "product_store_id");
					LOGGER.info("productStoreId: " + productStoreId);
					List<Map> allImagesUrl = new ArrayList<Map>();
					allImagesUrl = ParamUtil.getListData(productInfo, "all_images_url");
					LOGGER.info("allImagesUrl: " + allImagesUrl.toString());
					
					/** 
					 * Push other variants to Product in Shopify Store
					 */
					pushVariantsToProductShopify(syncProduct, productStoreId, consumerKey, domain);
					
					/**
					 * Push other images (back image, design images) to Shopify Store
					 */
					List<List<Map>> allImagesUrlSubLists = new ArrayList<>();
					if (allImagesUrl.size() > 0) {
						allImagesUrlSubLists = ListUtils.partition(allImagesUrl, 6);
					}
					LOGGER.info("allImagesUrlSubLists: " + allImagesUrlSubLists.toString());
					if (allImagesUrlSubLists.size() > 0) {
						for (List<Map> subList : allImagesUrlSubLists) {
							updateAllImages(productStoreId, subList, consumerKey, domain);
						}
					}
					
					/**
					 * Update image id for variants
					 */	
					ShopifyProductObj productObj = updateVariantV2(productStoreId, mappingVariantImageMap, consumerKey, domain, defaultImgUrl);
							

					List<ShopifyVariantObj> vObjList = productObj.getVariants();
					int totalVariant = vObjList.size();
					LOGGER.info("totalVariant= " + totalVariant);
					vObjList.remove(0);
					for (ShopifyVariantObj vObj : vObjList) {
						ShopifyAppService.insertProductVariant(vObj, 1);
						ShopifyAppService.updateRefIdToSyncedVariant(vObj);
					}
					
					List<ShopifyOptionObj> optionList = productObj.getOptions();
					for (ShopifyOptionObj opObj : optionList) {
						ShopifyAppService.insertProductOption(opObj);
					}
											
					List<ShopifyImageObj> imgObjList = productObj.getImages();
					for (ShopifyImageObj imgObj : imgObjList) {
						LOGGER.info("imgId= " + imgObj.getId());
						LOGGER.info("position= " + imgObj.getPosition());
						List<String> variantIdList = imgObj.getVariantIds().stream().map(l -> String.valueOf(l)).collect(Collectors.toList());
						String variantIds = String.join(",", variantIdList);
						LOGGER.info("variantIds= " + variantIds);
						ShopifyAppService.insertProductImage(imgObj, variantIds);
					}
					
					Thread.sleep(6000);
					
					Long productRefId = Long.parseLong(productStoreId);
					ShopifyAppService.updateStateProduct(productRefId, ResourceStates.APPROVED);
					
					LOGGER.info("published product...");
					Map requestMap = new HashMap<>();
					Map publishedProduct = new LinkedHashMap<>();
					publishedProduct.put("id", productStoreId);
					publishedProduct.put("published", true);
					requestMap.put("product", publishedProduct);
					LOGGER.info("published product: " + requestMap.toString());
					
					String requestBody = new JsonObject(requestMap).encode();
					
					String putPublishedUrl = String.format(ShopifyAPIEndpoints.PRODUCT_USING_TOKEN, domain, productStoreId);
					HttpResponse<String> responsePut = Unirest.put(putPublishedUrl).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
							.body(requestBody).asString();
					
					LOGGER.info("response published product status: " + responsePut.getStatus());
					if (responsePut.getStatus() != 200 && responsePut.getStatus() != 201) {
					    LOGGER.info("published product to shopify store response-status:" + responsePut.getStatus());
					    LOGGER.info("data result text:" + responsePut.getStatusText());
					    LOGGER.info("message: " + responsePut.getBody());
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
		
		Map productSearchResult = ShopifyAppService.searchProduct(storeId, "", "", 1, 10);
		
		routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
		routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
		routingContext.put(AppParams.RESPONSE_DATA, productSearchResult);
		
		future.complete();
	}
	
	private void pushCampManyProductsToShopifyStore(String campaignId, Map syncProduct, String storeId, String consumerKey, String domain, String storeName,
			RoutingContext routingContext)
			throws SQLException, UnirestException, ParseException, UnsupportedEncodingException, InterruptedException {
		
		LOGGER.info("Push Product to Shopify Store --- CampaignId: " + campaignId);
		
		Map mappingVariantImageMap = ParamUtil.getMapData(syncProduct, AppParams.IMAGES);
		List<String> defaultImgUrl = ParamUtil.getListData(syncProduct, AppParams.DEFAULT);
		LOGGER.info("Default Image Url: " + defaultImgUrl);
		
		/**
		 * Create New Product with one Variant and Push to Shopify store form Map syncProduct
		 */	
		Map productInfo = createNewProductWithOneVariant(syncProduct, storeId, consumerKey, domain, storeName, mappingVariantImageMap);
		
		String productStoreId = ParamUtil.getString(productInfo, "product_store_id");
		LOGGER.info("productStoreId: " + productStoreId);
		List<Map> allImagesUrl = new ArrayList<Map>();
		allImagesUrl = ParamUtil.getListData(productInfo, "all_images_url");
		LOGGER.info("allImagesUrl: " + allImagesUrl.toString());
		
		/** 
		 * Push other variants to Product in Shopify Store
		 */
		pushVariantsToProductShopify(syncProduct, productStoreId, consumerKey, domain);
		
		/**
		 * Push other images (back image, design images) to Shopify Store
		 */	
		List<List<Map>> allImagesUrlSubLists = new ArrayList<>();
		if (allImagesUrl.size() > 0) {
			allImagesUrlSubLists = ListUtils.partition(allImagesUrl, 6);
		}
		LOGGER.info("allImagesUrlSubLists: " + allImagesUrlSubLists.toString());
		if (allImagesUrlSubLists.size() > 0) {
			for (List<Map> subList : allImagesUrlSubLists) {
				updateAllImages(productStoreId, subList, consumerKey, domain);
			}
		}
		/**
		 * Update image id for variants
		 */	
		ShopifyProductObj productObj = updateVariantV2(productStoreId, mappingVariantImageMap, consumerKey, domain, defaultImgUrl);
				
		List<ShopifyVariantObj> vObjList = productObj.getVariants();
		int totalVariant = vObjList.size();
		LOGGER.info("totalVariant= " + totalVariant);
		vObjList.remove(0);
		for (ShopifyVariantObj vObj : vObjList) {
			ShopifyAppService.insertProductVariant(vObj, 1);
			ShopifyAppService.updateRefIdToSyncedVariant(vObj);
		}
		
		List<ShopifyOptionObj> optionList = productObj.getOptions();
		for (ShopifyOptionObj opObj : optionList) {
			ShopifyAppService.insertProductOption(opObj);
		}
								
		List<ShopifyImageObj> imgObjList = productObj.getImages();
		for (ShopifyImageObj imgObj : imgObjList) {
			LOGGER.info("imgId= " + imgObj.getId());
			LOGGER.info("position= " + imgObj.getPosition());
			List<String> variantIdList = imgObj.getVariantIds().stream().map(l -> String.valueOf(l)).collect(Collectors.toList());
			String variantIds = String.join(",", variantIdList);
			LOGGER.info("variantIds= " + variantIds);
			ShopifyAppService.insertProductImage(imgObj, variantIds);
		}
		
		Thread.sleep(6000);
		
		Long productRefId = Long.parseLong(productStoreId);
		ShopifyAppService.updateStateProduct(productRefId, ResourceStates.APPROVED);
		
		LOGGER.info("published product...");
		Map requestMap = new HashMap<>();
		Map publishedProduct = new LinkedHashMap<>();
		publishedProduct.put("id", productStoreId);
		publishedProduct.put("published", true);
		requestMap.put("product", publishedProduct);
		LOGGER.info("published product: " + requestMap.toString());
		
		String requestBody = new JsonObject(requestMap).encode();
		
		String putPublishedUrl = String.format(ShopifyAPIEndpoints.PRODUCT_USING_TOKEN, domain, productStoreId);
		HttpResponse<String> responsePut = Unirest.put(putPublishedUrl).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.body(requestBody).asString();
		
		LOGGER.info("response published product status: " + responsePut.getStatus());
		if (responsePut.getStatus() != 200 && responsePut.getStatus() != 201) {
		    LOGGER.info("published product to shopify store response-status:" + responsePut.getStatus());
		    LOGGER.info("data result text:" + responsePut.getStatusText());
		    LOGGER.info("message: " + responsePut.getBody());
		}
	}
	
	private static Map createNewProductWithOneVariant(Map syncProduct, String storeId, String consumerKey, String domain, String storeName, Map mappingVariantImageMap) 
			throws UnirestException, SQLException, InterruptedException {
		
		LOGGER.info("Push New Product with one Variant to Shopify store...");
		
		Map productInfo = new HashMap<>();
				
		Map productPush = new LinkedHashMap<>();		
		
		String titleProductSpf = ParamUtil.getString(syncProduct, AppParams.TITLE);
		
		List<Map> variantList = ParamUtil.getListData(syncProduct, AppParams.VARIANTS);
		Map variantMap = variantList.get(0);
        boolean checkDefaultVariant = variantList.stream().filter(m -> (m.get(AppParams.DEFAULT)).equals(true))
				.findFirst().isPresent();
        String colorDefault = "";
        Map defaultVariant = new HashMap<>();
        if (checkDefaultVariant) {
        	defaultVariant = variantList.stream().filter(m -> (m.get(AppParams.DEFAULT)).equals(true))
    				.findFirst().get();
        	colorDefault = ParamUtil.getString(defaultVariant, AppParams.COLOR_NAME);
        	
        }
        
//		boolean defaultVariant = ParamUtil.getBoolean(variantMap, AppParams.DEFAULT);
//		LOGGER.info("defaultVariant: " + defaultVariant);
		boolean backView = ParamUtil.getBoolean(variantMap, AppParams.BACK_VIEW);

		String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
		String productName = ParamUtil.getString(variantMap, AppParams.PRODUCT_NAME);
		if (baseId.matches(PartnerConst.getPrintwayBases())) {
			titleProductSpf = titleProductSpf + " - " + productName;
		}

		String variantId = ParamUtil.getString(variantMap, AppParams.VARIANT_ID);
		String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
		LOGGER.info("colorName: " + colorName);
		String sizeId = ParamUtil.getString(variantMap, AppParams.SIZE_ID);
		String sizeName = ParamUtil.getString(variantMap, AppParams.SIZE_NAME);
		String price = ParamUtil.getString(variantMap, AppParams.PRICE);
		double compare_at_price = Double.parseDouble(price) * 120 / 100;
		
		List<Map> variants = new ArrayList<>();
		Map variant =  new LinkedHashMap<>();
		
		variant.put("option1", productName);
		variant.put("option2", colorName);
		variant.put("option3", sizeName);

		variant.put("price", price);
		variant.put("compare_at_price", compare_at_price);
		variant.put("sku", variantId + "|" + sizeId );
		
//		variant.put(AppParams.INVENTORY_MANAGEMENT, null);
//		variant.put(AppParams.FULFILLMENT_SERVICE, "BurgerPrints");
		variant.put(AppParams.INVENTORY_POLICY, AppParams.DENY);
		variants.add(variant);
//		LOGGER.info("variants: " + variants.toString());
		
		List<String> colors = new ArrayList<>();
		colors.add(colorName);
		
		List<String> sizes = new ArrayList<>();
		sizes.add(sizeName);
		
		List<Map> options = new ArrayList<>();
		
		Map nameOption = new LinkedHashMap<>();
		nameOption.put("name", "Name");
		nameOption.put("values", productName);	
		options.add(nameOption);
		
		Map colorOption = new LinkedHashMap<>();
		colorOption.put("name", "Color");
		colorOption.put("values", colors);
		options.add(colorOption);
		
		Map sizeOption = new LinkedHashMap<>();
		
		sizeOption.put("name", "Size");
		sizeOption.put("values", sizes);
		options.add(sizeOption);
			
		Set<Map> imagePush = new LinkedHashSet<Map>();
		Set<Map> allImagesUrl = new LinkedHashSet<Map>();
		List<Map> imgDefaultList = new ArrayList<>();
		Map<String, String> imgDefault1 = new HashMap<>();
		Map<String, String> imgDefault2 = new HashMap<>();
		
		for (int i = 0; i < variantList.size(); i++) {
			
			Map varMap = variantList.get(i);
			
			String imageFront = ParamUtil.getString(varMap, AppParams.MOCKUP_FRONT_URL);
			String imageBack = ParamUtil.getString(varMap, AppParams.MOCKUP_BACK_URL);
			if (!StringUtils.isEmpty(imageBack)) {
				imageBack = imageBack + "?";
			}
			imageFront = imageFront + "?";
			
			Map<String, String> imgSourceFront = new HashMap<>();
			Map<String, String> imgSourceBack = new HashMap<>();
			imgSourceFront.put("src", imageFront);
			if (!StringUtils.isEmpty(imageBack)) {
				imgSourceBack.put("src", imageBack);
			}

			if (i < 1) {
				if (!backView) {				
					imagePush.add(imgSourceFront);
					imagePush.add(imgSourceBack);
				} else {
					imagePush.add(imgSourceBack);
					imagePush.add(imgSourceFront);
				}
			} else {
				if (!backView) {				
					allImagesUrl.add(imgSourceFront);
					allImagesUrl.add(imgSourceBack);
				} else {
					allImagesUrl.add(imgSourceBack);
					allImagesUrl.add(imgSourceFront);
				}
			}
			LOGGER.info("imagePush: " + imagePush.toString());
			LOGGER.info("allImagesUrl: " + allImagesUrl.toString());
		}
		
		for (Map m : imagePush) {
			allImagesUrl.remove(m);
		}
		
		List<Map> imagePushList = new ArrayList<Map>(imagePush);
		List<Map> allImagesUrlList = new ArrayList<Map>(allImagesUrl);
		
		LOGGER.info("imagePushList: " + imagePushList.toString());
		LOGGER.info("allImagesUrlList: " + allImagesUrlList.toString());
			
		productPush.put("title", titleProductSpf);
		productPush.put("body_html", StringUtil.urlDecode(ParamUtil.getString(syncProduct, AppParams.DESCRIPTION)));
		productPush.put("vendor", "BurgerPrints");
		productPush.put("product_type", ParamUtil.getString(syncProduct, AppParams.PRODUCT_TYPE));
		productPush.put("tags", ParamUtil.getString(syncProduct, AppParams.TAGS));
		productPush.put("published", false);
		productPush.put("variants", variants);
		
		productPush.put("options", options);
		productPush.put("images", imagePushList);
		
//		List<Map> metafields = new ArrayList<>();
//		Map metafield = new LinkedHashMap<>();
//		metafield.put("key", "webhook_create_product");
//		metafield.put("value", "webhook_create_product");
//		metafield.put("value_type", "string");
//		metafield.put("namespace", "burgerprints");
//		metafields.add(metafield);	
//		productPush.put("metafields", metafields);				
		
		Map product = new HashMap<>();
		product.put("product", productPush);
//		LOGGER.info("product: " + product.toString());
		
		String url = String.format(ShopifyAPIEndpoints.PRODUCTS_ALL_USING_TOKEN, domain);
		String requestBody = new JsonObject(product).encode();	
		
		HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", "id,variants,images")
				.body(requestBody)			
				.asString();
		LOGGER.info("response status: " + response.getStatus());
		if (response.getStatus() != 200 && response.getStatus() != 201) {
		    LOGGER.info("push product to shopify store response-status:" + response.getStatus());
		    LOGGER.info("data result text:" + response.getStatusText());
		    LOGGER.info("message: " + response.getBody());
//		    ShopifyAppService.updateStateSyncedProductVariant(productId, ResourceStates.DELETED);
		    throw new BadRequestException(SystemError.INVALID_REQUEST);
		}
				
		Map responseMap = new JsonObject(response.getBody()).getMap();
		Map productMap = ParamUtil.getMapData(responseMap, "product");	
		String productStoreId = ParamUtil.getString(productMap, AppParams.ID);
				
		ShopifyProductPullObj productPullObj = new Gson().fromJson(response.getBody().toString(), ShopifyProductPullObj.class);		
		ShopifyProductObj productObj = productPullObj.getProduct();
		
		String bodyHtml = productObj.getBodyHtml();
		String bodyHtmlEncode = StringUtil.urlEncode(bodyHtml);
		
		ShopifyAppService.insertFetchedProduct(storeId, productObj, storeName, ResourceStates.PROCESSING, bodyHtmlEncode);
		
		firstVariantUpdateInfo(productMap, syncProduct, mappingVariantImageMap, consumerKey, domain);
		
		productInfo.put("product_store_id", productStoreId);		
		productInfo.put("all_images_url", allImagesUrlList);
		return productInfo;
	}
	
	private static void firstVariantUpdateInfo(Map productMap, Map syncProduct, Map mappingVariantImageMap, String consumerKey, String domain) 
			throws UnirestException, SQLException, InterruptedException {
		
		LOGGER.info("Update data for the first variant...");
		String productStoreId = ParamUtil.getString(productMap, AppParams.ID);
		
		Long productRefId = Long.parseLong(productStoreId);
		String collectionIds = ParamUtil.getString(syncProduct, AppParams.COLLECTION_ID);
		if (collectionIds != null && collectionIds.isEmpty() == false) {
			List<String> collectionIdList = Arrays.asList(collectionIds.split(","));
			
			for (String collectionId : collectionIdList) {
				updateCollection(productRefId, collectionId, consumerKey, domain);
			}
		}
		
		String campaignId = ParamUtil.getString(syncProduct, AppParams.CAMPAIGN_ID);
		ShopifyAppService.updateShopifyProduct(productRefId, campaignId, collectionIds);
		
		/**
		 * Update image id for first variant
		 */	
		ShopifyProductObj productObj = updateVariantV2(productStoreId, mappingVariantImageMap, consumerKey, domain, null);
		
		List<ShopifyVariantObj> vObjList = productObj.getVariants();
		for (ShopifyVariantObj vObj : vObjList) {
			ShopifyAppService.insertProductVariant(vObj, 1);
			ShopifyAppService.updateRefIdToSyncedVariant(vObj);
		}
		
		List<ShopifyImageObj> imgObjList = productObj.getImages();
		for (ShopifyImageObj imgObj : imgObjList) {
			LOGGER.info("position: " + imgObj.getPosition());
			List<String> variantIdList = imgObj.getVariantIds().stream().map(l -> String.valueOf(l)).collect(Collectors.toList());
			String variantIds = String.join(",", variantIdList);
			ShopifyAppService.insertProductImage(imgObj, variantIds);
		}
	}

	private static Map pushVariantsToProductShopify(Map syncProduct, String productStoreId, String consumerKey, String domain) 
			throws UnirestException, SQLException {
		
		LOGGER.info("Push other variants to Product in Shopify Store...");
		
		String postUrl = String.format(ShopifyAPIEndpoints.PRODUCT_VARIANT_USING_TOKEN, domain, productStoreId);
		
		List<Map> variantList = ParamUtil.getListData(syncProduct, AppParams.VARIANTS);
		List<Map> variants = new ArrayList<>();
		variantList.remove(0);
		
		for (Map variantMap : variantList) {
			
			String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
			String productName = ParamUtil.getString(variantMap, AppParams.PRODUCT_NAME);
			String variantId = ParamUtil.getString(variantMap, AppParams.VARIANT_ID);
			String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
			String sizeId = ParamUtil.getString(variantMap, AppParams.SIZE_ID);
			String sizeName = ParamUtil.getString(variantMap, AppParams.SIZE_NAME);
			String price = ParamUtil.getString(variantMap, AppParams.PRICE);
			double compare_at_price = Double.parseDouble(price) * 120 / 100;
		
			Map variant =  new LinkedHashMap<>();		
			variant.put("option1", productName);
			variant.put("option2", colorName);
			variant.put("option3", sizeName);
	
			variant.put("price", price);
			variant.put("compare_at_price", compare_at_price);
			variant.put("sku", variantId + "|" + sizeId );
			
			variant.put(AppParams.INVENTORY_MANAGEMENT, null);
//			variant.put(AppParams.FULFILLMENT_SERVICE, "BurgerPrints");
			variant.put(AppParams.INVENTORY_POLICY, AppParams.DENY);
			
			Map variantPush = new LinkedHashMap<>();		
			variantPush.put("variant", variant);
			
			String requestBody = new JsonObject(variantPush).encode();		
			
			HttpResponse<String> postResponse = Unirest.post(postUrl).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
					.body(requestBody)
					.asString();
			
			LOGGER.info("push variant to shopify store response-status: " + postResponse.getStatus());
			if (postResponse.getStatus() != 200 && postResponse.getStatus() != 201) {
			    LOGGER.info("data result text: " + postResponse.getStatusText());
			    LOGGER.info("message: " + postResponse.getBody());
//			    ShopifyAppService.updateStateSyncedProductVariant(variantSyncId, ResourceStates.DELETED);
//			    throw new BadRequestException(SystemError.INVALID_REQUEST);
			} else {
				ShopifyProductObj productObj = null;
				productObj = new Gson().fromJson(postResponse.getBody().toString(), ShopifyProductObj.class);			
				ShopifyVariantObj variantObj = productObj.getVariant();
				LOGGER.info("Shopify Store VariantId: " + variantObj.getId());
			}
		}
		
		String getUrl = String.format(ShopifyAPIEndpoints.PRODUCT_USING_TOKEN, domain, productStoreId);
		
		HttpResponse<String> getResponse = Unirest.get(getUrl).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", FIELDS)
                .asString();
		
		Map responseMap = new JsonObject(getResponse.getBody()).getMap();
		Map productMap = ParamUtil.getMapData(responseMap, "product");
		
		return productMap;
	}
	
	private static ShopifyProductObj updateVariantV2(String shopifyProductRefId, Map mappingVariantImageMap, String consumerKey, 
			String domain, List<String> defaultImgUrl) 
			throws UnirestException, InterruptedException {
		
		LOGGER.info("updateVariant... ");
		String url = String.format(ShopifyAPIEndpoints.PRODUCTS_ONE_USING_TOKEN, domain, shopifyProductRefId);
		
		HttpResponse<String> getResponse = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", FIELDS)
                .asString();
		
		Map responseMap = new JsonObject(getResponse.getBody()).getMap();
		Map productMap = ParamUtil.getMapData(responseMap, "product");
		List<Map> variants = ParamUtil.getListData(productMap, "variants");
		List<Map> images = ParamUtil.getListData(productMap, "images");
		List<Map> mappedVariantImgList = new ArrayList<>();
		for (Map variant : variants) {
			
			String variantId = ParamUtil.getString(variant, AppParams.ID);
			String productName = ParamUtil.getString(variant, "option1");
			String colorName = ParamUtil.getString(variant, "option2");
			
			String imgPrefix = productName + " / " + colorName;
			String imgUrl = (String) mappingVariantImageMap.get(imgPrefix);
			
			if (StringUtils.isNotEmpty(imgUrl)) {
				Map mappedVariantImg = new HashedMap();
				mappedVariantImg.put("variant_id", variantId);
				mappedVariantImg.put("img_url", imgUrl);
				mappedVariantImgList.add(mappedVariantImg);
			}
		}
		LOGGER.info("mappedVariantImgList: " + mappedVariantImgList.toString());
		
		List<String> imgDefaultIds = new ArrayList<>();
		for (Map i : images) {
			String imageSource = ParamUtil.getString(i, "src");
			LOGGER.info("imageSource= " + imageSource);
			String imageId = ParamUtil.getString(i, AppParams.ID);
			
			Map imageMap = new LinkedHashMap<>();
			imageMap.put("id", imageId);
			List<String> variantIdList = new ArrayList<>();
			
			for (Map m : mappedVariantImgList) {
				String imgUrl = ParamUtil.getString(m, "img_url");
				String variantId = ParamUtil.getString(m, "variant_id");
				
				if (imageSource.contains(imgUrl)) {
					variantIdList.add(variantId);
				}
			}
			
			imageMap.put("variant_ids", variantIdList);
			
			// push
			if (CollectionUtils.isNotEmpty(variantIdList)) {
				LOGGER.info("imageMap to push: " + imageMap.toString());
				updateVariantImageV2(shopifyProductRefId, imageId, imageMap, consumerKey, domain);
			}
			
			if (CollectionUtils.isNotEmpty(defaultImgUrl)) {
				LOGGER.info("Default Image Url List: " + defaultImgUrl);
				for (String imgUrlDefault : defaultImgUrl) {
					LOGGER.info("imgUrlDefault: " + imgUrlDefault);
					if (imageSource.contains(imgUrlDefault)) {
						imgDefaultIds.add(imageId);
					}
				}
				LOGGER.info("Default Image Ids: " + imgDefaultIds);
			}
		}
		
		if (CollectionUtils.isNotEmpty(imgDefaultIds)) {
			for (int i = 0; i < imgDefaultIds.size(); i++) {
				String imgId = imgDefaultIds.get(i);
				updatePositionImage(shopifyProductRefId, imgId, i+1, consumerKey, domain);
			}
		}
		
		HttpResponse<String> getResponse2 = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.queryString("fields", FIELDS)
                .asString();
//		LOGGER.info("get product response-status:" + getResponse.getStatus());
//		LOGGER.info("data result text:" + getResponse.getStatusText());
		ShopifyProductPullObj productPullObj = new ShopifyProductPullObj();
		ShopifyProductObj productObj = new ShopifyProductObj();
		productPullObj = new Gson().fromJson(getResponse2.getBody().toString(), ShopifyProductPullObj.class);		
		
		productObj = productPullObj.getProduct();
		
		return productObj;	
	}
	
	private static void updateVariantImageV2(String shopifyProductRefId, String imageId, Map imageMap, String consumerKey,
			String domain) throws InterruptedException {
		
		String url = String.format(ShopifyAPIEndpoints.PRODUCT_ONE_IMAGE_USING_TOKEN, domain, shopifyProductRefId, imageId);
		
		LOGGER.info("updateImageIdForVariant - imageId: " + imageId);
		
		Map requestMap = new LinkedHashMap<>();
		requestMap.put("image", imageMap);
		
		String requestBody = new JsonObject(requestMap).encode();
		
		try {

			HttpResponse<String> response = Unirest.put(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
					.body(requestBody).asString();
			
			LOGGER.info("variant response-status: " + response.getStatus());
			LOGGER.info("data result text:" + response.getStatusText());
//			Map responseMap = new JsonObject(response.getBody()).getMap();
//			Map productMap = ParamUtil.getMapData(responseMap, "product");
			
			Thread.sleep(500);
			
		} catch (UnirestException e) {
			LOGGER.severe(e.toString());
		}
	}

	private void updateAllImages(String productId, List<Map> all_images_url, String consumerKey, String domain) 
			throws InterruptedException, UnirestException {
		
		if (all_images_url == null || all_images_url.isEmpty()) {
			LOGGER.info("all_images_url: Null");
			
		} else {
			LOGGER.info("all_images_url: Updating...");
			LOGGER.info("all_images_url: " + all_images_url.toString());
			
			String url = String.format(ShopifyAPIEndpoints.PRODUCTS_ONE_USING_TOKEN, domain, productId);
			
			HttpResponse<String> getResponse = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
					.queryString("fields", FIELDS)
	                .asString();
			
			Map responseMap = new JsonObject(getResponse.getBody()).getMap();
			Map productMap = ParamUtil.getMapData(responseMap, "product");
			
			
			List<Map> images = ParamUtil.getListData(productMap, "images");
			Set<Map> all_images_push = new LinkedHashSet<Map>();
			
			for (Map i : images) {
				Map<String, String> imageIdMap = new HashMap<>();
				String imageId = ParamUtil.getString(i, AppParams.ID);
				imageIdMap.put("id", imageId);
				all_images_push.add(imageIdMap);
			}		
			all_images_push.addAll(all_images_url);
			LOGGER.info("all_images_push: " + all_images_push.toString());
			
			Map updateAllImage = new LinkedHashMap<>();
			updateAllImage.put("id", productId);	
			updateAllImage.put("images", all_images_push);
			
			Map requestMap = new LinkedHashMap<>();
			requestMap.put("product", updateAllImage);
			
			String requestBody = new JsonObject(requestMap).encode();
			HttpResponse<String> response = Unirest.put(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
					.body(requestBody).asString();
			
			LOGGER.info("update all images response-status: " + response.getStatus());
			LOGGER.info("data result text:" + response.getStatusText());
			
		}
	}
	
	private static void updatePositionImage(String productStoreId, String imgId, int position, String consumerKey, String domain) 
			throws UnirestException {
		
		LOGGER.info("Update Position for ImageId:" + imgId);
		
		Map imageUpdate = new HashMap<>();
		Map imgIdAndPosition = new HashMap<>();
		imgIdAndPosition.put("id", imgId);
		imgIdAndPosition.put("position", position);
		imageUpdate.put("image", imgIdAndPosition);
		
		String requestBody = new JsonObject(imageUpdate).encode();
		
		String url = String.format(ShopifyAPIEndpoints.PRODUCT_ONE_IMAGE_USING_TOKEN, domain, productStoreId, imgId);
		
		HttpResponse<String> response = Unirest.put(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.body(requestBody).asString();
		if (response.getStatus() != 200 && response.getStatus() != 201) {
		    LOGGER.info("update position for imageId response-status: " + response.getStatus());
		    LOGGER.info("data result text:" + response.getStatusText());
		    LOGGER.info("message: " + response.getBody());
		    throw new BadRequestException(SystemError.INVALID_REQUEST);
		}
	}
	
	private static void updateCollection(Long productRefId, String collectionId, String consumerKey, String domain) throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.COLLECT_USING_TOKEN, domain);
		
		String productStoreId = Long.toString(productRefId);
		
		Map collect = new LinkedHashMap<>();
		collect.put("product_id", productStoreId);
		collect.put("collection_id", collectionId);
		
		Map requestMap = new HashMap<>();
		requestMap.put("collect", collect);
		
		String requestBody = new JsonObject(requestMap).encode();
		HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.body(requestBody).asString();
		
//		LOGGER.info("update collection response-status: " + response.getStatus());
//		LOGGER.info("data result text:" + response.getStatusText());
	}
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyAddProductHandler.class.getName());

}