package asia.leadsgen.psp.obj.etsy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EtsyListing {
    private String storeId;
    private String productId;
    private String taxonomyId;
    private String title;
}
