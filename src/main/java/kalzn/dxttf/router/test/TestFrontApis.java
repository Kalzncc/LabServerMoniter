package kalzn.dxttf.router.test;

import com.google.gson.Gson;
import io.javalin.http.Context;
import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.inner.AuthenticationTask;
import kalzn.dxttf.pojo.outer.AuthenticationResult;
import kalzn.dxttf.service.ServiceManager;
import kalzn.dxttf.service.auth.AuthenticationService;
import kalzn.dxttf.util.TokenUtil;
import kalzn.dxttf.util.factory.ResponseFactory;


import java.util.Map;

/**
 * Front server api.
 * Only used in test.
 */
@Component(disable = true, type = Component.ROUTER)
public class TestFrontApis {

    private final AuthenticationService authenticationService = ServiceManager.getService("auth_service", AuthenticationService.class);


    @Api(types = {"before"}, mapping = "/info.html")
    public void info(Context ctx) {

        try {
            String token = ctx.sessionAttribute(GlobalConfig.auth.tokenKey);
            String name = ctx.sessionAttribute(GlobalConfig.auth.nameKey);
            if ((token == null || name == null) && GlobalConfig.auth.cookieAuth) {
                token = ctx.cookie(GlobalConfig.auth.tokenKey);
                name = ctx.cookie(GlobalConfig.auth.nameKey);
            }
            if (token == null || name == null) {
                ctx.redirect("/login.html");
                return;
            }

            AuthenticationTask authReq = new AuthenticationTask();
            String ipStr = ctx.ip();
            if (ctx.ip().charAt(0) == '[' && ctx.ip().charAt(ctx.ip().length() - 1) == ']')
                ipStr = ctx.ip().substring(1, ctx.ip().length() - 1);

            authReq.setIp(ipStr);
            authReq.setName(name);
            authReq.setToken(token);
            authReq.setAuthenticationType(AuthenticationTask.AUTHENTICATE_BY_TK);

            var res = authenticationService.authenticate(authReq);
            if (res.getResult() != AuthenticationResult.SUCCESS) {
                ctx.redirect("/login.html");
                return;
            }

        }  catch (Exception e) {
            ctx.redirect("/error.html");
        }



    }


    @Api(types = "get", mapping = "/", disable = false)
    public void redirectLogin(Context ctx) {
        ctx.redirect("/info.html");
    }

    @Api(types = "post", mapping = "/loginByPwd")
    public void login(Context ctx) {
        try {
            Map<String, Object> loginReq = (Map<String, Object>) new Gson().fromJson(ctx.body(), Map.class);
            AuthenticationTask authReq = new AuthenticationTask();

            String ipstr = ctx.ip().substring(1, ctx.ip().length()-1);
            authReq.setIp(ipstr);
            authReq.setName((String) loginReq.get("name"));
            authReq.setPassword(TokenUtil.getMD5((String) loginReq.get("password")));
            authReq.setAuthenticationType(AuthenticationTask.AUTHENTICATE_BY_PWD);

            var res = authenticationService.authenticate(authReq);

            if (res.getResult() == AuthenticationResult.SUCCESS) {
                ctx.sessionAttribute(GlobalConfig.auth.tokenKey, res.getToken());
                ctx.cookie(GlobalConfig.auth.tokenKey, res.getToken(), 100);

                ctx.sessionAttribute(GlobalConfig.auth.nameKey, res.getName());
                ctx.cookie(GlobalConfig.auth.nameKey, res.getName());


                ctx.result(new Gson().toJson(ResponseFactory.create(res)));
            } else {
                ctx.redirect("/error.html");
            }

        } catch (Exception e) {
            ctx.redirect("/error.html");
        }

    }

    @Api(types = "before", mapping = "/s")
    public boolean test1(Context ctx) {
        ctx.result("Haha");
        return false;
    }

    @Api(types = "before", mapping = "/s")
    public void test2(Context ctx) {
        ctx.result("Haha");
    }


    @Api(types = "get", mapping = "/s")
    public void test3(Context ctx) {
        ctx.result("Haha");
    }
}
