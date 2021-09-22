package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.service_fulfill.BaseService;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.external.api.ISPApiConnector;
import asia.leadsgen.psp.obj.DropshipBaseSkuObj;
import asia.leadsgen.psp.obj.DropshipImportFileRowObj;
import asia.leadsgen.psp.obj.DropshipOrderProductObj;
import asia.leadsgen.psp.obj.ShippingFeeObj;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service.ShippingFeeService;
import asia.leadsgen.psp.service_fulfill.DropshipBaseSkuService;
import asia.leadsgen.psp.service_fulfill.DropshipImportFileRowsService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderProductService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service_fulfill.MediaService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ProductUtil;
import asia.leadsgen.psp.util.ResourceSource;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 *
 * @author ToanNN 25/12/2019.
 */
public class DropshipOrderImportReprocessHandler extends PSPOrderHandler implements Handler<RoutingContext> {
	public static final String _2D_SHIRT_SKU_REGEX = "^([0-9]{3}-[0-9]{3}-[0-9]{3})$";
	public static final String IGNORE_ADDRESS_CHECK_NOTE = "Seller agree for bypass address verified";
	public static final String POSTER_SKU_REGEX = "^matte-poster-(?:11x17|16x24|17x11|24x16|24x36|36x24)$";
	public static final String MUG_SKU_REGEX = "^(beverage-mug\\|(?:11oz|15oz)\\|(?:white|black))|(color-changing-beverage-mug\\|11oz\\|white)$";
	public static final String LADIES_RACERBACK_TANK_REGEX = "^007-00(?:2|3|4)-00(?:1|2|3|4|5)$";
	public static final String _3D_ALLOW_2SIDE_DESIGNS = "^(?:MJK|WY|LMS|BX|TX|TK|ZIP)-(?:XS|S|M|L|XL|(?:2|3|4|5)XL)|(?:LMS|WY|TX|ZIP)KID-(?:S|M|L|XL)$";
	
	@Override
	public void handle(RoutingContext routingContext) {
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
//		String userId = "A1955";
		routingContext.vertx().executeBlocking(future -> {
			String dropship_order_file_row_id = routingContext.request().getParam("id");
			if (StringUtils.isEmpty(dropship_order_file_row_id)) {
				LOGGER.info("bad request");
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
				future.complete();
			} else {
				try {
					DropshipImportFileRowObj obj = null;
					try {
						obj = DropshipImportFileRowsService.getRowsById(dropship_order_file_row_id);
					} catch (Exception e) {
						e.printStackTrace();
					}
							
					if(obj != null) {
						initItemGroupQuantity();
						String order_id = obj.getOrderId();
						Map order_db = DropshipOrderService.lookUpV2(order_id, true, false, false);
						if(!order_db.isEmpty()) {
							String order_state = ParamUtil.getString(order_db, AppParams.STATE);
							Map address = ParamUtil.getMapData(ParamUtil.getMapData(order_db, AppParams.SHIPPING), AppParams.ADDRESS);
							String shipping_country = ParamUtil.getString(address, AppParams.COUNTRY);
							String shipping_method = ParamUtil.getString(order_db, AppParams.SHIPPING_METHOD);
							if(ResourceStates.QUEUED.equalsIgnoreCase(order_state)
							|| ResourceStates.DRAFT.equalsIgnoreCase(order_state)) {
								initItemGroupQuantity();
								String order_product_id = obj.getOrderProductId();
								DropshipOrderProductService.deleteOrderItem(order_product_id);
								if(obj.getSource().equalsIgnoreCase(ResourceSource.CUSTOM_IMPORT)) {
									DropshipBaseSkuObj baseSKUobj = null;
									try {
										baseSKUobj = DropshipBaseSkuService.getBySku(obj.getLineitemSku());
									} catch (SQLException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									List<Map> itemList = ParamUtil.getListData(order_db, AppParams.ITEMS);
									for (Map map : itemList) {
										String baseId = ParamUtil.getString(map, AppParams.BASE_ID);
										int quantity = ParamUtil.getInt(map, AppParams.QUANTITY);
										ShippingFeeObj shippingFeeObj = ShippingFeeService.getShippingFee(baseId, shipping_country);
										int groupTotalItem = itemGroupQuantity.get(shippingFeeObj.getGroupId());
										itemGroupQuantity.put(shippingFeeObj.getGroupId(), groupTotalItem + quantity);

									}
									createOrderSKUItem(userId, order_id, obj, baseSKUobj, shipping_country, shipping_method);
								} else {
								}
								Map order = DropshipOrderService.findByReferenceOrder(obj.getStoreId(), obj.getReferenceOrder());
								List<Map> orderItemList = ParamUtil.getListData(order, AppParams.ITEMS);
								int totalItems = 0;
								double orderTotal = 0.00;
								double orderShippingTotal = 0.00d;
								double orderSubTotal = 0.00d;
								Double totalTax = 0.00d;
								int addressVerified = 0;
								if (StringUtils.isEmpty(obj.getByPassCheckAdress())
										|| !obj.getByPassCheckAdress().contains("1")) {
									addressVerified = 0;
								} else {
									addressVerified = 1;
								}
								for (Map orderItem : orderItemList) {
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
									addressCheckMessage = IGNORE_ADDRESS_CHECK_NOTE;
								}
								try {
									DropshipOrderService.updateQuantityAmountAddressCheck(order_id, orderTotal, orderSubTotal,
											orderShippingTotal, totalItems, addressVerified, addressCheckMessage, totalTax.toString());

									LOGGER.info("order done");
									routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
									routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
									future.complete();
								
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
							} else {
								LOGGER.info("order placed");
								routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
								routingContext.put(AppParams.RESPONSE_MSG, "Order placed");
								future.complete();
							}
						} else {
							LOGGER.info("bad request");
							routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
							routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
							future.complete();
						}
					} else {
						LOGGER.info("bad request");
						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
						future.complete();
					
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}

		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}
	


	private Map createOrderSKUItem(String userId, String orderId, DropshipImportFileRowObj vObj,
			DropshipBaseSkuObj baseSKUobj, String shipping_country, String shipping_method) {
		boolean is_create_item = false;
		int reprocess = 1;
		CheckDesignsResponse checkDesignsResponse = new CheckDesignsResponse();
		Map orderItem = new LinkedHashMap<>();
		String message = "";
		String md5_image = "";
		try {
			int quantity = 0;
			if (StringUtils.isNotEmpty(vObj.getLineitemQuantity())) {
				try {
					quantity = Integer.parseInt(vObj.getLineitemQuantity());
				} catch (Exception e) {
				}
			}
			String baseId = baseSKUobj.getBaseId();
			String colorId = baseSKUobj.getColorId();
			String colorName = baseSKUobj.getColorName();
			String colorValue = baseSKUobj.getColorValue();
			String sizeName = baseSKUobj.getSizeName();
			String sizeId = baseSKUobj.getSizeId();
			String baseShortCode = baseSKUobj.getBaseShortCode();
			String baseName = baseSKUobj.getBaseName();

			String design_front_url = vObj.getDesignFrontUrl();
			String design_back_url = vObj.getDesignBackUrl();
			String mockup_front_url = vObj.getMockupFrontUrl();
			String mockup_back_url = vObj.getMockupBackUrl();
			String download_design_front_url = vObj.getDesignFrontUrl();
			String download_design_back_url = vObj.getDesignBackUrl();

			Double shippingFee =0d;
			Double taxAmount = 0d;
			Double productSubTotal =0d;
			Double productAmount = 0d;
			Double baseCost = 0d;

			if (baseSKUobj != null) {

				Map feeMap = ProductUtil.calculateShippingFeeAndTax(itemGroupQuantity, shipping_method, baseId, shipping_country, quantity);
				shippingFee = ParamUtil.getDouble(feeMap,  AppParams.SHIPPING_FEE);
				taxAmount = ParamUtil.getDouble(feeMap, AppParams.TAX_AMOUNT);

				int isTwoDesigns = 0;
				if (StringUtils.isNotEmpty(design_front_url) && StringUtils.isNotEmpty(design_back_url)) {
					isTwoDesigns = 1;
				}

				baseCost = BaseService.getDropshipBaseCost(baseId, sizeId, isTwoDesigns);

				productSubTotal = baseCost * quantity;
				productAmount = GetterUtil.format(baseCost * quantity + shippingFee + taxAmount, 2);

				if (org.apache.commons.lang.StringUtils.isNotEmpty(design_back_url)
						&& (vObj.getLineitemSku().matches(LADIES_RACERBACK_TANK_REGEX)
								|| (!vObj.getLineitemSku().matches(_2D_SHIRT_SKU_REGEX)
										&& !vObj.getLineitemSku().matches(_3D_ALLOW_2SIDE_DESIGNS)))) {
					is_create_item = false;
					LOGGER.info("vObj.getDesign_back_url()" + vObj.getDesignBackUrl());
					LOGGER.info(
							"vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler.LADIES_RACERBACK_TANK_REGEX) "
									+ vObj.getLineitemSku().matches(LADIES_RACERBACK_TANK_REGEX));
					LOGGER.info("!vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler._2D_SHIRT_SKU_REGEX) "
							+ !vObj.getLineitemSku().matches(_2D_SHIRT_SKU_REGEX));
					LOGGER.info(
							"!vObj.getLineitem_sku().matches(DropshipCustomApiOrderCreateHandler._3D_ALLOW_2SIDE_DESIGNS) "
									+ !vObj.getLineitemSku().matches(_3D_ALLOW_2SIDE_DESIGNS));
					LOGGER.info("Order items: sku does not support back print. " + vObj.getLineitemSku());

					message = "Sku does not support back print.";

				} else {
					boolean check_design = false;
					boolean is_download_design = false;

					MediaObj mediaFrontObj = null;
					MediaObj mediaBackObj = null;
					if (StringUtils.isNotEmpty(design_front_url) && !design_front_url.startsWith("http")) {
						LOGGER.info("download_design_front_url khong phai link anh");
						mediaFrontObj = MediaService.getMediaBySku(vObj.getUserId(), vObj.getDesignFrontUrl());
						if (mediaFrontObj != null) {
							md5_image = mediaFrontObj.getMd5();
							design_front_url = mediaFrontObj.getUrl();
						} else {
							design_front_url = "";
						}
					} else {
						LOGGER.info("download_design_front_url la link anh");
						download_design_front_url = vObj.getDesignFrontUrl();
						if(StringUtils.isNotEmpty(download_design_front_url)) {
							is_download_design = true;
						}
					}
					
					
					if (StringUtils.isNotEmpty(mockup_front_url) && !mockup_front_url.startsWith("http")) {
						LOGGER.info("mockup_front_url khong phai link anh");
						MediaObj mockupFrontObj = MediaService.getMediaBySku(vObj.getUserId(), mockup_front_url);
						if(mockupFrontObj != null) {
							mockup_front_url = mockupFrontObj.getUrl();
						} else {
							mockup_front_url = "";
						}
					}
					
					if (StringUtils.isNotEmpty(design_back_url) && !design_back_url.startsWith("http")) {
						LOGGER.info("download_design_back_url khong phai link anh");
						mediaBackObj = MediaService.getMediaBySku(vObj.getUserId(), vObj.getDesignBackUrl());
					
						if(mediaBackObj != null) {
							if(StringUtils.isEmpty(md5_image)) {
								md5_image = mediaBackObj.getMd5();
							}
							design_back_url = mediaBackObj.getUrl();
						} else {
							design_back_url = "";
						}
					} else {
						LOGGER.info("download_design_back_url la link anh");
						download_design_back_url = vObj.getDesignBackUrl();
						if(StringUtils.isNotEmpty(download_design_back_url)) {
							is_download_design = true;
						}
					}
					
					if (StringUtils.isNotEmpty(mockup_back_url) && !mockup_back_url.startsWith("http")) {
						LOGGER.info("mock_back_url khong phai link anh");
						MediaObj mockupBackObj = MediaService.getMediaBySku(vObj.getUserId(), mockup_back_url);
						if(mockupBackObj != null) {
							mockup_back_url = mockupBackObj.getUrl();
						} else {
							mockup_back_url = "";
						}
					}
					
//					if ((StringUtils.isNotEmpty(mockup_front_url) && !StringUtils.isNotEmpty(design_front_url))
//							|| (StringUtils.isNotEmpty(mockup_back_url) && !StringUtils.isNotEmpty(design_back_url))) {
//						is_download_design = true;
//					}
					
					if (StringUtils.isNotEmpty(design_front_url)) {
						if (StringUtils.isNotEmpty(mockup_front_url)) {
							check_design = true;
						} else {
							check_design = false;
						}
					}

					if (!check_design && StringUtils.isNotEmpty(design_back_url)) {
						if (StringUtils.isNotEmpty(mockup_back_url)) {
							check_design = true;
						} else {
							check_design = false;
						}
					}

					if (check_design) {
						if (is_download_design) {
							if (StringUtils.isEmpty(baseId)) {
								message = "Invalid SKU";
								is_create_item = false;
							} else {
								if(StringUtils.isEmpty(download_design_front_url) && StringUtils.isEmpty(download_design_back_url)) {
									message = "Design is required";
									design_back_url = "";
									design_front_url = "";
									is_create_item = false;
								} else {

									checkDesignsResponse = ISPApiConnector.checkDesignUrls(userId, baseId,
											download_design_front_url, download_design_back_url);
									if (!checkDesignsResponse.getIsValid()) {
										if (checkDesignsResponse.getCode() > 0) {
											message = checkDesignsResponse.getMessage();
										} else {
											is_create_item = false;
											message = checkDesignsResponse.getDesignFront().getDescription();
											if (StringUtils.isEmpty(message)) {
												message = checkDesignsResponse.getDesignBack().getDescription();
											}
											if (StringUtils.isEmpty(message)) {
												message = "File design is not valid";
											}
										}
										LOGGER.info("File design is not valid");
									} else {
										is_create_item = true;
										message = "";
										reprocess = 0;
										md5_image = checkDesignsResponse.getDesignFront().getMd5Checksum();
										if (StringUtils.isEmpty(md5_image)) {
											md5_image = checkDesignsResponse.getDesignBack().getMd5Checksum();
										}

										if (checkDesignsResponse.getDesignFront() != null
												&& StringUtils.isNotEmpty(checkDesignsResponse.getDesignFront().getUrl()) 
												&& StringUtils.isNotEmpty(download_design_front_url)) {
											design_front_url = checkDesignsResponse.getDesignFront().getUrl();
										}
										if (checkDesignsResponse.getDesignBack() != null
												&& StringUtils.isNotEmpty(checkDesignsResponse.getDesignBack().getUrl())
												&& StringUtils.isNotEmpty(download_design_back_url)) {
											design_back_url = checkDesignsResponse.getDesignBack().getUrl();
										}
									}
								
								}
							}
						}
					} else {
						LOGGER.info("createOrderSKUItem()- File design is null");
						message = "Design is required";
						is_create_item = false;
					}
				}
				if (StringUtils.isEmpty(vObj.getLineitemSku())) {
					message = "SKU is required";
					is_create_item = false;
				}
				
			} else {
				baseName = vObj.getLineitemName();
				message = "Invalid SKU";

				design_front_url = vObj.getDesignFrontUrl();
				design_back_url = vObj.getDesignBackUrl();
				is_create_item = false;
			}

			DropshipOrderProductObj orderProductObj = new DropshipOrderProductObj.Builder(orderId)
					.campaignId(userId + "-" + md5_image)
//					.productId(productId)
//					.variantId(variantId)
					.sizeId(sizeId)
					.price(baseCost)
					.shippingFee(shippingFee)
					.currency("USD")
					.quantity(quantity)
					.state(ResourceStates.APPROVED)
					.variantName(baseName + " - " + colorName)
					.amount(productAmount)
					.baseCost(baseCost)
					.baseId(baseId)
//					.lineItemId(referenceOrderId)
					.variantFrontUrl(mockup_front_url)
					.variantBackUrl(mockup_back_url)
					.colorId(colorId)
					.colorValue(colorValue)
					.partnerSku(baseSKUobj.getSku())
					.colorName(colorName)
					.sizeName(sizeName)
					.shippingMethod(shipping_method)
					.designBackUrl(design_back_url)
					.designFrontUrl(design_front_url)
//					.printDetail(setPrintDetail)
					.itemType(ResourceStates.NORMAL)
//					.partnerProperties(setPartnerProperties)
//					.partnerOption(setPartnerOption)
					.baseShortCode(baseShortCode)
					.taxAmount(taxAmount)
					.build();
			if (vObj.getLineitemSku().matches(DropshipCustomApiOrderCreateHandler.POSTER_SKU_REGEX)
					|| vObj.getLineitemSku().matches(DropshipCustomApiOrderCreateHandler.MUG_SKU_REGEX)) {
				orderProductObj.setColorName("White");
				orderProductObj.setColorValue("#ffffff");
			}

			LOGGER.info("createOrderSKUItem()- create item=" + orderProductObj.toString());
			orderItem = DropshipOrderProductService.insertDropshipOrderProduct(orderProductObj);
			LOGGER.info("createOrderSKUItem()- create item done orderItem=" + orderItem.toString());
			if (quantity <= 0) {
				is_create_item = false;
				message = "Quantity is required";
			}
			
			orderItem.put(AppParams.SUBTOTAL, productSubTotal);
			orderItem.put("is_create_item", is_create_item);
			String state = "warning";
			if (is_create_item) {
				state = "done";
			}

			String order_product_id = ParamUtil.getString(orderItem, AppParams.ID);
			DropshipImportFileRowsService.updateRow(vObj.getId(), state, orderId, ResourceSource.CUSTOM_IMPORT, message,
					order_product_id, reprocess);

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.info("createOrderSKUItem()- Exception=" + e);
			try {
				DropshipImportFileRowsService.updateRow(vObj.getId(), "warning", orderId, ResourceSource.CUSTOM_IMPORT,
						"Error create lineitem sku.", "", 0);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		return orderItem;
	}

	public static String getCountryCodeFromName(String country) {
		return Arrays.asList(Locale.getISOCountries()).stream().map((s) -> new Locale("", s))
				.filter((l) -> l.getDisplayCountry().equals(country)).findFirst().map((l) -> l.getCountry()).orElse("");
	}

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderImportReprocessHandler.class.getName());

}
