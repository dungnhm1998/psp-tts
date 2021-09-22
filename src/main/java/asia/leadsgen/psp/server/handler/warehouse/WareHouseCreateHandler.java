package asia.leadsgen.psp.server.handler.warehouse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.server.handler.etsy.EtsyConnectHandler;
import asia.leadsgen.psp.service_fulfill.UpdateTrackingService;
import asia.leadsgen.psp.util.AppParams;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

public class WareHouseCreateHandler implements Handler<RoutingContext> {
	
	private static final String INFOMATION_LINK = "http://developer.30usd.com/errors/400.html";
	
	private static final Logger LOGGER = Logger.getLogger(EtsyConnectHandler.class.getName());
	
	private static String domain;
	private static String apiPrefix;
	
	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {
				Map responseData = new LinkedHashMap<>();
				
				HttpServerRequest httpServerRequest = routingContext.request();
				MultiMap requestHeaders = httpServerRequest.headers().getDelegate();
				String token = requestHeaders.get("Authorization");
				
				if (!UpdateTrackingService.validateToken(token)) {
					responseData.put("code",1);
					responseData.put("message", "Invalid Token");
				} else {
					JsonArray requestBody = new JsonArray(routingContext.getBodyAsString());
					if (requestBody.size() < 1) {
						LOGGER.severe("[ERROR] body invalid");
						throw new BadRequestException(new SystemError("BODY INVALID", "Body invalid", "", INFOMATION_LINK));
					}
					
					String partnerId = "H4Qah6MbqrkxPFTH";
					String groupId = UpdateTrackingService.insertData(requestBody , partnerId );
					
					
					responseData.put("url", domain + apiPrefix + "/warehouse/status/" + groupId);
					responseData.put("code",0);
					responseData.put("message", "Accepted");
					
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
	
	public void setDomain (String domain) {
		WareHouseCreateHandler.domain = domain;
	}

	public void setApiPrefix (String apiPrefix) {
		WareHouseCreateHandler.apiPrefix = apiPrefix;
	}
}
