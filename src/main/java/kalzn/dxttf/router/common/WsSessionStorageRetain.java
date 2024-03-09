package kalzn.dxttf.router.common;


import io.javalin.websocket.WsConfig;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.router.support.WebsocketSessionStorage;

@Component(type = Component.ROUTER, disable = true)
public class WsSessionStorageRetain {


    @Api(types = "wsBefore",  priority = -5)
    public void wsSessionOpen(WsConfig ws) {
        ws.onConnect(ctx -> {
            WebsocketSessionStorage.openSession(ctx.sessionId());
        });
    }

    @Api(types = "wsAfter", priority = 5)
    public void wsSessionClose(WsConfig ws) {
        ws.onClose(ctx -> {
            WebsocketSessionStorage.closeSession(ctx.sessionId());
        });
    }
}
