package kalzn.dxttf.util;

import kalzn.dxttf.config.GlobalConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class HashUtil {



    private static final Random random = new Random();
    static {

        random.setSeed(GlobalConfig.server.randomSeed);
    }

    public static String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String generateRandomToken(int len) {
        StringBuilder token = new StringBuilder();
        while(token.length() < len) {
            long randomInt = random.nextLong(1, (long)0x3f3f3f3f * 0x3f3f3f3f);
            while(randomInt != 0 && token.length() < GlobalConfig.auth.tokenLength) {
                int element = (int)(randomInt % (26 + 26 + 10));
                randomInt /= (26 + 26 + 10);
                char ch;
                if (element < 26) {
                    ch = (char)(element + (int)'a');
                } else if (element < 26 + 26) {
                    ch = (char)(element - 26  + (int)'A');
                } else {
                    ch = (char)(element - 26 - 26 + (int)'0');
                }
                token.append(ch);
            }
        }
        return token.toString();
    }
}
