package kalzn.dxttf.util;

import com.google.gson.Gson;

import java.util.Map;

public class GsonUtil {
    public static Map<String, Object> toStringKeyMap(String jsonStr) {
        try {
            Map ss = new Gson().fromJson(jsonStr, Map.class);
            return ss;
        } catch (Exception e) {
            return null;
        }
    }
}
