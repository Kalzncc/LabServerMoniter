package kalzn.dxttf.service.heartbeat;


import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.executor.*;
import kalzn.dxttf.pojo.outer.HeartbeatInfo;
import kalzn.dxttf.util.LogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

@Component(name = "heartbeat_service", type = Component.SERVICE)
public class HeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatService.class);

    private void getCpuAndMemInfo(HeartbeatInfo info) {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        info.setCpuUsage(Math.round(osBean.getSystemLoadAverage() * 10000) / 100.0);

        var freeExecutor = ExecutorManager.getExecutor("free_executor", FreeExecutor.class, true);
        try {
            assert freeExecutor != null;
            freeExecutor.execute(true);
            List<Long> res = freeExecutor.getMemoryInfo();
            info.setFreeMemory(res.get(1));
            info.setTotalMemory(res.get(0));
        } catch (Exception e) {
            logger.warn(String.format(LogRecord.WARN_SCRIPT_EXECUTE_FAIL, freeExecutor.getCmd()));
            info.setTotalMemory(0);
            info.setFreeMemory(0);
        }
    }
    private void getProcessList(HeartbeatInfo info) {
        var psAux = ExecutorManager.getExecutor("psaux_executor", PsAuxExecutor.class, true);
        try {
            assert psAux != null;
            psAux.execute(true);
        } catch (Exception e) {
            logger.warn(String.format(LogRecord.WARN_SCRIPT_EXECUTE_FAIL, psAux.getCmd()));
            info.setRunningProcess(null);
        }
        info.setRunningProcess(psAux.getProcessList());
    }
    private void getCudaInfo(HeartbeatInfo info) {
        var cudaExecutor = ExecutorManager.getExecutor("cuda_executor", CudaExecutor.class, true);
        try {
            assert cudaExecutor != null;
            cudaExecutor.execute(true);
        } catch (Exception e) {
            logger.warn(String.format(LogRecord.WARN_SCRIPT_EXECUTE_FAIL, cudaExecutor.getCmd()));
            HeartbeatInfo.CudaInfo cudaInfo = new HeartbeatInfo.CudaInfo();
            cudaInfo.setCudaCount(-1);
            info.setCudaInfo(cudaInfo);
        }
        info.setCudaInfo(cudaExecutor.getCudaInfo());
    }
    public HeartbeatInfo getHeartbeatInfo() {
        var info = new HeartbeatInfo();
        getCpuAndMemInfo(info);
        getProcessList(info);
        getCudaInfo(info);
        return info;
    }

    public String getCudaDetailInfo(int cudaNumber) {
        var cudaDetailExecutor =
                ExecutorManager.getExecutor("cuda_detail_executor", CudaDetailExecutor.class, true);
        try {
            assert cudaDetailExecutor != null;
            cudaDetailExecutor.execute(true, String.valueOf(cudaNumber));
        } catch (Exception e) {
            logger.warn(String.format(LogRecord.WARN_SCRIPT_EXECUTE_FAIL, cudaDetailExecutor.getCmd()));
            return null;
        }
        return cudaDetailExecutor.postProcess();

    }
}
