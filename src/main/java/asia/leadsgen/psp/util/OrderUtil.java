package asia.leadsgen.psp.util;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import asia.leadsgen.psp.interfaces.LoggerInterface;
import asia.leadsgen.psp.obj.DropshipBaseSkuObj;
import asia.leadsgen.psp.obj.DropshipCustomApiItem;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipCampApiOrderCreateHandler;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service_fulfill.DropshipBaseSkuService;

public class OrderUtil implements LoggerInterface {

	public static Set<String> getSetBaseFromItemCamp(List<Map> requestItems) throws SQLException {

		Set<String> result = new HashSet<String>();

		for (Map requestItem : requestItems) {
			String variantId = ParamUtil.getString(requestItem, AppParams.VARIANT_ID);
			String sizeId = ParamUtil.getString(requestItem, AppParams.SIZE_ID);

			Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);

			String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);

			result.add(baseId);

		}
		return result;
	}

	public static Set<String> getSetBaseIdFromItemCustom(List<Map> requestItems) {
		Set<String> result = new HashSet<String>();
		for (Map requestItem : requestItems) {
			String baseId = ParamUtil.getString(requestItem, AppParams.BASE_ID);
			result.add(baseId);
		}
		return result;
	}
	
	public static Set<String> getSetBaseIdFromJsonObj(JSONArray requestItems) throws SQLException {
		
		Set<String> result = new HashSet<String>();
		int countItems = requestItems.length();
		
		for (int i = 0; i < countItems; i++) {
			
			JSONObject vObj = requestItems.getJSONObject(i);
			
			if (vObj.has("is_map") && vObj.get("is_map") instanceof Boolean) {
				LOGGER.info("Has key is_map");
				boolean is_map = vObj.getBoolean("is_map");
				if (is_map) {
					LOGGER.info("is_map: " + is_map);
					String baseId = vObj.getString(AppParams.BASE_ID);
					result.add(baseId);
				}
			} else {
				LOGGER.info("No key is_map");
				String[] lineSku = vObj.getString("sku").split("\\|");
				String variantId = lineSku[0];
				String sizeId = lineSku[1];
				Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);
				
				if (!variantMap.isEmpty()) {
					String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
					result.add(baseId);
				}
			}
		}
		
		return result;
	}
	
	public static Set<String> getSetBaseIdFromTransactions(List<Map> transactions) throws SQLException {
		
		Set<String> result = new HashSet<String>();
		
		for (Map singleTransactions : transactions) {
			
			Map productData = (Map) singleTransactions.get("product_data");
			String sku = ParamUtil.getString(productData, AppParams.SKU);
			
			if (sku.startsWith("BG")) {
				LOGGER.info("sku from transaction: " + sku);
				String[] lineSku = sku.split("\\|");
				String variantId = lineSku[0];
				String sizeId = lineSku[1];

				Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);
				
				if (!variantMap.isEmpty()) {
					String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
					result.add(baseId);
				}
			}
		}
		
		return result;
	}
	
	private static final Logger LOGGER = Logger.getLogger(OrderUtil.class.getName());

	public static Set<String> getSetBaseFromItemCampApi(List<DropshipCustomApiItem> items) throws SQLException {
		Set<String> result = new HashSet<String>();
		for (DropshipCustomApiItem item : items) {
			String sku = item.getSku();
			String variantId = sku.split("\\|")[0];
			String sizeId = sku.split("\\|")[1];
			Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);
			String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);

			result.add(baseId);
		}

		return result;
	}

	public static Set<String> getSetBaseFromItemCustompApi(List<DropshipCustomApiItem> items) throws SQLException {
		Set<String> result = new HashSet<String>();
		for (DropshipCustomApiItem item : items) {
			DropshipBaseSkuObj baseSku = DropshipBaseSkuService.getBySku(item.getSku());
			String baseId = baseSku.getBaseId();

			result.add(baseId);
		}

		return result;
	}
	
	@Deprecated
	public static Double getTaxByCountry(Double orderAmount , String countryCode) throws SQLException {
		
		Double taxAmount = 0.00d;
		
		Map countryTax = CountryTaxService.getTaxByCountry(countryCode);
		
		if (MapUtils.isEmpty(countryTax)) {
			logger.info("Country --" + countryCode + " has not been defined for taxes ");
			return taxAmount;
		} else {
			String type = ParamUtil.getString(countryTax, AppParams.S_TYPE);
			Double tax = ParamUtil.getDouble(countryTax, AppParams.S_TAX , 0.00d);
			if ("percent".equalsIgnoreCase(type)) {
				taxAmount = orderAmount/100*tax;
			} else if ("fix".equalsIgnoreCase(type)) {
				taxAmount = tax;
			}
		}
		
		taxAmount = GetterUtil.format(taxAmount, 2);
		return taxAmount;
	}

	public static Double getTaxByAmountAndByCountry(Double orderAmount , Map countryTax) throws SQLException {

		Double taxAmount = 0.00d;
		if (MapUtils.isEmpty(countryTax)) {
			logger.info("Country has not been defined for taxes ");
			return taxAmount;
		} else {
			String type = ParamUtil.getString(countryTax, AppParams.S_TYPE);
			Double tax = ParamUtil.getDouble(countryTax, AppParams.S_TAX , 0.00d);
			if ("percent".equalsIgnoreCase(type)) {
				taxAmount = orderAmount/100*tax;
			} else if ("fix".equalsIgnoreCase(type)) {
				taxAmount = tax;
			}
		}
		taxAmount = GetterUtil.format(taxAmount, 2);
		return taxAmount;
	}
}
