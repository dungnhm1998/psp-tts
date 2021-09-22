package asia.leadsgen.psp.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;

/**
 * Created by hungdx on 4/1/17.
 */
public class SessionService {

	private static DataSource dataSource;

	private static long sessionExpireTime;

	public static void setSessionExpireTime(long sessionExpireTime) {
		SessionService.sessionExpireTime = sessionExpireTime;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static Map find(String id) {

		LOGGER.fine("Session look up with id=" + id);

		Map session = RedisService.get(id);

		if (session != null) {
			LOGGER.fine("=> Session look up result: " + session.toString());
		}

		return session;
	}

	public static Map extend(String sessionId, Map sessionInfo) {
		return RedisService.save(sessionId, sessionInfo, sessionExpireTime, TimeUnit.MILLISECONDS);
	}

	public static Map save(String sessionId, Map userInfo) {

		Calendar calendar = Calendar.getInstance();

		SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

		// dateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));

		String createTime = dateFormat.format(calendar.getTime());

		calendar.setTimeInMillis(calendar.getTimeInMillis() + sessionExpireTime);

		String expireTime = dateFormat.format(calendar.getTime());

//		String userId = userInfo.isEmpty() ? "" : ParamUtil.getString(userInfo, AppParams.ID);
//		LOGGER.info("Session insert with userId =" + userId + ", createTime=" + createTime + ", expireTime=" + expireTime);

		Map sessionMap = new LinkedHashMap<>();
		sessionMap.put(AppParams.ID, sessionId);
		sessionMap.put(AppParams.CREATE_TIME, createTime);
		sessionMap.put(AppParams.EXPIRE_TIME, expireTime);
		sessionMap.put(AppParams.STATE, ResourceStates.APPROVED);
		sessionMap.put(AppParams.USER, userInfo);
		LOGGER.info("Session service userInfo = " + userInfo.toString());

		Map session = RedisService.save(sessionId, sessionMap, sessionExpireTime, TimeUnit.MILLISECONDS);

		return session;
	}

	public static void delete(String sessionId) {
		RedisService.delete(sessionId);
	}

	public static void markToolScripts(String userId, int toolScriptsFlag) {
		if (StringUtils.isNotEmpty(userId)) {
			Set<String> sessions = RedisService.getKeysMatch(userId);
			if (CollectionUtils.isNotEmpty(sessions)) {
//				logger.info("matches " + sessions.size() + " sessions");
				for (String ss : sessions) {
					Map sessionInfo = SessionService.find(ss);
//					logger.info("session " + ss + " | " + sessionInfo.toString());
					if (sessionInfo != null && sessionInfo.isEmpty() == false) {
						Map ssUser = ParamUtil.getMapData(sessionInfo, AppParams.USER);
						if (ssUser != null && ssUser.isEmpty() == false) {
							ssUser.put(AppParams.TOOL_SCRIPTS, toolScriptsFlag);
							sessionInfo.put(AppParams.USER, ssUser);
//							logger.info("updated session " + ss + " | " + sessionInfo.toString());
							SessionService.extend(ss, sessionInfo);
						}
					}
				}
			}
		}
	}

	private static final Logger LOGGER = Logger.getLogger(SessionService.class.getName());
}
