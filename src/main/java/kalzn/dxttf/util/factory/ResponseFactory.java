package kalzn.dxttf.util.factory;

import kalzn.dxttf.pojo.outer.Response;

public class ResponseFactory {
    public static final int Continue = 100;
    public static final int SwitchingProtocols = 101;
    public static final int Processing = 102;
    public static final int OK = 200;
    public static final int Created = 201;
    public static final int Accepted = 202;
    public static final int NoContent = 204;
    public static final int MovedPermanently = 301;
    public static final int MoveTemporarily = 302;
    public static final int BadRequest = 400;
    public static final int Unauthorized = 401;
    public static final int Forbidden = 403;
    public static final int NotFound = 404;
    public static final int MethodNotAllowed = 405;
    public static final int InternalServerError = 500;

    public static Response create(int status, String msg, Object data) {
        Response res = new Response();
        res.setMsg(msg);
        res.setStatus(status);
        res.setData(data);
        return res;
    }
    public static Response create(int status, String msg) {
        return create(status, msg, null);
    }
    public static Response create() {
        return create(200, "Ok.");
    }
    public static Response create(Object data) {
        return create(200, "Ok.", data);
    }
    public static Response create(int res) {
        switch (res) {
            case Continue -> {
                return create(100, "Continue");
            }
            case SwitchingProtocols -> {
                return create(101, "Switching Protocols");
            }
            case Processing -> {
                return create(102, "Processing");
            }
            case OK -> {
                return create(200, "OK");
            }
            case Created -> {
                return create(201, "Created");
            }
            case Accepted -> {
                return create(202, "Accepted");
            }
            case NoContent -> {
                return create(204, "No Content");
            }
            case MovedPermanently -> {
                return create(301, "Moved Permanently");
            }
            case MoveTemporarily -> {
                return create(302, "Processing");
            }
            case BadRequest -> {
                return create(400, "Bad Request");
            }
            case Unauthorized -> {
                return create(401, "Unauthorized");
            }
            case Forbidden -> {
                return create(403, "Forbidden");
            }
            case NotFound -> {
                return create(404, "Not Found");
            }
            case MethodNotAllowed -> {
                return create(405, "Method Not Allowed");
            }
            case InternalServerError -> {
                return create(500, "Internal Server Error");
            }
        }
        return create(500, "Internal Server Error");
    }
}
