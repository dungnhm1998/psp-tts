package asia.leadsgen.psp.obj.etsy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EtsyVariant {
    private String productId;
    private String variantId;
    private String colorName;
    private String sizeName;
    private String price;
    private String imgFront;
    private String imgBack;
    private String sizeId;
}
