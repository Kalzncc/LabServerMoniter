package kalzn.dxttf.router;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import kalzn.dxttf.router.ws.support.WebsocketSessionStorage;
import kalzn.dxttf.util.factory.ResponseFactory;
import lombok.Getter;

public class FilterChain {

    private static class FilterReject extends HttpResponseException {
        private final boolean autoBuildResponse;
        @Getter private Object data;
        @Getter private boolean wsCloseConnection;
        public FilterReject(boolean autoBuild, boolean wsCloseConnection) {
            super(400, "Bad Request");
            this.autoBuildResponse = autoBuild;
            this.wsCloseConnection = wsCloseConnection;
        }
        public FilterReject(int status, String msg, Object data, boolean wsCloseConnection) {
            super(status, msg);
            this.data = data;
            this.autoBuildResponse = true;
            this.wsCloseConnection = wsCloseConnection;
        }

    }

    public static void initialize(Javalin app) {
        app.exception(FilterReject.class, (filterReject, context) -> {
            if (!filterReject.autoBuildResponse) {
                return;
            }
            context.status(filterReject.getStatus());
            context.result(new Gson().toJson(
                    ResponseFactory.create(
                            filterReject.getStatus(),
                            filterReject.getMessage(),
                            filterReject.getData()
                    )));
        });
        app.wsException(FilterReject.class, (filterReject, wsContext) -> {
            if (!filterReject.autoBuildResponse) {
                if (filterReject.wsCloseConnection) {
                    WebsocketSessionStorage.closeSession(wsContext.sessionId());
                    wsContext.closeSession();
                }
                return;
            }
            wsContext.send(new Gson().toJson(
                    ResponseFactory.create(
                            filterReject.getStatus(),
                            filterReject.getMessage(),
                            filterReject.getData()
                    )));
            if (filterReject.wsCloseConnection) {
                WebsocketSessionStorage.closeSession(wsContext.sessionId());
                wsContext.closeSession();
            }
        });
    }

    // Filter pass do nothing.
    public static void pass() { return; }

    public static void reject(boolean autoFlag) {
        throw new FilterReject(autoFlag, false);
    }
    public static void wsReject(boolean autoFlag, boolean closeWsConnection) {
        throw new FilterReject(autoFlag, closeWsConnection);
    }



    public static void reject(String msg) {
        throw new FilterReject(400, msg, null, false);
    }
    public static void wsReject(String msg, boolean closeWsConnection) {
        throw new FilterReject(400, msg, null, closeWsConnection);
    }

    public static void reject(int status, String msg) {
        throw new FilterReject(status, msg, null, false);
    }
    public static void wsReject(int status, String msg, boolean wsCloseConnection) {
        throw new FilterReject(status, msg, null, wsCloseConnection);
    }

    public static void reject(String msg, Object data) {
        throw new FilterReject(400, msg, data, false);
    }
    public static void wsReject(String msg, Object data, boolean wsCloseConnection) {
        throw new FilterReject(400, msg, data, wsCloseConnection);
    }

    public static void reject(int status, String msg, Object data) {
        throw new FilterReject(status, msg, data, false);
    }
    public static void wsReject(int status, String msg, Object data, boolean wsCloseConnection) {
        throw new FilterReject(status, msg, data, wsCloseConnection);
    }

    public static void reject(Object data) {
        throw new FilterReject(400, "Bad Request.", data, false);
    }
    public static void wsReject(Object data, boolean wsCloseConnection) {
        throw new FilterReject(400, "Bad Request.", data, wsCloseConnection);
    }


}
