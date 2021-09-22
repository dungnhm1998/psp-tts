package asia.leadsgen.psp.server.handler.warehouse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.server.handler.etsy.EtsyConnectHandler;
import asia.leadsgen.psp.service_fulfill.UpdateTrackingService;
import asia.leadsgen.psp.util.AppParams;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

public class WareHouseGetStateHandler implements Handler<RoutingContext> {
	
	private static final Logger LOGGER = Logger.getLogger(EtsyConnectHandler.class.getName());
	
	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {
				LOGGER.info("GET STATUS START");
				Map responseData = null;
				String groupId = routingContext.request().params().get("id");
				if (StringUtils.isEmpty(groupId)) {
					LOGGER.severe("Group ID is null or empty");
					throw new BadRequestException(SystemError.INVALID_URL);
				}
				
				HttpServerRequest httpServerRequest = routingContext.request();
				MultiMap requestHeaders = httpServerRequest.headers().getDelegate();
				String token = requestHeaders.get("Authorization");
				
				if (!UpdateTrackingService.validateToken(token)) {
					responseData = new LinkedHashMap<>();
					responseData.put("code",1);
					responseData.put("message", "Invalid Token");
				} else {
					List<Map> datas = UpdateTrackingService.getDataByGroupId(groupId);
					
					responseData = UpdateTrackingService.format(datas);
				}
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, responseData);
				
			} catch (Exception e) {
				LOGGER.warning(e.getMessage());
				routingContext.fail(e);
			}
			
			future.complete();
			
		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});

	}
}
