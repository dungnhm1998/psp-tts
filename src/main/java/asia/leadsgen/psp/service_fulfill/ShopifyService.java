package asia.leadsgen.psp.service_fulfill;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.shopify.service.ShopifyAPIEndpoints;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.DBProcedurePool;
import asia.leadsgen.psp.util.DBProcedureUtil;
import asia.leadsgen.psp.util.JSONStringToMapUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.driver.OracleSQLException;

public class ShopifyService {

    private static DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    static final String DROPSHIP_GET_CAMP_INFO = "{call PKG_DROPSHIP.get_campaign_info(?,?,?,?)}";

    private static List<String> sizes = Arrays.asList("S", "M", "L", "XL", "2XL", "3XL", "4XL", "5XL");

    public static void main(String[] strs) throws SQLException {

        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("app-context.xml");
        Map productImages = new LinkedHashMap<>();
        Map xxx = generateVariantsMap("A355-3", productImages);

        String rqBody = new JsonObject(xxx).encode();
        System.out.println(rqBody);

        try {
            String url = "https://cf470defd0cb941ff9fee7b2d7bea18c:122bd1b29db67d5c5b7b240520855e70@teefanibabe.myshopify.com/admin/products.json";
            HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json").body(rqBody)
                    .asString();
            System.out.println(response.getBody().toString());
            if (response.getStatus() == 201 || response.getStatus() == 200) {
                String resBody = response.getBody();
                Map map = JSONStringToMapUtil.toMap(resBody);
                Map prd = ParamUtil.getMapData(map, "product");
                List<Map> variants = ParamUtil.getListData(prd, "variants");
                List<Map> images = ParamUtil.getListData(prd, "images");

                variants.forEach(v -> {
                    String vid = ParamUtil.getString(v, "id");
                    String prdname = ParamUtil.getString(v, "option1");
                    String imagePrefix = (String) productImages.get(prdname);
                    String image = imagePrefix + "-"
                            + ParamUtil.getString(v, "option2").toLowerCase().replace(" ", "-").trim();
                    images.forEach(i -> {
                        String xurl = ParamUtil.getString(i, "src");
                        if (StringUtils.containsIgnoreCase(xurl, image)) {
                            updateVariantImage(vid, ParamUtil.getString(i, "id"));
                        }
                    });

                });
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }

    }

    private static void updateVariantImage(String variantId, String imageId) {
        String url = "https://cf470defd0cb941ff9fee7b2d7bea18c:122bd1b29db67d5c5b7b240520855e70@teefanibabe.myshopify.com/admin/variants/"
                + variantId + ".json";
        Map update = new LinkedHashMap<>();
        Map variant = new LinkedHashMap<>();
        variant.put("image_id", imageId);
        update.put("variant", variant);

        String requestBody = new JsonObject(update).encode();
        System.out.println(requestBody);
        try {
            HttpResponse<String> response = Unirest.put(url).header("Content-Type", "application/json")
                    .body(requestBody).asString();
            System.out.println(response.getStatus());
        } catch (UnirestException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static Map connectStore(String authorizationCode, String storeName, String apiKey, String secretKey) {
        String url = "https://" + storeName + "." + AppConstants.SHOPIFY_AUTHENTICATION_URL;
        LOGGER.info("ShopifyService connectStore url requestUrl= " + url);
        Map authentication = new LinkedHashMap<>();
        Map shopifyAuthMap = new LinkedHashMap<>();
        authentication.put("client_id", apiKey);
        authentication.put("client_secret", secretKey);
        authentication.put("code", authorizationCode);
        LOGGER.info("authenticationMap= " + authentication.toString());

        String requestBody = new JsonObject(authentication).encode();
        try {
            HttpResponse<String> response = Unirest.post(url).header("accept", "application/json").header("Content-Type", "application/json").body(requestBody).asString();
            shopifyAuthMap = JSONStringToMapUtil.toMap(response.getBody());
            int result = response.getStatus();
            if (result != 200) {
            	LOGGER.info("connectStore response-status:" + response.getStatus());
    		    LOGGER.info("data result text:" + response.getStatusText());
    		    LOGGER.info("message: " + response.getBody());
                throw new BadRequestException(SystemError.INVALID_AUTH_TOKEN);
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return shopifyAuthMap;
    }

    public static Map getStoreLocation(String domain, String token) {
        String url = String.format(ShopifyAPIEndpoints.SHOPIFY_STORE_LOCATION, domain);

        Map storeLocationMap = new LinkedHashMap<>();

        try {
            HttpResponse<String> response = Unirest.get(url).header("accept", "application/json").header("Content-Type", "application/json").header("X-Shopify-Access-Token", token).asString();
            storeLocationMap = JSONStringToMapUtil.toMap(response.getBody());
            int result = response.getStatus();
            if (result != 200) {
                throw new BadRequestException(SystemError.INVALID_AUTH_TOKEN);
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return storeLocationMap;
    }

    public static Map generateVariantsMap(String campaignId, Map productImages) throws SQLException {

        List<Map> campaignProducts = getCampaignInfo(campaignId);

        List<Map> productVariants = new ArrayList<>();

        Map campaignInfoMap = new LinkedHashMap<>();

        if (!campaignProducts.isEmpty()) {

            List<String> processedProduct = new ArrayList<>();
            Set<String> allColors = new LinkedHashSet<>();
            List<Map> allImages = new ArrayList<>();

            campaignProducts.forEach(product -> {

                String name = ParamUtil.getString(product, "S_PRODUCT_NAME");
                String price = ParamUtil.getString(product, "S_SALE_PRICE");
                String variantId = ParamUtil.getString(product, "S_VARIANT_ID");
                String colorName = ParamUtil.getString(product, "S_COLOR_NAME");
                productVariants.addAll(addProductVariants(name, price, variantId, colorName, sizes));

                String img = ParamUtil.getString(product, "S_IMG_URL");
                String fullImageName = StringUtils
                        .substring(StringUtils.substring(img, StringUtils.lastIndexOf(img, "/") + 1), 0, 16);
                while (fullImageName.startsWith(StringPool.DASH)) {
                    fullImageName = fullImageName.replaceFirst(StringPool.DASH, StringPool.BLANK);
                }
                productImages.put(name, fullImageName);

                Map<String, String> imgSource = new HashMap<>();
                imgSource.put("src", img);
                allImages.add(imgSource);

            });

            Map firstProduct = campaignProducts.get(0);

            campaignInfoMap.put("title", ParamUtil.getString(firstProduct, "S_CAMPAIGN_NAME"));
            campaignInfoMap.put("body_html", ParamUtil.getString(firstProduct, "S_DESC"));

            campaignInfoMap.put("variants", productVariants);

            List<Map> options = new ArrayList<>();

            Map nameOption = new LinkedHashMap<>();
            nameOption.put("name", "Name");
            nameOption.put("values", processedProduct);

            options.add(nameOption);

            Map colorOption = new LinkedHashMap<>();
            List<String> colors = new ArrayList<>();
            colors.addAll(allColors);
            colorOption.put("name", "Color");
            colorOption.put("values", colors);

            options.add(colorOption);

            Map sizeOption = new LinkedHashMap<>();
            sizeOption.put("name", "Size");
            sizeOption.put("values", sizes);

            options.add(sizeOption);

            campaignInfoMap.put("options", options);
            campaignInfoMap.put("images", allImages);

        }

        Map productInfo = new LinkedHashMap<>();
        productInfo.put("product", campaignInfoMap);

        return productInfo;
    }

    private static Collection<? extends Map> addProductVariants(String name, String price, String variantId,
            String colorName, List<String> sizes) {

        List<Map> variants = new ArrayList<>();
        double compare_at_price = Double.parseDouble(price) * 120 / 100;

        sizes.forEach(size -> {
            Map variant = new LinkedHashMap<>();
            variant.put("option1", name);
            variant.put("option2", colorName);
            variant.put("option3", size);
            variant.put("price", price);
            variant.put("compare_at_price", compare_at_price);
            variant.put("sku", variantId);

            variants.add(variant);
        });

        return variants;

    }

    private static List<Map> getCampaignInfo(String campaignId) throws SQLException {

        Map inputParams = new LinkedHashMap<Integer, String>();
        inputParams.put(1, campaignId);

        Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
        outputParamsTypes.put(2, OracleTypes.NUMBER);
        outputParamsTypes.put(3, OracleTypes.VARCHAR);
        outputParamsTypes.put(4, OracleTypes.CURSOR);

        Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
        outputParamsNames.put(2, AppParams.RESULT_CODE);
        outputParamsNames.put(3, AppParams.RESULT_MSG);
        outputParamsNames.put(4, AppParams.RESULT_DATA);

        Map report = DBProcedureUtil.execute(dataSource, DROPSHIP_GET_CAMP_INFO, inputParams,
                outputParamsTypes, outputParamsNames);

        int resultCode = ParamUtil.getInt(report, AppParams.RESULT_CODE);
        if (resultCode != HttpResponseStatus.OK.code()) {
            throw new OracleSQLException();
        }

        return ParamUtil.getListData(report, AppParams.RESULT_DATA);

    }

    public static boolean createWebhook(String topic, String urlApi, String storeId, String domain, String apiKey, String address) throws UnirestException {

        Map requestBodyMap = new LinkedHashMap<>();
        Map webhook = new LinkedHashMap<>();
        webhook.put("topic", topic);
        webhook.put("address", urlApi + "/shopify-store/" + storeId + "/" + address);
        webhook.put("format", "json");
        requestBodyMap.put("webhook", webhook);

        String requestBody = new JsonObject(requestBodyMap).encode();

        String url = String.format(ShopifyAPIEndpoints.CREATE_WEBHOOK, domain);
        LOGGER.info("ShopifyService createWebhook url requestUrl=" + url);
        LOGGER.info("ShopifyService createWebhook requestBody=" + requestBody);
        HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", apiKey).body(requestBody).asString();

//		Map responseMap = new JsonObject(response.getBody()).getMap();
        LOGGER.info("ShopifyService response: " + response.getStatus());
        LOGGER.info("ShopifyService responseBody: " + response.getBody());
        if (response.getStatus() == 201 || response.getStatus() == 200) {
            return true;
        }
        return false;
    }

    public static List<Map> OrderPullData(String domain, String apiKey, String ids, String status, String page, String limit, String created_at_min, String created_at_max)
            throws UnirestException, IOException {
        String url = String.format(ShopifyAPIEndpoints.ORDERS_USING_TOKEN, domain);
        HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", apiKey)
                .queryString("limit", limit)
                .queryString("status", status)
                .queryString("created_at_min", created_at_min)
                .queryString("created_at_max", created_at_max)
                .queryString("ids", ids)
                .asString();

        if (response.getStatus() != 200) {

            throw new BadRequestException(SystemError.INVALID_REQUEST);
        }
        Map mapResult = new JsonObject(response.getBody()).getMap();

        List<Map> orders = ParamUtil.getListData(mapResult, "orders");

        return orders;

    }

    public static Map GetOrderById(String domain, String apiKey, String id)
            throws UnirestException {

        String url = String.format(ShopifyAPIEndpoints.ORDERS_DETAIL_USING_TOKEN, domain, id);
        HttpResponse<String> response = Unirest.get(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", apiKey).asString();

        Map result = new LinkedHashMap<>();
        if (response.getStatus() == 200) {

            result = ParamUtil.getMapData(new JsonObject(response.getBody()).getMap(), "order");

        }
        return result;

    }
    
    public static boolean createWebhookV2(String topic, String urlApi, String domain, String apiKey) throws UnirestException {

        Map requestBodyMap = new LinkedHashMap<>();
        Map webhook = new LinkedHashMap<>();
        webhook.put("topic", topic);
        webhook.put("address", urlApi);
        webhook.put("format", "json");
        requestBodyMap.put("webhook", webhook);

        String requestBody = new JsonObject(requestBodyMap).encode();

        String url = String.format(ShopifyAPIEndpoints.CREATE_WEBHOOK, domain);
        LOGGER.info("ShopifyService createWebhook url requestUrl=" + url);
        LOGGER.info("ShopifyService createWebhook requestBody=" + requestBody);
        HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json").header("X-Shopify-Access-Token", apiKey).body(requestBody).asString();

//		Map responseMap = new JsonObject(response.getBody()).getMap();
        LOGGER.info("ShopifyService response: " + response.getStatus());
        LOGGER.info("ShopifyService responseBody: " + response.getBody());
        if (response.getStatus() == 201 || response.getStatus() == 200) {
            return true;
        }
        return false;
    }
    
    private static final Logger LOGGER = Logger.getLogger(ShopifyService.class.getName());

}
