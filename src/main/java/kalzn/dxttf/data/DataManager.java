package kalzn.dxttf.data;

import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.manager.ComponentManager;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class DataManager extends ComponentManager  {
    @Getter
    private static final Map<String, Object> components = new HashMap<>();
    public static void registerDatabases() {
        ComponentManager.registerComponents(components, DataManager.class.getPackage(), Component.DATABASE);
    }
    public static <T> T getDatabase(String name, Class<T> databaseClass, boolean prototype) {
        return ComponentManager.getComponent(components, name, databaseClass, prototype);
    }

    public static <T> T getDatabase(String name, Class<T> databaseClass) {
        return ComponentManager.getComponent(components, name, databaseClass, false);
    }

    public static void unregisterDatabase(String databaseName) {
        ComponentManager.unregisterComponent(components, databaseName);
    }
}
