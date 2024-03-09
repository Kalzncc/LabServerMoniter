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
