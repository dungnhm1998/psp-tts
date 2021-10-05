package asia.leadsgen.psp.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

/**
 * Created by Hung on 8/3/2015.
 */
public class AppConstants {

	public static final int POSITION_DEFAULT = 1;

	public static final int PAGE_DEFAULT = 1;

	public static final int PAGE_SIZE_DEFAULT = 10;

	public static final int DEFAULT_PRODUCT_SHIPPING_EXPECTED_ARRIVAL_MIN_DAYS = 5;

	public static final int DEFAULT_PRODUCT_SHIPPING_EXPECTED_ARRIVAL_MAX_DAYS = 10;

	public static final int DEFAULT_COPY_CAMPAIGN_TITLE_MAX_LENGTHS = 50;

	public static final String DEFAULT_TIME_ZONE = "GMT-05:00";

	public static final String DEFAULT_SHIPPING_FEE_VALUE = "4.99";

	public static final double DEFAULT_SHIPPING_FEE_VALUE_DOUBLE = 4.99d;
	public static final double DEFAULT_ADDING_SHIPPING_FEE_VALUE_DOUBLE = 1.99d;

	public static final String DEFAULT_ADDITIONAL_SHIPPING_FEE_VALUE = "2.00";

	public static final String DEFAULT_SAVE_CARD_FEE = "0.05";

	public static final String DEFAULT_COUNTRY_CODE = "US";

	public static final String DEFAULT_COUNTRY_NAME = "United States";

	public static final String DEFAULT_LANGUAGE_ID = "en-US";

	public static final String DEFAULT_DOMAIN_ID = "LLCiQXEY5I3wT1KF";

	public static final String DEFAULT_TIMEZONE = "UTC";

	public static final String SIGNATURE_HEADER_CLIENT_ID_EXTRACT_REGEX = "Credential=([^/|?]+)/";

	public static final String CHARSET_UTF8 = "UTF-8";

	public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

	public static final String CONTENT_TYPE_APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
	
	public static final String CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";

	public static final String DESIGN_TYPE_FRONT = "front";

	public static final String DESIGN_TYPE_BACK = "back";

	public static final String MOCKUP_TYPE_FRONT = "front";

	public static final String MOCKUP_TYPE_BACK = "back";

	public static final String URL_TYPE_CAMPAIGN = "campaign";

	public static final String URL_TYPE_STORE = "store";

	public static final String URL_TYPE_CATEGORY = "category";

	public static final String PAYMENT_TYPE_SALE = "sale";

	public static final String PAYMENT_TYPE_CUSTOM_DOMAIN = "custom_domain";

	public static final String PAYMENT_TYPE_EMAIL_QUOTA = "email_quota";

	public static final String PAYMENT_TYPE_SAVE_CARD = "save_card";

	public static final String PR_TYPE_FREESHIP = "freeship";

	public static final String PR_TYPE_DISCOUNT = "discount";

	public static final String PR_DISCOUNT_TYPE_FIX = "FIX";

	public static final String PR_DISCOUNT_TYPE_PERCENT = "PERCENT";

	public static final String DEFAULT_DATE_TIME_FORMAT_PATTERN = "yyyyMMdd\'T\'HHmmss\'Z\'";

	public static final String DATE_FORMAT_PATTERN_EEE_MMM_YYYY = "EEE, MMM d";

	public static final String DATE_FORMAT_PATTERN_YYYY_MM_DD = "yyyyMMdd";

	public static final String EMAIL_MARKETING_TYPE_NOTIFY = "notify";

	public static final String ORDER_CONFIRMATION_EMAIL_MARKETING_TYPE = "order_confirmation";

	public static final String TICKET_ADMIN_NOTIFY = "ticket_admin_notify";
	
	public static final String W8_ADMIN_NOTIFY = "w8_admin_notify";

	public static final String WITHDRAW_CONFIRMATION_EMAIL_TYPE = "withdraw_confirm";

	public static final NumberFormat DEFAULT_AMOUNT_FORMAT = new DecimalFormat("#0.00");

	public static final String YYMMDD = "yyMMdd";

	public static final String YYYYMMDD = "yyyyMMdd";
	public static final SimpleDateFormat DEFAULT_DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd\'T\'HHmmss\'Z\'");
	public static final String DEFAULT_DATE_TIME_FORMAT_VALUE = "yyyyMMdd\'T\'HHmmss\'Z\'";

	public static final long DEFAULT_BASE_ID_REDIS_EXPIRE_IN_DAYS = 10;

	public static final String NOT_APPLICABLE = "N/A";

	public static final String DEFAULT_CLIENT_ID = "PRO30USDCOM";

	public static final String PAYMENT_METHOD_STRIPE = "stripe";

	public static final String CUSTOM_DOMAIN_DEFAULT_CHARGE_AMOUNT = "custom.domain.default.charge.amount";
	public static final String CUSTOM_DOMAIN_MAXIMUM_ALLOWABLE_PRICE = "custom.domain.default.maximum.allowable.price";
	public static final String GODADDY_DOMAIN_PURCHASE_REQUEST_BODY_TEMPLATE = "godaddy.domain.purchase.request.body.template";
	public static final String GODADDY_DOMAIN_UPDATE_DNS_REQUEST_BODY_TEMPLATE = "godaddy.domain.update.dns.request.body.template";
	public static final String CUSTOM_DOMAIN_DEFAULT_CURRENCY_USD = "USD";
	public static final String DEFAULT_GODADDY_AGREEMENTKEYS = "DNRA";
	public static final String GODADDY_DATE_TIME_FORMAT = "yyyy-MM-dd\'T\'hh:mm:ss\'Z\'";

	public static final String DEFAULT_TEE_BASE_GROUP_ID = "bokuCHNsIDVH3ubu";
	public static final String DEFAULT_CANVAS_BASE_GROUP_ID = "xi96DPRhJKosNBnA";
	public static final String DEFAULT_METAL_CANVAS_BASE_GROUP_ID = "N3ACmzTczz3zIive";
	public static final String DEFAULT_WOOD_CANVAS_BASE_GROUP_ID = "XM1sp8CRrxUF59Tc";

	public static final String DEFAULT_REFERRAL_PROFIT_PER_UNIT = "default.referral.profit.per.unit";

	public static final String DEFAULT_UPSELL_POST_SALE_POPUP = "post-sale-popup";

	public static final String SHOPIFY = "shopify";
	public static final String WOOCOMMERCE = "woocommerce";
	public static final String ETSY = "etsy";
	public static final String SHOPIFY_DOMAIN = "myshopify.com";
	public static final int SHOPIFY_MAX_VARIANTS = 100;
	public static final int NUMBER_SIZES = 8;
	public static final String SHOPIFY_AUTHENTICATION_URL = "myshopify.com/admin/oauth/access_token";

        //public static final String WOO = "woo";
        
	public static final String X_CUSTOMER_EMAIL = "X-CUSTOMER-EMAIL";

	public static final String DF_DOMAIN_30USD = "30usd.com";
	public static final String DF_DOMAIN_BURGER = "burgerprints.com";

	public static final String DEFAULT_INVOICE_TEMPLATE_CODE = "pdf_template_invoice";
	public static final String DEFAULT_DATE_FORMAT_DD_MM_YY = "dd/MM/yyyy";
	public static final String DEFAULT_CURRENTCY_PATTERN = "###,##0.00";

	public static final String DEFAULT_WIRE_TRANSFER = "wire_transfer";

	public static final String DEFAULT_PAYPAL = "paypal";

	public static final String DEFAULT_PAYONEER = "payoneer";
	
	public static final String SHOPBASE = "shopbase";
	public static final String SHOPBASE_DOMAIN = "onshopbase.com";
	public static final String SHOPBASE_AUTHENTICATION_URL = "onshopbase.com/admin/oauth/access_token.json";
	public static final String SHOPBASE_STORE_LOCATION = "https://%s/admin/locations.json";
	public static final String CREATE_WEBHOOK = "https://%s/admin/webhooks.json";

	public static final String PARTNER_NOTIFICATION_EMAIL = "partner_notification";
	
	
	public static final String UTF8_BOM_CHAR = "\ufeff";
}
