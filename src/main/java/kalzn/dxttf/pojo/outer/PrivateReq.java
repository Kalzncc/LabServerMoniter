package kalzn.dxttf.pojo.outer;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class PrivateReq {
    @NonNull private String authName;
    @NonNull private String authToken;

}
