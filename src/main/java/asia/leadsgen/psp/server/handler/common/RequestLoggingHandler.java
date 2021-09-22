package asia.leadsgen.psp.server.handler.common;

import java.util.Set;
import java.util.logging.Logger;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.StringPool;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class RequestLoggingHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {

				Set<Cookie> cookies = routingContext.cookies();
				// for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
				// Cookie cookie = (Cookie) iterator.next();
				// LOGGER.log(Level.INFO, "{0}->{1}", new Object[]{cookie.getName(),
				// cookie.getValue()});
				// }

				HttpServerRequest httpServerRequest = routingContext.request();
				String sourceIp = httpServerRequest.getHeader(AppParams.X_REMOTE_ADDR);
				if (sourceIp == null) {
					sourceIp = httpServerRequest.getHeader(AppParams.X_FORWARDED_FOR);
				}
				routingContext.put(AppParams.IP_ADDRESS, sourceIp);

				String userAgent = httpServerRequest.getHeader(AppParams.USER_AGENT);
				routingContext.put(AppParams.USER_AGENT, userAgent);

				String shippingId = httpServerRequest.getHeader(AppParams.X_SHIPPING_ID);
				routingContext.put(AppParams.SHIPPING_ID, shippingId);
				String host = httpServerRequest.getHeader(AppParams.HOST);
				routingContext.put(AppParams.HOST, host);
				// routingContext.put("user_id", "A677");

				LOGGER.info("[REQUEST] ************* " + sourceIp + " - " + host + " - " + shippingId + "  - "
						+ httpServerRequest.method() + StringPool.DOUBLE_SPACE + httpServerRequest.uri()
						+ " *************");

				Set<String> headerNames = httpServerRequest.headers().names();

				// headerNames.stream().filter(header ->
				// Arrays.asList(REQUIRED_HEADERS).stream().anyMatch(header::equalsIgnoreCase)).forEach(header
				// -> LOGGER.info("[REQUEST] HEADER: " + header + StringPool.SPACE +
				// StringPool.COLON + StringPool.SPACE + httpServerRequest.getHeader(header)));

				// String loggingRequestBody = routingContext.getBodyAsString().length() > 1000
				// ? routingContext.getBodyAsString().substring(0, 1000) + "..." :
				// routingContext.getBodyAsString();

				// LOGGER.log(Level.INFO, "[REQUEST] BODY: " + loggingRequestBody);

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

	private static final String[] REQUIRED_HEADERS = { AppParams.X_DATE, AppParams.X_EXPIRES, AppParams.X_AUTHORIZATION,
			HttpHeaders.COOKIE.toString(), HttpHeaders.USER_AGENT.toString(), HttpHeaders.CONTENT_TYPE.toString(),
			AppParams.X_REMOTE_ADDR, AppParams.X_SHIPPING_ID };

	private static final Logger LOGGER = Logger.getLogger(RequestLoggingHandler.class.getName());
}
