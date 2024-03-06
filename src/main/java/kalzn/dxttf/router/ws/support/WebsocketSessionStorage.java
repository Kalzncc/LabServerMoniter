package kalzn.dxttf.router.ws.support;


import io.javalin.websocket.WsCloseHandler;
import io.javalin.websocket.WsConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class WebsocketSessionStorage {
    private static final Map<String, Map<String, Object> > storage = new HashMap<>();



    public static void add(String id, String key, Object value) {
        if (!storage.containsKey(id)) {
            storage.put(id, new HashMap<>());
        }
        storage.get(id).put(key, value);
    }

    public static void closeSession(String id) {
        storage.remove(id);
    }

    public static Object get(String id, String key) {
        if (storage.containsKey(id) && storage.get(id).containsKey(key))
            return storage.get(id).get(key);
        return null;
    }

}
