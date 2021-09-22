package asia.leadsgen.psp.util;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import asia.leadsgen.psp.service.BaseColorService;
import asia.leadsgen.psp.service.MockupService;
import asia.leadsgen.psp.service.ProductDesignService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.ServerService;

/**
 * Created by hungdx on 6/21/17.
 */
public class CampaignUtil {

	private static final String FLUSH_CACHE = "/api/flushcache";

	public static void updateCampaignCacheData(String domainName, String uri) throws SQLException {

		uri = uri.contains(StringPool.FORWARD_SLASH) ? uri.replace(StringPool.FORWARD_SLASH, "") : uri;
		ArrayList<String> serverList = ServerService.getServerList();
		DateFormat df = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("x-authorization",
				"WSS-HMAC-SHA256 Credential=30USDCOM/20180816/leadsgen/psp/wss-request,SignedHeaders=,Signature=d05731fd84b674e160eec3aa247a1dd04269245ab6738913ef350e7264e904c8");
		headers.put("x-date", df.format(new Date()));
		headers.put("x-expires", "3600");

		Map<String, String> params = new HashMap<>();
		params.put("uri", uri);

		Map<String, Object> data = new HashMap<>();

		for (String server : serverList) {
			String url = server + FLUSH_CACHE;
			new Thread(() -> {
				try {
					HttpClient.sendRequest(url, "get", headers, params, data);
				} catch (Exception e) {
					LOGGER.severe(e.getMessage());
				}
			}).start();
		}
	}

	public static Date getCampaignEndDate(Date startDate, int campaignLength) {

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(startDate);
		calendar.add(Calendar.DATE, campaignLength - 1);

		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return calendar.getTime();
	}

	public static Map getBaseInfo(Map data) throws SQLException {

		Map base = new LinkedHashMap<>();
		String id = ParamUtil.getString(data, AppParams.S_BASE_ID);
		base.put(AppParams.ID, id);
		base.put(AppParams.SHORT_CODE, ParamUtil.getString(data, AppParams.S_SHORT_CODE));
		base.put(AppParams.DESCRIPTION, ParamUtil.getString(data, AppParams.S_DESC));
		base.put(AppParams.DESIGN_TYPE, ParamUtil.getString(data, AppParams.S_DESIGN_TYPE));
		base.put(AppParams.DESIGN_GROUP, ParamUtil.getString(data, AppParams.S_DESIGN_GROUP));
		base.put(AppParams.FULFILLMENTS, ParamUtil.getString(data, AppParams.S_FULFILLMENTS));
		base.put(AppParams.INCLUDE, ParamUtil.getString(data, AppParams.N_INCLUDE));
		base.put(AppParams.NAME, ParamUtil.getString(data, AppParams.S_NAME));
		base.put(AppParams.DISPLAY_NAME, ParamUtil.getString(data, AppParams.S_DISPLAY_NAME));
		base.put(AppParams.SIZE_PRICE_EDITABLE, ParamUtil.getBoolean(data, AppParams.N_EDIT_SIZE_PRICE));

		Map baseType = new LinkedHashMap<>();
		baseType.put(AppParams.ID, ParamUtil.getString(data, AppParams.S_TYPE_ID));
		baseType.put(AppParams.NAME, ParamUtil.getString(data, AppParams.S_TYPE_NAME));

		Map baseImage = new LinkedHashMap<>();
		baseImage.put(AppParams.BACK, ParamUtil.getString(data, AppParams.S_BACK_IMG_URL));
		baseImage.put(AppParams.FRONT, ParamUtil.getString(data, AppParams.S_FRONT_IMG_URL));
		baseImage.put(AppParams.HEIGHT, ParamUtil.getString(data, AppParams.S_FRONT_IMG_HEIGHT));
		baseImage.put(AppParams.WIDTH, ParamUtil.getString(data, AppParams.S_FRONT_IMG_WIDTH));
		baseImage.put(AppParams.ICON, ParamUtil.getString(data, AppParams.S_ICON_IMG_URL));
		baseImage.put(AppParams.UNIT, ParamUtil.getString(data, AppParams.S_UNIT, DimensionUnits.PIXEL));

		Map basePrintable = new LinkedHashMap<>();
		basePrintable.put(AppParams.BACK_HEIGHT, ParamUtil.getString(data, AppParams.S_PRINTABLE_BACK_HEIGHT));
		basePrintable.put(AppParams.BACK_LEFT, ParamUtil.getString(data, AppParams.S_PRINTABLE_BACK_LEFT));
		basePrintable.put(AppParams.BACK_TOP, ParamUtil.getString(data, AppParams.S_PRINTABLE_BACK_TOP));
		basePrintable.put(AppParams.BACK_WIDTH, ParamUtil.getString(data, AppParams.S_PRINTABLE_BACK_WIDTH));
		basePrintable.put(AppParams.FRONT_HEIGHT, ParamUtil.getString(data, AppParams.S_PRINTABLE_FRONT_HEIGHT));
		basePrintable.put(AppParams.FRONT_LEFT, ParamUtil.getString(data, AppParams.S_PRINTABLE_FRONT_LEFT));
		basePrintable.put(AppParams.FRONT_TOP, ParamUtil.getString(data, AppParams.S_PRINTABLE_FRONT_TOP));
		basePrintable.put(AppParams.FRONT_WIDTH, ParamUtil.getString(data, AppParams.S_PRINTABLE_FRONT_WIDTH));
		basePrintable.put(AppParams.UNIT, ParamUtil.getString(data, AppParams.S_UNIT, DimensionUnits.PIXEL));

		Map<String, Object> dimension = new HashMap<String, Object>();
		dimension.put(AppParams.WIDTH, ParamUtil.getString(data, AppParams.S_DIMENSION_WIDTH));
		dimension.put(AppParams.HEIGHT, ParamUtil.getString(data, AppParams.S_DIMENSION_HEIGHT));

		base.put(AppParams.DIMENSION, dimension);

		base.put(AppParams.COLORS, BaseColorService.getAllBaseColorsCache().get(id));
		base.put(AppParams.IMAGE, baseImage);
		base.put(AppParams.PRINTABLE, basePrintable);
		base.put(AppParams.TYPE, baseType);

		return base;
	}

	public static List<Map> getBaseColorList(String baseId, String colorIds, String defaultColorId)
			throws SQLException {
		List<Map> baseColors = (List<Map>) BaseColorService.getAllBaseColorsCache().get(baseId);
		List<String> colorStrs = Arrays.asList(colorIds.split(","));
		List<Map> productColors = new ArrayList<Map>();

		for (String colorId : colorStrs) {
			Optional<Map> containColor = baseColors.stream().filter(o -> colorId.equals(ParamUtil.getString(o, AppParams.ID))).findFirst();
			Map colorMap = new LinkedHashMap<>();
			if(containColor.isPresent()) {
				colorMap = containColor.get();
				colorMap.put(AppParams.DEFAULT, colorId.equalsIgnoreCase(defaultColorId) ? true : false);
				productColors.add(colorMap);
			}
		}

		return productColors;
	}

	public static Map getMapProductDesigns(String campaignId) throws SQLException {
		List<Map> productsDesigns = ProductDesignService.getAllProductsDesignsByCampId(campaignId);
		Set<String> productIds = productsDesigns.stream().map(o -> ParamUtil.getString(o, AppParams.PRODUCT_ID))
				.collect(Collectors.toSet());

		Map mapProductDesigns = new LinkedHashMap<>();

		for (String productId : productIds) {
			mapProductDesigns.put(productId,
					productsDesigns.stream().filter(o -> productId.equals(ParamUtil.getString(o, AppParams.PRODUCT_ID)))
							.collect(Collectors.toList()));
		}

		return mapProductDesigns;
	}

	public static Map getMapProductVariants(String campaignId) throws SQLException {
		List<Map> campVariants = ProductVariantService.getCampaignVariants(campaignId, ResourceStates.APPROVED);

		Set<String> productIds = campVariants.stream().map(o -> ParamUtil.getString(o, AppParams.PRODUCT_ID))
				.collect(Collectors.toSet());

		Map mapProductVariants = new LinkedHashMap<>();

		for (String productId : productIds) {
			mapProductVariants.put(productId,
					campVariants.stream().parallel()
							.filter(o -> productId.equals(ParamUtil.getString(o, AppParams.PRODUCT_ID)))
							.collect(Collectors.toList()));
		}

		return mapProductVariants;
	}

	public static Map getCampaignMockups(String campaignId) throws SQLException {
		List<Map> campMockups = MockupService.getMockupByCampaignId(campaignId);
		Set<String> variantIds = campMockups.stream().map(o -> ParamUtil.getString(o, AppParams.VARIANT_ID))
				.collect(Collectors.toSet());

		Map mapVariantMockups = new LinkedHashMap<>();

		for (String variantId : variantIds) {
			mapVariantMockups.put(variantId,
					campMockups.stream().filter(o -> variantId.equals(ParamUtil.getString(o, AppParams.VARIANT_ID)))
							.collect(Collectors.toList()));
		}

		return mapVariantMockups;
	}
	
	public static long getTimeRemaining() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime twentythree;
		if (now.getHour() == 23) {
			twentythree = now.plusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0);
		} else {
			twentythree = now.withHour(23).withMinute(0).withSecond(0).withNano(0);
		}

		return Duration.between(now, twentythree).toMillis();
	}

	private static final Logger LOGGER = Logger.getLogger(CampaignUtil.class.getName());

}
