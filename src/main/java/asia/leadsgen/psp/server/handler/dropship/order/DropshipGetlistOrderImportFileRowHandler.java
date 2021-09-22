package asia.leadsgen.psp.server.handler.dropship.order;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.interfaces.LoggerInterface;
import asia.leadsgen.psp.service_fulfill.UploadFileService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.GetterUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.MultiMap;

import io.vertx.rxjava.core.http.HttpServerRequest;

import io.vertx.rxjava.ext.web.RoutingContext;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DropshipGetlistOrderImportFileRowHandler implements Handler<RoutingContext>, LoggerInterface {

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.vertx().executeBlocking(new Handler<Future<Object>>() {
            @Override
            public void handle(Future<Object> future) {
                try {

                    HttpServerRequest httpServerRequest = routingContext.request();
                    MultiMap requestParams = routingContext.request().params();
                    String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
                    String fileId = routingContext.request().getParam("id");
                    String text =  GetterUtil.getString(httpServerRequest.getParam(AppParams.TEXT), "");
                    String state =  GetterUtil.getString(httpServerRequest.getParam(AppParams.STATE), "");
                    LOGGER.info("Dropship getList order importFile text:=" + text);

                    LOGGER.info("Dropship getList order importFile userId:=" + userId  + " fileId:="+ fileId);
                    if (StringUtils.isEmpty(userId)) {
                        throw new BadRequestException(SystemError.INVALID_USER);
                    } else if (StringUtils.isEmpty(fileId)) {
                        throw new BadRequestException(SystemError.INVALID_FILE_ID);
                    }

                    int page = GetterUtil.getInteger(requestParams.get(AppParams.PAGE), 1);
                    int pageSize = GetterUtil.getInteger(requestParams.get(AppParams.PAGE_SIZE), 10);

                    Map orderSearchResult = UploadFileService.getDropshipImportFileRow(userId, fileId, text, state, page, pageSize);

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

    private static final Logger LOGGER = Logger.getLogger(DropshipGetlistOrderImportFileRowHandler.class.getName());
}