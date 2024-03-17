package kalzn.dxttf.service.support;


import javassist.bytecode.ByteArray;
import kalzn.dxttf.config.GlobalConfig;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public class Psession {
    private final static int NOT_OPEN = -1;
    public final static int RUNNING = 0;
    public final static int EXIT = 1;


    @Getter private int status = -1;

    @Getter  private int pid;
    private Process stdoutMonitor, fdoutMonitor, fderrMonitor;

    private ServiceCallback callback;



    public Psession(int pid,  Process stdout, Process fdout, Process fderr, ServiceCallback callback) {
        this.pid = pid;
        this.stdoutMonitor = stdout;
        this.fdoutMonitor = fdout;
        this.fderrMonitor = fderr;
        this.callback = callback;
    }
    private String getOutStr(BufferedReader reader, int outLen) throws IOException {
        StringBuilder builder = new StringBuilder();
        int readLine = outLen / 16 + (outLen % 16 == 0 ? 0 : 1);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int i = 0; i < readLine; i++) {
            String line = reader.readLine().strip();
            String[] hexString = line.substring(8, 59).strip().split(" +");
            for (var hex : hexString) {
                if (  hex.matches("[0-9a-f][0-9a-f]")  )
                    byteArrayOutputStream.write(Integer.parseInt(hex, 16));
            }
        }
        return byteArrayOutputStream.toString(GlobalConfig.server.encoding);
    }

    private void monitor() {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.stdoutMonitor.getErrorStream()));
                String line;
                while((line = reader.readLine()) != null) {
                    if (line.matches("write\\(1, [\\s\\S]*= [0-9]*")) {
                        String[] item = line.strip().split(" ");
                        int outLen = Integer.parseInt(item[item.length-1]);
                        String outStr = getOutStr(reader, outLen);

                        callback.push(System.currentTimeMillis() + "$stdout#std$" + outStr, null);
                    } else if (line.matches("write\\(2, [\\s\\S]*= [0-9]*")) {
                        String[] item = line.strip().split(" ");
                        int outLen = Integer.parseInt(item[item.length-1]);
                        String outStr = getOutStr(reader, outLen);

                        callback.push(System.currentTimeMillis() + "$stderr#std$" + outStr, null);
                    }

                }
                callback.push(null, new RuntimeException("Process Exited."));
            } catch (Exception e) {
                callback.push(null, e);
            }
        }).start();

        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.fdoutMonitor.getInputStream()));
                String line;
                while((line = reader.readLine()) != null) {
                    callback.push(System.currentTimeMillis() + "$stdout#fd$" + line, null);
                }
                callback.push(null, new RuntimeException("Process Exited."));
            } catch (Exception e) {
                callback.push(null, e);
            }
        }).start();

        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.fderrMonitor.getInputStream()));
                String line;
                while((line = reader.readLine()) != null) {
                    callback.push(System.currentTimeMillis() + "$stderr#fd$" + line, null);
                }
                callback.push(null, new RuntimeException("Process Exited."));
            } catch (Exception e) {
                callback.push(null, e);
            }
        }).start();

    }

    public void open() {
        if (status != NOT_OPEN) return;
        status = RUNNING;
        monitor();
    }

    public void stopMonitor() {
        status = EXIT;

        stdoutMonitor.descendants().forEach(ProcessHandle::destroy);
        stdoutMonitor.destroy();

        fdoutMonitor.descendants().forEach(ProcessHandle::destroy);
        fdoutMonitor.destroy();

        fderrMonitor.descendants().forEach(ProcessHandle::destroy);
        fderrMonitor.destroy();
    }
}
