/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.Address;
import asia.leadsgen.psp.obj.DropshipCampApiOrder;
import asia.leadsgen.psp.obj.DropshipCustomApiItem;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.DropshipOrderProductTypeObj;
import asia.leadsgen.psp.obj.DropshipOrderTypeObj;
import asia.leadsgen.psp.obj.DropshipStoreObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.service_fulfill.DropshipOrderServiceV2;
import asia.leadsgen.psp.util.OrderUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import asia.leadsgen.psp.external.api.SSPApiConnector;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service_fulfill.BaseSizeService;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service.ProductService;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.IsoUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author liamle
 */
public class DropshipCampApiOrderCreateHandler extends PSPOrderHandler implements Handler<RoutingContext> {

	public static final String IGNORE_ADDRESS_CHECK_NOTE = "Seller agree for bypass address verified";

	private Map<String, ItemMapper> orderItemVariantMapper = null;

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {
				String requestString = routingContext.getBodyAsJson().encode();

				DropshipCampApiOrder order = null;

				Response response = new Response(true, "Order was added successfully", 200);

				if (response.getSuccess() && StringUtils.isNotEmpty(requestString)) {
					order = new Gson().fromJson(requestString, DropshipCampApiOrder.class);
				} else {
					response = new Response(false,
							"Bad Request: Sorry there was an error processing your order. Please contact support", 400);
				}

				if (response.getSuccess() && (StringUtils.isEmpty(order.getApiKey())
						|| !DropShipStoreService.isExistThisApiKey(order.getApiKey()))) {
					response = new Response(false, "Failed to authenticate.", 401);
				}

				if (response.getSuccess()) {
					response = checkAddress(response, order);
				}

				String orderMiddle = "";
				if (response.getSuccess() && CollectionUtils.isEmpty(order.getItems())) {
					response = new Response(false, "Order items can not be empty.", 400);
				} else if (!OrderUtil.checkValidIossNumber(order.getIossNumber())) {
					response = new Response(false, "Invalid IOSS number", 400);
				} else if (response.getSuccess()) {

					orderItemVariantMapper = new HashMap<>();
					for (int i = 0; i < order.getItems().size(); i++) {
						DropshipCustomApiItem orderItem = order.getItems().get(i);
						if (response.getSuccess() && StringUtils.isEmpty(orderItem.getSku())) {
							response = new Response(false, String.format("order items[%d] : missing sku.", i,
									order.getItems().get(i).getSku()), 400);
							break;
						}

						if (response.getSuccess()) {
							response = isAValidSku(response, i, orderItem.getSku(), order.getApiKey());
							if (!response.getSuccess()) {
								break;
							}
						}

						orderMiddle = orderItemVariantMapper.get(orderItem.getSku()).getCampaignId() + "-"
								+ orderItemVariantMapper.get(orderItem.getSku()).getBaseShortCode();

						if (response.getSuccess() && isDuplicateSku(orderItem.getSku(), order.getItems())) {
							response = new Response(false,
									String.format("order items[%d] : can not add sku %s in more than one order item.",
											i, orderItem.getSku()),
									400);
							break;
						}

						if (response.getSuccess()
								&& (orderItem.getQuantity() == null || orderItem.getQuantity() <= 0)) {
							response = new Response(false, String.format("order items[%d] : quantity is invalid.", i),
									400);
							break;
						}
					}

				}

				String orderId = null;
				if (response.getSuccess() && order.getSandbox() == false) {
					DropshipStoreObj store = DropShipStoreService.findByApiKey(order.getApiKey());
					if (StringUtils.isNotEmpty(order.getReferenceOrderId())) {
						if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(store.getId(),
								order.getReferenceOrderId(), ResourceSource.CAMP_API)) {
							response = new Response(false,
									String.format("Order %s is exist!.", order.getReferenceOrderId()), 400);
						}
					}
					if (response.getSuccess()) {
						orderId = createOrder(store, order.getReferenceOrderId(), response.getShippingId(), orderMiddle,
								order);
					}
				} else {
					orderId = "ASAMPLE-FQ79-16899";
				}

				Map responseM = new HashMap<String, Object>();
				responseM.put("is_success", response.getSuccess());
				responseM.put("message", response.getMessage());

				if (response.getSuccess()) {
					responseM.put("order_id", orderId);
				}

				String reasonPhase = response.getCode().intValue() == 200 ? HttpResponseStatus.OK.reasonPhrase()
						:HttpResponseStatus.BAD_REQUEST.reasonPhrase();

				routingContext.put(AppParams.RESPONSE_CODE, response.getCode());
				routingContext.put(AppParams.RESPONSE_MSG, reasonPhase);
				routingContext.put(AppParams.RESPONSE_DATA, responseM);

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

	private boolean isDuplicateSku(String sku, List<DropshipCustomApiItem> items) {
		long count = items.parallelStream().filter(o -> o.getSku().equals(sku)).count();
		return count > 1;
	}

	private Response isAValidSku(Response response, int itemIdex, String sku, String apiKey) throws SQLException {
		if (!sku.contains("|") && !(sku.split("\\|").length == 2)) {
			return new Response(false, String.format("order items[%d] : sku %s is invalid.", itemIdex, sku), 400);
		}
//		LOGGER.info("pass !sku.contains(\"|\") && !(sku.split(\"\\\\|\").length == 2) ");

		DropshipStoreObj store = DropShipStoreService.findByApiKey(apiKey);
		String variantId = sku.split("\\|")[0];
		String sizeId = sku.split("\\|")[1];
		Map variantMap = ProductVariantService.getVariantMapByIdAndSizeId(variantId, sizeId);
		if (variantMap == null || variantMap.isEmpty()) {
			return new Response(false, String.format("order items[%d] : sku %s is invalid.", itemIdex, sku), 400);
		}

//		LOGGER.info("pass variantMap == null || variantMap.isEmpty()");

		String campaignId = ParamUtil.getString(variantMap, AppParams.CAMPAIGN_ID);
		if (StringUtils.isEmpty(campaignId) || !campaignId.startsWith(store.getUserId())) {
			return new Response(false, String.format("order items[%d] : you can not use sku %s .", itemIdex, sku), 400);
		}

//		LOGGER.info("pass StringUtils.isEmpty(campaignId) || !campaignId.startsWith(store.getUserId())");

		String campaignState = CampaignService.getCampaignState(campaignId);
		if (StringUtils.isEmpty(campaignState) || ResourceStates.LOCKED.equalsIgnoreCase(campaignState)) {
			return new Response(false,
					String.format("order items[%d] : sku %s is invalid due to campaign %s was locked.", itemIdex, sku,
							campaignId),
					400);
		}

//		LOGGER.info("pass StringUtils.isEmpty(campaignState) || ResourceStates.LOCKED.equalsIgnoreCase(campaignState)");

		String variantName = ParamUtil.getString(variantMap, AppParams.NAME);
		String productId = ParamUtil.getString(variantMap, AppParams.PRODUCT_ID);
		String baseId = ParamUtil.getString(variantMap, AppParams.BASE_ID);
		double baseCost = ParamUtil.getDouble(variantMap, AppParams.BASE_COST);
		String baseShortCode = ParamUtil.getString(variantMap, AppParams.BASE_SHORT_CODE);
		String sizeName = ParamUtil.getString(variantMap, AppParams.SIZE_NAME);
		String colorId = ParamUtil.getString(variantMap, AppParams.COLOR_ID);
		String colorName = ParamUtil.getString(variantMap, AppParams.COLOR_NAME);
		String colorValue = ParamUtil.getString(variantMap, AppParams.COLOR);
		Map img = ParamUtil.getMapData(variantMap, AppParams.IMAGE);
		String imgFrontUrl = ParamUtil.getString(img, AppParams.FRONT);
		String imgBackUrl = ParamUtil.getString(img, AppParams.BACK);
		String designFrontUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_FRONT_URL);
		String designBackUrl = ParamUtil.getString(variantMap, AppParams.DESIGN_BACK_URL);

		if (BaseSizeService.checkAvailabilityForBase(sizeId, baseId) == false) {
			return new Response(false, String.format("order items[%d] : sku %s is invalid.", itemIdex, sku), 400);
		}

//		LOGGER.info("pass BaseSizeService.checkAvailabilityForBase(sizeId, baseId)");

		ItemMapper itemMapper = new ItemMapper(variantId, variantName, productId, campaignId, baseId, baseCost, baseShortCode,
				sizeId, sizeName, colorId, colorName, colorValue, imgFrontUrl, imgBackUrl, designFrontUrl, designBackUrl);
		orderItemVariantMapper.put(sku, itemMapper);
		return response;

	}

	private String createOrder(DropshipStoreObj store, String referenceOrderId, String shippingId, String orderPrefix,
							   DropshipCampApiOrder orderRequest) throws SQLException, ParseException {

		DropshipOrderTypeObj dropshipOrder = DropshipOrderTypeObj.builder()
				.idPrefix(orderPrefix)
				.currency("USD")
				.state(ResourceStates.QUEUED)
				.shippingId(shippingId)
				.trackingCode(AppUtil.generateOrderTrackingNumber())
				.note(orderRequest.getApiKey())
				.channel("api")
				.source(ResourceSource.CAMP_API)
				.storeId(store.getId())
				.userId(store.getUserId())
				.referenceOrder(referenceOrderId)
				.minifiedJson(new Gson().toJson(orderRequest))
				.iossNumber(orderRequest.getIossNumber())
				.build();

		Map savedOrder = DropshipOrderServiceV2.insertDropshipOrderV2(dropshipOrder);
		String savedOrderId = ParamUtil.getString(savedOrder, AppParams.ID);

		initItemGroupQuantity();

		Double orderTotal = 0d;
		Double orderSubTotal = 0d;
		Double orderShippingTotal = 0d;
		int totalItems = 0;

		Set<String> setBaseId = OrderUtil.getSetBaseFromItemCampApi(orderRequest.getItems());

		Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, orderRequest.getShippingCountry(), AppParams.STANDARD);

		Map countryTax = CountryTaxService.getTaxByCountry(orderRequest.getShippingCountry());

		Double totalTax = 0d;

		for (DropshipCustomApiItem item : orderRequest.getItems()) {

			ItemMapper itemMapper = orderItemVariantMapper.get(item.getSku());
			Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, itemMapper.getBaseId(), AppParams.STANDARD, item.getQuantity(), shippingInfo);
			Double shippingFee = ParamUtil.getDouble(feeMap, AppParams.SHIPPING_FEE);

			double productSubTotal = itemMapper.getBaseCost() * item.getQuantity();
			double productTotal = GetterUtil.format(productSubTotal + shippingFee, 2);
			LOGGER.info("+++productAmount = " + productTotal);
			Double taxRate =0d;
			Double taxAmount =0d;
			if (StringUtils.isEmpty(orderRequest.getIossNumber())) {
				taxAmount = OrderUtil.getTaxByAmountAndByCountry(productTotal, countryTax);
				taxRate=OrderUtil.getTaxRateFromCountryTax(countryTax);
			}

			productTotal = GetterUtil.format(productTotal + taxAmount, 2);
			LOGGER.info("+++taxAmount = " + taxAmount + ", taxRate = " + taxRate);

			LOGGER.info("api create savedOrderId=" + savedOrderId);
			DropshipOrderProductTypeObj dropshipOrderProduct = DropshipOrderProductTypeObj.builder()
					.orderId(savedOrderId)
					.quantity(item.getQuantity())
					.campaignId(itemMapper.getCampaignId())
					.productId(itemMapper.getProductId())
					.variantId(itemMapper.getId())
					.variantName(itemMapper.getName())
					.sizeId(itemMapper.getSizeId())
					.baseId(itemMapper.getBaseId())
					.sizeName(itemMapper.getSizeName())
					.colorId(itemMapper.getColorId())
					.colorName(itemMapper.getColorName())
					.colorValue(itemMapper.getColorValue())
					.variantFrontUrl(itemMapper.getImgFrontUrl())
					.variantBackUrl(itemMapper.getImgBackUrl())
					.designFrontUrl(itemMapper.getDesignFrontUrl())
					.designBackUrl(itemMapper.getDesignBackUrl())
					.state(ResourceStates.APPROVED)
					.price(String.valueOf(itemMapper.getBaseCost()))
					.baseCost(String.valueOf(itemMapper.getBaseCost()))
					.amount(String.valueOf(productTotal))
					.shippingFee(String.valueOf(shippingFee))
					.shippingMethod(AppParams.STANDARD)
					.taxAmount(String.valueOf(taxAmount))
					.taxRate(String.valueOf(taxRate))
					.build();

			DropshipOrderProductService.insertDropshipOrderProductV2(dropshipOrderProduct);

			orderTotal += productTotal;
			orderSubTotal += productSubTotal;
			orderShippingTotal += shippingFee;
			totalItems += item.getQuantity();
			totalTax += taxAmount;

		}

		String addressCheckMessage = "";

		if (orderRequest.getIgnoreAddressCheck()) {
			addressCheckMessage = IGNORE_ADDRESS_CHECK_NOTE;
		}
		orderTotal = GetterUtil.format(orderTotal, 2);
		totalTax = GetterUtil.format(totalTax, 2);
		DropshipOrderService.updateQuantityAmountAddressCheck(savedOrderId, orderTotal, orderSubTotal,
				orderShippingTotal, totalItems, 1, addressCheckMessage, totalTax.toString());

		return savedOrderId;
	}

	private Response checkAddress(Response response, DropshipCampApiOrder order) throws SQLException {

		if (response.getSuccess() && (order != null && StringUtils.isEmpty(order.getShippingName()))) {
			response = new Response(false, "shipping_name can not be empty.", 400);
		}

		// shipping_address1
		if (response.getSuccess() && (order != null && StringUtils.isEmpty(order.getShippingAddress1()))) {
			response = new Response(false, "shipping_address1 can not be empty.", 400);
		}

		// shipping_address2
//		if (response.getSuccess() && (order != null && StringUtils.isEmpty(order.getShippingAddress2()))) {
//			response = new Response(false, "shipping_address2 can not be empty.", 400);
//		}

		// shipping_city
		if (response.getSuccess() && (order != null && StringUtils.isEmpty(order.getShippingCity()))) {
			response = new Response(false, "shipping_city can not be empty.", 400);
		}
//				shipping_state
		if (response.getSuccess() && (order != null && StringUtils.isEmpty(order.getShippingState()))) {
			response = new Response(false, "shipping_state can not be empty.", 400);
		}
//				shipping_zip
		if (response.getSuccess() && (order != null && StringUtils.isEmpty(order.getShippingZip()))) {
			response = new Response(false, "shipping_zip can not be empty.", 400);
		}
//				shipping_country
		if (response.getSuccess() && (order != null && StringUtils.isEmpty(order.getShippingCountry()))) {
			response = new Response(false, "shipping_country can not be empty.", 400);
		}

//		shipping_country iso
		if (response.getSuccess() && !IsoUtil.isValidISOCountry(order.getShippingCountry())) {
			response = new Response(false,
					"shipping_country must is ISO Alpha-2 code (https://www.nationsonline.org/oneworld/country_code_list.htm).",
					400);
		}

//				shipping_email
//		if (response.getSuccess() && (order != null && StringUtils.isEmpty(order.getShippingEmail())
//				|| !EmailValidator.getInstance().isValid(order.getShippingEmail()))) {
//			response = new Response(false, "shipping_email is invalid.", 400);
//		}
//				shipping_phone

//		if (response.getSuccess() && (order != null && StringUtils.isEmpty(order.getShippingPhone()))) {
//			response = new Response(false, "shipping_phone can not be empty.", 400);
//		}

		if (response.getSuccess() && order.getIgnoreAddressCheck() == false
				&& "US".equalsIgnoreCase(order.getShippingCountry())) {
			Map verifyResult = SSPApiConnector
					.verifyAddress(new Address(order.getShippingName(), order.getShippingAddress1(),
							order.getShippingAddress2(), order.getShippingCity(), order.getShippingState(),
							order.getShippingZip(), order.getShippingCountry(), order.getShippingPhone()))
					.getMap();
			if (ParamUtil.getBoolean(verifyResult, "success") == false) {
				String message = "Address verification failed.";
				List<Map> reasons = ParamUtil.getListData(verifyResult, "reason");
				if (CollectionUtils.isNotEmpty(reasons)) {
					message = ParamUtil.getString(reasons.get(0), "message");
				}
				response = new Response(false, message, 400);
			}
		}

		if (response.getSuccess()) {
			Map shippingMap = ShippingService.insert(order.getShippingName(), order.getShippingEmail(),
					order.getShippingPhone(), order.getShippingAddress1(), order.getShippingAddress2(),
					order.getShippingCity(), order.getShippingState(), order.getShippingZip(),
					order.getShippingCountry(), "");
			response.setShippingId(ParamUtil.getString(shippingMap, AppParams.ID));
		}

		return response;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	class Response {
		private Boolean success;
		private String message;
		private Integer code;

		public Response(Boolean success, String message, Integer code) {
			this.success = success;
			this.message = message;
			this.code = code;
		}

		public Response(Boolean success) {
			this.success = success;
		}

		private String shippingId;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	class ItemMapper {
		private String id;
		private String name;
		private String productId;
		private String campaignId;
		private String baseId;
		private double baseCost;
		private String baseShortCode;
		private String sizeId;
		private String sizeName;
		private String colorId;
		private String colorName;
		private String colorValue;
		private String imgFrontUrl;
		private String imgBackUrl;
		private String designFrontUrl;
		private String designBackUrl;

	}

	private static final Logger LOGGER = Logger.getLogger(DropshipCampApiOrderCreateHandler.class.getName());

}
