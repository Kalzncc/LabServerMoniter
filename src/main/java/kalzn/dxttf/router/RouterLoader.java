package kalzn.dxttf.router;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;
import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.manager.ComponentManager;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * @kalzncc
 * <<Api Router Note>>
 * 1. Mount all router that do not require authentication to:    /public/*
 *
 * 2. Mount all router that require authentication to:           /private/*
 *    Before accessing the /private/* route, each session should first access /public/auth for authentication.
 *    The /private/* route has an authentication filter with a priority of -5, which filters out all unauthorized sessions.
 *
 * 3. Mount all websocket router that do not require authentication to: /publicWs/*
 *
 * 4. Mount all websocket router that require authentication to: /privateWs/*
 *    Every request attempting to connect to a route prefixed with /privateWs/* must undergo an authentication handshake process first.
 *    The /privateWs/* route has an authentication filter with a priority of -5, which filters out all unauthorized sessions.
 *    <<Private Websocket Connection Authentication>>
 *    In close authentication mode:
 *    1. Client: Open websocket connection.
 *    2. Server: Send json message: {status: 210, msg: "Ready to authenticate."}
 *    3. Client: Send json message: {authName: "", authToken: ""}
 *    If Authentication Success Then:
 *        4.1 Server: Send json message: {status: 211, msg: "Authentication success."}
 *        4.2 Private websocket connection authentication success, start to private interactive.
 *    Else Then:
 *        5.1 Server: Send json message: {status: 401, msg: "Unauthorized"}
 *        5.2 Server: Close websocket connection.
 *    In open authentication mode:
 *    1. Client: Open websocket connection.
 *    2. Server: Send json message: {status: 211, msg: "Authentication success."}
 *    3. Private websocket connection authentication success, start to private interactive.
 *
 * 5. Mount all router that need to run script with superuser privilege to: /privateWs/super/*
 *    The /private/super/* route has an authentication filter with a priority of -3, which filters out all unauthorized sessions.
 *    All requests that require privileged execution of scripts need to access /private/superAuthAgain for secondary authentication.
 *    Secondary authentication requires matching both the token and the password. Sessions that have already undergone secondary
 *    authentication do not need to be authenticated again.
 *
 * 6. In the open authentication mode, all authentication filters are disabled, allowing all requests to pass through without filtering.
 */
public class RouterLoader {


    private static  Javalin app = null;
    private static final Logger logger = LoggerFactory.getLogger(RouterLoader.class);

    private static final String[] JAVALIN_COMMON_HANDLE_METHODS = {"after", "before", "wsFilter", "filter", "wsBefore", "wsAfter"};


    private static class RouterManager extends ComponentManager {
        @Getter
        private static final Map<String, Object> components = new HashMap<>();
        public static void loadRouters () {
            registerComponents(components, RouterLoader.class.getPackage(), Component.ROUTER);
        }

    }

    private static void mountRoute(String type, String mapping, Object router, Method handler) {

        Method javalinMethod = null;
        if ("filter".equals(type)) type = "before";
        if ("wsFilter".equals(type)) type = "wsBefore";

        try {

            if (mapping.isEmpty()) {
                if (type.contains("ws")) {
                    javalinMethod = Javalin.class.getMethod(type, Consumer.class);
                    javalinMethod.invoke(app, (Consumer) context -> {
                        try {
                            handler.invoke(router, context);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw (RuntimeException) e.getTargetException();
                        }
                    });
                } else {
                    javalinMethod = Javalin.class.getMethod(type, Handler.class);
                    javalinMethod.invoke(app, (Handler) context -> {
                        try {
                            handler.invoke(router, context);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw (RuntimeException) e.getTargetException();
                        }
                    });
                }
            } else {
                if (type.contains("ws")) {
                    javalinMethod = Javalin.class.getMethod(type, String.class, Consumer.class);
                    javalinMethod.invoke(app, mapping, (Consumer) context -> {
                        try {
                            handler.invoke(router, context);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw (RuntimeException) e.getTargetException();
                        }
                    });
                } else {
                    javalinMethod = Javalin.class.getMethod(type, String.class, Handler.class);
                    javalinMethod.invoke(app, mapping, (Handler) context -> {
                        try {
                            handler.invoke(router, context);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw (RuntimeException) e.getTargetException();
                        }
                    });
                }
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Javalin no such method [" + type +"], mapping : [" + (mapping.isEmpty()?"ALL":mapping) + "]"+
                                        " in Router " + router.getClass().getName() + "." + handler.getName());
        } catch (IllegalAccessException| InvocationTargetException e) {
            throw new RuntimeException("Can't mount route : [" + (mapping.isEmpty()?"ALL":mapping) + "]" +
                                        "in Router " + router.getClass().getName() + "." + handler.getName());
        }


    }


    private static boolean registerRouterHandleMethods(Object router, Method handler) {
        Api api = handler.getAnnotation(Api.class);
        if (api.disable()) return false;
        String[] types = api.types();
        if (types.length == 0) {
            return false;
        }
        if (types.length == 1 && List.of(JAVALIN_COMMON_HANDLE_METHODS).contains(types[0])) {
            mountRoute(types[0], api.mapping(), router, handler);
            return true;
        }
        for (var type : types) {
            if (!GlobalConfig.server.allowMethods.contains(type)) {
                return false;
            }
        }
        for (var type : types) {
            mountRoute(type, api.mapping(), router, handler);
        }
        return true;
    }


    private static class ApiHandler{
        Object router;
        Method method;
        int priority;
        public ApiHandler(Object router, Method method) {
            this.router = router;
            this.method = method;
            priority = method.getAnnotation(Api.class).priority();
        }
    };


    private static void loadApis() {
        List<ApiHandler> handlers = new ArrayList<>();

        RouterManager.loadRouters();
        var routers = RouterManager.getComponents().values();
        for (var router : routers) {
            Method[] methods = router.getClass().getMethods();
            for (var method : methods) {
                if (method.getAnnotation(Api.class) != null) {
                    handlers.add(new ApiHandler(router, method));
                }
            }
        }
        handlers.sort(Comparator.comparingInt(o -> o.priority));
        for (var handler : handlers) {
            var router = handler.router;
            var method = handler.method;
            if (registerRouterHandleMethods(router, method)) {
                logger.info("Mount Handler : " + method.getAnnotation(Api.class).mapping() + " type :"
                        + List.of(method.getAnnotation(Api.class).types())
                );
            } else {
                logger.warn("Ignore Handler : " + routers.getClass().getName() + "." + method.getName());
            }
        }
    }
    private static void loadConfig() {
        app = Javalin.create(config -> {
           config.staticFiles.add(files -> {
               files.hostedPath = GlobalConfig.server.staticMap;
               files.directory = GlobalConfig.server.staticPath;
               files.location = Location.CLASSPATH;
           });
           config.staticFiles.add(files -> {
               files.hostedPath = GlobalConfig.server.externalStaticMap;
               files.directory = GlobalConfig.server.externalStaticPath;
               files.location = Location.EXTERNAL;
           });
        });
    }

    public static void loadRouter() {
        loadConfig();
        loadApis();


        // Build filter chain. (Set FilterReject exception handler)
        FilterChain.initialize(app);
    }

    public static void start(boolean block) {
        if (block)
            app.start(GlobalConfig.server.port);
        else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    app.start(GlobalConfig.server.port);
                }
            }).start();
        }
    }
}
