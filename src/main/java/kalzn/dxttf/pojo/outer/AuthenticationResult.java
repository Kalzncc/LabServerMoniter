package kalzn.dxttf.pojo.outer;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AuthenticationResult {

    public final static int CONTINUE_PASS_COMMON_AUTH = -1;
    public final static int SUCCESS = 0;
    public final static int FAIL_USER_NOT_EXIST = 1;
    public final static int FAIL_WRONG_PASSWORD_OR_TOKEN = 2;
    public final static int FAIL_TOO_MANY_TRY = 3;
    public final static int FAIL_INTERVAL_TOO_SHORT = 4;
    public final static int FAIL_INVALID_IP = 5;
    public final static int FAIL_TOKEN_EXPIRE = 6;
    public final static int FAIL_USER_BANNED = 7;
    public final static int FAIL_AUTH_DATABASE_REJECT = 8;
    public final static int FAIL_SYSTEM_ERROR = 9;

    private String name;
    private Integer result;
    private Long loginTimestamp;
    private Boolean keepActive;
    private String token;
    private String ip;

}
