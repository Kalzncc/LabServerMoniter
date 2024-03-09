package kalzn.dxttf.manager;

import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.util.LogRecord;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public abstract class ComponentManager {

    private static final Logger logger = LoggerFactory.getLogger(ComponentManager.class);



    protected static void registerComponents(Map<String, Object> components, Package manager, int componentType) {
        String componentPackageName = manager.getName();
        Reflections reflector = new Reflections(componentPackageName);
        Set<Class<?>> componentClasses = reflector.getTypesAnnotatedWith(Component.class);
        componentClasses.forEach(componentClass -> {
            Component annotation = componentClass.getAnnotation(Component.class);
            if (annotation == null || annotation.disable() || annotation.type() != componentType)
                return;
            String[] names = annotation.name().length == 0?new String[]{componentClass.getSimpleName()}:annotation.name();
            for (var name : names) {
                if (components.containsKey(name)) {
                    logger.warn(String.format(LogRecord.WARN_COMPONENT_DUPLICATE, name));
                    return;
                }
                try {
                    components.put(name, componentClass.getConstructor().newInstance());
                } catch (Exception e) {
                    logger.warn(String.format(LogRecord.WARN_COMPONENT_CONSTRUCT, name));
                    return;
                }
                logger.info(String.format(LogRecord.INFO_COMPONENT_REGISTER, name));
            }
        });
    }

    protected static <T> T getComponent(Map<String, Object> components, String name, Class<T> componentClass, boolean prototype) {
        if (componentClass.getAnnotation(Component.class) == null) {
            return null;
        }
        if (!components.containsKey(name) || !componentClass.isInstance(components.get(name))) {
            return null;
        }
        if (prototype) {
            try {
                return (T) components.get(name).getClass().getConstructor().newInstance();
            } catch (Exception e) {
                return null;
            }
        } else {
            return (T) components.get(name);
        }

    }

    protected static void unregisterComponent(Map<String, Object> components, String componentName) {
        components.remove(componentName);
    }
    

}
