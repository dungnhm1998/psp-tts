package asia.leadsgen.psp.server.handler.dropship.product;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import asia.leadsgen.psp.obj.export.ExportCSVObj;
import asia.leadsgen.psp.obj.export.ExportWooCSVObj;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.StoreOptionService;
import asia.leadsgen.psp.util.CSVUtils;
import asia.leadsgen.psp.util.DataAccessSecurer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.exception.LoginException;
import asia.leadsgen.psp.exception.SystemException;
import asia.leadsgen.psp.obj.export.ExportShopifyCSVObj;
import asia.leadsgen.psp.service.AmazonS3Service;
import asia.leadsgen.psp.service.ProductVariantService;
import asia.leadsgen.psp.util.AppConstants;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ContextUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class CampaignExportProductHandler implements Handler<RoutingContext> {

	private static String path;
	private static String s3PathPrefix;

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking(future -> {

			try {

				String userId = ContextUtil.getString(routingContext, AppParams.USER_ID);

				Map requestMap = routingContext.getBodyAsJson().getMap();

				String campaignId = ParamUtil.getString(requestMap, AppParams.CAMPAIGN_ID);
				DataAccessSecurer.secureCampaign(userId, campaignId);

				String storeId = ParamUtil.getString(requestMap, AppParams.STORE_ID);
				Map dropshipStore = DropShipStoreService.lookUp(storeId);

				if (dropshipStore.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_DROPSHIP_STORE_ID);
				}

				String userIdOfDropshipStore = ParamUtil.getString(dropshipStore, AppParams.USER_ID);
				if (userId.equals(userIdOfDropshipStore) == false) {
					throw new LoginException(SystemError.OPERATION_NOT_PERMITTED);
				}

				String channel = ParamUtil.getString(dropshipStore, AppParams.CHANNEL);
				LOGGER.info("---export " + channel);

				List<Map> listVariantMap = ProductVariantService.getProductVariantsToExport(campaignId, channel);
				if (CollectionUtils.isEmpty(listVariantMap)) {
					throw new BadRequestException(SystemError.INVALID_PRODUCT_VARIANT);
				}

				List<ExportCSVObj> listCSVObj =new ArrayList<>();

				if (AppConstants.WOOCOMMERCE.equalsIgnoreCase(channel)) {
					List<ExportWooCSVObj> allProduct = new ArrayList<>();

					List<Map> listOption = StoreOptionService.lookUp(storeId, AppParams.ATTRIBUTE);

					allProduct.add(ExportWooCSVObj.formatExport(campaignId, listVariantMap, listOption));

					String parentSKU = allProduct.get(0).getSKU();

					for (Map variant : listVariantMap) {
						allProduct.add(ExportWooCSVObj.formatVariationExport(variant, listOption, parentSKU));
					}

					listCSVObj.addAll(allProduct);

				} else if (AppConstants.SHOPIFY.equalsIgnoreCase(channel)) {

					Map<String, List<Map>> groupByCamaign = listVariantMap.stream().collect(Collectors.groupingBy(e -> ParamUtil.getString(e, AppParams.CAMPAIGN_ID)));

					for (Map.Entry<String, List<Map>> eachCampaign : groupByCamaign.entrySet()) {

						Map<String, List<Map>> groupByProduct = eachCampaign.getValue().stream().collect(Collectors.groupingBy(e -> ParamUtil.getString(e, AppParams.ID)));

						for (Map.Entry<String, List<Map>> eachProduct : groupByProduct.entrySet()) {
							List<ExportShopifyCSVObj> lstRowForProduct = ExportShopifyCSVObj.processEachProduct(eachProduct.getValue());
							listCSVObj.addAll(lstRowForProduct);
						}
					}
				}

				Map<String, String> responseData = new LinkedHashMap<>();

				String s3Url = processData(campaignId, dropshipStore, listCSVObj);

				responseData.put(AppParams.URL, s3Url);

				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, responseData);

			} catch (Exception e) {
				routingContext.fail(e);
			}

			future.complete();

		}, asyncResult -> {
			if (asyncResult.succeeded()) {
				routingContext.next();
			} else {
				routingContext.fail(asyncResult.cause());
			}
		});
	}

	public static String processData(String campaignId, Map dropshipStore, List<ExportCSVObj> listCSVObj) {
		
		String channel = ParamUtil.getString(dropshipStore, AppParams.CHANNEL);
		String storeName = ParamUtil.getString(dropshipStore, AppParams.NAME);
		
		String fileName = channel + "-" + storeName + "-" + campaignId + ".csv";

		String pathSaveFile = path + fileName;

		File file = createFileCSV(listCSVObj, pathSaveFile);
		if (file == null) {
			throw new SystemException(SystemError.INTERNAL_SERVER_ERROR);
		}

		String s3KeyName = null;
		if (StringUtils.isNotEmpty(s3PathPrefix)) {
			s3KeyName = s3PathPrefix + StringPool.FORWARD_SLASH + fileName;
		} else {
			s3KeyName = fileName;
		}
		LOGGER.info("---s3KeyName = " + s3KeyName);

		String s3Url = AmazonS3Service.uploadFile(s3KeyName, file);

		if (StringUtils.isEmpty(s3Url)) {
			throw new SystemException(SystemError.INTERNAL_SERVER_ERROR);
		}

		file.delete();
		return s3Url;
	}

	private static File createFileCSV(List<ExportCSVObj> lstRow, String path) {

		File file = new File(path);
		ICsvBeanWriter beanWriter = null;

		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
			writer.write(AppConstants.UTF8_BOM_CHAR);

			beanWriter = new CsvBeanWriter(writer, CsvPreference.STANDARD_PREFERENCE);

			LOGGER.info("lstRow.get(0).getHeaderExport()= " + lstRow.get(0).getHeaderExport());
			beanWriter.writeHeader(lstRow.get(0).getHeaderExport());

			for (ExportCSVObj row : lstRow) {
				beanWriter.write(row, lstRow.get(0).getHeaderMapping());
			}

		} catch (Exception e) {
			LOGGER.severe("-- createFileCSV --msg " + e.getMessage());
			return null;
		} finally {
			try {
				beanWriter.close();
			} catch (IOException e) {
				LOGGER.severe("-- createFileCSV --beanWriter close error --msg " + e.getMessage());
				return null;
			}
		}
		return file;
	}

	public static void setPath(String path) {
		CampaignExportProductHandler.path = path;
	}

	public static void setS3PathPrefix(String s3PathPrefix) {
		CampaignExportProductHandler.s3PathPrefix = s3PathPrefix;
	}

	static Logger LOGGER = Logger.getLogger(CampaignExportProductHandler.class.getName());
}
