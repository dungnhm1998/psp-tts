package asia.leadsgen.psp.server.handler.fulfillment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import asia.leadsgen.psp.email.MailUtil;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.FulfillmentDetailObj;
import asia.leadsgen.psp.obj.FulfillmentObj;
import asia.leadsgen.psp.obj.FulfillmentReviewObj;
import asia.leadsgen.psp.service.ExternalTrackingService;
import asia.leadsgen.psp.service_fulfill.BaseSKUService;
import asia.leadsgen.psp.service_fulfill.FulfillmentDetailService;
import asia.leadsgen.psp.service_fulfill.FulfillmentReviewService;
import asia.leadsgen.psp.service_fulfill.FulfillmentService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.PartnerConst;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class AssignedItemToPartnerHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {
			try {
				
				if (StringUtils.isEmpty(routingContext.getBodyAsString())) {
					throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
				}
				
				Map requestBody = routingContext.getBodyAsJson().getMap();
				String requestId = ParamUtil.getString(requestBody, AppParams.ID);
				String requestPartnerId = ParamUtil.getString(requestBody, AppParams.PARTNER_ID);
				
				if (StringUtils.isEmpty(requestId) || StringUtils.isEmpty(requestPartnerId)) {
					throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
				}
				
				FulfillmentDetailObj item = FulfillmentDetailService.getById(requestId);
				
				if (item == null) {
					throw new BadRequestException(new SystemError("INVALID FULFILLMENT ID",
							"Invalid Fulfillment Id", "", "http://developer.30usd.com/errors/400.html"));
				}
				
				item.setPartnerId(requestPartnerId);
				
				Map baseSKU = BaseSKUService.getBaseSkuByBaseIdAndSizeIdAndColorPartnerId(item.getBaseId(), item.getSizeId(), item.getColorId(), requestPartnerId);
				
				if (baseSKU == null) {
					throw new BadRequestException(new SystemError("INVALID PARTNER ID",
							"Invalid Partner Id", "", "http://developer.30usd.com/errors/400.html"));
				}
				
				FulfillmentReviewObj review = getReview(item, baseSKU);
				
				if (review == null) {
					throw new BadRequestException(new SystemError("REVIEW NOT EXIST",
							"Review not exist", "", "http://developer.30usd.com/errors/400.html"));
				}
				
				if (StringUtils.isNotEmpty(item.getFrontDesignId())) {
					item.setFrontImageUrl(review.getUrlPrintFront());
				}
				if (StringUtils.isNotEmpty(item.getBackDesignId())) {
					item.setBackImageUrl(review.getUrlPrintBack());
				}
				
				item.setPartnerId(requestPartnerId);
				item.setPrintType(review.getPrintType());

				String partnerSKU = ParamUtil.getString(baseSKU, AppParams.S_PARTNER_SKU);
				
				String sku = "";
				if (AppParams.AUTO.equalsIgnoreCase(review.getAdjustType())) {
					if (PartnerConst.LEE_COW_LEATHER.equalsIgnoreCase(requestPartnerId)) {
						sku = review.getSku();
					} else {
						sku = partnerSKU;
					}
				} else if (AppParams.COREL.equalsIgnoreCase(review.getAdjustType())) {
					
					item.setSeq(review.getSeq());
					sku = item.getSeq() + "-" + partnerSKU;
				}

				item.setSku(sku);
				
				Double shippingCost = 0d;
				Double shippingUS = ParamUtil.getDouble(baseSKU, "S_SHIPPING_US");
				Double shippingWW = ParamUtil.getDouble(baseSKU, "S_SHIPPING_WW");
				Double shippingAddingCostUS = ParamUtil.getDouble(baseSKU, "S_SHIPPING_ADDING_US");
				Double shippingAddingCostWW = ParamUtil.getDouble(baseSKU, "S_SHIPPING_ADDING_WW");
				
				if ("US".equalsIgnoreCase(item.getShippingCountryCode())) {
					shippingCost = shippingUS + (shippingAddingCostUS * (item.getQuantity() - 1));
				} else {
					shippingCost = shippingWW + (shippingAddingCostWW * (item.getQuantity() - 1));
				}
				
				double itemCost = 0d;
				Double price = ParamUtil.getDouble(baseSKU, AppParams.S_PRICE);
				Double addPrice = ParamUtil.getDouble(baseSKU, AppParams.S_ADD_PRICE);
				
				if (StringUtils.isNotEmpty(item.getFrontDesignId()) && StringUtils.isNotEmpty(item.getBackDesignId())) {
					price = price + addPrice;
				}
				
				itemCost = item.getQuantity() * price;
				
				item.setProductCost(String.format("%.2f", itemCost));
				item.setShippingCost(String.format("%.2f", shippingCost));
				
				FulfillmentDetailObj resultData = doAssignForOrder(item);
				Map responseMap = new LinkedHashMap<>();
				if (resultData != null) {
//					sendEmailToPartner(item.getPartnerId());
					Gson gson = new Gson();
					String resStr = gson.toJson(resultData);
					responseMap = new JsonObject(resStr).getMap();
				}
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, responseMap);
				
				logger.info(">>>>> DONE = " + item.getId());
				
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
	
	private FulfillmentReviewObj getReview(FulfillmentDetailObj item, Map baseSKU) throws Exception {

		String productSKU = ParamUtil.getString(baseSKU, AppParams.S_PRODUCT_SKU);
		String partnerSKU = ParamUtil.getString(baseSKU, AppParams.S_PARTNER_SKU);
		String adjustType = ParamUtil.getString(baseSKU, AppParams.S_ADJUST_TYPE);
		String partnerId = ParamUtil.getString(baseSKU, AppParams.S_PARTNER_ID);
		
		String skuToGetReview = "";
		
		if (!AppParams.AUTO.equalsIgnoreCase(adjustType)) {
			if (PartnerConst.ROSALINDA.equalsIgnoreCase(item.getPartnerId())
					|| PartnerConst.TIANLONG.equalsIgnoreCase(item.getPartnerId())) {
				skuToGetReview = partnerSKU;
			} else {
				skuToGetReview = productSKU;
			}
		}
		
		FulfillmentReviewObj result = null;
		logger.info("--detail info --base_id =" + item.getBaseId() + "--size_id=" + item.getSizeId() 
			+ "--color_id = " + item.getColorId() + "--partner_id = " + partnerId 
			+ "--product_sku = " + productSKU + "--partner_sku =" + partnerSKU);
		if (StringUtils.isNotEmpty(item.getFrontDesignId())) {

			//get by state ignored,approved,done
			FulfillmentReviewObj reviewFront = FulfillmentReviewService.getByCampaignDesignBaseColorSkuPartner(
					item.getCampaignId(), item.getFrontDesignId(), item.getBaseId(), item.getColorId(), skuToGetReview,
					partnerId, AppParams.FRONT);

			logger.info(">>>>> getReview Front for item id <fulfillment id> = " + item.getId() + " reviewFront = "
					+ reviewFront);
			
			if (reviewFront == null) {
				FulfillmentReviewObj checkExist = FulfillmentReviewService
						.checkExistReviewByCampaignDesignBaseColorSkuPartner(item.getCampaignId(), item.getFrontDesignId(),
								item.getBaseId(), item.getColorId(), skuToGetReview, partnerId,
								AppParams.FRONT);
				
				if (checkExist == null) {
					//clone
					FulfillmentReviewObj checkExistReviewForClone = FulfillmentReviewService
							.checkExistReviewByCampaignDesignBaseColorSkuPartner(item.getCampaignId(), item.getFrontDesignId(),
									item.getBaseId(), item.getColorId(), StringPool.BLANK, StringPool.BLANK,
									AppParams.FRONT);
					if (checkExistReviewForClone != null) {
						logger.info(" Clone review from -- " + checkExistReviewForClone.getId());
						checkExistReviewForClone.setPartnerId(partnerId);
						checkExistReviewForClone.setState("created");
						
						if (!AppParams.AUTO.equalsIgnoreCase(adjustType)) {
							if (PartnerConst.ROSALINDA.equalsIgnoreCase(item.getPartnerId())
									|| PartnerConst.TIANLONG.equalsIgnoreCase(item.getPartnerId())) {
								checkExistReviewForClone.setSku(partnerSKU);
							} else {
								checkExistReviewForClone.setSku(productSKU);
							}
						}
						
						checkExistReviewForClone.setUserEmail(null);
						FulfillmentReviewObj reviewClone = FulfillmentReviewService
								.insertSingle(checkExistReviewForClone);
						reviewFront = reviewClone;
						logger.info(" Clone review result -- " + reviewClone.getId());
					} else {
						logger.info("--getReview FRONT : --checkExistReviewForClone is null ");
					}
					
				} else {
					reviewFront = checkExist;
					logger.info("--getReview FRONT : --state checkExist review =  " + checkExist.getState());
				}
				
			} 
			
			if (reviewFront == null) {
				return null;
			} else {
				result = reviewFront;
				if (AppParams.COREL.equalsIgnoreCase(result.getAdjustType())) {
					if (StringUtils.isEmpty(result.getUrlPrintFront())) {
						result.setUrlPrintFront(result.getUrlDesignFront());
					}

					if ("done".equalsIgnoreCase(result.getState())) {
						result.setUrlPrintBack(result.getUrlPrintFront());
					}
				}
			}
			
		}

		if (StringUtils.isNotEmpty(item.getBackDesignId())
				&& !PartnerConst.PRINTWAY.equalsIgnoreCase(item.getPartnerId())
				&& !PartnerConst.SCALABLE_PRESS.equalsIgnoreCase(item.getPartnerId())) {

			//get by state ignored,approved,done
			FulfillmentReviewObj reviewBack = FulfillmentReviewService.getByCampaignDesignBaseColorSkuPartner(
					item.getCampaignId(), item.getBackDesignId(), item.getBaseId(), item.getColorId(), skuToGetReview,
					partnerId, AppParams.BACK);

			logger.info(">>>>> getReview Back for item id <fulfillment id> = " + item.getId() + " reviewFront = "
					+ reviewBack);
			
			if (reviewBack == null) {
				FulfillmentReviewObj checkExist = FulfillmentReviewService
						.checkExistReviewByCampaignDesignBaseColorSkuPartner(item.getCampaignId(), item.getBackDesignId(),
								item.getBaseId(), item.getColorId(), skuToGetReview, partnerId,
								AppParams.BACK);
				
				if (checkExist == null) {
					//clone
					FulfillmentReviewObj checkExistReviewForClone = FulfillmentReviewService
							.checkExistReviewByCampaignDesignBaseColorSkuPartner(item.getCampaignId(), item.getBackDesignId(),
									item.getBaseId(), item.getColorId(), skuToGetReview, StringPool.BLANK,
									AppParams.BACK);
					if (checkExistReviewForClone != null) {
						logger.info(" Clone review from -- " + checkExistReviewForClone.getId());
						checkExistReviewForClone.setPartnerId(partnerId);
						checkExistReviewForClone.setState("created");
						
						if (!AppParams.AUTO.equalsIgnoreCase(adjustType)) {
							if (PartnerConst.ROSALINDA.equalsIgnoreCase(item.getPartnerId())
									|| PartnerConst.TIANLONG.equalsIgnoreCase(item.getPartnerId())) {
								checkExistReviewForClone.setSku(partnerSKU);
							} else {
								checkExistReviewForClone.setSku(productSKU);
							}
						}
						
						checkExistReviewForClone.setUserEmail(null);
						FulfillmentReviewObj reviewClone = FulfillmentReviewService
								.insertSingle(checkExistReviewForClone);
						reviewBack = reviewClone;
						logger.info(" Clone review result -- " + reviewClone.getId());
					} else {
						logger.info("--getReview BACK : --checkExistReviewForClone is null ");
					}
					
				} else {
					reviewBack = checkExist;
					logger.info("--getReview BACK : --state checkExist review =  " + checkExist.getState());
				}
				
			} 
			
			if (reviewBack == null) {
				return null;
			} else {
				if (result == null) {
					result = reviewBack;
				} else {
					result.setDesignBackId(reviewBack.getDesignBackId());
					result.setUrlDesignBack(reviewBack.getUrlDesignBack());
					result.setUrlPrintBack(reviewBack.getUrlPrintBack());
				}

				if (AppParams.COREL.equalsIgnoreCase(result.getAdjustType())) {
					if (StringUtils.isEmpty(result.getUrlPrintBack())) {
						result.setUrlPrintBack(result.getUrlDesignBack());
					}

					if ("done".equalsIgnoreCase(result.getState())) {
						result.setUrlPrintBack(result.getUrlPrintFront());
					}
				}
			}
		}

		if (result != null) {
			logger.info(">>>>> getReview for item id <fulfillment id> = " + item.getId() + " result = "
					+ result.toString());
		}

		return result;
	}
	
	private FulfillmentDetailObj doAssignForOrder(FulfillmentDetailObj item) throws Exception {
		//old packageId
		String oldPackageId = item.getPackageId();
		String packageId = FulfillmentService.assignItemToPartner(item);
		FulfillmentDetailObj result = null;
		if (StringUtils.isEmpty(packageId)) {
			throw new Exception("Error while processing package for item  " + item.toString());
		} else {
			logger.info(">>>>> Assign to partner = " + item.getPartnerId() + " for item : " + item.getId()
					+ " success with package : " + packageId);
			if (PartnerConst.CANVAS_CHAMBO.equalsIgnoreCase(item.getPartnerId())
					|| PartnerConst.CANVAS_CHAMP.equalsIgnoreCase(item.getPartnerId())) {
				FulfillmentObj fulfillmentObj = new FulfillmentObj.Builder(packageId).campaignId(item.getCampaignId())
						.campaignTitle(item.getCampaignTitle()).quantity(item.getQuantity()).partnerId(item.getPartnerId()).build();

				List<FulfillmentObj> lst = new ArrayList<FulfillmentObj>();
				lst.add(fulfillmentObj);
				FulfillmentService.insertFulfillment(lst);
			}
			
			List<FulfillmentDetailObj> lstUpdate = new ArrayList<FulfillmentDetailObj>();
			lstUpdate.add(item);
			FulfillmentService.updatePrintUrlAndSkuAndCost(lstUpdate);
			result = FulfillmentDetailService.getById(item.getId());
			
			if (StringUtils.isNotEmpty(oldPackageId)) {
				ExternalTrackingService.deleteExternalTrackingByPackageId(oldPackageId);
			}
			
		}
		return result;
	}
	
	private void sendEmailToPartner(String partnerId) {
		if (PartnerConst.CANVAS_CHAMBO.equalsIgnoreCase(partnerId)) {

			Map emailData = new HashMap<>();
			emailData.put(AppParams.ID, PartnerConst.CANVAS_CHAMBO);
			emailData.put(AppParams.EMAIL, PartnerConst.CANVAS_CHAMBO_EMAIL);
			emailData.put(AppParams.NAME, PartnerConst.CANVAS_CHAMBO_NAME);

			emailData.put(AppParams.ASSIGNED_PRODUCTS, 1);
			emailData.put(AppParams.ASSIGNED_CAMPAIGNS, 1);

			MailUtil.sendNotificationEmailToPartner(emailData);

		}

		if (PartnerConst.CANVAS_CHAMP.equalsIgnoreCase(partnerId)) {
			Map emailData = new HashMap<>();
			emailData.put(AppParams.ID, PartnerConst.CANVAS_CHAMP);
			emailData.put(AppParams.EMAIL, PartnerConst.CANVAS_CHAMP_EMAIL);
			emailData.put(AppParams.NAME, PartnerConst.CANVAS_CHAMP_NAME);

			emailData.put(AppParams.ASSIGNED_PRODUCTS, 1);
			emailData.put(AppParams.ASSIGNED_CAMPAIGNS, 1);

			MailUtil.sendNotificationEmailToPartner(emailData);
		}
	}
	
	private static final Logger logger = Logger.getLogger(AssignedItemToPartnerHandler.class.getName());

}
