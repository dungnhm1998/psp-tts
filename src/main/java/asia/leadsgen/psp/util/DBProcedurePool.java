package asia.leadsgen.psp.util;

/**
 * Created by HungDX on 1/28/2015.
 */
public class DBProcedurePool {

	// temp
	public static final String DROPSHIP_GET_CAMP_INFO = "{call PKG_DROPSHIP.get_campaign_info(?,?,?,?)}";
	public static final String BASE_COLOR_GET_ALL = "{call PKG_BASE_COLOR.base_color_get_all(?,?,?)}";

//	public static final String DROPSHIP_STORE_GET = "{call pkg_dropship_store.get_stores(?,?,?,?,?,?)}";
//	public static final String DROPSHIP_STORE_GET = "{call PKG_SHOPIFY_APP_ORDER.get_stores(?,?,?,?,?,?,?)}";
	
	public static final String DROPSHIP_STORE_LOOKUP = "{call pkg_dropship_store.get_store(?,?,?,?)}";
	
	
	//
	public static final String CLIENT_GET = "{call PKG_CLIENT.client_get(?,?,?,?)}";

	public static final String USER_GET = "{call PKG_USER.user_get(?,?,?,?)}";
	public static final String USER_LOOKUP = "{call PKG_USER.user_lookup(?,?,?,?)}";
	public static final String USER_INSERT = "{call PKG_USER.user_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String USER_ASP_INSERT = "{call PKG_USER.user_asp_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String USER_UPDATE = "{call PKG_USER.user_update(?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String SESSION_GET = "{call PKG_SESSION.session_get(?,?,?,?)}";
	public static final String SESSION_FIND = "{call PKG_SESSION.session_find(?,?,?,?)}";
	public static final String SESSION_INSERT = "{call PKG_SESSION.session_insert(?,?,?,?,?,?)}";
	public static final String SESSION_UPDATE = "{call PKG_SESSION.session_update(?,?,?,?,?,?)}";

	public static final String COLOR_GET_DEFAULT = "{call PKG_BASE_COLOR.color_get_default(?,?,?)}";
	public static final String BASE_COLOR_LIST = "{call PKG_BASE_COLOR.base_color_list(?,?,?,?,?,?)}";
	public static final String BASE_COLOR_SEARCH = "{call PKG_BASE_COLOR.base_color_search(?,?,?,?,?,?,?)}";
	public static final String BASE_COLOR_INSERT = "{call PKG_BASE_COLOR.base_color_insert(?,?,?,?,?,?,?,?)}";
	public static final String GET_ALL_BASES_COLORS = "{call PKG_BASE_COLOR.get_all_bases_colors(?,?,?,?)}";

	public static final String BASE_GROUP_SEARCH = "{call PKG_BASE_GROUP.base_group_search(?,?,?,?,?,?,?,?)}";
	public static final String BASE_GROUP_INSERT = "{call PKG_BASE_GROUP.base_group_insert(?,?,?,?,?,?,?,?)}";

	public static final String BASE_TYPE_SEARCH = "{call PKG_BASE_TYPE.base_type_search(?,?,?,?,?,?,?)}";
	public static final String BASE_TYPE_INSERT = "{call PKG_BASE_TYPE.base_type_insert(?,?,?,?,?,?,?,?)}";

	public static final String BASE_SIZE_LIST = "{call PKG_BASE_SIZE.base_size_list(?,?,?,?,?,?)}";
	public static final String BASE_SIZE_SEARCH = "{call PKG_BASE_SIZE.base_size_search(?,?,?,?,?,?)}";
	public static final String BASE_SIZE_INSERT = "{call PKG_BASE_SIZE.base_size_insert(?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String PRODUCT_GET = "{call PKG_PRODUCT.product_get(?,?,?,?)}";
	public static final String PRODUCT_SEARCH = "{call PKG_PRODUCT.product_search(?,?,?,?,?)}";
	public static final String PRODUCT_INSERT = "{call PKG_PRODUCT.product_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PRODUCT_ALLOVER_INSERT = "{call PKG_PRODUCT.allover_product_insert(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PRODUCT_INSERT_AND_UPDATE_DESIGN = "{call PKG_PRODUCT.product_insert_and_update_design(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PRODUCT_UPDATE = "{call PKG_PRODUCT.product_update(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PRODUCT_UPDATE_BASE_COST = "{call PKG_PRODUCT.product_update_base_cost(?,?,?,?)}";
	public static final String PRODUCT_UDATE_ART = "{call PKG_PRODUCT.product_update_art(?,?,?,?,?,?,?,?)}";
	public static final String PRODUCT_DELETE = "{call PKG_PRODUCT.product_delete(?,?,?)}";
	public static final String PRODUCT_DELETE_2 = "{call PKG_PRODUCT.product_delete_2(?,?,?,?,?,?,?)}";
	public static final String PRODUCT_SET_TO_DEFAULT = "{call PKG_PRODUCT.SET_TO_DEFAULT(?,?,?,?,?,?,?)}";
	public static final String PRODUCT_GET_BASE_INFO_AND_PRICE = "{call PKG_PRODUCT.get_base_info_and_price(?,?,?,?,?,?)}";
	public static final String PRODUCT_PRICE_UPDATE_PRICE_AND_SALE_EXPECTED = "{call PKG_PRODUCT_PRICE.update_price_and_sale_expected(?,?,?,?,?,?)}";
	public static final String PRODUCT_PRICE_UPDATE_DEFAULT = "{call PKG_PRODUCT_PRICE.UPDATE_DEFAULT(?,?,?,?,?,?,?)}";
	public static final String PRODUCT_PRICE_GET_PRICES = "{call PKG_PRODUCT_PRICE.get_prices(?,?,?,?)}";
	public static final String PRODUCT_PRICE_GET_BY_CAMP_ID = "{call PKG_PRODUCT_PRICE.get_prices_by_camp_id(?,?,?,?)}";

	public static final String DESIGN_GET = "{call PKG_DESIGN.design_get(?,?,?,?)}";
	public static final String DESIGN_INSERT = "{call PKG_DESIGN.design_insert(?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String DESIGN_DELETE = "{call PKG_DESIGN.design_delete(?,?,?)}";
	public static final String ALL_OVER_INSERT_DESIGN = "{call PKG_DESIGN.allover_insert_design(?,?,?,?,?,?)}";
	public static final String ALL_OVER_UPDATE_DESIGN = "{call PKG_DESIGN.allover_update_design(?,?,?,?,?)}";

	public static final String PRODUCT_DESIGN_SEARCH = "{call PKG_PRODUCT_DESIGN.product_design_search(?,?,?,?,?,?)}";
	public static final String PRODUCT_DESIGN_INSERT = "{call PKG_PRODUCT_DESIGN.product_design_insert(?,?,?,?,?,?)}";
	public static final String PRODUCT_DESIGN_UPDATE = "{call PKG_PRODUCT_DESIGN.product_design_update(?,?,?,?)}";
	public static final String PRODUCT_DESIGN_SET_MAIN = "{call PKG_PRODUCT_DESIGN.product_design_set_main(?,?,?,?,?)}";
	public static final String PRODUCT_DESIGN_DELETE = "{call PKG_PRODUCT_DESIGN.product_design_delete(?,?,?,?)}";
	public static final String PRODUCT_DESIGNS_DELETE = "{call PKG_PRODUCT_DESIGN.product_designs_delete(?,?,?,?)}";
	public static final String PRODUCT_DESIGNS_BY_CAMP_ID = "{call PKG_PRODUCT_DESIGN.get_designs_by_campaign_id_new(?,?,?,?)}";
	public static final String UPDATE_DESIGNS = "{call PKG_PRODUCT_DESIGN.update_Designs(?,?,?,?)}";

	public static final String VARIANT_GET_WITH_SIZE_ID = "{call PKG_PRODUCT_VARIANT.variant_get_with_size_id(?,?,?,?,?)}";
	public static final String VARIANT_SEARCH = "{call PKG_PRODUCT_VARIANT.variant_search(?,?,?,?,?,?,?,?,?,?)}";
	public static final String VARIANT_INSERT = "{call PKG_PRODUCT_VARIANT.variant_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String VARIANT_UPDATE = "{call PKG_PRODUCT_VARIANT.variant_update(?,?,?,?,?,?)}";
	public static final String VARIANT_UPDATE_DEFAULT = "{call PKG_PRODUCT_VARIANT.variant_update_default(?,?,?,?,?)}";
	public static final String VARIANT_DELETE = "{call PKG_PRODUCT_VARIANT.variant_delete(?,?,?)}";
	public static final String PRODUCT_VARIANTS_DELETE = "{call PKG_PRODUCT_VARIANT.product_variants_delete(?,?,?)}";
	public static final String VARIANT_UPDATE_STATE = "{call PKG_PRODUCT_VARIANT.update_state(?,?,?,?,?)}";
	public static final String UPDATE_VARIANT_ORDER = "{call PKG_PRODUCT_VARIANT.update_variant_order(?,?,?,?,?)}";
	public static final String GET_DELETED_VARIANTS_BY_COLOR_IDS = "{call PKG_PRODUCT_VARIANT.get_deleted_variants_by_color_ids(?,?,?,?,?)}";

	public static final String MOCKUP_CATEGORIES_SEARCH = "{call PKG_MOCKUP.categories_search(?,?,?,?,?)}";
	public static final String MOCKUP_TYPES_SEARCH = "{call PKG_MOCKUP.types_search(?,?,?,?,?,?)}";
	public static final String MOCKUP_TEMPLATES_SEARCH = "{call PKG_MOCKUP.templates_search(?,?,?,?,?,?)}";
	public static final String TEMPLATE_GET = "{call PKG_MOCKUP.template_get(?,?,?,?)}";
	public static final String MOCKUP_GET = "{call PKG_MOCKUP.mockup_get(?,?,?,?)}";
	public static final String MOCKUP_SEARCH = "{call PKG_MOCKUP.mockup_search(?,?,?,?,?,?)}";
	public static final String MOCKUP_INSERT = "{call PKG_MOCKUP.mockup_insert(?,?,?,?,?,?,?)}";
	public static final String MOCKUP_DELETE = "{call PKG_MOCKUP.mockup_delete(?,?,?)}";
	public static final String VARIANT_MOCKUPS_DELETE = "{call PKG_MOCKUP.variant_mockups_delete(?,?,?)}";
	public static final String CAMP_URL_MOCKUP_SEARCH = "{call PKG_MOCKUP.get_campaign_url_by_mockup_id(?,?,?,?)}";
	public static final String MOCKUP_SEARCH_BY_CAMP_ID = "{call PKG_MOCKUP.get_mockup_by_campaign_id(?,?,?,?)}";
	public static final String MOCKUP_ALLOVER_UPDATE = "{call PKG_MOCKUP.allover_update(?,?,?,?,?)}";

	public static final String IMAGE_GET = "{call PKG_IMAGE.image_get(?,?,?,?)}";
	public static final String IMAGE_INSERT = "{call PKG_IMAGE.image_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String IMAGE_UPDATE = "{call PKG_IMAGE.image_update(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String IMAGE_DELETE = "{call PKG_IMAGE.image_delete(?,?,?)}";
	public static final String IMAGE_ALLOVER_UPDATE = "{call PKG_IMAGE.allover_update(?,?,?,?,?,?,?,?)}";

	public static final String CAMP_GET = "{call PKG_CAMPAIGN.camp_get(?,?,?,?)}";
	public static final String UNFINISHED_CAMP_GET = "{call PKG_CAMPAIGN.unfinished_camp_get(?,?,?,?)}";
	public static final String CAMP_INSERT = "{call PKG_CAMPAIGN.camp_insert(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String CAMP_ALLOVER_INSERT = "{call PKG_CAMPAIGN.camp_allover_insert(?,?,?,?,?,?)}";
	public static final String CAMP_UPDATE = "{call PKG_CAMPAIGN.camp_update(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String CAMP_UPDATE_INFO = "{call PKG_CAMPAIGN.CAMP_UPDATE_INFO(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String CAMP_UPDATE_STATE = "{call PKG_CAMPAIGN.camp_update_state(?,?,?,?,?)}";
	public static final String CAMP_GET_PRODUCT_IDS = "{call PKG_CAMPAIGN.camp_get_product_ids(?,?,?,?)}";
	public static final String CAMP_SEARCH = "{call PKG_CAMPAIGN.camp_search(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String CAMP_SEARCH_V2 = "{call PKG_CAMPAIGN.camp_search_v2(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String CAMP_DUPLICATE = "{call PKG_CAMPAIGN.camp_duplicate(?,?,?,?,?,?,?)}";
	public static final String CAMP_GET_COMBO = "{call PKG_CAMPAIGN.get_combo_campaign(?,?,?,?,?)}";
	public static final String CAMP_UPDATE_STATE_MANUALLY = "{call PKG_CAMPAIGN.camp_update_state_manually(?,?,?,?,?,?)}";
	public static final String CAMP_UPDATE_FAVORITE = "{call PKG_CAMPAIGN.camp_update_favorite(?,?,?,?,?)}";
	public static final String CAMP_UPDATE_ARCHIVE = "{call PKG_CAMPAIGN.camp_update_archive(?,?,?,?,?)}";
	public static final String CAMP_UPDATE_DEFAULT_IMAGES = "{call PKG_CAMPAIGN.CAMP_UPDATE_DEFAULT_IMAGES(?,?,?,?)}";
	public static final String CAMP_GET_INFO_FOR_EMAIL_MARKETING = "{call PKG_CAMPAIGN.get_info_for_email_marketing(?,?,?,?)}";
	public static final String CAMP_GET_4_BULK_DUPLICATE = "{call PKG_CAMP_DUPLICATE.get_4_bulk_duplicate(?,?,?,?,?,?)}";

	public static final String PREFERENCE_GET = "{call PKG_PREFERENCES.preference_get(?,?,?,?)}";
	public static final String PREFERENCE_SEARCH = "{call PKG_PREFERENCES.preference_search(?,?,?,?,?,?,?)}";
	public static final String PREFERENCE_INSERT = "{call PKG_PREFERENCES.preference_insert(?,?,?,?,?,?,?)}";
	public static final String PREFERENCE_UPDATE = "{call PKG_PREFERENCES.preference_update(?,?,?,?,?)}";

	public static final String CATEGORY_SEARCH = "{call PKG_CATEGORY.category_search(?,?,?,?,?,?,?,?)}";
	public static final String CATEGORY_AFF_SEARCH = "{call PKG_CATEGORY.category_aff_search(?,?,?,?,?,?,?,?)}";
	public static final String CATEGORY_INSERT = "{call PKG_CATEGORY.category_insert(?,?,?,?,?,?,?,?)}";

	public static final String CATEGORY_INSERT_BY_DOMAIN = "{call PKG_CATEGORY.insert_by_domain(?,?,?,?,?,?,?,?,?)}";
	public static final String CATEGORY_UPDATE_BY_DOMAIN = "{call PKG_CATEGORY.update_by_domain(?,?,?,?,?,?,?,?,?)}";
	public static final String CATEGORY_GET_LIST_BY_DOMAIN = "{call PKG_CATEGORY.get_list_by_domain_id(?,?,?,?,?,?,?,?)}";

	public static final String DOMAIN_SEARCH = "{call PKG_DOMAIN_URL.domain_search(?,?,?,?,?,?)}";
	public static final String DOMAIN_URL_SEARCH = "{call PKG_DOMAIN_URL.url_search(?,?,?,?,?,?,?,?)}";
	public static final String DOMAIN_URI_CHECK = "{call PKG_DOMAIN_URL.url_check(?,?,?,?,?,?,?,?)}";
	public static final String DOMAIN_URL_INSERT = "{call PKG_DOMAIN_URL.url_insert(?,?,?,?,?,?,?)}";
	public static final String DOMAIN_URL_UPDATE = "{call PKG_DOMAIN_URL.url_update(?,?,?,?,?)}";

	public static final String DOMAIN_CREATE = "{call PKG_DOMAIN.create_domain(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String DOMAIN_UPDATE = "{call PKG_DOMAIN.update_domain(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String DOMAIN_UPDATE_TRACKING = "{call PKG_DOMAIN.update_tracking(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String DOMAIN_UPDATE_CERTIFICATE = "{call PKG_DOMAIN.update_certificate(?,?,?,?,?,?,?,?)}";
	public static final String DOMAIN_REGISTER = "{call PKG_DOMAIN.register_domain(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String DOMAIN_UPDATE_STATE = "{call PKG_DOMAIN.update_state(?,?,?,?,?)}";
	public static final String DOMAIN_UPDATE_DNS_STATE = "{call PKG_DOMAIN.update_dns_state(?,?,?,?,?)}";
	public static final String DOMAIN_LIST = "{call PKG_DOMAIN.list_domains(?,?,?,?)}";
	public static final String DOMAIN_LOOKUP = "{call PKG_DOMAIN.lookup(?,?,?,?,?)}";
	public static final String DOMAIN_LIST_CUSTOM = "{call PKG_DOMAIN.get_custom_domains(?,?,?,?,?,?)}";
	public static final String DOMAIN_ADD_CODE = "{call PKG_DOMAIN.add_code_domain(?,?,?,?,?,?)}";
	public static final String DOMAIN_ACTIVE_SEARCH = "{call PKG_DOMAIN.active_search_bar(?,?,?,?,?)}";
	public static final String DOMAIN_UPDATE_INFO_TO_REGISTER = "{call PKG_DOMAIN.update_domain_info_to_register(?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String STORE_GET = "{call PKG_STORE.store_get(?,?,?,?)}";

	public static final String STORE_SEARCH = "{call PKG_STORE.store_search(?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String STORE_INSERT = "{call PKG_STORE.store_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String STORE_UPDATE = "{call PKG_STORE.store_update(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String STORE_FIND_BY_STORE_URL = "{call PKG_STORE.find_by_store_url(?,?,?,?,?,?,?,?)}";
	public static final String STORE_FIND_BY_STORE_ID = "{call PKG_STORE.find_by_store_id(?,?,?,?,?)}";
	public static final String STORE_SEARCH_CAMPAIGNS = "{call PKG_STORE.search_campaigns(?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String STORE_MASS_UPDATE_CAMPS = "{call PKG_STORE_CAMPAIGN.mass_update_camps(?,?,?,?,?,?,?)}";
	public static final String STORE_CAMP_SEARCH = "{call PKG_STORE_CAMPAIGN.store_camp_search(?,?,?,?,?,?,?)}";
	public static final String STORE_CAMP_INSERT = "{call PKG_STORE_CAMPAIGN.store_camp_insert(?,?,?,?,?)}";
	public static final String STORE_CAMP_UPDATE_STATE = "{call PKG_STORE_CAMPAIGN.store_camp_update_state(?,?,?,?,?)}";
	public static final String STORE_CAMPS_DELETE = "{call PKG_STORE_CAMPAIGN.store_camps_delete(?,?,?)}";
	public static final String STORE_CAMPS_DELETE_CAMP = "{call PKG_STORE_CAMPAIGN.store_camps_delete_with_camp(?,?,?)}";

	public static final String PR_TYPE_SEARCH = "{call PKG_PROMOTION.pr_type_search(?,?,?,?,?)}";
	public static final String PR_DISCOUNT_TYPE_SEARCH = "{call PKG_PROMOTION.pr_discount_type_search(?,?,?,?,?)}";
	public static final String PR_GET = "{call PKG_PROMOTION.pr_get(?,?,?,?)}";
	public static final String PR_SEARCH = "{call PKG_PROMOTION.pr_search(?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PR_INSERT = "{call PKG_PROMOTION.pr_insert(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PR_UPDATE = "{call PKG_PROMOTION.pr_update(?,?,?,?,?,?,?,?,?,?)}";
	public static final String PR_UPDATE_COUNT = "{call PKG_PROMOTION.pr_update_count(?,?,?,?,?)}";
	public static final String PR_DELETE = "{call PKG_PROMOTION.pr_delete(?,?,?)}";
	public static final String PR_CAMPAIGN_CHECK = "{call PKG_PROMOTION.pr_campaign_check(?,?,?,?,?,?)}";
	public static final String PR_STORE_CHECK = "{call PKG_PROMOTION.pr_store_check(?,?,?,?,?,?)}";
	public static final String PR_ACTIVE_FREE_SHIPPING = "{call PKG_PROMOTION.active_free_shipping(?,?,?,?,?,?,?,?)}";
	public static final String PR_TYPE_LOOKUP = "{call PKG_PROMOTION.pr_type_lookup(?,?,?,?)}";
	public static final String PR_UPDATE_FREE_SHIPPING = "{call PKG_PROMOTION.update_free_shipping(?,?,?,?,?,?)}";
	public static final String PR_ACTIVE_FREESHIP_SEARCH = "{call PKG_PROMOTION.active_freeship_search(?,?,?,?)}";
	public static final String PR_TOGGLE_VOLUME_DISCOUNT = "{call PKG_PROMOTION.toggle_volume_discount(?,?,?,?,?,?,?,?)}";
	public static final String PR_UPDATE_VOLUME_DISCOUNT = "{call PKG_PROMOTION.update_volume_discount(?,?,?,?,?,?)}";
	public static final String PR_GET_VOLUME_DISCOUNT = "{call PKG_PROMOTION.get_volume_discounts(?,?,?,?,?)}";
	public static final String PR_GET_VOLUME_DISCOUNT_BY_DOMAIN = "{call PKG_PROMOTION.get_volume_discounts_by_domain(?,?,?,?)}";
	public static final String PR_SEARCH_BY_CAMPAIGN_ID = "{call PKG_PROMOTION.pr_search_by_campaign_id(?,?,?,?,?,?,?,?,?)}";
	public static final String PR_SEARCH_BY_STORE_ID = "{call PKG_PROMOTION.pr_search_by_store_id(?,?,?,?,?,?,?,?,?)}";
	public static final String SHIPPING_DELETE_BY_ID_CSV_IMPORT = "{call PKG_SHIPPING.shipping_csv_delete_by_id(?,?,?)}";

	public static final String ORDER_OVERVIEW = "{call PKG_ORDER.order_overview(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String ORDER_GET = "{call PKG_ORDER.order_get(?,?,?,?)}";
	public static final String ORDER_CLONE = "{call PKG_ORDER.order_clone(?,?,?,?,?)}";
	public static final String ORDER_TRACKING = "{call PKG_ORDER.order_tracking(?,?,?,?)}";
	public static final String ORDER_INSERT = "{call PKG_ORDER.order_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String ORDER_UPDATE = "{call PKG_ORDER.order_update(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String ORDER_UPDATE_STATE = "{call PKG_ORDER.order_update_state(?,?,?,?,?)}";
	public static final String FIND_FIRST_CAMPAIGN_OF_ORDER = "{call PKG_ORDER.find_first_campaign_of_order(?,?,?,?)}";
	public static final String ORDER_UPDATE_MAIL_CAMP_ID = "{call PKG_ORDER.update_mail_camp_id(?,?,?,?,?)}";
	public static final String ORDER_FIND_BY_TRACKING_CODE = "{call PKG_ORDER.find_by_tracking_code(?,?,?,?)}";
	public static final String ORDER_SEARCH_SHIPPING_ITEM = "{call PKG_ORDER.search_shipping_item(?,?,?,?)}";
	public static final String ORDER_SEARCH_ITEMS = "{call PKG_ORDER.order_search_items(?,?,?,?)}";
	public static final String ORDER_SEARCH_ITEMS_BY_FF_DETAIL_IDS = "{call PKG_ORDER.search_items_by_ff_detail_ids(?,?,?,?)}";
	public static final String CHECK_IF_CREATED_LABEL = "{call PKG_ORDER.check_if_created_label(?,?,?,?,?)}";
	public static final String UPDATE_SHIPPING = "{call PKG_ORDER.update_shipping(?,?,?,?,?,?,?,?,?,?)}";

	public static final String ORDER_ADJUST_BY_STRIPE_CHARGE_KEY = "{call PKG_ORDER.order_adjust_by_stripe_charge_key(?,?,?,?,?)}";

	public static final String ORDER_PRD_GET = "{call PKG_ORDER_PRODUCT.order_prd_get(?,?,?,?)}";
	public static final String ORDER_PRD_SEARCH = "{call PKG_ORDER_PRODUCT.order_prd_search(?,?,?,?,?,?,?)}";
	public static final String SEARCH_REFUNDED_ITEMS = "{call PKG_ORDER_PRODUCT.search_refunded_items(?,?,?,?,?)}";
	public static final String ORDER_PRD_TRACKING = "{call PKG_ORDER_PRODUCT.order_prd_tracking(?,?,?,?,?)}";
	public static final String ORDER_PRD_INSERT = "{call PKG_ORDER_PRODUCT.order_prd_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String ORDER_PRD_UPDATE = "{call PKG_ORDER_PRODUCT.order_prd_update(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String ORDER_PRD_DELETE_ALL = "{call PKG_ORDER_PRODUCT.order_prd_delete_all(?,?,?)}";
	public static final String ORDER_PRD_ACTIVE_FREESHIP = "{call PKG_ORDER_PRODUCT.active_freeship(?,?,?,?)}";

	public static final String ORDER_PRD_GET_BY_ORDER_ID = "{call PKG_ORDER_PRODUCT.order_product_get_order_id(?,?,?,?,?)}";
	public static final String ORDER_PRD_REFUND_GET_BY_ORDER_ID = "{call PKG_ORDER_PRODUCT.order_product_refund_get_order_id(?,?,?,?,?)}";

	public static final String SHIPPING_GET = "{call PKG_SHIPPING.shipping_get(?,?,?,?)}";
	public static final String SHIPPING_INSERT = "{call PKG_SHIPPING.shipping_insert(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String SHIPPING_UPDATE = "{call PKG_SHIPPING.shipping_update(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String GET_SHIPPING_OWNER = "{call PKG_SHIPPING.get_shipping_owner(?,?,?)}";
	public static final String SHIPPING_GET_SHIPPING_PRODUCT = "{call PKG_SHIPPING.get_shipping_product(?,?,?)}";

	public static final String SHIPPING_FEE_SEARCH = "{call PKG_SHIPPING_FEE.shipping_fee_search(?,?,?,?,?,?,?)}";
	public static final String SHIPPING_GET_BY_ORDER_ID = "{call PKG_SHIPPING.shipping_get_by_order_id(?,?,?,?)}";
	public static final String SHIPPING_FEE_GET_BY_BASEID_AND_COUNTRY = "{call PKG_SHIPPING_FEE.get_by_baseid_and_country(?,?,?,?,?)}";

	public static final String PAYMENT_GET = "{call PKG_PAYMENT.payment_get(?,?,?,?)}";
	public static final String PAYMENT_GET_BY_ORDER_ID = "{call PKG_PAYMENT.payment_get_by_order_id(?,?,?,?)}";

	public static final String PAYMENT_SEARCH = "{call PKG_PAYMENT.payment_search(?,?,?,?,?,?,?,?)}";
	public static final String PAYMENT_INSERT = "{call PKG_PAYMENT.payment_insert(?,?,?,?,?,?,?,?)}";
	public static final String PAYMENT_PAYPAL_INSERT = "{call PKG_PAYMENT.payment_paypal_insert(?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PAYMENT_PAYPAL_SENT_INVOICE_UPDATE = "{call PKG_PAYMENT.payment_paypal_sent_invoice_update(?,?,?,?,?,?)}";
	public static final String PAYMENT_UPDATE = "{call PKG_PAYMENT.payment_update(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PAYMENT_SEARCH_BY_ORDER = "{call PKG_PAYMENT.payment_search_by_order(?,?,?,?,?,?)}";
	public static final String PAYMENT_REMOVE_BY_INVOICE_NUMBER = "{call PKG_PAYMENT.payment_paypal_remove_by_invoice_number(?,?,?)}";
	public static final String PAYMENT_UPDATE_BY_PAYPAL_SALE_ID = "{call PKG_PAYMENT.payment_state_update_by_paypal_sale_id(?,?,?,?,?,?,?)}";

	public static final String PAYMENT_UPDATE_BY_PAYPAL_SALE_ID_OTHER_DROPSHIP = "{call PKG_PAYMENT.payment_state_update_by_paypal_sale_id_other_dropship(?,?,?,?,?,?)}";

	public static final String INVOICE_UPDATE_STATE_BY_ID = "{call PKG_INVOICE.INVOICE_UPDATE_STATE_BY_ID(?,?,?,?)}";
	public static final String INVOICE_GET_ORDER_ID = "{call PKG_INVOICE.invoice_get_order_id(?,?,?,?)}";

	public static final String MAIL_TEMPLATE_SEARCH = "{call PKG_EMAIL_TEMPLATE.template_search(?,?,?,?,?,?)}";
	public static final String MAIL_TEMPLATE_GET = "{call PKG_EMAIL_TEMPLATE.template_get(?,?,?,?)}";
	public static final String MAIL_TEMPLATE_INSERT = "{call PKG_EMAIL_TEMPLATE.template_insert(?,?,?,?,?,?,?)}";

	public static final String MAIL_SEARCH = "{call PKG_EMAIL_MARKETING.mail_search(?,?,?,?,?,?)}";
	public static final String MAIL_INSERT = "{call PKG_EMAIL_MARKETING.mail_insert(?,?,?,?,?,?,?,?,?,?)}";
	public static final String MAIL_FIND_BY_MESSAGE_ID = "{call PKG_EMAIL_MARKETING.find_by_message_id(?,?,?,?)}";
	public static final String MAIL_UPDATE_SENT_FAIL_REASONS = "{call PKG_EMAIL_MARKETING.update_sent_fail_reasons(?,?,?,?,?,?)}";

	public static final String CAMP_RELAUNCH = "{call PKG_CAMPAIGN_RELAUNCH.camp_relaunch(?,?,?,?)}";
	public static final String CAMP_RELAUNCH_INSERT = "{call PKG_CAMPAIGN_RELAUNCH.camp_relaunch_insert(?,?,?,?,?)}";

	public static final String CUSTOMER_EMAIL_SEARCH = "{call PKG_CUSTOMER_EMAIL.customer_email_search(?,?,?,?,?,?,?)}";
	public static final String CUSTOMER_EMAIL_INSERT = "{call PKG_CUSTOMER_EMAIL.customer_email_insert(?,?,?,?,?,?,?,?,?)}";
	public static final String CUSTOMER_EMAIL_INSERT_LIST = "{call PKG_CUSTOMER_EMAIL.insert_list(?,?,?,?,?,?,?,?,?)}";
	public static final String CUSTOMER_EMAIL_INSERT_LIST_DATA = "{call PKG_CUSTOMER_EMAIL.insert_list_data(?,?,?,?,?,?,?)}";
	public static final String CUSTOMER_EMAIL_LOOKUP_LIST_DATA = "{call PKG_CUSTOMER_EMAIL.lookup_list_data(?,?,?,?,?)}";
	public static final String CUSTOMER_EMAIL_INSERT_LIST_FROM_CAMPS = "{call PKG_CUSTOMER_EMAIL.insert_list_from_camps(?,?,?,?,?,?,?,?)}";

	public static final String CUSTOMER_SEARCH = "{call PKG_CUSTOMER.customer_search(?,?,?,?)}";
	public static final String CUSTOMER_INSERT = "{call PKG_CUSTOMER.customer_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

//	public static final String GET_CUSTOMER_LIST = "{call PKG_CUSTOMER.get_customer_list(?,?,?,?,?)}";
	public static final String TRAFFIC_SEARCH = "{call PKG_TRAFFIC.traffic_search(?,?,?,?,?,?,?)}";
	public static final String TRAFFIC_INSERT = "{call PKG_TRAFFIC.traffic_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String AFFILIATE_PORTAL_GET_SALE_CONVERTION_RATE = "{call PKG_AFFILIATE_PORTAL.get_sale_convertion_rate(?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFFILIATE_PORTAL_GET_TOP_LOCATIONS = "{call PKG_AFFILIATE_PORTAL.get_top_traffic_locations(?,?,?,?,?,?,?,?)}";
	public static final String AFFILIATE_PORTAL_GET_TOP_CAMPAIGNS = "{call PKG_AFFILIATE_PORTAL.get_top_campaigns(?,?,?,?,?,?,?,?)}";
	public static final String AFFILIATE_PORTAL_GET_CAMPAIGNS_OVERVIEW = "{call PKG_AFFILIATE_PORTAL.get_campaigns_overview(?,?,?,?,?,?,?,?)}";

	public static final String SUB_AFF_SALE_CONVERTION_RATE = "{call PKG_SUB_AFFILIATE.get_sale_convertion_rate(?,?,?,?,?,?,?)}";
	public static final String SUB_AFF_TOP_LOCATIONS = "{call PKG_SUB_AFFILIATE.get_top_traffic_locations(?,?,?,?,?,?,?)}";
	public static final String SUB_AFF_TOP_CAMPAIGNS = "{call PKG_SUB_AFFILIATE.get_top_campaigns(?,?,?,?,?,?,?)}";
	public static final String SUB_AFF_CAMPS_OVERVIEW = "{call PKG_SUB_AFFILIATE.get_campaigns_overview(?,?,?,?,?,?,?)}";
	public static final String SUB_AFF_CAMPS_ANALYSIS = "{call PKG_SUB_AFFILIATE.camp_analysis(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String COMBINATION_WITHOUT_DATE_TIME = "{call PKG_AFF_RP_COMBINATION.report(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFF_TOTAL_FOR_DATETIME = "{call PKG_AFF_RP_COMBINATION.sum_datetime_total(?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFF_TOTAL_WITHOUT_DATETIME = "{call PKG_AFF_RP_COMBINATION.sum_total(?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFF_MONTHLY_REPORT = "{call PKG_AFF_RP_DAY.monthly_report(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFF_DAILY_REPORT = "{call PKG_AFF_RP_DAY.daily_report(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFF_DAYS_OF_WEEK_REPORT = "{call PKG_AFF_RP_DAY_PARTING.day_of_week_report(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFF_HOURSLY_REPORT = "{call PKG_AFF_RP_DAY_PARTING.hourly_report(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFF_PRODUCT_REPORT = "{call PKG_AFF_RP_PRODUCT.product_report(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFF_CAMP_ANALYSIS = "{call PKG_AFFILIATE_PORTAL.camp_analysis(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String AFF_CAMP_ANALYSIS_TOTAL = "{call PKG_AFFILIATE_PORTAL.camp_analysis_total(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String CHECK_EXIST_URL = "{call PKG_URL.CHECK_EXIST_URL(?,?)}";
	public static final String INSERT_CAMP_URL = "{call PKG_URL.insert_url(?,?,?,?,?,?)}";

	public static final String CAMP_COPY_CAMP_RECORD = "{call PKG_CAMP_DUPLICATE.copy_campaign(?,?,?,?,?,?,?,?)}";
	public static final String CAMP_COPY_PRODUCT_RECORD = "{call PKG_CAMP_DUPLICATE.copy_product(?,?,?,?,?)}";
	public static final String CAMP_COPY_IMAGE_RECORD = "{call PKG_CAMP_DUPLICATE.copy_image_of_product_design(?,?,?,?)}";
	public static final String CAMP_COPY_DESIGN_RECORD = "{call PKG_CAMP_DUPLICATE.copy_design_of_product_design(?,?,?,?,?)}";
	public static final String CAMP_COPY_PRODUCT_DESIGN_RECORD = "{call PKG_CAMP_DUPLICATE.copy_product_design(?,?,?,?,?,?)}";

	public static final String CAMP_GET_TOBE_COPIED_PRODUCT_IDS = "{call PKG_CAMP_DUPLICATE.get_tobe_copied_product_ids(?,?)}";
	public static final String CAMP_GET_TOBE_COPIED_PRODUCT_DESIGNS = "{call PKG_CAMP_DUPLICATE.get_tobe_copied_product_designs(?,?)}";
	public static final String CAMP_GET_GROUP_ID_OF_PRODUCT_DESIGN = "{call PKG_CAMP_DUPLICATE.get_group_id_of_product(?,?)}";

	public static final String PAYOUT_GET = "{call PKG_PAYOUT.payout_get(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PAYOUT_WITHDRAW = "{call PKG_PAYOUT.payout_withdraw(?,?,?,?,?,?,?)}";
	public static final String PAYOUT_CONFIRM = "{call PKG_PAYOUT.payout_confirm(?,?,?,?,?)}";
	public static final String PAYOUT_UNAPPROVED = "{call PKG_PAYOUT.payout_unupproved(?,?,?,?)}";
	public static final String PAYOUT_DELETE = "{call PKG_PAYOUT.payout_delete(?,?)}";

	public static final String REPORT_CAMP_INSERT = "{call PKG_CAMPAIGN.report_camp_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String GET_TICKET = "{call PKG_TICKET.get_ticket(?,?,?,?)}";
	public static final String TICKET_GET_BY_EMAIL = "{call PKG_TICKET.get_by_email(?,?,?,?)}";
	public static final String TICKET_GET_CATEGORIES = "{call PKG_TICKET.get_categories(?,?,?)}";
	public static final String TICKET_GET_ADMIN_EMAILS = "{call PKG_TICKET.get_admin_emails(?,?,?)}";
	public static final String TICKET_INSERT_MESSAGE = "{call PKG_TICKET.INSERT_MESSAGE(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String INSERT_TB_TICKET_DISPUTE = "{call PKG_TICKET.INSERT_TB_TICKET_DISPUTE(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String UPDATE_STATE_TB_TICKET_DISPUTE = "{call PKG_TICKET.UPDATE_STATE_TB_TICKET_DISPUTE(?,?,?,?,?)}";

	public static final String CATEGORY_SEARCH_V2 = "{call PKG_CATEGORY.category_search_v2(?,?,?,?,?,?,?,?,?)}";

	public static final String DOMAIN_SEARCH_V2 = "{call PKG_DOMAIN.domain_search_v2(?,?,?,?,?)}";
	public static final String DOMAIN_SALE_EVENT_CHANGE = "{call PKG_DOMAIN.update_sale_event(?,?,?,?,?,?,?)}";
	public static final String SEARCH_DOMAIN = "{call PKG_DOMAIN_URL.search(?,?,?,?)}";

	public static final String CAMP_GET_NEW_PRODUCTS = "{call PKG_CAMPAIGN.camp_get_new_products(?,?,?,?,?,?,?,?,?)}";

	public static final String STORE_SEARCH_V2 = "{call PKG_STORE.store_search_v2(?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String DOMAIN_TRACKING = "{call PKG_DOMAIN_URL.domain_tracking(?,?,?,?,?)}";

	public static final String ART_INSERT = "{call PKG_ART.art_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String FULL_COPY_CAMP = "{call PKG_CAMP_DUPLICATE.full_duplicate_campaign(?,?,?,?,?,?,?,?)}";
	public static final String GET_CATEGORY_NAME = "{call PKG_CATEGORY.get_category_name(?,?,?)}";

	public static final String ART_UPDATE = "{call PKG_ART.art_update(?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String ART_DELETE = "{call PKG_ART.art_delete(?,?,?)}";

	public static final String ART_CATEGORY_GET = "{call PKG_ART_CATEGORY.art_category_get(?,?,?,?)}";

	public static final String ART_SEARCH = "{call PKG_ART.art_search(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String ART_SEARCH_ALL = "{call PKG_ART.art_search_all(?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String ART_LOOKUP = "{call PKG_ART.art_lookup(?,?,?,?,?)}";

	public static final String CAMP_UPSELL_INSERT = "{call PKG_CAMPAIGN_UPSELL.upsell_insert(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String CAMP_UPSELL_GET = "{call PKG_CAMPAIGN_UPSELL.upsell_get(?,?,?,?,?,?,?,?)}";
	public static final String CAMP_UPSELL_DELETE = "{call PKG_CAMPAIGN_UPSELL.upsell_delete(?,?,?)}";
	public static final String CAMP_UPSELL_RANDOM = "{call PKG_CAMPAIGN_UPSELL.random_up_sell(?,?,?,?,?)}";

	public static final String CAMP_ART_SEARCH = "{call PKG_CAMPAIGN.camp_art_search(?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String ART_ORDER_PROFIT = "{call PKG_ART.art_get_order_profit(?,?,?,?,?,?)}";

	public static final String GET_TOP_ART = "{call PKG_ART.get_top_art(?,?,?,?,?,?,?,?,?,?)}";

	public static final String USER_FIND_BY_TOKEN = "{call PKG_USER.user_find_by_token(?,?,?,?)}";
	public static final String USER_GET_REFERRALS = "{call PKG_USER.user_get_referrals(?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String FULFILLMENT_SEARCH_BY_ORDER_ID = "{call PKG_FULFILLMENT.search_by_order_id(?,?,?,?)}";
	public static final String FULFILLMENT_SEARCH_FF_DETAIL = "{call PKG_FULFILLMENT.search_ff_detail_for_creating_label(?,?,?,?,?)}";

	public static final String FULFILLMENT_UPDATE_CREATING_LABEL_STATE = "{call PKG_FULFILLMENT.update_creating_label_state(?,?,?,?,?)}";
	public static final String FULFILLMENT_GET_CREATED_LABELS = "{call PKG_FULFILLMENT.get_created_labels(?,?,?,?,?)}";
	public static final String FULFILLMENT_SPLIT_PACKAGE = "{call PKG_FULFILLMENT_DETAIL.split_ff_detail_package(?,?,?,?,?)}";
	public static final String FULFILLMENT_UPDATE_SHIPPING = "{call PKG_FULFILLMENT_DETAIL.UPDATE_SHIPPING(?,?,?,?,?,?,?,?,?)}";

	public static final String CAMP_PATCH_DESIGN = "{call PKG_CAMPAIGN.camp_patch_url(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String CAMP_PATCH_UNCHECK_DESIGN = "{call PKG_CAMPAIGN.camp_patch_uncheck_design(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String VARIANT_GET_BY_CAMP_ID = "{call PKG_PRODUCT_VARIANT.variant_get_by_campaign(?,?,?,?,?)}";
	public static final String FILTER_VARIANT_COLORS = "{call PKG_PRODUCT_VARIANT.filter_variant_colors(?,?,?,?,?)}";
	public static final String VARIANT_ALLOVER_UPDATE = "{call PKG_PRODUCT_VARIANT.allover_update(?,?,?,?,?,?,?)}";

	/**
	 * * email campaigns **
	 */
	public static final String EMAIL_CAMP_LOOK_UP_EMAIL_LIST = "{call pkg_email_campaigns.look_up_email_list(?,?,?,?,?,?)}";
	public static final String EMAIL_CAMP_DELETE_EMAIL_LIST = "{call pkg_email_campaigns.delete_email_list(?,?,?,?,?)}";
	public static final String EMAIL_CAMP_DELETE = "{call pkg_email_campaigns.delete_email_camp(?,?,?,?,?)}";
	public static final String EMAIL_CAMP_SEARCH_EMAIL_LIST = "{call pkg_email_campaigns.search_email_list(?,?,?,?,?,?,?,?,?)}";
	public static final String EMAIL_CAMP_INSERT = "{call pkg_email_campaigns.insert_email_campaign(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String EMAIL_CAMP_UPDATE = "{call pkg_email_campaigns.update_email_campaign(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String EMAIL_CAMP_INSERT_DETAIL = "{call pkg_email_campaigns.insert_email_campaign_detail(?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String EMAIL_CAMP_LOOK_UP_EMAIL_CAMPAIGN = "{call pkg_email_campaigns.lookup_email_campaign(?,?,?,?,?)}";
	public static final String EMAIL_CAMP_SEARCH_EMAIL_CAMPAIGN = "{call pkg_email_campaigns.search_email_campaign(?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String EMAIL_CAMP_UPDATE_OPEN_COUNT = "{call pkg_email_campaigns.update_open_count_for_campaign(?,?,?)}";
	public static final String EMAIL_CAMP_UPDATE_CLICK_COUNT = "{call pkg_email_campaigns.update_click_count_for_campaign(?,?,?)}";
	public static final String EMAIL_CAMP_UNSUBSCRIBE = "{call pkg_email_campaigns.unsubscribe_email(?,?,?,?,?)}";
	public static final String EMAIL_CAMP_SEARCH_ADD_CAMPAIGNS = "{call pkg_email_campaigns.search_add_campaigns(?,?,?,?,?,?,?,?)}";
	public static final String EMAIL_CAMP_SCHEDULE_SENDING = "{call pkg_email_campaigns.schedule_sending(?,?,?,?,?,?,?,?)}";

	public static final String EMAIL_QUOTA_LOOKUP = "{call PKG_EMAIL_QUOTA.look_up_quota(?,?,?,?)}";
	public static final String EMAIL_QUOTA_SEARCH = "{call PKG_EMAIL_QUOTA.search_package(?,?,?)}";
	public static final String EMAIL_QUOTA_LOOKUP_PACKAGE = "{call PKG_EMAIL_QUOTA.lookup_package(?,?,?,?)}";
	public static final String EMAIL_QUOTA_ADD_QUOTA = "{call PKG_EMAIL_QUOTA.add_quota(?,?,?,?,?)}";

	public static final String GET_ART_LIST = "{call PKG_ART.get_art_list(?,?,?)}";
	public static final String ART_UPDATE_DETECT_COLOR = "{call PKG_ART.update_detect_color(?,?,?,?,?)}";

	public static final String GET_APPAREL_FULFILLMENT = "{call PKG_FULFILLMENT.get_apparel_fulfillment(?,?,?,?,?,?)}";
	public static final String GET_APPAREL_FULFILLMENT_DETAIL = "{call PKG_FULFILLMENT_DETAIL.fulfillment_oder_summary(?,?,?,?,?)}";
	public static final String MANUAL_UPDATE_SHIPPING = "{call PKG_FULFILLMENT_DETAIL.manual_update_shipping(?,?,?,?,?,?,?,?)}";

	public static final String GET_CUSTOMER_ORDER_PLACED = "{call PKG_CUSTOMER.get_customer_order_placed(?,?,?,?,?,?)}";
	public static final String GET_CUSTOMER_NEW_LETTER = "{call PKG_CUSTOMER.get_customer_new_letter(?,?,?,?,?,?)}";

	public static final String CAMP_UPDATE_SEO = "{call PKG_CAMPAIGN.camp_update_seo(?,?,?,?,?,?,?)}";
	public static final String CAMP_GET_SEO = "{call PKG_CAMPAIGN.camp_get_seo(?,?,?,?)}";
	public static final String CAMP_GET_URI = "{call PKG_URL.search_url(?,?,?,?)}";

	public static final String PARTNER_PAYOUT_GET = "{call PKG_PARTNER_PAYOUT.PARTNER_PAYOUT_GET(?,?,?,?,?,?)}";
	public static final String TEMPLATE_SEARCH = "{call PKG_EMAIL_TEMPLATE.template_search(?,?,?,?,?,?)}";

	public static final String CAMP_TEAKEDOWN_INSERT = "{call PKG_CAMPAIGN_TAKEDOWN.camp_takedown_insert(?,?,?,?,?,?,?)}";
	public static final String CAMP_TAKEDOWN_GET = "{call PKG_CAMPAIGN_TAKEDOWN.camp_takedown_get(?,?,?,?,?)}";

	public static final String GET_SERVER_LIST = "{call PKG_SERVER.list_server(?,?,?)}";

	public static final String DELETE_BILLING = "{call PKG_BILLING_ADDRESS.delete_billing(?,?,?,?)}";
	public static final String SAVE_BILLING = "{call PKG_BILLING_ADDRESS.save_billing(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String USER_UPDATE_W8 = "{call PKG_USER.user_update_w8(?,?,?,?,?)}";

	public static final String SHOPPER_INSERT = "{call PKG_SHOPPER.shopper_insert(?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String SHOPPER_GET_BY_EMAIL = "{call PKG_SHOPPER.shopper_get_by_email(?,?,?,?,?)}";
	public static final String SHOPPER_UPDATE_TOKEN = "{call PKG_SHOPPER.shopper_update_token(?,?,?,?,?,?)}";
	public static final String SHOPPER_UPDATE_PASSWORD = "{call PKG_SHOPPER.shopper_update_password(?,?,?,?,?)}";
	public static final String SHOPPER_GET_ORDER_BY_SHOPPER_ID = "{call PKG_SHOPPER.get_orders_by_shopper_id(?,?,?,?,?,?,?)}";
	public static final String SHOPPER_UPDATE_INFO = "{call PKG_SHOPPER.shopper_update_info(?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String FIND_BY_TRACKING_CODE = "{call PKG_ORDER.check_tracking_code(?,?,?,?)}";
	public static final String RECEIVE_TAKEDOWN_EMAIL = "{call PKG_CAMPAIGN_TAKEDOWN.receive_takedown_email_info(?,?,?,?,?)}";

	public static final String PRODUCT_VARIANTS_EXPORT = "{call PKG_PRODUCT_VARIANT.product_variants_export(?,?,?,?,?)}";
	public static final String DROPSHIP_SHIPPING_UPDATE = "{call PKG_SHIPPING.dropship_shipping_update(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static final String PRODUCT_FEED_CREATE = "{call PKG_PRODUCT_FEED_GENERATE.product_feed_create(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PRODUCT_FEED_GET_DOMAIN = "{call PKG_PRODUCT_FEED_GENERATE.product_feed_get_domain(?,?,?,?,?)}";
	public static final String ACCOUNTING_COMPUTE_SIZEMAP = "{call PKG_ACCOUNTING.compute_sizemap(?,?,?)}";
	public static final String CUSTOMER_GENERATE_CREATE = "{call PKG_CUSTOMER_GENERATE.customer_generate_create(?,?,?,?,?)}";
	public static final String CUSTOMER_GENERATE_GET = "{call PKG_CUSTOMER_GENERATE.customer_generate_get(?,?,?,?,?)}";
	public static final String BASE_FEED_GET = "{call PKG_BASE_FEED.base_feed_get(?,?,?)}";
	public static final String BASE_SIZE_GET_BY_BASE_IDS = "{call PKG_BASE_SIZE.base_size_get_by_base_ids(?,?,?,?)}";
	public static final String BASE_COLOR_GET_BY_BASE_IDS = "{call PKG_BASE_COLOR.get_base_color_by_base_ids(?,?,?,?)}";
	public static final String PRODUCT_FEED_GET = "{call PKG_PRODUCT_FEED_GENERATE.product_feed_get(?,?,?,?)}";
	public static final String PRODUCT_FEED_UPDATE = "{call PKG_PRODUCT_FEED_GENERATE.product_feed_update(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PRODUCT_FEED_DELETE = "{call PKG_PRODUCT_FEED_GENERATE.product_feed_delete(?,?,?,?)}";
	public static final String PRODUCT_FEED_GET_BY_ID = "{call PKG_PRODUCT_FEED_GENERATE.product_feed_get_by_id(?,?,?,?)}";
	public static final String CAMPAIGN_TAGS_LIST = "{call PKG_PRODUCT_FEED_GENERATE.campaign_tags_list(?,?,?,?,?)}";
	public static final String GET_CONTEST_SALES = "{call PKG_AFFILIATE_PORTAL.get_contest_sales(?,?,?,?,?)}";
	public static final String RENEW_DOMAIN = "{call PKG_DOMAIN.renew_domain(?,?,?,?,?)}";
	public static final String GET_CAMP_DETAIL = "{call PKG_AFFILIATE_PORTAL.get_campaigns_detail(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String GET_CAMPAIGN_STATE = "{call PKG_CAMPAIGN.GET_CAMPAIGN_STATE(?,?,?,?)}";
	public static final String GET_SIZE_AND_PRICE_BY_BASE_ID = "{call PKG_BASE_SIZE.get_size_and_price_by_base_id(?,?,?,?)}";
	public static final String GET_ALL_BASE_AND_BASE_GROUP_NAME = "{call PKG_BASE.get_all_base_and_base_group_name(?,?,?)}";
	/**
	 * PKG_EXTERNAL_TRACKING *
	 */
	public static final String EXTERNAL_TRACKING_INSERT = "{call PKG_EXTERNAL_TRACKING.insert_tracking(?,?,?,?,?,?)}";
	public static final String EXTERNAL_TRACKING_GET_BY_REFERENCE_AND_VENDOR = "{call PKG_EXTERNAL_TRACKING.get_by_reference_and_vendor(?,?,?,?,?)}";
	public static final String EXTERNAL_TRACKING_UPDATE_PACKAGE_STATE = "{call PKG_EXTERNAL_TRACKING.update_package_state(?,?,?,?,?,?)}";
	public static final String EXTERNAL_TRACKING_DELETE_OTHER_CARRIERS_DETECTED = "{call PKG_EXTERNAL_TRACKING.delete_other_carriers_detected(?,?,?,?,?,?)}";
	public static final String EXTERNAL_TRACKING_UPDATE_TRACKING_STATUS = "{call PKG_EXTERNAL_TRACKING.update_tracking_status(?,?,?,?,?,?)}";

	public static final String USER_MARK_TOOL_SCRIPTS = "{call PKG_USER.mark_tool_scripts(?,?,?,?,?)}";
	public static final String CAMP_UPSELL_LOOK_UP = "{call PKG_CAMPAIGN_UPSELL.look_up(?,?,?,?)}";
	public static final String DROPSHIP_ORDER_CAMP_SEARCH = "{call PKG_DROPSHIP_ORDER.camp_search(?,?,?,?,?,?,?,?)}";

	/**
	 * PKG_COLOR_TEMPLATE *
	 */
	public static final String COLOR_TEMPLATE_LIST = "{call PKG_COLOR_TEMPLATE.color_template_list(?,?,?,?)}";
	public static final String COLOR_TEMPLATE_CREATE = "{call PKG_COLOR_TEMPLATE.color_template_create(?,?,?,?,?,?,?)}";
	public static final String COLOR_TEMPLATE_GET_BY_ID = "{call PKG_COLOR_TEMPLATE.color_template_get_by_id(?,?,?,?)}";
	public static final String COLOR_TEMPLATE_UPDATE = "{call PKG_COLOR_TEMPLATE.color_template_update(?,?,?,?,?,?,?)}";
	public static final String COLOR_TEMPLATE_DELETE = "{call PKG_COLOR_TEMPLATE.color_template_delete(?,?,?,?)}";
	public static final String GET_BASES_BY_TYPE_ID = "{call PKG_COLOR_TEMPLATE.get_bases_by_type_id(?,?,?)}";
	public static final String COLOR_TEMPLATE_LIST_COLOR = "{call PKG_COLOR_TEMPLATE.color_template_list_color(?,?,?,?)}";

	/** ORDER TO FINANCE QUEUE **/
	public static final String ORDER_TO_FINANCE_QUEUE_SAVE = "{call PKG_ORDER_TO_FINANCE_QUEUE.proc_save(?,?,?,?,?,?,?,?,?)}";

	public static final String CAMPAIGN_CHECK_TRADEMARK = "{call PKG_CAMPAIGN.isTrademark(?,?,?,?)}";
	
	/**
	 * LEATHER *
	 */
	public static final String PRODUCT_UPDATE_COLORS = "{call PKG_PRODUCT.product_update_colors(?,?,?,?,?)}";
	public static final String PRODUCT_LEATHER_INSERT = "{call PKG_PRODUCT.leather_product_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String PRODUCT_LEATHER_SET_TO_DEFAULT = "{call PKG_PRODUCT.leather_set_to_default(?,?,?,?,?,?,?)}";
	public static final String UPDATE_DESIGN_ID = "{call PKG_PRODUCT.update_design_id(?,?,?)}";
	public static final String PRODUCT_LEATHER_DELETE = "{call PKG_PRODUCT.product_leather_delete(?,?,?,?)}";
	public static final String VARIANT_LEATHER_DELETE = "{call PKG_PRODUCT.variant_leather_delete(?,?,?,?)}";
	
//	public static final String CAMP_DETAIL_UPSELL_GET = "{call PKG_CAMPAIGN_UPSELL.camp_detail_upsell_get(?,?,?,?,?,?,?,?)}";

	public static final String GET_PRICE_BY_PRODUCT_ID_AND_SIZE_ID = "{call PKG_BASE_SIZE.get_price_by_product_id_and_size_id(?,?,?,?,?)}";
	
	/**
	 * BRAND *
	 */
	public static final String BRAND_GET = "{call PKG_BRAND.brand_get(?,?,?,?,?,?,?)}";
	public static final String BRAND_SEARCH = "{call PKG_BRAND.brand_search(?,?,?,?,?,?,?,?,?,?,?,?)}";
	public static final String BRAND_INSERT = "{call PKG_BRAND.brand_insert(?,?,?,?,?,?,?,?,?,?)}";
	public static final String BRAND_OPTION_PRICE = "{call PKG_BRAND.brand_option_price(?,?,?,?)}";
	public static final String BRAND_OPTION_INSERT = "{call PKG_BRAND.brand_option_insert(?,?,?,?,?,?,?,?)}";
	public static final String BRAND_DELETE = "{call PKG_BRAND.brand_delete(?,?,?)}";
	public static final String BRAND_UPDATE_STATE = "{call PKG_BRAND.brand_update_state(?,?,?,?)}";
	public static final String BRAND_UPDATE = "{call PKG_BRAND.brand_update(?,?,?,?,?,?,?,?)}";
	public static final String BRAND_CHECK_AVAILBLE = "{call PKG_BRAND.brand_check_availble(?,?,?,?,?)}";
	public static final String BRAND_GET_OPTIONS_TOTAL_AMOUNT = "{call PKG_BRAND.get_options_total_amount(?,?,?,?)}";
	public static final String BRAND_DOMAIN_LIST = "{call PKG_BRAND.brand_domain_list(?,?,?,?)}";
	public static final String BRAND_DOMAIN_STORE_COMBO = "{call PKG_BRAND.brand_get_domain_store_combo(?,?,?,?)}";
	
	public static final String CAMP_DETAIL_UPSELL_GET = "{call PKG_CAMPAIGN_UPSELL.camp_detail_upsell_get(?,?,?,?,?,?,?,?)}";
	public static final String LOGIN_HISTORY_INSERT = "{call PKG_USER.login_history_insert(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	/**
	 * GMC
	 */
    public static final String CREATE_FEED_GMC =	"{call PKG_GMC.create_product_feed_gmc(?,?,?,?,?,?,?,?,?,?,?,?,?)" ;
	public static final String FEED_EXPORT_ALL_CAMPAIGN_ID = "{call PKG_PRODUCT_FEED_GENERATE.export_all_campaign_id(?,?,?,?,?,?)}";
	public static final String FILTER_CAMPAIGN_BY_TAG = "{call PKG_PRODUCT_FEED_GENERATE.campaign_filter_by_tag(?,?,?,?,?)}";
	public static final String PRODUCT_FEED_GET_BASE_ID_AND_SIZES = "{call PKG_PRODUCT_FEED_GENERATE.product_feed_get_base_id_and_sizes(?,?,?)}";
    public static final String GET_CAMPAIGN_PRODUCT_FEED_V2 = "{call PKG_GMC.campaign_product_feed_get_v2(?,?,?,?,?,?,?)";
    public static final String INSERT_GMC_TOKEN = "{call PKG_GMC.insert_gmc_token(?,?,?,?,?,?)}" ;
	public static final String GET_GMC_TOKEN = "{call PKG_GMC.get_refresh_token(?,?,?,?,?)}";
	public static final String GET_ALL_GMC_ID = "{call PKG_GMC.get_gmc_id(?,?,?,?,?)}" ;
    public static final String EXPORT_ALL_CAMP_ID = "{call PKG_GMC.export_all_campaign_id(?,?,?,?,?,?,?)}";
    public static final String UPDATE_GMC_ID = "{call PKG_GMC.update_gmc_id(?,?,?,?,?)";
    public static final String DISCONNECT_GMC = "{call PKG_GMC.disconnect_gmc(?,?,?,?)}";
}
