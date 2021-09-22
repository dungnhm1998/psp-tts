package asia.leadsgen.psp.server.handler.media;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.OracleException;
import asia.leadsgen.psp.obj.Image;
import asia.leadsgen.psp.service.DesignService;
import asia.leadsgen.psp.service.ImageService;
import asia.leadsgen.psp.service_fulfill.MediaService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.CheckDesignsResponse;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.ISPUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.PartnerConst;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import oracle.jdbc.OracleTypes;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class MediaCreateHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		
		routingContext.vertx().executeBlocking(future -> {

			try {
				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				LOGGER.info("userId= " + userId);
				if(StringUtils.isNotEmpty(userId)) {
					JsonObject requestBodyJson = routingContext.getBodyAsJson();
					String url = requestBodyJson.getString(AppParams.URL);
					String tags = requestBodyJson.getString(AppParams.TAGS);
					String type = requestBodyJson.getString(AppParams.TYPE);
					String base_id = requestBodyJson.getString(AppParams.BASE_ID);
					String md5 = requestBodyJson.getString(AppParams.MD5);
					String state = requestBodyJson.getString(AppParams.STATE);
					String name = requestBodyJson.getString(AppParams.NAME);
					String size = requestBodyJson.getString(AppParams.SIZE);
					String resolution = requestBodyJson.getString(AppParams.RESOLUTION);
					String thumb_url = requestBodyJson.getString(AppParams.THUMB_URL);
					Map objData = MediaService.createMedia(userId, type, tags, base_id, url, state, name, size, resolution, thumb_url, md5); 
					
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
					
					routingContext.put(AppParams.RESPONSE_DATA, objData);

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

	private static final Logger LOGGER = Logger.getLogger(MediaCreateHandler.class.getName());
}