package asia.leadsgen.psp.server.handler.dropship.shopbase;

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

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreCampService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ShopbaseProductUploadHandler implements Handler<RoutingContext> {

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

				DataAccessSecurer.secureDropshipStore(userId, storeId);

				String campaignId = ParamUtil.getString(requestBody, AppParams.CAMPAIGN_ID);
				String customize = ParamUtil.getString(requestBody, "customize");

				Map storeSearchResult = DropShipStoreService.lookUp(storeId);

				if (storeSearchResult.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
				}

				String channel = ParamUtil.getString(storeSearchResult, AppParams.CHANNEL);
				String customVariantsData = "";
				if ("1".equals(customize)) {
					customVariantsData = createVariantsMap(requestBody);
				}
				if (channel.equalsIgnoreCase(AppConstants.SHOPIFY)) {
					DropShipStoreCampService.insertStoreCamp(storeId, campaignId, null, null, ResourceStates.CREATED,
							customVariantsData, "shopbase");
				}

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());

			} catch (Exception e) {

				LOGGER.log(Level.SEVERE, "[ERROR]", e);
				routingContext.fail(e.getCause());
			}

			future.complete();

		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	public static String createVariantsMap(Map requestBody) {
		String campaignTitle = ParamUtil.getString(requestBody, AppParams.TITLE);

		// Add product_type
		String baseGroupId = ParamUtil.getString(requestBody, AppParams.BASE_GROUP_ID);
		String productType = ParamUtil.getString(baseGroupMap, baseGroupId);

		Map productsProcessedMap = new LinkedHashMap<>();
		List<Map> productVariants = new ArrayList<>();
		Set<String> allColors = new LinkedHashSet<>();
		List<Map> allImages = new ArrayList<>();
		List<String> processedProduct = new ArrayList<>();
		Set<String> sizesName = new HashSet<>();

		List<Map> products = ParamUtil.getListData(requestBody, AppParams.PRODUCTS);
		products.forEach(product -> {
			String name = ParamUtil.getString(product, AppParams.PRODUCT_NAME);
			List<Map> variantsCustom = ParamUtil.getListData(product, AppParams.VARIANTS);
			variantsCustom.forEach(variantCustom -> {
				processedProduct.add(name);
				String baseId = ParamUtil.getString(variantCustom, AppParams.BASE_ID);
				String variantId = ParamUtil.getString(variantCustom, AppParams.ID);
				String variantName = ParamUtil.getString(variantCustom, AppParams.NAME);
				String colorName = variantName.substring(variantName.lastIndexOf("-") + 2);
				Map imageMap = ParamUtil.getMapData(variantCustom, AppParams.IMAGE);
				String imageFront = ParamUtil.getString(imageMap, AppParams.FRONT);
				String image = StringUtils.isEmpty(ParamUtil.getString(variantCustom, AppParams.FRONT_DESIGN_ID))
						? ParamUtil.getString(imageMap, AppParams.BACK)
						: imageFront;
				if (!StringUtils.isEmpty(imageFront)) {
					image = imageFront;
				}
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
					variant.put("option5", baseId);
					variant.put("price", price);
					variant.put("compare_at_price", compare_at_price);
					variant.put("sku", variantId + "|" + sizeId);

					// Hardcode inventory_quantity
					variant.put("inventory_quantity", 9999);
					variant.put("inventory_management", "shopbase");
					variant.put("inventory_policy", "continue");

					variants.add(variant);
					sizesName.add(sizeName);
				});
				productVariants.addAll(variants);
				allColors.add(colorName);
				Map imgSource = new HashMap<>();
				imgSource.put("src", image);
				allImages.add(imgSource);
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
		productsProcessedMap.put("images", allImages);

		productsProcessedMap.put("images", allImages);
		productsProcessedMap.put("title", campaignTitle);
		productsProcessedMap.put("body_html", "");
		productsProcessedMap.put("variants", productVariants);

		// add product_type
		productsProcessedMap.put("product_type", productType);

		Map productInfo = new LinkedHashMap<>();
		productInfo.put("product", productsProcessedMap);

		return new JsonObject(productInfo).encode();
	}

	private static final Logger LOGGER = Logger.getLogger(ShopbaseProductUploadHandler.class.getName());
}
