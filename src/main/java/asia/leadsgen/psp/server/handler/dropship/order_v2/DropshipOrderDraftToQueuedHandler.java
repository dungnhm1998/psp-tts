package asia.leadsgen.psp.server.handler.dropship.order_v2;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.server.handler.order.PSPOrderHandler;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderDraftToQueuedHandler extends PSPOrderHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
		LOGGER.info("userId= " + userId);
		if (StringUtils.isEmpty(userId)) {
			throw new LoginException(SystemError.LOGIN_REQUIRED);
		}
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		Map requestOrderInfoMap = routingContext.getBodyAsJson().getMap();
		
		String storeId = ParamUtil.getString(requestOrderInfoMap, AppParams.STORE_ID);
		LOGGER.info("storeId= " + storeId);
		if (StringUtils.isEmpty(storeId)) {
			throw new LoginException(SystemError.INVALID_DROPSHIP_STORE_ID);
		}
		
		Map storeMap = null;
		try {
			storeMap = DropShipStoreService.getStoreApprovedAndDisconnectedById(storeId);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}			
		String storeUserId = ParamUtil.getString(storeMap, AppParams.USER_ID);
		LOGGER.info("storeUserId= " + storeUserId);
		if (!storeUserId.equalsIgnoreCase(userId)) {
			throw new LoginException(SystemError.INVALID_USER);
		}
		
		routingContext.vertx().executeBlocking(future -> {
			
			try {
							
				String orderId = routingContext.request().getParam(AppParams.ID);				

				Map dbOrderInfoMap = DropshipOrderService.lookUpV2(orderId, true, false, false);
				String userIdOfdbOrderInfoMap = ParamUtil.getString(dbOrderInfoMap, AppParams.USER_ID);
				String source = ParamUtil.getString(dbOrderInfoMap, AppParams.SOURCE);
				
				if (!userId.equals(userIdOfdbOrderInfoMap)) {
					throw new BadRequestException(SystemError.OPERATION_NOT_PERMITTED);
				}
				
				String orderState = ParamUtil.getString(dbOrderInfoMap, AppParams.STATE);
				LOGGER.info("OrderId= " + orderId + " - Order State= " + orderState);
				if (!ResourceStates.DRAFT.equalsIgnoreCase(orderState)) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
				}
				
				Map rqShipping = ParamUtil.getMapData(dbOrderInfoMap, AppParams.SHIPPING);
				if (rqShipping == null || rqShipping.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_ORDER_SHIPPING);
				}
				
				String amount = ParamUtil.getString(requestOrderInfoMap, AppParams.AMOUNT);
				if(amount == null || amount.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_ORDER);
				}
				
				List<Map> requestOrderItemList = ParamUtil.getListData(dbOrderInfoMap, AppParams.ITEMS);
				if (requestOrderItemList.size() > 0) {
					for (Map requestItem : requestOrderItemList) {
						String baseId = ParamUtil.getString(requestItem, AppParams.BASE_ID);
						if (baseId == null || baseId.isEmpty()) {
							throw new BadRequestException(SystemError.INVALID_ORDER);
						}
						String sizeId = ParamUtil.getString(requestItem, AppParams.SIZE_ID);
						if (sizeId == null || sizeId.isEmpty()) {
							throw new BadRequestException(SystemError.INVALID_ORDER);
						}
						Map campaignMap =  ParamUtil.getMapData(requestItem, AppParams.CAMPAIGN);
						String campaignId = ParamUtil.getString(campaignMap, AppParams.ID);
						if (campaignId == null || campaignId.isEmpty()) {
							throw new BadRequestException(SystemError.INVALID_ORDER);
						}else{
							// source là custom => campaign_Id = User_id-MD5
							 if(source.contains("custom")){
								LOGGER.info("Custom = " + source);
								String [] campaignStrings = campaignId.split("-",0);
								LOGGER.info("Campaign size = " + campaignStrings.length);							
								if (campaignStrings.length < 2 ) {
									throw new BadRequestException(SystemError.INVALID_DESIGN);
								}
							}
						}
						String color_name = ParamUtil.getString(requestItem, AppParams.COLOR_NAME);
						if (color_name == null || color_name.isEmpty()) {
							throw new BadRequestException(SystemError.INVALID_ORDER);
						}
						
						int quantity = ParamUtil.getInt(requestItem, AppParams.QUANTITY);
						if (quantity <= 0) {
							throw new BadRequestException(SystemError.INVALID_ORDER);
						}
						
						Map designMap = ParamUtil.getMapData(requestItem, AppParams.DESIGNS);
						String design_front_url = ParamUtil.getString(designMap, AppParams.DESIGN_FRONT_URL);
						String design_back_url = ParamUtil.getString(designMap, AppParams.DESIGN_BACK_URL);
						String mock_front_url = ParamUtil.getString(designMap, AppParams.MOCKUP_FRONT_URL);
						String mock_back_url = ParamUtil.getString(designMap, AppParams.MOCKUP_BACK_URL);

						boolean check_design = false;

						if ((StringUtils.isNotEmpty(design_front_url) && StringUtils.isNotEmpty(mock_front_url))
							|| (StringUtils.isNotEmpty(design_back_url) && StringUtils.isNotEmpty(mock_back_url))){
 							check_design = true;
						}

						if (!check_design) {
							throw new BadRequestException(SystemError.INVALID_DESIGN);
						}
					}
				}
				
				DropshipOrderService.updateStateV2(orderId, ResourceStates.QUEUED);
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());

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
	
	private static final Logger LOGGER = Logger.getLogger(DropshipOrderDraftToQueuedHandler.class.getName());

}
