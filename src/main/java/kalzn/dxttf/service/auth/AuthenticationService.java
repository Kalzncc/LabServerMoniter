package kalzn.dxttf.service.auth;

import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.data.DataManager;
import kalzn.dxttf.data.authfile.AuthenticationDatabase;
import kalzn.dxttf.pojo.inner.AuthenticationInfo;
import kalzn.dxttf.pojo.inner.AuthenticationTask;
import kalzn.dxttf.pojo.outer.AuthenticationResult;
import kalzn.dxttf.util.HashUtil;
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
        if (loginReq.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_PWD
                && info.getTryCount() > GlobalConfig.auth.pauseFailCount
                && System.currentTimeMillis() - info.getLastTryAuthenticationTime() < GlobalConfig.auth.pauseInterval) {
            return AuthenticationResult.FAIL_TOO_MANY_TRY;
        }

        // Try interval too short.
        if (loginReq.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_PWD
                && System.currentTimeMillis() - info.getLastAuthenticationTime() < GlobalConfig.auth.tryInterval) {
            return AuthenticationResult.FAIL_INTERVAL_TOO_SHORT;
        }


        // Token authenticate from unknown ip
        if (loginReq.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_TK
                && (!loginReq.getIp().equals(info.getIp()) && GlobalConfig.auth.strictIp)) {
            return AuthenticationResult.FAIL_INVALID_IP;
        }

        // Token expired.
        if (loginReq.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_TK
                && System.currentTimeMillis() - info.getTokenTimestamp() > GlobalConfig.auth.tokenActive) {
            return AuthenticationResult.FAIL_TOKEN_EXPIRE;
        }

        return AuthenticationResult.CONTINUE_PASS_COMMON_AUTH;
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
                info.setIp(authenticationTask.getIp());
                if (authenticationTask.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_PWD) {
                    info.setToken(HashUtil.generateRandomToken(GlobalConfig.auth.tokenLength));
                    result.setToken(info.getToken());
                    info.setLastAuthenticationTime(currentTimestamp);
                    info.setLastTryAuthenticationTime(currentTimestamp);
                    info.setTryCount(0);
                    info.setTokenTimestamp(currentTimestamp);
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
                if (info.getTryCount() > GlobalConfig.auth.blockFailCount) {
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
        if (resultCode != AuthenticationResult.CONTINUE_PASS_COMMON_AUTH) {
            AuthenticationResult result = AuthenticationResultFactory.createFail(resultCode);
            return reportAuth(authenticationTask, result, info);

        }


        if (authenticationTask.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_PWD) {
            AuthenticationResult result = null;
            if (info.getPassword().equals(authenticationTask.getPassword())) {
                result = AuthenticationResultFactory.createSuccess(authenticationTask);
            } else {
                result = AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_WRONG_PASSWORD_OR_TOKEN);
            }
            return reportAuth(authenticationTask, result, info);
        }


        if (authenticationTask.getAuthenticationType() == AuthenticationTask.AUTHENTICATE_BY_TK) {
            AuthenticationResult result = null;
            if (info.getToken().equals(authenticationTask.getToken())) {
                result = AuthenticationResultFactory.createSuccess(authenticationTask);
            } else {
                result = AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_WRONG_PASSWORD_OR_TOKEN);
            }
            return reportAuth(authenticationTask, result, info);
        }

        return reportAuth(
                authenticationTask,
                AuthenticationResultFactory.createFail(AuthenticationResult.FAIL_SYSTEM_ERROR),
                info
            );
    }


}
