package asia.leadsgen.psp.server.handler.dropship.dashboard;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import asia.leadsgen.psp.service_fulfill.DropshipOrderServiceV2;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.MultiMap;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DropshipConversionDetailHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {

        routingContext.vertx().executeBlocking(future -> {

            try {

                String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
                if (StringUtils.isEmpty(userId)) {
                    throw new LoginException(SystemError.LOGIN_REQUIRED);
                }

                int page = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE), 1);
                int pageSize = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE_SIZE), 50);
                String startDate = routingContext.request().getParam(AppParams.START_DATE);
                String endDate = routingContext.request().getParam(AppParams.END_DATE);
                LOGGER.info("startDate= " + startDate + ", endDate= " + endDate);
                Calendar affCal = Calendar.getInstance();
                DateFormat df = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

                if (StringUtils.isNotEmpty(startDate)) {
                    affCal.setTimeInMillis(Long.valueOf(startDate));
                    startDate = df.format(affCal.getTime());
                }

                if (StringUtils.isNotEmpty(endDate)) {
                    affCal.setTimeInMillis(Long.valueOf(endDate));
                    endDate = df.format(affCal.getTime());
                }

				LOGGER.info("userId= " + userId + ", page= " + page + ", pageSize= " + pageSize + ", formatStartDate= " + startDate + ", formatEndDate= " + endDate);

                List<Map> dashboardInfo = DropshipOrderServiceV2.getDashboardConversionDetail(userId, page, pageSize, startDate, endDate);
                int total = 0;
                if (dashboardInfo.size() > 0 ){
                    total = ParamUtil.getInt(dashboardInfo.get(0), AppParams.S_TOTAL);
                    LOGGER.info("dashboardDetail total= " + total);
                }

                List<Map> rowData = dashboardInfo.stream().map(o -> {
                    Map row = new HashedMap();
                    row.put(AppParams.DATE, ParamUtil.getString(o, AppParams.D_CREATE_ORDER));
                    row.put(AppParams.UNIT_SALES, ParamUtil.getInt(o, AppParams.S_UNIT_SALES));
                    row.put(AppParams.PROCESSING, ParamUtil.getInt(o, AppParams.PROCESSING_COUNT));
                    row.put(AppParams.SHIPPED, ParamUtil.getInt(o, AppParams.SHIPPED_COUNT));
                    row.put(AppParams.DELIVERED, ParamUtil.getInt(o, AppParams.DELIVERED_COUNT));
                    row.put(AppParams.REFUND, 0);
                    row.put(AppParams.REMAKE, ParamUtil.getInt(o, AppParams.REMAKE_COUNT));
                    row.put(AppParams.CANCELLED, ParamUtil.getInt(o, AppParams.CANCELLED_COUNT));
                    return  row;
                }).collect(Collectors.toList());

                Map response = new HashedMap();
                response.put(AppParams.TOTAL, total);
                response.put(AppParams.DATA, rowData);

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

    private static final Logger LOGGER = Logger.getLogger(DropshipConversionDetailHandler.class.getName());
}
