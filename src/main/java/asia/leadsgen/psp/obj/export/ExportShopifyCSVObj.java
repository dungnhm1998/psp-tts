package asia.leadsgen.psp.obj.export;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringPool;
import asia.leadsgen.psp.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ExportShopifyCSVObj extends ExportCSVObj {
	static final String[] HEADERS_EXPORT_SHOPIFY = new String[]{"Handle", "Title", "Body (HTML)", "Vendor", "Type",
			"Tags", "Published", "Option1 Name", "Option1 Value", "Option2 Name", "Option2 Value", "Option3 Name",
			"Option3 Value", "Variant SKU", "Variant Grams", "Variant Inventory Tracker", "Variant Inventory Qty",
			"Variant Inventory Policy", "Variant Fulfillment Service", "Variant Price", "Variant Compare At Price",
			"Variant Requires Shipping", "Variant Taxable", "Variant Barcode", "Image Src", "Image Position",
			"Image Alt Text", "Gift Card", "SEO Title", "SEO Description", "Google Shopping / Google Product Category",
			"Google Shopping / Gender", "Google Shopping / Age Group", "Google Shopping / MPN",
			"Google Shopping / AdWords Grouping", "Google Shopping / AdWords Labels", "Google Shopping / Condition",
			"Google Shopping / Custom Product", "Google Shopping / Custom Label 0", "Google Shopping / Custom Label 1",
			"Google Shopping / Custom Label 2", "Google Shopping / Custom Label 3", "Google Shopping / Custom Label 4",
			"Variant Image", "Variant Weight Unit", "Variant Tax Code", "Cost per item", "Status"};

	static final String[] HEADER_MAPPING_SHOPIFY = new String[]{"handle", "title", "body", "vendor", "type", "tags",
			"published", "option1Name", "option1Value", "option2Name", "option2Value", "option3Name", "option3Value",
			"variantSKU", "variantGrams", "variantInventoryTracker", "variantInventoryQty", "variantInventoryPolicy",
			"variantFulfillmentService", "variantPrice", "variantCompareAtPrice", "variantRequiresShipping",
			"variantTaxable", "variantBarcode", "imageSrc", "imagePosition", "imageAltText", "giftCard", "sEOTitle",
			"sEODescription", "googleProductCategory", "googleGender", "googleAgeGroup", "googleMPN",
			"googleAdWordsGrouping", "googleAdWordsLabels", "googleCondition", "googleCustomProduct",
			"googleCustomLabel0", "googleCustomLabel1", "googleCustomLabel2", "googleCustomLabel3",
			"googleCustomLabel4", "variantImage", "variantWeightUnit", "variantTaxCode", "costPerItem", "status"};

	private String handle;
	private String title;
	private String body;
	private String vendor;
	private String type;
	private String tags;
	private String published;
	private String option1Name;
	private String option1Value;
	private String option2Name;
	private String option2Value;
	private String option3Name;
	private String option3Value;
	private String variantSKU;
	private String variantGrams;
	private String variantInventoryTracker;
	private String variantInventoryQty;
	private String variantInventoryPolicy;
	private String variantFulfillmentService;
	private String variantPrice;
	private String variantCompareAtPrice;
	private String variantRequiresShipping;
	private String variantTaxable;
	private String variantBarcode;
	private String imageSrc;
	private String imagePosition;
	private String imageAltText;
	private String giftCard;
	private String sEOTitle;
	private String sEODescription;
	private String googleProductCategory;
	private String googleGender;
	private String googleAgeGroup;
	private String googleMPN;
	private String googleAdWordsGrouping;
	private String googleAdWordsLabels;
	private String googleCondition;
	private String googleCustomProduct;
	private String googleCustomLabel0;
	private String googleCustomLabel1;
	private String googleCustomLabel2;
	private String googleCustomLabel3;
	private String googleCustomLabel4;
	private String variantImage;
	private String variantWeightUnit;
	private String variantTaxCode;
	private String costPerItem;
	private String status;

	public ExportShopifyCSVObj() {
		this.headerExport = HEADERS_EXPORT_SHOPIFY;
		this.headerMapping = HEADER_MAPPING_SHOPIFY;
	}

	public static ExportShopifyCSVObj formatFromMap(Map variantInfo, List<String> lstImageSrc, int rowNumber) {

		String handler = ParamUtil.getString(variantInfo, AppParams.ID);
		String title = ParamUtil.getString(variantInfo, AppParams.CAMPAIGN_TITLE) + StringPool.DASH + ParamUtil.getString(variantInfo, AppParams.NAME);
		String body = StringUtil.urlDecode(ParamUtil.getString(variantInfo, AppParams.DESC));


		String vendor = "";
		String type = ParamUtil.getString(variantInfo, AppParams.PRODUCT_TYPE);
		String published = AppParams.TRUE;

		String option1Name = "Name";
		String option1Value = ParamUtil.getString(variantInfo, AppParams.PRODUCT_NAME);
		String option2Name = "Color";
		String option2Value = ParamUtil.getString(variantInfo, AppParams.COLOR_NAME);
		String option3Name = "Size";
		String option3Value = ParamUtil.getString(variantInfo, AppParams.SIZE_NAME);

		String variantSKU = ParamUtil.getString(variantInfo, AppParams.VARIANT_ID) + StringPool.VERTICAL_BAR + ParamUtil.getString(variantInfo, AppParams.SIZE_ID);
		String variantGrams = "0";
		String variantInventoryPolicy = "deny";
		String variantFulfillmentService = "manual";
		String variantPrice = ParamUtil.getString(variantInfo, AppParams.PRICE);

		String variantCompareAtPrice = StringUtils.isEmpty(variantPrice) ? StringPool.BLANK:Double.valueOf(GetterUtil.format((Double.valueOf(variantPrice) * 120 / 100), 2)).toString();
		String variantRequiresShipping = AppParams.TRUE;
		String variantTaxable = AppParams.TRUE;
		String imageSrc = rowNumber > lstImageSrc.size() ? StringPool.BLANK:lstImageSrc.get(rowNumber - 1);
		String imagePosition = rowNumber > lstImageSrc.size() ? StringPool.BLANK:String.valueOf(rowNumber);
		String girfCard = AppParams.FALSE;

		int nDesignFront = ParamUtil.getInt(variantInfo, AppParams.N_DESIGN_FRONT);
		Map imgMap = ParamUtil.getMapData(variantInfo, AppParams.IMAGE);
		String imgFrontUrl = ParamUtil.getString(imgMap, AppParams.FRONT);
		String imgBackUrl = ParamUtil.getString(imgMap, AppParams.BACK);
		String variantImage = nDesignFront > 0 ? imgFrontUrl:imgBackUrl;
		String variantWeightUnit = "kg";
		String status = "active";

		//required column:
		ExportShopifyCSVObj row = new ExportShopifyCSVObj();
		row.setHandle(handler);
		row.setOption1Value(option1Value);
		row.setOption2Value(option2Value);
		row.setOption3Value(option3Value);
		row.setVariantSKU(variantSKU);
		row.setVariantGrams(variantGrams);
		row.setVariantInventoryPolicy(variantInventoryPolicy);
		row.setVariantFulfillmentService(variantFulfillmentService);
		row.setVariantPrice(variantPrice);
		row.setVariantCompareAtPrice(variantCompareAtPrice);
		row.setVariantRequiresShipping(variantRequiresShipping);
		row.setVariantTaxable(variantTaxable);
		row.setVariantImage(variantImage);
		row.setVariantWeightUnit(variantWeightUnit);
		row.setImageSrc(imageSrc);
		row.setImagePosition(imagePosition);
		row.setStatus(status);

		//first row for product
		if (rowNumber == 1) {
			row.setTitle(title);
			row.setBody(body);
			row.setVendor(vendor);
			row.setType(type);
			row.setPublished(published);
			row.setOption1Name(option1Name);
			row.setOption2Name(option2Name);
			row.setOption3Name(option3Name);
			row.setGiftCard(girfCard);
		}

		return row;
	}

	public static List<ExportShopifyCSVObj> processEachProduct(List<Map> variantInfos) {

		List<ExportShopifyCSVObj> lstRowForProduct = new ArrayList<>();

		Map<String, List<Map>> groupByColor = variantInfos.stream().collect(Collectors.groupingBy(e -> ParamUtil.getString(e, AppParams.COLOR_ID)));

		int rowNumber = 1;

		Map<String, Map> mapByColor = variantInfos.stream().collect(Collectors.toMap(e -> ParamUtil.getString(e, AppParams.COLOR_ID), e -> e, (e1, e2) -> e1));
		List<String> lstImageSrc = new ArrayList<String>();
		for (Map.Entry<String, Map> productColor : mapByColor.entrySet()) {

			int nDesignFront = ParamUtil.getInt(productColor.getValue(), AppParams.N_DESIGN_FRONT);

			Map imgMap = ParamUtil.getMapData(productColor.getValue(), AppParams.IMAGE);
			String imgFront = ParamUtil.getString(imgMap, AppParams.FRONT);
			String imgBack = ParamUtil.getString(imgMap, AppParams.BACK);

			if (nDesignFront > 0) {
				lstImageSrc.add(imgFront);
				lstImageSrc.add(imgBack);
			} else {
				lstImageSrc.add(imgBack);
				lstImageSrc.add(imgFront);
			}

		}

		for (Map.Entry<String, List<Map>> eachColor : groupByColor.entrySet()) {
			List<Map> sortBySize = eachColor.getValue().stream()
					.sorted(Comparator.comparing(e -> ParamUtil.getInt(e, AppParams.POSITION)))
					.collect(Collectors.toList());

			for (Map variantInfo : sortBySize) {
				ExportShopifyCSVObj row = ExportShopifyCSVObj.formatFromMap(variantInfo, lstImageSrc, rowNumber);
				lstRowForProduct.add(row);
				rowNumber++;
			}

		}

		return lstRowForProduct;
	}

}
