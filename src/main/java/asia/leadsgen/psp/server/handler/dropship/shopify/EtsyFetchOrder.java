package asia.leadsgen.psp.server.handler.dropship.shopify;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.github.scribejava.core.model.OAuth1AccessToken;

import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service_fulfill.EtsyService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.OrderUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;

public class EtsyFetchOrder extends PSPOrderHandler {

	private static OAuth1AccessToken accessToken;

	public Map getEtsyOrderByShopId(String token, String tokenSecret, String storeId,
									String userId, String etsyStoreId, String date, String status) throws Exception {
		OAuth1AccessToken accessToken = new OAuth1AccessToken(token, tokenSecret);
		EtsyService etsyService = new EtsyService(accessToken);
		int _date = Integer.parseInt(date);

		long DAY_IN_MS = 1000 * 60 * 60 * 24;
		Date createdDate = new Date(System.currentTimeMillis() - (_date * DAY_IN_MS));
		long dateInEpoch = createdDate.getTime() / 1000;

		String etsyStatus = (status.equalsIgnoreCase("unfulfilled")) ? "open":"completed";

		Map data = new HashMap();
		Map orders = etsyService.getService("/shops/" + etsyStoreId + "/receipts/" + etsyStatus);


		List<Map> resultOrders = (List<Map>) orders.get("results");
		LOGGER.info("TOTAL ORDER GET FROM API : " + resultOrders.size());

		if (!resultOrders.isEmpty()) {

			List<Map> newOrders = etsyService.filterOrderByDate(storeId, resultOrders, dateInEpoch);

			for (Map singleOrder : newOrders) {
				initItemGroupQuantity();
				insertEtsyOrder(singleOrder, storeId, userId, etsyService);
			}

			data.put(AppParams.RESULT_MSG, "TOTAL ORDER SYNC SUCCESSFULLY:" + newOrders.size());
			return data;
		}

		data.put(AppParams.RESULT_MSG, "TOTAL ORDER SYNC SUCCESSFULLY:" + 0);
		return data;
	}

	public void insertEtsyOrder(Map singleOrder, String storeId, String userId, EtsyService etsyService) throws Exception {

		String ref_id = ParamUtil.getString(singleOrder, "receipt_id");
		String countryId = ParamUtil.getString(singleOrder, "country_id");
		String[] countryInfo = convertToCountry(etsyService, countryId);
		String countryCode = countryInfo[0];
		String countryName = countryInfo[1];

		Map transaction = etsyService.getService("/receipts/" + ref_id + "/transactions");
		List<Map> transactions = (List<Map>) transaction.get("results");

		String orderId = createDropshipOrder(transactions, singleOrder, storeId, userId, countryCode, countryName);

		int totalItems = 0;
		double orderTotal = 0.00;
		double orderShippingTotal = 0.00d;
		double orderSubTotal = 0.00d;
		int addressVerified = 0;
		Double totalTax = 0d;
		
		Set<String> setBaseId = OrderUtil.getSetBaseIdFromTransactions(transactions);
		
		Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, countryCode,  AppParams.STANDARD);

		Map countryTax = CountryTaxService.getTaxByCountry(countryCode);

		for (Map singleTransactions : transactions) {
			Map orderItem = createDropshipOrderProduct(singleTransactions, orderId, userId, countryCode, shippingInfo, countryTax);
			double price = ParamUtil.getDouble(orderItem, AppParams.PRICE);
			int quantity = GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
			orderTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.AMOUNT));
			orderSubTotal += (quantity * price);
			orderShippingTotal += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE));
			totalItems += GetterUtil.getInteger(ParamUtil.getInt(orderItem, AppParams.QUANTITY));
			totalTax += GetterUtil.getDouble(ParamUtil.getString(orderItem, AppParams.TAX_AMOUNT));
		}

		String addressCheckMessage = "";
		if (addressVerified > 1) {
			addressCheckMessage = "Seller agree for bypass address verified";
		}

		orderTotal = GetterUtil.format(orderTotal, 2);
		totalTax = GetterUtil.format(totalTax, 2);
		
		DropshipOrderService.updateQuantityAmountAddressCheck(orderId, orderTotal, orderSubTotal,
				orderShippingTotal, totalItems, addressVerified, addressCheckMessage, totalTax.toString());

	}

	public static String createDropshipOrder(List<Map> transactions, Map singleOrder, String storeId, String userId, String countryCode, String countryName) throws IOException, SQLException, ParseException {
		String referenceOrderId = ParamUtil.getString(singleOrder, "receipt_id");

		Double totalAmount = ParamUtil.getDouble(singleOrder, "grandtotal");
//        Double subAmount = ParamUtil.getDouble(singleOrder, "subtotal");
//        Double shippingFee = ParamUtil.getDouble(singleOrder, "total_shipping_cost");

		String customerName = ParamUtil.getString(singleOrder, "name");
		String line1 = ParamUtil.getString(singleOrder, "first_line");
		String line2 = ParamUtil.getString(singleOrder, "second_line");
		String city = ParamUtil.getString(singleOrder, "city");
		String state = ParamUtil.getString(singleOrder, "state");
		String zipCode = ParamUtil.getString(singleOrder, "zip");
		String email = ParamUtil.getString(singleOrder, "buyer_email");

//            Boolean isGift = ParamUtil.getBoolean(singleOrder, "is_gift");

		Map shipping = ShippingService.insert(customerName,
				email,
				"",
				line1,
				line2,
				city,
				state,
				zipCode,
				countryCode,
				countryName);

		String shippingId = ParamUtil.getString(shipping, AppParams.ID);

		String source = ResourceSource.CAMP_SYNC;
		String resourceState = ResourceStates.QUEUED;
		String orderPrefix = userId + "-ES";

		Map productData = ParamUtil.getMapData(transactions.get(0), "product_data");
		String sku = ParamUtil.getString(productData, AppParams.SKU);

		if ((StringUtils.isEmpty(sku) || !sku.startsWith("BG"))) {
			orderPrefix = userId + "-" + "CT";
			source = ResourceSource.CUSTOM_SYNC;
			resourceState = ResourceStates.DRAFT;
		}

		DropshipOrderObj dropshipOrderObj = new DropshipOrderObj.Builder(orderPrefix)
				.orderAmount(totalAmount)
				.orderCurrency("USD")
				.state(resourceState)
				.shippingId(shippingId)
				.trackingNumber(AppUtil.generateOrderTrackingNumber())
				.channel(AppConstants.ETSY)
				.storeId(storeId)
				.userId(userId)
				.referenceOrderId(referenceOrderId)
				.source(source)
				.addrVerified(0)
				.build();

		Map dropshipOrder = DropshipOrderService.insertDropshipOrder(dropshipOrderObj);
		String orderId = ParamUtil.getString(dropshipOrder, AppParams.ID);
		return orderId;
	}

	public Map createDropshipOrderProduct(Map singleTransactions, String orderId, String userId, String countryCode, Map shippingInfo, Map countryTax) throws SQLException {

		Map productData = (Map) singleTransactions.get("product_data");
		String sku = ParamUtil.getString(productData, AppParams.SKU);

		Map orderItem = new LinkedHashMap<>();
		if (StringUtils.isEmpty(sku) || !sku.startsWith("BG")) {

			DropshipOrderProductObj dropshipOrderProductObj = new DropshipOrderProductObj.Builder(orderId)
					.campaignId(userId + "-")
					.variantName(ParamUtil.getString(singleTransactions, AppParams.TITLE))
					.price(ParamUtil.getDouble(singleTransactions, AppParams.PRICE))
					.currency(ParamUtil.getString(singleTransactions, "currency_code"))
					.quantity(ParamUtil.getInt(singleTransactions, AppParams.QUANTITY))
					.build();
			orderItem = DropshipOrderProductService.insertDropshipOrderProduct(dropshipOrderProductObj);

		} else {

			String[] lineSku = sku.split("\\|");
			String variantId = lineSku[0];
			String sizeId = lineSku[1];

			Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);

			if (!variantMap.isEmpty()) {
				String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
				double baseCost = ParamUtil.getDouble(variantMap, AppParams.BASE_COST);
				String baseShortCode = ParamUtil.getString(variantMap, AppParams.BASE_SHORT_CODE);

				String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);
				String campaignId = ParamUtil.getString(variantMap, AppParams.CAMPAIGN_ID);
				String variantName = ParamUtil.getString(variantMap, AppParams.NAME);

				String colorId = ParamUtil.getString(variantMap, AppParams.COLOR_ID);
				String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
				String colorValue = ParamUtil.getString(variantMap, AppParams.COLOR);
				String sizeName = ParamUtil.getString(variantMap, AppParams.SIZE_NAME);

				Map image = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
				String variantFrontUrl = ParamUtil.getString(image, AppParams.FRONT);
				String variantBackUrl = ParamUtil.getString(image, AppParams.BACK);

				String designFrontUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_FRONT_URL);
				String designBackUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_BACK_URL);

				int quantity = ParamUtil.getInt(singleTransactions, AppParams.QUANTITY);

//				Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, AppParams.STANDARD, baseId, countryCode, quantity);
				Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseId, AppParams.STANDARD, quantity, shippingInfo);
				Double shippingFee = ParamUtil.getDouble(feeMap, AppParams.SHIPPING_FEE);

				double productAmount = GetterUtil.format(baseCost * quantity + shippingFee, 2);
				LOGGER.info("+++productAmount = " + productAmount);
				Double taxAmount = OrderUtil.getTaxByAmountAndByCountry(productAmount,countryTax);
				productAmount = GetterUtil.format(productAmount + taxAmount, 2);
				LOGGER.info("+++taxAmount = " + taxAmount);

				DropshipOrderProductObj dropshipOrderProductObj = new DropshipOrderProductObj.Builder(orderId)
						.campaignId(campaignId)
						.productId(productId)
						.variantId(variantId)
						.sizeId(sizeId)
						.price(baseCost)
						.shippingFee(shippingFee)
						.currency(ParamUtil.getString(singleTransactions, "currency_code"))
						.quantity(quantity)
						.state(ResourceStates.APPROVED)
						.variantName(variantName)
						.amount(productAmount)
						.baseCost(baseCost)
						.baseId(baseId)
						.lineItemId("")
						.variantFrontUrl(variantFrontUrl)
						.variantBackUrl(variantBackUrl)
						.colorId(colorId)
						.colorValue(colorValue)
						.colorName(colorName)
						.sizeName(sizeName)
						.shippingMethod(AppParams.STANDARD)
						.itemType(ResourceStates.NORMAL)
						.baseShortCode(baseShortCode)
						.designFrontUrl(designFrontUrl)
						.designBackUrl(designBackUrl)
						.taxAmount(taxAmount)
						.build();

				orderItem = DropshipOrderProductService.insertDropshipOrderProduct(dropshipOrderProductObj);
			}
		}

		return orderItem;
	}

	public static String[] convertToCountry(EtsyService etsyService, String countryId) throws IOException {
		Map getResult = etsyService.getService("countries/" + countryId);
		List results = (List) getResult.get("results");
		Map country = (Map) results.get(0);
		String countryCode = ParamUtil.getString(country, "iso_country_code");
		String countryName = ParamUtil.getString(country, "name");
		return new String[]{countryCode, countryName};
	}

	private static final Logger LOGGER = Logger.getLogger(EtsyFetchOrder.class.getName());
}

