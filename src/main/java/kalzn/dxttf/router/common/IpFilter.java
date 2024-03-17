package kalzn.dxttf.router.common;

import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.websocket.WsConfig;
import kalzn.dxttf.config.GlobalConfig;
import kalzn.dxttf.config.annotation.Api;
import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.router.FilterChain;
import kalzn.dxttf.util.IpAddress;

@Component(type = Component.ROUTER)
public class IpFilter {


    private boolean checkIp(String host) {
        String ipStr = host;
        if (host.charAt(0) == '[' && host.charAt(host.length() - 1) == ']')
            ipStr = host.substring(1, host.length() - 1);



        IpAddress ip = IpAddress.createIpFromString(ipStr);

        if (ip == null) {
            return GlobalConfig.server.frontIps.contains(ipStr);

        }
        try {
            for (var whiteIpString : GlobalConfig.server.frontIps) {
                if (IpAddress.isIp(whiteIpString) && whiteIpString.equals(ipStr)) {
                    return true;
                } else if (IpAddress.isIpAndMask(whiteIpString) ) {
                    var whiteNetSegment = IpAddress.createIpAndMaskFromString(whiteIpString);

                    assert whiteNetSegment != null;
                    if (ip.inSuchNetSegment(whiteNetSegment)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }


    @Api(types = {"filter"}, priority = -10)
    public void whiteIpFilter(Context ctx) {
        try {
            if (!checkIp(ctx.ip())) {
                FilterChain.reject(403, "Forbidden");
            }
        } catch (Exception e) {
            FilterChain.reject(403, "Forbidden");
        }
    }


    @Api(types = {"wsFilter"}, priority = -10)
    public void whiteIpWsFilter(WsConfig wsConfig) {
        wsConfig.onConnect(ws -> {
            try {
                if (!checkIp(ws.host())) {
                    FilterChain.wsReject(403, "Forbidden", true);
                }
            } catch (Exception e) {
                FilterChain.wsReject(403, "Forbidden", true);
            }
        });
    }
}
