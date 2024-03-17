package kalzn.dxttf.data.authfile;

import com.google.gson.Gson;
import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.inner.AuthenticationInfo;
import kalzn.dxttf.pojo.inner.AuthenticationTask;
import kalzn.dxttf.pojo.inner.AuthenticationToken;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
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
    private AuthenticationInfo getAuthInfoWithoutLock(String name) {
        if (!nameSecurityCheck(name)) {
            return null;
        }
        FileReader reader = null;
        AuthenticationInfo info;
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
    private List<String> getAllAuthInfoNamesWithoutLock() {
        File authDir = new File(authPath);
        File[] authFiles = authDir.listFiles();
        ArrayList<String> result = new ArrayList<>();
        assert authFiles != null;
        for (var file : authFiles) {
            if (file.isDirectory())
                continue;
            result.add(file.getName());
        }
        return result;
    }


    private boolean writeInfoWithoutLock(AuthenticationInfo info) {
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
        }
        return true;
    }


    public boolean updateAuthInfo(AuthenticationInfo info) {
        if (!updateInfoSecurityCheck(info)) return false;
        if (!GlobalConfig.auth.register) return false;

        // For safety reasons, update can't ban/unban user, can't change password, can't change token.
        lock.writeLock().lock();
        try {
            var oInfo = getAuthInfoWithoutLock(info.getName());
            if (oInfo == null) {
                return writeInfoWithoutLock(info);
            }
            if (oInfo.getBan() != info.getBan()) {
                return false;
            }
            if (!oInfo.getPassword().equals(info.getPassword())) {
                return false;
            }
            if (oInfo.getTokens() == null && info.getTokens() != null) {
                return false;
            }
            if (oInfo.getTokens() != null && info.getTokens() == null) {
                return false;
            }

            if (oInfo.getTokens()!= null && !oInfo.getTokens().equals(info.getTokens())) {
                return false;
            }
            return writeInfoWithoutLock(info);

        } finally {
            lock.writeLock().unlock();
        }


    }

    public boolean banUser(String name) {
        lock.writeLock().lock();
        try {
            AuthenticationInfo info = getAuthInfoWithoutLock(name);
            if (info == null) return false;
            info.setBan(1);
            return writeInfoWithoutLock(info);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean unbanUser(String name) {
        lock.writeLock().lock();
        try {
            AuthenticationInfo info = getAuthInfoWithoutLock(name);
            if (info == null) return false;
            info.setBan(0);
            return writeInfoWithoutLock(info);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean changePassword(String name, String oPwd, String nPwd) {
        lock.writeLock().lock();
        try {
            AuthenticationInfo info = getAuthInfoWithoutLock(name);
            if (info == null) return false;
            if (!info.getPassword().equals(oPwd))
                return false;
            info.setPassword(nPwd);
            return writeInfoWithoutLock(info);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean removeToken(String name, String token) {
        if (!nameSecurityCheck(name)) return false;
        lock.writeLock().lock();
        try {
            AuthenticationInfo info = getAuthInfoWithoutLock(name);
            if (info == null) return false;
            if (info.getTokens() == null) return false;
            info.getTokens().remove(token);
            return writeInfoWithoutLock(info);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean addToken(String name, AuthenticationToken token) {
        if (!nameSecurityCheck(name)) return false;
        lock.writeLock().lock();
        try {
            AuthenticationInfo info = getAuthInfoWithoutLock(name);
            if (info == null) return false;
            if (info.getTokens() == null) {
                info.setTokens(new HashMap<>());
            }
            info.getTokens().put(token.getToken(), token);
            return writeInfoWithoutLock(info);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean clearToken(String name) {
        if (!nameSecurityCheck(name)) return false;
        lock.writeLock().lock();
        try {
            AuthenticationInfo info = getAuthInfoWithoutLock(name);
            if (info == null) return false;
            info.setTokens(null);
            return writeInfoWithoutLock(info);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
