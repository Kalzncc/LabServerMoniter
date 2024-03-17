package kalzn.dxttf.pojo.inner;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
public class AuthenticationInfo {
    @NonNull private String name;
    @NonNull private String password;
    private int ban;
    private long lastAuthenticationTime;
    private long lastTryAuthenticationTime;
    private int tryCount;
    private Map<String, AuthenticationToken> tokens;
}
