/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.webhook;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.obj.OrderToFinanceObj;
import asia.leadsgen.psp.obj.TopupHistoryObj;
import asia.leadsgen.psp.server.handler.dropship.payment.DropshipTopupExecuteHandler;
import asia.leadsgen.psp.service.InvoiceService;
import asia.leadsgen.psp.service.OrderToFinanceService;
import asia.leadsgen.psp.service.PaymentService;
import asia.leadsgen.psp.service_fulfill.DropshipOrderService;
import asia.leadsgen.psp.service_fulfill.TopupHistoryService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import asia.leadsgen.psp.util.ResourceStates;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 *
 * @author HIEPHV
 */
public class PaypalIPNHandler implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {

		if (routingContext.getBodyAsString() == null || routingContext.getBodyAsString().isEmpty()) {
			throw new BadRequestException(SystemError.INVALID_REQUEST_BODY);
		}

		routingContext.vertx().executeBlocking((Future<Object> future) -> {
			try {

				JsonObject requestBody = routingContext.getBodyAsJson();
				LOGGER.info("requestBody=" + requestBody.toString());

				String resource_type = requestBody.getString("resource_type"); // invoices ,plan,product..
				Map resourceMap = ParamUtil.getMapData(requestBody.getMap(), "resource");

				if ("invoices".equalsIgnoreCase(resource_type)) {

					Map invoiceMap = ParamUtil.getMapData(resourceMap, "invoice");
					String saleId = ParamUtil.getString(invoiceMap, "id");
					String status = ParamUtil.getString(invoiceMap, "status");

					Map paymentsMap = ParamUtil.getMapData(invoiceMap, "payments");

					Map paidAmountMap = ParamUtil.getMapData(paymentsMap, "paid_amount");
					Double currentMoneyPaid = ParamUtil.getDouble(paidAmountMap, "value");

					List<Map> transactionsMaps = ParamUtil.getListData(paymentsMap, "transactions");
					String transactionId = "";
					
					if (transactionsMaps.size() > 0) {

						transactionId = ParamUtil.getString(transactionsMaps.get(0), "payment_id");
					}

					Map detailMap = ParamUtil.getMapData(invoiceMap, "detail");

					String invoiceNumber = ParamUtil.getString(detailMap, "invoice_number");

					switch (status.toLowerCase()) {
					case "paid":

						if (invoiceNumber.startsWith("iv_")) {
							PaymentService.paypalUpdateStateBySaleIdOtherDropship(saleId, transactionId, "approved");
							InvoiceService.UpdateStateId(invoiceNumber, "paid");
							saveOrderToFinanceQueue(invoiceNumber);
						} else if (invoiceNumber.startsWith("topup_")) {
							PaymentService.paypalUpdateStateBySaleIdOtherDropship(saleId, transactionId, "approved");
							InvoiceService.UpdateStateId(invoiceNumber, "paid");
							TopupHistoryObj obj = TopupHistoryService.updateStateByRefid(invoiceNumber, "in_review", transactionId);
							LOGGER.info("obj= " + obj.toString());
							DropshipTopupExecuteHandler.notifyEmail(obj);
						} else {
							if (PaymentService.paypalUpdateStateBySaleId(saleId, transactionId, "approved",
									currentMoneyPaid.toString())) {
								DropshipOrderService.UpdateStateByPaypalSaleId(saleId, "placed");
								saveOrderToFinanceQueue(invoiceNumber);
							} else {
//								DropshipOrderService.UpdateStateByPaypalSaleId(saleId, "charge_fail");
							}
						}

						routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
						routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
						break;
					case "refunded":
						// DropshipOrderService.UpdateStateByPaypalSaleId(saleId, "refunded");
						// DropshipOrder.DropshipOrderRefundService.insertRefund(saleId, saleId, saleId,
						// 0, 0, 0, status)
						// routingContext.put(AppParams.RESPONSE_CODE,
						// HttpResponseStatus.CREATED.code());
						// routingContext.put(AppParams.RESPONSE_MSG,
						// HttpResponseStatus.CREATED.reasonPhrase());
						break;
					case "scheduled":

						break;

					default:

					}

				}

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

	private void saveOrderToFinanceQueue(String invoiceNumber) throws SQLException {
		String orderId = InvoiceService.getOrderId(invoiceNumber);
		if (StringUtils.isNotEmpty(orderId)) {
			OrderToFinanceService.save(new OrderToFinanceObj(orderId, ResourceStates.CREATED, true));
		}
	}

	private static final Logger LOGGER = Logger.getLogger(PaypalIPNHandler.class.getName());

}
