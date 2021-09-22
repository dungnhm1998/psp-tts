package asia.leadsgen.psp.server.handler.privileges;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.JSONStringToMapUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringUtil;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UserPrivilegesHandler implements Handler<RoutingContext> {

	private static String AUTHORIZE_HEADER = "X-Authorization";
	private static int endOfBearer = 7;


	@Override
	public void handle(RoutingContext routingContext) {
		try {
			LOGGER.info("UserPrivilegesHandler");
			HttpServerRequest httpServerRequest = routingContext.request();
			String jwtToken = httpServerRequest.getHeader(AUTHORIZE_HEADER);

			if (!StringUtils.isEmpty(jwtToken)) {
				String token = jwtToken.substring(endOfBearer, jwtToken.length());
				Map<String,String> userInfo = decodeJWT(token);

				String userId = userInfo.get(AppParams.USER_ID).toString();
				String aspRefId = userInfo.get(AppParams.ASP_REF_ID).toString();
				String owner = userInfo.get(AppParams.OWNER);

				Boolean isOwner = owner.equalsIgnoreCase("yes");
				String timeZone = userInfo.get(AppParams.TIMEZONE).toString();

				Set<String> domainNames = new HashSet<>();
				Set<String> module = new HashSet<>();
				Set<String> global = new HashSet<>();
				List<String> storeDropship = new ArrayList<>();

				if (!isOwner) {
					String domains = ParamUtil.getString(userInfo, AppParams.DOMAINS);
					String modulePermissions = ParamUtil.getString(userInfo, AppParams.MODULE_PERMISSIONS);
					String globalPermissions = ParamUtil.getString(userInfo, AppParams.GLOBAL_PERMISSIONS);
					domainNames = StringUtils.isEmpty(domains) ? null : new HashSet<>(Arrays.asList(domains.split(",")));
					module = StringUtils.isEmpty(modulePermissions) ? null : new HashSet<>(Arrays.asList(modulePermissions.split(",")));
					global = StringUtils.isEmpty(globalPermissions) ? null : new HashSet<>(Arrays.asList(globalPermissions.split(",")));

					storeDropship = CollectionUtils.isEmpty(domainNames)
							? null
							: domainNames.stream().filter(d -> !d.contains(".")).collect(Collectors.toList());
				}



				routingContext.put(AppParams.USER_ID, userId);
				routingContext.put(AppParams.ASP_REF_ID, aspRefId);
				routingContext.put(AppParams.OWNER, isOwner);
				routingContext.put(AppParams.DOMAINS, domainNames);
				routingContext.put(AppParams.TIMEZONE, timeZone.replaceAll("UTC", "GMT"));
				routingContext.put(AppParams.MODULE_PERMISSIONS, module);
				routingContext.put(AppParams.GLOBAL_PERMISSIONS, global);
				routingContext.put(AppParams.STORES, storeDropship);

			}

			routingContext.next();

		} catch (Exception e) {
			routingContext.fail(e);
		}
	}

	public Map decodeJWT(String jwtToken) throws Exception {
		String[] split_string = jwtToken.split("\\.");
		String base64EncodedBody = split_string[1];

		Base64 base64Url = new Base64();

		String body = new String((byte[]) base64Url.decode(base64EncodedBody));

		Map payload = JSONStringToMapUtil.toMap(body);
		return encryptMap(payload);
	}

	private String decryptS5(String input) throws Exception {

		byte[] cipherText = java.util.Base64.getDecoder().decode(input);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING", "SunJCE");

		SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");

		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[16]));

		return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
	}

	private Map encryptMap(Map orgMap) throws Exception {
		Map<String, String> resultMap = new HashMap<>();

		Set<String> keySet = orgMap.keySet();
		for (String key : keySet) {
			String newKey = decryptS5(key);
			String value = decryptS5(orgMap.get(key).toString());
			resultMap.put(newKey, value);
		}
		return resultMap;
	}

	private String encryptionKey = "HjCAqlWuSpQNkhsT";

	private static final Logger LOGGER = Logger.getLogger(UserPrivilegesHandler.class.getName());
}


