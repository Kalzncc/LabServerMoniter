package kalzn.dxttf.util.checker;

import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.pojo.inner.AuthenticationTask;

public class AuthenticationTaskChecker {
    public static boolean check(AuthenticationTask authenticationTask) {
        int reqType = authenticationTask.getAuthenticationType();
        if (reqType != AuthenticationTask.AUTHENTICATE_BY_PWD && reqType != AuthenticationTask.AUTHENTICATE_BY_TK && reqType != AuthenticationTask.AUTHENTICATE_BY_TK_AND_PWD) {
            return false;
        }
        if (!authenticationTask.getName().matches(GlobalConfig.auth.namePatten))
            return false;

        return true;
    }
}
