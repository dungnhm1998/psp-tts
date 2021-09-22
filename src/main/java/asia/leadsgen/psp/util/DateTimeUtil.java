package asia.leadsgen.psp.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateTimeUtil {

    private static final String DEFAULT_TIMEZONE_PREFIX = "UTC";

    public static String convertDateToDefaultTimezone(String dateInString, String clientTimezone)
            throws ParseException {

        SimpleDateFormat sdf_yyyyMMdd = new SimpleDateFormat(AppConstants.YYYYMMDD);
        TimeZone tzClient = TimeZone.getTimeZone(clientTimezone);
        sdf_yyyyMMdd.setTimeZone(tzClient);
        Date dateInClient = sdf_yyyyMMdd.parse(dateInString);

        SimpleDateFormat sdf_yyyyMMddTHHmmssZ = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);
        TimeZone tzServer = TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE);
        sdf_yyyyMMddTHHmmssZ.setTimeZone(tzServer);
        String formattedAmericaDate = sdf_yyyyMMddTHHmmssZ.format(dateInClient);
        return formattedAmericaDate;

    }

    public static Date addDays(Date date, int days) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);

        return cal.getTime();
    }

    public static String formatTimezone(String timezone) {

        timezone = timezone.trim().replace(' ', '+');

        if (timezone.endsWith(".25")) {
            timezone = timezone.replace(".25", ".5");
        }

        if (timezone.endsWith(".5")) {
            if (timezone.contains("+")) {
                if (timezone.substring((timezone.lastIndexOf("+") + 1), timezone.lastIndexOf(".")).length() < 2) {
                    timezone = (timezone.replace("+", "+0")).replace(".5", ":30");
                } else {
                    timezone = timezone.replace(".5", ":30");
                }

            } else if (timezone.contains("-")) {
                if (timezone.substring((timezone.lastIndexOf("-") + 1), timezone.lastIndexOf(".")).length() < 2) {
                    timezone = (timezone.replace("-", "-0")).replace(".5", ":30");
                } else {
                    timezone = timezone.replace(".5", ":30");
                }
            }

        } else if (timezone.endsWith(".75")) {
            if (timezone.contains("+")) {
                if (timezone.substring((timezone.lastIndexOf("+") + 1), timezone.lastIndexOf(".")).length() < 2) {
                    timezone = (timezone.replace("+", "+0")).replace(".75", ":45");
                } else {
                    timezone = timezone.replace(".75", ":45");
                }

            } else if (timezone.contains("-")) {
                if (timezone.substring((timezone.lastIndexOf("-") + 1), timezone.lastIndexOf(".")).length() < 2) {
                    timezone = (timezone.replace("-", "-0")).replace(".75", ":45");
                } else {
                    timezone = timezone.replace(".75", ":45");
                }
            }
        } else {
            if (timezone.contains("+")) {
                if (timezone.substring((timezone.lastIndexOf("+") + 1)).length() < 2) {
                    timezone = (timezone.replace("+", "+0")).concat(":00");
                } else {
                    timezone = timezone.concat(":00");
                }

            } else if (timezone.contains("-")) {
                if (timezone.substring((timezone.lastIndexOf("-") + 1)).length() < 2) {
                    timezone = (timezone.replace("-", "-0")).concat(":00");
                } else {
                    timezone = timezone.concat(":00");
                }
            }
        }

        return timezone.replace("UTC", "GMT");
    }

    public static String convertTimezoneAtoB(String timeAsLong, String tzA, String tzB) {
        Calendar clientCal = new GregorianCalendar(TimeZone.getTimeZone(tzA));
        clientCal.setTimeInMillis(Long.valueOf(timeAsLong));
        clientCal.setTimeZone(TimeZone.getTimeZone(tzB));
        return "" + clientCal.getTimeInMillis();
    }

    public static String longToDate(long data, String format) {
        Date date = new Date(data);
        SimpleDateFormat df2 = new SimpleDateFormat(format);
        return df2.format(date);
    }
}
