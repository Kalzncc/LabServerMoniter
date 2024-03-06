package kalzn;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class LogTest {
    private static final Logger logger = LoggerFactory.getLogger(LogTest.class);



    @Test
    void testLog() {
        logger.info("Logger Test - Log info produced by slf4j.");
    }


    @Test
    void testHeartbeat() {
        // 获取操作系统MXBean
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        // 获取系统CPU占用率
        double cpuUsage = osBean.getSystemLoadAverage();
        System.out.println("CPU占用率: " + cpuUsage + "%");

        // 获取系统内存占用率
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        double memoryUsage = ((double)(totalMemory - freeMemory) / totalMemory) * 100;
        System.out.println("内存占用率: " + memoryUsage + "%");
    }
}
