package kalzn.dxttf.pojo.outer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CudaProcessDetail {
    private int cudaNumber;
    private String giId;
    private String ciId;
    private int pid;
    private String type;
    private String processName;
    private int usageMem;
}
