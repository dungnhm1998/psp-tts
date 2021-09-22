package asia.leadsgen.psp.server.handler.shopify_app;

import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.mashape.unirest.http.Headers;
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
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreCampService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.RedisService;
import asia.leadsgen.psp.service_fulfill.ShopifyAppService;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyFetchProductHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			
			String storeId = routingContext.request().getParam(AppParams.STORE_ID);
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
			String userId = ParamUtil.getString(storeMap, AppParams.USER_ID);
			LOGGER.info("userId= " + userId);
			
			String source = routingContext.request().getParam(AppParams.SOURCE);
			LOGGER.info("source= " + source);
			if (!source.equalsIgnoreCase("burgerprints")) {
				String requestUserId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				LOGGER.info("requestUserId= " + requestUserId);
				
				if (StringUtils.isEmpty(requestUserId)) {
					throw new LoginException(SystemError.LOGIN_REQUIRED);
				}
				if (!requestUserId.equalsIgnoreCase(userId)) {
					throw new LoginException(SystemError.INVALID_USER);
				}
			}
			
			try {
				
                String storeName = ParamUtil.getString(storeMap, AppParams.NAME);
            	String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
				String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
				String currency = ParamUtil.getString(storeMap, AppParams.CURRENCY);			
				
				final String key = userId + "_" + storeId + "_" + RedisKeyEnum.TASK_PROCESS_FETCH_PRODUCT_SHOPIFY.getValue();
				Map task = RedisService.get(key);
				
				if (task != null && !task.isEmpty()) {
					
					Map mapResult = new LinkedHashMap();
					mapResult.put(AppParams.RESULT_MSG, "Fetching product is in progress, you must wait for this job to finish.");
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.SEE_OTHER.code());
					routingContext.put(AppParams.RESPONSE_MSG, "Fetching product is in progress, you must wait for this job to finish.");
					routingContext.put(AppParams.RESPONSE_DATA, mapResult);
					
				} else {
					
					int total_product = countTotalProduct(consumerKey, domain);
					LOGGER.info("count product: " + total_product);
					
					int limitedProduct = 0;
					
					if (source.equalsIgnoreCase("burgerprints")) {
						limitedProduct = 20;
					} else {
						limitedProduct = 100;
					}
					
					final int product_per_page = limitedProduct;
					if (total_product > limitedProduct) {
										
						Map map = new LinkedHashMap<Integer, String>();
						map.put(AppParams.START_TIME, new Date());
						map.put(AppParams.STORE_ID, storeId);
						
						LOGGER.info("save key: " + key);
						RedisService.save(key, map);
						Thread one = new Thread() {
							public void run() {
								try {							
									
									int page = 1;
									int total_page = (total_product / product_per_page) + 1;
									LOGGER.info("total_page=" + total_page);
									
									int total_product_success = 0;
									String page_info = "";
									List<String> link = new ArrayList<String>();
									
									while (page <= total_page) {
										if (page == 1) {
											
											LOGGER.info("page=" + page);
											HttpResponse<String> response = getData(consumerKey, domain, product_per_page);

											Headers header = response.getHeaders();	
											LOGGER.info("Headers.... " + header.toString());
											link = header.get("Link");
											LOGGER.info("Link.... " + link.toString());
											Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		
											String[] pairs = link.get(0).split("&");
											LOGGER.info("pairs...." + pairs.toString());
											for (String pair : pairs) {
										        int idx = pair.indexOf("=");
										        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
										    }
											
											String page_info_next = query_pairs.get("page_info");
											LOGGER.info("page_info_next: " + page_info_next);
											page_info = page_info_next.substring(0, page_info_next.lastIndexOf(">"));
											
											ShopifyProductPullObj productPullObj = null;		
											productPullObj = new Gson().fromJson(response.getBody().toString(), ShopifyProductPullObj.class);		
											
											int product_fetch_success_per_page = processData(productPullObj, storeId, storeName, currency, source);
											LOGGER.info("product_fetch_success_per_page= " + product_fetch_success_per_page);
											total_product_success += product_fetch_success_per_page;
											page++;
											
										} else {
											
											LOGGER.info("page=" + page);
											HttpResponse<String> response = getData(consumerKey, domain, page_info, product_per_page);
											
											Headers header = response.getHeaders();	
											link = header.get("Link");
											
											Map<String, String> query_pairs = new LinkedHashMap<String, String>();
											String[] pairs = link.get(0).split("&");
											for (String pair : pairs) {
										        int idx = pair.indexOf("=");
										        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
										    }

											String page_info_next = query_pairs.get("page_info");
											page_info = page_info_next.substring(0, page_info_next.lastIndexOf(">"));
											
											ShopifyProductPullObj productPullObj = null;		
											productPullObj = new Gson().fromJson(response.getBody().toString(), ShopifyProductPullObj.class);
											
											int product_fetch_success_per_page = processData(productPullObj, storeId, storeName, currency, source);
											LOGGER.info("product_fetch_success_per_page= " + product_fetch_success_per_page);
											total_product_success += product_fetch_success_per_page;
											page++;
										
										}
									}
									
									LOGGER.info("total_product_success= " + total_product_success);
									
									LOGGER.info("delete key: " + key);
									RedisService.delete(key);
									
								} catch (UnirestException e) {								
									e.printStackTrace();
									if (source.equalsIgnoreCase("burgerprints")) {
										try {
											DropShipStoreCampService.updateStoreSyncedByStoreId(storeId, 2);
										} catch (SQLException e1) {
											e1.printStackTrace();
										}					
									}
									LOGGER.info("delete key: " + key);
									RedisService.delete(key);
								} catch (Exception e) {
									e.printStackTrace();
									if (source.equalsIgnoreCase("burgerprints")) {
										try {
											DropShipStoreCampService.updateStoreSyncedByStoreId(storeId, 2);
										} catch (SQLException e1) {
											e1.printStackTrace();
										}					
									}
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
						
					} else {
						
						try {
							
							HttpResponse<String> response = getData(consumerKey, domain, product_per_page);
							
							ShopifyProductPullObj productPullObj = null;		
							productPullObj = new Gson().fromJson(response.getBody().toString(), ShopifyProductPullObj.class);
							
							int product_fetch_success = processData(productPullObj, storeId, storeName, currency, source);
							LOGGER.info("product_fetch_success= " + product_fetch_success);
							String msg = "TOTAL PRODUCT PULL SUCCESSFULLY: " + product_fetch_success;
							
							Map productSearchResult = ShopifyAppService.searchProduct(storeId, "", "", 1, 10);
							productSearchResult.put("msg", msg);
							
							routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
							routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
							routingContext.put(AppParams.RESPONSE_DATA, productSearchResult);
							
						} catch (UnirestException e) {
							e.printStackTrace();
							if (source.equalsIgnoreCase("burgerprints")) {
								try {
									DropShipStoreCampService.updateStoreSyncedByStoreId(storeId, 2);
								} catch (SQLException e1) {
									e1.printStackTrace();
								}					
							}
						} catch (Exception e) {
							e.printStackTrace();
							if (source.equalsIgnoreCase("burgerprints")) {
								try {
									DropShipStoreCampService.updateStoreSyncedByStoreId(storeId, 2);
								} catch (SQLException e1) {
									e1.printStackTrace();
								}					
							}
						}
					}
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

	private static int countTotalProduct(String consumerKey, String domain) throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.COUNT_TOTAL_PRODUCT, domain);
		
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
				.asString();
		
		Map mapResult = new JsonObject(response.getBody()).getMap();
		int count = ParamUtil.getInt(mapResult, "count");
		
		return count;
	}
	
	private HttpResponse<String> getData(String consumerKey, String domain, int product_per_page) throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.FETCH_PRODUCTS_USING_TOKEN, domain);
		
		long start = System.currentTimeMillis();
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
                .queryString("limit", product_per_page)
                .asString();
		
		long elapsedTimeMillis = System.currentTimeMillis() - start;
		float elapsedTimeSec = elapsedTimeMillis / 1000F;

		LOGGER.info("Execution Time: " + elapsedTimeSec);
		LOGGER.info("data result code: " + response.getStatus());
		if (response.getStatus() != 200 && response.getStatus() != 201) {    
		    throw new BadRequestException(SystemError.INVALID_REQUEST);
		}
		
		return response;
	}
	
	private HttpResponse<String> getData(String consumerKey, String domain, String page_info, int product_per_page)
			throws UnirestException {
		
		String url = String.format(ShopifyAPIEndpoints.FETCH_PRODUCTS_USING_TOKEN, domain);
		
		long start = System.currentTimeMillis();
		HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", consumerKey)
                .queryString("limit", product_per_page)
                .queryString("page_info", page_info)
                .asString();
		
		long elapsedTimeMillis = System.currentTimeMillis() - start;
		float elapsedTimeSec = elapsedTimeMillis / 1000F;

		LOGGER.info("Execution Time: " + elapsedTimeSec);
		LOGGER.info("data result code: " + response.getStatus());
		if (response.getStatus() != 200 && response.getStatus() != 201) {
		    throw new BadRequestException(SystemError.INVALID_REQUEST);
		}
		
		return response;
	}
	
	private int processData(ShopifyProductPullObj productPullObj, String storeId, String storeName, String currency, String source) 
			throws SQLException, InterruptedException, ParseException {
		
		int product_fetch_success = 0;
//		LOGGER.info("productPullObj= " + productPullObj.toString());		
		List<ShopifyProductObj> productList = productPullObj.getProducts();
//		LOGGER.info("productList= " + productList.toString());
		for (ShopifyProductObj pObj : productList) {
//			LOGGER.info("ShopifyProductObj= " + pObj.toString());				
			Long productRefId = pObj.getId();
			LOGGER.info("Fetch Shopify Product ID: " + productRefId);
			if(ShopifyAppService.isExistProduct(productRefId)) {
				LOGGER.info("Product: " + productRefId + "is exist!");
				continue;
			}			
			
			List<ShopifyVariantObj> variantList = pObj.getVariants();			
			int isInsertedVariant = 0;
			String campaignIdSynced = "";
			for (ShopifyVariantObj vObj : variantList) {
				LOGGER.info("Fetch Shopify Variant ID: " + vObj.getId());
				
				if(StringUtils.isEmpty(vObj.getSku())) {
					LOGGER.info("ShopifyVariantObj: Sku is Null!");
					if (source.equalsIgnoreCase("burgerprints")) {
						continue;
					}
				} else {
					String sku = vObj.getSku();
					String[] skuArr = sku.split("\\|");
					if (skuArr.length >= 2) {
						LOGGER.info("ShopifyVariantObj: Sku= " + sku);
						String variantId = skuArr[0];
						Map variantMap = ProductVariantService.get(variantId);
						if (variantMap != null && variantMap.isEmpty() == false) {
							LOGGER.info("Product Variant is exist in BurgerPrints. VariantId= " + variantId);
							String campaignId = ParamUtil.getString(variantMap, AppParams.CAMPAIGN_ID);
							String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);
							LOGGER.info("CampaignId: " + campaignId);
							LOGGER.info("productId: " + productId);
							String campaignState = CampaignService.getCampaignState(campaignId);
							if (StringUtils.isEmpty(campaignState) || ResourceStates.LOCKED.equalsIgnoreCase(campaignState)) {
								LOGGER.info("This campaign is Locked: " + campaignId);
								
							} else {
								LOGGER.info("Sync Variant: " + variantId + " to Shopify-App!");
								String sizeId = skuArr[1];
								LOGGER.info("sizeId: " + sizeId);
								try {
									campaignIdSynced = syncProductVariant(variantMap, sizeId, vObj, currency);
								} catch (Exception e){
									LOGGER.info("Sync VariantId: " + variantId + " - Exception!");
									e.printStackTrace();
									break;
								}
							}
						} else {
							if (source.equalsIgnoreCase("burgerprints")) {
								continue;
							}
						}
					} else {
						if (source.equalsIgnoreCase("burgerprints")) {
							continue;
						}
					}
				}

				ShopifyAppService.insertProductVariant(vObj, 0);
				if (campaignIdSynced != null && campaignIdSynced.isEmpty() == false) {
					ShopifyAppService.updateNSyncedProductVariant(pObj.getId(), vObj.getId());
				}
				
				isInsertedVariant++;
			}
			
			LOGGER.info("Shopify ProductId: " + productRefId + " --- Insert Variant: " + isInsertedVariant + " variants!");
			if (isInsertedVariant == 0) {
				continue;
			}
			
			List<ShopifyOptionObj> optionList = pObj.getOptions();		
			for (ShopifyOptionObj opObj : optionList) {
				LOGGER.info("ShopifyOptionObj: " + opObj.toString());
				ShopifyAppService.insertProductOption(opObj);
			}
								
			List<ShopifyImageObj> imageList = pObj.getImages();
			for (ShopifyImageObj imgObj : imageList) {
				LOGGER.info("ShopifyImageObj: " + imgObj.toString());
				List<String> variantIdList = imgObj.getVariantIds().stream().map(l -> String.valueOf(l)).collect(Collectors.toList());
				String variantIds = String.join(",", variantIdList);
				ShopifyAppService.insertProductImage(imgObj, variantIds);
			}
			
			LOGGER.info("ProductObj: " + pObj.toString());
			String bodyHtml = pObj.getBodyHtml();
			String bodyHtmlEncode = StringUtil.urlEncode(bodyHtml);
			
			LOGGER.info("bodyHtmlEncode length: " + bodyHtmlEncode.length());
			if (bodyHtmlEncode.length() > 4000) {			
				int indexOfImgSrc = bodyHtml.indexOf("<img src=");
				if (indexOfImgSrc != -1) {
					int lastIndexOfImgSrc = bodyHtml.indexOf("\">", indexOfImgSrc);					
					bodyHtml = bodyHtml.substring(0, indexOfImgSrc) + bodyHtml.substring(lastIndexOfImgSrc + 2, bodyHtml.length());
					bodyHtmlEncode = StringUtil.urlEncode(bodyHtml);
					if (bodyHtmlEncode.length() > 4000) {
						bodyHtmlEncode = bodyHtmlEncode.substring(0, 3999);
					}
				} else {
					bodyHtmlEncode = bodyHtmlEncode.substring(0, 3999);
				}
				LOGGER.info("bodyHtmlEncode length substring: " + bodyHtmlEncode);
			}
			
			Map result = ShopifyAppService.insertFetchedProduct(storeId, pObj, storeName, ResourceStates.APPROVED, bodyHtmlEncode);
			if (result != null && result.isEmpty() == false) {
				LOGGER.info("insertFetchedProduct: " + result.toString());
				product_fetch_success++;
				if (campaignIdSynced != null && campaignIdSynced.isEmpty() == false) {
					ShopifyAppService.updateShopifyProduct(productRefId, campaignIdSynced, "");
					if (source.equalsIgnoreCase("burgerprints")) {
						DropShipStoreCampService.updateStoreSyncedByStoreId(storeId, 1);					
					}
				}
			}
			Thread.sleep(1000);
		}
		
		return product_fetch_success;
	}
	
	/**
	 * Insert synced Variant to tb_shopify_synced_product_variant
	 * @param syncVariant
	 * @param productId
	 * @throws SQLException
	 * @throws ParseException 
	 */
	private String syncProductVariant(Map syncVariant, String sizeId, ShopifyVariantObj vObj, String currency) throws SQLException, ParseException {
		LOGGER.info("syncVariant: " + syncVariant.toString());
		String campaignId = ParamUtil.getString(syncVariant, AppParams.CAMPAIGN_ID);
		
		String productId = ParamUtil.getString(syncVariant, AppParams.PRODUCT_ID);
		
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
				if ((ParamUtil.getString(design, AppParams.TYPE)).equalsIgnoreCase("full")) {
					designFrontUrl = ParamUtil.getString(productImage, AppParams.URL);
				}
			}
		}
		
		String variantId = ParamUtil.getString(syncVariant, AppParams.ID);
		String baseId = ParamUtil.getString(syncVariant, AppParams.BASE_ID);
		
		Map<String, String> baseSizeMap = BaseService.getBaseSizeMap();
		String sizeName = baseSizeMap.get(sizeId);
		
		String colorId = ParamUtil.getString(syncVariant, AppParams.COLOR_ID);
		String colorName = ParamUtil.getString(syncVariant, AppParams.COLOR_NAME);
		String colorValue = ParamUtil.getString(syncVariant, AppParams.COLOR);
		
		Map imageMap = ParamUtil.getMapData(syncVariant, AppParams.IMAGE);
					
		String mockup_front_url = ParamUtil.getString(imageMap, AppParams.FRONT);
		if (mockup_front_url == null || mockup_front_url.isEmpty()) {
			LOGGER.info("mockup_front_url: " + mockup_front_url);
			throw new BadRequestException(SystemError.INVALID_MOCKUP_IMAGE);
		}
		String mockup_back_url = ParamUtil.getString(imageMap, AppParams.BACK);
		
		Long imageId = vObj.getImageId();
		String salePrice = vObj.getPrice();
		Long productRefId = vObj.getProductId();
		Long variantRefId = vObj.getId();
		
		int saleExpected = Integer.parseInt(ParamUtil.getString(campaignProduct, AppParams.SALE_EXPECTED));
		
		String sku = "";
		sku = ShopifyAppService.checkSku(baseId, sizeId, colorName);
		LOGGER.info("sku: " + sku);
		
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
		syncedProductObj.setSku(sku);
		syncedProductObj.setColorName(colorName);
		syncedProductObj.setSizeName(sizeName);
		syncedProductObj.setColorValue(colorValue);
		syncedProductObj.setProductRefId(productRefId);
		syncedProductObj.setVariantRefId(variantRefId);
		
		ShopifyAppService.syncProductVariant(syncedProductObj);
		
		return campaignId;
	}
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyFetchProductHandler.class.getName());

}
