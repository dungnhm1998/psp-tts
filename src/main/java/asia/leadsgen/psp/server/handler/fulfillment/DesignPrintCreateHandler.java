package asia.leadsgen.psp.server.handler.fulfillment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.Image;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.DesignService;
import asia.leadsgen.psp.service.ImageService;
import asia.leadsgen.psp.service_fulfill.FulfillmentReviewService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.CheckDesignsResponse;
import asia.leadsgen.psp.util.ISPUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.PartnerConst;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DesignPrintCreateHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {
				String id = routingContext.request().getParam("id");
				LOGGER.info("Create Design Print For review with id = " + id);
				if(StringUtils.isNotEmpty(id)) {
					Map designInfoMap = new LinkedHashMap<>();
					
					Map fulfillmentReview = FulfillmentReviewService.getFulfillmentReviewById(id);
					
					if(MapUtils.isNotEmpty(fulfillmentReview)) {
						
						String adjustType = ParamUtil.getString(fulfillmentReview, AppParams.ADJUST_TYPE);
						if (!AppParams.AUTO.equalsIgnoreCase(adjustType)) {
							
							LOGGER.info("Create Design Print : Adjust type isn't auto ");
							throw new BadRequestException(SystemError.INVALID_DESIGN);
							
						}
						
//						String sku = ParamUtil.getString(fulfillmentReview, AppParams.SKU);
						String print_type = ParamUtil.getString(fulfillmentReview, AppParams.PRINT_TYPE);
						String base_id = ParamUtil.getString(fulfillmentReview, AppParams.BASE_ID);
						
						String type_print = "";
						if (base_id.matches(PartnerConst.getScalablepressMugBases())) {
							type_print = "mug";
						} else if (base_id.matches(PartnerConst.getScalablepressPosterBases())) {
							type_print = "poster";
						} else if (base_id.matches(PartnerConst.getPrintwayBases())) {
							type_print = "phonecase";
						} else if (base_id.matches(PartnerConst.getLeecowleatherBases())) {
							type_print = "leather";
						}
						
						boolean is_update = false;

						String design_front_id = ParamUtil.getString(fulfillmentReview, AppParams.DESIGN_FRONT_ID);
						String url_design_front = ParamUtil.getString(fulfillmentReview, AppParams.URL_DESIGN_FRONT);
						
						String design_back_id = ParamUtil.getString(fulfillmentReview, AppParams.DESIGN_BACK_ID);
						String url_design_back = ParamUtil.getString(fulfillmentReview, AppParams.URL_DESIGN_BACK);
						
						String url_print_front = ParamUtil.getString(fulfillmentReview, AppParams.URL_PRINT_FRONT);
						String url_print_back = ParamUtil.getString(fulfillmentReview, AppParams.URL_PRINT_BACK);
						
						String color_id = ParamUtil.getString(fulfillmentReview, AppParams.COLOR_ID);
						String color_name = ParamUtil.getString(fulfillmentReview, AppParams.COLOR_NAME);
						String color_value = ParamUtil.getString(fulfillmentReview, AppParams.COLOR_VALUE);
						
						String campaign_id = ParamUtil.getString(fulfillmentReview, AppParams.CAMPAIGN_ID);
						Map campaign = CampaignService.get(campaign_id);
						
						
						if(MapUtils.isEmpty(campaign)) {
							LOGGER.info("khong tim thay campaign= " + campaign_id);
							if(StringUtils.isNotEmpty(design_front_id)) {
								is_update = true;
								try {
									if (type_print.equalsIgnoreCase("leather")) {
										CheckDesignsResponse adjustedFrontUrl = ISPUtil
												.adjustLeeCowLeatherDesign(url_design_front, base_id, color_id);
										if (adjustedFrontUrl != null) {
											if (adjustedFrontUrl.getHasLaser() == 1) {
												print_type = "laser";
											} else {
												print_type = "normal";
											}
											url_print_front = adjustedFrontUrl.getUrl();
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}

							}
							if(StringUtils.isNotEmpty(design_back_id)) {
								is_update = true;
								try {
									if (type_print.equalsIgnoreCase("leather")) {
										CheckDesignsResponse adjustedFrontUrl = ISPUtil
												.adjustLeeCowLeatherDesign(url_design_front, base_id, color_id);
										if (adjustedFrontUrl != null) {
											if (adjustedFrontUrl.getHasLaser() == 1) {
												print_type = "laser";
											} else {
												print_type = "normal";
											}
											url_print_back = adjustedFrontUrl.getUrl();
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						
						} else {
							LOGGER.info("co thong tin campaign= " + campaign_id);
							
							String print_detail = ParamUtil.getString(fulfillmentReview, AppParams.PRINT_DETAIL);
							
							Map printDetail = new JsonObject(print_detail).getMap();
							
							String custom_data = ParamUtil.getString(printDetail, "personalize");
							
							if(StringUtils.isNotEmpty(design_front_id)) {
								String [] design_front_id_temp = design_front_id.split("_", -1);
								design_front_id = design_front_id_temp[0];
								LOGGER.info("get front design with id = " + design_front_id);
								is_update = true;
								try {
									Map design_front = DesignService.get(design_front_id);	
									String image_id = ParamUtil.getString(design_front, AppParams.IMAGE_ID);
									LOGGER.info("type_print: " + type_print);
									if (type_print.equalsIgnoreCase("leather")) {
										CheckDesignsResponse adjustedFrontUrl = ISPUtil
												.adjustLeeCowLeatherDesign(url_design_front, base_id, color_id);
										if (adjustedFrontUrl != null) {
											if (adjustedFrontUrl.getHasLaser() == 1) {
												print_type = "laser";
											} else {
												print_type = "normal";
											}
											url_print_front = adjustedFrontUrl.getUrl();
										}
									} else {
										LOGGER.info("image_id=" + image_id);
										Image image = ImageService.getImage(image_id);
										image.setUrl(url_design_front);
										image.setColorName(color_name);
										image.setColorValue(color_value);
										try {
											if (StringUtils.isNotEmpty(custom_data)) {
												
												JsonObject json_custom_data = new JsonObject(custom_data);
												if(json_custom_data.containsKey("front")) {
													JsonObject json_front = json_custom_data.getJsonObject("front");
													json_front.put("side", "front");
													image.setCustomData(json_front);
												}
												
											}
										} catch (Exception e) {
											// TODO: handle exception
										}
										if (StringUtils.isEmpty(type_print)) {
											url_print_front = ISPUtil.adjustDesign(image, true);
										} else {
											url_print_front = ISPUtil.adjustAccessoriesDesign(image, type_print, base_id);
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}

							}
							
							if(StringUtils.isNotEmpty(design_back_id)) {
								LOGGER.info("get back design with id = " + design_front_id);
								String [] design_back_id_temp = design_back_id.split("_", -1);
								design_back_id = design_back_id_temp[0];
								is_update = true;
								try {
									Map design_back = DesignService.get(design_back_id);	
									String type = ParamUtil.getString(design_back, AppParams.TYPE);
									if (type_print.equalsIgnoreCase("leather")) {
										CheckDesignsResponse adjustedFrontUrl = ISPUtil
												.adjustLeeCowLeatherDesign(url_design_back, base_id, color_id);
										if (adjustedFrontUrl != null) {
											if (adjustedFrontUrl.getHasLaser() == 1) {
												print_type = "laser";
											} else {
												print_type = "normal";
											}
											url_print_back = adjustedFrontUrl.getUrl();
										}
									} else {
										String image_id = ParamUtil.getString(design_back, AppParams.IMAGE_ID);
										LOGGER.info("image_id=" + image_id);
										Image image = ImageService.getImage(image_id);
										
										image.setUrl(url_design_back);
										image.setColorName(color_name);
										image.setColorValue(color_value);

										try {
											if (StringUtils.isNotEmpty(custom_data)) {

												JsonObject json_custom_data = new JsonObject(custom_data);
												if (json_custom_data.containsKey("back")) {
													JsonObject json_back = json_custom_data.getJsonObject("back");
													json_back.put("side", "back");
													image.setCustomData(json_back);
												}
												
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
										if (StringUtils.isEmpty(type_print)) {
											url_print_back = ISPUtil.adjustDesign(image, false);
										} else {
											url_print_back = ISPUtil.adjustAccessoriesDesign(image, type_print, base_id);
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
						
						if(is_update) {
							if (type_print.equalsIgnoreCase("mug")) {
								if (!StringUtils.isEmpty(url_print_front)) {
									url_print_back = "";
								} else {
									url_print_front = url_print_back;
									url_print_back = "";
								}
							}
							LOGGER.info("DesignPrintCreateHandler url print front -- " + url_print_front);
							LOGGER.info("DesignPrintCreateHandler url print back -- " + url_print_back);
							
							designInfoMap = FulfillmentReviewService.updateDesignPrint(id, url_print_front, url_print_back, print_type);
						} else {
							designInfoMap = fulfillmentReview;
						}
						
						
					} else {
						LOGGER.info("Create Design Print For review is null or empty ");
						throw new BadRequestException(SystemError.INVALID_DESIGN);
					}
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
					
					routingContext.put(AppParams.RESPONSE_DATA, designInfoMap);

					routingContext.next();

				} else {
					LOGGER.info("Create Design Print For review with id is null ");
					throw new BadRequestException(SystemError.INVALID_DESIGN);
				}

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
	
	private static final Logger LOGGER = Logger.getLogger(DesignPrintCreateHandler.class.getName());
	
}
