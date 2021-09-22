package asia.leadsgen.psp.server.handler.media;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderImportCVSHandlerV2;
import asia.leadsgen.psp.service_fulfill.MediaService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class MediaUpdateHandler implements Handler<RoutingContext> {
	private static final Logger LOGGER = Logger.getLogger(MediaUpdateHandler.class.getName());
	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}
		routingContext.vertx().executeBlocking(future -> {

			try {

				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
				JsonObject requestBodyJson = routingContext.getBodyAsJson();
				String mediaId = routingContext.request().getParam(AppParams.ID);
				
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
				
				Map media = MediaService.getMediaById(userId, mediaId);
				LOGGER.info("userId=" + userId + " --- mediaId= " + mediaId);
				if(!media.isEmpty()) {
					boolean update = false;
					if(StringUtils.isEmpty(url)){
						url = ParamUtil.getString(media, AppParams.URL);
					} else {
						if(!url.equalsIgnoreCase(ParamUtil.getString(media, AppParams.URL))) {
							update = true;
						}
					}
					
					if(StringUtils.isEmpty(tags)){
						tags = ParamUtil.getString(media, AppParams.TAGS);
					} else {
						if(!tags.equalsIgnoreCase(ParamUtil.getString(media, AppParams.TAGS))) {
							update = true;
						}
					}
					
					
					if(StringUtils.isEmpty(base_id)){
						base_id = ParamUtil.getString(media, AppParams.BASE_ID);
					} else {
						if(!ParamUtil.getString(media, AppParams.BASE_ID).contains(base_id)) {
							base_id = ParamUtil.getString(media, AppParams.BASE_ID) + "," + base_id;
							update = true;
						}
					}
					
					
					if(StringUtils.isEmpty(state)){
						state = ParamUtil.getString(media, AppParams.STATE);
					} else {
						if(!state.equalsIgnoreCase(ParamUtil.getString(media, AppParams.STATE))) {
							update = true;
						}
					}
					
					
					if(StringUtils.isEmpty(type)){
						type = ParamUtil.getString(media, AppParams.TYPE);
					} else {
						if(!type.equalsIgnoreCase(ParamUtil.getString(media, AppParams.TYPE))) {
							update = true;
						}
					}
					
					if(StringUtils.isEmpty(name)){
						name = ParamUtil.getString(media, AppParams.NAME);
					} else {
						if(!name.equalsIgnoreCase(ParamUtil.getString(media, AppParams.NAME))) {
							update = true;
						}
					}
					
					if(StringUtils.isEmpty(size)){
						size = ParamUtil.getString(media, AppParams.SIZE);
					} else {
						if(!size.equalsIgnoreCase(ParamUtil.getString(media, AppParams.SIZE))) {
							update = true;
						}
					}
					
					if(StringUtils.isEmpty(resolution)){
						resolution = ParamUtil.getString(media, AppParams.RESOLUTION);
					} else {
						if(!resolution.equalsIgnoreCase(ParamUtil.getString(media, AppParams.RESOLUTION))) {
							update = true;
						}
					}
					
					if(StringUtils.isEmpty(thumb_url)){
						thumb_url = ParamUtil.getString(media, AppParams.THUMB_URL);
					} else {
						if(!thumb_url.equalsIgnoreCase(ParamUtil.getString(media, AppParams.THUMB_URL))) {
							update = true;
						}
					}
					
					if(StringUtils.isEmpty(md5)){
						md5 = ParamUtil.getString(media, AppParams.MD5);
					} else {
						if(!md5.equalsIgnoreCase(ParamUtil.getString(media, AppParams.MD5))) {
							update = true;
						}
					}
					
					
					
					if(update) {
						Map mediaInfoMap = MediaService.updateMediaById(userId, mediaId, type, tags, base_id, url, state, name, size, resolution, thumb_url, md5);
						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
						routingContext.put(AppParams.RESPONSE_DATA, mediaInfoMap);
					} else {

						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
						routingContext.put(AppParams.RESPONSE_DATA, media);
					}
				} else {
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.FOUND.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.FOUND.reasonPhrase());
				}

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
}
