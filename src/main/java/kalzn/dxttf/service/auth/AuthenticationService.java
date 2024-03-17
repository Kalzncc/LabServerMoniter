package kalzn.dxttf.service.auth;

import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.data.DataManager;
import kalzn.dxttf.data.authfile.AuthenticationDatabase;
import kalzn.dxttf.pojo.inner.AuthenticationInfo;
import kalzn.dxttf.pojo.inner.AuthenticationTask;
import kalzn.dxttf.pojo.inner.AuthenticationToken;
import kalzn.dxttf.pojo.outer.AuthenticationResult;
import kalzn.dxttf.util.TokenUtil;
import kalzn.dxttf.util.LogRecord;
import kalzn.dxttf.util.checker.AuthenticationTaskChecker;
import kalzn.dxttf.util.factory.AuthenticationResultFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(name={"auth_service"}, type = Component.SERVICE)
public class AuthenticationService {

    private final AuthenticationDatabase authDatabase;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService() {
        authDatabase = DataManager.getDatabase("auth_database", AuthenticationDatabase.class);
    }

    private int commonAuthenticate(AuthenticationTask loginReq, AuthenticationInfo info) {

        // User Banned.
        if (info.getBan() == 1) {
            return AuthenticationResult.FAIL_USER_BANNED;
        }

        // Try times too many in short time.
        if (loginReq.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_TK
                && info.getTryCount() > GlobalConfig.auth.pauseFailCount
                && System.currentTimeMillis() - info.getLastTryAuthenticationTime() < GlobalConfig.auth.pauseInterval) {
            return AuthenticationResult.FAIL_TOO_MANY_TRY;
        }

        // Try interval too short.
        if (loginReq.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_PWD
                && System.currentTimeMillis() - info.getLastAuthenticationTime() < GlobalConfig.auth.tryInterval) {
            return AuthenticationResult.FAIL_INTERVAL_TOO_SHORT;
        }


        return AuthenticationResult.PASS_COMMON_AUTH_CONTINUE;
    }
    private AuthenticationResult pwdAuthenticate(AuthenticationTask authenticationTask, AuthenticationInfo info) {
        AuthenticationResult result = null;
        if (info.getPassword().equals(authenticationTask.getPassword())) {
            result = AuthenticationResultFactory.createSuccess(authenticationTask);
        } else {
            result = AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_WRONG_PASSWORD_OR_TOKEN);
        }
       return result;
    }
    private AuthenticationResult tokenAuthenticate(AuthenticationTask authenticationTask, AuthenticationInfo info) {
        String reqToken = authenticationTask.getToken();
        String reqIp = authenticationTask.getIp();

        // No token.
        if (info.getTokens() == null || info.getTokens().isEmpty()) {
            return AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_WRONG_PASSWORD_OR_TOKEN);
        }

        // No matched token.
        if (!info.getTokens().containsKey(reqToken)) {
            return AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_WRONG_PASSWORD_OR_TOKEN);
        }

        // Token expired.
        if (System.currentTimeMillis() - info.getTokens().get(reqToken).getTimestamp() > GlobalConfig.auth.tokenActive) {
            return AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_TOKEN_EXPIRE);
        }

        // Token Ip mismatch.
        if (GlobalConfig.auth.strictIp && !info.getTokens().get(reqToken).getIp().equals(reqIp)) {
            return AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_INVALID_IP);
        }

        return AuthenticationResultFactory.createSuccess(authenticationTask);
    }
    private boolean record(AuthenticationTask authenticationTask, AuthenticationResult result, AuthenticationInfo info) {
        logger.info(String.format(LogRecord.INFO_LOGIN_TRY,
                authenticationTask.getName(),
                authenticationTask.getAuthenticationType(),
                authenticationTask.getIp(),
                result.getResult()));

        switch (result.getResult()) {
            case AuthenticationResult.SUCCESS -> {
                long currentTimestamp = System.currentTimeMillis();


                if (authenticationTask.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_PWD) {
                    String tokenStr = TokenUtil.generateRandomToken(GlobalConfig.auth.tokenLength);
                    AuthenticationToken token = new AuthenticationToken(tokenStr, authenticationTask.getIp(), currentTimestamp);


                    result.setToken(token.getToken());
                    info.setLastAuthenticationTime(currentTimestamp);
                    info.setLastTryAuthenticationTime(currentTimestamp);
                    info.setTryCount(0);
                    authDatabase.updateAuthInfo(info);
                    return authDatabase.addToken(info.getName(), token);


                } else if (authenticationTask.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_TK) {
                    info.setLastAuthenticationTime(currentTimestamp);
                    info.setLastTryAuthenticationTime(currentTimestamp);
                    info.setTryCount(0);
                }
                return authDatabase.updateAuthInfo(info);
            }
            case AuthenticationResult.FAIL_USER_NOT_EXIST, AuthenticationResult.FAIL_USER_BANNED -> {
                return true;
            }
            default -> {
                info.setTryCount(info.getTryCount() + 1);
                boolean newBanFlag = false;
                if (GlobalConfig.auth.blockFailCount != -1 && info.getTryCount() > GlobalConfig.auth.blockFailCount) {
                    if (!authDatabase.banUser(info.getName())) {
                        return false;
                    }
                    info.setBan(1);
                    newBanFlag = true;
                }
                info.setLastTryAuthenticationTime(System.currentTimeMillis());
                boolean updateFlag = authDatabase.updateAuthInfo(info);
                if (newBanFlag && updateFlag) {
                    logger.warn(String.format(LogRecord.WARN_FORCE_LOGIN_BANNED,
                            authenticationTask.getIp(),
                            authenticationTask.getName()
                            ));
                }
                return updateFlag;
            }
        }
    }
    public AuthenticationResult reportAuth(AuthenticationTask authenticationTask, AuthenticationResult result, AuthenticationInfo info) {
        if (record(authenticationTask, result, info))
            return result;
        else {
            logger.warn(String.format(LogRecord.WARN_MALFORMATION_LOGIN_REQ,
                    authenticationTask.getName(),
                    authenticationTask.getIp()));
            return AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_AUTH_DATABASE_REJECT);
        }
    }
    public AuthenticationResult authenticate(AuthenticationTask authenticationTask) {

        if (!AuthenticationTaskChecker.check(authenticationTask)) {
            logger.warn(String.format(LogRecord.WARN_MALFORMATION_LOGIN_REQ,
                    authenticationTask.getName(),
                    authenticationTask.getIp()));
            return AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_WRONG_PASSWORD_OR_TOKEN);
        }


        AuthenticationInfo info = authDatabase.getAuthInfo(authenticationTask.getName());
        if (info == null) {
            AuthenticationResult result = AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_USER_NOT_EXIST);
            return reportAuth(authenticationTask, result, null);
        }


        int resultCode = commonAuthenticate(authenticationTask, info);
        if (resultCode != AuthenticationResult.PASS_COMMON_AUTH_CONTINUE) {
            AuthenticationResult result = AuthenticationResultFactory.createFail(resultCode);
            return reportAuth(authenticationTask, result, info);

        }


        if (authenticationTask.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_PWD) {
            var result = pwdAuthenticate(authenticationTask, info);
            return reportAuth(authenticationTask, result, info);
        }


        if (authenticationTask.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_TK) {
            var result = tokenAuthenticate(authenticationTask, info);
            return reportAuth(authenticationTask, result, info);
        }

        if (authenticationTask.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_TK_AND_PWD) {
            var pwdResult = pwdAuthenticate(authenticationTask, info);
            var tokenResult = tokenAuthenticate(authenticationTask, info);
            if (pwdResult.getResult() != AuthenticationResult.SUCCESS) {
                return reportAuth(authenticationTask, pwdResult, info);
            }
            return reportAuth(authenticationTask, tokenResult, info);
        }

        return reportAuth(
                authenticationTask,
                AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_WRONG_PASSWORD_OR_TOKEN),
                info
            );
    }

    public boolean unAuthenticate(String name, String token) {
        return authDatabase.removeToken(name, token);
    }

}
