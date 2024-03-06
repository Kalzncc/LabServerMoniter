package kalzn.dxttf.pojo.inner;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AuthenticationInfo {
    @NonNull private String name;
    @NonNull private String password;
    private int ban;
    private String token;
    private String ip;
    private long tokenTimestamp;
    private long lastAuthenticationTime;
    private long lastTryAuthenticationTime;
    private int tryCount;
}
