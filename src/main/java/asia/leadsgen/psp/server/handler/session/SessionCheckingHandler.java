package asia.leadsgen.psp.server.handler.session;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.server.handler.common.ClientAuthorizationHandler;
import asia.leadsgen.psp.service.DomainService;
import asia.leadsgen.psp.service.RedisService;
import asia.leadsgen.psp.service.SessionService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.SessionUtil;
import asia.leadsgen.psp.util.ShopbaseUtil;
import asia.leadsgen.psp.util.ShopifyUtil;
import asia.leadsgen.psp.util.StringPool;
import asia.leadsgen.psp.util.UUIDUtil;
import asia.leadsgen.psp.util.UserPrivilegesUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class SessionCheckingHandler implements Handler<RoutingContext> {

	private static long sessionExpireTime;
	private static long cookieExpireTime;
	private static HttpServiceConfig aspServiceConfig;

	static final String TOOL_REDIRECT_URL = "https://www.bbc.co.uk";

	public void setSessionExpireTime(long sessionExpireTime) {
		this.sessionExpireTime = sessionExpireTime;
	}

	public void setCookieExpireTime(long cookieExpireTime) {
		this.cookieExpireTime = cookieExpireTime;
	}

	public void setAspServiceConfig(HttpServiceConfig aspServiceConfig) {
		this.aspServiceConfig = aspServiceConfig;
	}

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {

			try {

				Map clientInfoMap = ContextUtil.getMapData(routingContext, AppParams.CLIENT_INFO);

				boolean signatureCheck = ParamUtil.getBoolean(clientInfoMap, AppParams.AUTH_CHECK);

				HttpServerRequest httpServerRequest = routingContext.request();

				String requestUri = httpServerRequest.uri();

				String requestMethod = httpServerRequest.method().name();

				if (requestUri.contains("/pspfulfill/api/token")) {
					LOGGER.info("requestUri: " + requestUri);
					// DO NOTHING
				} else if (ShopifyUtil.isShopifyWebHookRequest(httpServerRequest)
						|| ShopbaseUtil.isShopbaseWebHookRequest(httpServerRequest)) {
					// DO NOTHING
				} else if (isSendGridRequest(requestUri)) {
					// DO NOTHING
				} else if (requestUri.contains("email-tracking")) {
					// DO NOTHING
				} else if (requestUri.contains("tsc-notification")) {
					// DO NOTHING
				} else if (requestUri.contains("shopify-notification")) {
					// DO NOTHING
				} else if (requestUri.contains("stripe-notification")) {
					// DO NOTHING
				} else if (requestUri.contains("shopper/")) {
					// DO NOTHING
				} else if (requestUri.contains("/dropship/woocommerce/auth")) {
					// DO NOTHING
				} else if (requestUri.contains("/payments/invoice")) {
					// DO NOTHING
				} else if (requestUri.contains("dropship/payment/paypal")) {
					// DO NOTHING
				} else if (requestUri.contains("stripe/radar-warning")) {
					// DO NOTHING
				} else if (requestUri.contains("dropship/woocommerce/order")) {
					// DO NOTHING
				} else if (UserPrivilegesUtil.bypassClientAndSessionChecking(requestMethod, requestUri)) {
					// :TODO Move all above stuff to
					// UserPrivilegesUtil.bypassClientAndSessionChecking
				} else if (requestUri.contains("/webhooks/paypal-ipn-listener-8090d89a")) {
				} else if (ClientAuthorizationHandler.isPublicDropshipAPI(requestUri)) {
				} else if (!(requestUri.contains("/pspfulfill/api/v2/sessions")
						&& (requestMethod.equalsIgnoreCase(HttpMethod.POST.name())
								|| requestMethod.equalsIgnoreCase(HttpMethod.DELETE.name())))) {

					Map sessionInfoMap;

					String sessionId = routingContext.getCookie(AppParams.SESSION_ID) != null
							? routingContext.getCookie(AppParams.SESSION_ID).getValue()
							: StringPool.BLANK;

					sessionInfoMap = createSessionInfoMap(sessionId);
					doesRequireLogin(routingContext, sessionInfoMap);
					String host = routingContext.get(AppParams.HOST);
					Map defaultDomain = DomainService.getDefaultDomain(host);
					String domainName = ParamUtil.getString(defaultDomain, AppParams.NAME);
					Cookie sessionCookie = createSessionCookie(sessionInfoMap, domainName);
					Map userInfoMap = createUserInfoMap(routingContext, sessionInfoMap);
					Cookie userCookie = createUserCookie(domainName, userInfoMap);
					routingContext.addCookie(sessionCookie);
					routingContext.addCookie(userCookie);
					routingContext.put(AppParams.SESSION_ID, ParamUtil.getString(sessionInfoMap, AppParams.ID));
					routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
					routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
					routingContext.put(AppParams.RESPONSE_DATA, sessionInfoMap);
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

	private Map createSessionInfoMap(String sessionId) throws SQLException, ParseException {
		Map sessionInfoMap;
		if (sessionId.isEmpty()) {
			sessionInfoMap = createSession();
		} else {
			sessionInfoMap = SessionService.find(sessionId);
			if (sessionInfoMap == null) {
				sessionInfoMap = createSession();
			} else {
				sessionInfoMap = extendSession(sessionId);
			}
		}
		return sessionInfoMap;
	}

	private Map createUserInfoMap(RoutingContext routingContext, Map sessionInfoMap) {

		Map userInfoMap = ParamUtil.getMapData(sessionInfoMap, AppParams.USER);
		
		if (userInfoMap != null && !userInfoMap.isEmpty()) {
			String userId = ParamUtil.getString(userInfoMap, AppParams.ID);
			String aspRefId = ParamUtil.getString(userInfoMap, AppParams.ASP_REF_ID);
			String timezone = ParamUtil.getString(userInfoMap, AppParams.TIMEZONE);

			Boolean isOwner = ParamUtil.getBoolean(userInfoMap, AppParams.OWNER);

			Map permissionMap = ParamUtil.getMapData(userInfoMap, AppParams.PERMISSIONS);
			List<Map> accessibleAPIList = ParamUtil.getListData(permissionMap, AppParams.ACCESSIBLE_APIS);

			List<Map> domainMapList = ParamUtil.getListData(permissionMap, AppParams.DOMAINS);
			Map domainAndStoreMap = SessionUtil.checkPermissionDomainAndStore(isOwner, domainMapList);

			List<Map> globalPermissionList = ParamUtil.getListData(permissionMap, AppParams.GLOBAL_PERMISSIONS);
			List<Map> modulePermissionList = ParamUtil.getListData(permissionMap, AppParams.MODULE_PERMISSIONS);

			routingContext.put(AppParams.USER_ID, userId);
			routingContext.put(AppParams.ASP_REF_ID, aspRefId);
			routingContext.put(AppParams.TIMEZONE, timezone.replaceAll("UTC", "GMT"));
			routingContext.put(AppParams.OWNER, isOwner);
			routingContext.put(AppParams.ACCESSIBLE_APIS, accessibleAPIList);

			routingContext.put(AppParams.DOMAINS, ParamUtil.getListData(domainAndStoreMap, AppParams.DOMAINS));
			routingContext.put(AppParams.STORES, ParamUtil.getListData(domainAndStoreMap, AppParams.STORES));
			
			LOGGER.info("session check list domain: " + ParamUtil.getListData(domainAndStoreMap, AppParams.DOMAINS));
			LOGGER.info("session check list store: " + ParamUtil.getListData(domainAndStoreMap, AppParams.STORES));

			routingContext.put(AppParams.GLOBAL_PERMISSIONS, globalPermissionList.stream()
					.map(o -> ParamUtil.getString(o, AppParams.ACTION)).collect(Collectors.toSet()));
			routingContext.put(AppParams.MODULE_PERMISSIONS, modulePermissionList.stream()
					.map(o -> ParamUtil.getString(o, AppParams.ACTION)).collect(Collectors.toSet()));
		}
		return userInfoMap;
	}

	private boolean isSendGridRequest(String requestUri) {
		return requestUri.contains("/webhooks/send-email");
	}

	private Cookie createSessionCookie(Map sessionInfoMap, String domainName) {
		Cookie sessionCookie = Cookie.cookie(AppParams.SESSION_ID, ParamUtil.getString(sessionInfoMap, AppParams.ID));
		sessionCookie.setDomain(domainName);
		sessionCookie.setPath(StringPool.FORWARD_SLASH);
		sessionCookie.setMaxAge(cookieExpireTime);
		return sessionCookie;
	}

	private Cookie createUserCookie(String domainName, Map userInfoMap) {
		Cookie userCookie = Cookie.cookie(AppParams.USER_EMAIL, ParamUtil.getString(userInfoMap, AppParams.EMAIL));
		userCookie.setDomain(domainName);
		userCookie.setPath(StringPool.FORWARD_SLASH);
		userCookie.setMaxAge(cookieExpireTime);
		return userCookie;
	}

	private void doesRequireLogin(RoutingContext routingContext, Map sessionInfoMap) {

		Map userInfo = ParamUtil.getMapData(sessionInfoMap, AppParams.USER);

		String clientId = ContextUtil.getString(routingContext, AppParams.CLIENT_ID);

		if (clientId.equals(AppConstants.DEFAULT_CLIENT_ID) && userInfo.isEmpty()) {
			throw new LoginException(SystemError.LOGIN_REQUIRED);
		}
	}

	private static Map createSession() {
		String sessionId = UUIDUtil.getUuid();
		return SessionService.save(sessionId, new LinkedHashMap<>());

	}

	private static Map extendSession(String sessionId) throws ParseException, SQLException {

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(calendar.getTimeInMillis() + sessionExpireTime);

		SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

		// dateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));
		String expireTime = dateFormat.format(calendar.getTime());

		Map sessionInfo = RedisService.get(sessionId);
		sessionInfo.remove(AppParams.EXPIRE_TIME);
		sessionInfo.put(AppParams.EXPIRE_TIME, expireTime);

		return SessionService.extend(sessionId, sessionInfo);

	}

	private static final Logger LOGGER = Logger.getLogger(SessionCheckingHandler.class.getName());
}
