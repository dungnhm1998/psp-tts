package asia.leadsgen.psp.server.handler.dropship.order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.obj.DropshipImportFileRowObj;
import asia.leadsgen.psp.service_fulfill.DropshipImportFileRowsService;
import asia.leadsgen.psp.util.AppParams;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipOrderCustomApiCheckLogHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			try {
				String fileId = routingContext.request().getParam(AppParams.ID);
				LOGGER.info("---look up info file_id : " + fileId);
				List<DropshipImportFileRowObj> orderInfoMap = DropshipImportFileRowsService.lookUp(fileId);
				DropshipImportFileRowObj obj = orderInfoMap.get(0);
				
				Map orderCheckLog = new LinkedHashMap<>();
				orderCheckLog.put(AppParams.ORDER_ID, obj.getOrderId());
				orderCheckLog.put(AppParams.SHIPPING_NAME, obj.getShippingName());
				orderCheckLog.put(AppParams.SHIPPING_ADDRESS1, obj.getShippingAddress1());
				orderCheckLog.put(AppParams.SHIPPING_ADDRESS2, obj.getShippingAddress2());
				orderCheckLog.put(AppParams.SHIPPING_CITY, obj.getShippingCity());
				orderCheckLog.put(AppParams.SHIPPING_STATE, obj.getShippingProvince());
				orderCheckLog.put(AppParams.SHIPPING_ZIP, obj.getShippingZip());
				orderCheckLog.put(AppParams.SHIPPING_COUNTRY, obj.getShippingCountry());
				orderCheckLog.put(AppParams.SHIPPING_EMAIL, obj.getEmail());
				orderCheckLog.put(AppParams.SHIPPING_PHONE, obj.getShippingPhone());
				orderCheckLog.put(AppParams.IGNORE_ADDRESS_CHECK, obj.getByPassCheckAdress().equalsIgnoreCase("1"));
				orderCheckLog.put(AppParams.REFERENCE_ORDER_ID, obj.getReferenceOrder());
				
				List<Map> listData = new ArrayList<>();
				for (DropshipImportFileRowObj item : orderInfoMap) {
					
					Map data = new LinkedHashMap<>();
					data.put(AppParams.LINEITEM_SKU, item.getLineitemSku());
					data.put(AppParams.LINEITEM_QUANTITY, item.getLineitemQuantity());
					data.put(AppParams.DESIGN_FRONT_URL, item.getDesignFrontUrl());
					data.put(AppParams.DESIGN_BACK_URL, item.getDesignBackUrl());
					data.put(AppParams.MOCKUP_FRONT_URL, item.getMockupFrontUrl());
					data.put(AppParams.MOCKUP_BACK_URL, item.getMockupBackUrl());
					data.put(AppParams.STATUS, item.getStatus());
					data.put(AppParams.NOTES, item.getNotes());
					
					listData.add(data);
				}
				orderCheckLog.put(AppParams.ITEMS, listData);
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, orderCheckLog);

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

	private static final Logger LOGGER = Logger.getLogger(DropshipOrderCustomApiCheckLogHandler.class.getName());
}
