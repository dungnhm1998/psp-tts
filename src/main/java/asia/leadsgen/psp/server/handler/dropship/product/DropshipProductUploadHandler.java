package asia.leadsgen.psp.server.handler.dropship.product;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreCampService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.WooService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.service_fulfill.ProductVariantMockupService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipProductUploadHandler implements Handler<RoutingContext> {

	private static final Map<String, String> baseGroupMap = initMap();

	private static Map<String, String> initMap() {
		Map<String, String> map = new HashMap<>();
		map.put("45GIOu2TGamLQwFV", "Hooded Blanket");
		map.put("dtyb7T3qjOqiijCq", "Blanket");
		map.put("N3ACmzTczz3zIive", "Wood Prints");
		map.put("XM1sp8CRrxUF59Tc", "Metal Prints");
		map.put("bokuCHNsIDVH3ubu", "Apparel");
		map.put("xi96DPRhJKosNBnA", "Canvas Prints");
		map.put("NVngNagqBFbd3011", "Tee Over Prints");

		return Collections.unmodifiableMap(map);
	}

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			try {

				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

				Map requestBody = routingContext.getBodyAsJson().getMap();
				String storeId = ParamUtil.getString(requestBody, AppParams.STORE_ID);

				Map storeSearchResult = DataAccessSecurer.secureDropshipStoreV2(userId, storeId);
				String campaignId = ParamUtil.getString(requestBody, AppParams.CAMPAIGN_ID);
				String customize = ParamUtil.getString(requestBody, "customize");

				if (storeSearchResult.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
				}

				String channel = ParamUtil.getString(storeSearchResult, AppParams.CHANNEL);
				if (channel.equalsIgnoreCase("woocommerce")) {
					boolean checkMapAttributes = WooService.checkStoreIsMapped(storeId);
					if (!checkMapAttributes) {
						throw new BadRequestException(SystemError.NOT_MAPPED_ATTRIBUTES);
					} else {
						DropShipStoreCampService.insertStoreCamp(storeId, campaignId, null, null, ResourceStates.CREATED,
								"", channel);

						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
					}
					
				} else {
					String customVariantsData = "";
					if ("1".equals(customize)) {
						customVariantsData = createVariantsMap(requestBody);
					}
					
					DropShipStoreCampService.insertStoreCamp(storeId, campaignId, null, null, ResourceStates.CREATED,
							customVariantsData, channel);

					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
				}
				
				future.complete();

			} catch (Exception e) {

				LOGGER.log(Level.SEVERE, "[ERROR]", e);
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

	public static String createVariantsMap(Map requestBody) throws SQLException, ParseException {
		String campaignTitle = ParamUtil.getString(requestBody, AppParams.TITLE);

		// Add product_type
		String campaignId = ParamUtil.getString(requestBody, AppParams.CAMPAIGN_ID);
		Map campaignInfo = CampaignService.getV2(campaignId);
		List<Map> campaignProductList = ParamUtil.getListData(campaignInfo, AppParams.PRODUCTS);
		
		List<Map> products = ParamUtil.getListData(requestBody, AppParams.PRODUCTS);
		Map firstProductRequets = products.get(0);
		String firstProductRequestId = ParamUtil.getString(firstProductRequets, AppParams.ID);
		
		Map productInfoDB = campaignProductList.stream().filter(m -> (m.get(AppParams.ID)).equals(firstProductRequestId)).findFirst().get();
		String productType = ParamUtil.getString(productInfoDB, AppParams.PRODUCT_TYPE);
		
		Map productsProcessedMap = new LinkedHashMap<>();
		List<Map> productVariants = new ArrayList<>();
		Set<String> allColors = new LinkedHashSet<>();
//		List<Map> allImages = new ArrayList<>();
		Set<Map> allImagesUrl = new LinkedHashSet<Map>();
		List<String> processedProduct = new ArrayList<>();
		Set<String> sizesName = new HashSet<>();
		Map mappingVariantImageMap = new LinkedHashMap<>();
		
		products.forEach(product -> {
			String name = ParamUtil.getString(product, AppParams.PRODUCT_NAME);
			boolean backView = ParamUtil.getBoolean(product, AppParams.BACK_VIEW);
			List<Map> variantsCustom = ParamUtil.getListData(product, AppParams.VARIANTS);
			variantsCustom.forEach(variantCustom -> {
				processedProduct.add(name);
				String baseId = ParamUtil.getString(variantCustom, AppParams.BASE_ID);
				String variantId = ParamUtil.getString(variantCustom, AppParams.ID);
				String variantName = ParamUtil.getString(variantCustom, AppParams.NAME);
				String colorName = variantName.substring(variantName.lastIndexOf("-") + 2);
				
				Map imageMap = ParamUtil.getMapData(variantCustom, AppParams.IMAGE);
//				String imageFront = ParamUtil.getString(imageMap, AppParams.FRONT);
//				String image = StringUtils.isEmpty(ParamUtil.getString(variantCustom, AppParams.FRONT_DESIGN_ID))
//						? ParamUtil.getString(imageMap, AppParams.BACK)
//						: imageFront;
//				if (!StringUtils.isEmpty(imageFront)) {
//					image = imageFront;
//				}
				
				String imageFront = ParamUtil.getString(imageMap, AppParams.FRONT);
				String imageBack = ParamUtil.getString(imageMap, AppParams.BACK);
				String image, image2 = "";
				if (backView) {
					image = imageBack;
					image2 = imageFront;
				} else {
					image = imageFront;
					image2 = imageBack;
				}
				
				String imgSplit = StringUtils.substring(StringUtils.substring(image, 0, StringUtils.lastIndexOf(image, ".")), StringUtils.lastIndexOf(image, "/") + 1);
				mappingVariantImageMap.put(name + " / " + colorName, imgSplit);
				
				List<String> allImages = new ArrayList<String>();
				allImages.add(image);
				allImages.add(image2);
				
				Set<Map> allImagesProduct = new LinkedHashSet<Map>();
				try {
					allImagesProduct = getAllImageProduct(variantId, campaignId, allImages);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				allImagesUrl.addAll(allImagesProduct);
				
				List<Map> sizesCustom = ParamUtil.getListData(variantCustom, AppParams.SIZES);

				List<Map> variants = new ArrayList<>();
				sizesCustom.forEach(sizeCustom -> {
					String sizeId = ParamUtil.getString(sizeCustom, AppParams.SIZE_ID);
					String price = ParamUtil.getString(sizeCustom, AppParams.PRICE);
					double compare_at_price = Double.parseDouble(price) * 120 / 100;
					String sizeName = ParamUtil.getString(sizeCustom, AppParams.SIZE_NAME);
					Map variant = new LinkedHashMap<>();
					variant.put("option1", name);
					variant.put("option2", colorName);
					variant.put("option3", sizeName);
					variant.put("price", price);
					variant.put("compare_at_price", compare_at_price);
					variant.put("sku", variantId + "|" + sizeId);

					// Hardcode inventory_quantity
//					variant.put("inventory_quantity", 9999);
//					variant.put("inventory_management", "shopify");
//					variant.put("inventory_policy", "continue");
					variant.put(AppParams.INVENTORY_MANAGEMENT, null);
					variant.put(AppParams.INVENTORY_POLICY, "deny");

					variants.add(variant);
					sizesName.add(sizeName);
				});
				productVariants.addAll(variants);
				allColors.add(colorName);
				
//				Map imgSource = new HashMap<>();
//				imgSource.put("src", image);
//				allImages.add(imgSource);
			});
		});

		List<Map> options = new ArrayList<>();

		Map nameOption = new LinkedHashMap<>();
		nameOption.put("name", "Name");
		nameOption.put("values", processedProduct);

		options.add(nameOption);

		Map colorOption = new LinkedHashMap<>();
		List<String> colors = new ArrayList<>();
		colors.addAll(allColors);

		colorOption.put("name", "Color");
		colorOption.put("values", colors);
		options.add(colorOption);

		Map sizeOption = new LinkedHashMap<>();
		sizeOption.put("name", "Size");
		sizeOption.put("values", sizesName);

		options.add(sizeOption);

		productsProcessedMap.put("options", options);
//		productsProcessedMap.put("images", allImages);

		productsProcessedMap.put("title", campaignTitle);
		productsProcessedMap.put("body_html", "");
		productsProcessedMap.put("vendor", "BurgerPrints");
		productsProcessedMap.put("variants", productVariants);

		// add product_type
		productsProcessedMap.put("product_type", productType);
		
		List<Map> allImagesUrlList = new ArrayList<Map>(allImagesUrl);
		Map productInfo = new LinkedHashMap<>();
		productInfo.put("product_info", productsProcessedMap);
		productInfo.put("all_images_url", allImagesUrlList);
		productInfo.put("mapping_variant_img", mappingVariantImageMap);

		return new JsonObject(productInfo).encode();
	}
	
	private static Set<Map> getAllImageProduct(String variantId, String campaignId, List<String> allImages)
			throws SQLException {

		List<String> allMockup = ProductVariantMockupService.getMockupByVariantId(variantId, campaignId);
		allImages.addAll(allMockup);

		Set<Map> allImagesProduct = new LinkedHashSet<Map>();
		for (String img : allImages) {
			Map<String, String> imageMap = new HashMap<>();
			if (img != null && img.isEmpty() == false) {
				imageMap.put("src", img);
				allImagesProduct.add(imageMap);
			}
		}

		return allImagesProduct;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipProductUploadHandler.class.getName());
}
