package asia.leadsgen.psp.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class StringUtil {

	public static boolean emptyAtLeastOne(List<String> args) {
		for (String arg : args) {
			if (arg == null || arg.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public static String urlEncode(String source) {
		try {
			source = source.replaceAll(" ", "%20");
			return URLEncoder.encode(source, AppConstants.CHARSET_UTF8);
		} catch (Exception e) {
			throw new BadRequestException(SystemError.INVALID_CAMPAIGN_DESCRIPTION);
		}
	}

	public static String urlDecode(String source) {
		try {
			return URLDecoder.decode(source, AppConstants.CHARSET_UTF8);
		} catch (Exception e) {
			throw new BadRequestException(SystemError.INVALID_CAMPAIGN_DESCRIPTION);
		}
	}

	public static String escapeUnicode(String source) {
		String result = "";
		char c;
		int pos;
		for (int i = 0; source != null && i < source.length(); i++) {
			c = source.charAt(i);
			if ((pos = UNICODE.indexOf(c)) != -1) {
				result += NOSIGN[pos];
				// System.out.println("pos=" + pos + ", c=" + c);
			} else {
				result += c;
			}
		}
		return result;
	}

	public static String[] split(String s, char delimiter) {

		if (s == null || s.isEmpty()) {
			return _emptyStringArray;
		}

		s = s.trim();

		if (s.length() == 0) {
			return _emptyStringArray;
		}

		if ((delimiter == CharPool.RETURN) || (delimiter == CharPool.NEW_LINE)) {

			return splitLines(s);
		}

		List<String> nodeValues = new ArrayList<>();

		int offset = 0;
		int pos = s.indexOf(delimiter, offset);

		while (pos != -1) {
			nodeValues.add(s.substring(offset, pos));

			offset = pos + 1;
			pos = s.indexOf(delimiter, offset);
		}

		if (offset < s.length()) {
			nodeValues.add(s.substring(offset));
		}

		return nodeValues.toArray(new String[nodeValues.size()]);
	}

	public static String[] split(String s, String delimiter) {
		if (s == null || s.isEmpty() || (delimiter == null) || (delimiter.equals(StringPool.BLANK))) {

			return _emptyStringArray;
		}

		s = s.trim();

		if (s.equals(delimiter)) {
			return _emptyStringArray;
		}

		if (delimiter.length() == 1) {
			return split(s, delimiter.charAt(0));
		}

		List<String> nodeValues = new ArrayList<>();

		int offset = 0;
		int pos = s.indexOf(delimiter, offset);

		while (pos != -1) {
			nodeValues.add(s.substring(offset, pos));

			offset = pos + delimiter.length();
			pos = s.indexOf(delimiter, offset);
		}

		if (offset < s.length()) {
			nodeValues.add(s.substring(offset));
		}

		return nodeValues.toArray(new String[nodeValues.size()]);
	}

	public static String[] splitLines(String s) {
		if (s == null || s.isEmpty()) {
			return _emptyStringArray;
		}

		s = s.trim();

		List<String> lines = new ArrayList<>();

		int lastIndex = 0;

		while (true) {
			int returnIndex = s.indexOf(CharPool.RETURN, lastIndex);
			int newLineIndex = s.indexOf(CharPool.NEW_LINE, lastIndex);

			if ((returnIndex == -1) && (newLineIndex == -1)) {
				break;
			}

			if (returnIndex == -1) {
				lines.add(s.substring(lastIndex, newLineIndex));

				lastIndex = newLineIndex + 1;
			} else if (newLineIndex == -1) {
				lines.add(s.substring(lastIndex, returnIndex));

				lastIndex = returnIndex + 1;
			} else if (newLineIndex < returnIndex) {
				lines.add(s.substring(lastIndex, newLineIndex));

				lastIndex = newLineIndex + 1;
			} else {
				lines.add(s.substring(lastIndex, returnIndex));

				lastIndex = returnIndex + 1;

				if (lastIndex == newLineIndex) {
					lastIndex++;
				}
			}
		}

		if (lastIndex < s.length()) {
			lines.add(s.substring(lastIndex));
		}

		return lines.toArray(new String[lines.size()]);
	}

	private static String[] _emptyStringArray = new String[0];

	private static final String UNICODE = "áàảãạăắằẳẵặâấầẩẫậđéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵÁÀẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬĐÉÈẺẼẸÊẾỀỂỄỆÍÌỈĨỊÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÚÙỦŨỤƯỨỪỬỮỰÝỲỶỸỴ";
	private static final String NOSIGN_CONST = "aaaaaaaaaaaaaaaaadeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyAAAAAAAAAAAAAAAAADEEEEEEEEEEEIIIIIOOOOOOOOOOOOOOOOOUUUUUUUUUUUYYYYY";
	private static char[] NOSIGN;

	static {
		NOSIGN = new char[NOSIGN_CONST.length()];
		for (int i = 0; i < NOSIGN_CONST.length(); i++) {
			NOSIGN[i] = NOSIGN_CONST.charAt(i);
		}
	}
}