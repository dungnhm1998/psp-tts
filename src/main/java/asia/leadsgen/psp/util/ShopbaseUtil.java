package asia.leadsgen.psp.util;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.rxjava.core.http.HttpServerRequest;

public class ShopbaseUtil {

	private static String apiV1Prefix = "/psp/api/v1";

	public static boolean isShopbaseWebHookRequest(HttpServerRequest httpServerRequest) {
		boolean isShopbaseOrderPaidRequest = isShopbaseOrderPaidRequest(httpServerRequest);
		boolean isShopbaseOrderRefundRequest = isShopbaseOrderRefundRequest(httpServerRequest);
		return isShopbaseOrderPaidRequest || isShopbaseOrderRefundRequest;
	}

	private static boolean isShopbaseOrderPaidRequest(HttpServerRequest httpServerRequest) {

		String requestUri = httpServerRequest.path().replace(apiV1Prefix, StringPool.BLANK);
		String requestMethod = httpServerRequest.method().name();
		String matchPattern = "^/shopbase-store/.*/paid-order$";

		if ("POST".equalsIgnoreCase(requestMethod)) {
			Pattern pattern = Pattern.compile(matchPattern);
			Matcher matcher = pattern.matcher(requestUri);
			if (matcher.find() && matcher.start() == 0) {
				return true;
			}
		}
		return false;
	}

	private static boolean isShopbaseOrderRefundRequest(HttpServerRequest httpServerRequest) {
		String requestUri = httpServerRequest.path().replace(apiV1Prefix, StringPool.BLANK);
		String requestMethod = httpServerRequest.method().name();
		String matchPattern = "^/shopbase-store/.*/refund-order$";

		if ("POST".equalsIgnoreCase(requestMethod)) {
			Pattern pattern = Pattern.compile(matchPattern);
			Matcher matcher = pattern.matcher(requestUri);
			if (matcher.find() && matcher.start() == 0) {
				return true;
			}
		}
		return false;
	}

	private static final Logger LOGGER = Logger.getLogger(ShopbaseUtil.class.getName());

}
