package asia.leadsgen.psp.server.handler.dropship.dashboard;

import java.util.Map;
import java.util.logging.Logger;

import asia.leadsgen.psp.service_fulfill.DropshipOrderServiceV2;
import asia.leadsgen.psp.util.ParamUtil;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipConversionOverviewHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				if (StringUtils.isEmpty(userId)) {
					throw new LoginException(SystemError.LOGIN_REQUIRED);
				}
				
				Map dashboardInfo = DropshipOrderServiceV2.getDashboardConversionOverview(userId);
				Map processInfo = ParamUtil.getMapData(dashboardInfo, AppParams.RESULT_DATA);
				LOGGER.info("--"+dashboardInfo);

				Map<Object, Object> response = new HashedMap<>();
				response.put(AppParams.UNIT_SALES, ParamUtil.getDouble(dashboardInfo, AppParams.UNIT_SALES));
				response.put(AppParams.PROCESSING, ParamUtil.getDouble(processInfo, AppParams.N_PROCESSING));
				response.put(AppParams.SHIPPED, ParamUtil.getDouble(processInfo, AppParams.N_SHIPPED));
				response.put(AppParams.DELIVERED, ParamUtil.getDouble(processInfo, AppParams.N_DELIVERED));
				response.put(AppParams.REVENUE, ParamUtil.getDouble(dashboardInfo, AppParams.REVENUE));

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, response);
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

	private static final Logger LOGGER = Logger.getLogger(DropshipConversionOverviewHandler.class.getName());
}
