package asia.leadsgen.psp.server.handler.payout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.PartnerPayoutObj;
import asia.leadsgen.psp.service.PartnerPayoutService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class InvoiceAdjustHandler implements Handler<RoutingContext> {

	static final String CDN_INVOICE = "https://cdn.burgerprints.com/invoices/";
	static final String PATH_INVOICE = "/opt/burgerprints/cdn/invoices/";
	
//	static final String CDN_INVOICE = "http://cdn.30usd.com/invoices/";
//	static final String PATH_INVOICE = "/usr/share/nginx/30usd.com/cdn/invoices/";

	@Override
	public void handle(RoutingContext routingContext) {

		routingContext.vertx().executeBlocking(future -> {
			String invoiceNumber, url = null, note;
			Double adjustment = 0.00;
			JsonObject requestBodyJson = routingContext.getBodyAsJson();
			LOGGER.info("[Adjustment request] " + requestBodyJson.encode());
			Map<String, Object> requestBody = requestBodyJson.getMap();
			try {
				if (requestBodyJson.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
				}
				invoiceNumber = ParamUtil.getString(requestBody, AppParams.INVOICE_NUMBER);
				if (invoiceNumber == null || invoiceNumber.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_INVOICE_NUMBER);
				}

				adjustment = ParamUtil.getDouble(requestBody, AppParams.ADJUSTMENT, 0.00);

				note = ParamUtil.getString(requestBody, AppParams.NOTE);

				PartnerPayoutObj obj = PartnerPayoutService.getByInvoiceNumber(invoiceNumber);
				if (obj == null || StringUtils.isEmpty(obj.getInvoiceUrl())) {
					throw new BadRequestException(SystemError.INVALID_INVOICE_NUMBER);
				}

				String originInvoicePath = PATH_INVOICE + obj.getInvoiceUrl().replace(CDN_INVOICE, "");
				String partnerName = obj.getInvoiceUrl().replace(CDN_INVOICE, "").split("/")[0];
				String invoiceFileName = obj.getInvoiceUrl().replace(CDN_INVOICE, "").split("/")[1];

				LOGGER.info("*** Making adjusment to file " + originInvoicePath);
				LOGGER.info("*** partnerName= " + partnerName);
				LOGGER.info("*** invoiceFileName= " + invoiceFileName);

				XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(originInvoicePath));
				savingAdjustmentToFile(note, adjustment, partnerName, workbook);

				String newInvoiceFilePath = PATH_INVOICE
						+ obj.getInvoiceUrl().replace(CDN_INVOICE, "").replace(".xlsx", "") + "_adjusted.xlsx";

				String newInvoiceURL = obj.getInvoiceUrl().replace(".xlsx", "") + "_adjusted.xlsx";

				FileOutputStream fos = new FileOutputStream(new File(newInvoiceFilePath));
				workbook.write(fos);
				fos.close();
				workbook.close();

				Map<String, Object> response = new HashMap<>();
				response.put(AppParams.URL, newInvoiceURL);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, response);

				future.complete();

			} catch (Exception e) {
				routingContext.fail(e);
			}
		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	private void savingAdjustmentToFile(String note, Double adjustment, String partnerName, XSSFWorkbook workbook) {
		XSSFSheet sheet = workbook.getSheetAt(0);
		XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
		XSSFCell cellPartner = row.createCell(XLSXInvoiceColumn.PARTNER);
		cellPartner.setCellValue(partnerName);
		XSSFCell cell0 = row.createCell(XLSXInvoiceColumn.ORDER_ID);
		cell0.setCellValue("ADJUSTMENT");

		XSSFCell cell1 = row.createCell(XLSXInvoiceColumn.PRODUCT_NAME);
		cell1.setCellValue(note);

		XSSFCell cell2 = row.createCell(XLSXInvoiceColumn.BURGER_SKU);
		cell2.setCellValue("");

		XSSFCell cell3 = row.createCell(XLSXInvoiceColumn.PARTNER_SKU);
		cell3.setCellValue("");

		XSSFCell cell4 = row.createCell(XLSXInvoiceColumn.SIZE);
		cell4.setCellValue("");

		XSSFCell cell5 = row.createCell(XLSXInvoiceColumn.COLOR_NAME);
		cell5.setCellValue("");

		XSSFCell cell6 = row.createCell(XLSXInvoiceColumn.SHIP_DATE);
		cell6.setCellValue("");

		XSSFCell cell7 = row.createCell(XLSXInvoiceColumn.QUANTITY);
		cell7.setCellValue(1);

		XSSFCell cell8 = row.createCell(XLSXInvoiceColumn.GARMENT_COST);
		cell8.setCellValue(adjustment);

		XSSFCell cell9 = row.createCell(XLSXInvoiceColumn.SHIPPING_COST);
		cell9.setCellValue(Double.parseDouble("0.00"));

		XSSFCell cell10 = row.createCell(XLSXInvoiceColumn.TOTAL);
		cell10.setCellValue(Double.parseDouble("0.00"));

		XSSFCell cell11 = row.createCell(XLSXInvoiceColumn.CUSTOMER);
		cell11.setCellValue("");

		XSSFCell cell12 = row.createCell(XLSXInvoiceColumn.EMAIL);
		cell12.setCellValue("");

		XSSFCell cell13 = row.createCell(XLSXInvoiceColumn.PHONE);
		cell13.setCellValue("");

		XSSFCell cell14 = row.createCell(XLSXInvoiceColumn.ADDLINE1);
		cell14.setCellValue("");

		XSSFCell cell15 = row.createCell(XLSXInvoiceColumn.ADDLINE2);
		cell15.setCellValue("");

		XSSFCell cell16 = row.createCell(XLSXInvoiceColumn.CITY);
		cell16.setCellValue("");

		XSSFCell cell17 = row.createCell(XLSXInvoiceColumn.STATE);
		cell17.setCellValue("");

		XSSFCell cell18 = row.createCell(XLSXInvoiceColumn.ZIP);
		cell18.setCellValue("");

		XSSFCell cell19 = row.createCell(XLSXInvoiceColumn.COUNTRY);
		cell19.setCellValue("");

		XSSFCell cell20 = row.createCell(XLSXInvoiceColumn.CARRIER);
		cell20.setCellValue("");

		XSSFCell cell21 = row.createCell(XLSXInvoiceColumn.TRACKING_CODE);
		cell21.setCellValue("");

		XSSFCell cell22 = row.createCell(XLSXInvoiceColumn.TRACKING_URL);
		cell22.setCellValue("");
	}

	class XLSXInvoiceColumn {
		static final int PARTNER = 0;
		static final int ORDER_ID = 1;
		static final int PRODUCT_NAME = 2;
		static final int BURGER_SKU = 3;
		static final int PARTNER_SKU = 4;
		static final int SIZE = 5;
		static final int COLOR_NAME = 6;
		static final int SHIP_DATE = 7;
		static final int QUANTITY = 8;
		static final int GARMENT_COST = 9;
		static final int SHIPPING_COST = 10;
		static final int TOTAL = 11;
		static final int CUSTOMER = 12;
		static final int EMAIL = 13;
		static final int PHONE = 14;
		static final int ADDLINE1 = 15;
		static final int ADDLINE2 = 16;
		static final int CITY = 17;
		static final int STATE = 18;
		static final int ZIP = 19;
		static final int COUNTRY = 20;
		static final int CARRIER = 21;
		static final int TRACKING_CODE = 22;
		static final int TRACKING_URL = 23;
	}

	private static final Logger LOGGER = Logger.getLogger(InvoiceAdjustHandler.class.getName());
}
