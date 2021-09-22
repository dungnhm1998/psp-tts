package asia.leadsgen.psp.server.handler.dropship.order;

import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import asia.leadsgen.psp.obj.DropshipCustomApiItem;
import asia.leadsgen.psp.obj.DropshipCustomApiOrder;
import asia.leadsgen.psp.obj.DropshipStoreObj;
import asia.leadsgen.psp.service_fulfill.DropShipStoreService;
import asia.leadsgen.psp.service_fulfill.DropshipImportFileRowsService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service_fulfill.UploadFileService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.IsoUtil;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceSource;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class DropshipCustomApiOrderCreateV2Handler implements Handler<RoutingContext> {
	
	public static final String LADIES_RACERBACK_TANK_REGEX = "^007-00(?:2|3|4)-00(?:1|2|3|4|5)$";
	public static final String _2D_SHIRT_SKU_REGEX = "^([0-9]{3}-[0-9]{3}-[0-9]{3})$";
	public static final String _3D_ALLOW_2SIDE_DESIGNS = "^(?:MJK|WY|LMS|BX|TX|TK|ZIP)-(?:XS|S|M|L|XL|(?:2|3|4|5)XL)|(?:LMS|WY|TX|ZIP)KID-(?:S|M|L|XL)$";
	
	private DropshipStoreObj store = null;
	
	@Override
	public void handle(RoutingContext routingContext) {
		
		routingContext.vertx().executeBlocking(future -> {
			
			try {
				
				String requestString = routingContext.getBodyAsJson().encode();

				DropshipCustomApiOrder orderRequest = null;

				String[] schemes = { "http", "https" };

				Response response = new Response(true, "Order was added successfully", 200);
				
				if (response.getSuccess() && StringUtils.isNotEmpty(requestString)) {
					orderRequest = new Gson().fromJson(requestString, DropshipCustomApiOrder.class);
				} else {
					response = new Response(false,
							"Bad Request: Sorry there was an error processing your order. Please contact support", 400);
				}
				
				if (response.getSuccess() && StringUtils.isEmpty(orderRequest.getApiKey())) {
					response = new Response(false, "api_key can not be empty.", 401);
				}
				
				if (response.getSuccess()) {
					store = DropShipStoreService.findByApiKey(orderRequest.getApiKey());
					if (store == null) {
						response = new Response(false, "Failed to authenticate.", 401);
					}
				}
				
				if (response.getSuccess()) {
					response = checkAddress(response, orderRequest);
				}
				
				if (response.getSuccess() && CollectionUtils.isEmpty(orderRequest.getItems())) {
					response = new Response(false, "Order items can not be empty.", 400);
				}
				String orderId = null;
				String fileId = null;
				if (response.getSuccess()) {
					if (orderRequest.getSandbox() == false) {
						if (StringUtils.isNotEmpty(orderRequest.getReferenceOrderId())) {
							if (DropshipOrderService.isExistStoreIdReferenceOrderIdSource(store.getId(),
									orderRequest.getReferenceOrderId(), ResourceSource.CUSTOM_API)) {
								response = new Response(false,
										String.format("Order %s is exist!.", orderRequest.getReferenceOrderId()), 400);
							}
						}
						if (response.getSuccess()) {
//							String orderIdPrefix = String.format("%s-%s", store.getUserId(), firstItemBaseShortCode);
							//insert custom api data to TB_DROPSHIP_IMPORT_FILE
							Map customApi = new HashMap<>();
							String fileName = orderRequest.getReferenceOrderId();
							String fileType = "api";
							String userId = store.getUserId();
							String storeId = store.getId();
							String source = ResourceSource.CUSTOM_API;
							customApi = UploadFileService.insert(fileName, "", fileType, userId, storeId, source);
							fileId = ParamUtil.getString(customApi, AppParams.ID);
							
							//insert custom api data to TB_DROPSHIP_IMPORT_FILE_ROWS
							String groupColumn = "";
							try {
								groupColumn = getMD5(fileId + userId + storeId + orderRequest.getReferenceOrderId());
							} catch (Exception e) {
								LOGGER.info("notnull Md5 " + fileId + " fileId " + userId + " userId " + storeId + " storeId "
										+ orderRequest.getReferenceOrderId() + " S_REFERENCE_ORDER ");
							}
							insertCustomApiOrder(fileId, userId, storeId, orderRequest.getReferenceOrderId(), "", orderRequest, source, groupColumn, fileType, fileName);
						}
					} else {
						orderId = "ASAMPLE-FQ79-16899";
					}
				}
				
				Map responseM = new HashMap<String, Object>();
				responseM.put("is_success", response.getSuccess());
				responseM.put("message", response.getMessage());

				if (response.getSuccess()) {
					if (StringUtils.isNotEmpty(orderId)) {
						responseM.put("order_id", orderId);
					}
					if (StringUtils.isNotEmpty(fileId)) {
						responseM.put("log_id", fileId);
					}
				}

				String reasonPhase = response.getCode().intValue() == 200 ? HttpResponseStatus.OK.reasonPhrase()
						: HttpResponseStatus.BAD_REQUEST.reasonPhrase();

				routingContext.put(AppParams.RESPONSE_CODE, response.getCode());
				routingContext.put(AppParams.RESPONSE_MSG, reasonPhase);
				routingContext.put(AppParams.RESPONSE_DATA, responseM);

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
	
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	class Response {
		private Boolean success;
		private String message;
		private Integer code;

		public Response(Boolean success, String message, Integer code) {
			this.success = success;
			this.message = message;
			this.code = code;
		}

		private String shippingId;
	}
	
	private Response checkAddress(Response response, DropshipCustomApiOrder orderRequest) throws SQLException {
		
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingName()))) {
			response = new Response(false, "shipping_name can not be empty.", 400);
		}

		if (response.getSuccess()
				&& (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingAddress1()))) {
			response = new Response(false, "shipping_address1 can not be empty.", 400);
		}
		
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingCity()))) {
			response = new Response(false, "shipping_city can not be empty.", 400);
		}
		
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingState()))) {
			response = new Response(false, "shipping_state can not be empty.", 400);
		}
		
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingZip()))) {
			response = new Response(false, "shipping_zip can not be empty.", 400);
		}
		
		if (response.getSuccess() && (orderRequest != null && StringUtils.isEmpty(orderRequest.getShippingCountry()))) {
			response = new Response(false, "shipping_country can not be empty.", 400);
		}
		
		if (response.getSuccess() && !IsoUtil.isValidISOCountry(orderRequest.getShippingCountry())) {
			response = new Response(false,
					"shipping_country must is ISO Alpha-2 code (https://www.nationsonline.org/oneworld/country_code_list.htm).",
					400);
		}
		
		return response;
	}
	
	
	public static String getMD5(String fileName) throws Exception {
		String original = fileName;
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(original.getBytes());
		byte[] digest = md.digest();
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {
			sb.append(Integer.toHexString((int) (b & 0xff)));
		}

		return sb.toString();
	}
	
	private void insertCustomApiOrder(String fileId, String userId, String storeId, String refOrder, String orderId, DropshipCustomApiOrder orderRequest,
			String source, String groupColumn, String type, String fileName) throws SQLException {
		
		String shippingName = orderRequest.getShippingName();
		String shippingAddress1 = orderRequest.getShippingAddress1();
		String shippingAddress2 = orderRequest.getShippingAddress2();
		String shippingCity = orderRequest.getShippingCity();
		String shippingState = orderRequest.getShippingState();
		String shippingZip = orderRequest.getShippingZip();
		String shippingCountry = orderRequest.getShippingCountry();
		String shippingEmail = orderRequest.getShippingEmail();
		String shippingPhone = orderRequest.getShippingPhone();
		String addressCheck = orderRequest.getIgnoreAddressCheck() ? "1" : "0";
		String minifiedJson = new Gson().toJson(orderRequest);
		
		for (int i = 0; i < orderRequest.getItems().size(); i++) {
			DropshipCustomApiItem orderItem = orderRequest.getItems().get(i);
			
			DropshipImportFileRowsService.insertCustomApiOrder(fileId, userId, storeId, refOrder, orderId, 
					shippingEmail, shippingName, shippingAddress1, shippingAddress2, shippingCity, shippingZip, 
					shippingState, shippingCountry, shippingPhone, addressCheck, orderItem, source, groupColumn, type, fileName, minifiedJson);
		}
	}
	
	private static final Logger LOGGER = Logger.getLogger(DropshipCustomApiOrderCreateV2Handler.class.getName());
}
