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
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import asia.leadsgen.psp.server.handler.campaign_v2.CampaignModel;
import asia.leadsgen.psp.server.handler.campaign_v2.CampaignPayload;
import asia.leadsgen.psp.server.handler.campaign_v2.ColorModel;
import asia.leadsgen.psp.server.handler.campaign_v2.DesignCreateV2;
import asia.leadsgen.psp.server.handler.campaign_v2.DesignModel;
import asia.leadsgen.psp.server.handler.campaign_v2.ProductModel;
import asia.leadsgen.psp.server.handler.campaign_v2.SizeModel;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.SystemException;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.service_fulfill.CampaignCreateServiceV2;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.Base2dClassificationUtil;
import asia.leadsgen.psp.util.BasePhoneCaseUtil;
import asia.leadsgen.psp.util.BaseSizePhoneCaseUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.JSONStringToMapUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopifyCampaignCreateV2Handler implements Handler<RoutingContext> {
	
	private static String ispPrefixTshirt;
	private static String ispPrefixOtherBase;

	public static void setIspPrefixTshirt(String ispPrefixTshirt) {
		ShopifyCampaignCreateV2Handler.ispPrefixTshirt = ispPrefixTshirt;
	}

	public static void setIspPrefixOtherBase(String ispPrefixOtherBase) {
		ShopifyCampaignCreateV2Handler.ispPrefixOtherBase = ispPrefixOtherBase;
	}

	@Override
    public void handle(RoutingContext routingContext) {

        routingContext.vertx().executeBlocking(future -> {

            try {
            	
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
				
				String state = campModel.getState();
				if (state == null ) {
					throw new BadRequestException(SystemError.INVALID_REQUEST);
				} else {
					if (!ResourceStates.SHOPIFY_APP.equalsIgnoreCase(state)) {
						throw new BadRequestException(SystemError.INVALID_REQUEST);
					}
				}
							
				String stores = campModel.getStores();
				if (stores == null) {
					stores = "";
				}
				String currency = "";
				String storeId = campaignPayload.getStoreId();
				if (!StringUtils.isEmpty(storeId)) {
					Map storeMap = DropShipStoreService.lookUp(storeId);
	                LOGGER.info("storeMap= " + storeMap.toString());
	                currency = ParamUtil.getString(storeMap, AppParams.CURRENCY);
				}
				
				Map createCampaign = CampaignCreateServiceV2.insertCampaign(campModel, "7.0");
				LOGGER.info("createCampaign: " + createCampaign.toString());
				
				String campaignId = ParamUtil.getString(createCampaign, AppParams.CAMPAIGN_ID);
				List<ProductModel> productList = campaignPayload.getProducts();
				List<String> designTypeList = new ArrayList<>();
				List<Map> createDesignList = new ArrayList<>();
				int isBackview = 0;
				List<ProductModel> productModelList = new ArrayList<>();
				List<Map> baseAndSizesList = new ArrayList<>();
				
				for (ProductModel pModel : productList) {
					
					int position = productList.indexOf(pModel) + 1;
					LOGGER.info("product position= " + position);
					List<SizeModel> sizeModels = pModel.getSizes();
					List<DesignModel> designModels = pModel.getDesigns();
					boolean defaultProduct = pModel.isDefault();
					LOGGER.info("defaultProduct= " + defaultProduct);
					int saleExpected = pModel.getSale_expected();
					
					if (pModel.isBackView() == true) {
						isBackview = 1;
					}
					
					String baseIdRequest = pModel.getBaseId();
					Map baseMap = BaseService.get(baseIdRequest);
					String designType = ParamUtil.getString(baseMap, AppParams.DESIGN_TYPE);
					
					Map baseInfo = new LinkedHashMap<>();
					baseInfo.put(AppParams.ID, ParamUtil.getString(baseMap, AppParams.ID));
					baseInfo.put(AppParams.PRINTABLE, ParamUtil.getMapData(baseMap, AppParams.PRINTABLE));
					LOGGER.info("baseIdRequest: " + baseIdRequest);
					
					if (!designTypeList.contains(designType)) {
						designTypeList.add(designType);
						Map createDesignMap = new HashedMap<>();
						createDesignMap.put("base_id", baseIdRequest);
						createDesignMap.put("design_type", designType);
						createDesignMap.put("base_info", baseInfo);
						createDesignMap.put("design_models", designModels);
						createDesignList.add(createDesignMap);
					}
						
					if (!BasePhoneCaseUtil.isPhoneCase(baseIdRequest)) {
						ProductModel formatPModel = formatProductModel(pModel, position);
						productModelList.add(formatPModel);
						List<Map> sizeList = new ArrayList<>();
						for (SizeModel sModel : sizeModels) {
							
							String sizeId = sModel.getId();

							Map sizeMap = new HashMap<>();
							sizeMap.put("id", sizeId);
							sizeMap.put("sale_price", sModel.getSale_price());
							sizeMap.put("sale_expected", saleExpected);
							sizeList.add(sizeMap);			
						}
						
						Map baseIdAndSizes = new HashedMap<>();
						baseIdAndSizes.put(AppParams.BASE_ID, baseIdRequest);
						baseIdAndSizes.put(AppParams.SIZES, sizeList);
						
						baseAndSizesList.add(baseIdAndSizes);
						
					} else {
						
						Map<String, String> baseSizePhoneCaseMap = BaseSizePhoneCaseUtil.initMap();
						for (SizeModel sModel : sizeModels) {
							LOGGER.info("baseIdRequest: " + baseIdRequest);
							String sizeId = sModel.getId();
							String baseIdPhonecase = baseSizePhoneCaseMap.get(sizeId);
							LOGGER.info("baseIdPhonecase: " + baseIdPhonecase);
							boolean defaultProductPhone = false;
							if (baseIdPhonecase.equalsIgnoreCase(baseIdRequest)) {
								if (defaultProduct = true) {
									defaultProductPhone = true;
								}
							}
							
							LOGGER.info("defaultProductPhonecase: " + defaultProductPhone);
							ProductModel formatPphoneModel = formatPhonecaseProductModel(pModel, position, baseIdPhonecase, sModel, defaultProductPhone);
							
							productModelList.add(formatPphoneModel);
							
							List<Map> sizeList = new ArrayList<>();
							Map sizeMap = new HashMap<>();
							sizeMap.put("id", sizeId);
							sizeMap.put("sale_price", sModel.getSale_price());
							sizeMap.put("sale_expected", saleExpected);
							sizeList.add(sizeMap);
							
							Map baseIdAndSizes = new HashedMap<>();
							baseIdAndSizes.put(AppParams.BASE_ID, baseIdPhonecase);
							baseIdAndSizes.put(AppParams.SIZES, sizeList);
							
							baseAndSizesList.add(baseIdAndSizes);
						}
					}
				}
				
				CampaignCreateServiceV2.insertAllProduct(campaignId, productModelList);

				List<Map> productIdWithBaseIdList = CampaignCreateServiceV2.getCampaignProductIdWithBaseId(campaignId);
				
				List<Map> allProductSizes = new ArrayList<>();
				
				List<Map> allProductAndSizesForSPFApp = new ArrayList<>();
				
				for (Map baseAndSizes : baseAndSizesList) {
					
					String baseId = ParamUtil.getString(baseAndSizes, AppParams.BASE_ID);
					List<Map> sizeList = ParamUtil.getListData(baseAndSizes, AppParams.SIZES);
					Map productIdWithBaseId = productIdWithBaseIdList.stream().filter(m -> (m.get(AppParams.S_BASE_ID)).equals(baseId))
							.findFirst().get();
					String productId = ParamUtil.getString(productIdWithBaseId, AppParams.S_ID);
					
					Map productAndSizesForSPFApp = new HashedMap<>();
					List<Map> sizeListForSPFApp = new ArrayList<>();
					productAndSizesForSPFApp.put(AppParams.ID, productId);
					
					for (Map sizeMap : sizeList) {
						Map insertSize = new HashedMap<>();
						insertSize.put(AppParams.PRODUCT_ID, productId);
						insertSize.put(AppParams.BASE_ID, baseId);
						insertSize.put(AppParams.SALE_EXPECTED, ParamUtil.getInt(sizeMap, AppParams.SALE_EXPECTED));
						insertSize.put(AppParams.ID, ParamUtil.getString(sizeMap, AppParams.ID));
						insertSize.put(AppParams.SALE_PRICE, ParamUtil.getDouble(sizeMap, AppParams.SALE_PRICE));
						allProductSizes.add(insertSize);
						
						Map sizesMapForSPFApp = new HashedMap<>();
						sizesMapForSPFApp.put(AppParams.ID, ParamUtil.getString(sizeMap, AppParams.ID));
						sizesMapForSPFApp.put(AppParams.SALE_PRICE, ParamUtil.getDouble(sizeMap, AppParams.SALE_PRICE));
						sizeListForSPFApp.add(sizesMapForSPFApp);
					}
					
					productAndSizesForSPFApp.put(AppParams.SIZES, sizeListForSPFApp);
					allProductAndSizesForSPFApp.add(productAndSizesForSPFApp);
				}
				CampaignCreateServiceV2.insertAllSize(allProductSizes, currency);
				
				for (Map createDesign : createDesignList) {
					String baseId = ParamUtil.getString(createDesign, AppParams.BASE_ID);
					Map baseInfo = ParamUtil.getMapData(createDesign, "base_info");
					
					Map productIdWithBaseId = productIdWithBaseIdList.stream().filter(m -> (m.get(AppParams.S_BASE_ID)).equals(baseId))
							.findFirst().get();
					String productId = ParamUtil.getString(productIdWithBaseId, AppParams.S_ID);
					List<DesignModel> designModels = ParamUtil.getListData(createDesign, "design_models");
					
					for (DesignModel dModel : designModels) {
						String url = dModel.getUrl();							
						LOGGER.info("url: " + url);
						LOGGER.info("design side: " + (designModels.indexOf(dModel) + 1));
						if (!StringUtils.isEmpty(url)) {
							Map designInfoMap = DesignCreateV2.insert(campaignId, productId, dModel, baseInfo, state, true, designModels.indexOf(dModel) + 1);
							LOGGER.info("productId: " + productId + " insert design ====> designMap: " + designInfoMap.toString());
						}		
					}
				}
				
				Map campaignInfo = CampaignService.getV2(campaignId);
				
				List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);
				double saleprice = 0.0d;
				for (Map productInfoMap : campaignProductList) {

					boolean productDefault = ParamUtil.getBoolean(productInfoMap, AppParams.DEFAULT);

					if (productDefault) {
						List<Map> prices = ParamUtil.getListData(productInfoMap, AppParams.PRICES);
						saleprice = Double.MAX_VALUE;
						for (Map p : prices) {
							double price = ParamUtil.getDouble(p, AppParams.PRICE);
							if (price < saleprice) {
								saleprice = price;
							}
						}
						saleprice = GetterUtil.format(saleprice, 2);
						break;
					}
				}
				
				LOGGER.info("campaignId= " + campaignId + ", saleprice= " + saleprice);
				CampaignCreateServiceV2.updateCampInfo(campaignId, saleprice, isBackview);
				campModel.setId(campaignId);
				
				createCampaignProductVariantsV2(routingContext, campModel, allProductAndSizesForSPFApp);
				
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
	
	private ProductModel formatProductModel(ProductModel productModel, int position) {
		
		List<String> sizeIdList = new ArrayList<String>();
		List<SizeModel> sizeModels = productModel.getSizes();
		
		for (SizeModel sModel : sizeModels) {
			String sizeId = sModel.getId();
			if (!sizeId.isEmpty()) {
				sizeIdList.add(sizeId);
			}		
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
		
		productModel.setPosition(position);
		productModel.setAllSizes(sizes);
		productModel.setAllColors(colors);
		productModel.setDefaultColorId(defaultColorId);
		
		return productModel;
	}
	
	private ProductModel formatPhonecaseProductModel(ProductModel productModel, int position, String baseIdPhonecase, SizeModel sModel, boolean isDefault) {
		
		String sizeId = sModel.getId();
		List<String> colorIdList = new ArrayList<String>();
		List<ColorModel> colorModels = productModel.getColors();
		String defaultColorId = "";
		
		for (ColorModel clModel : colorModels) {
			
			String colorId = clModel.getId();
			LOGGER.info("colorId= " + colorId);
			boolean defaultColor = clModel.isDefault();
			LOGGER.info("defaultColor= " + defaultColor);
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
		LOGGER.info("colors= " + colors);
		
		productModel.setBaseId(baseIdPhonecase);
		productModel.setPosition(position);
		productModel.setAllSizes(sizeId);
		productModel.setAllColors(colors);
		productModel.setDefaultColorId(defaultColorId);
		productModel.setDefault(isDefault);

		return (ProductModel) SerializationUtils.clone(productModel);
	}
    
	private Map filterProductVariant(Map productInfo) {
		
		Map resultMap = new LinkedHashMap();

		String productId = ParamUtil.getString(productInfo, AppParams.ID);

		Map productBase = ParamUtil.getMapData(productInfo, AppParams.BASE);

		List<Map> productColorList = ParamUtil.getListData(productInfo, AppParams.COLORS);

		List<Map> productDesignList = ParamUtil.getListData(productInfo, AppParams.DESIGNS);

		boolean productDefault = ParamUtil.getBoolean(productInfo, AppParams.DEFAULT);

		resultMap.put(AppParams.ID, productId);
		resultMap.put(AppParams.BASE, productBase);
		resultMap.put(AppParams.COLORS, productColorList);
		resultMap.put(AppParams.DESIGNS, productDesignList);
		resultMap.put(AppParams.DEFAULT, productDefault);
		
		return resultMap;
	}

	private void createCampaignProductVariantsV2(RoutingContext routingContext, CampaignModel campModel, List<Map> allProductAndSizesForSPFApp) 
    		throws SQLException, UnsupportedEncodingException, ParseException {
		
		String campaignId = campModel.getId();
    	String userId = campModel.getUserId();
    	LOGGER.info("campaignId= " + campaignId);
    	Map campaignInfo = CampaignService.getV2(campaignId);
    	
    	List<Map> shirts = new ArrayList<>();
		List<Map> mugs = new ArrayList<>();
		List<Map> posters = new ArrayList<>();
		List<Map> phonecases = new ArrayList<>();
		List<Map> canvas = new ArrayList<>();
		List<Map> normal3d = new ArrayList<>();
		List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);

		for (Map campaignProduct : campaignProductList) {
			Map ispProduct = filterProductVariant(campaignProduct);
			
			Map base = ParamUtil.getMapData(ispProduct, AppParams.BASE);
			String baseId = ParamUtil.getString(base, AppParams.ID);
			if(StringUtils.isNotEmpty(baseId) && StringUtils.isNotEmpty(Base2dClassificationUtil.classify(baseId))) {
				checkGroup(ispProduct, shirts, mugs, posters, phonecases, canvas, normal3d);
			}
		}
		
		List<List<Map>> phoneCaseSubLists = new ArrayList<>();
		List<List<Map>> tShirtSubLists = new ArrayList<>();
		List<Map> phoneCaseMainDesign = new ArrayList<>();
		List<Map> phoneCaseMainBase = new ArrayList<>();
		List<Map> shirtMainDesign = new ArrayList<>();
		List<Map> shirtMainBase = new ArrayList<>();
		
		if (phonecases.size() > 0) {
			phoneCaseSubLists = ListUtils.partition(phonecases, 6);
			for (Map phonecase : phonecases) {
				List<Map> designs = ParamUtil.getListData(phonecase, AppParams.DESIGNS);
				Boolean isMainDesign = false;
				for (Map design : designs) {
					isMainDesign = ParamUtil.getBoolean(design, AppParams.MAIN);
					if (isMainDesign) {
						phoneCaseMainDesign = designs;
						break;
					}
				}
				if (isMainDesign) {
					Map base = ParamUtil.getMapData(phonecase, AppParams.BASE);
					phoneCaseMainBase.add(base);
					break;
				}
			}
		}
		
		if (shirts.size() > 0) {
			tShirtSubLists = ListUtils.partition(shirts, 2);
			findMainDesign(shirts, AppParams.FRONT, shirtMainDesign);
			findMainDesign(shirts, AppParams.BACK, shirtMainDesign);
		}
		
		List<CompletableFuture<List<Map>>> allFutures = new ArrayList<>();
		CompletableFuture<List<Map>> mugFuture = execute(userId, campaignId, mugs, ispPrefixOtherBase + "mugs");
		CompletableFuture<List<Map>> posterFuture = execute(userId, campaignId, posters, ispPrefixOtherBase + "poster");
		CompletableFuture<List<Map>> canvasFuture = execute(userId, campaignId, canvas, ispPrefixOtherBase + "canvas");
		CompletableFuture<List<Map>> normal3dFuture = execute(userId, campaignId, normal3d, ispPrefixOtherBase + "3d-normal");
		
		if (phoneCaseSubLists.size() > 0) {
			for (List<Map> subList : phoneCaseSubLists) {
				CompletableFuture<List<Map>> phoneCaseFuture = executeV2(userId, campaignId, subList, ispPrefixOtherBase + "phonecase",
																						phoneCaseMainDesign, phoneCaseMainBase);
				allFutures.add(phoneCaseFuture);
			}
		}
		
		if (tShirtSubLists.size() > 0) {
			for (List<Map> subList : tShirtSubLists) {
				CompletableFuture<List<Map>> shirtFuture = executeV2(userId, campaignId, subList,
						ispPrefixTshirt + "2d-tshirt", shirtMainDesign, shirtMainBase);
				allFutures.add(shirtFuture);
			}
		}
		
		allFutures.add(mugFuture);
		allFutures.add(posterFuture);
		allFutures.add(canvasFuture);
		allFutures.add(normal3dFuture);
		
		CompletableFuture<Void> futures = CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[allFutures.size()]));
		CompletableFuture<List<List<Map>>> allResultOfFutures = futures.thenApply(v -> {
			return allFutures.stream().map(f -> f.join()).collect(Collectors.toList());
		});
		List<List<Map>> results = allResultOfFutures.join();
		List<Map> finalResult = new ArrayList<>();
		for (List<Map> list : results) {
			finalResult.addAll(list);
		}

		LOGGER.info("finalResult size : " + finalResult.size());
		
		if (!finalResult.isEmpty()) {
			
			CampaignCreateServiceV2.insertAllVariant(finalResult);
			
			CampaignService.updateDefaultImages(campaignId);
			
			String state = campModel.getState();
			
			campaignInfo = CampaignService.getV2(campaignId);			
				
			String collections = campModel.getCollections();
			String tags = campModel.getTags();
			
			Map result = new LinkedHashMap<>();
			result.put(AppParams.CAMPAIGN_ID, campaignId);
			result.put(AppParams.TITLE, ParamUtil.getString(campaignInfo, AppParams.TITLE));
			result.put(AppParams.DESIGN_FRONT_URL, ParamUtil.getString(campaignInfo, AppParams.DESIGN_FRONT_URL));
			result.put(AppParams.PRODUCTS, allProductAndSizesForSPFApp);
			result.put(AppParams.COLLECTION_ID, collections);
			result.put(AppParams.TAGS, tags);
			
			LOGGER.info("Shopify App resultMap: " + result.toString());
			
			routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
			routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
			routingContext.put(AppParams.RESPONSE_DATA, result);

			routingContext.next();
			
		} else {
			routingContext.fail(500);
		}
	}
	
	private void checkGroup(Map ispProduct, List<Map> shirts, List<Map> mugs, List<Map> posters,
				List<Map> phonecases, List<Map> canvas, List<Map> normal3d) {
		Map base = ParamUtil.getMapData(ispProduct, AppParams.BASE);
		String baseId = ParamUtil.getString(base, AppParams.ID);
		String group = Base2dClassificationUtil.classify(baseId);
		if (group.equalsIgnoreCase("shirt")) shirts.add(ispProduct);
		if (group.equalsIgnoreCase("mugs")) mugs.add(ispProduct);
		if (group.equalsIgnoreCase("poster")) posters.add(ispProduct);
		if (group.equalsIgnoreCase("phonecase")) phonecases.add(ispProduct);
		if (group.equalsIgnoreCase("canvas")) canvas.add(ispProduct);
		if (group.equalsIgnoreCase("normal3d")) normal3d.add(ispProduct);
	}
	
	private void findMainDesign(List<Map> shirts, String mainType, List<Map> shirtMainDesign) {
		for (Map shirt : shirts) {
			List<Map> designs = ParamUtil.getListData(shirt, AppParams.DESIGNS);
			Boolean isMainDesign = false;
			String type = "";
			for (Map design : designs) {
				isMainDesign = ParamUtil.getBoolean(design, AppParams.MAIN);
				type = ParamUtil.getString(design, AppParams.TYPE);
				if (isMainDesign && type.equals(mainType)) {
					Map<String, Object> base = ParamUtil.getMapData(shirt, AppParams.BASE);
					design.put(AppParams.BASE, base);
					shirtMainDesign.add(design);
				}
			}
		}
	}
	
	private CompletableFuture<List<Map>> execute(String userId, String campaignId, List<Map> products, String ispUri) {
		return CompletableFuture.supplyAsync(() -> {
			if (!products.isEmpty()) {
				try {
					return generateVariantsMockUp(userId, campaignId, products, ispUri);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return Collections.EMPTY_LIST;
		});
	}
	
	private CompletableFuture<List<Map>> executeV2(String userId, String campaignId, List<Map> products,
			String ispUri, List<Map> designs, @Nullable List<Map> bases) {
		return CompletableFuture.supplyAsync(() -> {
			if (!products.isEmpty()) {
				try {
					return generateVariantsMockUpWithBaseInfo(userId, campaignId, products, ispUri, designs, bases);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return Collections.EMPTY_LIST;
		});
	}
	
	private List<Map> generateVariantsMockUp(String userId, String campaignId, List<Map> products, String ispUri) 
			throws JsonProcessingException, UnirestException, ParseException, UnsupportedEncodingException, SQLException {
		List<Map> result = new ArrayList<>();
		Map ispRequestBodyMap = new LinkedHashMap();
		ispRequestBodyMap.put(AppParams.USER_ID, userId);
		ispRequestBodyMap.put(AppParams.CAMPAIGN_ID, campaignId);
		ispRequestBodyMap.put(AppParams.PRODUCTS, products);
		ObjectMapper mapper = new ObjectMapper();
		String jsonBody = mapper.writeValueAsString(ispRequestBodyMap);
		LOGGER.info("ISP REQUEST BODY :" + jsonBody);

		HttpResponse<JsonNode> response = Unirest.post(ispUri)
												.body(jsonBody)
												.asJson();

		result = ispResponseHandlerV2(ispUri, campaignId, response);
		return result;
	}
	
	private List<Map> generateVariantsMockUpWithBaseInfo(String userId, String campaignId, List<Map> products, String ispUri, List<Map> designs, List<Map> bases)
			throws JsonProcessingException, UnirestException, ParseException, UnsupportedEncodingException, SQLException {
		List<Map> result = new ArrayList<>();
		Map ispRequestBodyMap = new LinkedHashMap();
		ispRequestBodyMap.put(AppParams.USER_ID, userId);
		ispRequestBodyMap.put(AppParams.CAMPAIGN_ID, campaignId);
		ispRequestBodyMap.put(AppParams.PRODUCTS, products);
		ispRequestBodyMap.put(AppParams.DESIGNS, designs);
		ispRequestBodyMap.put(AppParams.BASES, bases);
		ObjectMapper mapper = new ObjectMapper();
		String jsonBody = mapper.writeValueAsString(ispRequestBodyMap);
		LOGGER.info("ISP REQUEST BODY :" + jsonBody);

		HttpResponse<JsonNode> response = Unirest.post(ispUri).body(jsonBody).asJson();

		result = ispResponseHandlerV2(ispUri, campaignId, response);
		return result;
	}
	
	private List<Map> ispResponseHandlerV2(String ispUri, String campaignId, HttpResponse<JsonNode> response)
			throws UnsupportedEncodingException, SQLException, ParseException {
		
		List<Map> result = new ArrayList<>();
		int responseCode = response.getStatus();
		LOGGER.info(ispUri + " response code : " + responseCode);
		if (responseCode != HttpResponseStatus.CREATED.code()) {
			CampaignService.updateState(campaignId, ResourceStates.DRAFT);
			throw new SystemException(GENERATE_MOCKUP_FAILED);
		}
		
		Map<String, Object> map = JSONStringToMapUtil.toMap(response.getBody().getObject());
		List<Map> productList = ParamUtil.getListData(map, AppParams.PRODUCTS);
		
		String productId, defaultColorId = "", baseId, baseName, frontDesignId = "", backDesignId = "";
		boolean productDefault;
		Map productBaseInfoMap;
		List<Map> ispVariants, productColorList, productDesignList;
		List<String> productColorIds;
		
		List<Map> allVariantDBTypeList = new ArrayList<>();
		
		for (Map product : productList) {
			
			productId = ParamUtil.getString(product, AppParams.ID);
			productDefault = ParamUtil.getBoolean(product, AppParams.DEFAULT);
			ispVariants = ParamUtil.getListData(product, AppParams.VARIANTS);
			productColorList = ParamUtil.getListData(product, AppParams.COLORS);
			productColorIds = productColorList.stream().map(o -> ParamUtil.getString(o, AppParams.ID))
					.collect(Collectors.toList());
			
			if (productDefault) {
				for (Map productColor : productColorList) {
					if (ParamUtil.getBoolean(productColor, AppParams.DEFAULT)) {
						defaultColorId = ParamUtil.getString(productColor, AppParams.ID);
					}
				}
			}
			
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
			
			List<Map> variantDBTypeList = new ArrayList<>();
			if (!CollectionUtils.isEmpty(ispVariants)) {
				variantDBTypeList = insertNewProductVariants(campaignId, productId, ispVariants, baseId, baseName, frontDesignId,
						backDesignId, productColorIds);
			}
			
			allVariantDBTypeList.addAll(variantDBTypeList);
		}
		
		return allVariantDBTypeList;
	}
	
	private static List<Map> insertNewProductVariants(String campaignId, String productId, List<Map> requestVariants,
			String baseId, String baseName, String frontDesignId, String backDesignId, List<String> productColorIds) throws SQLException {
		
    	Map variantColor;
		String colorId, colorValue, colorName, variantName, frontImageUrl, backImageUrl;
		boolean defaultVariant;
		int nOrder;
		List<Map> variantDBTypeList = new ArrayList<>();
		
		for (Map productVariant : requestVariants) {
			Map variantDBType = new HashedMap<>();
			
			variantColor = ParamUtil.getMapData(productVariant, AppParams.COLOR);
			colorId = ParamUtil.getString(variantColor, AppParams.ID);
			colorValue = ParamUtil.getString(variantColor, AppParams.VALUE);
			colorName = ParamUtil.getString(variantColor, AppParams.NAME);
			variantName = baseName + " - " + colorName;
			frontImageUrl = ParamUtil.getString(productVariant, AppParams.URLFRONT);
			backImageUrl = ParamUtil.getString(productVariant, AppParams.URLBACK);
			defaultVariant = ParamUtil.getBoolean(variantColor, AppParams.DEFAULT);
			nOrder = productColorIds.indexOf(colorId);
			
			variantDBType.put(AppParams.PRODUCT_ID, productId);
			variantDBType.put(AppParams.COLOR_ID, colorId);
			variantDBType.put(AppParams.COLOR_VALUE, colorValue);
			variantDBType.put(AppParams.URLFRONT, frontImageUrl);
			variantDBType.put(AppParams.URLBACK, backImageUrl);
			variantDBType.put(AppParams.FRONT_DESIGN_ID, frontDesignId);
			variantDBType.put(AppParams.BACK_DESIGN_ID, backDesignId);
			variantDBType.put(AppParams.VARIANT_NAME, variantName);
			variantDBType.put(AppParams.BASE_ID, baseId);
			variantDBType.put(AppParams.N_DEFAULT, defaultVariant);
			variantDBType.put(AppParams.N_ORDER, nOrder);
			
			LOGGER.info("variantName= " + variantName);
			LOGGER.info("defaultVariant= " + defaultVariant);
			
			variantDBTypeList.add(variantDBType);
		}
		
		return variantDBTypeList;
	}
	
	private static final SystemError GENERATE_MOCKUP_FAILED = new SystemError("GENERATE_MOCKUP_FAILED",
			"Generate mockup failed. Please try again later!", "", "http://developer.30usd.com/errors/400.html");
	
	private static final Logger LOGGER = Logger.getLogger(ShopifyCampaignCreateV2Handler.class.getName());
}