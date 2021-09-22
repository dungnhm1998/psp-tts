package asia.leadsgen.psp.main;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.EtsyException;
import asia.leadsgen.psp.server.handler.etsy.EtsyApi;
import asia.leadsgen.psp.util.CharPool;
import asia.leadsgen.psp.util.JSONStringToMapUtil;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Map;

public class TestEtsy {
    public static String CONSUMER_KEY = "dqbk3zqu87eqgvod8hy411ai";
    public static String CONSUMER_SECRET = "89ubtg4m34";
    private static OAuth10aService service = new ServiceBuilder().apiKey(CONSUMER_KEY).apiSecret(CONSUMER_SECRET).build(EtsyApi.instance());
    private static OAuth1AccessToken accessToken = new OAuth1AccessToken("81a35800af386863f6d6a5f9efd7a6", "7dd619c16d");

    public static String REDIRECT_URL;
    public static String redirectUrlPrefix = "https://pro.30usd.com/dropship/etsy/connect?store=";

    public static OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
    public static OAuthProvider provider = new DefaultOAuthProvider(
            "https://openapi.etsy.com/v2/oauth/request_token",
            "https://openapi.etsy.com/v2/oauth/access_token",
            "https://www.etsy.com/oauth/signin");


    public static void main(String[] args) throws IOException {
//        String orders = getService("/shops/26356338/receipts/open?limit=1000");
//
//        String t1 = getService("receipts/1884918019/transactions");
////        Map t2 = getService("receipts/1884918019/transactions");
//
//        String trans1 = getService("transactions/2185159462");
////        transaction_id -> {Long@2415} 2185159462
////        transaction_id -> {Long@2514} 2209499036
//
//        String listing = getService("listings/900885912");
//        System.out.println(listing);
//
//        String country = getService("countries/212");
//        System.out.println(country);

//        String image = getService("listings/900885912/images/2748646477");
//        System.out.println( image);
//
//        String products = getService("listings/900885912");
//        System.out.println(products);
        String sku = "BG.S.a3UGDVXLuHZTDmHN";
        String[] abc = StringUtils.split(sku, CharPool.DOT);
        System.out.println(abc);
    }

    public static String getService(String method) throws IOException {
        OAuthRequest callRequest = new OAuthRequest(Verb.GET, "https://openapi.etsy.com/v2/" + method, service);
        service.signRequest(accessToken, callRequest);
        return sendRequest(callRequest);
    }

    public static String postService(String method) throws IOException {
        OAuthRequest callRequest = new OAuthRequest(Verb.POST, "https://openapi.etsy.com/v2/"+method, service);
        service.signRequest(accessToken, callRequest);
        return sendRequest(callRequest);
    }

    public static String postService(String method, Map<String, Object> param) throws IOException {
        OAuthRequest callRequest = new OAuthRequest(Verb.POST, "https://openapi.etsy.com/v2/"+method, service);
        callRequest = addParameterMap(callRequest, param);
        service.signRequest(accessToken, callRequest);
        return sendRequest(callRequest);
    }

    public static OAuthRequest addParameterMap(OAuthRequest authRequest, Map<String, Object> param){
        OAuthRequest callRequest = authRequest;
        for (Map.Entry<String, Object> entry : param.entrySet()) {
            callRequest.addParameter(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return callRequest;
    }

    public static String sendRequest(OAuthRequest callRequest) throws IOException {
        Response resp = callRequest.send();

        String results = "";
        try {
            if(!resp.isSuccessful()) {
                SystemError error = new SystemError(resp.getMessage().toUpperCase().replace(" ", "_"),
                        resp.getBody(), "", "http://developer.30usd.com/errors/401.html");
                throw new EtsyException(error);
            }
            results = resp.getBody();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }
}
