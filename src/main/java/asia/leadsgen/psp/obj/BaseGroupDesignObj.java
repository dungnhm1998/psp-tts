package asia.leadsgen.psp.obj;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BaseGroupDesignObj {
    private String groupName;
    private int total;
    private List<BaseUploading> patternUploading;
    private List<BaseUploading> normalUploading;
}
