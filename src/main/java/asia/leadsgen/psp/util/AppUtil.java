package asia.leadsgen.psp.util;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.util.Base64Utils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;

/**
 * Created by HungDX on 27-Jan-16.
 */
public class AppUtil {

	public static byte[] decodeBase64(String source) {

		try {

			return Base64Utils.decodeFromString(source);

		} catch (Exception e) {
			throw new BadRequestException(SystemError.INVALID_BASE64_DATA);
		}
	}

	public static boolean isExpired(String yyyyMMddTHHmmssZ) {

		boolean expired = false;

		try {

			SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

//			dateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));

			Date expireDate = dateFormat.parse(yyyyMMddTHHmmssZ);

			Date now = new Date();

			expired = expireDate.before(now);

		} catch (ParseException e) {
			LOGGER.log(Level.SEVERE, "[DATE PARSER ERROR] Invalid date time format!", e);
		}

		return expired;
	}

	public static boolean isExpiredAfter2Hours(String yyyyMMddTHHmmssZ) {

		boolean expired = false;

		try {

			SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

//			dateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));

			Date expireDate = dateFormat.parse(yyyyMMddTHHmmssZ);

			Date hoursAgo = DateUtils.addHours(new Date(), -2);

			expired = expireDate.before(hoursAgo);

		} catch (ParseException e) {
			LOGGER.log(Level.SEVERE, "[DATE PARSER ERROR] Invalid date time format!", e);
		}

		return expired;
	}

	public static String generateFriendlyUrl(String source) throws Exception {

		String ret = "";
		int t;
		for (int i = 0; i < source.length(); i++) {
			t = (int) source.charAt(i);
			if (((t >= 48) && (t <= 57)) || ((t >= 65) && (t <= 90)) || ((t >= 97) && (t <= 122)) || (t == 45)
					|| (t == 32) || (t == 93) || (t == 47)) {
				if (t == 93) {
					ret += " ";
				} else {
					ret += source.charAt(i);
				}
			} else if ((t == 10) || (t == 13)) {
				break;
			}
		}

		ret = ret.trim();

		ret = ret.replaceAll(" ", "-").toLowerCase();
		
		ret = ret.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")

		.replaceAll("[^\\p{Alnum}]+", "-");

		while (ret.indexOf("--") >= 0) {
			ret = ret.replaceAll("--", "-");
		}

		return ret;

	}

	public static String generateFriendlyText(String source) throws Exception {

		String ret = "";
		int t;
		for (int i = 0; i < source.length(); i++) {
			t = (int) source.charAt(i);
			if (((t >= 48) && (t <= 57)) || ((t >= 65) && (t <= 90)) || ((t >= 97) && (t <= 122)) || (t == 45)
					|| (t == 32) || (t == 93) || (t == 47)) {
				if (t == 93) {
					ret += " ";
				} else {
					ret += source.charAt(i);
				}
			} else if ((t == 10) || (t == 13)) {
				break;
			}
		}

		ret = ret.trim();

		ret = ret.replaceAll(" ", "-");

		while (ret.indexOf("--") >= 0) {
			ret = ret.replaceAll("--", "-");
		}
		return ret;

	}

	public static String generateRandomNumber(int length) {

		Random random = new Random();

		StringBuilder stringBuilder = new StringBuilder(length);

		for (int i = 0; i < length; i++) {

			int randomNumber = random.nextInt(9);

			stringBuilder.append(randomNumber);
		}

		return stringBuilder.toString();
	}

	public static String getContentLength(String payload) {
		try {
			return String.valueOf(payload.getBytes("UTF-8").length);
		} catch (UnsupportedEncodingException e) {
			return "0";
		}
	}

	public static String escapeSpecialCharacter(String src) {
		if (src == null) {
			return null;
		}
		return src.replaceAll("([^0-9a-zA-Z,. ]+)", " ");
	}

	public static String escapeUTF8(String s) {
		String result = "";
		char c;
		int pos;
		for (int i = 0; s != null && i < s.length(); i++) {
			c = s.charAt(i);
			if ((pos = UNICODE.indexOf(c)) != -1) {
				result += NOSIGN[pos];
				// System.out.println("pos=" + pos + ", c=" + c);
			} else {
				result += c;
			}
		}
		return result;
	}

	private static final String UNICODE = "Ã¡Ã áº£Ã£áº¡Äƒáº¯áº±áº³áºµáº·Ã¢áº¥áº§áº©áº«áº­Ä‘Ã©Ã¨áº»áº½áº¹Ãªáº¿á»�á»ƒá»…á»‡Ã­Ã¬á»‰Ä©á»‹Ã³Ã²á»�Ãµá»�Ã´á»‘á»“á»•á»—á»™Æ¡á»›á»�á»Ÿá»¡á»£ÃºÃ¹á»§Å©á»¥Æ°á»©á»«á»­á»¯á»±Ã½á»³á»·á»¹á»µÃ�Ã€áº¢Ãƒáº Ä‚áº®áº°áº²áº´áº¶Ã‚áº¤áº¦áº¨áºªáº¬Ä�Ã‰Ãˆáººáº¼áº¸ÃŠáº¾á»€á»‚á»„á»†Ã�ÃŒá»ˆÄ¨á»ŠÃ“Ã’á»ŽÃ•á»ŒÃ”á»�á»’á»”á»–á»˜Æ á»šá»œá»žá» á»¢ÃšÃ™á»¦Å¨á»¤Æ¯á»¨á»ªá»¬á»®á»°Ã�á»²á»¶á»¸á»´";

	private static final String NOSIGN_CONST = "aaaaaaaaaaaaaaaaadeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyAAAAAAAAAAAAAAAAADEEEEEEEEEEEIIIIIOOOOOOOOOOOOOOOOOUUUUUUUUUUUYYYYY";

	private static char[] NOSIGN;

	static {
		NOSIGN = new char[NOSIGN_CONST.length()];
		for (int i = 0; i < NOSIGN_CONST.length(); i++) {
			NOSIGN[i] = NOSIGN_CONST.charAt(i);
		}
	}

	public static String generateOrderTrackingNumber() {
		return String.valueOf(System.currentTimeMillis());
	}

//	public static String generateDropshipApiOrderId(String userId) {
//		return new StringBuffer().append(userId).append("-").append(RandomStringUtils.randomAlphabetic(2).toUpperCase())
//				.append(RandomStringUtils.randomNumeric(2)).toString();
//	}
//
//	public static String generateDropshipApiOrderTrackingNumber() {
//		return new StringBuffer().append(RandomStringUtils.randomAlphabetic(3))
//				.append(RandomStringUtils.randomNumeric(3)).append(RandomStringUtils.randomAlphabetic(3)).toString()
//				.toUpperCase();
//	}

	private static final Logger LOGGER = Logger.getLogger(AppUtil.class.getName());
}
