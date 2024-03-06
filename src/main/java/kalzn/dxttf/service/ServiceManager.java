package kalzn.dxttf.service;

import kalzn.dxttf.manager.ComponentManager;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class ServiceManager extends ComponentManager {
    @Getter
    private static final Map<String, Object> components = new HashMap<>();
    public static void registerServices() {
        ComponentManager.registerComponents(components, ServiceManager.class.getPackage());
    }
    public static <T> T getService(String name, Class<T> serviceClass, boolean prototype) {
        return ComponentManager.getComponent(components, name, serviceClass, prototype);
    }

    public static <T> T getService(String name, Class<T> serviceClass) {
        return ComponentManager.getComponent(components, name, serviceClass, false);
    }

    public static void unregisterService(String serviceName) {
        ComponentManager.unregisterComponent(components, serviceName);
    }

}
