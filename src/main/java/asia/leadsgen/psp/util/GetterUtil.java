package asia.leadsgen.psp.util;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by HungDX on 12/25/2014
 */
public class GetterUtil {

	public static String[] BOOLEANS = { "true", "t", "y", "on", "1" };

	public static final boolean DEFAULT_BOOLEAN = false;

	public static final double DEFAULT_DOUBLE = 0.0;

	public static final float DEFAULT_FLOAT = 0;

	public static final int DEFAULT_INTEGER = 0;

	public static final long DEFAULT_LONG = 0;

	public static final String DEFAULT_STRING = StringPool.BLANK;

	public static double formatDouble(double value, int decimals) {
		if (decimals < 0)
			throw new IllegalArgumentException();
		long factor = (long) Math.pow(10, decimals);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

	public static boolean get(Object value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String) {
			return get((String) value, defaultValue);
		}

		Class<?> clazz = value.getClass();

		if (clazz.isAssignableFrom(Boolean.class)) {
			return (Boolean) value;
		}

		return defaultValue;
	}

	public static Date get(Object value, DateFormat dateFormat, Date defaultValue) {

		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String) {
			return get((String) value, dateFormat, defaultValue);
		}

		Class<?> clazz = value.getClass();

		if (clazz.isAssignableFrom(Date.class)) {
			return (Date) value;
		}

		return defaultValue;
	}

	public static double get(Object value, double defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String) {
			return get((String) value, defaultValue);
		}

		Class<?> clazz = value.getClass();

		if (clazz.isAssignableFrom(Double.class)) {
			return (Double) value;
		}

		if (value instanceof Number) {
			Number number = (Number) value;

			return number.doubleValue();
		}

		return defaultValue;
	}

	public static float get(Object value, float defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String) {
			return get((String) value, defaultValue);
		}

		Class<?> clazz = value.getClass();

		if (clazz.isAssignableFrom(Float.class)) {
			return (Float) value;
		}

		if (value instanceof Number) {
			Number number = (Number) value;

			return number.floatValue();
		}

		return defaultValue;
	}

	public static int get(Object value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String) {
			return get((String) value, defaultValue);
		}

		Class<?> clazz = value.getClass();

		if (clazz.isAssignableFrom(Integer.class)) {
			return (Integer) value;
		}

		if (value instanceof Number) {
			Number number = (Number) value;

			return number.intValue();
		}

		return defaultValue;
	}

	public static long get(Object value, long defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String) {
			return get((String) value, defaultValue);
		}

		Class<?> clazz = value.getClass();

		if (clazz.isAssignableFrom(Long.class)) {
			return (Long) value;
		}

		if (value instanceof Number) {
			Number number = (Number) value;

			return number.longValue();
		}

		return defaultValue;
	}

	public static Number get(Object value, Number defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String) {
			if (value == null || ((String) value).isEmpty()) {
				return defaultValue;
			}

			if (getFloat(value) == getInteger(value)) {
				return getInteger(value);
			} else {
				return getFloat(value);
			}
		}

		Class<?> clazz = value.getClass();

		if (clazz.isAssignableFrom(Byte.class)) {
			return (Byte) value;
		} else if (clazz.isAssignableFrom(Double.class)) {
			return (Double) value;
		} else if (clazz.isAssignableFrom(Float.class)) {
			return (Float) value;
		} else if (clazz.isAssignableFrom(Integer.class)) {
			return (Integer) value;
		} else if (clazz.isAssignableFrom(Long.class)) {
			return (Long) value;
		} else if (clazz.isAssignableFrom(Short.class)) {
			return (Short) value;
		}

		if (value instanceof Number) {
			return (Number) value;
		}

		return defaultValue;
	}

	public static String get(Object value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof String) {
			return get((String) value, defaultValue);
		}

		return defaultValue;
	}

	public static boolean get(String value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		try {
			value = value.trim().toLowerCase();

			return value.equals(BOOLEANS[0]) || value.equals(BOOLEANS[1]) || value.equals(BOOLEANS[2])
					|| value.equals(BOOLEANS[3]) || value.equals(BOOLEANS[4]);
		} catch (Exception e) {
			_logger.log(Level.SEVERE, "", e);
		}

		return defaultValue;
	}

	public static Date get(String value, DateFormat dateFormat, Date defaultValue) {

		if (value == null) {
			return defaultValue;
		}

		try {
			Date date = dateFormat.parse(value.trim());

			if (date != null) {
				return date;
			}
		} catch (Exception e) {
			_logger.log(Level.SEVERE, "", e);
		}

		return defaultValue;
	}

	public static double get(String value, double defaultValue) {
		if (value != null) {
			try {
				return Double.parseDouble(_trim(value));
			} catch (Exception e) {
				_logger.log(Level.SEVERE, "", e);
			}
		}

		return defaultValue;
	}

	public static float get(String value, float defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		try {
			return Float.parseFloat(_trim(value));
		} catch (Exception e) {
			_logger.log(Level.SEVERE, "", e);
		}

		return defaultValue;
	}

	public static int get(String value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		return _parseInt(_trim(value), defaultValue);
	}

	public static long get(String value, long defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		return _parseLong(_trim(value), defaultValue);
	}

	public static String get(String value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		value = value.trim();

		if (value.indexOf(CharPool.RETURN) != -1) {
			value = value.replaceAll(StringPool.RETURN_NEW_LINE, StringPool.NEW_LINE);
		}

		return value;
	}

	public static boolean getBoolean(String value) {
		return getBoolean(value, DEFAULT_BOOLEAN);
	}

	public static boolean getBoolean(String value, boolean defaultValue) {
		return get(value, defaultValue);
	}

	public static Date getDate(String value, DateFormat dateFormat) {
		return getDate(value, dateFormat, new Date());
	}

	public static Date getDate(String value, DateFormat dateFormat, Date defaultValue) {

		return get(value, dateFormat, defaultValue);
	}

	public static double getDouble(String value) {
		return getDouble(value, DEFAULT_DOUBLE);
	}

	public static double getDouble(String value, double defaultValue) {
		return get(value, defaultValue);
	}

	public static double format(double value, int decimalPlaces) {
		if (decimalPlaces < 0)
			throw new IllegalArgumentException();
		long factor = (long) Math.pow(10, decimalPlaces);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;

	}

	public static float getFloat(Object value) {
		return getFloat(value, DEFAULT_FLOAT);
	}

	public static float getFloat(Object value, float defaultValue) {
		return get(value, defaultValue);
	}

	public static float getFloat(String value) {
		return getFloat(value, DEFAULT_FLOAT);
	}

	public static float getFloat(String value, float defaultValue) {
		return get(value, defaultValue);
	}

	public static int getInteger(Object value) {
		return getInteger(value, DEFAULT_INTEGER);
	}

	public static int getInteger(Object value, int defaultValue) {
		return get(value, defaultValue);
	}

	public static int getInteger(String value) {
		return getInteger(value, DEFAULT_INTEGER);
	}

	public static int getInteger(String value, int defaultValue) {
		return get(value, defaultValue);
	}

	public static long getLong(String value) {
		return getLong(value, DEFAULT_LONG);
	}

	public static long getLong(String value, long defaultValue) {
		return get(value, defaultValue);
	}

	public static String getString(String value) {
		return getString(value, DEFAULT_STRING);
	}

	public static String getString(String value, String defaultValue) {
		return get(value, defaultValue);
	}

	private static int _parseInt(String value, int defaultValue) {
		int length = value.length();

		if (length <= 0) {
			return defaultValue;
		}

		int pos = 0;
		int limit = -Integer.MAX_VALUE;
		boolean negative = false;

		char c = value.charAt(0);

		if (c < CharPool.NUMBER_0) {
			if (c == CharPool.MINUS) {
				limit = Integer.MIN_VALUE;
				negative = true;
			} else if (c != CharPool.PLUS) {
				return defaultValue;
			}

			if (length == 1) {
				return defaultValue;
			}

			pos++;
		}

		int smallLimit = limit / 10;

		int result = 0;

		while (pos < length) {
			if (result < smallLimit) {
				return defaultValue;
			}

			c = value.charAt(pos++);

			if ((c < CharPool.NUMBER_0) || (c > CharPool.NUMBER_9)) {
				return defaultValue;
			}

			int number = c - CharPool.NUMBER_0;

			result *= 10;

			if (result < (limit + number)) {
				return defaultValue;
			}

			result -= number;
		}

		if (negative) {
			return result;
		} else {
			return -result;
		}
	}

	private static long _parseLong(String value, long defaultValue) {

		int length = value.length();

		if (length <= 0) {
			return defaultValue;
		}

		int pos = 0;
		long limit = -Long.MAX_VALUE;
		boolean negative = false;

		char c = value.charAt(0);

		if (c < CharPool.NUMBER_0) {
			if (c == CharPool.MINUS) {
				limit = Long.MIN_VALUE;
				negative = true;
			} else if (c != CharPool.PLUS) {
				return defaultValue;
			}

			if (length == 1) {
				return defaultValue;
			}

			pos++;
		}

		long smallLimit = limit / 10;

		long result = 0;

		while (pos < length) {
			if (result < smallLimit) {
				return defaultValue;
			}

			c = value.charAt(pos++);

			if ((c < CharPool.NUMBER_0) || (c > CharPool.NUMBER_9)) {
				return defaultValue;
			}

			int number = c - CharPool.NUMBER_0;

			result *= 10;

			if (result < (limit + number)) {
				return defaultValue;
			}

			result -= number;
		}

		if (negative) {
			return result;
		} else {
			return -result;
		}
	}

	private static String _trim(String value) {
		value = value.trim();

		int length = value.length();

		StringBuilder sb = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			char c = value.charAt(i);

			if (Character.isDigit(c)
					|| ((c == CharPool.DASH) && ((i == 0) || (value.charAt(i - 1) == CharPool.UPPER_CASE_E)
							|| (value.charAt(i - 1) == CharPool.LOWER_CASE_E)))
					|| (c == CharPool.PERIOD) || (c == CharPool.UPPER_CASE_E) || (c == CharPool.LOWER_CASE_E)) {

				sb.append(c);
			}
		}

		return sb.toString();
	}

	private static final Logger _logger = Logger.getLogger(GetterUtil.class.getName());
}