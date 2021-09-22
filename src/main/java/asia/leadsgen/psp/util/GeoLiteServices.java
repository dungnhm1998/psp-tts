/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.util;

import java.io.File;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Subdivision;

/**
 *
 * @author hungdt
 */
public class GeoLiteServices {

	private static GeoLiteServices instance = null;
	File database;
	DatabaseReader reader;

	protected GeoLiteServices() {
		// Exists only to defeat instantiation.
		try {
			database = new File(GeoLiteServices.class.getClassLoader().getResource("geo_data.mmdb").getPath());
			LOGGER.info("path = " + database.getPath());
			reader = new DatabaseReader.Builder(database).build();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "", e);
		}
	}

	public static GeoLiteServices getInstance() {
		if (instance == null) {
			instance = new GeoLiteServices();
		}
		return instance;
	}

	public Map getGeoInfo(String ipAddress) {

		Map geoInfoMap = new LinkedHashMap();

		try {
			if (database != null) {

				InetAddress inetAddress = InetAddress.getByName(ipAddress);

				if (inetAddress != null && reader != null) {

					CityResponse cityResponse = reader.city(inetAddress);
					if (cityResponse != null) {
						String city = StringUtils.isEmpty(cityResponse.getCity().getName()) ? AppParams.UNKNOWN
								: cityResponse.getCity().getName();
						String postalCode = StringUtils.isEmpty(cityResponse.getPostal().getCode()) ? AppParams.UNKNOWN
								: cityResponse.getPostal().getCode();
						String countryCode = StringUtils.isEmpty(cityResponse.getCountry().getIsoCode())
								? AppParams.UNKNOWN
								: cityResponse.getCountry().getIsoCode();
						String countryName = StringUtils.isEmpty(cityResponse.getCountry().getName())
								? AppParams.UNKNOWN
								: cityResponse.getCountry().getName();
						List<Subdivision> subdivisions = cityResponse.getSubdivisions();
						String stateRegion = AppParams.UNKNOWN;
						if (!CollectionUtils.isEmpty(subdivisions)) {
							stateRegion = StringUtils.isEmpty(subdivisions.get(0).getName()) ? AppParams.UNKNOWN
									: subdivisions.get(0).getName();
						}
						geoInfoMap.put(AppParams.CITY, city);
						geoInfoMap.put(AppParams.POSTAL_CODE, postalCode);
						geoInfoMap.put(AppParams.COUNTRY, countryCode);
						geoInfoMap.put(AppParams.COUNTRY_NAME, countryName);
						geoInfoMap.put(AppParams.STATE_REGION, stateRegion);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warning("Exception while getting geo info from ip: " + ipAddress);
		}

		return geoInfoMap;
	}

	// public GeoItem getGeoCity(String ipaddr) {
	// InetAddress ipAddress;
	// GeoItem geoItem = null;
	// try {
	// ipAddress = InetAddress.getByName(ipaddr);
	// CityResponse response = reader.city(ipAddress);
	// Location location = response.getLocation();
	// geoItem = new GeoItem();
	// geoItem.setLatitude(location.getLatitude());
	// geoItem.setLongitude(location.getLongitude());
	// } catch (IOException | GeoIp2Exception e) {
	// logger.error("", e);
	// }
	// return geoItem;
	// }
	private static final Logger LOGGER = Logger.getLogger(GeoLiteServices.class.getName());
}
