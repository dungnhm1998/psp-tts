package asia.leadsgen.psp.obj;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ShopifySyncedProductObj {
	
	private String id;
	private String bgpVariantId;
	private String baseId;
	private String sizeId;
	private String colorId;
	private Long imageId;
	private String mediaId;
	
	private String frontDesign;
	private String frontMockup;

	private String backDesign;
	private String backMockup;
	
	private String bgpProductId;
	
	private Long productRefId;
	private Long variantRefId;
	
	private String salePrice;
	private String currency;
	private int saleExpected;
	
	private String sku;
	private String skuRef;
	private String colorName;
	private String sizeName;
	private String colorValue;
}
