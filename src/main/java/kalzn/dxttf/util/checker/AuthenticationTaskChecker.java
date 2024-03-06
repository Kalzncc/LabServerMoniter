package kalzn.dxttf.util.checker;

import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.pojo.inner.AuthenticationTask;

public class AuthenticationTaskChecker {
    public static boolean check(AuthenticationTask authenticationTask) {
        int reqType = authenticationTask.getAuthenticationType();
        if (reqType != AuthenticationTask.AUTHENTICATE_BY_PWD && reqType != AuthenticationTask.AUTHENTICATE_BY_TK) {
            return false;
        }
        if (!authenticationTask.getName().matches(GlobalConfig.auth.namePatten))
            return false;
        if (authenticationTask.getToken() == null ^ authenticationTask.getPassword() == null)
            return true;
        return false;
    }
}
