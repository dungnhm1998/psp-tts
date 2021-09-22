package asia.leadsgen.psp.obj;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class BaseUploading {
    private String id;
    private String name;
    private String image;

    private String base_group_id;
    private Map base;

    @Override
    public boolean equals(Object obj) {
        BaseUploading base = (BaseUploading) obj;
        if (base.getId().equalsIgnoreCase(this.id)) {
            return true;
        }
        return false;
    }

}
