package kalzn.dxttf.executor;

import java.io.InputStream;

public interface ScriptCallback {
    void callback(InputStream stdout, InputStream stderr);
}
