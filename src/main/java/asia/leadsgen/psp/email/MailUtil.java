package asia.leadsgen.psp.email;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

import asia.leadsgen.psp.data.type.EmailContentType;
import asia.leadsgen.psp.data.type.EmailMarketingEnum;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.MailException;
import asia.leadsgen.psp.obj.EmailObj;
import asia.leadsgen.psp.obj.EmailTemplate;
import asia.leadsgen.psp.obj.OrderItemObj;
import asia.leadsgen.psp.obj.OrderObj;
import asia.leadsgen.psp.obj.PartnerObj;
import asia.leadsgen.psp.service.CampaignService;
import asia.leadsgen.psp.service.DomainService;
import asia.leadsgen.psp.service.EmailMarketingService;
import asia.leadsgen.psp.service.EmailTemplateService;
import asia.leadsgen.psp.service.OrderService;
import asia.leadsgen.psp.service.PaymentService;
import asia.leadsgen.psp.service.PreferencesService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.CurrencyCodes;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import asia.leadsgen.psp.util.StringPool;
import asia.leadsgen.security.wss.AESCrypt;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

/**
 * Created by hungdx on 7/12/17.
 */
public class MailUtil {

	private static final String NOTIFY = "notify";

	public static String emailTrackingUrl;

	public static void setEmailTrackingUrl(String emailTrackingUrl) {
		MailUtil.emailTrackingUrl = emailTrackingUrl;
	}

	public static String getEmailTrackingUrl() {
		return MailUtil.emailTrackingUrl;
	}

	private static String url;

	public static void setUrl(String url) {
		MailUtil.url = url;
	}

	private static String passwordAES;

	public static void setPasswordAES(String passwordAES) {
		MailUtil.passwordAES = passwordAES;
	}

	public static void sendPlainTextOrderConfirmationEmail(OrderObj order) throws SQLException {

		if (order == null || order.getId() == null || order.getId().isEmpty()) {
			return;
		}

		List<OrderItemObj> itemObjs = OrderService.searchItems(order.getId());

		try {

			Map templateMap = getEmailTemplateContent(EmailMarketingEnum.PLAIN_TEXT_ORDER_CONFIRM.getValue());

			String template = ParamUtil.getString(templateMap, "content");
			String subject = ParamUtil.getString(templateMap, "subject");

			Context mailContext = new Context();
			mailContext.setVariable("paymentDate", order.getPaymentObj().getCreateDate());
			String paymentMethod = order.getPaymentObj().getMethod();
			if (paymentMethod.equalsIgnoreCase("paypal")) {
				paymentMethod = "Paypal";
			} else {
				paymentMethod = "Credit or Debit Card";
			}
			mailContext.setVariable("paymentMethod", paymentMethod);

			mailContext.setVariable("customerName", order.getShipping().getCustomerName());
			mailContext.setVariable("customerPhone", order.getShipping().getCustomerPhone());

			String shippingLine1 = order.getShipping().getShippingAddress().getLine1();
			String shippingLine2 = order.getShipping().getShippingAddress().getLine2();

			String shippingCity = order.getShipping().getShippingAddress().getCity();
			String shippingState = order.getShipping().getShippingAddress().getState();
			String postalCode = order.getShipping().getShippingAddress().getPostalCode();
			String countryCode = order.getShipping().getShippingAddress().getCountry();

			mailContext.setVariable("shippingAddress", shippingLine1 + ", " + shippingLine2 + ", " + shippingCity + ", "
					+ shippingState + ", " + postalCode + ", " + countryCode);

			mailContext.setVariable("orderAmount", order.getOrderAmount());
			mailContext.setVariable("trackingCode", order.getTrackingCode());

			String trackingUrlTemplate = PreferencesService.get("tracking.template.url");
			String domainName = order.getDomainObj().getName();
			String trackingUrl = MessageFormat.format(trackingUrlTemplate, domainName);

			mailContext.setVariable("trackingUrl", trackingUrl + order.getTrackingCode());
			mailContext.setVariable("orderItemList", itemObjs);

			TemplateEngine templateEngine = new TemplateEngine();

			String mailContent = templateEngine.process(template, mailContext);

			String content = compress(mailContent);

			if (!("30usd.com".equals(domainName) || "burgerprints.com".equals(domainName))) {
				subject = subject.replaceAll("BurgerPrints", domainName);
			}

			EmailObj emailObj = new EmailObj(NOTIFY, order.getShipping().getCustomerEmail(), subject, content,
					ResourceStates.APPROVED, domainName, EmailContentType.NO_IMAGE.getValue());

//			EmailMarketingService.insert(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY,
//					order.getShipping().getCustomerEmail(), subject, content, domainName, EmailContentType.NO_IMAGE, 0);

			EmailMarketingService.insert(emailObj);

			LOGGER.info("Email marketing queue success!");

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception while sending confirmation email for order: " + order.getId(), e);
		}
	}

	public static void sendPlainTextOrderShippedEmail(OrderObj order, List<String> ffDetailIdList) throws SQLException {
		if (order == null || order.getId() == null || order.getId().isEmpty() || ffDetailIdList == null
				|| ffDetailIdList.isEmpty()) {
			return;
		}

		String ffDetailIds = String.join(StringPool.COMMA, ffDetailIdList);

		List<OrderItemObj> itemObjs = OrderService.searchItemByFfDetailIds(ffDetailIds);

		try {

			Map templateMap = getEmailTemplateContent(EmailMarketingEnum.PLAIN_TEXT_ORDER_SHIPPED.getValue());

			String template = ParamUtil.getString(templateMap, "content");
			String subject = ParamUtil.getString(templateMap, "subject");

			Context mailContext = new Context();
			mailContext.setVariable("paymentDate", order.getPaymentObj().getCreateDate());
			String paymentMethod = order.getPaymentObj().getMethod();
			if (paymentMethod.equalsIgnoreCase("paypal")) {
				paymentMethod = "Paypal";
			} else {
				paymentMethod = "Credit or Debit Card";
			}
			mailContext.setVariable("paymentMethod", paymentMethod);

			mailContext.setVariable("customerName", order.getShipping().getCustomerName());
			mailContext.setVariable("customerPhone", order.getShipping().getCustomerPhone());

			String shippingLine1 = order.getShipping().getShippingAddress().getLine1();
			String shippingLine2 = order.getShipping().getShippingAddress().getLine2();

			String shippingCity = order.getShipping().getShippingAddress().getCity();
			String shippingState = order.getShipping().getShippingAddress().getState();
			String postalCode = order.getShipping().getShippingAddress().getPostalCode();
			String countryCode = order.getShipping().getShippingAddress().getCountry();

			mailContext.setVariable("shippingAddress", shippingLine1 + ", " + shippingLine2 + ", " + shippingCity + ", "
					+ shippingState + ", " + postalCode + ", " + countryCode);

			mailContext.setVariable("orderAmount", order.getOrderAmount());
			mailContext.setVariable("trackingCode", order.getTrackingCode());

			String trackingUrlTemplate = PreferencesService.get("tracking.template.url");
			String domainName = order.getDomainObj().getName();
			String trackingUrl = MessageFormat.format(trackingUrlTemplate, domainName);

			mailContext.setVariable("trackingUrl", trackingUrl + order.getTrackingCode());
			mailContext.setVariable("orderItemList", itemObjs);

			TemplateEngine templateEngine = new TemplateEngine();

			String mailContent = templateEngine.process(template, mailContext);

			String content = compress(mailContent);

			if (!("30usd.com".equals(domainName) || "burgerprints.com".equals(domainName))) {
				subject = subject.replaceAll("BurgerPrints", domainName);
			}

			EmailObj emailObj = new EmailObj(NOTIFY, order.getShipping().getCustomerEmail(), subject, content,
					ResourceStates.APPROVED, domainName, EmailContentType.NO_IMAGE.getValue());
			EmailMarketingService.insert(emailObj);

//			EmailMarketingService.insert(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY,
//					order.getShipping().getCustomerEmail(), subject, content, domainName, EmailContentType.NO_IMAGE, 0);

			LOGGER.info("Email marketing queue success!");

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception while sending shipped email for order: " + order.getId(), e);
		}
	}

	public static void sendOrderConfirmationEmail(Map orderInfoMap) {

		String orderId = ParamUtil.getString(orderInfoMap, AppParams.ID);

		try {

			LOGGER.info("Processing email marketing for order: " + orderId);

			Map paymentSearch = PaymentService.search_by_order(orderId, ResourceStates.APPROVED, false);
			Map paymentInfo = new HashMap();
			List<Map> paymentItems = ParamUtil.getListData(paymentSearch, AppParams.PAYMENTS);
			if (paymentItems.size() > 0) {
				paymentInfo = paymentItems.get(0);
			}

			Map templateMap = getEmailTemplateContent(AppConstants.ORDER_CONFIRMATION_EMAIL_MARKETING_TYPE);
			String template = ParamUtil.getString(templateMap, "content");
			String subject = ParamUtil.getString(templateMap, "subject");

			Context mailContext = new Context();

			String paymentDate = ParamUtil.getString(paymentInfo, AppParams.CREATE_TIME);

			SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DEFAULT_DATE_TIME_FORMAT_PATTERN);

			Date payDate = dateFormat.parse(paymentDate);
			
			Calendar c1 = Calendar.getInstance();
	        Calendar c2 = Calendar.getInstance();
	        Calendar c3 = Calendar.getInstance();
	        
	        c1.setTime(payDate);
	        c2.setTime(payDate);
	        c3.setTime(payDate);
	        
	        c1.add(Calendar.DATE, 10);
	        c2.add(Calendar.DATE, 21);
	        c3.add(Calendar.DATE, 1);
	        
	        Date after10Days = c1.getTime();
	        Date after21Days = c2.getTime();
	        Date after24Hour = c3.getTime();
	        
	        dateFormat = new SimpleDateFormat("E MMM dd yyyy, K:mm aaa z");
	        paymentDate = dateFormat.format(payDate);
			String changeOrderDate = dateFormat.format(after24Hour);

	        dateFormat = new SimpleDateFormat("MMM dd yyyy");
			String arrivedDate1 = dateFormat.format(after10Days);
			String arrivedDate2 = dateFormat.format(after21Days);

			mailContext.setVariable("paymentDate", paymentDate);
			mailContext.setVariable("arrivedDate1", arrivedDate1);
			mailContext.setVariable("arrivedDate2", arrivedDate2);
			mailContext.setVariable("changeOrderDate", changeOrderDate);
			
			String paymentMethod = ParamUtil.getString(paymentInfo, AppParams.METHOD);
			if (paymentMethod.equalsIgnoreCase("paypal")) {
				paymentMethod = "Paypal";
			} else {
				paymentMethod = "Credit or Debit Card";
			}
			mailContext.setVariable("paymentMethod", paymentMethod);

			Map shippingInfoMap = ParamUtil.getMapData(orderInfoMap, AppParams.SHIPPING);

			String customerEmail = ParamUtil.getString(shippingInfoMap, AppParams.EMAIL);

			mailContext.setVariable("customerName", ParamUtil.getString(shippingInfoMap, AppParams.NAME));
			mailContext.setVariable("customerEmail", customerEmail);
			mailContext.setVariable("customerPhone", ParamUtil.getString(shippingInfoMap, AppParams.PHONE));

			Map shippingAddress = ParamUtil.getMapData(shippingInfoMap, AppParams.ADDRESS);
			mailContext.setVariable("shippingLine1", ParamUtil.getString(shippingAddress, AppParams.LINE1));
			mailContext.setVariable("shippingLine2", ParamUtil.getString(shippingAddress, AppParams.LINE2));

			String shippingCity = ParamUtil.getString(shippingAddress, AppParams.CITY);
			String shippingState = ParamUtil.getString(shippingAddress, AppParams.STATE);
			String postalCode = ParamUtil.getString(shippingAddress, AppParams.POSTAL_CODE);
			String countryCode = ParamUtil.getString(shippingAddress, AppParams.COUNTRY);

			mailContext.setVariable("shippingCountry",
					shippingCity + ", " + shippingState + ", " + postalCode + ", " + countryCode);

			String orderAmount = ParamUtil.getString(orderInfoMap, AppParams.AMOUNT);
			String orderCurrency = ParamUtil.getString(orderInfoMap, AppParams.CURRENCY).equals(CurrencyCodes.USD) ? "$"
					: "?";

			mailContext.setVariable("orderAmount", orderAmount);
			mailContext.setVariable("orderCurrency", orderCurrency);

			String tracking_code = ParamUtil.getString(orderInfoMap, AppParams.TRACKING);
			mailContext.setVariable("tracking_code", tracking_code);

			String trackingUrlTemplate = PreferencesService.get("tracking.template.url");
			String domainName = ParamUtil.getString(orderInfoMap, AppParams.DOMAIN);
			String trackingUrl = MessageFormat.format(trackingUrlTemplate, domainName);

			String domainLogo = null, domainUrl = "https://" + domainName;
			Map domainInfoMap = DomainService.search(domainName);

			if (domainInfoMap != null && !domainInfoMap.isEmpty()) {
				domainLogo = ParamUtil.getString(domainInfoMap, AppParams.LOGO);
			}

			mailContext.setVariable("tracking_url", trackingUrl + tracking_code);

			List<Map> orderItems = ParamUtil.getListData(orderInfoMap, AppParams.ITEMS);

			Double shippingFees = new Double(0);
			Double subTotal = new Double(0);

			List<Map> orderItemList = new ArrayList<>();

			for (Map orderItem : orderItems) {

				Map itemInfo = orderItem;

				String itemPrice = ParamUtil.getString(orderItem, AppParams.PRICE);
				int itemQuantity = ParamUtil.getInt(orderItem, AppParams.QUANTITY);
				subTotal += GetterUtil.getDouble(itemPrice) * itemQuantity;
				String itemShippingFee = ParamUtil.getString(orderItem, AppParams.SHIPPING_FEE);
				shippingFees += GetterUtil.getDouble(itemShippingFee);
				Double subtotalItem = GetterUtil.getDouble(itemPrice) * itemQuantity;// +
				String subTotalAmount = AppConstants.DEFAULT_AMOUNT_FORMAT.format(subtotalItem);
				itemInfo.put("subTotalAmount", subTotalAmount);
				orderItemList.add(itemInfo);
			}

			String subTotalAmount = AppConstants.DEFAULT_AMOUNT_FORMAT.format(subTotal);
			String totalShippingFees = AppConstants.DEFAULT_AMOUNT_FORMAT.format(shippingFees);

			mailContext.setVariable("subTotalAmount", subTotalAmount);
			mailContext.setVariable("shippingFees", totalShippingFees);
			mailContext.setVariable("orderItemList", orderItemList);
			mailContext.setVariable("domain_logo", domainLogo);
			mailContext.setVariable("domain_url", domainUrl);
			mailContext.setVariable("domain_name", domainName.substring(0, domainName.indexOf('.')));
			mailContext.setVariable("domain", domainName);

//			String campaignId = OrderService.findFirstCampaignOfOrder(orderId);
//			if (campaignId != null && campaignId.isEmpty() == false) {
//				Map<String, Object> upsell = CampaignUpsellService.get(campaignId, "post-sale-popup", "", "approved");
//				int total = ParamUtil.getInt(upsell, AppParams.TOTAL, 0);
//				List<Map<String, Object>> upsellList = ParamUtil.getListData(upsell, AppParams.UPSELL_CAMPAIGN);
//				mailContext.setVariable("upsell_total", total);
//				if (total > 0 && upsellList != null && total > 0 && upsellList.isEmpty() == false) {
//					NumberFormat formatter = new DecimalFormat("#0.00");
//					double price, discountValue;
//					for (Map<String, Object> cupsell : upsellList) {
//						price = ParamUtil.getDouble(cupsell, "price", 0d);
//						discountValue = ParamUtil.getDouble(cupsell, "upsell_discount_value", 0d);
//						cupsell.put("upsell_discount_value", formatter.format(discountValue));
//						cupsell.put("upsell_discounted_value", formatter.format(price - discountValue));
//					}
//					mailContext.setVariable("upsell_data", upsellList);
//				}
//			}

			TemplateEngine templateEngine = new TemplateEngine();

			String content = compress(templateEngine.process(template, mailContext));

			if (!("30usd.com".equals(domainName) || "burgerprints.com".equals(domainName))) {
				subject = subject.replaceAll("BurgerPrints", domainName);
			}
			EmailObj emailObj = new EmailObj(NOTIFY, customerEmail, subject, content, ResourceStates.APPROVED,
					domainName, EmailContentType.IMAGE.getValue());
			EmailMarketingService.insert(emailObj);

//			EmailMarketingService.insert(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY, customerEmail, subject, content,
//					domainName, EmailContentType.IMAGE, sendAfterDays);

			LOGGER.info("Email marketing queue success!");

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception while sending confirmation email for order: " + orderId, e);
		}
	}

	public static void sendOrderUpsellEmail(String orderId, String customerEmail, Map upsellInfoMap)
			throws SQLException {

		int total = ParamUtil.getInt(upsellInfoMap, AppParams.TOTAL, 0);
		if (0 < total) {

			List<Map> upsells = ParamUtil.getListData(upsellInfoMap, AppParams.UPSELL_CAMPAIGN);

			if (!upsells.isEmpty()) {
				Map upsell = upsells.get(0);
				String campaignName = ParamUtil.getString(upsell, AppParams.UPSELL_CAMPAIGN_NAME);
				String upsellDiscountType = ParamUtil.getString(upsell, AppParams.UPSELL_DISCOUNT_TYPE);
				double discountAmount = ParamUtil.getDouble(upsell, AppParams.UPSELL_DISCOUNT_VALUE, 0);
				String imageUrl = ParamUtil.getString(upsell, AppParams.UPSELL_VARIANT_URL);
				String itemName = ParamUtil.getString(upsell, AppParams.UPSELL_VARIANT_NAME);
				double price = ParamUtil.getDouble(upsell, AppParams.PRICE);
				double salePrice = price;

				if ("fix".equals(upsellDiscountType)) {
					salePrice = price - discountAmount;
				}

				String campaignUri = ParamUtil.getString(upsell, AppParams.URL);
				String domain = ParamUtil.getString(upsell, AppParams.DOMAIN);

				String encodedUpsellOrderId;
				AESCrypt aesCrypt = new AESCrypt();
				encodedUpsellOrderId = aesCrypt.encrypt(passwordAES, orderId);

				Map domainInfo = DomainService.domainSearch(domain);

				String domainUrl = "https://" + domain;
				String domainLogo = ParamUtil.getString(domainInfo, AppParams.LOGO);

				String buttonBuyUrl = domainUrl + "/shop/" + campaignUri + "?upsell=" + encodedUpsellOrderId;

				Context mailContext = new Context();
				mailContext.setVariable("campaignName", campaignName);
				mailContext.setVariable("discountAmount", AppConstants.DEFAULT_AMOUNT_FORMAT.format(discountAmount));
				mailContext.setVariable("imageUrl", imageUrl);
				mailContext.setVariable("itemName", itemName);
				mailContext.setVariable("price", AppConstants.DEFAULT_AMOUNT_FORMAT.format(price));
				mailContext.setVariable("salePrice", AppConstants.DEFAULT_AMOUNT_FORMAT.format(salePrice));
				mailContext.setVariable("buttonBuyUrl", buttonBuyUrl);
				mailContext.setVariable("domain", domain);
				mailContext.setVariable("domain_url", domainUrl);
				mailContext.setVariable("domain_logo", domainLogo);

				boolean getContent = true;

				Map mailTemplateSearchResultMap = EmailTemplateService.search("order_upsell_email",
						ResourceStates.APPROVED, getContent);

				int totalTemplate = ParamUtil.getInt(mailTemplateSearchResultMap, AppParams.TOTAL);

				if (totalTemplate <= 0) {
					throw new MailException(SystemError.INVALID_MAIL_TEMPLATE);
				}

				List<Map> mailTemplateList = ParamUtil.getListData(mailTemplateSearchResultMap, AppParams.TEMPLATES);

				String subject = ParamUtil.getString(mailTemplateList.get(0), AppParams.SUBJECT);

				if (!("30usd.com".equals(domain) || "burgerprints.com".equals(domain))) {
					subject = subject.replaceAll("BurgerPrints", domain);
				}

				String template = ParamUtil.getString(mailTemplateList.get(0), AppParams.CONTENT);

				TemplateEngine templateEngine = new TemplateEngine();

				String mailContent = templateEngine.process(template, mailContext);
				String content = compress(mailContent);
//				int sendAfterDays = 1;

				EmailObj emailObj = new EmailObj(NOTIFY, customerEmail, subject, content, ResourceStates.APPROVED,
						domain, EmailContentType.IMAGE.getValue(), 1);
				EmailMarketingService.insert(emailObj);

//				EmailMarketingService.insert(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY, customerEmail, subject, content,
//						domain, EmailContentType.IMAGE, sendAfterDays);

				LOGGER.info("Email marketing queue success!");

			}
		}
	}

	/**
	 *
	 * @param domain
	 * @param banner
	 * @param logo
	 * @param title
	 * @param description
	 * @param campaignIds
	 * @param templateContent
	 * @return
	 */
	public static String processMarketingEmailCampaignContent(String emailCampId, String domainId, String domain,
			String banner, String logo, String title, String description, String campaignIds, String templateContent,
			int templateColumn, String prCode, boolean isPreview) {
		String mailContent = "";
		try {

			List<Map> campaigns = CampaignService.getInfoForEmailMarketing(emailCampId, campaignIds, prCode, isPreview);
			if (templateColumn != 1) {
				int remove = campaigns.size() % templateColumn;
				for (int i = 1; i <= remove; i++) {
					campaigns.remove(campaigns.size() - i);
				}
			}

			LOGGER.info("campaigns=" + campaigns.size());

			Context mailContext = new Context();
			mailContext.setVariable(AppParams.DOMAIN, "https://" + domain);
			mailContext.setVariable(AppParams.DOMAIN_NAME, domain);
			mailContext.setVariable(AppParams.BANNER, banner);
			mailContext.setVariable(AppParams.LOGO, logo);
			mailContext.setVariable(AppParams.TITLE, title);
			mailContext.setVariable(AppParams.DESCRIPTION, description);
			mailContext.setVariable(AppParams.CAMPAIGNS, campaigns);
			Date date = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			mailContext.setVariable(AppParams.YEAR, cal.get(Calendar.YEAR));
			if (!isPreview) {
				mailContext.setVariable(AppParams.TRACKING_URL, emailTrackingUrl + "?action=open&mail_camp="
						+ emailCampId + "&email=" + AppConstants.X_CUSTOMER_EMAIL);
				mailContext.setVariable(AppParams.UNSUBSCRIBE_URL, emailTrackingUrl + "?action=unsubcribe&mail_camp="
						+ emailCampId + "&domain=" + domainId + "&email=" + AppConstants.X_CUSTOMER_EMAIL);
			}

			TemplateEngine templateEngine = new TemplateEngine();
			mailContent = templateEngine.process(templateContent, mailContext);
			mailContent = compress(mailContent);

			return mailContent;

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "process marketing email campaign: " + e);
		}
		return mailContent;
	}

	private static Map getEmailTemplateContent(String emailType) throws SQLException {
		boolean getContent = true;
		Map mailTemplateSearchResultMap = EmailTemplateService.search(emailType, ResourceStates.APPROVED, getContent);
		int totalTemplate = ParamUtil.getInt(mailTemplateSearchResultMap, AppParams.TOTAL);
		if (totalTemplate <= 0) {
			throw new MailException(SystemError.INVALID_MAIL_TEMPLATE);
		}
		List<Map> mailTemplateList = ParamUtil.getListData(mailTemplateSearchResultMap, AppParams.TEMPLATES);

		Map template = new LinkedHashMap<>();
		template.put("content", ParamUtil.getString(mailTemplateList.get(0), AppParams.CONTENT));
		template.put("subject", ParamUtil.getString(mailTemplateList.get(0), AppParams.SUBJECT));

		return template;
	}

	public static void sendWithdrawConfirmationEmail(String userEmail, String amount, String payoutMethod,
			String paypalEmail, String paypalFirstName, String paypalLastName, String payoneerEmail, String pingpongEmail,
			String wireTransferAccountName, String wireTransferAccountNumber, String wireTransferAccountCountry,
			String wireTransferAccountNameRoutingNumber, String confirmCode, String id) {

		try {
			Map templateMap = getEmailTemplateContent(AppConstants.WITHDRAW_CONFIRMATION_EMAIL_TYPE);
			String template = ParamUtil.getString(templateMap, "content");
			String subject = ParamUtil.getString(templateMap, "subject");

			Context mailContext = new Context();

			String paymentMethodHTMLFormat = "";

			if (payoutMethod.equalsIgnoreCase("paypal")) {
				paymentMethodHTMLFormat = "<li>\r\n" + "	Paypal Email: " + paypalEmail + "\r\n" + "</li>\r\n"
						+ "<li>\r\n" + "	First Name: " + paypalFirstName + "\r\n" + "</li>\r\n" + "<li>\r\n"
						+ "	Last Name: " + paypalLastName + "\r\n" + "</li>";

			} else if (payoutMethod.equalsIgnoreCase("payoneer")) {
				paymentMethodHTMLFormat = "<li>\r\n" + "	Payoneer Email: " + payoneerEmail + "\r\n" + "</li>";
				
			} else if (payoutMethod.equalsIgnoreCase("pingpong")) {
				paymentMethodHTMLFormat = "<li>\r\n" + "	PingPong Email: " + pingpongEmail + "\r\n" + "</li>";	

			} else if (payoutMethod.equalsIgnoreCase("wire_transfer")) {
				paymentMethodHTMLFormat = "<li>\r\n" + "	Account Name: " + wireTransferAccountName + "\r\n"
						+ "</li>\r\n" + "<li>\r\n" + "	Account Number: " + wireTransferAccountNumber + "\r\n"
						+ "</li>\r\n" + "<li>\r\n" + "	Account Country: " + wireTransferAccountCountry + "\r\n"
						+ "</li>\r\n" + "<li>\r\n" + "	Routing Number: " + wireTransferAccountNameRoutingNumber
						+ "\r\n" + "</li>";
			}

			mailContext.setVariable("paymentMethodHTMLFormat", paymentMethodHTMLFormat);

			mailContext.setVariable("amount", amount);
			mailContext.setVariable("code", confirmCode);
			mailContext.setVariable("id", id);

			TemplateEngine templateEngine = new TemplateEngine();

			String content = compress(templateEngine.process(template, mailContext));
//            int sendAfterDays = 0;

			EmailObj emailObj = new EmailObj(NOTIFY, userEmail, subject, content, ResourceStates.APPROVED, "",
					EmailContentType.IMAGE.getValue());
			EmailMarketingService.insert(emailObj);

//			EmailMarketingService.insert("", userEmail, userEmail, content, null, EmailContentType.IMAGE,
//					sendAfterDays);

			LOGGER.info("Email marketing queue success!");

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception while sending confirmation email for user: " + userEmail, e);
		}

	}

	public static int send(String type, String receiver, String subject, String content) {

		int responseCode = 0;

		HttpURLConnection httpConnection = null;

		try {

			String espRequestURL = url + "/esp/api/v1/email";

			Map espRequestBodyMap = new LinkedHashMap();
			espRequestBodyMap.put(AppParams.TYPE, type);

			Map emailInfoMap = new LinkedHashMap();
			emailInfoMap.put(AppParams.RECEIVE, receiver);
			emailInfoMap.put(AppParams.SUBJECT, subject);
			emailInfoMap.put(AppParams.BODY, content);

			espRequestBodyMap.put(AppParams.EMAIL, emailInfoMap);

			String espRequestBody = new JsonObject(espRequestBodyMap).encode();

			URL url = new URL(espRequestURL);

			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setDoOutput(true);
			httpConnection.setDoInput(true);
			httpConnection.setRequestMethod(HttpMethod.POST.name());
			httpConnection.setRequestProperty("Accept", "application/json");
			httpConnection.setRequestProperty("Content-Type", "application/json");

			LOGGER.info("[ESP REQUEST] URL: " + httpConnection.getURL());
			LOGGER.info("[ESP REQUEST] METHOD: " + httpConnection.getRequestMethod());
			LOGGER.info("[ESP REQUEST] HEADERS: " + httpConnection.getRequestProperties().toString());
			LOGGER.info("[ESP REQUEST] BODY: " + espRequestBody);

			OutputStreamWriter streamWriter = new OutputStreamWriter(httpConnection.getOutputStream());
			streamWriter.write(espRequestBody);
			streamWriter.flush();

			responseCode = httpConnection.getResponseCode();

			String responseMsg = httpConnection.getResponseMessage();

//            LOGGER.info("[ESP RESPONSE] CODE: " + responseCode);
//            LOGGER.info("[ESP RESPONSE] MESSAGE: " + responseMsg);
//            LOGGER.info("[ESP RESPONSE] HEADERS: " + httpConnection.getHeaderFields().toString());

			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {

				InputStreamReader inputStreamReader = new InputStreamReader(httpConnection.getInputStream());

				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				StringBuffer responseBodyBuffer = new StringBuffer();

				String inputLine;

				while ((inputLine = bufferedReader.readLine()) != null) {
					responseBodyBuffer.append(inputLine);
				}

				bufferedReader.close();

				LOGGER.info("[ESP RESPONSE] BODY: " + responseBodyBuffer.toString());
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ERROR]", e);
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}

		return responseCode;
	}

	public static void finishCreateLabelNotify(PartnerObj partnerObj, String fulfillmentInfo, String fileUrl,
			int success, int total) throws SQLException {

		Map templateMap = getEmailTemplateContent(EmailMarketingEnum.FINISH_CREATE_LABEL.getValue());
		String template = ParamUtil.getString(templateMap, "content");
		String subject = ParamUtil.getString(templateMap, "subject");
		subject = String.format(subject, fulfillmentInfo);

		Context mailContext = new Context();
		mailContext.setVariable(AppParams.NAME, partnerObj.getName());
		mailContext.setVariable(AppParams.URL, fileUrl);
		mailContext.setVariable("success", success);
		mailContext.setVariable("total", total);

		TemplateEngine engine = new TemplateEngine();
		String content = engine.process(template, mailContext);

		EmailObj emailObj = new EmailObj(NOTIFY, partnerObj.getEmail(), subject, content, ResourceStates.APPROVED, "",
				EmailContentType.IMAGE.getValue());
		EmailMarketingService.insert(emailObj);

//		EmailMarketingService.insert(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY, partnerObj.getEmail(), subject, content,
//				"", EmailContentType.IMAGE, 0);

	}

	public static void shopifyChargeFailNotify(String orderId, String username, String email) throws SQLException {

		String receiver = "hungdt@leadsgen.asia";
		String subject = "[ Burgerprints ] Shopify payment failed " + orderId;
		String content = "order id = " + orderId;
		EmailObj emailObj1 = new EmailObj(NOTIFY, receiver, subject, content, ResourceStates.APPROVED, "",
				EmailContentType.IMAGE.getValue());
		EmailMarketingService.insert(emailObj1);

		Map templateMap = getEmailTemplateContent(EmailMarketingEnum.SHOPIFY_CHARGE_FAIL.getValue());
		String template = ParamUtil.getString(templateMap, "content");
		subject = ParamUtil.getString(templateMap, "subject");
		subject = String.format(subject, orderId);

		if (StringUtils.isEmpty(username)) {
			username = email;
		}

		Context mailContext = new Context();
		mailContext.setVariable(AppParams.NAME, username);
		mailContext.setVariable(AppParams.ORDER_ID, orderId);

		TemplateEngine engine = new TemplateEngine();
		content = engine.process(template, mailContext);

		EmailObj emailObj2 = new EmailObj(NOTIFY, email, subject, content, ResourceStates.APPROVED, "",
				EmailContentType.IMAGE.getValue());
		EmailMarketingService.insert(emailObj2);

	}

	public static String compress(String source) {

		HtmlCompressor htmlCompressor = new HtmlCompressor();
		htmlCompressor.setRemoveComments(true);
		htmlCompressor.setRemoveMultiSpaces(true);
		htmlCompressor.setRemoveIntertagSpaces(true);
		htmlCompressor.setSimpleDoctype(true);
		htmlCompressor.setRemoveSurroundingSpaces(HtmlCompressor.ALL_TAGS);

		return htmlCompressor.compress(
				source.replaceAll("<!doctype[^>]*>\\n", "").replaceAll("<html>", "").replaceAll("</html>", ""));
	}

	public static void sendEmailResetPassword(String email, String name, String domain, String token, String type)
			throws SQLException {

		try {
			Map mailTemplateSearchResultMap = getEmailTemplateContent(type);

			String subject = ParamUtil.getString(mailTemplateSearchResultMap, AppParams.SUBJECT);
			String content = ParamUtil.getString(mailTemplateSearchResultMap, AppParams.CONTENT);

			String domainLogo = null, domainUrl = "https://" + domain;
			Map domainInfoMap = DomainService.search(domain);

			if (domainInfoMap != null && !domainInfoMap.isEmpty()) {
				domainLogo = ParamUtil.getString(domainInfoMap, AppParams.LOGO);
			}

			subject = subject.replace("[Shopper]", domain);

			content = content.replaceAll("<username>", name).replaceAll("<token>", token).replaceAll("<domain>", domain)
					.replaceAll("<logo>", domainLogo);

			EmailObj emailObj2 = new EmailObj(NOTIFY, email, subject, content, ResourceStates.APPROVED, "",
					EmailContentType.IMAGE.getValue());
			EmailMarketingService.insert(emailObj2);

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception while sending confirmation email for user: " + email, e);
		}
	}
	
	public static void sendNotificationEmailToPartner(Map partnerInfoMap) {

		String partnerId = ParamUtil.getString(partnerInfoMap, AppParams.ID);

		try {

			LOGGER.info("Processing notification email for partner: " + partnerId);
			EmailTemplate emailTemplate = EmailTemplateService.getByType(AppConstants.PARTNER_NOTIFICATION_EMAIL);
			if (emailTemplate == null) {
				LOGGER.severe("Missing email template which key= " + AppConstants.PARTNER_NOTIFICATION_EMAIL);
			} else {
				String email = ParamUtil.getString(partnerInfoMap, AppParams.EMAIL);
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE MMM dd, yyyy");
				String date = dateFormat.format(new Date());

				Context mailContext = new Context();
				mailContext.setVariable(AppParams.EMAIL, email);
				mailContext.setVariable(AppParams.NAME, ParamUtil.getString(partnerInfoMap, AppParams.NAME));
				mailContext.setVariable(AppParams.ASSIGNED_PRODUCTS,
						ParamUtil.getString(partnerInfoMap, AppParams.ASSIGNED_PRODUCTS));
				mailContext.setVariable(AppParams.ASSIGNED_CAMPAIGNS,
						ParamUtil.getString(partnerInfoMap, AppParams.ASSIGNED_CAMPAIGNS));
				mailContext.setVariable(AppParams.DATE, date);

				TemplateEngine templateEngine = new TemplateEngine();

				String mailContent = templateEngine.process(emailTemplate.getContent(), mailContext);
				String content = compress(mailContent);
				int sendAfterDays = 0;

				EmailObj obj = new EmailObj(AppConstants.EMAIL_MARKETING_TYPE_NOTIFY, email, emailTemplate.getSubject(),
						content, "pending", "", "image", sendAfterDays);
				EmailMarketingService.insert(obj);

				LOGGER.info("Email marketing queue success!");
			}

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception while sending notification email for partner ", e);
		}
	}

	private static final Logger LOGGER = Logger.getLogger(MailUtil.class.getName());

}
