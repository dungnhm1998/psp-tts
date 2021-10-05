package asia.leadsgen.psp.error;

/**
 * Created by HungDX on 28-Apr-16.
 */
public class SystemError {

	/**
	 * 400
	 */
	public static final SystemError INVALID_HEADER_X_DATE = new SystemError("INVALID_HEADER_X_DATE",
			"Request header X_DATE not found.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_HEADER_X_EXPIRES = new SystemError("INVALID_HEADER_X_EXPIRES",
			"Request header X_EXPIRES not found.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_HEADER_X_AUTHORIZATION = new SystemError("INVALID_HEADER_X_AUTHORIZATION",
			"Request header X_AUTHORIZATION not found.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError UNSUPPORTED_CONTENT_TYPE = new SystemError("UNSUPPORTED_CONTENT_TYPE",
			"Unsupported request content type. Support application/json only", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_REQUEST_URI = new SystemError("INVALID_REQUEST_URI", "Invalid request uri",
			"", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_REQUEST_BODY = new SystemError("INVALID_REQUEST_BODY",
			"Invalid request body", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_NUMBER_FORMAT = new SystemError("INVALID_NUMBER_FORMAT",
			"Invalid number format", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError MALFORMED_URL_EXCEPTION = new SystemError("MALFORMED_URL_EXCEPTION",
			"Malformed url exception", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_REQUEST = new SystemError("INVALID_REQUEST", "Invalid request ", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_ACTION = new SystemError("INVALID_ACTION", "Invalid action ", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_CUSTOMER_EMAIL = new SystemError("INVALID_CUSTOMER_EMAIL",
			"Invalid customer email", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_JSON_FORMAT = new SystemError("INVALID_JSON_FORMAT", "Invalid JSON Format",
			"", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_PRODUCT = new SystemError("INVALID_PRODUCT", "Invalid product", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_PRODUCT_VARIANT = new SystemError("INVALID_PRODUCT_VARIANT",
			"Invalid product variant", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_PRODUCT_BASE = new SystemError("INVALID_PRODUCT_BASE",
			"Invalid product base", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_URL = new SystemError("INVALID_URL", "Invalid URL", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_DOMAIN = new SystemError("INVALID_DOMAIN", "Invalid Domain", "",
			"http://developer.30usd.com/errors/400.html");
	public static final SystemError INVALID_CATEGORY = new SystemError("INVALID_CATEGORY", "Invalid Category", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_URI = new SystemError("INVALID_URI", "Invalid URI", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError DUPLICATE_URI = new SystemError("DUPLICATE_URI", "Duplicate URI", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_DESIGN = new SystemError("INVALID_DESIGN", "Invalid design", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_BASE64_DATA = new SystemError("INVALID_BASE64_DATA", "Invalid Base64 data",
			"", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_KEY = new SystemError("INVALID_KEY", "Invalid key.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_TYPE = new SystemError("INVALID_TYPE", "Invalid type.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_TITLE = new SystemError("INVALID_TITLE", "Invalid title.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_PROMOTION_CODE = new SystemError("INVALID_PROMOTION_CODE",
			"Invalid promotion code", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError PROMOTION_CODE_IS_NOT_AVAILABLE = new SystemError("PROMOTION_CODE_IS_NOT_AVAILABLE",
			"Promotion code is not available!", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_FILE_TYPE = new SystemError("INVALID_FILE_TYPE", "Invalid file type.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_FILE_COLUMN_HEADER = new SystemError("INVALID_FILE_COLUMN_HEADER",
			"Invalid file column header.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError EMPTY_CUSTOMER_LIST = new SystemError("EMPTY_CUSTOMER_LIST",
			"Customer list is empty.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_MOCKUP = new SystemError("INVALID_MOCKUP",
			"Invalid mockup image or template", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_MOCKUP_ID = new SystemError("INVALID_MOCKUP_ID", "Invalid mockup id", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_MOCKUP_TYPE = new SystemError("INVALID_MOCKUP_TYPE", "Invalid mockup type.",
			"", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_MOCKUP_IMAGE = new SystemError("INVALID_MOCKUP_IMAGE",
			"Invalid mockup image.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_IMAGE_URL = new SystemError("INVALID_IMAGE_URL", "Invalid image url.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_MIME_TYPE = new SystemError("INVALID_MIME_TYPE", "Invalid mime type.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_MOCKUP_TEMPLATE = new SystemError("INVALID_MOCKUP_TEMPLATE",
			"Invalid mockup template", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_IMAGE_ENCODED_DATA = new SystemError("INVALID_IMAGE_ENCODED_DATA",
			"Invalid image encoded data.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_FILE_SIZE = new SystemError("INVALID_FILE_SIZE", "Invalid file size", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_FILE_NAME = new SystemError("INVALID_FILE_NAME", "Invalid file name", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_IMAGE_DIMENSION = new SystemError("INVALID_IMAGE_DIMENSION",
			"Invalid image dimension", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_CAMPAIGN = new SystemError("INVALID_CAMPAIGN", "Invalid campaign", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_PACKAGE = new SystemError("INVALID_PACKAGE", "Invalid package", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_DROPSHIP_STORE_ID = new SystemError("INVALID_DROPSHIP_STORE_ID",
			"Invalid dropship store's id", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_CAMPAIGN_DESCRIPTION = new SystemError("INVALID_CAMPAIGN_DESCRIPTION",
			"Invalid campaign description", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_STORE = new SystemError("INVALID_STORE", "Invalid store", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_ORDER = new SystemError("INVALID_ORDER", "Invalid order", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_ORDER_SHIPPING = new SystemError("INVALID_ORDER_SHIPPING",
			"Invalid order shipping info", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_PROMOTION = new SystemError("INVALID_PROMOTION", "Invalid Promotion", "",
			"http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_PAYMENT = new SystemError("INVALID_PAYMENT", "Invalid payment", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_PAYMENT_TOKEN = new SystemError("INVALID_PAYMENT_TOKEN",
			"Invalid payment token", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_PAYMENT_CURRENCY = new SystemError("INVALID_PAYMENT_CURRENCY",
			"Invalid payment currency", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_EMAIL_TEMPLATE = new SystemError("INVALID_EMAIL_TEMPLATE",
			"Invalid Email Template", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_DATE = new SystemError("INVALID_DATE", "Invalid Date", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_STORE_ID = new SystemError("INVALID_STORE_ID", "Invalid Store Id.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_WOO_CONFIG_KEY = new SystemError("INVALID_WOO_CONFIG_KEY",
			"Consumer key is invalid or Invalid signature - provided signature does not match.", "",
			"http://developer.30usd.com/errors/401.html");

	public static final SystemError DUPLICATE_REFERENCE_ORDER = new SystemError("DUPLICATE_REFERENCE_ORDER",
			"Please try again with an other reference orderId.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_FILE_ID = new SystemError("INVALID_FILE_ID", "Invalid file id", "",
			"http://developer.30usd.com/errors/401.html");

	/**
	 * 409
	 */
	public static final SystemError WOO_STORE_CONFLIC = new SystemError("WOO_STORE_CONFLIC",
			"This wooecommerce has already", "", "http://developer.30usd.com/errors/409.html");

	public static final SystemError INVALID_PAYMENT_METHOD = new SystemError("INVALID_PAYMENT_METHOD",
			"Invalid payment method.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError DUPLICATE_PAYMENT = new SystemError("DUPLICATE_PAYMENT",
			"Your payment was successful but it takes longer than usual to process. We'll notify you when the process finishes.",
			"", "http://developer.30usd.com/errors/400.html");

	public static final SystemError DUPLICATE_DROPSHIP_STORE = new SystemError("DUPLICATE_DROPSHIP_STORE",
			"Please try again with an other store's name.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError STORE_NAME_CAN_NOT_BE_EMPTY = new SystemError("STORE_NAME_CAN_NOT_BE_EMPTY",
			"Please enter store's name.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError DUPLICATE_DOMAIN = new SystemError("DUPLICATE_DOMAIN", "Duplicate domain.", "",
			"http://developer.30usd.com/errors/400.html");

//	public static final SystemError INVALID_REFUND_REQUEST1 = new SystemError("INVALID_REFUND_REQUEST",
//			"This order was fulfilled.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError CANT_REFUND_ORDER_READY_TO_PRINT = new SystemError("INVALID_REFUND_REQUEST",
			"You can not refund this order because it's now ready for printing.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_EMAIL_LIST_DELETE_REQUEST = new SystemError(
			"INVALID_EMAIL_LIST_DELETE_REQUEST", "There are one or more campaign references to this list.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_EMAIL_CAMP_DELETE_REQUEST = new SystemError(
			"INVALID_EMAIL_CAMP_DELETE_REQUEST", "You cannot delete this campaign.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_EMAIL_CAMP_ID = new SystemError("INVALID_EMAIL_CAMP_ID",
			"You provided an invalid ID for email campaign!", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_EMAIL_LIST_ID = new SystemError("INVALID_EMAIL_LIST_ID",
			"You provided an invalid ID for email list!", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_EMAIL_CAMP_UPLOAD_REQUEST = new SystemError(
			"INVALID_EMAIL_CAMP_UPLOAD_REQUEST", "This feature is exclusive to custom domain owners.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError QUOTA_EXCEEDED = new SystemError("QUOTA_EXCEEDED",
			"Your quota is exceeded, upgrade now or choose other email list.", "",
			"http://developer.30usd.com/errors/400.html");

//    export customer list	

	public static final SystemError INVALID_CUSTOMER_TYPE = new SystemError("INVALID_CUSTOMER_TYPE",
			"Invalid customer type.", "", "http://developer.30usd.com/errors/400.html");

	// end export customer list

	public static final SystemError INVALID_FULFILLMENT_IDS = new SystemError("INVALID_FULFILLMENT_IDS",
			"Invalid Fulfillment Ids.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError BALANCE_IS_NOT_ENOUGH = new SystemError("BALANCE_IS_NOT_ENOUGH",
			"Your account balance is insufficient to complete the transaction", "", "http://developer.30usd.com/errors/400.html");

	/**
	 * 401
	 */

	public static final SystemError INVALID_SERVICE_REGION = new SystemError("INVALID_SERVICE_REGION",
			"Invalid service region", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_SERVICE_NAME = new SystemError("INVALID_SERVICE_NAME",
			"Invalid service name.", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_AUTHORIZATION_TYPE = new SystemError("INVALID_AUTHORIZATION_TYPE",
			"Invalid service authorization type.", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_AUTHORIZATION_ALGORITHM = new SystemError("INVALID_AUTHORIZATION_ALGORITHM",
			"Invalid service authorization algorithm.", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_CLIENT = new SystemError("INVALID_CLIENT", "Invalid Client.", "",
			"http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_SERVICE_SIGNATURE = new SystemError("INVALID_SERVICE_SIGNATURE",
			"Invalid service signature.", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_AUTH_TOKEN = new SystemError("INVALID_AUTH_TOKEN",
			"Invalid authorization token", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_SESSION = new SystemError("INVALID_SESSION", "Invalid session", "",
			"http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_USER = new SystemError("INVALID_USER", "Invalid user", "",
			"http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_PASSWORD = new SystemError("INVALID_PASSWORD", "Invalid password", "",
			"http://developer.30usd.com/errors/401.html");

	public static final SystemError PASSWORD_CANNOT_BE_EMPTY = new SystemError("PASSWORD_CANNOT_BE_EMPTY",
			"Password shouldn't be empty.", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_OLD_PASSWORD = new SystemError("INVALID_OLD_PASSWORD",
			"Invalid old password", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError AUTHORIZATION_FAILURE = new SystemError("AUTHORIZATION_FAILURE",
			"Authorization failure!", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError SESSION_EXPIRED = new SystemError("SESSION_EXPIRED",
			"Your session has been expired. Please login to continue!", "",
			"http://developer.30usd.com/errors/401.html");
	public static final SystemError TOKEN_EXPIRED = new SystemError("TOKEN_EXPIRED", "Your token has been expired!", "",
			"http://developer.30usd.com/errors/401.html");

	public static final SystemError LOGIN_REQUIRED = new SystemError("LOGIN_REQUIRED", "Login required!", "",
			"http://developer.30usd.com/errors/401.html");

	public static final SystemError DELETE_COOKIE_REQUIRED = new SystemError("DELETE_COOKIE_REQUIRED",
			"Delete cookie required!", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError PAYOUT_PENDING_PROCESSING = new SystemError("PAYOUT_PENDING_PROCESSING",
			"Payout is pending processing!", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_PAYMENT_INFO = new SystemError("INVALID_PAYMENT_INFO",
			"Please setting payment method!", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError EXCEEDED_LIMIT_ADD = new SystemError("EXCEEDED_LIMIT_ADD",
			"Exceeded limit add campaign upsell!", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_INPUT_DATA = new SystemError("INVALID_INPUT_DATA",
			"Input value must be greater than 0!", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError INVALID_IMAGE_DPI = new SystemError("INVALID_IMAGE_DPI",
			"Image dpi must be greater than 0!", "", "http://developer.30usd.com/errors/401.html");

	/**
	 * 403
	 */

	public static final SystemError OPERATION_NOT_PERMITTED = new SystemError("OPERATION_NOT_PERMITTED",
			"Operation not permitted!", "", "http://developer.30usd.com/errors/403.html");

	/**
	 * 404
	 */
	public static final SystemError DATA_NOT_FOUND = new SystemError("DATA_NOT_FOUND", "Data not found", "",
			"http://developer.30usd.com/errors/404.html");

	/**
	 * 406
	 */
	public static final SystemError DATA_NOT_ACCEPTABLE = new SystemError("DATA_NOT_ACCEPTABLE", "The brand already activated", "",
			"http://developer.30usd.com/errors/406.html");

	/**
	 * 409
	 */
	public static final SystemError PROMOTION_CODE_WAS_USED = new SystemError("PROMOTION_CODE_WAS_USED",
			"This promotion code has already been redeemed", "", "http://developer.30usd.com/errors/409.html");

	/**
	 * 500
	 */
	public static final SystemError INVALID_MAIL_TEMPLATE = new SystemError("INVALID_MAIL_TEMPLATE",
			"Invalid email template", "", "http://developer.30usd.com/errors/500.html");

	public static final SystemError OPERATION_EXPIRED = new SystemError("OPERATION_EXPIRED",
			"Request operation expired. Please create a new one.", "", "http://developer.30usd.com/errors/500.html");

	public static final SystemError INVALID_FRAUD_DATA = new SystemError("INVALID_FRAUD_DATA", "Invalid fraud data", "",
			"http://developer.30usd.com/errors/500.html");

	public static final SystemError PAYMENT_PROCESSING_ERROR = new SystemError("PAYMENT_PROCESSING_ERROR",
			"Payment processing error", "", "http://developer.30usd.com/errors/500.html");

	public static final SystemError INTERNAL_SERVER_ERROR = new SystemError("INTERNAL_SERVER_ERROR",
			"Oops !!! Something went wrong, please try again later!", "", "http://developer.30usd.com/errors/500.html");

	/** BASE **/
	public static final SystemError BASE_ID_CAN_NOT_BE_EMPTY = new SystemError("BASE_ID_CAN_NOT_BE_EMPTY",
			"base_id can not be empty.", "", "http://developer.30usd.com/errors/400.html");

	/** CAMPAIGN **/
	public static final SystemError CAMPAIGN_ID_CAN_NOT_BE_EMPTY = new SystemError("CAMPAIGN_ID_CAN_NOT_BE_EMPTY",
			"id can not be empty.", "", "http://developer.30usd.com/errors/400.html");

	/** PRODUCT **/
	public static final SystemError PRODUCT_ID_CAN_NOT_BE_EMPTY = new SystemError("PRODUCT_ID_CAN_NOT_BE_EMPTY",
			"id can not be empty.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError MISSING_DESIGNS = new SystemError("MISSING_DESIGNS", "missing designs.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError MISSING_MAIN_DESIGNS = new SystemError("MISSING_MAIN_DESIGNS",
			"missing main designs.", "", "http://developer.30usd.com/errors/400.html");

	/** VARIANT **/
	public static final SystemError VARIANT_ID_CAN_NOT_BE_EMPTY = new SystemError("VARIANT_ID_CAN_NOT_BE_EMPTY",
			"id can not be empty.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError VARIANT_ID_NOT_FOUND = new SystemError("VARIANT_ID_NOT_FOUND",
			"variant id not found.", "", "http://developer.30usd.com/errors/404.html");

	public static final SystemError SOCIAL_SOURCE_CAN_NOT_BE_EMPTY = new SystemError("SOCIAL_SOURCE_CAN_NOT_BE_EMPTY",
			"social source can not be empty.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError SOCIAL_LOGIN_DO_NOT_WORKING = new SystemError("SOCIAL_LOGIN_DO_NOT_WORKING",
			"social login do not working.", "", "http://developer.30usd.com/errors/400.html");

	/** PAYOUT **/
	public static final SystemError INVALID_INVOICE_NUMBER = new SystemError("INVALID_INVOICE_NUMBER",
			"Invalid invoice number.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError STOP_FULFILLMENT_3D = new SystemError("STOP_FULFILLMENT_3D",
			"Burgerprints is stopping fulfillment 3D products from Jan 28th to Feb 13th!", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_ORDER_ID = new SystemError("INVALID_ORDER_ID", "Invalid order id", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError PAYMENT_CREATE_INVOICE_FAILED = new SystemError("PAYMENT_CREATE_INVOICE_FAILED",
			"Payment create invoice failed.", "", "http://developer.30usd.com/errors/500.html");

	public static final SystemError PAYMENT_INVOICE_REFUND_FAILED = new SystemError("PAYMENT_INVOICE_REFUND_FAILED",
			"Payment invoice refund failed.", "", "http://developer.30usd.com/errors/500.html");

	public static final SystemError INVALID_AUTHORIZATION = new SystemError("INVALID_AUTHORIZATION",
			"Oops! Something went wrong. Please try again later.", "", "http://developer.30usd.com/errors/401.html");

	public static final SystemError CAN_NOT_END_ACTIVE_DROPSHIP_CAMPAIGN = new SystemError("INVALID_OPERATION",
			"Can not end or active dropship campaign.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError DUPLICATE_TAG = new SystemError("DUPLICATE_TAG",
			"Please try with another tag name!", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError CAN_NOT_ADD_NEW_BRAND = new SystemError("CAN_NOT_ADD_NEW_BRAND",
			"Create new brand failed. Please deactive your brand on this domain/store before add new one.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError CAN_NOT_ADD_NEW_BRAND_OPTIONS = new SystemError("CAN_NOT_ADD_NEW_BRAND_OPTIONS",
			"Minimum Order Quantity requirement unsatisfied.", "", "http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_SOURCE = new SystemError("INVALID_SOURCE", "Invalid Source.", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_MEDIA = new SystemError("INVALID_MEDIA", "Invalid media", "",
			"http://developer.30usd.com/errors/400.html");
	public static final SystemError INVALID_STOCK = new SystemError("INVALID_STOCK", "Invalid stock", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError NOT_FOUND_MERCHANT_ID = new SystemError("NOT_FOUND_MERCHANT_ID", "Not found merchant id", "",
			"http://developer.30usd.com/errors/400.html" );
	
	public static final SystemError NOT_MAPPED_ATTRIBUTES = new SystemError("NOT_MAPPED_ATTRIBUTES", "This store has not been mapped Attribues", "", 
			"http://developer.30usd.com/errors/404.html");
	
	public static final SystemError INVALID_BASE_ID = new SystemError("INVALID_BASE_ID", "Invalid base id", "",
			"http://developer.30usd.com/errors/400.html");
	
	public static final SystemError INVALID_COLOR_OR_SIZE = new SystemError("INVALID_COLOR_OR_SIZE", "Invalid color or size", "",
			"http://developer.30usd.com/errors/400.html");

	public static final SystemError INVALID_IOSS_NUMBER = new SystemError("INVALID_IOSS_NUMBER", "Invalid IOSS number", "",
			"http://developer.30usd.com/errors/400.html");

	public int getCode() {
		return code;
	}

	public String getReason() {
		return reason;
	}

	public String getName() {
		return name;
	}

	public String getMessage() {
		return message;
	}

	public String getDetails() {
		return details;
	}

	public String getInformationLink() {
		return informationLink;
	}

	public SystemError(String name, String message, String details, String informationLink) {
		this.name = name;
		this.message = message;
		this.details = details;
		this.informationLink = informationLink;
	}

	public SystemError(int code, String reason, String name, String message, String details, String informationLink) {
		this.code = code;
		this.reason = reason;
		this.name = name;
		this.message = message;
		this.details = details;
		this.informationLink = informationLink;
	}

	private int code;
	private String reason;
	private String name;
	private String message;
	private String details;
	private String informationLink;
}
