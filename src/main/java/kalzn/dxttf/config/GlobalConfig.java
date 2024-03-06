package kalzn.dxttf.config;


import java.util.List;

public class GlobalConfig {

    public static class server {
        public static int port;
        public static int randomSeed;

        public static String staticPath;
        public static String staticMap;
        public static String externalStaticPath;
        public static String externalStaticMap;

        public static List<String> allowMethods;
        public static List<String> whiteIps;
        public static long heartbeatInterval;

        public static class ws {
            public static int maxKeepActive;
        }
    }

    public static class auth {
        public static boolean register;
        public static String authFile;

        public static long tokenActive;
        public static long tryInterval;

        public static int pauseFailCount;

        public static long pauseInterval;

        public static int blockFailCount;

        public static boolean strictIp;

        public static int maxNameLength;

        public static int tokenLength;

        public static String namePatten;
        public static String passwdPatten;

        public static String tokenKey;
        public static String nameKey;

        public static boolean cookieAuth;
    }



    public static class front {
        public static String loginLocation;
        public static String registerLocation;
    }

    public static class script {
        public static String scriptPath;
        public static boolean superUserEnable;
        public static String executorUser;
        public static String python;
        public static String bash;
        public static String bat;
        public static List<ScriptRegisterConfig> register;
    }


}


