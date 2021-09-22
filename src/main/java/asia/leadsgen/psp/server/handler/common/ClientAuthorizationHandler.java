package asia.leadsgen.psp.server.handler.common;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.AuthorizationException;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service.ClientService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.ShopbaseUtil;
import asia.leadsgen.psp.util.ShopifyUtil;
import asia.leadsgen.psp.util.StringPool;
import asia.leadsgen.psp.util.UserPrivilegesUtil;
import asia.leadsgen.security.wss.Authorization;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class ClientAuthorizationHandler implements Handler<RoutingContext> {

	private static String serviceName;
	private static String serviceRegion;
	private static String serviceAuthType;
	private static String serviceAuthAlgorithm;

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setServiceRegion(String serviceRegion) {
		this.serviceRegion = serviceRegion;
	}

	public void setServiceAuthType(String serviceAuthType) {
		this.serviceAuthType = serviceAuthType;
	}

	public void setServiceAuthAlgorithm(String serviceAuthAlgorithm) {
		this.serviceAuthAlgorithm = serviceAuthAlgorithm;
	}

	@Override
	public void handle(RoutingContext routingContext) {

		HttpServerRequest httpServerRequest = routingContext.request();

		MultiMap requestHeaders = httpServerRequest.headers().getDelegate();

		MultiMap requestParams = httpServerRequest.params().getDelegate();

		String contentType = requestHeaders.get(HttpHeaders.CONTENT_TYPE);

		String requestUri = httpServerRequest.uri();

		String methodName = httpServerRequest.method().name();

		if (requestUri.contains("/pspfulfill/api/token")) {
			routingContext.next();
			// DO NOTHING
		} else if (ShopifyUtil.isShopifyWebHookRequest(httpServerRequest)
				|| ShopbaseUtil.isShopbaseWebHookRequest(httpServerRequest)) {
			routingContext.next();
			// DO NOTHING
		} else if (isSendGridRequest(requestUri)) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name()) && requestUri.contains("/email-list/upload")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name()) && requestUri.contains("tsc-notification")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name()) && requestUri.contains("shopify-notification")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name()) && requestUri.contains("stripe-notification")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name())
				&& requestUri.contains("/dropship/woocommerce/auth")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name()) && requestUri.contains("/payments/invoice")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.GET.name()) && requestUri.contains("/shopify-app/check-store")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name()) && requestUri.contains("/shopify-app/connect")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.GET.name()) && requestUri.contains("/shopify-app/get-products")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name()) && requestUri.contains("/shopify-notification/shop/redact")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name()) && requestUri.contains("/s3_webhook")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.GET.name()) && requestUri.contains("/dropship/v2/orders")) {
			routingContext.next();
			// DO NOTHING
		}
		
		else if ((methodName.equalsIgnoreCase(HttpMethod.GET.name()) || methodName.equalsIgnoreCase(HttpMethod.POST.name())) && requestUri.contains("/warehouse")) {
			routingContext.next();
		}

		else if (methodName.equalsIgnoreCase(HttpMethod.POST.name())
				&& requestUri.contains("/dropship/payment/paypal")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name())
				&& requestUri.contains("/stripe/radar-warning")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name())
				&& requestUri.contains("/webhooks/paypal-ipn-listener-8090d89a")) {
			routingContext.next();
		} else if (isPublicDropshipAPI(requestUri)) {
			routingContext.next();
		}

//        else if (methodName.equalsIgnoreCase(HttpMethod.GET.name()) && requestUri.contains("/orders/overview/detail")) {
//            routingContext.next();
//            // DO NOTHING
//        }
//         
		else if (methodName.equalsIgnoreCase(HttpMethod.POST.name())
				&& requestUri.contains("/dropship/woocommerce/order")) {
			routingContext.next();
			// DO NOTHING
		} else if (methodName.equalsIgnoreCase(HttpMethod.POST.name()) && requestUri.contains("shopper/")) {
			routingContext.next();
			// DO NOTHING
		} else if (UserPrivilegesUtil.bypassClientAndSessionChecking(methodName, requestUri)) {
			// :TODO Move all above stuff to
			// UserPrivilegesUtil.bypassClientAndSessionChecking
			routingContext.next();
		} else {

			if ((!methodName.equalsIgnoreCase(HttpMethod.GET.name())
					&& !methodName.equalsIgnoreCase(HttpMethod.DELETE.name()))
					&& (contentType == null
							|| !contentType.equalsIgnoreCase(AppConstants.CONTENT_TYPE_APPLICATION_JSON))) {
				throw new BadRequestException(SystemError.UNSUPPORTED_CONTENT_TYPE);
			}

			String requestTime = requestHeaders.get(AppParams.X_DATE);

			if (requestTime == null || requestTime.isEmpty()) {
				throw new BadRequestException(SystemError.INVALID_HEADER_X_DATE);
			}

			int requestAuthorizationExpires = GetterUtil.getInteger(requestHeaders.get(AppParams.X_EXPIRES), 0);

			if (requestAuthorizationExpires <= 0) {
				throw new BadRequestException(SystemError.INVALID_HEADER_X_EXPIRES);
			}

			String authorizationHeader = requestHeaders.get(AppParams.X_AUTHORIZATION);

			if (authorizationHeader == null || authorizationHeader.isEmpty()) {
				throw new BadRequestException(SystemError.INVALID_HEADER_X_AUTHORIZATION);
			}

			String clientId = getClientId(authorizationHeader);

			if (clientId == null || clientId.isEmpty()) {
				throw new AuthorizationException(SystemError.INVALID_CLIENT);
			}

			routingContext.put(AppParams.CLIENT_ID, clientId);

			routingContext.vertx().executeBlocking(future -> {

				try {

					Map clientInfoMap = ClientService.get(clientId, true);

					String state = ParamUtil.getString(clientInfoMap, AppParams.STATE);

					if (!state.equals(ResourceStates.APPROVED)) {
						throw new AuthorizationException(SystemError.INVALID_CLIENT);
					}

					routingContext.put(AppParams.CLIENT_INFO, clientInfoMap);

					boolean signatureCheck = ParamUtil.getBoolean(clientInfoMap, AppParams.AUTH_CHECK);

					if (signatureCheck) {

						String clientAccessKey = ParamUtil.getString(clientInfoMap, AppParams.KEY);

						Authorization requestAuthorization = new Authorization(authorizationHeader, requestTime,
								requestAuthorizationExpires);

						if (requestAuthorization.isExpired()) {
							throw new AuthorizationException(SystemError.OPERATION_EXPIRED);
						}

						String requestServiceName = requestAuthorization.getServiceName();

						if (!requestServiceName.equalsIgnoreCase(serviceName)) {
							throw new AuthorizationException(SystemError.INVALID_SERVICE_NAME);
						}

						String requestServiceRegion = requestAuthorization.getServiceRegion();

						if (!requestServiceRegion.equalsIgnoreCase(serviceRegion)) {
							throw new AuthorizationException(SystemError.INVALID_SERVICE_REGION);
						}

						String requestAuthorizationType = requestAuthorization.getTerminator();

						if (!requestAuthorizationType.equalsIgnoreCase(serviceAuthType)) {
							throw new AuthorizationException(SystemError.INVALID_AUTHORIZATION_TYPE);
						}

						String requestAuthorizationAlgorithm = requestAuthorization.getAlgorithm();

						if (!requestAuthorizationAlgorithm.equalsIgnoreCase(serviceAuthAlgorithm)) {
							throw new AuthorizationException(SystemError.INVALID_AUTHORIZATION_ALGORITHM);
						}

						Map<String, String> requestSignedHeader = requestAuthorization.getSignedHeaderMap();

						Date requestDate = requestAuthorization.getTimeStamp();

						String requestSignature = requestAuthorization.getSignature();

						Buffer payload = routingContext.getBody().getDelegate();

						Map<String, String> signedHeaderMap = new LinkedHashMap<>();

						for (Map.Entry<String, String> entry : requestHeaders) {
							if (requestSignedHeader.containsKey(entry.getKey().toLowerCase())) {
								signedHeaderMap.put(entry.getKey(), entry.getValue());
							}
						}

						Map<String, String> queryParameterMap = new LinkedHashMap<>();

						for (Map.Entry<String, String> entry : requestParams.entries()) {
							queryParameterMap.put(entry.getKey(), entry.getValue());
						}

						Authorization serverAuthorization = new Authorization(clientId, clientAccessKey,
								requestServiceRegion, requestServiceName, httpServerRequest.method().name(),
								httpServerRequest.path(), queryParameterMap, signedHeaderMap, payload.getBytes(),
								requestDate, requestAuthorizationExpires);

						String serverSignature = serverAuthorization.getSignature();

						if (!requestSignature.equals(serverSignature)) {

							Map<String, String> debugInfoMap = serverAuthorization.getDebugInfoMap();

							for (Map.Entry<String, String> entry : debugInfoMap.entrySet()) {
								LOGGER.info(entry.getKey() + ":" + entry.getValue());
							}

							throw new AuthorizationException(SystemError.INVALID_SERVICE_SIGNATURE);
						}
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

	private boolean isSendGridRequest(String requestUri) {
		return requestUri.contains("/webhooks/send-email");
	}

	public static boolean isPublicDropshipAPI(String uri) {
		return uri.contains("/dropship-api/order/v1") || uri.contains("/dropship-api/order/v2");
	}

	private static String getClientId(String authorizationHeader) {

		Pattern pattern = Pattern.compile(AppConstants.SIGNATURE_HEADER_CLIENT_ID_EXTRACT_REGEX);

		Matcher matcher = pattern.matcher(authorizationHeader);

		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return StringPool.BLANK;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ClientAuthorizationHandler.class.getName());
}
