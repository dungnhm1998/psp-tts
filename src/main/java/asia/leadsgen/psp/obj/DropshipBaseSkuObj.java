package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DropshipBaseSkuObj implements Serializable {
	private static final long serialVersionUID = -2983600627331475796L;
	private String id;
	private String baseId;
	private String baseName;
	private String sizeId;
	private String sizeName;
	private String colorId;
	private String colorName;
	private String colorValue;
	private String sku;
	private String desc;
	private Double price;
	private Double secondPrice;
	private String addPrice;
	private String includeShipping;
	private String designTemplate;
	private String designDpi;
	private String designDesc;
	
	private String baseShortCode;
	
	public static DropshipBaseSkuObj fromMap(Map<String, Object> input) {
		DropshipBaseSkuObj obj = new DropshipBaseSkuObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setBaseId(ParamUtil.getString(input, AppParams.S_BASE_ID));
		obj.setBaseName(ParamUtil.getString(input, AppParams.S_BASE_NAME));
		obj.setSizeId(ParamUtil.getString(input, AppParams.S_SIZE_ID));
		obj.setSizeName(ParamUtil.getString(input, AppParams.S_SIZE_NAME));
		obj.setColorId(ParamUtil.getString(input, AppParams.S_COLOR_ID));
		obj.setColorName(ParamUtil.getString(input, AppParams.S_COLOR_NAME));
		obj.setColorValue(ParamUtil.getString(input, AppParams.S_COLOR_VALUE));
		obj.setSku(ParamUtil.getString(input, AppParams.S_SKU));
		obj.setDesc(ParamUtil.getString(input, AppParams.S_DESC));
		obj.setPrice(ParamUtil.getDouble(input, AppParams.S_PRICE));
		obj.setSecondPrice(ParamUtil.getDouble(input, AppParams.S_2ND_PRICE));
		obj.setAddPrice(ParamUtil.getString(input, AppParams.S_ADD_PRICE));
		obj.setIncludeShipping(ParamUtil.getString(input, AppParams.N_INCLUDE_SHIPPING));
		obj.setDesignTemplate(ParamUtil.getString(input, AppParams.S_DESIGN_TEMPLATE));
		obj.setDesignDpi(ParamUtil.getString(input, AppParams.S_DESIGN_DPI));
		obj.setDesignDesc(ParamUtil.getString(input, AppParams.S_DESIGN_DESC));
		
		obj.setBaseShortCode(ParamUtil.getString(input, AppParams.S_BASE_SHORT_CODE));
		
		
		return obj;
	}
}

