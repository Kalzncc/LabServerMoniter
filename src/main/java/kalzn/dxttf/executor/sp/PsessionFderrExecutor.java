package kalzn.dxttf.executor.sp;

import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.executor.Executor;


@Component(name = "psession_fderr_executor", type = Component.EXECUTOR)
public class PsessionFderrExecutor extends Executor {
    public PsessionFderrExecutor() { /*For ComponentManager.*/ }
    public PsessionFderrExecutor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        super(scriptPath, scriptType, withSuperUser);
    }
}
