package kalzn.dxttf.executor;

import kalzn.dxttf.config.annotation.Component;

@Component(name = "cuda_detail_executor", type = Component.EXECUTOR)
public class CudaDetailExecutor extends Executor{
    public CudaDetailExecutor() { /*For ComponentManager.*/ }
    public CudaDetailExecutor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        super(scriptPath, scriptType, withSuperUser);
    }

    @Override
    public String postProcess() {
        return super.postProcess().substring(1);
    }
}
