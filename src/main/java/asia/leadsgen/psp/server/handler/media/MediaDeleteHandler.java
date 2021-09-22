package asia.leadsgen.psp.server.handler.media;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.MediaService;
import asia.leadsgen.psp.service.ProductDesignService;
import asia.leadsgen.psp.service.ProductService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class MediaDeleteHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {

				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				if(StringUtils.isNotEmpty(userId)) {
					String mediaId = routingContext.request().getParam(AppParams.ID);
					MediaService.deleteMediaById(userId, mediaId);
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
	
					future.complete();
				} else {
					throw new BadRequestException(SystemError.INVALID_MEDIA);
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
}
