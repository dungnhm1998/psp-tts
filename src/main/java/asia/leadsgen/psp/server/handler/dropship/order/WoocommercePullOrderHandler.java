/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.server.handler.dropship.order;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.WooService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DataAccessSecurer;
import asia.leadsgen.psp.util.DateTimeUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 *
 * @author HIEPHV
 */
public class WoocommercePullOrderHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {

        routingContext.vertx().executeBlocking(future -> {
            try {

//                String storeId = routingContext.request().getParam(AppParams.ID);
//                String page = routingContext.request().getParam(AppParams.PAGE);
//                String orderIds = routingContext.request().getParam(AppParams.ORDERS);
//                String pageSize = routingContext.request().getParam(AppParams.PAGE_SIZE);
//                String status = routingContext.request().getParam(AppParams.STATUS);
//
//                String startDate = routingContext.request().getParam(AppParams.START_DATE);
//                String endDate = routingContext.request().getParam(AppParams.END_DATE);
//                
//                Calendar affCal = Calendar.getInstance();
//                
//                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//                String timezone = DateTimeUtil.formatTimezone(routingContext.request().getParam(AppParams.TIMEZONE));
//                df.setTimeZone(TimeZone.getTimeZone(timezone));
//
//                if (StringUtils.isNotEmpty(startDate)) {
//                    affCal.setTimeInMillis(Long.valueOf(startDate));
//                    startDate = df.format(affCal.getTime());
//                } else {
//                    startDate = df.format(new Date(0));
//                }
//
//                if (StringUtils.isNotEmpty(endDate)) {
//                    affCal.setTimeInMillis(Long.valueOf(endDate));
//                    endDate = df.format(affCal.getTime());
//                }else{
//              
//                   endDate = df.format(new Date());
//                }
//
//                if (status == null || status.isEmpty()) {
//                    status = "any";
//                }
//                
//                DataAccessSecurer.secureSubaccountAccessStore(routingContext, storeId);
//                
//                Map storeMap = DropShipStoreService.lookUp(storeId);
//                if (storeMap.isEmpty()) {
//                    throw new BadRequestException(SystemError.INVALID_STORE);
//                }
//                
//                String consumerKey = ParamUtil.getString(storeMap, AppParams.API_KEY);
//                String consumerSecret = ParamUtil.getString(storeMap, AppParams.SECRET);
//                String domain = ParamUtil.getString(storeMap, AppParams.DOMAIN);
//                String userId = ParamUtil.getString(storeMap, AppParams.USER_ID);
//                int offset = 1;
//                
//               //   String[] IdList = orderIds.split("\\,");
//                JSONArray orderResult = WooService.OrderPullData(domain, consumerKey, consumerSecret, page, offset, pageSize, status, startDate, endDate, orderIds);
//                routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
//                routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
//
//                Map resultDataMap = new LinkedHashMap<>();
//                resultDataMap.put(AppParams.TOTAL, orderResult.length());
//                resultDataMap.put(AppParams.DATA, orderResult.toList());
//
//                routingContext.put(AppParams.RESPONSE_DATA, resultDataMap);
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

    private static final Logger LOGGER = Logger.getLogger(WoocommercePullOrderHandler.class.getName());
}