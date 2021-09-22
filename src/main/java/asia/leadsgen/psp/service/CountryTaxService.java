package asia.leadsgen.psp.service;

import java.sql.SQLException;
import java.util.Map;

import asia.leadsgen.psp.interfaces.LoggerInterface;

public class CountryTaxService extends MasterService implements LoggerInterface {
	
	public static final String GET_TAX_BY_COUNTRY = "{call PKG_FF_COUNTRY_TAX.get_tax_by_country(?,?,?,?)}";
	
	public static Map getTaxByCountry(String countryCode) throws SQLException {
		Object[] args = new Object[] {countryCode};
		Map result = searchOne(GET_TAX_BY_COUNTRY, args);
		return result;
	}
	
}
