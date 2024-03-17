package kalzn.dxttf.executor.sp;

import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.executor.Executor;

@Component(name = "kill_executor", type = Component.EXECUTOR)
public class KillExecutor extends Executor {
    public KillExecutor() { /*For ComponentManager.*/ }
    public KillExecutor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        super(scriptPath, scriptType, withSuperUser);
    }
}
