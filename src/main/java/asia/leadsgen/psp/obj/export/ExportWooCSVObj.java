package asia.leadsgen.psp.obj.export;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.Setter;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Setter
@Getter
public class ExportWooCSVObj extends ExportCSVObj {

	static final String[] HEADERS_EXPORT_WOO = new String[]{"ID", "Type", "SKU", "Name", "Published", "Is featured?",
			"Visibility in catalog", "Short description", "Description", "Date sale price starts",
			"Date sale price ends", "Tax status", "Tax class", "In stock?", "Low stock amount", "Stock",
			"Backorders allowed?", "Sold individually?", "Weight (lbs)", "Length (in)", "Width (in)", "Height (in)",
			"Allow customer reviews?", "Purchase note", "Sale price", "Regular price", "Categories",
			"Tags", "Shipping class", "Images", "Download limit", "Download expiry days", "Parent", "Grouped products",
			"Upsells", "Cross-sells", "External URL", "Button text", "Position", "Meta: _wpcom_is_markdown",
			"Attribute 1 name", "Attribute 1 value(s)", "Attribute 1 visible", "Attribute 1 global",
			"Attribute 2 name", "Attribute 2 value(s)", "Attribute 2 visible", "Attribute 3 global",
			"Attribute 3 name", "Attribute 3 value(s)", "Attribute 3 visible", "Attribute 3 global",
			"Download 1 name", "Download 1 URL", "Download 2 name", "Download 2 URL", "Download 3 name", "Download 3 URL"};
	static final String[] HEADER_MAPPING_WOO = new String[]{"iD", "type", "sKU", "name", "published", "isFeatured",
			"visibilityInCatalog", "shortDescription", "description", "dateSalePriceStarts", "dateSalePriceEnds",
			"taxStatus", "taxClass", "isInStock", "lowStockAmount", "stock", "isBackordersAllowed", "isSoldIndividually",
			"weight", "length", "width", "height", "isAllowCustomerReviews", "purchaseNote", "salePrice", "regularPrice",
			"categories", "tags", "shippingClass", "images", "downloadLimit", "downloadExpiryDays", "parent", "groupedProducts",
			"upsells", "crossSells", "externalURL", "buttonText", "position", "wpcom_is_markdown", "attribute1Name",
			"attribute1Value", "attribute1Visible", "attribute1Global", "attribute2Name", "attribute2Value", "attribute2Visible",
			"attribute2Global", "attribute3Name", "attribute3Value", "attribute3Visible", "attribute3Global",
			"download1Name", "download1URL", "download2Name", "download2URL", "download3Name", "download3URL"};

	private String iD;
	private String type;
	private String sKU;
	private String name;
	private String published;
	private String isFeatured;
	private String visibilityInCatalog;
	private String shortDescription;
	private String description;
	private String dateSalePriceStarts;
	private String dateSalePriceEnds;
	private String taxStatus;
	private String taxClass;
	private String isInStock;
	private String lowStockAmount;
	private String stock;
	private String isBackordersAllowed;
	private String isSoldIndividually;
	private String weight;
	private String length;
	private String width;
	private String height;
	private String isAllowCustomerReviews;
	private String purchaseNote;
	private String salePrice;
	private String regularPrice;
	private String categories;
	private String tags;
	private String shippingClass;
	private String images;
	private String downloadLimit;
	private String downloadExpiryDays;
	private String parent;
	private String groupedProducts;
	private String upsells;
	private String crossSells;
	private String externalURL;
	private String buttonText;
	private String position;
	private String wpcom_is_markdown;
	private String attribute1Name;
	private String attribute1Value;
	private String attribute1Visible;
	private String attribute1Global;
	private String attribute2Name;
	private String attribute2Value;
	private String attribute2Visible;
	private String attribute2Global;
	private String attribute3Name;
	private String attribute3Value;
	private String attribute3Visible;
	private String attribute3Global;
	private String download1Name;
	private String download1URL;
	private String download2Name;
	private String download2URL;
	private String download3Name;
	private String download3URL;

	public ExportWooCSVObj() {
		this.headerExport = HEADERS_EXPORT_WOO;
		this.headerMapping = HEADER_MAPPING_WOO;
	}

	public static ExportWooCSVObj formatVariationExport(Map variant, List<Map> listOption, String parentSKU) {
		String variantId = ParamUtil.getString(variant, AppParams.ID);
		String productSizeID = ParamUtil.getString(variant, AppParams.SIZE_ID);
		String productName = ParamUtil.getString(variant, AppParams.PRODUCT_NAME);
		String sizeName = ParamUtil.getString(variant, AppParams.SIZE_NAME);
		String colorName = ParamUtil.getString(variant, AppParams.COLOR_NAME);
		String price = ParamUtil.getString(variant, AppParams.PRICE);
		Map imgMap = ParamUtil.getMapData(variant, AppParams.IMAGE);

		String imgFrontUrl = ParamUtil.getString(imgMap, AppParams.FRONT);

		Map styleMap = listOption.stream().filter(o -> ParamUtil.getString(o, AppParams.S_BGP_OPTION).equals(AppParams.STYLE)).collect(Collectors.toList()).get(0);
		Map colorMap = listOption.stream().filter(o -> ParamUtil.getString(o, AppParams.S_BGP_OPTION).equals(AppParams.COLOR)).collect(Collectors.toList()).get(0);
		Map sizeMap = listOption.stream().filter(o -> ParamUtil.getString(o, AppParams.S_BGP_OPTION).equals(AppParams.SIZE)).collect(Collectors.toList()).get(0);

		Calendar calendar = Calendar.getInstance();

		ExportWooCSVObj woo = new ExportWooCSVObj();
		woo.setID("");
		woo.setType("variation");
		woo.setSKU(variantId + "|" + productSizeID + "|" + calendar.getTimeInMillis());
		woo.setName(productName);
		woo.setPublished("1");
		woo.setIsFeatured("0");
		woo.setVisibilityInCatalog("visible");
		woo.setShortDescription(productName + "|" + colorName + "|" + sizeName);
		woo.setDescription(productName + "|" + colorName + "|" + sizeName);
		woo.setDateSalePriceStarts("");
		woo.setDateSalePriceEnds("");
		woo.setTaxStatus("taxable");
		woo.setTaxClass("parent");
		woo.setIsInStock("1");
		woo.setLowStockAmount("");
		woo.setStock("");
		woo.setIsBackordersAllowed("0");
		woo.setIsSoldIndividually("0");
		woo.setWeight("");//-----------------------------------
		woo.setLength("");//-----------------------------------
		woo.setWidth("");//-----------------------------------
		woo.setHeight("");//-----------------------------------
		woo.setIsAllowCustomerReviews("0");
		woo.setPurchaseNote("");
		woo.setSalePrice(price);
		woo.setRegularPrice(price);
		woo.setCategories("");//-----------------------------------
		woo.setTags("");
		woo.setShippingClass("");
		woo.setImages(imgFrontUrl);//-----------------------------------
		woo.setDownloadLimit("");
		woo.setDownloadExpiryDays("");
		woo.setParent(parentSKU);
		woo.setGroupedProducts("");
		woo.setUpsells("");
		woo.setCrossSells("");
		woo.setExternalURL("");
		woo.setButtonText("");
		woo.setPosition("0");
		woo.setAttribute1Name(ParamUtil.getString(styleMap, AppParams.S_OPTION_NAME));
		woo.setAttribute1Value(productName);
		woo.setAttribute1Visible("");
		woo.setAttribute1Global("1");
		woo.setAttribute2Name(ParamUtil.getString(colorMap, AppParams.S_OPTION_NAME));
		woo.setAttribute2Value(colorName);
		woo.setAttribute2Visible("");
		woo.setAttribute3Global("1");
		woo.setAttribute3Name(ParamUtil.getString(sizeMap, AppParams.S_OPTION_NAME));
		woo.setAttribute3Value(sizeName);
		woo.setAttribute3Visible("");
		woo.setAttribute3Global("1");
		woo.setWpcom_is_markdown("");
		woo.setDownload1Name("");
		woo.setDownload1URL("");
		woo.setDownload2Name("");
		woo.setDownload2URL("");
		woo.setDownload3Name("");
		woo.setDownload3URL("");

		return woo;
	}

	public static ExportWooCSVObj formatExport(String campaignId, List<Map> listVariant, List<Map> listOption) {
		String campaignName = ParamUtil.getString(listVariant.get(0), AppParams.CAMPAIGN_TITLE);
		String campaignDesc = ParamUtil.getString(listVariant.get(0), AppParams.DESC);
		String productName = listVariant.stream().map(o -> ParamUtil.getString(o, AppParams.PRODUCT_NAME)).collect(Collectors.toSet()).stream().collect(Collectors.joining(","));
		String sizeName = listVariant.stream().map(o -> ParamUtil.getString(o, AppParams.SIZE_NAME)).collect(Collectors.toSet()).stream().collect(Collectors.joining(","));
		String colorName = listVariant.stream().map(o -> ParamUtil.getString(o, AppParams.COLOR_NAME)).collect(Collectors.toSet()).stream().collect(Collectors.joining(","));
		String imgFrontUrl = listVariant.stream().map(o -> {
			Map imgMap = ParamUtil.getMapData(o, AppParams.IMAGE);
			return ParamUtil.getString(imgMap, AppParams.FRONT);
		}).collect(Collectors.toSet()).stream().collect(Collectors.joining(","));

		Map styleMap = listOption.stream().filter(o -> ParamUtil.getString(o, AppParams.S_BGP_OPTION).equals(AppParams.STYLE)).collect(Collectors.toList()).get(0);
		Map colorMap = listOption.stream().filter(o -> ParamUtil.getString(o, AppParams.S_BGP_OPTION).equals(AppParams.COLOR)).collect(Collectors.toList()).get(0);
		Map sizeMap = listOption.stream().filter(o -> ParamUtil.getString(o, AppParams.S_BGP_OPTION).equals(AppParams.SIZE)).collect(Collectors.toList()).get(0);

		Calendar calendar = Calendar.getInstance();

		ExportWooCSVObj woo = new ExportWooCSVObj();
		woo.setID("");
		woo.setType("variable");
		woo.setSKU(campaignId + "|" + calendar.getTimeInMillis());
		woo.setName(campaignName);
		woo.setPublished("1");
		woo.setIsFeatured("0");
		woo.setVisibilityInCatalog("visible");
		woo.setShortDescription(campaignDesc);
		woo.setDescription(campaignDesc);
		woo.setDateSalePriceStarts("");
		woo.setDateSalePriceEnds("");
		woo.setTaxStatus("taxable");
		woo.setTaxClass("");
		woo.setIsInStock("1");
		woo.setLowStockAmount("");
		woo.setStock("");
		woo.setIsBackordersAllowed("0");
		woo.setIsSoldIndividually("0");
		woo.setWeight("");//-----------------------------------
		woo.setLength("");//-----------------------------------
		woo.setWidth("");//-----------------------------------
		woo.setHeight("");//-----------------------------------
		woo.setIsAllowCustomerReviews("1");
		woo.setPurchaseNote("");
		woo.setSalePrice("");
		woo.setRegularPrice("");
		woo.setCategories("Uncategorized");//-----------------------------------
		woo.setTags("");
		woo.setShippingClass("");
		woo.setImages(imgFrontUrl);//-----------------------------------
		woo.setDownloadLimit("");
		woo.setDownloadExpiryDays("");
		woo.setParent("");
		woo.setGroupedProducts("");
		woo.setUpsells("");
		woo.setCrossSells("");
		woo.setExternalURL("");
		woo.setButtonText("");
		woo.setPosition("0");
		woo.setAttribute1Name(ParamUtil.getString(styleMap, AppParams.S_OPTION_NAME));
		woo.setAttribute1Value(productName);
		woo.setAttribute1Visible("1");
		woo.setAttribute1Global("1");
		woo.setAttribute2Name(ParamUtil.getString(colorMap, AppParams.S_OPTION_NAME));
		woo.setAttribute2Value(colorName);
		woo.setAttribute2Visible("1");
		woo.setAttribute3Global("1");
		woo.setAttribute3Name(ParamUtil.getString(sizeMap, AppParams.S_OPTION_NAME));
		woo.setAttribute3Value(sizeName);
		woo.setAttribute3Visible("1");
		woo.setAttribute3Global("1");
		woo.setWpcom_is_markdown("");
		woo.setDownload1Name("");
		woo.setDownload1URL("");
		woo.setDownload2Name("");
		woo.setDownload2URL("");
		woo.setDownload3Name("");
		woo.setDownload3URL("");

		return woo;
	}
}
