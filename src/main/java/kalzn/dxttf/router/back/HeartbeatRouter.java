package kalzn.dxttf.router.back;

import com.google.gson.Gson;
import io.javalin.http.Context;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.outer.HeartbeatInfo;
import kalzn.dxttf.service.ServiceManager;
import kalzn.dxttf.service.heartbeat.HeartbeatService;
import kalzn.dxttf.util.LogRecord;
import kalzn.dxttf.util.factory.ResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(type = Component.ROUTER)
public class HeartbeatRouter {

    private final HeartbeatService heartbeatService;
    private final Logger logger = LoggerFactory.getLogger(HeartbeatRouter.class);

    public HeartbeatRouter() {
        heartbeatService = ServiceManager.getService("heartbeat_service", HeartbeatService.class);
    }

    @Api(types = "get", mapping = "/private/heartbeat")
    public void heartbeat(Context ctx) {
        try {
            HeartbeatInfo info = heartbeatService.getHeartbeatInfo();
            ctx.result(new Gson().toJson(ResponseFactory.create(info)));
        } catch (Exception e) {
            ctx.result(new Gson().toJson(ResponseFactory.create(500, e.getMessage())));
        }
    }

    @Api (types = "get", mapping = "/private/cudaDetail")
    public void cudaDetail(Context ctx) {
        try {
            String cudaIdStr = ctx.queryParam("cudaId");
            if (cudaIdStr == null) throw new RuntimeException("");
            int cudaId = Integer.parseInt(cudaIdStr);

            String detail = heartbeatService.getCudaDetailInfo(cudaId);

            Map<String, String> data = new HashMap<>();
            data.put("detail", detail);

            ctx.result(new Gson().toJson(ResponseFactory.create(data)));

        } catch (Exception e) {
            ctx.result(new Gson().toJson(ResponseFactory.create(500, e.getMessage())));
        }
    }

    @Api (types = "get", mapping = "/private/systemInfo")
    public void systemInfo(Context ctx) {
        try {
            String info = heartbeatService.getSystemInfo();
            Map<String, String> data = new HashMap<>();
            data.put("info", info);
            ctx.result(new Gson().toJson(ResponseFactory.create(data)));

        } catch (Exception e) {
            ctx.result(new Gson().toJson(ResponseFactory.create(500, e.getMessage())));
        }
    }

    @Api (types = "get", mapping = "/private/super/kill")
    public void kill(Context ctx) {
        try {
            assert ctx.queryParam("pid") != null;
            logger.info(LogRecord.INFO_SUPER_PRIVILEGE_EXECUTE_ATTEMPT("/private/super/kill", ctx.ip(), "Kill "+ctx.queryParam("pid")));
            String result = heartbeatService.kill(Integer.parseInt(ctx.queryParam("pid")));

        } catch (Exception e) {
            // kill script execution results are not echoed.
            ctx.result(new Gson().toJson(ResponseFactory.create()));
        }
        ctx.result(new Gson().toJson(ResponseFactory.create()));
    }
}
