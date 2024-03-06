package kalzn.dxttf.config;


import kalzn.dxttf.util.LogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class GlobalSystemStatus {

    public static String os;
    private static final Logger logger = LoggerFactory.getLogger(GlobalSystemStatus.class);
    static {
        os = System.getProperty("os.name");
    }


    public static boolean superUserProcess;
    static {
        try {
            if (os.contains("Linux")) {
                Process process = Runtime.getRuntime().exec("id -u");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String output = reader.readLine();
                int uid = Integer.parseInt(output);
                superUserProcess = uid == 0;
            } else if (os.contains("Windows")) {
                Process process = Runtime.getRuntime().exec("whoami /priv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = "";
                superUserProcess = false;
                while((line = reader.readLine()) != null) {
                    if (line.contains("SeIncreaseQuotaPrivilege")) {
                        superUserProcess = true;
                        break;
                    }
                }
            } else {
                superUserProcess = true;
                logger.warn(LogRecord.WARN_UNKNOWN_OS);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
