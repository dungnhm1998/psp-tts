package asia.leadsgen.psp.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import asia.leadsgen.psp.server.handler.privileges.UserPrivilegesHandler;
import io.vertx.core.http.HttpMethod;

public class UserPrivilegesUtil {

	public static boolean bypassClientAndSessionChecking(String method, String uri) {

		if (method.equalsIgnoreCase(HttpMethod.POST.name()) && uri.contains("/webhooks/trackingmore-b2d6e422-83b9")) {
			return true;
		}

		return false;
	}

	private static List<Map> publicAPIs;

	private static void initPublicApis() {
		publicAPIs = new ArrayList<>();

//		publicAPIs.add(addPublicApi("GET", "^/campaigns.*$"));
//		publicAPIs.add(addPublicApi("GET", "^/categories.*$"));
		publicAPIs.add(addPublicApi("GET", "^/stores.*"));
		publicAPIs.add(addPublicApi("GET", "^/store/products.*"));

		publicAPIs.add(addPublicApi("GET", "^/orders/.*$"));
		publicAPIs.add(addPublicApi("POST", "^/orders"));
		publicAPIs.add(addPublicApi("PUT", "^/orders/.*$"));
		publicAPIs.add(addPublicApi("GET", "^/orders_tracking.*"));

		publicAPIs.add(addPublicApi("GET", "^/payments/.*$"));
		publicAPIs.add(addPublicApi("GET", "^/payments.*"));
		publicAPIs.add(addPublicApi("POST", "^/orders/.*/payments$"));
		publicAPIs.add(addPublicApi("POST", "^/payments/execute"));

		publicAPIs.add(addPublicApi("GET", "^/traffic_tracking"));
		publicAPIs.add(addPublicApi("GET", "^/email-tracking"));

		publicAPIs.add(addPublicApi("POST", "^/sessions$"));
		publicAPIs.add(addPublicApi("DELETE", "^/sessions/.*$"));
		publicAPIs.add(addPublicApi("GET", "^/sessions/.*$"));

		publicAPIs.add(addPublicApi("GET", "^/promotion_types.*"));
		publicAPIs.add(addPublicApi("GET", "^/promotion_discounts.*"));
		publicAPIs.add(addPublicApi("GET", "^/promotions/.*"));
		publicAPIs.add(addPublicApi("GET", "^/promotions.*"));

		publicAPIs.add(addPublicApi("GET", "^/preferences.*"));

		publicAPIs.add(addPublicApi("POST", "^/user/add$"));

		publicAPIs.add(addPublicApi("GET", "^/ticket.*"));
		publicAPIs.add(addPublicApi("GET", "^/ticket_categories.*"));
		publicAPIs.add(addPublicApi("POST", "^/ticket"));

		// upsell
		publicAPIs.add(addPublicApi("GET", "^/email-upsell.*"));

//		TEST
		publicAPIs.add(addPublicApi("GET", "^/dropship/shopify/connect"));
		publicAPIs.add(addPublicApi("POST", "^/dropship/shopify/connect"));

		publicAPIs.add(addPublicApi("POST", "^/shopify-store/update-fulfillment$"));

		publicAPIs.add(addPublicApi("POST", "^/cms/package/label$"));
		publicAPIs.add(addPublicApi("POST", "^/cms/package/m/label$"));
		publicAPIs.add(addPublicApi("POST", "^/cms/package/email$"));

		// webhook
		publicAPIs.add(addPublicApi("POST", "^/shopify-store/.*/paid-order$"));
		publicAPIs.add(addPublicApi("POST", "^/shopify-store/.*/refund-order$"));
		publicAPIs.add(addPublicApi("POST", "^/shopbase-store/.*/paid-order$"));
		publicAPIs.add(addPublicApi("POST", "^/shopbase-store/.*/refund-order$"));
		publicAPIs.add(addPublicApi("POST", "^/webhooks/send-email$"));
		publicAPIs.add(addPublicApi("POST", "^/webhooks/trackingmore-b2d6e422-83b9$"));

		publicAPIs.add(addPublicApi("POST", "^/report-campaign"));

		// Detect color for Art
		publicAPIs.add(addPublicApi("GET", "^/art-detect-color"));

		publicAPIs.add(addPublicApi("GET", "^/create-missing-variant"));

		// Remove after test
		publicAPIs.add(addPublicApi("POST", "^/fulfillment/export"));

		publicAPIs.add(addPublicApi("PUT", "^/tracking/.*"));

		publicAPIs.add(addPublicApi("GET", "^/flushcache"));

		publicAPIs.add(addPublicApi("GET", "^/dropship/shopify/connect.*"));
		publicAPIs.add(addPublicApi("POST", "^/invoice/adjustment"));

		publicAPIs.add(addPublicApi("POST", "^/campaigns-takedown"));
		publicAPIs.add(addPublicApi("GET", "^/campaigns-takedown"));

		publicAPIs.add(addPublicApi("POST", "^/tsc-notification/shipment"));
		publicAPIs.add(addPublicApi("POST", "^/tsc-notification/release"));
		publicAPIs.add(addPublicApi("POST", "^/tsc-notification/cancel-order"));
		
		publicAPIs.add(addPublicApi("POST", "^/shopify-notification/create-product"));
		publicAPIs.add(addPublicApi("POST", "^/shopify-notification/update-product"));
		publicAPIs.add(addPublicApi("POST", "^/shopify-notification/delete-product"));
		
		publicAPIs.add(addPublicApi("POST", "^/shopify-notification/create-order"));
		publicAPIs.add(addPublicApi("POST", "^/shopify-notification/update-order"));
		publicAPIs.add(addPublicApi("POST", "^/shopify-notification/delete-order"));
		publicAPIs.add(addPublicApi("POST", "^/shopify-notification/cancelled-order"));
		publicAPIs.add(addPublicApi("POST", "^/shopify-notification/shop/redact"));
		
		publicAPIs.add(addPublicApi("POST", "^/stripe-notification/dispute"));
		publicAPIs.add(addPublicApi("POST", "^/stripe-notification/dispute-update"));
		publicAPIs.add(addPublicApi("POST", "^/paypal-notification/dispute"));
		publicAPIs.add(addPublicApi("POST", "^/payoneer-mass-payout/create"));
		publicAPIs.add(addPublicApi("POST", "^/shopper/register"));
		publicAPIs.add(addPublicApi("POST", "^/shopper/login"));
		publicAPIs.add(addPublicApi("POST", "^/shopper/forgot"));
		publicAPIs.add(addPublicApi("POST", "^/shopper/password-update"));

		publicAPIs.add(addPublicApi("GET", "^/shopper/orders/list"));
		publicAPIs.add(addPublicApi("POST", "^/shopper/password-change"));
		publicAPIs.add(addPublicApi("GET", "^/shopper/info"));
		publicAPIs.add(addPublicApi("PUT", "^/shopper/info"));
		publicAPIs.add(addPublicApi("POST", "^/dropship/orders/import/:id"));
		publicAPIs.add(addPublicApi("POST", "^/dropship/orders/import"));
		publicAPIs.add(addPublicApi("POST", "^/dropship/v2/payment/active_topup"));
		publicAPIs.add(addPublicApi("POST", "^/dropship/v2/payment/topup"));
		publicAPIs.add(addPublicApi("GET", "^/orders/overview/user"));

//        publicAPIs.add(addPublicApi("GET", "^/orders/overview/detail/:id"));
		publicAPIs.add(addPublicApi("GET", "^/domains"));

		publicAPIs.add(addPublicApi("POST", "^/dropship/payment/paypal/create-invoice"));
		publicAPIs.add(addPublicApi("POST", "^/payments/invoice"));
		publicAPIs.add(addPublicApi("POST", "^/dropship/woocommerce/auth"));
		publicAPIs.add(addPublicApi("POST", "^/dropship/woocommerce/connect"));
		publicAPIs.add(addPublicApi("POST", "^/dropship/woocommerce/order"));
		publicAPIs.add(addPublicApi("POST", "^/email_marketing"));
		publicAPIs.add(addPublicApi("POST", "^/stripe/radar-warning"));
		publicAPIs.add(addPublicApi("POST", "^/dropship/payment/paypal/:id/refund"));
		publicAPIs.add(addPublicApi("POST", "^/stripe/radar-warning"));

		publicAPIs.add(addPublicApi("POST", "^/cms/aff-session-2896ec97-f629"));
		publicAPIs.add(addPublicApi("POST", "/users/human-verify"));

		publicAPIs.add(addPublicApi("POST", "/category/add"));
		publicAPIs.add(addPublicApi("PUT", "/category/update/:id"));
		publicAPIs.add(addPublicApi("GET", "/categories/domain"));

		publicAPIs.add(addPublicApi("GET", "/dropship/woocommerce/store/:id/order-pull"));
		publicAPIs.add(addPublicApi("POST", "/dropship/woocommerce/store/:id/order-sync"));
		publicAPIs.add(addPublicApi("GET", "/dropship/shopify/store/:id/order-pull"));
		publicAPIs.add(addPublicApi("POST", "/dropship/shopify/store/:id/order-sync"));

		publicAPIs.add(addPublicApi("POST", "/webhooks/paypal-ipn-listener-8090d89a"));

		publicAPIs.add(addPublicApi("POST", "/accounting/adjust/fulfillment-cost"));
		publicAPIs.add(addPublicApi("POST", "/accounting/adjust/aff-payout"));
		publicAPIs.add(addPublicApi("POST", "/dropship-api/order/v1"));
		publicAPIs.add(addPublicApi("POST", "/dropship-api/order/v1/cancel"));
		publicAPIs.add(addPublicApi("GET", "/dropship-api/order/v1/.*"));
		publicAPIs.add(addPublicApi("POST", "/dropship-api/order/v2"));
		publicAPIs.add(addPublicApi("POST", "/create-design-print/.*"));
		publicAPIs.add(addPublicApi("POST", "/order-review/update/.*"));
		publicAPIs.add(addPublicApi("POST", "/card-verification"));
		publicAPIs.add(addPublicApi("GET", "/shopify-app/check-store"));
		publicAPIs.add(addPublicApi("POST", "/shopify-app/connect"));
		publicAPIs.add(addPublicApi("GET", "/shopify-app/get-products"));
		publicAPIs.add(addPublicApi("POST", "^/s3_webhook"));
		publicAPIs.add(addPublicApi("GET", "/dropship-api/order/v2/check-log/.*"));
		publicAPIs.add(addPublicApi("GET", "/dropship/v2/orders"));

		//warehouse
		publicAPIs.add(addPublicApi("GET", "/warehouse/status/*"));
		publicAPIs.add(addPublicApi("POST", "/warehouse/update-orders"));
		publicAPIs.add(addPublicApi("GET", "/warehouse/list-orders"));
		
		publicAPIs.add(addPublicApi("PUT", "/assigned-to-partner"));
		
	}

	private static Map addPublicApi(String method, String requestURI) {
		Map api = new LinkedHashMap<>();
		api.put(AppParams.METHOD, method);
		api.put(AppParams.URL, requestURI);
		return api;
	}

	public static boolean isCallPublicAPI(String method, String uri) {
		if (publicAPIs == null || publicAPIs.isEmpty()) {
			initPublicApis();
		}
		return match(publicAPIs, method, uri);
	}

	public static boolean canCallAPI(List<Map> roles, String method, String uri) {
		return match(roles, method, uri);
	}

	private static boolean match(List<Map> roles, String method, String uri) {
		Pattern pattern = null;
		Matcher matcher = null;
		if (roles != null && !roles.isEmpty()) {
			for (Map role : roles) {
				String roleURI = (String) role.get(AppParams.URL);
				String roleMethod = (String) role.get(AppParams.METHOD);
				if (roleMethod.equalsIgnoreCase(method)) {
					pattern = Pattern.compile(roleURI);
					matcher = pattern.matcher(uri);
					if (matcher.find() && matcher.start() == 0) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static final Logger LOGGER = Logger.getLogger(UserPrivilegesHandler.class.getName());

}
