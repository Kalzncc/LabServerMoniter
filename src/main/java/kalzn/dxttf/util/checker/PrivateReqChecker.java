package kalzn.dxttf.util.checker;

import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.pojo.outer.PrivateReq;

public class PrivateReqChecker {
    public static boolean check(PrivateReq privateReq) {
        if (!privateReq.getAuthName().matches(GlobalConfig.auth.namePatten))
            return false;
        if (privateReq.getAuthToken().isEmpty())
            return false;
        return true;
    }
}
