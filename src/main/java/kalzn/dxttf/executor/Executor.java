package kalzn.dxttf.executor;

import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.GlobalSystemStatus;
import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class Executor {


    public static final int BASH     = 0;
    public static final int BAT      = 1;
    public static final int PYTHON   = 2;


    @Getter private String scriptPath;
    @Getter private int scriptType;
    @Getter private boolean withSuperUser;
    private ScriptCallback callback;


    @Getter protected String cmd;
    @Getter protected String[] args;
    protected InputStream stdout;
    protected InputStream stderr;

    public Executor() { /*For ComponentManager.*/ }
    public Executor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        this.scriptPath = scriptPath;
        this.scriptType = scriptType;
        this.withSuperUser = withSuperUser;
    }

    private String buildCmd() {
        StringBuilder cmdBuilder = new StringBuilder();
        switch (scriptType) {
            case BASH -> cmdBuilder.append(GlobalConfig.script.bash);
            case PYTHON -> cmdBuilder.append(GlobalConfig.script.python);
            case BAT -> cmdBuilder.append(GlobalConfig.script.bat);
        }
        Path scriptPath = Paths.get(GlobalConfig.script.scriptPath, this.scriptPath);
        cmdBuilder.append(" ").append(scriptPath);
        for (var arg : args) {
            cmdBuilder.append(" ").append(arg);
        }
        return cmdBuilder.toString();
    }

    public Executor setCallBack(ScriptCallback callback) {
        this.callback = callback;
        return this;
    }

    private boolean checkSuperUserEnable() {
        return GlobalSystemStatus.superUserProcess && GlobalConfig.script.superUserEnable;
    }
    private String switchExecutorUserInLinux(String cmd) {
        return "sudo -u " + GlobalConfig.script.executorUser + " -c " + cmd;
    }

    public final void execute(boolean block, String ...args) throws IOException, RuntimeException {
        this.args = args;
        String cmd = buildCmd();
        if (!withSuperUser && checkSuperUserEnable()) {
            if (GlobalSystemStatus.os.contains("Linux")) {
                cmd = switchExecutorUserInLinux(cmd);
            } else {
                // TODO
            }
        } else if (withSuperUser && !checkSuperUserEnable()) {
            throw new RuntimeException("Executor permission denied.");
        }
        this.cmd = cmd;
        if (block) {
            Process ps = Runtime.getRuntime().exec(cmd);
            this.stdout = ps.getInputStream();
            this.stderr = ps.getErrorStream();
        } else {
            String finalCmd = cmd;
            new Thread(() -> {
                Exception err = null;
                Process ps = null;
                try {
                    ps = Runtime.getRuntime().exec(finalCmd);
                    this.stdout = ps.getInputStream();
                    this.stderr = ps.getErrorStream();
                }catch (Exception e) {
                    err = e;
                }
                if (callback != null && ps != null) {
                    callback.callback(ps.getInputStream(), ps.getErrorStream(), err);
                }
            }).start();
        }
    }
    public String postProcess() {
        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stderr));

        StringBuilder stderrStringBuilder = new StringBuilder();
        String bufferLine = "";
        try {
            while ((bufferLine = stderrReader.readLine()) != null) {
                stderrStringBuilder.append(bufferLine).append('\n');
            }
            if (!stderrStringBuilder.toString().isEmpty()) {
                throw new RuntimeException("Script execute fail : " + stderrStringBuilder);
            }
        } catch (IOException e) {
            throw new RuntimeException("Script executor can't read stderr.");
        }

        StringBuilder stdoutStringBuilder = new StringBuilder();
        try {
            while ((bufferLine = stdoutReader.readLine()) != null) {
                stdoutStringBuilder.append(bufferLine).append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException("Script executor can't read stdout.");
        }

        return stdoutStringBuilder.toString();
    }
}
