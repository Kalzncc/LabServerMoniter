package kalzn.dxttf.util.factory;

import kalzn.dxttf.pojo.outer.Response;

public class ResponseFactory {


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
}
