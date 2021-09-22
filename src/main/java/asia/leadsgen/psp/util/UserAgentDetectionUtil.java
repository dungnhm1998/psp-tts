package asia.leadsgen.psp.util;

import net.pieroxy.ua.detection.Browser;
import net.pieroxy.ua.detection.BrowserFamily;
import net.pieroxy.ua.detection.Device;
import net.pieroxy.ua.detection.DeviceType;
import net.pieroxy.ua.detection.OS;
import net.pieroxy.ua.detection.OSFamily;
import net.pieroxy.ua.detection.UserAgentDetectionResult;
import net.pieroxy.ua.detection.UserAgentDetector;

public class UserAgentDetectionUtil {

	private static UserAgentDetector userAgentDetector = new UserAgentDetector();

	public static UserAgentDetectionResult getDetectionResult(String userAgent) {
		return userAgentDetector.parseUserAgent(userAgent);
	}

//	public static String getDevice(UserAgentDetectionResult userAgentDetectionResult) {
//		Device device = userAgentDetectionResult.getDevice();
//		DeviceType deviceType = device.getDeviceType();
//		String deviceName = device.getDevice();
//		if (deviceType.isMobile() && deviceName.equalsIgnoreCase("Unknown")) {
//			Browser browser = userAgentDetectionResult.getBrowser();
//			deviceName = browser.getVendor().getLabel();
//		} else if (!deviceType.isMobile() && StringUtils.isEmpty(deviceName)) {
//			deviceName = deviceType.getLabel();
//		}
//		return deviceName.isEmpty() ? "Unknown" : deviceName;
//	}
	
	public static String getDevice(UserAgentDetectionResult userAgentDetectionResult) {
		Device device = userAgentDetectionResult.getDevice();
		DeviceType deviceType = device.getDeviceType();
		String deviceName = deviceType.getLabel();
		
		return deviceName.isEmpty() ? "Unknown" : deviceName;
	}

	public static String getOperatingSystem(UserAgentDetectionResult userAgentDetectionResult) {
		OS operatingSystem = userAgentDetectionResult.getOperatingSystem();
		OSFamily osf = operatingSystem.getFamily();
		return osf.getLabel().isEmpty() ? "Unknown" : osf.getLabel();
	}

	public static String getOperatingSystemVersion(UserAgentDetectionResult userAgentDetectionResult) {
		OS operatingSystem = userAgentDetectionResult.getOperatingSystem();
		return operatingSystem.getVersion().isEmpty() ? "Unknown" : operatingSystem.getVersion();
	}

	public static String getBrowser(UserAgentDetectionResult userAgentDetectionResult) {
		Browser browser = userAgentDetectionResult.getBrowser();
		BrowserFamily browserFamily = browser.getFamily();
		return browserFamily.getLabel().isEmpty() ? "Unknown" : browserFamily.getLabel();
	}

	public static String getBrowserVersion(UserAgentDetectionResult userAgentDetectionResult) {
		Browser browser = userAgentDetectionResult.getBrowser();
		return browser.getVersion().isEmpty() ? "Unknown" : browser.getVersion();
	}

}
