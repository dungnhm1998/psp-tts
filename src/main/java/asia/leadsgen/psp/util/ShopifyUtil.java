package asia.leadsgen.psp.util;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.rxjava.core.http.HttpServerRequest;

public class ShopifyUtil {

	private static String apiV1Prefix = "/psp/api/v1";

	public static boolean isShopifyWebHookRequest(HttpServerRequest httpServerRequest) {
		boolean isShopifyOrderPaidRequest = isShopifyOrderPaidRequest(httpServerRequest);
		boolean isShopifyOrderRefundRequest = isShopifyOrderRefundRequest(httpServerRequest);
		return isShopifyOrderPaidRequest || isShopifyOrderRefundRequest;
	}

	private static boolean isShopifyOrderPaidRequest(HttpServerRequest httpServerRequest) {

		String requestUri = httpServerRequest.path().replace(apiV1Prefix, StringPool.BLANK);
		String requestMethod = httpServerRequest.method().name();
		String matchPattern = "^/shopify-store/.*/paid-order$";

		if ("POST".equalsIgnoreCase(requestMethod)) {
			Pattern pattern = Pattern.compile(matchPattern);
			Matcher matcher = pattern.matcher(requestUri);
			if (matcher.find() && matcher.start() == 0) {
				return true;
			}
		}
		return false;
	}

	private static boolean isShopifyOrderRefundRequest(HttpServerRequest httpServerRequest) {
		String requestUri = httpServerRequest.path().replace(apiV1Prefix, StringPool.BLANK);
		String requestMethod = httpServerRequest.method().name();
		String matchPattern = "^/shopify-store/.*/refund-order$";

		if ("POST".equalsIgnoreCase(requestMethod)) {
			Pattern pattern = Pattern.compile(matchPattern);
			Matcher matcher = pattern.matcher(requestUri);
			if (matcher.find() && matcher.start() == 0) {
				return true;
			}
		}
		return false;
	}

	private static final Logger LOGGER = Logger.getLogger(ShopifyUtil.class.getName());

}
