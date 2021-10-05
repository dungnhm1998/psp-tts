package asia.leadsgen.psp.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.service.CountryTaxService;
import asia.leadsgen.psp.service_fulfill.UpdateTrackingService;
import asia.leadsgen.psp.util.AppUtil;
import asia.leadsgen.psp.util.OrderUtil;
import io.vertx.core.json.JsonObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import asia.leadsgen.psp.server.PSPServer;

/**
 * Created by hungdx on 4/1/17.
 */
public class PSPMain {

	public static void main(String[] args) throws ParseException {
		try {
			ApplicationContext applicationContext = new ClassPathXmlApplicationContext("app-context.xml");

//			try {
//				int port = Integer.valueOf(args[0]);
//				PSPServer job = (PSPServer) applicationContext.getBean("pspServer");
//				job.setServerPort(port);
//				job.init();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			getTotalGoals("", 2011);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		}

	}

	public static int getTotalGoals(String team, int year) throws SQLException {
		String countryCode = "CZ";
		Map countryTax =  CountryTaxService.getTaxByCountry(countryCode);

		Double taxRate = OrderUtil.getTaxRateFromCountryTax(countryTax);

		System.out.println(taxRate);

		return 1;

	}

	public static String getMd5(String input)
	{
		try {

			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger no = new BigInteger(1, messageDigest);
			String hashtext = no.toString(16);
			while (hashtext.length() < 32) {
				hashtext = "0" + hashtext;
			}
			return hashtext;
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static final Logger LOGGER = Logger.getLogger(PSPMain.class.getName());

}
