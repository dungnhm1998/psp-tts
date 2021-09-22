package asia.leadsgen.psp.server.handler.dropship.order;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.service.CampaignSearchParams;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipCampaignSearchHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			try {

				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				if (StringUtils.isEmpty(userId)) {
					throw new LoginException(SystemError.LOGIN_REQUIRED);
				}

				String title = routingContext.request().getParam(AppParams.TITLE);
				if (StringUtils.isNotEmpty(title)) {
					title = title.trim();
					int index = title.lastIndexOf("/");
					if (index > -1) {
						title = title.substring(index + 1);
					}
				}

				String domain = routingContext.request().getParam(AppParams.DOMAIN_NAME);
				int page = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE), 1);
				int pageSize = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE_SIZE), 10);

				String sort = routingContext.request().getParam(AppParams.SORT);
				String dir = routingContext.request().getParam(AppParams.DIR);
				
				CampaignSearchParams params = new CampaignSearchParams(userId, domain, title, "", "", "", "", -1, "",
						page, pageSize, "", true);

				params.setOrderby(sort);
				params.setOrderByDir(dir);
				
				Map lstCampaign = CampaignService.search(params);
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, lstCampaign);

			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "[ERROR]", e);
				routingContext.fail(e.getCause());
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

	private static final Logger LOGGER = Logger.getLogger(DropshipCampaignSearchHandler.class.getName());
}
