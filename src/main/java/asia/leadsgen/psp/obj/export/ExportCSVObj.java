package asia.leadsgen.psp.obj.export;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class ExportCSVObj {

	//shopify columns
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

	//woocommerece columns
	private String iD;
	//	private String type;
	private String sKU;
	private String name;
	//	private String published;
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
	//	private String tags;
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
	private String wpcom_is_markdown;
	private String download1Name;
	private String download1URL;
	private String download2Name;
	private String download2URL;
	private String download3Name;
	private String download3URL;

	//
	String channel;
	String[] headerExport;
	String[] headerMapping;

	public ExportCSVObj() {
	}

	public ExportCSVObj(String channel) {
		this.channel = channel;
	}

	public ExportCSVObj convertObj() {
		switch (channel) {
			case "woocommerce":
				return new ExportWooCSVObj();
			case "shopify":
				return new ExportShopifyCSVObj();
		}
		return null;
	}

}
