package asia.leadsgen.psp.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 24-Nov-15.
 */
public class ContextUtil {

    public static boolean getBoolean(RoutingContext routingContext, String paramName) {
        return getBoolean(routingContext, paramName, false);
    }

    public static boolean getBoolean(RoutingContext routingContext, String paramName, boolean defaultValue) {
        return routingContext.get(paramName) != null ? GetterUtil.getBoolean(getString(routingContext, paramName), defaultValue) : defaultValue;
    }

    public static int getInt(RoutingContext routingContext, String paramName) {
        return getInt(routingContext, paramName, 0);
    }

    public static int getInt(RoutingContext routingContext, String paramName, int defaultValue) {
        return routingContext.get(paramName) != null ? GetterUtil.getInteger(routingContext.get(paramName).toString(), defaultValue) : defaultValue;
    }

    public static long getLong(RoutingContext routingContext, String paramName) {
        return getLong(routingContext, paramName, 0L);
    }

    public static long getLong(RoutingContext routingContext, String paramName, long defaultValue) {
        return routingContext.get(paramName) != null ? GetterUtil.getLong(routingContext.get(paramName).toString(), defaultValue) : defaultValue;
    }

    public static String getString(RoutingContext routingContext, String paramName) {
        return getString(routingContext, paramName, StringPool.BLANK);
    }

    public static double getDouble(RoutingContext routingContext, String paramName) {
        return getDouble(routingContext, paramName, 0);
    }

    public static double getDouble(RoutingContext routingContext, String paramName, double defaultValue) {
        paramName = paramName.trim();
        return routingContext.get(paramName) != null ? GetterUtil.getDouble(routingContext.get(paramName).toString(), defaultValue) : defaultValue;
    }

    public static String getString(RoutingContext routingContext, String paramName, String defaultValue) {

        if (routingContext == null || paramName.isEmpty()) {
            return defaultValue;
        }

        if (routingContext.get(paramName) == null || routingContext.get(paramName).toString().isEmpty()) {
            return defaultValue;
        }

        if (routingContext.get(paramName) instanceof List) {
            return ((List) routingContext.get(paramName)).get(0).toString().trim();
        }

        if (routingContext.get(paramName) instanceof java.sql.Timestamp) {

            SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

//            dateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));

            return dateFormat.format(routingContext.get(paramName));
        }

        return routingContext.get(paramName).toString().trim();
    }

    public static Map getMapData(RoutingContext routingContext, String paramName) {
        return getMapData(routingContext, paramName, new HashMap());
    }

    public static Map getMapData(RoutingContext routingContext, String paramName, Map defaultValue) {

        Object paramValue = routingContext.get(paramName);

        if (paramValue != null) {

            if (paramValue instanceof String) {

                return new JsonObject(paramValue.toString()).getMap();

            } else if (paramValue instanceof Map) {

                return (Map) paramValue;
            } else {
                return defaultValue;
            }

        } else {
            return defaultValue;
        }
    }


    public static List getListData(RoutingContext routingContext, String paramName) {
        return getListData(routingContext, paramName, new ArrayList<>());
    }

    public static List getListData(RoutingContext routingContext, String paramName, List defaultValue) {

        Object paramValue = routingContext.get(paramName);

        if (paramValue != null) {

            if (paramValue instanceof List) {

                return (List) paramValue;

            } else {
                return defaultValue;
            }

        } else {
            return defaultValue;
        }
    }
}
