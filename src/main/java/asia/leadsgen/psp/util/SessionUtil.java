package asia.leadsgen.psp.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;

public class SessionUtil {
	
	@SuppressWarnings("unchecked")
	public static Map checkPermissionDomainAndStore(Boolean isOwner, List<Map> domainMapList) {

		Map domainAndStoreMap = new HashedMap();
		if (isOwner) {

		} else {
			String pattern = "(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]"; // regex pattern check if String matches Domain

			domainAndStoreMap.put(AppParams.DOMAINS,  domainMapList.stream()
																.filter(o ->  ParamUtil.getString(o, AppParams.NAME).matches(pattern))
																.map(o -> ParamUtil.getString(o, AppParams.NAME))
																.collect(Collectors.toList()));

			domainAndStoreMap.put(AppParams.STORES, domainMapList.stream()
																.filter(o -> !ParamUtil.getString(o, AppParams.NAME).contains("."))
																.map(o -> ParamUtil.getString(o, AppParams.NAME))
																.collect(Collectors.toList()));
		}
		return domainAndStoreMap;
	}
}
