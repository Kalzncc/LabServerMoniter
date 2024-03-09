package kalzn.dxttf.router.back;

import com.google.gson.Gson;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.outer.AuthenticationResult;
import kalzn.dxttf.service.ServiceManager;
import kalzn.dxttf.service.auth.AuthenticationService;
import kalzn.dxttf.util.GsonUtil;
import kalzn.dxttf.util.checker.RequestChecker;
import kalzn.dxttf.util.factory.AuthenticationTaskFactory;
import kalzn.dxttf.util.factory.ResponseFactory;

import java.util.Map;

@Component(type = Component.ROUTER)
public class LoginRouter {


    private final AuthenticationService authenticationService;
    public LoginRouter() {
        authenticationService = ServiceManager.getService("auth_service", AuthenticationService.class);
    }

    @Api(types = "post", mapping = "/private/auth")
    public void defaultAuth(Context ctx) {
        AuthenticationResult result = new AuthenticationResult();
        result.setResult(AuthenticationResult.SUCCESS);
        ctx.result(new Gson().toJson(ResponseFactory.create(result)));
    }
    @Api (types = "post", mapping = "/private/logout")
    public void logout(Context ctx) {
        ctx.consumeSessionAttribute(GlobalConfig.auth.nameKey);
        ctx.consumeSessionAttribute(GlobalConfig.auth.tokenKey);
        ctx.removeCookie(GlobalConfig.auth.nameKey);
        ctx.removeCookie(GlobalConfig.auth.tokenKey);
        ctx.result(new Gson().toJson(ResponseFactory.create()));
    }
    @Api(types = "post", mapping = "/public/loginByPwd")
    public void loginByPwd(Context ctx) {
        if (GlobalConfig.auth.openAuth) {
            AuthenticationResult result = new AuthenticationResult();
            result.setResult(AuthenticationResult.SUCCESS);
            ctx.result(new Gson().toJson(ResponseFactory.create(result)));
            return;
        }

        Map<String, Object> req = GsonUtil.toStringKeyMap(ctx.body());
        if (req == null || !RequestChecker.checkLoginByPwdReq(req)) {
            throw new BadRequestResponse();
        }
        AuthenticationResult res = authenticationService.authenticate(
                AuthenticationTaskFactory.createFromLoginByPwdReqMap(ctx, req)
        );
        if (res.getResult() == AuthenticationResult.SUCCESS) {
            ctx.cookie(GlobalConfig.auth.nameKey, res.getName());
            ctx.cookie(GlobalConfig.auth.tokenKey, res.getToken());
        }
        ctx.result(new Gson().toJson(ResponseFactory.create(res)));

    }
}
