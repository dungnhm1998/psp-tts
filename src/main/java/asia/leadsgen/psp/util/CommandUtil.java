package asia.leadsgen.psp.util;

import java.io.IOException;

public class CommandUtil {

	public static void execute(String command) throws IOException, InterruptedException {

		Process process = new ProcessBuilder("/bin/sh", "-c", command).start();
		process.waitFor();

	}

}
