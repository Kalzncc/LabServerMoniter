package kalzn;

import io.javalin.Javalin;
import kalzn.dxttf.config.GlobalConfigLoader;
import kalzn.dxttf.data.DataManager;
import kalzn.dxttf.executor.CudaDetailExecutor;
import kalzn.dxttf.executor.CudaExecutor;
import kalzn.dxttf.executor.ExecutorManager;
import kalzn.dxttf.executor.PsAuxExecutor;
import kalzn.dxttf.router.RouterLoader;
import kalzn.dxttf.service.ServiceManager;
import kalzn.dxttf.util.LogRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;

public class TestJavaLin {

    private static final Logger logger = LoggerFactory.getLogger(TestJavaLin.class);
    @BeforeAll
    static void before() throws FileNotFoundException {
        GlobalConfigLoader.load(null);
        DataManager.registerDatabases();
        ServiceManager.registerServices();
        ExecutorManager.registerExecutors();
        try {
            RouterLoader.loadRouter();
        } catch (Exception e) {
            logger.error(String.format(LogRecord.ERROR_MOUNT_ROUTE_FAIL, e.getMessage()));
        }

    }

    @Test
    void testJavaLin() throws InterruptedException {
        try {
            GlobalConfigLoader.load("./config/config.yaml");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    @Test
    void testScript() throws Exception {



        PsAuxExecutor psaux = ExecutorManager.getExecutor("psaux_executor", PsAuxExecutor.class, true);

        psaux.execute(true);

        var processList =  psaux.getProcessList();


    }

    @Test
    void testCudaScript() throws Exception {
        CudaExecutor cudaExecutor = ExecutorManager.getExecutor("cuda_executor", CudaExecutor.class, true);

        cudaExecutor.execute(true);

        var cudaInfo = cudaExecutor.getCudaInfo();
    }

    @Test
    void testCudaDetailScript() throws Exception {
        CudaDetailExecutor cudaDetailExecutor =
                ExecutorManager.getExecutor("cuda_detail_executor", CudaDetailExecutor.class, true);

        cudaDetailExecutor.execute(true, "1");

        var cudaInfo = cudaDetailExecutor.postProcess();
    }

    @Test
    void testProcessScript() throws Exception {
        var psAux = ExecutorManager.getExecutor("psaux_executor", PsAuxExecutor.class, true);

        psAux.execute(true);

        psAux.getProcessList();
    }


    @Test
    void testWsFilter() throws Exception {
        var app = Javalin.create();
        app.wsBefore("/private/*", wsConfig -> {
           wsConfig.onConnect(ctx -> {
               System.out.println("private1");
           });
        });
        app.wsBefore("/private/ws/*", wsConfig -> {
            wsConfig.onConnect(ctx -> {
                System.out.println("private2");
            });
        });
        app.start(8084);
    }

    @Test
    void testInt() throws Exception {
        System.out.println(Integer.parseInt("3f", 16));
    }
}
