package asia.leadsgen.psp.server.handler.dropship.payment;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.TopupHistoryObj;
import asia.leadsgen.psp.service_fulfill.TopupHistoryService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipActiveTopupExecuteHandler implements Handler<RoutingContext> {
	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		Map requestBodyMap = routingContext.getBodyAsJson().getMap();
		LOGGER.info("requestBodyMap=" + requestBodyMap.toString());

		routingContext.vertx().executeBlocking(future -> {

			try {
				
				String email = ParamUtil.getString(requestBodyMap, AppParams.EMAIL);
				String id = ParamUtil.getString(requestBodyMap, AppParams.ID);
				Double amount = ParamUtil.getDouble(requestBodyMap, AppParams.AMOUNT);
				Double extra_fee = ParamUtil.getDouble(requestBodyMap, AppParams.EXTRA_FEE);
				String note = ParamUtil.getString(requestBodyMap, AppParams.NOTE);
				String state = ParamUtil.getString(requestBodyMap, AppParams.STATE);
				if(StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(email)) {
					TopupHistoryObj obj = TopupHistoryService.updateStateByID(id, state, email, amount, extra_fee, note);
					LOGGER.info("obj= " + obj.toString());
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				} else {
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.BAD_REQUEST.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.BAD_REQUEST.reasonPhrase());
				}
				
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
	
	private static final Logger LOGGER = Logger.getLogger(DropshipActiveTopupExecuteHandler.class.getName());

}
