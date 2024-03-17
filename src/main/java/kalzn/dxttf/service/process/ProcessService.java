package kalzn.dxttf.service.process;

import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.executor.ExecutorManager;
import kalzn.dxttf.executor.sp.PsessionFderrExecutor;
import kalzn.dxttf.executor.sp.PsessionFdoutExecutor;
import kalzn.dxttf.executor.sp.PsessionStdoutExecutor;
import kalzn.dxttf.service.support.WsPushCallback;
import kalzn.dxttf.service.support.Psession;
import kalzn.dxttf.util.LogRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component(name = "process_service", type = Component.SERVICE)
public class ProcessService {

    private final Map<Integer, Psession> psessions = new ConcurrentHashMap<>();



    public void openPsession(int pid, WsPushCallback wsPushCallback) {
        if (psessions.containsKey(pid)) throw new RuntimeException("Duplicate open");
        var stdout = ExecutorManager.getExecutor(
                "psession_stdout_executor",
                PsessionStdoutExecutor.class,
                true);
        var fdout = ExecutorManager.getExecutor(
                "psession_fdout_executor",
                PsessionFdoutExecutor.class,
                true);
        var fderr = ExecutorManager.getExecutor(
                "psession_fderr_executor",
                PsessionFderrExecutor.class,
                true);
        try {
            assert stdout != null;
            assert fdout != null;
            assert fderr != null;

            stdout.execute(false, String.valueOf(pid));
            fdout.execute(false, String.valueOf(pid));
            fderr.execute(false, String.valueOf(pid));

            log.info(LogRecord.INFO_SUPER_PRIVILEGE_EXECUTE_RESULT(stdout.getCmd(), ""));
        } catch (Exception e) {
            log.warn(LogRecord.WARN_PROCESS_MONITOR_ERROR(e.getMessage()));
            throw new RuntimeException("Open Error");
        }

        var psession = new Psession(pid, stdout.getPs(), fdout.getPs(), fderr.getPs(), (obj, e) -> {
            if (e != null) {
                if (!"Process Exited.".equals(e.getMessage()))
                    log.warn(LogRecord.WARN_PROCESS_MONITOR_ERROR(e.getMessage()));
                try {
                    psessions.get(pid).stopMonitor();
                    psessions.remove(pid);
                } catch (Exception ignored) { return; }

                Map<String, Object> content = new HashMap<>();
                content.put("pid", pid);

                wsPushCallback.push(content, e);
            } else {
                Map<String, Object> content = new HashMap<>();
                content.put("pid", pid);
                content.put("out", obj);
                wsPushCallback.push(content, null);
            }
        });
        psessions.put(pid, psession);
        psession.open();
    }

    public void closePsession(int pid) {
        if (!psessions.containsKey(pid)) {
            return;
        }
        psessions.get(pid).stopMonitor();
        psessions.remove(pid);
    }




}
