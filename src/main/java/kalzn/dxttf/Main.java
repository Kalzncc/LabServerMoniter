package kalzn.dxttf;


import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.GlobalConfigLoader;
import kalzn.dxttf.config.GlobalSystemStatus;
import kalzn.dxttf.data.DataManager;
import kalzn.dxttf.executor.ExecutorManager;
import kalzn.dxttf.router.RouterLoader;
import kalzn.dxttf.service.ServiceManager;

import kalzn.dxttf.util.LogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static String externalConfigFile = null;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length > 0) {
            externalConfigFile = args[0];
            logger.info(String.format(LogRecord.INFO_LOAD_CONFIG, externalConfigFile));
        }
        try {
            GlobalConfigLoader.load(externalConfigFile);
        } catch (Exception e) {
            logger.error(String.format(LogRecord.ERROR_CONFIG_LOAD, e.getMessage()));
            System.exit(-1);
        }

        try {
            Class.forName("kalzn.dxttf.config.GlobalSystemStatus");
            // Static acquisition of the current state of the system.

//            Class.forName("kalzn.dxttf.router.support.WebsocketSessionStorage");
//            // Load websocket session status storage proxy.

        } catch (ClassNotFoundException e) {
            logger.error(String.format(LogRecord.ERROR_CANT_LOAD_SYSTEM_STATUS));
            System.exit(-1);
        }


        if (GlobalSystemStatus.superUserProcess != GlobalConfig.script.superUserEnable) {
            logger.error(String.format(LogRecord.ERROR_PRIVILEGE_SETTING_INVALID,
                    GlobalConfig.script.superUserEnable?"true":"false",
                    GlobalSystemStatus.superUserProcess?"super user":"non super user"
                    ));
            System.exit(-1);
        }



        DataManager.registerDatabases();
        ExecutorManager.registerExecutors();
        ServiceManager.registerServices();

        try {
            RouterLoader.loadRouter();
        } catch (Exception e) {
            logger.error(String.format(LogRecord.ERROR_MOUNT_ROUTE_FAIL, e.getMessage()));
            System.exit(-1);
        }

        RouterLoader.start(true);



    }
}
