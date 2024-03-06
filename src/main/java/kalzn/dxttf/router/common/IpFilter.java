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


    private void checkIp(String host) {
        String ipStr = host;
        if (host.charAt(0) == '[' && host.charAt(host.length() - 1) == ']')
            ipStr = host.substring(1, host.length() - 1);



        IpAddress ip = IpAddress.createIpFromString(ipStr);

        if (ip == null) {
            if (GlobalConfig.server.whiteIps.contains(ipStr)) {
                return;
            }
            FilterChain.reject(403, "Forbidden");
        }
        try {
            for (var whiteIpString : GlobalConfig.server.whiteIps) {
                if (IpAddress.isIp(whiteIpString) && whiteIpString.equals(ipStr)) {
                    return;
                } else if (IpAddress.isIpAndMask(whiteIpString) ) {
                    var whiteNetSegment = IpAddress.createIpAndMaskFromString(whiteIpString);

                    assert whiteNetSegment != null;
                    if (ip.inSuchNetSegment(whiteNetSegment)) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            throw new InternalServerErrorResponse();
        }
        FilterChain.reject(403, "Forbidden");
    }


    @Api(types = {"filter"}, priority = -10)
    public void whiteIpFilter(Context ctx) {
        checkIp(ctx.ip());
    }


    @Api(types = {"wsFilter"}, priority = -10)
    public void whiteIpWsFilter(WsConfig wsConfig) {
        wsConfig.onConnect(ws -> {
            checkIp(ws.host());
        });
    }
}
