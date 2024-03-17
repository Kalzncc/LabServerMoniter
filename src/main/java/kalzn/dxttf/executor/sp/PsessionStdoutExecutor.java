package kalzn.dxttf.executor.sp;

import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.executor.Executor;
@Component(name = "psession_stdout_executor", type = Component.EXECUTOR)
public class PsessionStdoutExecutor extends Executor {
    public PsessionStdoutExecutor() { /*For ComponentManager.*/ }
    public PsessionStdoutExecutor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        super(scriptPath, scriptType, withSuperUser);
    }
}
