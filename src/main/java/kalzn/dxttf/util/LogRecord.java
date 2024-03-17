package kalzn.dxttf.util;

public class LogRecord {





    public static final String INFO_COMPONENT_REGISTER = "Component %s registered.";
    public static final String INFO_LOGIN_TRY = "Login try username: %s, auth type: %s, from ip: %s, result: %s.";
    public static final String INFO_LOAD_CONFIG = "Load External Configuration File from %s";

    public static String INFO_SUPER_PRIVILEGE_EXECUTE_ATTEMPT(String router, String ip, String content) {
      return String.format("Attempt execute privilege script though router: %s, front ip: %s content", router, ip, content);
    }
    public static String INFO_SUPER_PRIVILEGE_EXECUTE_RESULT(String cmd, String result) {
        return String.format("Privilege script  cmd: %s,  Result %s",  cmd, result);
    }


    public static final String WARN_UNKNOWN_OS = "Unknown os, regard as super user.";


    public static final String WARN_COMPONENT_DUPLICATE = "Component name %s duplicate, registration failed.";
    public static final String WARN_COMPONENT_CONSTRUCT = "Unable to construct %s component instance, registration failed.";
    public static final String WARN_MALFORMATION_LOGIN_REQ = "Malformation login req username: %s, from ip: %s.";
    public static final String WARN_FORCE_LOGIN_BANNED = "Found force authentication try from ip: %s, username: %s, user banned.";
    public static final String WARN_SCRIPT_EXECUTE_FAIL = "Script <%s> execute fail.";



    public static final String ERROR_CONFIG_LOAD = "External configuration file load error (%s).";
    public static final String ERROR_MOUNT_ROUTE_FAIL = "Fail to mount router : %s";
    public static final String ERROR_CANT_LOAD_SYSTEM_STATUS = "Load system status fail.";
    public static final String ERROR_PRIVILEGE_SETTING_INVALID = "Privilege setting invalid : Super user enable is <%s>, but process is running in <%s>";



    public static String WARN_PROCESS_MONITOR_ERROR(String errorMsg) {
        return String.format("Process monitor setup error: %s", errorMsg);
    }
    public static String WARN_PROCESS_ENTER_ERROR(String errorMsg) {
        return String.format("Process stdin enter error: %s", errorMsg);
    }
}

