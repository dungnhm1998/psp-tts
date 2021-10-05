/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import asia.leadsgen.psp.obj.DropshipOrderProductTypeObj;
import asia.leadsgen.psp.obj.DropshipOrderTypeObj;
import asia.leadsgen.psp.service_fulfill.BaseService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderServiceV2;
import asia.leadsgen.psp.util.OrderUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import com.google.gson.Gson;

import asia.leadsgen.psp.external.api.ISPApiConnector;
import asia.leadsgen.psp.external.api.SSPApiConnector;
import asia.leadsgen.psp.obj.Address;
import asia.leadsgen.psp.obj.BaseSKUObj;
import asia.leadsgen.psp.obj.DropshipBaseSkuObj;
import asia.leadsgen.psp.obj.DropshipCustomApiItem;
import asia.leadsgen.psp.obj.DropshipCustomApiOrder;
import asia.leadsgen.psp.obj.DropshipOrderObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.DropshipStoreObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service.ShippingService;
import asia.leadsgen.psp.service_fulfill.BaseSKUService;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipBaseSkuService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
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
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author liamle
 *
 */
public class DropshipCustomApiOrderCreateHandler extends PSPOrderHandler implements Handler<RoutingContext> {

	static final String _2D_SHIRT_SKU_REGEX = "^([0-9]{3}-[0-9]{3}-[0-9]{3})$";
	public static final String IGNORE_ADDRESS_CHECK_NOTE = "Seller agree for bypass address verified";
	public static final String POSTER_SKU_REGEX = "^matte-poster-(?:11x17|16x24|17x11|24x16|24x36|36x24)$";
	public static final String MUG_SKU_REGEX = "^(beverage-mug\\|(?:11oz|15oz)\\|(?:white|black))|(color-changing-beverage-mug\\|11oz\\|white)$";
	public static final String LADIES_RACERBACK_TANK_REGEX = "^007-00(?:2|3|4)-00(?:1|2|3|4|5)$";
	public static final String _3D_ALLOW_2SIDE_DESIGNS = "^(?:MJK|WY|LMS|BX|TX|TK|ZIP)-(?:XS|S|M|L|XL|(?:2|3|4|5)XL)|(?:LMS|WY|TX|ZIP)KID-(?:S|M|L|XL)$";

	private DropshipStoreObj store = null;
	private Map<String, BaseSKUObj> orderItemSkuMapper = null;

//	public static HashMap<String, String> POSTER_SIZES = new HashMap<String, String>() {
//		private static final long serialVersionUID = 1L;
//		{
//			put("24x36", "b21ONbnqa2A2nqb3");
//			put("16x24", "Fol2t8BILYRs5Yd3");
//			put("36x24", "oaIyIxmJkbbjXa1x");
//			put("24x16", "qnWaRanakXN8uNs3");
//			put("11x17", "v51OL8T7ts5Idyas");
//			put("17x11", "UspJoEKw3ef9hatq");
//		}
//	};

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {
				String requestString = routingContext.getBodyAsJson().encode();

				DropshipCustomApiOrder orderRequest = null;

				String[] schemes = { "http", "https" };

				String[] imageSuffixs = { ".jpg", ".JPG", ".png", ".PNG", ".JPEG", ".jpeg" };

				UrlValidator urlValidator = new UrlValidator(schemes);

				Response response = new Response(true, "Order was added successfully", 200);

				if (response.getSuccess() && StringUtils.isNotEmpty(requestString)) {
					orderRequest = new Gson().fromJson(requestString, DropshipCustomApiOrder.class);
				} else {
					response = new Response(false, "Bad Request: Sorry there was an error processing your order. Please contact support", 400);
				}

				if (response.getSuccess() && StringUtils.isEmpty(orderRequest.getApiKey())) {
					response = new Response(false, "api_key can not be empty.", 401);
				}

				if (response.getSuccess()) {
					store = DropShipStoreService.findByApiKey(orderRequest.getApiKey());
					if (store == null) {
						response = new Response(false, "Failed to authenticate.", 401);
					}
				}

				if (response.getSuccess()) {
					response = checkAddress(response, orderRequest);
				}

				String firstItemBaseShortCode = "";
				if (response.getSuccess() && CollectionUtils.isEmpty(orderRequest.getItems())) {
					response = new Response(false, "Order items can not be empty.", 400);
				}else if (!OrderUtil.checkValidIossNumber(orderRequest.getIossNumber())) {
					response = new Response(false, "Invalid IOSS number", 400);
				}  else if (response.getSuccess()) {
					for (int i = 0; i < orderRequest.getItems().size(); i++) {

						DropshipCustomApiItem orderItem = orderRequest.getItems().get(i);

						if (response.getSuccess()
								&& (StringUtils.isEmpty(orderItem.getSku()) || matchSkuAndReturnIfSkuIsAvailable(orderItem.getSku()) == false)) {
							response = new Response(false, String.format("order items[%d] : sku %s is invalid.", i, orderItem.getSku()), 400);
							break;
						} else {
							if (StringUtils.isEmpty(firstItemBaseShortCode)) {
								firstItemBaseShortCode = BaseSKUService.getBySku(orderItem.getSku()).getBaseShortCode();
							}
						}

						if (response.getSuccess() && orderItem.getSku().matches(LADIES_RACERBACK_TANK_REGEX)) {
							if (StringUtils.isNotEmpty(orderItem.getDesignUrlBack()) || StringUtils.isNotEmpty(orderItem.getMockupUrlBack())) {
								response = new Response(false,
										String.format("order items[%d] : sku %s does not support back print.", i, orderItem.getSku()), 400);
								break;
							}
						}

						if (response.getSuccess() && StringUtils.isEmpty(orderItem.getDesignUrlFront())
								&& StringUtils.isEmpty(orderItem.getDesignUrlBack())) {
							response = new Response(false, String.format("order items[%d] : missing  design_url.", i), 400);
							break;
						}

						if (response.getSuccess()
								&& (StringUtils.isNotEmpty(orderItem.getDesignUrlFront()) || StringUtils.isNotEmpty(orderItem.getDesignUrlBack()))) {
							boolean designIsOk = false;
							String message = "";
							if (StringUtils.isNotEmpty(orderItem.getDesignUrlFront())) {
								if (urlValidator.isValid(orderItem.getDesignUrlFront())
										&& StringUtils.endsWithAny(orderItem.getDesignUrlFront(), imageSuffixs)) {
									if (StringUtils.isNotEmpty(orderItem.getMockupUrlFront()) && urlValidator.isValid(orderItem.getMockupUrlFront())
											&& StringUtils.endsWithAny(orderItem.getMockupUrlFront(), imageSuffixs)) {
										designIsOk = true;
									} else if (StringUtils.isBlank(orderItem.getMockupUrlFront())) {
										message = String.format("order items[%d] : missing mockup_url_front.", i);
										designIsOk = false;
									} else {
										message = String.format("order items[%d] : mockup_url_front is invalid.", i);
										designIsOk = false;
									}
								} else {
									message = String.format("order items[%d] : design_url_front is invalid.", i);
									designIsOk = false;
								}
							}

							if (StringUtils.isNotEmpty(orderItem.getDesignUrlBack())) {
								if (urlValidator.isValid(orderItem.getDesignUrlBack())
										&& StringUtils.endsWithAny(orderItem.getDesignUrlBack(), imageSuffixs)) {
									if (StringUtils.isNotEmpty(orderItem.getMockupUrlBack()) && urlValidator.isValid(orderItem.getMockupUrlBack())
											&& StringUtils.endsWithAny(orderItem.getMockupUrlBack(), imageSuffixs)) {
										designIsOk = true;
									} else if (StringUtils.isBlank(orderItem.getMockupUrlBack())) {
										message = String.format("order items[%d] : missing mockup_url_back.", i);
										designIsOk = false;
									} else {
										message = String.format("order items[%d] : mockup_url_back is invalid.", i);
										designIsOk = false;
									}
								} else {
									message = String.format("order items[%d] : design_url_back is invalid.", i);
									designIsOk = false;
								}
							}

							if (!designIsOk) {
								if (StringUtils.isEmpty(message)) {
									response = new Response(false,
											String.format("order items[%d] : there was an error while processing your design.", i), 400);
									break;
								} else {
									response = new Response(false, message, 400);
									break;
								}

							}
						} else {
							response = new Response(false, String.format("order items[%d] : missing  design_url.", i), 400);
							break;
						}

//						if (response.getSuccess() && (StringUtils.isEmpty(orderItem.getDesignUrlFront())
//								|| !urlValidator.isValid(orderItem.getDesignUrlFront())
//								|| !StringUtils.endsWithAny(orderItem.getDesignUrlFront(), imageSuffixs))) {
//							response = new Response(false,
//									String.format("order items[%d] : design_url_front is invalid.", i), 400);
//							break;
//						}
//						LOGGER.info("response = " + response.getSuccess());
//						LOGGER.info("orderItem.getMockupUrlFront() = " + orderItem.getMockupUrlFront());
//
//						if (response.getSuccess() && StringUtils.isNotBlank(orderItem.getDesignUrlFront())
//								&& (StringUtils.isBlank(orderItem.getMockupUrlFront())
//										|| !urlValidator.isValid(orderItem.getMockupUrlFront())
//										|| !StringUtils.endsWithAny(orderItem.getMockupUrlFront(), imageSuffixs))) {
//							response = new Response(false,
//									String.format("order items[%d] : mockup_url_front is invalid.", i), 400);
//							break;
//						}
//
//						if (response.getSuccess() && StringUtils.isNotEmpty(orderItem.getDesignUrlBack())
//								&& (!urlValidator.isValid(orderItem.getDesignUrlBack())
//										|| !StringUtils.endsWithAny(orderItem.getDesignUrlBack(), imageSuffixs))) {
//							response = new Response(false,
//									String.format("order items[%d] : design_url_back is invalid.", i), 400);
//							break;
//						}
//
//						if (response.getSuccess() && StringUtils.isEmpty(orderItem.getDesignUrlBack())
//								&& StringUtils.isNotEmpty(orderItem.getMockupUrlBack())) {
//							response = new Response(false,
//									String.format("order items[%d] : missing  design_url_back.", i), 400);
//							break;
//						}
//
//						if (response.getSuccess() && StringUtils.isNotBlank(orderItem.getDesignUrlBack())
//								&& (StringUtils.isBlank(orderItem.getMockupUrlBack())
//										|| !urlValidator.isValid(orderItem.getMockupUrlBack())
//										|| !StringUtils.endsWithAny(orderItem.getMockupUrlBack(), imageSuffixs))) {
//							response = new Response(false,
//									String.format("order items[%d] : mockup_url_back is invalid.", i), 400);
//							break;
//						}

						if (response.getSuccess()
								&& !(orderItem.getSku().matches(_2D_SHIRT_SKU_REGEX) || orderItem.getSku().matches(_3D_ALLOW_2SIDE_DESIGNS))
								&& StringUtils.isNotEmpty(orderItem.getDesignUrlBack())) {
							response = new Response(false,
									String.format("order items[%d] : unable to process design_url_back for %s.", i, orderItem.getSku()), 400);
							break;
						}

						if (response.getSuccess()
								&& !(orderItem.getSku().matches(_2D_SHIRT_SKU_REGEX) || orderItem.getSku().matches(_3D_ALLOW_2SIDE_DESIGNS))
								&& StringUtils.isNotEmpty(orderItem.getMockupUrlBack())) {
							response = new Response(false,
									String.format("order items[%d] : unable to process mockup_url_back for %s.", i, orderItem.getSku()), 400);
							break;
						}

						if (response.getSuccess()) {
							BaseSKUObj matchedSku = orderItemSkuMapper.get(orderItem.getSku());
							CheckDesignsResponse checkDesignsResponse = ISPApiConnector.checkDesignUrls(store.getUserId(), matchedSku.getBaseId(), orderItem.getDesignUrlFront(), orderItem.getDesignUrlBack());

							if (checkDesignsResponse.getIsValid() == false && checkDesignsResponse.getDesignFront() != null) {

								if (checkDesignsResponse.getDesignFront().getIsSuccessDownload() != null
										&& checkDesignsResponse.getDesignFront().getIsSuccessDownload() == false) {
									response = new Response(false, String.format("order items[%d] : can not download design_url_front.", i), 400);
									break;
								} else if (checkDesignsResponse.getDesignFront().getIsValid() == false) {
									response = new Response(false,
											String.format("order items[%d] : unable to process design_url_front due to %s.", i, checkDesignsResponse.getDesignFront().getDescription()),
											400);
									break;
								}

							} else {

								if (StringUtils.isEmpty(orderItem.getMd5CampaignId())) {
									String md5 = checkDesignsResponse.getDesignFront().getMd5Checksum();
									if (StringUtils.isEmpty(md5)) {
										md5 = checkDesignsResponse.getDesignBack().getMd5Checksum();
									}
									LOGGER.info("md5=" + md5);
									orderItem.setMd5CampaignId(store.getUserId() + "-" + md5);
									orderItem.setDesignUrlFront(checkDesignsResponse.getDesignFront().getUrl());
								}
							}

							if (response.getSuccess() && checkDesignsResponse.getIsValid() == false && checkDesignsResponse.getDesignBack() != null) {

								if (checkDesignsResponse.getDesignBack().getIsSuccessDownload() != null
										&& checkDesignsResponse.getDesignBack().getIsSuccessDownload() == false) {
									response = new Response(false, String.format("order items[%d] : can not download design_url_back.", i), 400);
									break;
								} else if (checkDesignsResponse.getDesignBack().getIsValid() != null && checkDesignsResponse.getIsValid() == false) {
									String.format("order items[%d] : unable to process design_url_back due to %s.", i, checkDesignsResponse.getDesignBack().getDescription(), 400);
									break;
								}

							} else {
								if (StringUtils.isEmpty(orderItem.getMd5CampaignId())) {
									orderItem.setMd5CampaignId(store.getUserId() + "-" + checkDesignsResponse.getDesignBack().getMd5Checksum());
								}
								orderItem.setDesignUrlBack(checkDesignsResponse.getDesignBack().getUrl());
							}
						}

						if (response.getSuccess() && (orderItem.getQuantity() == null || orderItem.getQuantity() <= 0)) {
							response = new Response(false, String.format("order items[%d] : quantity is invalid.", i), 400);
							break;
						}

					}
				}

				String orderId = null;
				if (response.getSuccess()) {
					if (orderRequest.getSandbox() == false) {
//						DropshipStoreObj store = DropShipStoreService.findByApiKey(orderRequest.getApiKey());
						if (StringUtils.isNotEmpty(orderRequest.getReferenceOrderId())) {
							if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(store.getId(), orderRequest.getReferenceOrderId(), ResourceSource.CUSTOM_API)) {
								response = new Response(false, String.format("Order %s is exist!.", orderRequest.getReferenceOrderId()), 400);
							}
						}
						if (response.getSuccess()) {
							orderId = createOrder(store, orderRequest.getReferenceOrderId(), response.getShippingId(), firstItemBaseShortCode, orderRequest);
						}
					} else {
						orderId = "ASAMPLE-FQ79-16899";
					}
				}

				Map responseM = new HashMap<String, Object>();
				responseM.put("is_success", response.getSuccess());
				responseM.put("message", response.getMessage());

				if (response.getSuccess()) {
					responseM.put("order_id", orderId);
				}

				String reasonPhase = response.getCode().intValue() == 200 ? HttpResponseStatus.OK.reasonPhrase()
						: HttpResponseStatus.BAD_REQUEST.reasonPhrase();

				routingContext.put(AppParams.RESPONSE_CODE, response.getCode());
				routingContext.put(AppParams.RESPONSE_MSG, reasonPhase);
				routingContext.put(AppParams.RESPONSE_DATA, responseM);

				future.complete();

			} catch (Exception e) {
				routingContext.fail(e);
			}
		}, asyncResult ->

		{
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	private boolean matchSkuAndReturnIfSkuIsAvailable(String sku) throws SQLException {
		if (orderItemSkuMapper == null) {
			orderItemSkuMapper = new LinkedHashMap<String, BaseSKUObj>();
		}
		if (orderItemSkuMapper.containsKey(sku)) {
			BaseSKUObj matchedSku = orderItemSkuMapper.get(sku);
			if (matchedSku != null) {
				return true;
			}
		} else {
			BaseSKUObj matchedSku = BaseSKUService.getBySku(sku);
			if (matchedSku != null) {
				orderItemSkuMapper.put(sku, matchedSku);
				return true;
			}
		}
		return false;
	}

	private String createOrder(DropshipStoreObj store, String referenceOrderId, String shippingId, String firstItemBaseShortCode, DropshipCustomApiOrder orderRequest)
			throws SQLException, ParseException {

		String orderIdPrefix = String.format("%s-%s", store.getUserId(), firstItemBaseShortCode);

		DropshipOrderTypeObj dropshipOrder = DropshipOrderTypeObj.builder()
				.idPrefix(orderIdPrefix)
				.currency("USD")
				.state(ResourceStates.QUEUED)
				.shippingId(shippingId)
				.trackingCode(AppUtil.generateOrderTrackingNumber())
				.channel("api")
				.storeId(store.getId())
				.userId(store.getUserId())
				.note(orderRequest.getApiKey())
				.referenceOrder(orderRequest.getReferenceOrderId())
				.source(ResourceSource.CUSTOM_API)
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

		Set<String> setBaseId = OrderUtil.getSetBaseFromItemCustompApi(orderRequest.getItems());

		Map shippingInfo = ProductUtil.getShippingInfoForListItems(setBaseId, orderRequest.getShippingCountry(), AppParams.STANDARD);

		Map countryTax = CountryTaxService.getTaxByCountry(orderRequest.getShippingCountry());

		Double totalTax = 0d;
		
		for (DropshipCustomApiItem item : orderRequest.getItems()) {
			DropshipBaseSkuObj baseSku = DropshipBaseSkuService.getBySku(item.getSku());

			Map feeMap = ProductUtil.calculateDropshipShippingFeeAndTaxV2(itemGroupQuantity, baseSku.getBaseId(), AppParams.STANDARD, item.getQuantity(), shippingInfo);
			Double shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);

			int isTwoDesigns = 0;
			if (StringUtils.isNotEmpty(item.getDesignUrlFront()) && StringUtils.isNotEmpty(item.getDesignUrlBack())) {
				isTwoDesigns = 1;
			}

			double baseCost = BaseService.getDropshipBaseCost(baseSku.getBaseId(), baseSku.getSizeId(), isTwoDesigns);

			double productSubTotal = baseCost * item.getQuantity();
			double productTotal = GetterUtil.format(productSubTotal + shippingFee, 2);
			LOGGER.info("+++productTotal = " + productTotal);
			Double taxAmount =0d;
			if (StringUtils.isEmpty(orderRequest.getIossNumber())) {
				taxAmount = OrderUtil.getTaxByAmountAndByCountry(productTotal, countryTax);
			}
			productTotal = GetterUtil.format(productTotal + taxAmount, 2);
			LOGGER.info("+++taxAmount = " + taxAmount);

			DropshipOrderProductTypeObj dropshipOrderProduct = DropshipOrderProductTypeObj.builder()
					.orderId(savedOrderId)
					.campaignId(item.getMd5CampaignId())
					.productId("")// -------------------------------------ProductId
					.variantId("")// -------------------------------------VariantId
					.sizeId(baseSku.getSizeId())
					.price(String.valueOf(baseSku.getPrice()))
					.shippingFee(String.valueOf(shippingFee))
					.currency("USD")
					.quantity(item.getQuantity())
					.state(ResourceStates.APPROVED)
					.variantName(baseSku.getBaseName() + " - " + baseSku.getColorName())
					.amount(String.valueOf(productTotal))
					.baseCost(String.valueOf(baseCost))
					.baseId(baseSku.getBaseId())
					.lineItemId(referenceOrderId)
					.variantFrontUrl(item.getMockupUrlFront())
					.variantBackUrl(item.getMockupUrlBack())
					.colorId(baseSku.getColorId())
					.colorValue(baseSku.getColorValue())
					.partnerSku(item.getSku())
					.colorName(baseSku.getColorName())
					.sizeName(baseSku.getSizeName())
					.shippingMethod(AppParams.STANDARD)
					.printDetail("")
					.itemType(ResourceStates.NORMAL)// -------------------------------------ItemType
//					.partnerProperties(partnerProperties.toString())
//					.partnerOption(partnerOption.toString())
					.taxAmount(String.valueOf(taxAmount))
					.build();

			DropshipOrderProductService.insertDropshipOrderProductV2(dropshipOrderProduct);

			orderTotal += productTotal;
			orderSubTotal += productSubTotal;
			orderShippingTotal += shippingFee;
			totalItems += item.getQuantity();
			totalTax += taxAmount;
		}

		String addressCheckMessage = "";
		int isIgnoreAddressCheck = 0;
		if (orderRequest.getIgnoreAddressCheck()) {
			isIgnoreAddressCheck = 1;
			addressCheckMessage = IGNORE_ADDRESS_CHECK_NOTE;
		}
		orderTotal = GetterUtil.format(orderTotal, 2);
		totalTax = GetterUtil.format(totalTax, 2);
		DropshipOrderService.updateQuantityAmountAddressCheck(savedOrderId, orderTotal, orderSubTotal, orderShippingTotal, totalItems, isIgnoreAddressCheck, addressCheckMessage, totalTax.toString());

		return savedOrderId;
	}

	private Response checkAddress(Response response, DropshipCustomApiOrder orderRequest) throws SQLException {

		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingName()))) {
			response = new Response(false, "shipping_name can not be empty.", 400);
		}

		// shipping_address1
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingAddress1()))) {
			response = new Response(false, "shipping_address1 can not be empty.", 400);
		}

		// shipping_address2
//		if (response.getSuccess()
//				&& (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingAddress2()))) {
//			response = new Response(false, "shipping_address2 can not be empty.", 400);
//		}

		// shipping_city
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingCity()))) {
			response = new Response(false, "shipping_city can not be empty.", 400);
		}
//				shipping_state
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingState()))) {
			response = new Response(false, "shipping_state can not be empty.", 400);
		}
//				shipping_zip
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingZip()))) {
			response = new Response(false, "shipping_zip can not be empty.", 400);
		}
//				shipping_country
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingCountry()))) {
			response = new Response(false, "shipping_country can not be empty.", 400);
		}
//		shipping_country iso
		if (response.getSuccess() && !IsoUtil.isValidISOCountry(orderRequest.getShippingCountry())) {
			response = new Response(false,
					"shipping_country must is ISO Alpha-2 code (https://www.nationsonline.org/oneworld/country_code_list.htm).", 400);
		}

//				shipping_email
//		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingEmail())
//				|| !EmailValidator.getInstance().isValid(orderRequest.getShippingEmail()))) {
//			response = new Response(false, "shipping_email is invalid.", 400);
//		}
//				shipping_phone

//		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingPhone()))) {
//			response = new Response(false, "shipping_phone can not be empty.", 400);
//		}

		if (response.getSuccess() && orderRequest.getIgnoreAddressCheck() == false && "US".equalsIgnoreCase(orderRequest.getShippingCountry())) {
			Map verifyResult = SSPApiConnector.verifyAddress(new Address(orderRequest.getShippingName(), orderRequest.getShippingAddress1(),
					orderRequest.getShippingAddress2(), orderRequest.getShippingCity(), orderRequest.getShippingState(),
					orderRequest.getShippingZip(), orderRequest.getShippingCountry(), orderRequest.getShippingPhone())).getMap();
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
			Map shippingMap = ShippingService.insert(orderRequest.getShippingName(), orderRequest.getShippingEmail(), orderRequest.getShippingPhone(), orderRequest.getShippingAddress1(), orderRequest.getShippingAddress2(), orderRequest.getShippingCity(), orderRequest.getShippingState(), orderRequest.getShippingZip(), orderRequest.getShippingCountry(), "");
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

		private String shippingId;
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipCustomApiOrderCreateHandler.class.getName());

}
