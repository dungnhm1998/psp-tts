package asia.leadsgen.psp.server.handler.media;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.MediaService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class ListMediaHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		
		routingContext.vertx().executeBlocking(future -> {

			try {
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				
				String text = routingContext.request().getParam(AppParams.TEXT);
				String type = routingContext.request().getParam(AppParams.TYPE);
				int page = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE), 1);
                int pageSize = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE_SIZE), 10);
				if (StringUtils.isNotEmpty(userId)) {
					LOGGER.info("co thong tin user");
					Map resultData = MediaService.getListMediaByUserId(userId, page, pageSize, text, type);
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, resultData);

					routingContext.next();
				} else {
					LOGGER.info("ko co thong tin user");
					throw new BadRequestException(SystemError.INVALID_MEDIA);
				}

			} catch (Exception e) {
				e.printStackTrace();
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


	private static final Logger LOGGER = Logger.getLogger(ListMediaHandler.class.getName());
}
