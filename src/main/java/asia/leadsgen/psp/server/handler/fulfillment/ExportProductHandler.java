package asia.leadsgen.psp.server.handler.fulfillment;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.FulfillmentService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.CommandUtil;
import asia.leadsgen.psp.util.ExcelWriterUtil;
import asia.leadsgen.psp.util.GetterUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.StringPool;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ExportProductHandler implements Handler<RoutingContext> {
	
	private static String fulfillmentDir;
	private static String fulfillmentHost;
	
	public void setFulfillmentDir(String fulfillmentDir) {
		ExportProductHandler.fulfillmentDir = fulfillmentDir;
	}

	public void setFulfillmentHost(String fulfillmentHost) {
		ExportProductHandler.fulfillmentHost = fulfillmentHost;
	}

	@Override
	public void handle(RoutingContext routingContext) {
		
		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking(future -> {

			try {
				
				JsonObject requestBodyJson = routingContext.getBodyAsJson();
				if (requestBodyJson.isEmpty()) {
					throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
				}
				String fulfillmentIds = requestBodyJson.getString(AppParams.FULFILLMENTS);
				if (StringUtils.isEmpty(fulfillmentIds)) {
					throw new BadRequestException(SystemError.INVALID_FULFILLMENT_IDS);
				}
				int threshold = GetterUtil.getInteger(requestBodyJson.getInteger(AppParams.THRESHOLD), 0);
				String startDate = requestBodyJson.getString(AppParams.START_DATE);
				String endDate = requestBodyJson.getString(AppParams.END_DATE);
				
				List<Map> fulfillments = FulfillmentService.getApparelFulfillment(startDate, endDate, fulfillmentIds);
				List<String> fulfillmentIdSplited = splitByQuantity(fulfillments, threshold);
				LOGGER.info("FulfillmentId Splited : " + fulfillmentIdSplited);
				
				String fileZip = "fulfillment_" + System.currentTimeMillis() + ".zip";
				String timeDir = System.currentTimeMillis() + "/";
				String tempDir = fulfillmentDir + timeDir;
				String url = fulfillmentHost + timeDir + fileZip;
				if (!new File(tempDir).isDirectory()) {
					new File(tempDir).mkdirs();
				}
				
				int numberSuffix = 1;
				int count= 0;
				for (String fulfillmentId : fulfillmentIdSplited) {
					count++;
					String orderBy = "";
					fulfillmentId = String.join(StringPool.COMMA, fulfillmentId);
					String fileName = "";
					if (fulfillmentId.contains(StringPool.COMMA) || count >= (fulfillmentIdSplited.size())) {
						fileName = "DTG_" + String.format("%02d", numberSuffix);
						orderBy = AppParams.S_FULFILLMENT_ID;
						numberSuffix++;
					} else {
						fileName = fulfillmentId;
						orderBy = AppParams.S_PRODUCT_NAME;
					}
					
					List<Map> fulfillmentDetail = FulfillmentService.getApparelFulfillmentDetail(fulfillmentId, orderBy);

					// Export products to excel file
					ExcelWriterUtil.write(fulfillmentDetail, tempDir + fileName);
				}
				
				// Zip folder
				String command = "zip -j " + tempDir + fileZip + " " + tempDir + "*.xlsx";
				LOGGER.info("Zip : " + command);
				CommandUtil.execute(command);
				
				Map responseMap = new LinkedHashMap<>();
				responseMap.put(AppParams.URL, url);
				
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.CREATED.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.CREATED.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, responseMap);

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

	private static List<String> splitByQuantity(List<Map> fulfillments, int threshold) {
		int sum = 0;
		List<String> temp = new ArrayList<String>();
		List<String> fulfillmentIdSplited = new ArrayList<>();
		List<String> fulfillmentIdSplitedAll = new ArrayList<>();
		List<String> fulfillmentIdSplitedSmall = new ArrayList<>();
		List<String> fulfillmentIdSplitedSmallFormated = new ArrayList<>();
		for (int i = 0; i < fulfillments.size(); i++) {
			sum += ParamUtil.getInt(fulfillments.get(i), AppParams.N_QUANTITY);
			temp.add(ParamUtil.getString(fulfillments.get(i), AppParams.S_ID));
			if (sum > threshold) {
				List<String> fulfillmentId = new ArrayList<>();
				fulfillmentId.addAll(temp);
				fulfillmentIdSplited.add(String.join(StringPool.COMMA, fulfillmentId));
				sum = 0;
				temp = new ArrayList<>();
			}
		}
		fulfillmentIdSplitedSmall.addAll(temp);
		if (fulfillmentIdSplitedSmall.size() > 0) {
			fulfillmentIdSplitedSmallFormated.add(String.join(StringPool.COMMA, fulfillmentIdSplitedSmall));
		}
		fulfillmentIdSplitedAll.addAll(fulfillmentIdSplited);
		fulfillmentIdSplitedAll.addAll(fulfillmentIdSplitedSmallFormated);

		return fulfillmentIdSplitedAll;

	}

	private static final Logger LOGGER = Logger.getLogger(ExportProductHandler.class.getName());

}
