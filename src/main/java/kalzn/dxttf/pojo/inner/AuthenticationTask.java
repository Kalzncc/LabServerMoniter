package kalzn.dxttf.pojo.inner;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AuthenticationTask {
    public static final int AUTHENTICATE_BY_PWD = 0;
    public static final int AUTHENTICATE_BY_TK  = 1;


    @NonNull private String name;
    private int authenticationType;
    private String password;
    private String ip;
    private String token;

}
