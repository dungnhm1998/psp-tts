package asia.leadsgen.psp.test.main;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hungdx on 5/12/17.
 */
public class PSPTestMain {


    public static void main(String... arg) {

        try {

            String url = "https://api.30usd.com/psp/api/v1/promotions?check=KM01&campaign=EdssadrET3aDG56ddf";

            String regexp = "check=([^&]+)&campaign=([^&]+)";

            Pattern p = Pattern.compile(regexp);

            Matcher m = p.matcher(url);

            LOGGER.info(String.valueOf(m.find()));

//            ApplicationContext applicationContext = new ClassPathXmlApplicationContext("app-context.xml");
//
//            String testOrderId = "aPmB7qk-n_07F0Dd"; //XYV0TyBB77aR8UC3
//
//            Map orderInfoMap = OrderService.get(testOrderId, true);
//
//            MailUtil.sendOrderConfirmationEmail(orderInfoMap);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Mail template processing error", e);
        }
    }
//    public static void main(String ...arg){
//
//        try {
//
//            ApplicationContext applicationContext = new ClassPathXmlApplicationContext("app-context.xml");
//
//            String testOrderId = "OUZ3-Et6lFmO6y_i";
//
//            String emailType = "notify";
//
//            String emailSubject = "Order confirmation";
//
//            Map orderInfoMap = OrderService.get(testOrderId, true);
//
//            Map shippingInfoMap = ParamUtil.getMapData(orderInfoMap, AppParams.SHIPPING);
//
//            String customerName = ParamUtil.getString(shippingInfoMap, AppParams.NAME);
//
//            String customerEmail = ParamUtil.getString(shippingInfoMap, AppParams.EMAIL);
//
//            String customerPhone = ParamUtil.getString(shippingInfoMap, AppParams.PHONE);
//
//            Map shippingAddress = ParamUtil.getMapData(shippingInfoMap, AppParams.ADDRESS);
//
//            String shippingLine1 = ParamUtil.getString(shippingAddress, AppParams.LINE1);
//
//            String shippingLine2 = ParamUtil.getString(shippingAddress, AppParams.LINE2);
//
//            String shippingCity = ParamUtil.getString(shippingAddress, AppParams.CITY);
//
//            String shippingState = ParamUtil.getString(shippingAddress, AppParams.STATE);
//
//            String postalCode = ParamUtil.getString(shippingAddress, AppParams.POSTAL_CODE);
//
//            String countryCode = ParamUtil.getString(shippingAddress, AppParams.COUNTRY);
//
//            String orderAmount = ParamUtil.getString(orderInfoMap, AppParams.AMOUNT);
//
//            String orderCurrencyCode = ParamUtil.getString(orderInfoMap, AppParams.CURRENCY);
//
//            String orderCurrency = orderCurrencyCode.equals(CurrencyCodes.USD) ? "$" : "?";
//
//            List<Map> orderItems = ParamUtil.getListData(orderInfoMap, AppParams.ITEMS);
//
//            Double shippingFees = new Double(0);
//            Double subTotal = new Double(0);
//
//            List<Map> orderItemList = new ArrayList<>();
//
//            for(Map orderItem : orderItems){
//
//                Map itemInfo = orderItem;
//
//                String itemPrice = ParamUtil.getString(orderItem, AppParams.PRICE);
//
//                int itemQuantity = ParamUtil.getInt(orderItem, AppParams.QUANTITY);
//
//                subTotal += GetterUtil.getDouble(itemPrice) * itemQuantity;
//
//                String itemShippingFee = ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE);
//
//                shippingFees += GetterUtil.getDouble(itemShippingFee);
//
//                String campaignEndTime = ParamUtil.getString(ParamUtil.getMapData(orderItem, AppParams.CAMPAIGN), AppParams.END_TIME);
//
//                if(!campaignEndTime.isEmpty()){
//
//                    int productShippingArrivalMinDays = GetterUtil.getInteger(PreferencesService.get(PreferenceKeys.PRODUCT_SHIPPING_EXPECTED_ARRIVAL_MIN_DAYS), AppConstants.DEFAULT_PRODUCT_SHIPPING_EXPECTED_ARRIVAL_MIN_DAYS);
//
//                    int productShippingArrivalMaxDays = GetterUtil.getInteger(PreferencesService.get(PreferenceKeys.PRODUCT_SHIPPING_EXPECTED_ARRIVAL_MAX_DAYS), AppConstants.DEFAULT_PRODUCT_SHIPPING_EXPECTED_ARRIVAL_MAX_DAYS);
//
//                    SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);
//
//                    dateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));
//
//                    Date campaignEndDate = dateFormat.parse(campaignEndTime);
//
//                    Calendar minCalendar = Calendar.getInstance();
//                    minCalendar.setTime(campaignEndDate);
//                    minCalendar.add(Calendar.DATE, productShippingArrivalMinDays);
//
//                    Date expectedArrivalMinDate = minCalendar.getTime();
//
//                    Calendar maxCalendar = Calendar.getInstance();
//                    maxCalendar.setTime(campaignEndDate);
//                    maxCalendar.add(Calendar.DATE, productShippingArrivalMaxDays);
//
//                    Date expectedArrivalMaxDate = maxCalendar.getTime();
//
//                    SimpleDateFormat expectedDateFormat = new SimpleDateFormat(AppConstants.DATE_FORMAT_PATTERN_EEE_MMM_YYYY);
//
//                    expectedDateFormat.setTimeZone(TimeZone.getTimeZone(AppConstants.DEFAULT_TIME_ZONE));
//
//                    itemInfo.put(AppParams.EXPECTED_ARRIVAL, expectedDateFormat.format(expectedArrivalMinDate) + " - " + expectedDateFormat.format(expectedArrivalMaxDate));
//                }
//
//                orderItemList.add(itemInfo);
//            }
//
//            String subTotalAmount = AppConstants.DEFAULT_AMOUNT_FORMAT.format(subTotal);
//
//            String totalShippingFees = AppConstants.DEFAULT_AMOUNT_FORMAT.format(shippingFees);
//
//            ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
//            templateResolver.setTemplateMode(TemplateMode.HTML);
//            templateResolver.setCharacterEncoding(AppConstants.CHARSET_UTF8);
//            templateResolver.setPrefix("/templates/");
//            templateResolver.setSuffix(".html");
//
//            TemplateEngine templateEngine = new TemplateEngine();
//            templateEngine.setTemplateResolver(templateResolver);
//
//            Context mailContext = new Context();
//            mailContext.setVariable("customerName", customerName);
//            mailContext.setVariable("customerEmail", customerEmail);
//            mailContext.setVariable("customerPhone", customerPhone);
//            mailContext.setVariable("shippingLine1", shippingLine1);
//            mailContext.setVariable("shippingLine2", shippingLine2);
//            mailContext.setVariable("shippingCountry", shippingCity + ", " + shippingState + ", " + postalCode + ", " + countryCode);
//
//            mailContext.setVariable("subTotalAmount", subTotalAmount);
//            mailContext.setVariable("shippingFees", totalShippingFees);
//            mailContext.setVariable("orderAmount", orderAmount);
//            mailContext.setVariable("orderCurrency", orderCurrency);
//            mailContext.setVariable("orderItemList", orderItemList);
//
//            String mailContent = templateEngine.process("order_placed_mail_template", mailContext);
//
//            LOGGER.info("===========> ORIGINAL LENGTH: " + mailContent.length());
//
//            LOGGER.info("============================== COMPRESSED ==============================");
//
//            HtmlCompressor htmlCompressor = new HtmlCompressor();
//            htmlCompressor.setRemoveComments(true);
//            htmlCompressor.setRemoveMultiSpaces(true);
//            htmlCompressor.setRemoveIntertagSpaces(true);
//            htmlCompressor.setSimpleDoctype(true);
//            htmlCompressor.setRemoveSurroundingSpaces(HtmlCompressor.ALL_TAGS);
//
//            String compressed = htmlCompressor.compress(mailContent.replaceAll("<!doctype[^>]*>\\n", "").replaceAll("<html>", "").replaceAll("</html>", ""));
//
//            LOGGER.info(compressed);
//
//            LOGGER.info("===========> COMPRESSED LENGTH: " + compressed.length());
//
//            MailClient.send(emailType, customerEmail, emailSubject, StringEscapeUtils.escapeHtml(compressed));
//
//        }catch (Exception e){
//            LOGGER.log(Level.SEVERE, "Mail template processing error", e);
//        }
//    }

    private static final Logger LOGGER = Logger.getLogger(PSPTestMain.class.getName());
}
