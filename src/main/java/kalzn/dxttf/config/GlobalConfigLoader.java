package kalzn.dxttf.config;

import kalzn.dxttf.config.annotation.Config;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.*;
import java.util.*;


public class GlobalConfigLoader {
    private static void mergeYaml(Map<String, Object> dstYaml, Map<String, Object> srcYaml) {
        for (var key : srcYaml.keySet()) {
            if (srcYaml.get(key) instanceof Map) {
                if (!dstYaml.containsKey(key) || !(dstYaml.get(key) instanceof Map)) {
                    dstYaml.put(key, new HashMap<String, Object>());
                }
                mergeYaml((Map<String, Object>)dstYaml.get(key), (Map<String, Object>)srcYaml.get(key));
            } else {
                dstYaml.put(key, srcYaml.get(key));
            }

        }
    }


    public static <T> T mapToEntity(Map<String, Object> map, Class<T> type) throws Exception {
        T instance = type.getConstructor().newInstance();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Field field = type.getDeclaredField(entry.getKey());
            field.set(instance, entry.getValue());

        }
        return instance;
    }

    private static void injectConfig(Class<?> configClass, Map<String, Object> configMap) {
        Field[] fields = configClass.getFields();
        for (var field : fields) {
            if (!Modifier.isStatic(field.getModifiers()))
                continue;


            String injectKey = field.getName();
            Config config = field.getAnnotation(Config.class);
            if (config != null && config.injectFlag())
                continue;
            if (config != null && !config.key().isEmpty()) {
                injectKey = config.key();
            }
            if (!configMap.containsKey(injectKey))
                continue;
            try {
                var fieldClass = field.getType();
                if (fieldClass.isAssignableFrom(List.class)) {
                    Type fieldType = field.getGenericType();
                    if (fieldType instanceof ParameterizedType parameterizedTypeFieldType) {
                        Type[] typeArguments = parameterizedTypeFieldType.getActualTypeArguments();
                        Type typeArgument = typeArguments[0];
                        Class<?> typeClass;
                        try {
                            typeClass = Class.forName(typeArgument.getTypeName());
                            if (!typeClass.isAssignableFrom(String.class) && !typeClass.isAssignableFrom(Number.class) && !typeClass.isAssignableFrom(Boolean.class)) {
                                List<Map> dynConfig = (List<Map>) configMap.get(injectKey);
                                List<Object> registerConfig = new ArrayList<>();
                                for (var configItemMap : dynConfig) {
                                    var configItem = mapToEntity(configItemMap, typeClass);
                                    registerConfig.add(configItem);
                                }
                                field.set(null, registerConfig);
                                continue;
                            }
                        } catch (Exception e) {
                            continue;
                        }

                    }
                }
                field.set(null, configMap.get(injectKey));
            } catch (IllegalAccessException exception) {
                throw new RuntimeException("Inject Configuration Error in key :" + injectKey);
            }
        }
        Class<?>[] subConfig = configClass.getClasses();
        for (var sub : subConfig) {
            if (!Modifier.isStatic(sub.getModifiers()))
                continue;
            String injectKey = sub.getSimpleName();
            Config config = sub.getAnnotation(Config.class);
            if (config != null && config.injectFlag())
                continue;
            if (config != null && !config.key().isEmpty()) {
                injectKey = config.key();
            }
            if (!configMap.containsKey(injectKey))
                continue;
            injectConfig(sub, (Map<String, Object>)configMap.get(injectKey));
        }
    }
    public static void load(String externalConfigFile) throws FileNotFoundException {

        Map<String, Object> configMap = new Yaml().loadAs(
            GlobalConfigLoader.class.getClassLoader().getResourceAsStream("server.yaml"),
            Map.class
        );
        if (externalConfigFile != null) {
            Map<String, Object> externalconfigMap = null;
            try {
                externalconfigMap = new Yaml().loadAs(
                    new FileInputStream(new File(externalConfigFile)),
                    Map.class
                );
            } catch (FileNotFoundException exception) {
                throw new FileNotFoundException("External Configuration File Not Found : " + externalConfigFile);
            }

            mergeYaml(configMap, externalconfigMap);
        }

        injectConfig(GlobalConfig.class, configMap);
    }
}
