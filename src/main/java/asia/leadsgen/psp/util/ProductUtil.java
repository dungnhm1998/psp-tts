package asia.leadsgen.psp.util;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.interfaces.LoggerInterface;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.service.BaseColorService;
import asia.leadsgen.psp.service.BaseSizeService;
import asia.leadsgen.psp.service.PreferencesService;
import asia.leadsgen.psp.service.ProductDesignService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.ShippingFeeService;

/**
 * Created by hungdx on 5/8/17.
 */
public class ProductUtil implements LoggerInterface{

	public static String calculateShippingFee1(String shippingPrice, int productQuantity, double addingPrice)
			throws SQLException {

		Double shippingFee = GetterUtil.getDouble(shippingPrice);

		if (shippingFee > 0 && productQuantity > 1) {

			Double additionalShippingFee = addingPrice * (productQuantity - 1);

			shippingFee += additionalShippingFee;
		}

		NumberFormat amountFormatter = new DecimalFormat("#0.00");

		return amountFormatter.format(shippingFee);
	}

	public static double calculateShippingFee(double shippingPrice, int productQuantity, double addingPrice)
			throws SQLException {

		if (shippingPrice > 0 && productQuantity > 1) {

			Double additionalShippingFee = addingPrice * (productQuantity - 1);

			shippingPrice += additionalShippingFee;
		} else if (productQuantity == 0) {
			shippingPrice = 0;
		}

		return GetterUtil.format(shippingPrice, 2);

	}

//	public static void updateProductBaseCost(String productId) throws SQLException {
//
//		Map productInfoMap = ProductService.get(productId, true, false, false, true, false);
//
//		double basePrice = GetterUtil
//				.getDouble(ParamUtil.getString(ParamUtil.getMapData(productInfoMap, AppParams.BASE), AppParams.PRICE));
//
//		double printingFees = 0.00;
//
//		List<Map> productDesignList = ParamUtil.getListData(productInfoMap, AppParams.DESIGNS);
//
//		if (productDesignList.size() > 0) {
//
//			int totalFrontLayers = 0;
//			int totalBackLayers = 0;
//
//			for (Map productDesign : productDesignList) {
//
//				String designType = ParamUtil.getString(productDesign, AppParams.TYPE);
//
//				if (designType.equalsIgnoreCase(AppConstants.DESIGN_TYPE_FRONT)) {
//
//					totalFrontLayers++;
//
//				} else if (designType.equalsIgnoreCase(AppConstants.DESIGN_TYPE_BACK)) {
//
//					totalBackLayers++;
//
//				} else {
//
//					String designId = ParamUtil.getString(productDesign, AppParams.ID);
//
//					DesignService.delete(designId);
//				}
//			}
//
//			boolean twoSidesPrinting = (totalFrontLayers > 0 && totalBackLayers > 0) ? true : false;
//
//			double mainPrintingPrice = twoSidesPrinting
//					? GetterUtil.getDouble(PreferencesService.get(PreferenceKeys.PRODUCT_TWO_SIDES_PRINTING_PRICE))
//					: GetterUtil.getDouble(PreferencesService.get(PreferenceKeys.PRODUCT_ONE_SIDE_PRINTING_PRICE));
//
//			double additionalPrintingPrice = 0.00;
//
//			if (totalFrontLayers > 1 || totalBackLayers > 1) {
//
//				double additionalLayersPrintingPrice = GetterUtil.getDouble(
//						PreferencesService.get(PreferenceKeys.PRODUCT_ADDITIONAL_LAYERS_UNIT_PRINTING_PRICE), 1.00);
//
//				double frontAdditionalLayersPrintingPrice = totalFrontLayers > 1
//						? (totalFrontLayers - 1) * additionalLayersPrintingPrice
//						: 0.00;
//
//				double backAdditionalLayersPrintingPrice = totalBackLayers > 1
//						? (totalBackLayers - 1) * additionalLayersPrintingPrice
//						: 0.00;
//
//				additionalPrintingPrice = frontAdditionalLayersPrintingPrice + backAdditionalLayersPrintingPrice;
//			}
//
//			printingFees = mainPrintingPrice + additionalPrintingPrice;
//		}
//
//		String baseCost = new DecimalFormat("#.00").format(basePrice + printingFees);
//
//		ProductService.updateBaseCost(productId, baseCost);
//	}

	public static String getDefaultProductColorId() throws SQLException {
		return ParamUtil.getString(BaseColorService.getDefault(), AppParams.ID);
	}

	public static List getBaseColorList(String colorIds, String defaultColorId) throws SQLException {

		Map colorListResultMap = BaseColorService.list(colorIds, ResourceStates.APPROVED);

		int total = ParamUtil.getInt(colorListResultMap, AppParams.TOTAL);

		if (total > 0) {

			List<Map> colorList = ParamUtil.getListData(colorListResultMap, AppParams.COLORS);

			if (defaultColorId != null && !defaultColorId.isEmpty()) {

				List<Map> resultList = new ArrayList<>();

				for (Map colorMap : colorList) {

					String colorId = ParamUtil.getString(colorMap, AppParams.ID);

					boolean colorDefault = colorId.equalsIgnoreCase(defaultColorId) ? true : false;

					colorMap.put(AppParams.DEFAULT, colorDefault);

					resultList.add(colorMap);
				}
				return resultList;
			} else {
				return colorList;
			}
		} else {
			return new ArrayList();
		}
	}

	public static List getBaseSizeList(String sizeIds) throws SQLException {

		Map sizeListResultMap = BaseSizeService.list(sizeIds, ResourceStates.APPROVED);

		int total = ParamUtil.getInt(sizeListResultMap, AppParams.TOTAL);

		if (total > 0) {
			return ParamUtil.getListData(sizeListResultMap, AppParams.SIZES);
		} else {
			return new ArrayList();
		}
	}

	public static List getProductDesignList(String productId) throws SQLException {

		Map designListResultMap = ProductDesignService.search(productId, StringPool.BLANK);

		int total = ParamUtil.getInt(designListResultMap, AppParams.TOTAL);

		if (total > 0) {
			return ParamUtil.getListData(designListResultMap, AppParams.DESIGNS);
		} else {
			return new ArrayList();
		}
	}

	public static List getProductVariantList(String productId) throws SQLException {

		Map variantSearchResultMap = ProductVariantService.search(productId, "", "", "", "", ResourceStates.APPROVED);

		int total = ParamUtil.getInt(variantSearchResultMap, AppParams.TOTAL);

		if (total > 0) {
			return ParamUtil.getListData(variantSearchResultMap, AppParams.VARIANTS);
		} else {
			return new ArrayList();
		}
	}

	public static String getPrintPrice(int totalColors) throws SQLException {
		String key = null;
		switch (totalColors) {
		case 8:
			key = "redis.default.adding.price.color.8";
			break;
		case 7:
			key = "redis.default.adding.price.color.7";
			break;
		case 6:
			key = "redis.default.adding.price.color.6";
			break;
		case 5:
			key = "redis.default.adding.price.color.5";
			break;
		case 4:
			key = "redis.default.adding.price.color.4";
			break;
		case 3:
			key = "redis.default.adding.price.color.3";
			break;
		case 2:
			key = "redis.default.adding.price.color.2";
			break;
		case 1:
			key = "redis.default.adding.price.color.1";
			break;
		case 0:
			key = "redis.default.adding.price.color.0";
			break;
		default:
			key = "redis.default.adding.price.color.9";
			break;
		}

		return PreferencesService.get(key);
	}

	public static Map calculateShippingFeeAndTax(Map<String, Integer> itemGroupQuantity, String shippingMethod, String baseId, String shippingCountryCode, int quantity) throws SQLException {
		Double shippingFee = 0.00d;
		Double taxAmount = 0.00d;

		ShippingFeeObj shippingFeeObj = ShippingFeeService.getShippingFee(baseId, shippingCountryCode);
		if (shippingFeeObj == null) {
			throw new BadRequestException(SystemError.INVALID_ORDER_SHIPPING);
		}

		int groupTotalItem = itemGroupQuantity.get(shippingFeeObj.getGroupId());

		double price = shippingMethod.equalsIgnoreCase(AppParams.EXPRESS)? shippingFeeObj.getExpressPrice() : shippingFeeObj.getDropshipPrice();
		double addingPrice = shippingMethod.equalsIgnoreCase(AppParams.EXPRESS)? shippingFeeObj.getExpressAddingPrice() : shippingFeeObj.getDropshipAddingPrice();
		taxAmount = Double.valueOf(shippingFeeObj.getTaxAmount());
		if (groupTotalItem == 0) {
			shippingFee = ProductUtil.calculateShippingFee(price, quantity, addingPrice);
		} else {
			shippingFee = addingPrice * quantity;
		}

		itemGroupQuantity.put(shippingFeeObj.getGroupId(), groupTotalItem + quantity);
		shippingFee = GetterUtil.format(shippingFee, 2);
		taxAmount = GetterUtil.format(taxAmount * quantity, 2);

		Map feeMap = new HashedMap();
		feeMap.put(AppParams.SHIPPING_FEE, shippingFee);
		feeMap.put(AppParams.TAX_AMOUNT, taxAmount);
		return feeMap;
	}
	
	/**
	 * 
	 * @param baseIds
	 * @param shippingCountryCode
	 * @param shippingMethod
	 * @return
	 * @throws Exception
	 */
	public static Map getShippingInfoForListItems(Collection<String> baseIds, String shippingCountryCode, String shippingMethod) throws SQLException {

		List<ShippingFeeObj> lstShippingFee = new ArrayList<ShippingFeeObj>();

		Map mapByBase = new HashedMap<String, String>();

		for (String baseId : baseIds) {
			logger.info("Get shipping fee with baseId=" + baseId + ", countryCode=" + shippingCountryCode);
			ShippingFeeObj shippingFeeObj = ShippingFeeService.getShippingFee(baseId, shippingCountryCode);
			if (shippingFeeObj == null) {
				throw new BadRequestException(SystemError.INVALID_ORDER_SHIPPING);
			}

			lstShippingFee.add(shippingFeeObj);
			mapByBase.put(baseId, shippingFeeObj.getGroupId());
			logger.info("mapByBase put baseId=" + baseId + ", shippingGroupId=" + shippingFeeObj.getGroupId());
		}

		Map<String, List<ShippingFeeObj>> mapByShippingGroup = lstShippingFee.stream().collect(Collectors.groupingBy(ShippingFeeObj::getGroupId));

		Map result = new HashedMap<>();
		result.put(AppParams.BASES, mapByBase);
		result.put(AppParams.GROUP_ID, mapByShippingGroup);

		return result;

	}

	public static Map calculateDropshipShippingFeeAndTaxV2(Map<String, Integer> itemGroupQuantity, String baseId, String shippingMethod, int quantity, Map shippingInfo) throws SQLException {
		Double shippingFee = 0.00d;
		Double taxAmount = 0.00d;
		String findItemMaxShippingKeyPrefix = "find_item_with_max_shipping_cost_";
		
		Map<String, String> groupByBaseId = ParamUtil.getMapData(shippingInfo, AppParams.BASES);
		Map<String, List<ShippingFeeObj>> mapByShippingGroup = ParamUtil.getMapData(shippingInfo, AppParams.GROUP_ID); 
		
		
		String shippingGroupId = groupByBaseId.get(baseId);
		
		Boolean findItemWithMaxShippingCost = ParamUtil.getBoolean(shippingInfo, findItemMaxShippingKeyPrefix + shippingGroupId , false);
		
		int groupTotalItem = itemGroupQuantity.get(shippingGroupId);

		List<ShippingFeeObj> lstShippingFeeByGroup = mapByShippingGroup.get(shippingGroupId);

		ShippingFeeObj shippingFeeObj = lstShippingFeeByGroup.stream().filter(e -> baseId.equalsIgnoreCase(e.getBaseId())).findFirst().get();
		
//		taxAmount = GetterUtil.format(Double.valueOf(shippingFeeObj.getTaxAmount()) * quantity, 2);
		
		ShippingFeeObj maxShippingFeeObj = null;
		
		if (AppParams.EXPRESS.equalsIgnoreCase(shippingMethod)) {
			
			maxShippingFeeObj = lstShippingFeeByGroup.stream().max(Comparator.comparing(ShippingFeeObj::getExpressPrice)).get();
			
			shippingFee = shippingFeeObj.getExpressPrice() + (quantity-1)*shippingFeeObj.getExpressAddingPrice();
			
		} else {
			
			maxShippingFeeObj = lstShippingFeeByGroup.stream().max(Comparator.comparing(ShippingFeeObj::getDropshipPrice)).get();
			
			shippingFee = shippingFeeObj.getDropshipPrice() + (quantity - 1)*shippingFeeObj.getDropshipAddingPrice();
			
		}
		
		if (baseId.equalsIgnoreCase(maxShippingFeeObj.getBaseId()) && !findItemWithMaxShippingCost) {
			if (AppParams.EXPRESS.equalsIgnoreCase(shippingMethod)) {
				shippingFee = maxShippingFeeObj.getExpressPrice() + (quantity-1)*maxShippingFeeObj.getExpressAddingPrice();
			} else {
				shippingFee = maxShippingFeeObj.getDropshipPrice() + (quantity - 1)*maxShippingFeeObj.getDropshipAddingPrice();
			}
			findItemWithMaxShippingCost = true;
			shippingInfo.put(findItemMaxShippingKeyPrefix + shippingGroupId , findItemWithMaxShippingCost);
		} else {
			
			if (AppParams.EXPRESS.equalsIgnoreCase(shippingMethod)) {

				shippingFee = shippingFeeObj.getExpressAddingPrice() * quantity;
			} else {
				shippingFee = shippingFeeObj.getDropshipAddingPrice() * quantity;
			}
		}

		itemGroupQuantity.put(shippingGroupId, groupTotalItem + quantity);
		
		shippingFee = GetterUtil.format(shippingFee, 2);
		

		Map feeMap = new HashedMap();
		feeMap.put(AppParams.SHIPPING_FEE, shippingFee);
		//Shipping fee tax hardcode = 0
		feeMap.put(AppParams.TAX_AMOUNT, taxAmount);
		return feeMap;
	}
	
}
