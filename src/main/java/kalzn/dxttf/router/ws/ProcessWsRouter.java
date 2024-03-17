package kalzn.dxttf.router.ws;


import com.google.gson.Gson;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.service.ServiceManager;
import kalzn.dxttf.service.process.ProcessService;
import kalzn.dxttf.util.GsonUtil;
import kalzn.dxttf.util.LogRecord;
import kalzn.dxttf.util.checker.RequestChecker;
import kalzn.dxttf.util.factory.ResponseFactory;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @kalzncc
 * The process session requires secondary authentication.
 * Process monitor output:
 * 1. Clent send json message : {type: "0", msg: "<pid>"} (Note: 'type' and 'pid' are both in string format.)
 * 2. Server start push process output, there are six types of output:
 *      0) Session open: <timestamp>$open$                  This is the flag of successful session creation,
 *                                                          sent immediately after the session is established by the server.
 *
 *      1) Stdout of process: <timestamp>$stdout$<output>   e.g.    1710513649662$stdout$HelloWorld!
 *
 *      2) Stderr of process: <timestamp>$stderr$<output>   e.g.    1710513649662$stderr$Error!
 *
 *      3) Keep active:       <timestamp>$keep_active$      This is information to keep the WebSocket session.
 *                                                          Please ignore.
 *
 *      4) Process exit:      <timestamp>$exit$             The server s,
    psession: {
        exit: "Process Exit",
        link: "Linking Process Session: ",
        needAuthAgain: "Need Secondary Authentication"
    }ending this message indicates that the monitored
 *                                                          process has exited, and the server will subsequently close
 *                                                          the WebSocket session.
 *
 *      5) System Error       <timestamp>$error$<msg>       An error has occurred in the system, and the error message
 *                                                          is being output. The server will subsequently close
 *                                                          the WebSocket session.
 *
 *  Note: 1. It cannot be guaranteed that the output will arrive at the client in order. If necessary, please sort by timestamp.
 *        2. A WebSocket session can only monitor the output of one process.
 */

@Slf4j
@Component(type = Component.ROUTER)
public class ProcessWsRouter {

    private static class PsessionConnection {
        public Integer monitorPid;
        public Map<String, WsMessageContext> listener;

    }
    private final static Map<Integer, PsessionConnection> sessionPool = new ConcurrentHashMap<>();
    private final static Map<String, Integer> listenedPid = new ConcurrentHashMap<>();

    private final Lock lock = new ReentrantLock();
    private final Condition hasListener = lock.newCondition();

    private final ProcessService processService;

    private void keepActive() {
        while (true) {
            lock.lock();
            try {
                if (listenedPid.isEmpty()) hasListener.await();
                for (var pid : sessionPool.keySet()) {
                    if (sessionPool.get(pid).listener == null)
                        continue;
                    for (var conn : sessionPool.get(pid).listener.values()) {
                        asynSend(conn, new Gson().toJson(ResponseFactory.create(System.currentTimeMillis() + "$keep_active$")));
                    }
                }
            } catch (Exception ignored) { } finally {
                lock.unlock();
            }
            try {
                Thread.sleep(5000);
            } catch (Exception ignored) {}
        }
    }

    public ProcessWsRouter() {
        processService = ServiceManager.getService("process_service", ProcessService.class);

        new Thread(this::keepActive).start();


    }
    private void asynSend(WsContext ctx, String msg) {
        new Thread(() -> {
            try {
                ctx.send(msg);
            } catch (Exception ignored) { }
        }).start();
    }
    private void asynClose(WsContext ctx, String bye) {
        new Thread(() -> {
            try {
                handleClose(ctx);
                ctx.send(bye);

                ctx.closeSession();
            } catch (Exception ignored) { }
        }).start();
    }
    private void asynClose(WsContext ctx) {
        new Thread(() -> {
            try {
                handleClose(ctx);
                ctx.closeSession();
            } catch (Exception ignored) { }
        }).start();
    }
    private void handleClose(WsContext ws) {
        lock.lock();
        try {
            String sessionId = ws.sessionId();
            Integer pid = listenedPid.get(sessionId);
            listenedPid.remove(sessionId);
            if (pid == null) {
                return;
            }
            sessionPool.get(pid).listener.remove(sessionId);
            if (sessionPool.get(pid).listener.isEmpty()) {
                processService.closePsession(pid);
                sessionPool.remove(pid);
            }
        } catch (Exception ignored) { } finally {
            lock.unlock();
        }
    }
    private void handleOutput(Object obj, Exception e) {
        List<WsContext> closeSession = new ArrayList<>();
        String closeInfo = "";
        lock.lock();
        try {
            if (e != null) {
                Map<String, Object> mobj = (Map<String, Object>) obj;
                Integer pid = (Integer) mobj.get("pid");
                if ("Process Exited.".equals(e.getMessage())) {
                    closeInfo = System.currentTimeMillis()+"$exit$";
                    for (var listener : sessionPool.get(pid).listener.values()) {
                        closeSession.add(listener);
                    }
                } else {
                    closeInfo = System.currentTimeMillis()+"$error$"+e.getMessage();
                    for (var listener : sessionPool.get(pid).listener.values()) {
                        closeSession.add(listener);
                    }
                }
            } else {
                Map<String, Object> mobj = (Map<String, Object>) obj;
                Integer pid = (Integer) mobj.get("pid");
                String msg = (String) mobj.get("out");
                for (var listener : sessionPool.get(pid).listener.values()) {
                    asynSend(listener, new Gson().toJson(ResponseFactory.create(msg)));
                }
            }
        } catch (Exception ignored) {

        } finally {
            lock.unlock();
        }

        for (var session : closeSession) {
            asynClose(session, new Gson().toJson(ResponseFactory.create(closeInfo)));
        }
    }
    @Api(types = "ws", mapping = "/privateWs/super/psession")
    public void psession(WsConfig wsConfig) {
        wsConfig.onMessage(ws-> {
            int msgType = 0;
            String msgContent = "";
            int pid = -1;
            try {
                Map<String, Object> msg = GsonUtil.toStringKeyMap(ws.message());

                if (!RequestChecker.checkContain(msg, new RequestChecker.Remember[]{
                        new RequestChecker.Remember(
                                "type", String.class.getName()),
                        new RequestChecker.Remember("msg", String.class.getName())

                }, true)) {
                    throw new RuntimeException("");
                }
                assert msg != null;
                msgType = Integer.parseInt((String) msg.get("type"));
                msgContent = (String) msg.get("msg");
                if (msgType == 0)
                    pid = Integer.parseInt(msgContent);
            } catch (Exception e) {
                ws.send(new Gson().toJson(ResponseFactory.create(ResponseFactory.BadRequest)));
            }
            log.info(LogRecord.INFO_SUPER_PRIVILEGE_EXECUTE_ATTEMPT("/privateWs/super/psession", ws.host(), ws.message()));

            if (msgType == 0) {
                // Handling listening request.
                lock.lock();
                try {
                    if (listenedPid.containsKey(ws.sessionId())) {
                        // One websocket session only monitor one process.
                        // Send message asynchronous, release lock timely.
                        asynSend(ws, new Gson().toJson(ResponseFactory.create(ResponseFactory.Forbidden)));
                        return;
                    }

                    listenedPid.put(ws.sessionId(), pid);


                    if (!sessionPool.containsKey(pid)) {
                        processService.openPsession(pid, (this::handleOutput));

                        var conn = new PsessionConnection();
                        conn.monitorPid = pid;
                        conn.listener = new ConcurrentHashMap<>();
                        sessionPool.put(pid, conn);
                    }
                    sessionPool.get(pid).listener.put(ws.sessionId(), ws);

                    hasListener.signalAll();
                    asynSend(ws, new Gson().toJson(ResponseFactory.create(System.currentTimeMillis()+"$open$")));

                } catch (Exception e) {
                    asynSend(ws, new Gson().toJson(ResponseFactory.create(ResponseFactory.InternalServerError)));
                } finally {
                    lock.unlock();
                }
            } else if (msgType == 1) {
                // Handling enter stdin request.
                lock.lock();
                try {
                    // Websocket session has not yet been bound to process monitor session.
                    if (!listenedPid.containsKey(ws.sessionId())) {
                        asynSend(ws, new Gson().toJson(ResponseFactory.create(ResponseFactory.Forbidden)));
                        return;
                    }
//                    processService.enterStdin(pid, msgContent);

                } catch (Exception e) {
                    asynSend(ws, new Gson().toJson(ResponseFactory.create(ResponseFactory.InternalServerError)));
                } finally {
                    lock.unlock();
                }
            }


        });
        wsConfig.onClose(ws -> {
            handleClose(ws);
        });
    }
}
