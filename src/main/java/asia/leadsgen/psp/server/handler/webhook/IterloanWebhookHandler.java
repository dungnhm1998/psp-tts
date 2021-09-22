package asia.leadsgen.psp.server.handler.webhook;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import asia.leadsgen.psp.service_fulfill.WebhookService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class IterloanWebhookHandler implements Handler<RoutingContext> {
	
	private static final String CURRENT_STATUS = "current_status";
	private static final String SUMMARY_INFO = "summary_info";
	
	@SuppressWarnings("rawtypes")
	@Override
	public void handle(RoutingContext routingContext) {
		routingContext.vertx().executeBlocking(future -> {

			try {
				
				Map requestBody = routingContext.getBodyAsJson().getMap();
				logger.info("--Interloan Webhook --request body: " + requestBody);
				
				String currentStatus = ParamUtil.getString(requestBody, CURRENT_STATUS);
				
				if (StringUtils.isNotEmpty(currentStatus)) {
					
					Map summaryInfo = ParamUtil.getMapData(requestBody, SUMMARY_INFO);
					
					String refId = ParamUtil.getString(summaryInfo, AppParams.ID); 
					
					List<Map> resultUpdate = WebhookService.interloanUpdateHistoryState(refId, currentStatus);
					
					if (CollectionUtils.isEmpty(resultUpdate)) {
						logger.info("--Interloan Webhook --ref_id : " + refId + " not exists");
					}
					
				}
				
				logger.info("--Interloan Webhook update success ");
			} catch (Exception e) {
				logger.info("--Interloan Webhook ERROR -- " + e.getMessage());
			}
			
			routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
			routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());

			future.complete();

		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}
	
	private static Logger logger = Logger.getLogger(IterloanWebhookHandler.class.getName());
}
