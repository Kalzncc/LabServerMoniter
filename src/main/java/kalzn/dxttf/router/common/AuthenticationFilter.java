package kalzn.dxttf.router.common;
import com.google.gson.Gson;
import io.javalin.http.Context;
import io.javalin.websocket.WsConfig;
import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.outer.AuthenticationResult;
import kalzn.dxttf.pojo.outer.PrivateReq;
import kalzn.dxttf.router.FilterChain;
import kalzn.dxttf.router.ws.support.WebsocketSessionStorage;
import kalzn.dxttf.service.ServiceManager;
import kalzn.dxttf.service.auth.AuthenticationService;
import kalzn.dxttf.util.checker.PrivateReqChecker;
import kalzn.dxttf.util.factory.AuthenticationTaskFactory;
import kalzn.dxttf.util.factory.ResponseFactory;

@Component(type = Component.ROUTER)
public class AuthenticationFilter {
    AuthenticationService authenticationService;

    public AuthenticationFilter() {
        authenticationService = ServiceManager.getService("auth_service", AuthenticationService.class);
    }




    @Api(types = {"filter"}, mapping = "/private/*", priority = -5)
    public void privateAuthFilter(Context ctx) {
        PrivateReq req = null;
        try {
            req = new Gson().fromJson(ctx.body(), PrivateReq.class);
            if (req.getAuthName().isEmpty() || req.getAuthToken().isEmpty()) {
                String sessionAuthName = ctx.sessionAttribute(GlobalConfig.auth.nameKey);
                String sessionAuthToken = ctx.sessionAttribute(GlobalConfig.auth.tokenKey);
                if (sessionAuthName == null || sessionAuthToken == null)
                    FilterChain.reject(401, "Unauthorized");
                req.setAuthName(sessionAuthName);
                req.setAuthToken(sessionAuthToken);
            }
        } catch (Exception e) {
            FilterChain.reject(401, "Unauthorized");
        }
        if (!PrivateReqChecker.check(req)) {
            FilterChain.reject(401, "Unauthorized");
        }

        AuthenticationResult res = authenticationService.authenticate(
                AuthenticationTaskFactory.createFromPrivateReq(ctx.ip(), req)
        );
        if (res.getResult() != AuthenticationResult.SUCCESS) {
            FilterChain.reject(401, "Unauthorized", res);
        }

    }


    private final static Integer WS_AUTHENTICATING = 0;
    private final static Integer WS_AUTHENTICATED  = 1;

    @Api(types = {"wsFilter"}, mapping = "/privateWs/*", priority = -5)
    public void privateWsAuthFilter(WsConfig wsConfig) {
        wsConfig.onConnect(ws -> {
            WebsocketSessionStorage.add(ws.sessionId(), "status", WS_AUTHENTICATING);
            ws.send(new Gson().toJson(ResponseFactory.create(210, "Ready to authenticate.")));
            FilterChain.wsReject(false, false);
        });
        wsConfig.onMessage(ws -> {
            if (WS_AUTHENTICATED.equals(WebsocketSessionStorage.get(ws.sessionId(), "status"))) {
                return;
            }
            String msg = ws.message();
            PrivateReq req = null;
            try {
                req = new Gson().fromJson(msg, PrivateReq.class);
                if (req.getAuthName().isEmpty() || req.getAuthToken().isEmpty()) {
                    FilterChain.wsReject(401, "Unauthorized", true);
                }
            } catch (Exception e) {
                FilterChain.wsReject(401, "Unauthorized", true);
            }
            if (!PrivateReqChecker.check(req)) {
                FilterChain.wsReject(401, "Unauthorized", true);
            }

            AuthenticationResult res = authenticationService.authenticate(
                    AuthenticationTaskFactory.createFromPrivateReq(ws.host(), req)
            );
            if (res.getResult() != AuthenticationResult.SUCCESS) {
                FilterChain.wsReject(401, "Unauthorized", res, true);
            } else {
                WebsocketSessionStorage.add(ws.sessionId(), "status", WS_AUTHENTICATED);
                ws.send(new Gson().toJson(ResponseFactory.create(211, "Authentication success.")));
                FilterChain.wsReject(false, false);
            }
        });

    }





}
