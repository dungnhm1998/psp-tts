package asia.leadsgen.psp.server.handler.dropship.order;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.interfaces.LoggerInterface;
import asia.leadsgen.psp.service_fulfill.UploadFileService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DropshipOrderImportFileHandler implements Handler<RoutingContext>, LoggerInterface {

    @Override
    public void handle(RoutingContext routingContext) {
        if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
            throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
        }

        routingContext.vertx().executeBlocking(new Handler<Future<Object>>() {
            @Override
            public void handle(Future<Object> future) {
                try {
                    JsonObject requestBody = routingContext.getBodyAsJson();
                    String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
                    String storeId = routingContext.request().getParam("id");
                    String urlFile = requestBody.getString(AppParams.URL);
                    String fileName = requestBody.getString(AppParams.FILE_NAME);
                    String source = requestBody.getString(AppParams.SOURCE);

                    LOGGER.info("userId:=" + userId  + " storeId:= " + storeId  + " urlFile:=" + urlFile + " source:=" + source + "fileName:=" + fileName);
                    if (StringUtils.isEmpty(userId)) {
                        throw new BadRequestException(SystemError.INVALID_USER);
                    } else if (StringUtils.isEmpty(storeId)) {
                        throw new BadRequestException(SystemError.INVALID_STORE_ID);
                    } else if (StringUtils.isEmpty(source)) {
                        throw new BadRequestException(SystemError.INVALID_SOURCE);
                    } else if (StringUtils.isEmpty(fileName)) {
                        throw new BadRequestException(SystemError.INVALID_FILE_NAME);
                    }

                    String fileType = "";
                    String CheckFileMimeType = "";

                    Map fileUpload = new HashMap<>();
                    if (!StringUtils.isEmpty(urlFile)) {
                        try {
                            URL url = new URL(urlFile);
                            CheckFileMimeType = FilenameUtils.getExtension(url.getPath());
                            if (CheckFileMimeType.equalsIgnoreCase("csv")) {
                                fileType = "csv";
                            } else if (CheckFileMimeType.equalsIgnoreCase("xls") || CheckFileMimeType.equalsIgnoreCase("xlsx")) {
                                fileType = "excel";

                            }
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        LOGGER.info("insert information file  into the database  " + urlFile +  " url "  + fileName + " file name " + source+ " fileMimeType " + " fileType " + fileType + userId + storeId);
                        fileUpload = UploadFileService.insert(fileName, urlFile, fileType, userId, storeId, source);
                    } else {
                        throw new BadRequestException(SystemError.INVALID_URL);
                    }
                    routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
                    routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
                    routingContext.put(AppParams.RESPONSE_DATA, fileUpload);
                    future.complete();

                } catch (Exception e) {
                    routingContext.fail(e);
                }
            }},
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
    private static final Logger LOGGER = Logger.getLogger(DropshipOrderImportFileHandler.class.getName());
}
