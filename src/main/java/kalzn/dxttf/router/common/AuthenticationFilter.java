package kalzn.dxttf.router.common;
import com.google.gson.Gson;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.websocket.WsConfig;
import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.outer.AuthenticationResult;
import kalzn.dxttf.pojo.outer.PrivateReq;
import kalzn.dxttf.router.FilterChain;
import kalzn.dxttf.router.support.WebsocketSessionStorage;
import kalzn.dxttf.service.ServiceManager;
import kalzn.dxttf.service.auth.AuthenticationService;
import kalzn.dxttf.util.GsonUtil;
import kalzn.dxttf.util.checker.PrivateReqChecker;
import kalzn.dxttf.util.checker.RequestChecker;
import kalzn.dxttf.util.factory.AuthenticationTaskFactory;
import kalzn.dxttf.util.factory.ResponseFactory;

import java.util.Map;

@Component(type = Component.ROUTER)
public class AuthenticationFilter {
    AuthenticationService authenticationService;

    public AuthenticationFilter() {
        authenticationService = ServiceManager.getService("auth_service", AuthenticationService.class);
    }




    @Api(types = {"filter"}, mapping = "/private/*", priority = -5)
    public void privateAuthFilter(Context ctx) {

        if (GlobalConfig.auth.openAuth) {
            return;
        }

        PrivateReq req = null;
        try {
            req = new Gson().fromJson(ctx.body(), PrivateReq.class);
            if (req == null || req.getAuthName() == null || req.getAuthToken() == null) {

                String sessionAuthName = ctx.sessionAttribute(GlobalConfig.auth.nameKey);
                String sessionAuthToken = ctx.sessionAttribute(GlobalConfig.auth.tokenKey);
                if (sessionAuthName == null || sessionAuthToken == null)
                    FilterChain.reject(401, "Unauthorized");
                req = new PrivateReq();
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


    /**
     * <<Private Websocket Connection Authentication>>
     * In close authentication mode:
     * 1. Client: Open websocket connection.
     * 2. Server: Send json message: {status: 210, msg: "Ready to authenticate."}
     * 3. Client: Send json message: {authName: "", authToken: ""}
     * If Authentication Success Then:
     *     4.1 Server: Send json message: {status: 211, msg: "Authentication success."}
     *     4.2 Private websocket connection authentication success, start to private interactive.
     * Else Then:
     *     5.1 Server: Send json message: {status: 401, msg: "Unauthorized"}
     *     5.2 Server: Close websocket connection.
     *
     * In open authentication mode:
     * 1. Client: Open websocket connection.
     * 2. Server: Send json message: {status: 211, msg: "Authentication success."}
     * 3. Private websocket connection authentication success, start to private interactive.
     */
    @Api(types = {"wsFilter"}, mapping = "/privateWs/*", priority = -5)
    public void privateWsAuthFilter(WsConfig wsConfig) {
        wsConfig.onConnect(ws -> {
            if (!GlobalConfig.auth.openAuth) {
                WebsocketSessionStorage.add(ws.sessionId(), "status", WS_AUTHENTICATING);
                ws.send(new Gson().toJson(ResponseFactory.create(210, "Ready to authenticate.")));
                FilterChain.wsReject(false, false);
            } else {
                WebsocketSessionStorage.add(ws.sessionId(), "status", WS_AUTHENTICATED);
                ws.send(new Gson().toJson(ResponseFactory.create(211, "Authentication success.")));
                FilterChain.wsReject(false, false);
            }
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
                WebsocketSessionStorage.add(ws.sessionId(), "token", req.getAuthToken());
                WebsocketSessionStorage.add(ws.sessionId(), "user", req.getAuthName());
                ws.send(new Gson().toJson(ResponseFactory.create(211, "Authentication success.")));
                FilterChain.wsReject(false, false);
            }
        });
    }

    /**
     * <<Private Websocket Connection Secondary Authentication>>
     * After <<Private Websocket Connection Authentication>> done:
     * In close authentication mode:
     * 1. Client send any message
     * If Client has done private websocket connection authentication Then:
     *      2.1 Server Pass
     * Else :
     *      3.1 Server send json message:  {status: 212, msg: "Need secondary authentication."}
     *      3.2 Client send json message: {name: "", password: ""}
     *      If Authentication Success Then:
     *          3.3.1 Server send json message:  {status: 213, msg: "Secondary authentication Success."}
     *      Else:
     *          3.4.1 Server send json message:  {status: 401, msg: "Unauthorized"}
     *
     * In open authentication mode:
     * No Secondary Authentication
     */
    @Api(types = "wsFilter", mapping = "/privateWs/super/*", priority = -3)
    public void dangrousWsScirptExecuteAuthAgain(WsConfig wsConfig) {
        wsConfig.onMessage(ws -> {
                if (GlobalConfig.auth.openAuth) {
                return;
            }
            if (WebsocketSessionStorage.get(ws.sessionId(), "superAuth") != null) {
                return;
            }
            AuthenticationResult res = null;
            try {
                String msg = ws.message();
                Map<String, Object> req = GsonUtil.toStringKeyMap(msg);
                if (req == null || !RequestChecker.checkLoginByPwdReq(req)) {
                    throw new RuntimeException("");
                }
                res = authenticationService.authenticate(
                        AuthenticationTaskFactory.createFromWsSuperAuthReq(ws.sessionId(), ws.host(), req)
                );
            } catch (Exception e) {
                ws.send(new Gson().toJson(ResponseFactory.create(212, "Need secondary authentication.")));
                FilterChain.wsReject(false, false);
                return;
            }


            if (res.getResult() != AuthenticationResult.SUCCESS) {
                FilterChain.wsReject(401, "Unauthorized", res, true);
            } else {
                ws.send(new Gson().toJson(ResponseFactory.create(213, "Secondary authentication Success.")));
                WebsocketSessionStorage.add(ws.sessionId(), "superAuth", "1");
                FilterChain.wsReject(false, false);

            }
        });
    }

    /**
     * Executing script with superuser privilege need to pass authentication again.
     */
    @Api(types = "filter", mapping = "/private/super/*", priority = -3)
    public void dangerousScriptExecuteAuthAgain(Context ctx) {
        if (GlobalConfig.auth.openAuth)
            return;
        if (!GlobalConfig.script.superUserAuthAgain)
            return;
        if (ctx.sessionAttributeMap().containsKey("superAuthPass"))
            return;
        FilterChain.reject(401, "Unauthorized");
    }



}
