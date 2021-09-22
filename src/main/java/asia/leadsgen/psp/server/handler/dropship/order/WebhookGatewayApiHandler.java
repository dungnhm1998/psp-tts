package asia.leadsgen.psp.server.handler.dropship.order;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import org.thymeleaf.util.StringUtils;

import com.google.gson.Gson;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.DropshipImportFileRowObj;
import asia.leadsgen.psp.service_fulfill.DropshipImportFileRowsService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

public class WebhookGatewayApiHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		LOGGER.info("WebhookGatewayApiHandler() - getAcceptableContentType= " + routingContext.getAcceptableContentType());
		LOGGER.info("WebhookGatewayApiHandler() - requestBodyMap= " + routingContext.getBody().toString());
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			
			try {
				
				LOGGER.info("WebhookGatewayApiHandler() - requestBodyMap= " + routingContext.getBodyAsString());
				Map objBoby = routingContext.getBodyAsJson().getMap();
				JsonObject objRecive = new JsonObject(ParamUtil.getString(objBoby, "Message"));
				String rowId = objRecive.getString("row_id");
				LOGGER.info("WebhookGatewayApiHandler() - rowId= " + rowId);
				if (!StringUtils.isEmpty(rowId)) {
					try {
						
						String receiveJson = ParamUtil.getString(objBoby, "Message");
						DropshipImportFileRowObj obj = DropshipImportFileRowsService.insertJsonGatewayApi(rowId, receiveJson);
						LOGGER.info("obj= " + obj.toString());
						
					} catch (SQLException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					future.complete();
					
				} else {
					LOGGER.info("bad request");
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					future.complete();
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
	
	private static final Logger LOGGER = Logger.getLogger(WebhookGatewayApiHandler.class.getName());
}
