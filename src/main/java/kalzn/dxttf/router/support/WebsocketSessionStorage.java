package kalzn.dxttf.router.support;


import io.javalin.websocket.WsCloseHandler;
import io.javalin.websocket.WsConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebsocketSessionStorage {
    private static final Map<String, Map<String, Object> > storage = new ConcurrentHashMap<>();



    public static void add(String id, String key, Object value) {
        if (!storage.containsKey(id)) {
            storage.put(id, new ConcurrentHashMap<>());
        }
        storage.get(id).put(key, value);

    }
    public static void addIfAbsent(String id, String key, Object value) {
        if (!storage.containsKey(id)) {
            storage.put(id, new ConcurrentHashMap<>());
        }
        storage.get(id).putIfAbsent(key, value);
    }

    public static Object remove(String id, String key) {
        if (storage.containsKey(id) && storage.get(id).containsKey(key)) {
            Object obj = storage.get(id).get(key);
            storage.get(id).remove(key);
            return obj;
        }
        return null;
    }
    public static Object get(String id, String key) {
        if (storage.containsKey(id) && storage.get(id).containsKey(key))
            return storage.get(id).get(key);
        return null;
    }

    public static boolean sessionActive(String id) {
        return storage.containsKey(id);
    }

    public static void openSession(String  id) {
        storage.put(id, new ConcurrentHashMap<>());
    }

    public static void closeSession(String id) {
        storage.remove(id);
    }



}
