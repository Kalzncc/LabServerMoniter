package kalzn.dxttf.executor.sp;


import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.executor.Executor;

@Component(name = "psession_fdout_executor", type = Component.EXECUTOR)
public class PsessionFdoutExecutor extends Executor {
    public PsessionFdoutExecutor() { /*For ComponentManager.*/ }
    public PsessionFdoutExecutor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        super(scriptPath, scriptType, withSuperUser);
    }
}
