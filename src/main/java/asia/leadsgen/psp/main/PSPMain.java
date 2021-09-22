package asia.leadsgen.psp.main;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

			try {
				int port = Integer.valueOf(args[0]);
				PSPServer job = (PSPServer) applicationContext.getBean("pspServer");
				job.setServerPort(port);
				job.init();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		}

	}

	private static final Logger LOGGER = Logger.getLogger(PSPMain.class.getName());

}
