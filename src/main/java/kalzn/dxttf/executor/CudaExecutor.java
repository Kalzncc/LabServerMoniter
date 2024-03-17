package kalzn.dxttf.executor;

import kalzn.dxttf.config.annotation.Component;
import kalzn.dxttf.pojo.outer.CudaDetailInfo;
import kalzn.dxttf.pojo.outer.CudaProcessDetail;
import kalzn.dxttf.pojo.outer.HeartbeatInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(name = "cuda_executor", type = Component.EXECUTOR)
public class CudaExecutor extends Executor{
    public CudaExecutor() { /*For ComponentManager.*/ }
    public CudaExecutor(String scriptPath, Integer scriptType, Boolean withSuperUser) {
        super(scriptPath, scriptType, withSuperUser);
    }

    public void getBaseInfo(HeartbeatInfo.CudaInfo info, String out) {
        String[] table = out.split("<sp>")[1].strip().split("\n");
        String[] baseInfoLine = table[2].split(" +");
        info.setSmiVersion(baseInfoLine[2]);
        info.setDriverVersion(baseInfoLine[5]);
        info.setCudaVersion(baseInfoLine[8]);
    }

    public void getCudaDetailInfo(HeartbeatInfo.CudaInfo info, String out) {
        String[] gpuAndTable = out.split("<sp>");
        String[] gpus = gpuAndTable[0].strip().split("\n");
        info.setCudaCount(gpus.length);
        List<CudaDetailInfo> detailInfos = new ArrayList<>();
        info.setCudaDetailInfos(detailInfos);
        for (int i = 0; i < gpus.length; i++) {
            detailInfos.add(new CudaDetailInfo());
        }
        Pattern type = Pattern.compile("GPU [0-9]+:[\\s\\S]+\\(");
        Pattern uuid = Pattern.compile("\\(UUID: [\\s\\S]+\\)");
        for (int i = 0; i < gpus.length; i++) {
            var gpu = gpus[i];
            Matcher matcher = type.matcher(gpu);
            String gpuType = "";
            if (matcher.find())
                gpuType = matcher.group();
            else
                throw new RuntimeException("Matcher Error");

            gpuType = gpuType.substring(gpuType.indexOf(':') + 2, gpuType.length()-2);

            Matcher matcher2 = uuid.matcher(gpu);
            String gpuUuid = "";
            if (matcher2.find())
                gpuUuid = matcher2.group();
            else
                throw new RuntimeException("Matcher Error");

            gpuUuid = gpuUuid.substring(gpuUuid.indexOf(':') + 2, gpuUuid.length()-1);
            info.getCudaDetailInfos().get(i).setNumber(i);
            info.getCudaDetailInfos().get(i).setGpuType(gpuType);
            info.getCudaDetailInfos().get(i).setGpuUuid(gpuUuid);
        }

        String[] table = gpuAndTable[1].strip().split("\n");

            for (int i = 9, j = 0; j < info.getCudaCount(); i += 4, j++) {
                String[] items = table[i].substring(1, table[i].length()-1).strip().split(" +");

                try {
                    CudaDetailInfo detail = info.getCudaDetailInfos().get(j);
                    detail.setFan(Integer.parseInt(items[0].substring(0, items[0].length() - 1)));
                    detail.setTemp(items[1]);
                    detail.setPower(Integer.parseInt(items[3].substring(0, items[3].length() - 1)));
                    detail.setMaxPower(Integer.parseInt(items[5].substring(0, items[5].length() - 1)));
                    detail.setUsageMem(Integer.parseInt(items[7].substring(0, items[7].length() - 3)));
                    detail.setTotalMem(Integer.parseInt(items[9].substring(0, items[9].length() - 3)));
                    detail.setUsage(Integer.parseInt(items[11].substring(0, items[11].length() - 1)));
                } catch (Exception e) {
                    System.out.println(table[i]);
                    for (var item : items) {
                        System.out.println(item + ", ");
                    }
                    throw e;
                }
            }




    }

    public void getProcessInfo(HeartbeatInfo.CudaInfo info, String out) {
        String[] gpuAndTable = out.split("<sp>");
        String[] table = gpuAndTable[1].strip().split("\n");
        int startLine = 0;
        for (;startLine < table.length;startLine++) {
            if (table[startLine].contains("Processes")) {
                break;
            }
        }
        List<CudaProcessDetail> details = new ArrayList<>();
        info.setCudaProcessDetails(details);
        for (int i = startLine + 4; i < table.length - 1; i++) {
            String[] items = table[i].split(" +");
            CudaProcessDetail detail = new CudaProcessDetail();
            details.add(detail);
            detail.setCudaNumber(Integer.parseInt(items[1]));
            detail.setGiId(items[2]);
            detail.setCiId(items[3]);
            detail.setPid(Integer.parseInt(items[4]));
            detail.setType(items[5]);
            detail.setProcessName(items[6]);
            detail.setUsageMem(Integer.parseInt(items[7].substring(0, items[7].length()-3)));
        }

    }



    public HeartbeatInfo.CudaInfo getCudaInfo() {

        var info = new HeartbeatInfo.CudaInfo();
        String out = postProcess();

        getBaseInfo(info, out);
        getCudaDetailInfo(info, out);
        getProcessInfo(info, out);

        return info;
    }
}
