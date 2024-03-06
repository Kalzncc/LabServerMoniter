package kalzn.dxttf.pojo.outer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CudaDetailInfo {
    private int number;
    private String gpuType;
    private String gpuUuid;
    private int fan;
    private String temp;
    private int maxPower;
    private int power;
    private int totalMem;
    private int UsageMem;
    private int usage;
    private String competeInfo;

}
