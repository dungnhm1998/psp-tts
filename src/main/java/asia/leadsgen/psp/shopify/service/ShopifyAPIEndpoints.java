package asia.leadsgen.psp.shopify.service;

public class ShopifyAPIEndpoints {

    /**
     * PRODUCTS *
     */
    public static String PRODUCTS_ALL = "https://%s:%s@%s/admin/api/2020-04/products.json";

    public static String PRODUCTS_ONE = "https://%s:%s@%s/admin/api/2020-04/products/%s.json";

    public static String VARIANTS_ONE = "https://%s:%s@%s/admin/api/2020-04/variants/%s.json";

    /**
     * COLLECTIONS *
     */
    public static String COLLECTIONS_ALL = "https://%s:%s@%s/admin/api/2020-04/custom_collections.json?page=%d&limit=%d";
    // "&published_status=published";

    /**
     * ORDERS *
     */
    public static String ORDERS = "https://%s:%s@%s/admin/api/2020-04/orders.json";

    public static String ORDERS_USING_TOKEN = "https://%s/admin/api/2020-04/orders.json";

    public static String ORDERS_DETAIL_USING_TOKEN = "https://%s/admin/api/2020-04/orders/%s.json";

    public static String VARIANTS_ONE_USING_TOKEN = "https://%s/admin/api/2020-04/variants/%s.json";

    public static String PRODUCTS_ALL_USING_TOKEN = "https://%s/admin/api/2020-04/products.json";

    public static String PRODUCTS_ONE_USING_TOKEN = "https://%s/admin/api/2020-04/products/%s.json";

    public static String COLLECTIONS_ALL_USING_TOKEN = "https://%s/admin/api/2020-04/custom_collections.json?page=%d&limit=%d";

//    public static String SHOPIFY_STORE_LOCATION = "https://%s/admin/locations.json";
    public static String SHOPIFY_STORE_LOCATION = "https://%s/admin/api/2020-04/locations.json";

//    public static String FULFILLMENT_ALL_USING_TOKEN = "https://%s/admin/orders/%s/fulfillments.json";
    public static String FULFILLMENT_ALL_USING_TOKEN = "https://%s/admin/api/2020-04/orders/%s/fulfillments.json";

//    public static String CREATE_WEBHOOK = "https://%s/admin/webhooks.json";
    
    public static String CREATE_WEBHOOK = "https://%s/admin/api/2020-04/webhooks.json";
    
    /**
     * NEW ORDER DROPSHIP *
     */
    public static String FETCH_ORDER_USING_TOKEN = "https://%s/admin/api/2020-04/orders.json";
    
    public static String GET_PRODUCT_VARIANT = "https://%s/admin/api/2020-04/products/%s/variants/%s.json";
    
    public static String GET_PRODUCT_ONE = "https://%s/admin/api/2020-04/products/%s.json";
     
    public static String GET_PRODUCT_IMAGE = "https://%s/admin/api/2020-04/products/%s/images/%s.json";
    
    public static String COUNT_PRODUCT = "https://%s/admin/api/2020-04/orders/count.json";
    
    /**
     * SHOPIFY-APP *
     */
    public static String PRODUCT_IMAGE_USING_TOKEN = "https://%s/admin/api/2020-04/products/%s/images.json";
    
    public static String PRODUCT_VARIANT_USING_TOKEN = "https://%s/admin/api/2020-04/products/%s/variants.json";
    
    public static String COUNT_TOTAL_PRODUCT = "https://%s/admin/api/2020-04/products/count.json";
    
    public static String FETCH_PRODUCTS_USING_TOKEN = "https://%s/admin/api/2020-04/products.json";
    
    public static String PRODUCT_USING_TOKEN = "https://%s/admin/api/2020-04/products/%s.json";
    
    public static String PRODUCT_ONE_IMAGE_USING_TOKEN = "https://%s/admin/api/2020-04/products/%s/images/%s.json";
    
    public static String COLLECTIONS_USING_TOKEN = "https://%s/admin/api/2020-04/custom_collections.json";
    
    public static String COLLECT_USING_TOKEN = "https://%s/admin/api/2020-04/collects.json";
    
    public static String GET_PRODUCT_METAFIELDS = "https://%s/admin/api/2020-04/products/%s/metafields.json";
    
    public static String DELETE_PRODUCT_METAFIELDS = "https://%s/admin/api/2020-04/metafields/%s.json";
    
    public static String PRODUCT_VARIANT_ONE_USING_TOKEN = "https://%s/admin/api/2020-04/products/%s/variants/%s.json";
    
}
