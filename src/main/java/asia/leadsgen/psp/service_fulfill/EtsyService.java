package asia.leadsgen.psp.service_fulfill;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.EtsyException;
import asia.leadsgen.psp.exception.SystemException;
import asia.leadsgen.psp.server.handler.etsy.EtsyApi;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.BasePhoneCaseUtil;
import asia.leadsgen.psp.util.JSONStringToMapUtil;
import asia.leadsgen.psp.util.ParamUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import lombok.Getter;
import lombok.Setter;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
@Setter
public class EtsyService extends MasterService {

    //Dân
//    public static String consumerKey = "1yjx034uaha90eu4xx8uce8t";
//    public static String consumerSecret = "xau5hlmnj9";

    //A Trung
    public static String consumerKey;
    public static String consumerSecret;
    private static OAuth10aService service;
    private static OAuth1AccessToken accessToken;

    public static String REDIRECT_URL;
    public static String redirectUrlPrefix = "https://pro.30usd.com/dropship/etsy/connect?store=";
    public static String skuPrefix = "BG";

    public static OAuthConsumer consumer;
    public static OAuthProvider provider = new DefaultOAuthProvider(
            "https://openapi.etsy.com/v2/oauth/request_token",
            "https://openapi.etsy.com/v2/oauth/access_token",
            "https://www.etsy.com/oauth/signin");

    public EtsyService(){}

    public EtsyService(OAuth1AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public static Map getOauthConnectUrl(String userId) throws OAuthCommunicationException, OAuthExpectationFailedException,
            OAuthNotAuthorizedException, OAuthMessageSignerException {
        LOGGER.info("Fetching request token from Etsy...");
        REDIRECT_URL = redirectUrlPrefix;
        LOGGER.info("consumerKey " + consumerKey);
        LOGGER.info("consumerSecret " + consumerSecret);
        LOGGER.info("REDIRECT_URL " + REDIRECT_URL);

        String oauthUrl = provider.retrieveRequestToken(consumer, REDIRECT_URL);
        LOGGER.info("RequestToken : " + consumer.getToken());
        LOGGER.info("TokenSecret : " + consumer.getTokenSecret());

        Map consumerToken = new HashMap();
        String redisKey = "etsy-" + userId;
        consumerToken.put("token", consumer.getToken());
        consumerToken.put("token_secret", consumer.getTokenSecret());
        RedisService.persist(redisKey, consumerToken);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("url", oauthUrl);
        return result;
    }

    public static Map retrieveAccessToken(String userId, String code) throws OAuthCommunicationException, OAuthExpectationFailedException,
            OAuthNotAuthorizedException, OAuthMessageSignerException, IOException, SQLException {

        String redisKey = "etsy-" + userId;
        Map consumerToken = RedisService.get(redisKey);
        String oldToken = consumerToken.get("token").toString();
        String oldTokenSecret = consumerToken.get("token_secret").toString();
        LOGGER.info("Old Token : " + consumer.getToken());
        LOGGER.info("Old TokenSecret : " + consumer.getTokenSecret());
        consumer.setTokenWithSecret(oldToken, oldTokenSecret);
        RedisService.delete(redisKey);

        provider.retrieveAccessToken(consumer, code);
        LOGGER.info("VERIFIER CODE : " + code);
        String token = consumer.getToken();
        String tokenSecret = consumer.getTokenSecret();
        if (StringUtils.isEmpty(token)) {
            throw new SystemException(SystemError.AUTHORIZATION_FAILURE);
        }

        LOGGER.info("Access Token : " + token);
        LOGGER.info("Token Secret : " + tokenSecret);

        //Luư vào DB cùng userId
        accessToken = new OAuth1AccessToken(token, tokenSecret);
        Map userInfo = getEtsyUserInfo();
        String etsyUserId = userInfo.get(AppParams.USER_ID).toString();
        List<Map<String,String>> stores = storeInfo(etsyUserId);
        String shopEmail = userInfo.get("primary_email").toString();

        for (Map store : stores) {
            String etsyStoreName = ParamUtil.getString(store, "shopName");
            String etsyStoreId = ParamUtil.getString(store, "shopId");

            Map status = DropShipStoreService.createEtsyStore(userId, etsyStoreName, token, tokenSecret, etsyStoreId, shopEmail);

            String connectResult = String.valueOf(status.get(AppParams.STATUS));
            if (connectResult.contains("Fail")) {
                int startIndex = connectResult.indexOf("Store");
                SystemError error = new SystemError("FAIL TO CONNECT ETSY STORE",
                        connectResult.substring(startIndex, connectResult.length()), "",
                        "http://developer.30usd.com/errors/401.html");
                throw new EtsyException(error);
            }
        }

        Map result = new LinkedHashMap();
        result.put(AppParams.STATUS, "Connect Success");
        return result;
    }

    public static Map getEtsyUserInfo() throws IOException {
        Map json = getService("users/__SELF__");
        List userInfo = (List) json.get("results");
        Map user = (Map) userInfo.get(0);
        return user;
    }

    public static List<Map<String,String>> storeInfo(String etsyUserId) throws IOException {
        Map shops = getService("users/" + etsyUserId + "/shops");
        List result = new ArrayList();
        List shopList = (List) shops.get("results");
        for (Object shop : shopList) {
            Map singleShop = (Map) shop;
            String shopName = String.valueOf(singleShop.get("shop_name"));
            String shopId = String.valueOf(singleShop.get("shop_id"));
            LOGGER.info("ETSY STORE NAME : " + shopName);

            Map<String,String> singleResult = new HashMap();
            singleResult.put("shopName", shopName);
            singleResult.put("shopId", shopId);
            result.add(singleResult);
        }
        return result;
    }

    public static Map getService(String method) throws IOException {
        OAuthRequest callRequest = new OAuthRequest(Verb.GET, "https://openapi.etsy.com/v2/" + method, service);
        service.signRequest(accessToken, callRequest);
        return sendRequest(callRequest);
    }

    public static Map<String, Object> postService(String method) throws IOException {
        OAuthRequest callRequest = new OAuthRequest(Verb.POST, "https://openapi.etsy.com/v2/" + method, service);
        service.signRequest(accessToken, callRequest);
        return sendRequest(callRequest);
    }

    public static Map<String, Object> postService(String method, Map<String, Object> param) throws IOException {
        OAuthRequest callRequest = new OAuthRequest(Verb.POST, "https://openapi.etsy.com/v2/" + method, service);
        callRequest = addParameterMap(callRequest, param);
        service.signRequest(accessToken, callRequest);
        return sendRequest(callRequest);
    }

    public static Map<String, Object> postImage(String method, File imageFile, String token, String tokenSecret) throws IOException, OAuthCommunicationException, OAuthExpectationFailedException, OAuthMessageSignerException {
        OAuthConsumer commonConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
        commonConsumer.setTokenWithSecret(token, tokenSecret);

        String url = "https://openapi.etsy.com/v2/" + method;
        HttpPost request = new HttpPost(url);
        commonConsumer.sign(request);

        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.STRICT);
        FileBody imageBody = new FileBody(imageFile, "image/jpeg");
        entity.addPart("image", imageBody);
        request.setEntity( entity);

        HttpClient httpClient = getSecureClient();
        HttpResponse response = httpClient.execute(request);

        BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String reply = in.readLine();
        Map result = JSONStringToMapUtil.toMap(reply);
        return result;
    }

    public static HttpClient getSecureClient() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        HttpParams params = new BasicHttpParams();
        SingleClientConnManager mgr = new SingleClientConnManager(params, schemeRegistry);
        HttpClient httpClient = new DefaultHttpClient(mgr, params);
        return httpClient;
    }

    public static Map<String, Object> putService(String method, Map<String, Object> param) throws IOException {
        OAuthRequest callRequest = new OAuthRequest(Verb.PUT, "https://openapi.etsy.com/v2/" + method, service);
        callRequest = addParameterMap(callRequest, param);
        service.signRequest(accessToken, callRequest);
        return sendRequest(callRequest);
    }

    public static OAuthRequest addParameterMap(OAuthRequest authRequest, Map<String, Object> param) throws JsonProcessingException {
        OAuthRequest callRequest = authRequest;
        for (Map.Entry<String, Object> entry : param.entrySet()) {
            callRequest.addParameter(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return callRequest;
    }

    public static Map sendRequest(OAuthRequest callRequest) throws IOException {
        Response resp = callRequest.send();

        String results = "";
        try {
            if (!resp.isSuccessful()) {
                SystemError error = new SystemError(resp.getMessage().toUpperCase().replace(" ", "_"),
                        resp.getBody(), "", "http://developer.30usd.com/errors/401.html");
                throw new EtsyException(error);
            }
            results = resp.getBody();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JSONStringToMapUtil.toMap(results);
    }

    public static List<Map> filterOrderByDate(String storeId, List<Map> resultOrders, long dateInEpoch) throws SQLException {
        String newestOrderIdFromDB = DropshipOrderServiceV2.getNewestOrderIdFromDB(storeId);
        List<Map> newOrders = new ArrayList<>();

        if (StringUtils.isEmpty(newestOrderIdFromDB)) {
            newOrders = resultOrders.stream().filter(order -> Long.parseLong(String.valueOf(order.get("creation_tsz"))) > dateInEpoch)
                    .collect(Collectors.toList());
        }
        else {
            Long newestId = Long.parseLong(newestOrderIdFromDB);
            newOrders = resultOrders.stream().filter(order -> Long.parseLong(order.get("receipt_id").toString()) > newestId
                    && Long.parseLong(String.valueOf(order.get("creation_tsz"))) > dateInEpoch)
                    .collect(Collectors.toList());
        }

        return newOrders;
    }

    public static Map<String, String> taxonomyMap(){
        Map<String, String> taxonomies = new HashMap();

        taxonomies.put("374", "8d_QKhXD-86igKPP,KfXL7aOF8wUOJZyH,9BESdMiawozq3yyX,aneISamhDEoT39oy,KRfcLkcYItB4MQmG,zUCKpLPrl7xnsQU5,"
                            + "-cwc847TMWTaZmVg,DpyfFZQ04G-jN_pL,xaLgxhHX9KFQfLNY,aIEqbcaqYKa0u8mr,lWoVznlz828x8wvd,QOYmaDsgdE7nNy21,"
                            + "7ojeKe0NVNlUR2kp,y5CtqcdLYx8QQp7M,fyO46EELynO72L0V,7wTKZtthhjDW71oA,xtcHqBfLVsVt6YT8,7wTKZtthhjDW71oA,"
                            + "GAFofttyq18A5l2u,Bj0pbciRSov9PhE4,mrnEth0Nvn3011nn,tL9HoLtLPWpM1Mz4,N9xCAMpNLanQnP2q,hncITeOCNanKXcYp,"
                            + "WS-CFAJxOx1edtrB,---2BfBws3P6wKDO,Nhy-R_esvYxrhdCX");

        taxonomies.put("66", "i8ja02GzNr57tTwZ,TYr3JbE7xrqWSI_8,2iOH2h0TgphfF48v,ZV6YA9SorkvO1eof,hOpvQ23725fpkzIP,L0baqM1xW12XIaxj,"
                            + "lAXkEDpRhWu3_ulu,1XzIeZzaWu65jf92,oxrtWFG9QF0FWVyE,AML2RNWTivebyev1,0-BOpogayL1X3FCt,a-x80z12BQyuq1E3,"
                            + "WWQ7xQ2aYSa0XUEC,EOCuz3dpn8DqSVPB,Q32VnDgI8ghohFaY,hnqJaRc12vOV5H-N,NIUJTUQ0BwZ473QF,wg1fgLbO77j2WKky,"
                            + "hUeboM3JbQW9lXRK,NsNoxYr6XBXcIYJQ,zDqncXhoPPasNQPE");

        taxonomies.put("825", "Y3AdqKzCEkazPfPc,E0qoEBLJTdDLDHvw,Nvn3011Za4jC123d,laalyONbbFagaHda,dpbQA2bjfcOh9VdS,IGNaSRIgZRuBLlon,"
                            + "ZP2A32i9BIbQRUvX,S8oFCkrVno49oi9H,hlsp9P34agWPv3Nm,5JOr6SDZa4jCLF2d,D4Y6mbipRmC6O3Gd,l4ACCzZed8CDlMn0,"
                            + "Lq3AqndC4bb5mrBT,63WaBRcMDxZyTk5j,CJOdfOGdNkK6mWcH,MePoRad98t33ASiG,2yAM3faYQDQcGoW4,kgNjAwjjmTOu0IWP,"
                            + "LIsl3r85EUoNrkjk,34he6lpTK5lvv6N7,NLpUVKFCzLanQn1I,N8qXaylZ2nanzTim,6Np2NpNHjPzDebv8,RamgjYWrWilXR3Xb,"
                            + "KPkHIQePhIWk3xBa,XCeYCk9a3TBLF0F2");

        taxonomies.put("891", "onaLVn9AeQASvObb,g3dTeZmjKXVP5rNH");
        taxonomies.put("132", "WAD7VY1N0YuM3DKe,aWwcce2tvF4eQxNh,S6uhxPpjAYdp8Fbt,jUfTaGbsKjIKXkcT,AVXTaGbsKjIKXkcT,j1e2h3Wq4Gz5ksjs,LCV9OQEvNZqeuX3D");
        taxonomies.put("323", "n8NgnYoLgiC8ZZre,nO5tUKpgjQ5YCCWt,nGvA2fQZNJM7apIq");
        taxonomies.put("1552", "1AZPrQ5bBYNDZ6A0,2L1g0zFethJLNSHT,3cfZ4NdOpY17oneV,4uLxCUkPOFb2hrqs,E17lOfRntVwoyBB6");

        return taxonomies;
    }

    public static String getTaxonomyId(String baseId) {
        Map<String, String> taxonomies = taxonomyMap();

        for (String taxonomyId : taxonomies.keySet()) {
            String baseIds = taxonomies.get(taxonomyId);
            if (baseIds.contains(baseId)) {
                return taxonomyId;
            }
        }
        return null;
    }

    public static boolean isPhoneCase(String baseId) {
        return BasePhoneCaseUtil.isPhoneCase(baseId);
    }

    public static void main(String[] args) throws IOException, OAuthCommunicationException, OAuthExpectationFailedException, OAuthMessageSignerException {
        String demoToken = "81a35800af386863f6d6a5f9efd7a6";
        String demoTokenSecret = "7dd619c16d";
        accessToken = new OAuth1AccessToken(demoToken, demoTokenSecret);

        Map variationMap = new HashMap();
        String listingId = "932760283";

        Path currentPath = Paths.get("");
        String path = currentPath.toAbsolutePath().toString();
        String url = "http://cdn.30usd.com/products/2020/12/28/A2075/A2075-2218/6Np2NpNHjPzDebv8-white-front.jpg";
        InputStream inputStream = new URL(url).openStream();
        String suffix = url.substring(url.lastIndexOf("."), url.length());
        String filePath = path + "/30usd" + suffix;
        FileOutputStream fileOS = new FileOutputStream(filePath);
        int i = IOUtils.copy(inputStream, fileOS);
        System.out.println(i);
        File file = new File(filePath);
        fileOS.close();
//        System.out.println(file.getName());
//        System.out.println(file.exists());
//        System.out.println(file.delete());
//
        Map results = postImage("/listings/" + listingId + "/images", file, demoToken, demoTokenSecret);
        System.out.println(results);
    }

    public static String getConsumerKey() {
        return consumerKey;
    }

    public static void setConsumerKey(String consumerKey) {
        EtsyService.consumerKey = consumerKey;
    }

    public static String getConsumerSecret() {
        return consumerSecret;
    }

    public static void setConsumerSecret(String consumerSecret) {
        EtsyService.consumerSecret = consumerSecret;
        EtsyService.service = new ServiceBuilder().apiKey(consumerKey).apiSecret(consumerSecret).build(EtsyApi.instance());
        EtsyService.consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
    }

    public static String getRedirectUrlPrefix() {
        return redirectUrlPrefix;
    }

    public static void setRedirectUrlPrefix(String redirectUrlPrefix) {
        EtsyService.redirectUrlPrefix = redirectUrlPrefix;
    }

    public static final Logger LOGGER = Logger.getLogger(EtsyService.class.getName());

    public static SystemError INVALID_ETSY_STORE = new SystemError("INVALID_ETSY_STORE",
            "Cannot find store with provided name, please double check and try again", "", "http://developer.30usd.com/errors/400.html");
    public static SystemError ETSY_STORE_CONNECTED_BEFORE = new SystemError("ETSY_STORE_CONNECTED_BEFORE",
            "Etsy Store was connected before by another user", "", "http://developer.30usd.com/errors/400.html");

    static final String INSERT_ETSY_LISTING_AND_VARIANT = "{call PKG_ETSY.insert_listing_and_variant(?,?,?,?)}";

}
