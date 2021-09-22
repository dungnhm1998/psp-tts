package asia.leadsgen.psp.util;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.data.type.BasePhoneCase;
import asia.leadsgen.psp.data.type.BasePhoneCaseDropship;

public class BasePhoneCaseUtil {

	private static String basePhoneCaseIds;
	
	private static String basePhoneCaseDropshipIds;

	public static boolean isPhoneCase(String s) {

		boolean match = false;
		for (BasePhoneCase b : BasePhoneCase.values()) {
			if (s.equals(b.getValue())) {
				match = true;
				break;
			}
		}
		return match;

	}

	public static String getBasePhoneCaseIds() {

		if (StringUtils.isEmpty(basePhoneCaseIds)) {

			Set<String> ids = new HashSet<String>();

			for (BasePhoneCase b : BasePhoneCase.values()) {
				ids.add(b.getValue());
			}

			basePhoneCaseIds = String.join(",", ids);
		}

		return basePhoneCaseIds;

	}
	
	public static boolean isPhoneCaseDropship(String s) {

		boolean match = false;
		for (BasePhoneCaseDropship b : BasePhoneCaseDropship.values()) {
			if (s.equals(b.getValue())) {
				match = true;
				break;
			}
		}
		return match;

	}
	
	public static String getBasePhoneCaseDropshipIds() {

		if (StringUtils.isEmpty(basePhoneCaseDropshipIds)) {

			Set<String> ids = new HashSet<String>();

			for (BasePhoneCaseDropship b : BasePhoneCaseDropship.values()) {
				ids.add(b.getValue());
			}

			basePhoneCaseDropshipIds = String.join(",", ids);
		}

		return basePhoneCaseDropshipIds;

	}

}
