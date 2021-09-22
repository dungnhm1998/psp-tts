package asia.leadsgen.psp.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.ip2location.IP2Location;
import com.ip2location.IPResult;

public class IP2LocationService {

    private static IP2LocationService instance = null;
    private static IP2Location loc = null;
    private static IP2Location locv6 = null;

    private static final String dbPath = AppUtil.class.getClassLoader()
            .getResource("IP-COUNTRY-REGION-CITY-LATITUDE-LONGITUDE-ZIPCODE.BIN").getPath();

    private static final String dbPathv6 = AppUtil.class.getClassLoader()
            .getResource("IPV6-COUNTRY-REGION-CITY-LATITUDE-LONGITUDE-ZIPCODE.BIN").getPath();

    private static final String license = AppUtil.class.getClassLoader().getResource("license.key").getPath();

    public static IP2LocationService getInstance() {
        if (instance == null) {
            instance = new IP2LocationService();
        }
        return instance;
    }

    protected IP2LocationService() {
        try {
            if (loc == null) {
                loc = new IP2Location();
            }
            if (locv6 == null) {
                locv6 = new IP2Location();
            }
            loc.IPDatabasePath = dbPath;
            loc.IPLicensePath = license;
            locv6.IPDatabasePath = dbPathv6;
            locv6.IPLicensePath = license;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    public Map getGeoInfo(String ipaddress) throws IOException {
        boolean isipv4 = false;
        //check ip v4, v6
        InetAddress address = InetAddress.getByName(ipaddress);
        if (address instanceof Inet6Address) {
            isipv4 = false;
        } else if (address instanceof Inet4Address) {
            isipv4 = true;
        }

        IPResult rec;
        if (isipv4) {
            rec = loc.IPQuery(ipaddress);
        } else {
            rec = locv6.IPQuery(ipaddress);
        }

        Map location = unknownFormat();

        if ("OK".equals(rec.getStatus())) {
            location = format(rec);
        } else if ("EMPTY_IP_ADDRESS".equals(rec.getStatus())) {
            LOGGER.severe("IP address cannot be blank.");
        } else if ("INVALID_IP_ADDRESS".equals(rec.getStatus())) {
            LOGGER.severe("Invalid IP address.");
        } else if ("MISSING_FILE".equals(rec.getStatus())) {
            LOGGER.severe("Invalid database path.");
        } else if ("IPV6_NOT_SUPPORTED".equals(rec.getStatus())) {
            LOGGER.severe("This BIN does not contain IPv6 data.");
        } else {
            LOGGER.severe("Unknown error." + rec.getStatus());
        }

        return location;
    }

    private static Map unknownFormat() {
        Map geoInfoMap = new LinkedHashMap<>();

        geoInfoMap.put(AppParams.CITY, AppParams.UNKNOWN);
        geoInfoMap.put(AppParams.POSTAL_CODE, AppParams.UNKNOWN);
        geoInfoMap.put(AppParams.COUNTRY, AppParams.UNKNOWN);
        geoInfoMap.put(AppParams.COUNTRY_NAME, AppParams.UNKNOWN);
        geoInfoMap.put(AppParams.STATE_REGION, AppParams.UNKNOWN);

        return geoInfoMap;
    }

    private static Map format(IPResult rec) {

        Map geoInfoMap = new LinkedHashMap<>();

        String city = StringUtils.isEmpty(rec.getCity()) ? AppParams.UNKNOWN : rec.getCity();
        String postalCode = StringUtils.isEmpty(rec.getZipCode()) ? AppParams.UNKNOWN : rec.getZipCode();
        String countryShort = StringUtils.isEmpty(rec.getCountryShort()) ? AppParams.UNKNOWN : rec.getCountryShort();
        String countryLong = StringUtils.isEmpty(rec.getCountryLong()) ? AppParams.UNKNOWN : rec.getCountryLong();
        String region = StringUtils.isEmpty(rec.getRegion()) ? AppParams.UNKNOWN : rec.getRegion();

        geoInfoMap.put(AppParams.CITY, city);
        geoInfoMap.put(AppParams.POSTAL_CODE, postalCode);
        geoInfoMap.put(AppParams.COUNTRY, countryShort);
        geoInfoMap.put(AppParams.COUNTRY_NAME, countryLong);
        geoInfoMap.put(AppParams.STATE_REGION, region);

        return geoInfoMap;
    }
   
    private static final Logger LOGGER = Logger.getLogger(IP2LocationService.class.getName());
}
