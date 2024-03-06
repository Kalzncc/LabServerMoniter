package kalzn.dxttf.pojo.outer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response {
    private int status;
    private String msg;
    private Object data;
}
