package kalzn.dxttf.pojo.outer;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HeartbeatInfo {
    private double cpuUsage;
    private long totalMemory;
    private long freeMemory;

    @Getter
    @Setter
    public static class ProcessInfo {
        private int pid;
        private String user;
        private double cpuUsage;
        private double memUsage;
        private long vsz;
        private long rss;
        private String tty;
        private String stat;
        private String start;
        private String time;
        private String command;
    }
    private List<ProcessInfo> runningProcess;

    @Getter
    @Setter
    public static class CudaInfo {
        private String driverVersion;
        private String cudaVersion;
        private String smiVersion;
        private int cudaCount;
        private List<CudaDetailInfo> cudaDetailInfos;
        private List<CudaProcessDetail> cudaProcessDetails;
    }

    private CudaInfo cudaInfo;
}
