package kalzn.dxttf.router.ws;


import com.google.gson.Gson;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsMessageContext;
import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.outer.HeartbeatInfo;
import kalzn.dxttf.router.support.WebsocketSessionStorage;
import kalzn.dxttf.service.ServiceManager;
import kalzn.dxttf.service.heartbeat.HeartbeatService;
import kalzn.dxttf.util.factory.ResponseFactory;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Component(type = Component.ROUTER)
public class HeartbeatWsRouter {

    private final static Map<String, WsMessageContext> sessionPool = new ConcurrentHashMap<>();
    private final static Lock poolLock = new ReentrantLock();
    private final static Condition heartbeatListenerExist = poolLock.newCondition();


    private HeartbeatService heartbeatService = null;
    public HeartbeatWsRouter() {
        heartbeatService = ServiceManager.getService("heartbeat_service", HeartbeatService.class);
        new Thread(new SendHeartbeat(heartbeatService)).start();
    }

    private record SendHeartbeat(HeartbeatService service) implements Runnable {
        @Override
        public void run() {
            while (true) {
                List<WsMessageContext> sendTarget = new ArrayList<>();
                poolLock.lock();
                try {
                    if (sessionPool.isEmpty())
                        heartbeatListenerExist.await();
                    sendTarget.addAll(sessionPool.values());
                } catch (InterruptedException ignored) {
                } finally {
                    poolLock.unlock();
                }
                HeartbeatInfo info = service.getHeartbeatInfo();

                for (var ctx : sendTarget) {
                    new Thread(() -> {
                        try {
                            ctx.send(new Gson().toJson(ResponseFactory.create(info)));
                        } catch (Exception ignored) {}
                    }).start();
                }

                try {
                    Thread.sleep(GlobalConfig.server.heartbeatInterval);
                } catch (InterruptedException ignored) {}
            }
        }
        }



    @Api(types = "ws", mapping = "/privateWs/heartbeat")
    public void heartbeatWs(WsConfig ws) {
        ws.onMessage(ctx -> {
            if ("start".equals(ctx.message())) {
                poolLock.lock();
                try {
                    if (sessionPool.size() >= GlobalConfig.server.ws.maxKeepActive) {
                        ctx.send(ResponseFactory.create(212, "Websocket connection pool is full."));
                        ctx.closeSession();
                        return;
                    }
                    sessionPool.put(ctx.sessionId(), ctx);
                    heartbeatListenerExist.signalAll();
                } finally {
                    poolLock.unlock();
                }
            } else if ("stop".equals(ctx.message())) {
                poolLock.lock();
                try {
                    sessionPool.remove(ctx.sessionId());
                } finally {
                    poolLock.unlock();
                }
            }
        });
        ws.onClose(ctx -> {
            WebsocketSessionStorage.closeSession(ctx.sessionId());
            poolLock.lock();
            try {
                sessionPool.remove(ctx.sessionId());
            } finally {
                poolLock.unlock();
            }
        });
    }
}
