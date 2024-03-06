package kalzn.dxttf.util.factory;

import io.javalin.http.Context;
import kalzn.dxttf.pojo.inner.AuthenticationTask;
import kalzn.dxttf.pojo.outer.PrivateReq;
import kalzn.dxttf.util.HashUtil;

import java.util.Map;

public class AuthenticationTaskFactory {
    public static AuthenticationTask createFromPrivateReq(String ip, PrivateReq req) {
        AuthenticationTask authTask = new AuthenticationTask();
        authTask.setName(req.getAuthName());
        authTask.setToken(req.getAuthToken());
        String ipStr = ip;
        if (ip.charAt(0) == '[' && ip.charAt(ip.length() - 1) == ']')
            ipStr = ip.substring(1, ip.length() - 1);
        authTask.setIp(ipStr);
        authTask.setAuthenticationType(AuthenticationTask.AUTHENTICATE_BY_TK);
        return authTask;
    }



    public static AuthenticationTask createFromLoginByPwdReqMap(Context ctx, Map<String, Object> pwdReq) {
        AuthenticationTask authTask = new AuthenticationTask();
        authTask.setName((String) pwdReq.get("name"));
        String ipStr = ctx.ip();
        if (ctx.ip().charAt(0) == '[' && ctx.ip().charAt(ctx.ip().length() - 1) == ']')
            ipStr = ctx.ip().substring(1, ctx.ip().length() - 1);

        authTask.setIp(ipStr);
        authTask.setAuthenticationType(AuthenticationTask.AUTHENTICATE_BY_PWD);
        authTask.setPassword(HashUtil.getMD5((String) pwdReq.get("password")));
        return authTask;
    }
}
