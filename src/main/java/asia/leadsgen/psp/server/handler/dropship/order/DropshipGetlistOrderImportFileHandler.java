package asia.leadsgen.psp.server.handler.dropship.order;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.interfaces.LoggerInterface;
import asia.leadsgen.psp.service_fulfill.UploadFileService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.GetterUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

public class DropshipGetlistOrderImportFileHandler implements Handler<RoutingContext>, LoggerInterface {

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.vertx().executeBlocking(new Handler<Future<Object>>() {
            @Override
            public void handle(Future<Object> future) {
                try {
                    HttpServerRequest httpServerRequest = routingContext.request();
                    MultiMap requestParams = routingContext.request().params();
                    String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
                    String storeId = GetterUtil.getString(httpServerRequest.getParam(AppParams.STORE_ID), "");
                    String state = GetterUtil.getString(httpServerRequest.getParam(AppParams.STATE), "");
                    String type = GetterUtil.getString(httpServerRequest.getParam(AppParams.TYPE), "");
                    String source = GetterUtil.getString(httpServerRequest.getParam(AppParams.SOURCE), "");

                    String filename =  GetterUtil.getString(httpServerRequest.getParam(AppParams.FILE_NAME), "");

                    LOGGER.info("get list dropship order file with filename:=" + filename);

                    if (StringUtils.isEmpty(userId)) {
                        throw new BadRequestException(SystemError.INVALID_USER);
                    }
                    LOGGER.info("get list dropship order file with storeId:=" + storeId);
                    int page = GetterUtil.getInteger(requestParams.get(AppParams.PAGE), 1);
                    int pageSize = GetterUtil.getInteger(requestParams.get(AppParams.PAGE_SIZE), 10);
                    
                    String startDate = routingContext.request().getParam(AppParams.START_DATE);
    				String endDate = routingContext.request().getParam(AppParams.END_DATE);
    				
    				Calendar affCal = Calendar.getInstance();
    				DateFormat df = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);
    				String timezone = DateTimeUtil.formatTimezone(routingContext.request().getParam(AppParams.TIMEZONE));
    				df.setTimeZone(TimeZone.getTimeZone(timezone));

    				if (StringUtils.isNotEmpty(startDate)) {
    					affCal.setTimeInMillis(Long.valueOf(startDate));
    					startDate = df.format(affCal.getTime());
    				}

    				if (StringUtils.isNotEmpty(endDate)) {
    					affCal.setTimeInMillis(Long.valueOf(endDate));
    					endDate = df.format(affCal.getTime());
    				}
    				
    				String sort = requestParams.contains(AppParams.SORT) ? requestParams.get(AppParams.SORT) : "date";
    				String dir = requestParams.contains(AppParams.DIR) ? requestParams.get(AppParams.DIR) : "desc";

                    Map orderSearchResult= UploadFileService.getDropshipImportFile(userId, storeId, state, type, source, filename, 
                    		page, pageSize, startDate, endDate, sort, dir);

                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
                    routingContext.put(AppParams.RESPONSE_DATA, orderSearchResult);

                } catch (Exception e) {

                    LOGGER.log(Level.SEVERE, "[ERROR]", e);

                    routingContext.fail(e.getCause());

                }

                future.complete();
            }
            },
                asyncResult
                        -> {
                    if (asyncResult.succeeded()) {
                        routingContext.next();
                    } else {
                        routingContext.fail(asyncResult.cause());
                    }
                }
        );
    }

    private static final Logger LOGGER = Logger.getLogger(DropshipGetlistOrderImportFileHandler.class.getName());
}