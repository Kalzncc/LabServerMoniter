package kalzn.dxttf.util;

import lombok.Getter;

@Getter
public class IpAddress {
    int[] seg = new int[8];
    String ipStr;
    int ver;
    int mask;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IpAddress ip)) return false;
        return ip.toString().equals(toString());
    }

    @Override
    public String toString() {
        if (ver == 4)
            return String.format("%d.%d.%d.%d%s%s",
                    seg[0], seg[1], seg[2], seg[3], mask==0?"":"/", mask==0?"":String.valueOf(mask));
        else
            return String.format("%d:%d:%d:%d:%d:%d:%d:%d%s%s",
                    seg[0], seg[1], seg[2], seg[3], seg[4], seg[5], seg[6], seg[7], mask==0?"":"/", mask==0?"":String.valueOf(mask));

    }



    public static boolean isIpAndMask(String ip) {
        return isIpAndMaskv4(ip) || isIpAndMaskv6(ip);
    }
    public static boolean isIp(String ip) {
        return isIpv4(ip) || isIpv6(ip);

    }

    public static boolean isIpAndMaskv4(String ip) {
        return ip.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}/[0-9]{1,2}");
    }
    public static boolean isIpv4(String ip) {
        return ip.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");

    }

    public static boolean isIpAndMaskv6(String ip) {
        return ip.matches("[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}/[0-9]{1,2}");
    }
    public static boolean isIpv6(String ip) {
        return ip.matches("[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}:[0-9]{1,3}");

    }

    public static IpAddress createIpFromString(String ip) {
        if (!isIp(ip)) return null;
        IpAddress result = new IpAddress();
        if (isIpv4(ip)) {
            String[] ipSegment = ip.split("\\.");
            for (int i = 0; i < 4; i++) result.seg[i] = Integer.parseInt(ipSegment[i]);
            if (result.seg[0] > 255 || result.seg[1] > 255 || result.seg[2] > 255 || result.seg[3] > 255) {
                return null;
            }
            result.ipStr = ip;
            result.mask = 0;
            result.ver = 4;
        } else {
            String[] ipSegment = ip.split(":");
            for (int i = 0; i < 8; i++) result.seg[i] = Integer.parseInt(ipSegment[i]);
            if (result.seg[0] > 255 || result.seg[1] > 255
                    || result.seg[2] > 255 || result.seg[3] > 255
                    || result.seg[4] > 255 || result.seg[5] > 255
                    || result.seg[6] > 255 || result.seg[7] > 255) {
                return null;
            }
            result.ipStr = ip;
            result.ver = 6;
        }
        return result;

    }
    public static IpAddress createIpAndMaskFromString(String ipAndMask) {
        if (!isIpAndMask(ipAndMask)) return null;
        String[] ipMaskSplit = ipAndMask.split("/");
        int mask = Integer.parseInt(ipMaskSplit[1]);
        IpAddress result = createIpFromString(ipMaskSplit[0]);
        if (result == null) return null;
        if (result.ver == 4 && mask >= 32) {
            return null;
        } else if (result.ver == 6 && mask >= 128) {
            return null;
        }
        result.mask = mask;
        return result;

    }

    public String getNetSegment() {
        return getNetSegment(this.mask);
    }
    public  String getNetSegment(int mask) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < (ver==4?4:8); i++) {
            int beginBit = i * 8, endBit = (i + 1) * 8;
            int segMask = 0;
            if (endBit <= mask) {
                segMask = (1 << 8) - 1;
            } else if (beginBit >= mask) {
                segMask = 0;
            } else {
                int hostBit = endBit - mask;
                segMask =  ((1 << 8) - 1) - ((1 << hostBit) - 1);
            }
            if (i != 0 && ver == 4)
                builder.append(".");
            if (i != 0 && ver == 6)
                builder.append(":");
            builder.append(String.valueOf(seg[i] & segMask));

        }
        builder.append("/").append(mask);
        return builder.toString();
    }

    public boolean inSuchNetSegment(IpAddress ip) {
        return getNetSegment(ip.getMask()).equals(ip.getNetSegment());
    }

}
