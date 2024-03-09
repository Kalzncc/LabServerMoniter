package kalzn.dxttf.executor;


import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.ScriptRegisterConfig;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.manager.ComponentManager;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutorManager extends ComponentManager {
    @Getter
    private static final Map<String, Object> components = new HashMap<>();
    private static final Map<String, ScriptRegisterConfig> componentConfigs = new HashMap<>();

    private static int toTypeInt(String type) {
        if ("py".equals(type)) {
            return Executor.PYTHON;
        } else if ("bat".equals(type)) {
            return Executor.BAT;
        } else {
            return Executor.BASH;
        }
    }

    public static void registerExecutors() {
        ComponentManager.registerComponents(components, ExecutorManager.class.getPackage(), Component.EXECUTOR);

        // Executor component need bind external script.
        List<ScriptRegisterConfig> registers = GlobalConfig.script.register;
        Map<String, ScriptRegisterConfig> registerMap = new HashMap<>();
        for (var register : registers) {
            registerMap.put(register.executor, register);
        }
        for (var name : components.keySet()) {
            if (registerMap.containsKey(name)) {
                try {
                    if (!(components.get(name) instanceof Executor)) {
                        throw new RuntimeException("Try register executor component " +
                                components.get(name).getClass().getName() +
                                " But it not extend abstract Executor");
                    }
                    var executorClass = components.get(name).getClass();
                    var executorConstruct = executorClass.getConstructor(String.class, Integer.class, Boolean.class);
                    components.put(name, executorConstruct.newInstance(
                            registerMap.get(name).target,
                            toTypeInt(registerMap.get(name).type),
                            registerMap.get(name).superUser
                    ));
                    componentConfigs.put(name, registerMap.get(name));
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException e) {
                    // It's impossible, Make compiler happy!
                    throw new RuntimeException(e.getMessage());
                }
            } else {
                throw new RuntimeException("Try register executor component " +
                        components.get(name).getClass().getName() +
                        " But configuration is not found.");
            }
        }

    }
    public static <T> T getExecutor(String name, Class<T> serviceClass, boolean prototype) {
        if (!prototype)
            return ComponentManager.getComponent(components, name, serviceClass, false);
        else {
            T executor = ComponentManager.getComponent(components, name, serviceClass, false);
            if (executor == null) return null;
            try {
                Constructor<T> executorConstruct =
                        (Constructor<T>) executor.getClass().getConstructor(String.class, Integer.class, Boolean.class);
                return executorConstruct.newInstance(
                        componentConfigs.get(name).target,
                        toTypeInt(componentConfigs.get(name).type),
                        componentConfigs.get(name).superUser
                );
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                // It's impossible, Make compiler happy!
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    public static <T> T getExecutor(String name, Class<T> serviceClass) {
        return ComponentManager.getComponent(components, name, serviceClass, false);
    }

    public static void unregisterExecutor(String serviceName) {
        ComponentManager.unregisterComponent(components, serviceName);
    }

}
