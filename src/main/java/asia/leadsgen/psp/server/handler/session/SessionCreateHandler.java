
package asia.leadsgen.psp.server.handler.session;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.AuthorizationException;
import asia.leadsgen.psp.exception.HttpServiceException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.exception.SystemException;
import asia.leadsgen.psp.service.DomainService;
import asia.leadsgen.psp.service.SessionService;
import asia.leadsgen.psp.service.UserService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.HttpClientUtil;
import asia.leadsgen.psp.util.HttpServiceConfig;
import asia.leadsgen.psp.util.IP2LocationService;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import asia.leadsgen.psp.util.UUIDUtil;
import asia.leadsgen.psp.util.UserAgentDetectionUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.RoutingContext;
import net.pieroxy.ua.detection.UserAgentDetectionResult;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class SessionCreateHandler implements Handler<RoutingContext> {

	private static long sessionExpireTime;
	private static long cookieExpireTime;

	private static HttpServiceConfig aspServiceConfig;

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
				
				String sourceIp = ContextUtil.getString(routingContext, AppParams.IP_ADDRESS);
				LOGGER.info("sourceIp1:=" + sourceIp);

				String authorizationToken = routingContext.getBodyAsString() != null
						&& !routingContext.getBodyAsString().isEmpty()
								? routingContext.getBodyAsJson().getString(AppParams.TOKEN)
								: "";

				if (authorizationToken != null && !authorizationToken.isEmpty()) {
					routingContext.put(AppParams.TOKEN, authorizationToken);
					LOGGER.info("authorizationToken != null");
					processAspTokenLookUpRequest(routingContext);
				} else {
					LOGGER.info("authorizationToken == null");
					responseSession(routingContext, new LinkedHashMap<>());
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

	private void processAspTokenLookUpRequest(RoutingContext routingContext) {
		String authorizationToken = ContextUtil.getString(routingContext, AppParams.TOKEN);

		Map queryParamsMap = new LinkedHashMap();
		queryParamsMap.put(AppParams.SCOPE, AppParams.PROFILE);

		String fspRequestURI = "/tokens/" + authorizationToken + "?scope=" + AppParams.PROFILE;

		HttpClientRequest aspClientRequest = HttpClientUtil.createHttpRequest(aspServiceConfig, fspRequestURI,
				HttpMethod.GET, queryParamsMap, StringPool.BLANK);

		aspClientRequest.handler(aspResponse -> aspTokenLookUpHandler(routingContext, aspResponse));

		aspClientRequest.exceptionHandler(throwable -> routingContext.fail(throwable));

		aspClientRequest.end();
	}

	private static void aspTokenLookUpHandler(RoutingContext routingContext, HttpClientResponse aspResponse) {

		int responseCode = aspResponse.statusCode();

		String responseMsg = aspResponse.statusMessage();

		aspResponse.bodyHandler(responseBody -> {

			try {
				
				JsonObject responseBodyJson = new JsonObject(responseBody.toString("UTF-8"));

				if (responseCode != HttpResponseStatus.OK.code()) {
					throw new HttpServiceException(responseCode, responseMsg, responseBodyJson.getMap());
				}

				if (responseBodyJson == null || responseBodyJson.isEmpty()) {
					throw new AuthorizationException(SystemError.INVALID_AUTH_TOKEN);
				}

				Map aspTokenInfoMap = responseBodyJson.getMap();

				String tokenState = ParamUtil.getString(aspTokenInfoMap, AppParams.STATE);

				if (!tokenState.equalsIgnoreCase(ResourceStates.APPROVED)) {
					throw new AuthorizationException(SystemError.INVALID_AUTH_TOKEN);
				}

				String tokenExpireTime = ParamUtil.getString(aspTokenInfoMap, AppParams.EXPIRE);
				LOGGER.info("isExpired=" + AppUtil.isExpired(tokenExpireTime));
				if (AppUtil.isExpired(tokenExpireTime)) {
					throw new AuthorizationException(SystemError.INVALID_AUTH_TOKEN);
				}

				Map aspUserProfileMap = ParamUtil.getMapData(aspTokenInfoMap, AppParams.USER);

				if (aspUserProfileMap.isEmpty()) {
					throw new AuthorizationException(SystemError.INVALID_USER);
				}

				String affUserId = ParamUtil.getString(aspUserProfileMap, AppParams.AFF_ID);
				if (StringUtils.isEmpty(affUserId)) {
					throw new SystemException(SystemError.INVALID_USER);
				}

				String aspUserId = ParamUtil.getString(aspUserProfileMap, AppParams.ID);
				String aspUserEmail = ParamUtil.getString(aspUserProfileMap, AppParams.EMAIL);

				if (aspUserId.isEmpty() || aspUserEmail.isEmpty()) {
					throw new AuthorizationException(SystemError.INVALID_USER);
				}

				String name = ParamUtil.getString(aspUserProfileMap, AppParams.NAME);

				String mobile = ParamUtil.getString(aspUserProfileMap, AppParams.MOBILE);

				String avatar = ParamUtil.getString(aspUserProfileMap, AppParams.AVATAR);

				String languageId = ParamUtil.getString(aspUserProfileMap, AppParams.LANGUAGE_ID,
						AppConstants.DEFAULT_LANGUAGE_ID);

				String timezone = ParamUtil.getString(aspUserProfileMap, AppParams.TIMEZONE,
						AppConstants.DEFAULT_TIMEZONE);

				Boolean isOwner = ParamUtil.getBoolean(aspUserProfileMap, AppParams.OWNER);

				Map aspUserAddr = ParamUtil.getMapData(aspUserProfileMap, AppParams.ADDRESS);

				String country = ParamUtil.getString(aspUserAddr, AppParams.COUNTRY_NAME);

				Map existingUser = UserService.get(affUserId);
				LOGGER.info("existingUser=" + existingUser.toString());
				if (isOwner) {

					if (existingUser.isEmpty()) {
						String referrer = ParamUtil.getString(aspUserProfileMap, AppParams.REFERRER);
						addNewUser(aspUserId, referrer, name, aspUserEmail, mobile, avatar, languageId, timezone,
								country);
					} else {
						String state = ParamUtil.getString(existingUser, AppParams.STATE);
						if (state.equalsIgnoreCase(ResourceStates.PENDING)) {
							responseForPendingUser(routingContext);
							return;
						} else {
							String aspUserState = ParamUtil.getString(aspUserProfileMap, AppParams.STATE);
							existingUser = updateExistingUser(aspUserId, name, aspUserEmail, mobile, avatar, languageId,
									timezone, aspUserState, country);
						}
					}
				}

				String affId = ParamUtil.getString(aspUserProfileMap, AppParams.AFF_ID);
				String userId = isOwner ? ParamUtil.getString(existingUser, AppParams.ID) : affId;
				int toolScripts = ParamUtil.getInt(existingUser, AppParams.TOOL_SCRIPTS, 0);
				
				int nPrivateCamp = ParamUtil.getInt(existingUser, "private_camp", 0);

				existingUser.put(AppParams.ASP_REF_ID, aspUserId);

				routingContext.put(AppParams.USER_ID, userId);
				routingContext.put(AppParams.USER_INFO, existingUser);
				routingContext.put(AppParams.OWNER, isOwner);
				routingContext.put(AppParams.AFF_ID, affId);
				routingContext.put(AppParams.ASP_REF_ID, aspUserId);

				aspUserProfileMap.put(AppParams.ID, affId);
				aspUserProfileMap.put(AppParams.AFF_ID, affId);
				aspUserProfileMap.put(AppParams.ASP_REF_ID, aspUserId);
				aspUserProfileMap.put(AppParams.REF_OWNER, ParamUtil.getBoolean(existingUser, AppParams.REF_OWNER));
				aspUserProfileMap.put(AppParams.TEST, ParamUtil.getBoolean(existingUser, AppParams.TEST));
				aspUserProfileMap.put(AppParams.TOOL_SCRIPTS, toolScripts);
				
				aspUserProfileMap.put("private_camp", nPrivateCamp);
				
				responseSession(routingContext, aspUserProfileMap);

			} catch (Exception e) {
				routingContext.fail(e);
			}
		});

		aspResponse.exceptionHandler(throwable -> routingContext.fail(throwable));
	}

	private static void responseSession(RoutingContext routingContext, Map userInfo) throws SQLException, IOException {

//		String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);
//		Map userRole = ParamUtil.getMapData(userInfo, AppParams.ROLE);
//		String roleName = ParamUtil.getString(userRole, AppParams.NAME);
		
		String affId = ParamUtil.getString(userInfo, AppParams.AFF_ID);
		
		// Save Login History
		saveLoginHistory(routingContext, affId);
		
		Map userInfoCopy = userInfo;
		
		Map sessionInfoMap = SessionService.save(affId + "-" + UUIDUtil.getUuid(), userInfo);

		
		Map permissions = ParamUtil.getMapData(userInfoCopy, AppParams.PERMISSIONS);
		permissions.remove(AppParams.ACCESSIBLE_APIS);
		userInfoCopy.put(AppParams.PERMISSIONS, permissions);
		sessionInfoMap.put(AppParams.USER, userInfoCopy);

		String host = routingContext.get(AppParams.HOST);
		Map defaultDomain = DomainService.getDefaultDomain(host);

		String domainName = ParamUtil.getString(defaultDomain, AppParams.NAME);

		Cookie sessionCookie = Cookie.cookie(AppParams.SESSION_ID, ParamUtil.getString(sessionInfoMap, AppParams.ID));
		sessionCookie.setDomain(domainName);
		sessionCookie.setPath(StringPool.FORWARD_SLASH);
		sessionCookie.setMaxAge(cookieExpireTime);

		if (userInfo.isEmpty()) {
			throw new LoginException(SystemError.LOGIN_REQUIRED);
		}

		Cookie userCookie = Cookie.cookie(AppParams.USER_EMAIL, ParamUtil.getString(userInfo, AppParams.EMAIL));
		userCookie.setDomain(domainName);
		userCookie.setPath(StringPool.FORWARD_SLASH);
		userCookie.setMaxAge(cookieExpireTime);

		routingContext.addCookie(sessionCookie);
		routingContext.addCookie(userCookie);

		routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
		routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());

		routingContext.put(AppParams.RESPONSE_DATA, sessionInfoMap);

		routingContext.next();
	}

	private static Map addNewUser(String aspUserId, String referrer, String name, String aspUserEmail, String mobile,
			String avatar, String languageId, String timezone, String country) throws SQLException {
		Map existingUser = UserService.insert(aspUserId, "", "", referrer, name, aspUserEmail, mobile, avatar,
				languageId, timezone, country);
		return existingUser;
	}

	private static void responseForPendingUser(RoutingContext routingContext) {
		routingContext.put(AppParams.RESPONSE_CODE, 412);
		routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.PRECONDITION_FAILED.reasonPhrase());
		routingContext.next();
	}

	private static Map updateExistingUser(String aspUserId, String name, String aspUserEmail, String mobile,
			String avatar, String languageId, String timezone, String state, String country) throws SQLException {
		Map existingUser = UserService.update(aspUserId, "", name, aspUserEmail, mobile, avatar, languageId, timezone,
				state, country);
		return existingUser;
	}
	
	private static void saveLoginHistory(RoutingContext routingContext, String affId) throws IOException, SQLException {
		String sourceIp = ContextUtil.getString(routingContext, AppParams.IP_ADDRESS);
		LOGGER.info("sourceIp:=" + sourceIp);
		if (sourceIp.contains(",")) {
			sourceIp = sourceIp.substring(0, sourceIp.indexOf(","));
		}
		Map geoInfo = IP2LocationService.getInstance().getGeoInfo(sourceIp);
		String countryName = ParamUtil.getString(geoInfo, AppParams.COUNTRY_NAME);
		String countryCode = ParamUtil.getString(geoInfo, AppParams.COUNTRY);
		String city = ParamUtil.getString(geoInfo, AppParams.CITY);
		String stateRegion = ParamUtil.getString(geoInfo, AppParams.STATE_REGION);
		
		String userAgent = ContextUtil.getString(routingContext, AppParams.USER_AGENT);
		UserAgentDetectionResult detectionResult = UserAgentDetectionUtil.getDetectionResult(userAgent);
		String device = UserAgentDetectionUtil.getDevice(detectionResult);
		String os = UserAgentDetectionUtil.getOperatingSystem(detectionResult);
		String osVersion = UserAgentDetectionUtil.getOperatingSystemVersion(detectionResult);
		String browsers = UserAgentDetectionUtil.getBrowser(detectionResult);
		String browsersVersion = UserAgentDetectionUtil.getBrowserVersion(detectionResult);
		
		UserService.insertLoginHistory(affId, sourceIp, countryName, countryCode, city, stateRegion, userAgent, device, os, osVersion, browsers, browsersVersion);
		
	}

	private static final Logger LOGGER = Logger.getLogger(SessionCreateHandler.class.getName());
}
