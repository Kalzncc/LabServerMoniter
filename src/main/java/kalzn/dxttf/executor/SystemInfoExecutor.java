package kalzn.dxttf.executor;

import kalzn.dxttf.config.annotation.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(name = "systeminfo_executor", type = Component.EXECUTOR)
public class SystemInfoExecutor extends Executor{
    public SystemInfoExecutor() { /*For ComponentManager.*/ }
    public SystemInfoExecutor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        super(scriptPath, scriptType, withSuperUser);
    }
}
