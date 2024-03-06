package kalzn.dxttf.data.authfile;

import com.google.gson.Gson;
import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.inner.AuthenticationInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;



@Component(name = {"auth_database"}, type = Component.DATABASE)
public class AuthenticationDatabase {

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private String authPath;








    public AuthenticationDatabase() {
        authPath = GlobalConfig.auth.authFile;
        File authDir = new File(authPath);
        if (!authDir.exists()) {
            authDir.mkdirs();
        }
    }


    // Authentication database mandatory security check
    private boolean nameSecurityCheck(String name) {
        return name.matches("[a-zA-Z0-9_]{1,64}");
    }
    private boolean updateInfoSecurityCheck(AuthenticationInfo info) {
        if (!info.getName().matches("[a-zA-Z0-9_]{1,64}"))
            return false;
        if (!info.getToken().matches("[0-9a-zA-Z]{1,1024}"))
            return false;
//        if (IpAddress.createIpFromString(info.getIp()) == null)
//            return false;
        if (info.getTryCount() < 0)
            return false;
        return true;
    }

    public AuthenticationInfo getAuthInfo(String name) {
        if (!nameSecurityCheck(name)) {
            return null;
        }
        FileReader reader = null;
        AuthenticationInfo info;
        lock.readLock().lock();
        try {
            reader = new FileReader(authPath + "/" + name);
            info = new Gson().fromJson(reader, AuthenticationInfo.class);
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException ignored) {}
            lock.readLock().unlock();
        }
        return info;
    }
    public List<String> getAllAuthInfoNames() {

        File authDir = new File(authPath);
        File[] authFiles = authDir.listFiles();
        ArrayList<String> result = new ArrayList<>();
        assert authFiles != null;
        lock.readLock().lock();
        try {
            for (var file : authFiles) {
                if (file.isDirectory())
                    continue;
                result.add(file.getName());
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }
    private boolean writeInfo(AuthenticationInfo info) {
        lock.writeLock().lock();
        FileWriter writer = null;
        try {
            String infoJson = new Gson().toJson(info);
            writer = new FileWriter(authPath + "/" + info.getName());
            writer.write(infoJson);

        } catch (IOException e) {
            return false;
        } finally {
            try {
                assert writer != null;
                writer.close();
            } catch (IOException ignored) {}
            lock.writeLock().unlock();
        }
        return true;
    }
    public boolean updateAuthInfo(AuthenticationInfo info) {
        if (!updateInfoSecurityCheck(info)) return false;
        if (!GlobalConfig.auth.register) return false;

        // For safety reasons, update can't ban/unban user, can't change password.
        var oInfo = getAuthInfo(info.getName());
        if (oInfo == null) {
            return writeInfo(info);
        }
        if (oInfo.getBan() != info.getBan()) {
            return false;
        }
        if (!oInfo.getPassword().equals(info.getPassword())) {
            return false;
        }

        return writeInfo(info);
    }

    public boolean banUser(String name) {
        AuthenticationInfo info = getAuthInfo(name);
        info.setBan(1);
        return writeInfo(info);
    }

    public boolean unbanUser(String name) {
        AuthenticationInfo info = getAuthInfo(name);
        info.setBan(0);
        return writeInfo(info);
    }

    public boolean changePassword(String name, String oPwd, String nPwd) {
        AuthenticationInfo info = getAuthInfo(name);
        if (!info.getPassword().equals(oPwd))
            return false;
        info.setPassword(nPwd);
        return writeInfo(info);
    }
}
