package kalzn.dxttf.executor;

import kalzn.dxttf.config.annotation.Component;

import java.util.ArrayList;
import java.util.List;

@Component(name = "free_executor", type = Component.EXECUTOR)
public class FreeExecutor extends Executor{
    public FreeExecutor() { /*For ComponentManager.*/ }
    public FreeExecutor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        super(scriptPath, scriptType, withSuperUser);
    }
    public List<Long> getMemoryInfo() {
        String out = postProcess();
        String[] info = out.split("\n+")[1].split(" +");
        List<Long> res = new ArrayList<>();
        res.add(Long.parseLong(info[1]));
        res.add(Long.parseLong(info[3]));
        return res;
    }
}
