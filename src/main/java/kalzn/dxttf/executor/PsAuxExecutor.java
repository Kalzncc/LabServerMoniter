package kalzn.dxttf.executor;


import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.outer.HeartbeatInfo;

import java.util.ArrayList;
import java.util.List;

@Component(name = "psaux_executor", type = Component.EXECUTOR)
public class PsAuxExecutor extends Executor{


    public PsAuxExecutor() {/*For component manager.*/}
    public PsAuxExecutor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        super(scriptPath, scriptType, withSuperUser);
    }
    private HeartbeatInfo.ProcessInfo createProcessInfo(String[] items) {
        var info = new HeartbeatInfo.ProcessInfo();
        info.setUser(items[0]);
        info.setPid(Integer.parseInt(items[1]));
        info.setCpuUsage(Double.parseDouble(items[2]));
        info.setMemUsage(Double.parseDouble(items[3]));
        info.setVsz(Long.parseLong(items[4]));
        info.setRss(Long.parseLong(items[5]));
        info.setTty(items[6]);
        info.setStat(items[7]);
        info.setStart(items[8]);
        info.setTime(items[9]);
        StringBuilder cmdBuilder = new StringBuilder();
        for (int i = 10; i < items.length; i++) {
            cmdBuilder.append(i==10?"":" ").append(items[i]);
        }
        info.setCommand(cmdBuilder.toString());
        return info;
    }

    public List<HeartbeatInfo.ProcessInfo> getProcessList() throws RuntimeException {
        List<HeartbeatInfo.ProcessInfo> infos = new ArrayList<>();
        String out = postProcess();
        String[] processInfoStrings = out.split("\n");
        for (int i = 1; i < processInfoStrings.length; i++) {
            String[] items = processInfoStrings[i].split(" +");
            infos.add(createProcessInfo(items));
        }

        return infos;
    }
}
