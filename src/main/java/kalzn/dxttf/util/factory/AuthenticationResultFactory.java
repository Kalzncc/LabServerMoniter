package kalzn.dxttf.util.factory;

import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.pojo.inner.AuthenticationTask;
import kalzn.dxttf.pojo.outer.AuthenticationResult;


public class AuthenticationResultFactory {
    public static AuthenticationResult createSuccess(AuthenticationTask req) {
        AuthenticationResult result = new AuthenticationResult();
        result.setResult(AuthenticationResult.SUCCESS);
        result.setIp(req.getIp());
        result.setName(req.getName());
        result.setToken(req.getToken());
        result.setKeepActive(GlobalConfig.auth.tokenActive != 0);
        result.setLoginTimestamp(System.currentTimeMillis());
        return result;
    }

    public static AuthenticationResult createFail(int FailType) {
        AuthenticationResult result = new AuthenticationResult();
        result.setResult(FailType);
        return result;
    }
}
