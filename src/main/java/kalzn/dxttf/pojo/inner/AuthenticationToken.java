package kalzn.dxttf.pojo.inner;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class AuthenticationToken {
    private String token;
    private String ip;
    private long timestamp;
    public AuthenticationToken(String token, String ip) {
        this.ip = ip;
        this.token = token;
        this.timestamp = System.currentTimeMillis();
    }
    public AuthenticationToken(String token, String ip, long timestamp) {
        this.ip = ip;
        this.token = token;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthenticationToken to) ) return false;
        return Objects.equals(token, to.getToken()) && Objects.equals(timestamp, to.getTimestamp()) && Objects.equals(ip, to.getIp());
    }

}
