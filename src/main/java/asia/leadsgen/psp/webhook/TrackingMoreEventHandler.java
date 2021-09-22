/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.webhook;

import java.util.Collections;
import java.util.List;

import asia.leadsgen.psp.interfaces.LoggerInterface;
import asia.leadsgen.psp.obj.ExternalTrackingObj;
import asia.leadsgen.psp.service.ExternalTrackingService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ExternalTrackingVendor;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class TrackingMoreEventHandler implements Handler<RoutingContext>, LoggerInterface {

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() != null || !routingContext.getBodyAsString().isEmpty()) {

			routingContext.vertx().executeBlocking(future -> {

				try {

					JsonObject requestBody = routingContext.getBodyAsJson();
					JsonObject meta = requestBody.getJsonObject("meta");
					int code = meta.getInteger("code");
					if (code == 200) {
						JsonObject data = requestBody.getJsonObject("data");
						String id = data.getString("id");
						List<ExternalTrackingObj> eobjList = ExternalTrackingService.getByReferenceAndVendor(id, ExternalTrackingVendor.TRACKING_MORE);
						if (eobjList.size() > 0) {
							
							for (ExternalTrackingObj eobj : eobjList) {
								String currentState = eobj.getState();
								String newState  = data.getString("status");
								if (!currentState.equals(newState)) {
									if ("notfound".equals(newState)) {
										String substatus = data.getString("substatus");
										if("notfound001".equals(substatus)) {
											newState = "transit";
										}
									}
									if ("exception".equals(newState)){
										String substatus = data.getString("substatus");
										if("exception005".equals(substatus)) {
											newState = "return";
										}
									}
									ExternalTrackingService.updateTrackingStatus(eobj.getId(), eobj.getPackageId(), newState);
									
									if ("transit".equals(newState)) {
										ExternalTrackingService.deleteOtherCarriersDetected(eobj.getId(), eobj.getPackageId(), eobj.getReferenceId());
										break;
									}	
								}
							}
							
						}else {
							logger.info("fail to process " + requestBody.encode());
						}
					}
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, Collections.EMPTY_MAP);

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
	}

}
