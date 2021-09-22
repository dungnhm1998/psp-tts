package asia.leadsgen.psp.server.handler.media;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.MediaObj;
import asia.leadsgen.psp.service_fulfill.MediaService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import oracle.jdbc.OracleTypes;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class MediaCreateListHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		LOGGER.info("---media create haandler---");
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {

			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking(future -> {

			try {
				List<MediaObj> listData = new ArrayList<>();
				List<JsonObject> listMediaResponse = new ArrayList<>();
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				if(StringUtils.isNotEmpty(userId)) {
				JsonArray requestBodyJson = routingContext.getBodyAsJsonArray();
				for (int i = 0; i < requestBodyJson.size(); i++) {
					JsonObject object = requestBodyJson.getJsonObject(i);
					MediaObj mediaObj = convertToMediaObj(userId, object);
					boolean isNameExists =  MediaService.checkNameMediaExists(userId, mediaObj.getsType() ,mediaObj.getsName() );
					String status = "success";
					String message = "";
					if (isNameExists) {
						// List media trùng tên file ảnh
						status = "false";
						message = "Name is exitsts";
					} else {
						listData.add(mediaObj);
					}
					object.put("status", status);
					object.put("message", message);
					listMediaResponse.add(object);
				}
				Map responMap = new LinkedHashMap<>();
				responMap.put(AppParams.DATA, listMediaResponse);
				MediaService.createListMedia(listData);
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, responMap);

				routingContext.next();

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

	private MediaObj convertToMediaObj(String userId, JsonObject object) {
		MediaObj mediaObj = new MediaObj();
		mediaObj.setsUserId(userId);
		mediaObj.setsUrl(object.getString("url"));
		mediaObj.setsTags(object.getString("tags"));
		mediaObj.setsType(object.getString("type"));
		mediaObj.setsBaseId(object.getString("base_id"));
		mediaObj.setSmd5(object.getString("md5"));
		mediaObj.setsState(object.getString("state"));
		mediaObj.setsName(object.getString("name"));
		mediaObj.setsSize(object.getString("size"));
		mediaObj.setsResolution(object.getString("resolution"));
		mediaObj.setsThumbUrl(object.getString("thumb_url"));

		return mediaObj;
	}

	private static final Logger LOGGER = Logger.getLogger(MediaCreateListHandler.class.getName());

}
