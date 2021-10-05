package asia.leadsgen.psp.server.vertical;

import java.util.logging.Level;
import java.util.logging.Logger;

import asia.leadsgen.psp.server.handler.common.ExceptionHandler;
import asia.leadsgen.psp.server.handler.common.RequestLoggingHandler;
import asia.leadsgen.psp.server.handler.common.ResponseHandler;
import asia.leadsgen.psp.server.handler.dropship.campaign.CampaignDropshipGetHandler;
import asia.leadsgen.psp.server.handler.dropship.campaign.TestApiHandler;
import asia.leadsgen.psp.server.handler.dropship.collection.DropshipCollectionSearchHandler;
import asia.leadsgen.psp.server.handler.dropship.dashboard.DropshipConversionDetailHandler;
import asia.leadsgen.psp.server.handler.dropship.dashboard.DropshipConversionOverviewHandler;
import asia.leadsgen.psp.server.handler.dropship.dashboard.GetCatalogDetailHandler;
import asia.leadsgen.psp.server.handler.dropship.dashboard.GetCatalogsHandler;
import asia.leadsgen.psp.server.handler.dropship.dashboard.GetNewProductHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipApiOrderCancelHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipApiOrderStatusHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipCampApiOrderCreateHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipCampaignSearchHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipCustomApiOrderCreateV2Handler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipGetlistOrderImportFileHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipGetlistOrderImportFileRowHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderBalanceHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderCreateHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderCustomApiCheckLogHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderDeleteHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderDuplicateHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderImportCVSHandlerV2;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderImportFileHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderImportReprocessHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderLookupHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderRefundHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderSearchHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderTopupHistoryHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderTrackingLookupHandler;
import asia.leadsgen.psp.server.handler.dropship.order.DropshipOrderUpdateHandler;
import asia.leadsgen.psp.server.handler.dropship.order.ShopbaseOrderPaidHandler;
import asia.leadsgen.psp.server.handler.dropship.order.ShopbaseOrderRefundHandler;
import asia.leadsgen.psp.server.handler.dropship.order.ShopifyOrderPaidHandler;
import asia.leadsgen.psp.server.handler.dropship.order.ShopifyOrderRefundHandler;
import asia.leadsgen.psp.server.handler.dropship.order.ShopifyOrderSyncHandler;
import asia.leadsgen.psp.server.handler.dropship.order.ShopifyPullOrderHandler;
import asia.leadsgen.psp.server.handler.dropship.order.WebhookGatewayApiHandler;
import asia.leadsgen.psp.server.handler.dropship.order.WoocommercePullOrderHandler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderCreateCustomHandler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderCreateV2Handler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderDeleteV2Handler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderDraftToQueuedHandler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderIgnoredHandler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderListBaseHandler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderLookupV2Handler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderQueuedPayallHandler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderSearchTrackingHandler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderSearchV2Handler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderUpdateCustomHandler;
import asia.leadsgen.psp.server.handler.dropship.order_v2.DropshipOrderUpdateV2Handler;
import asia.leadsgen.psp.server.handler.dropship.payment.DropshipActiveTopupExecuteHandler;
import asia.leadsgen.psp.server.handler.dropship.payment.DropshipPaymentExecuteHandler;
import asia.leadsgen.psp.server.handler.dropship.payment.DropshipPaymentExecuteV2Handler;
import asia.leadsgen.psp.server.handler.dropship.payment.DropshipTopupExecuteHandler;
import asia.leadsgen.psp.server.handler.dropship.product.CampaignExportProductHandler;
import asia.leadsgen.psp.server.handler.dropship.product.DropshipProductUploadHandler;
import asia.leadsgen.psp.server.handler.dropship.product.GetTopSellingProductHandler;
import asia.leadsgen.psp.server.handler.dropship.shopbase.ShopbaseConnectAuthHandler;
import asia.leadsgen.psp.server.handler.dropship.shopbase.ShopbaseConnectHandler;
import asia.leadsgen.psp.server.handler.dropship.shopify.FetchOrderDropShipHandler;
import asia.leadsgen.psp.server.handler.dropship.shopify.ShopifyConnectAuthHandler;
import asia.leadsgen.psp.server.handler.dropship.shopify.ShopifyConnectHandler;
import asia.leadsgen.psp.server.handler.dropship.shopify.WooCommerceOrderHandler;
import asia.leadsgen.psp.server.handler.dropship.shopify.WooEcommerceConnectAuthHandler;
import asia.leadsgen.psp.server.handler.dropship.shopify.WooEcommerceConnectHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropShipStoreSearchHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipApiStoreCreateHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipApiStoreUpdateHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipStoreCampListHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipStoreCreateHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipStoreDeleteHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipStoreLookupHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipStoreProductListHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipStoreReconnectHanlder;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipStoreUpdateAutoFulfillHandler;
import asia.leadsgen.psp.server.handler.dropship.store.DropshipStoreUpdateHandler;
import asia.leadsgen.psp.server.handler.dropship.store.campaign.DropshipCampaignReUploadHandler;
import asia.leadsgen.psp.server.handler.dropship.woo.WooCommerceGetAttribute;
import asia.leadsgen.psp.server.handler.dropship.woo.WooCommerceMapAttribute;
import asia.leadsgen.psp.server.handler.etsy.EtsyConnectAuthHandler;
import asia.leadsgen.psp.server.handler.etsy.EtsyConnectHandler;
import asia.leadsgen.psp.server.handler.fraud.FraudHandler;
import asia.leadsgen.psp.server.handler.fulfillment.AssignedItemToPartnerHandler;
import asia.leadsgen.psp.server.handler.fulfillment.DesignPrintCreateHandler;
import asia.leadsgen.psp.server.handler.fulfillment.ExportProductHandler;
import asia.leadsgen.psp.server.handler.fulfillment.GetShippingConfigHandler;
import asia.leadsgen.psp.server.handler.fulfillment.OrderProductUpdateFulfillmentReviewHandler;
import asia.leadsgen.psp.server.handler.media.ListMediaHandler;
import asia.leadsgen.psp.server.handler.media.MediaCreateHandler;
import asia.leadsgen.psp.server.handler.media.MediaCreateListHandler;
import asia.leadsgen.psp.server.handler.media.MediaDeleteHandler;
import asia.leadsgen.psp.server.handler.media.MediaUpdateHandler;
import asia.leadsgen.psp.server.handler.payment.PaypalCreateInvoiceHandler;
import asia.leadsgen.psp.server.handler.payout.PayoutConfirmHandler;
import asia.leadsgen.psp.server.handler.payout.PayoutHandler;
import asia.leadsgen.psp.server.handler.payout.PayoutWithdrawHandler;
import asia.leadsgen.psp.server.handler.preferences.PreferencesLookUpHandler;
import asia.leadsgen.psp.server.handler.privileges.UserPrivilegesHandler;
import asia.leadsgen.psp.server.handler.shopify_app.CurrencyExchangeRateHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyAddProductHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyAddProductVariantHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyAppCheckStoreHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyAppConnectAuthHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyAppMatchStoreHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyCampaignCreateV2Handler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyDeleteProductHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyDeleteProductVariantHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyEditDescriptionHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyEditPricesHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyEditProductVariantHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyFetchProductHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyGetCollectionsHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyGetExitsVariantHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyListBaseHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyListBaseToCreateOrderHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyLookupProductHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifySearchProductHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifySearchProductStateHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifySyncManualOrderHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifySyncOrdersHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifySyncProductVariantHandler;
import asia.leadsgen.psp.server.handler.shopify_app.ShopifyUnsyncProductVariantHandler;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.WebhookShopifyAppUninstalledHandler;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.WebhookShopifyCreateOrderHandler;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.WebhookShopifyCreateProductHandler;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.WebhookShopifyDeleteOrderHandler;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.WebhookShopifyDeleteProductHandler;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.WebhookShopifyRedactHandler;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.WebhookShopifyUpdateOrderHandler;
import asia.leadsgen.psp.server.handler.shopify_app.webhook.WebhookShopifyUpdateProductHandler;
import asia.leadsgen.psp.server.handler.stock.ListSkuOutStock;
import asia.leadsgen.psp.server.handler.warehouse.RosalindaGetOrdersHandler;
import asia.leadsgen.psp.server.handler.warehouse.WareHouseCreateHandler;
import asia.leadsgen.psp.server.handler.warehouse.WareHouseGetStateHandler;
import asia.leadsgen.psp.server.handler.webhook.IterloanWebhookHandler;
import asia.leadsgen.psp.util.StringPool;
import asia.leadsgen.psp.webhook.PaypalIPNHandler;
import asia.leadsgen.psp.webhook.TrackingMoreEventHandler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.ResponseTimeHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class PSPVertical extends AbstractVerticle {

	private String serverHost;
	private int serverPort;
	private boolean connectionKeepAlive;
	private long connectionTimeOut;
	private int connectionIdleTimeOut;
	private String apiPrefix;
//	private String apiPrefixV2;
//	private String apiTokenPrefix;

	public static HttpClient httpClient;
	public static HttpClient httpsClient;

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setConnectionKeepAlive(boolean connectionKeepAlive) {
		this.connectionKeepAlive = connectionKeepAlive;
	}

	public void setConnectionTimeOut(long connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
	}

	public void setConnectionIdleTimeOut(int connectionIdleTimeOut) {
		this.connectionIdleTimeOut = connectionIdleTimeOut;
	}

	public void setApiPrefix(String apiPrefix) {
		this.apiPrefix = apiPrefix;
	}

//	public void setApiPrefixV2(String apiPrefixV2) {
//		this.apiPrefixV2 = apiPrefixV2;
//	}

//	public void setApiTokenPrefix(String apiTokenPrefix) {
//		this.apiTokenPrefix = apiTokenPrefix;
//	}

	@Override
	public void start() throws Exception {

		LOGGER.log(Level.INFO, "[INIT] STARTING UP PSP API SERVER...");

		httpClient = vertx.createHttpClient();
		httpsClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

		super.start();

		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.route().handler(ResponseTimeHandler.create());
		router.route().handler(TimeoutHandler.create(connectionTimeOut));
		router.route().handler(CookieHandler.create());
		router.route().handler(new RequestLoggingHandler());
		router.route().handler(new FraudHandler());

		// TEST with postman comment out ClientAuthorizationHandler, SessionCheckingHandler,initSessionsRouter
//		router.route().handler(new ClientAuthorizationHandler());
//		router.route().handler(new SessionCheckingHandler());
		router.route().handler(new UserPrivilegesHandler());

		router.mountSubRouter(apiPrefix, initPreferencesRouter());
//		router.mountSubRouter(apiPrefix, initSessionsRouter());
		router.mountSubRouter(apiPrefix, initMediaRouter());
		router.mountSubRouter(apiPrefix, initPayoutRouter());
		router.mountSubRouter(apiPrefix, dropshipRouter());

		router.mountSubRouter(apiPrefix, initFulfillment());
//		router.mountSubRouter(apiPrefixV2, initDomainsV2Router());

		router.mountSubRouter(apiPrefix, exposedAPI());
		router.mountSubRouter(apiPrefix, initShopifyAppRouter());
		router.mountSubRouter(apiPrefix, initEtsyRouter());
		router.mountSubRouter(apiPrefix, initPaymentRouter());
		
		router.mountSubRouter(apiPrefix, initRosalindaRouter());
		router.mountSubRouter(apiPrefix, initWebhooksRouter());
		router.mountSubRouter(apiPrefix, initExportRouter());

		router.route().failureHandler(new ExceptionHandler());
		router.route().last().handler(new ResponseHandler());

		HttpServerOptions httpServerOptions = new HttpServerOptions();

		httpServerOptions.setHost(serverHost);
		httpServerOptions.setPort(serverPort);
		httpServerOptions.setTcpKeepAlive(connectionKeepAlive);
		httpServerOptions.setIdleTimeout(connectionIdleTimeOut);

		HttpServer httpServer = vertx.createHttpServer(httpServerOptions);

		httpServer.requestHandler(router::accept);
		httpServer.listen(result -> {
			if (result.failed()) {
				LOGGER.log(Level.SEVERE, "[INIT] START PSP API ERROR ", result.cause());
			} else {
				LOGGER.log(Level.INFO,
						"[INIT] PSP API STARTED AT" + StringPool.SPACE + serverHost + StringPool.COLON + serverPort);
			}
		});
	}

	private Router initPreferencesRouter() {

		Router router = Router.router(vertx);
		router.route(HttpMethod.GET, "/preferences*").handler(new PreferencesLookUpHandler());

		return router;
	}
	
	private Router initMediaRouter() {

		Router router = Router.router(vertx);
		router.route(HttpMethod.GET, "/media").handler(new ListMediaHandler());
		router.route(HttpMethod.POST, "/media").handler(new MediaCreateHandler());
		router.route(HttpMethod.POST, "/media/list").handler(new MediaCreateListHandler());
		router.route(HttpMethod.PUT, "/media/:id").handler(new MediaUpdateHandler());
		router.route(HttpMethod.DELETE, "/media/:id").handler(new MediaDeleteHandler());
		router.route(HttpMethod.GET, "/out-stock").handler(new ListSkuOutStock());

		return router;
	}

	private Router initPayoutRouter() {

		Router router = Router.router(vertx);
		router.route(HttpMethod.GET, "/payout*").handler(new PayoutHandler());
		router.route(HttpMethod.POST, "/payout-withdraw").handler(new PayoutWithdrawHandler());
		router.route(HttpMethod.PUT, "/payout/confirm").handler(new PayoutConfirmHandler());

		return router;
	}

	private Router dropshipRouter() {
		Router router = Router.router(vertx);
		router.route(HttpMethod.GET, "/test-api").handler(new TestApiHandler());
		router.route(HttpMethod.GET, "/dropship/stores").handler(new DropShipStoreSearchHandler());
		router.route(HttpMethod.GET, "/dropship/stores/:id").handler(new DropshipStoreLookupHandler());
		router.route(HttpMethod.DELETE, "/dropship/stores/:id").handler(new DropshipStoreDeleteHandler());
		router.route(HttpMethod.POST, "/dropship/stores").handler(new DropshipStoreCreateHandler());
		router.route(HttpMethod.PUT, "/dropship/stores/:id").handler(new DropshipStoreUpdateHandler());
		router.route(HttpMethod.GET, "/dropship/stores/:id/campaigns").handler(new DropshipStoreCampListHandler());
		router.route(HttpMethod.GET, "/dropship/stores/:id/products").handler(new DropshipStoreProductListHandler());
		router.route(HttpMethod.GET, "/dropship/stores/:id/collections").handler(new DropshipCollectionSearchHandler());
		router.route(HttpMethod.GET, "/dropship/store/:id/reconnect").handler(new DropshipStoreReconnectHanlder());
		
		router.route(HttpMethod.POST, "/dropship/products").handler(new DropshipProductUploadHandler());
		router.route(HttpMethod.POST, "/dropship/orders").handler(new DropshipOrderCreateHandler());
		router.route(HttpMethod.PUT, "/dropship/orders/:id").handler(new DropshipOrderUpdateHandler());
		router.route(HttpMethod.GET, "/dropship/orders/:id").handler(new DropshipOrderLookupHandler());
		router.route(HttpMethod.DELETE, "/dropship/orders/:id").handler(new DropshipOrderDeleteHandler());
		router.route(HttpMethod.POST, "/dropship/orders/:id/duplicate").handler(new DropshipOrderDuplicateHandler());
		router.route(HttpMethod.GET, "/dropship/orders").handler(new DropshipOrderSearchHandler());
		router.route(HttpMethod.POST, "/dropship/orders/:id/refund").handler(new DropshipOrderRefundHandler());
		
		router.route(HttpMethod.POST, "/dropship/payment/charge").handler(new DropshipPaymentExecuteHandler());
		
		router.route(HttpMethod.GET, "/dropship/shopify/connect").handler(new ShopifyConnectHandler());
		router.route(HttpMethod.POST, "/dropship/shopify/connect").handler(new ShopifyConnectAuthHandler());
		
		router.route(HttpMethod.POST, "/shopify-store/:id/paid-order").handler(new ShopifyOrderPaidHandler());
		router.route(HttpMethod.POST, "/shopify-store/:id/refund-order").handler(new ShopifyOrderRefundHandler());
		router.route(HttpMethod.PUT, "/shopify-store/:id/auto-fulfillment").handler(new DropshipStoreUpdateAutoFulfillHandler());
		
		router.route(HttpMethod.GET, "/dropship/orders/tracking/:id").handler(new DropshipOrderTrackingLookupHandler());
		router.route(HttpMethod.POST, "/dropship/orders/import/:id").handler(new DropshipOrderImportCVSHandlerV2());
		
		router.route(HttpMethod.POST, "/dropship/woocommerce/connect").handler(new WooEcommerceConnectHandler());
		router.route(HttpMethod.GET, "/dropship/get-order").handler(new FetchOrderDropShipHandler());
		router.route(HttpMethod.POST, "/dropship/woocommerce/auth").handler(new WooEcommerceConnectAuthHandler());
		router.route(HttpMethod.POST, "/dropship/woocommerce/order").handler(new WooCommerceOrderHandler());
		router.route(HttpMethod.GET, "/dropship/shopbase/connect").handler(new ShopbaseConnectHandler());
		router.route(HttpMethod.POST, "/dropship/shopbase/connect").handler(new ShopbaseConnectAuthHandler());
		router.route(HttpMethod.POST, "/shopbase-store/:id/paid-order").handler(new ShopbaseOrderPaidHandler());
		router.route(HttpMethod.POST, "/shopbase-store/:id/refund-order").handler(new ShopbaseOrderRefundHandler());
		router.route(HttpMethod.POST, "/dropship/payment/paypal/create-invoice").handler(new PaypalCreateInvoiceHandler());
		router.route(HttpMethod.PUT, "/dropship/campaign/reupload").handler(new DropshipCampaignReUploadHandler());
		router.route(HttpMethod.GET, "/dropship/woocommerce/store/:id/order-pull").handler(new WoocommercePullOrderHandler());
		router.route(HttpMethod.GET, "/dropship/shopify/store/:id/order-pull").handler(new ShopifyPullOrderHandler());
		router.route(HttpMethod.POST, "/dropship/shopify/store/:id/order-sync").handler(new ShopifyOrderSyncHandler());
		router.route(HttpMethod.GET, "/dropship/campaigns").handler(new DropshipCampaignSearchHandler());

		router.route(HttpMethod.GET, "/dropship/v2/orders").handler(new DropshipOrderSearchV2Handler());
		router.route(HttpMethod.POST, "/dropship/v2/orders").handler(new DropshipOrderCreateV2Handler());
		router.route(HttpMethod.GET, "/dropship/v2/orders/:id").handler(new DropshipOrderLookupV2Handler());
		router.route(HttpMethod.PUT, "/dropship/v2/orders/:id").handler(new DropshipOrderUpdateV2Handler());
		router.route(HttpMethod.PUT, "/dropship/v2/orders-ignore").handler(new DropshipOrderIgnoredHandler());
		router.route(HttpMethod.PUT, "/dropship/v2/orders-delete").handler(new DropshipOrderDeleteV2Handler());
		router.route(HttpMethod.GET, "/dropship/v2/order-payall").handler(new DropshipOrderQueuedPayallHandler());
		
		router.route(HttpMethod.POST, "/dropship/v2/payment/charge").handler(new DropshipPaymentExecuteV2Handler());
		router.route(HttpMethod.POST, "/dropship/v2/payment/topup").handler(new DropshipTopupExecuteHandler());
		router.route(HttpMethod.POST, "/dropship/v2/payment/active_topup").handler(new DropshipActiveTopupExecuteHandler());
		
		router.route(HttpMethod.POST, "/dropship/v2/order-custom").handler(new DropshipOrderCreateCustomHandler());
		router.route(HttpMethod.PUT, "/dropship/v2/order-custom/:id").handler(new DropshipOrderUpdateCustomHandler());
		router.route(HttpMethod.GET, "/dropship/v2/list-base").handler(new DropshipOrderListBaseHandler());
		router.route(HttpMethod.PUT, "/dropship/v2/orders-draft-to-queued/:id").handler(new DropshipOrderDraftToQueuedHandler());

		router.route(HttpMethod.POST, "/dropship-api/store").handler(new DropshipApiStoreCreateHandler());
		router.route(HttpMethod.PUT, "/dropship-api/store/:id").handler(new DropshipApiStoreUpdateHandler());

		router.route(HttpMethod.POST, "/order-review/update/:id").handler(new OrderProductUpdateFulfillmentReviewHandler());
		
		// ImportFile - ChinhVV
		router.route(HttpMethod.POST, "/dropship/orders/import-v2/:id").handler(new DropshipOrderImportFileHandler());
		router.route(HttpMethod.GET, "/dropship/orders/import-v2/files").handler(new DropshipGetlistOrderImportFileHandler());
		router.route(HttpMethod.GET, "/dropship/orders/import-file-row-v2/:id").handler(new DropshipGetlistOrderImportFileRowHandler());	
		router.route(HttpMethod.POST, "/dropship/orders/import/reprocess/:id").handler(new DropshipOrderImportReprocessHandler());

		router.route(HttpMethod.GET, "/dropship/orders-balance").handler(new DropshipOrderBalanceHandler());
		router.route(HttpMethod.GET, "/dropship/orders-topup-history").handler(new DropshipOrderTopupHistoryHandler());
		
		router.route(HttpMethod.POST, "/s3_webhook").handler(new WebhookGatewayApiHandler());
		router.route(HttpMethod.GET, "/dropship/orders-look-up/:id").handler(new DropshipOrderCustomApiCheckLogHandler());
		router.route(HttpMethod.GET, "/dropship/woocommerce/get-list-attribute/:id").handler(new WooCommerceGetAttribute());
		router.route(HttpMethod.POST, "/dropship/woocommerce/map-list-attribute/:id").handler(new WooCommerceMapAttribute());
		
		router.route(HttpMethod.GET, "/dropship/top-selling/products").handler(new GetTopSellingProductHandler());
		
		router.route(HttpMethod.GET, "/dropship/dashboard/conversion/overview").handler(new DropshipConversionOverviewHandler());
		router.route(HttpMethod.GET, "/dropship/dashboard/conversion/detail").handler(new DropshipConversionDetailHandler());

		router.route(HttpMethod.GET, "/dropship/catalog").handler(new GetCatalogsHandler());
		router.route(HttpMethod.GET, "/dropship/catalog/:id").handler(new GetCatalogDetailHandler());
		
		router.route(HttpMethod.GET, "/dropship/v2/orders-tracking").handler(new DropshipOrderSearchTrackingHandler());
		router.route(HttpMethod.GET, "/dropship/list-campaigns").handler(new CampaignDropshipGetHandler());
		
		router.route(HttpMethod.GET, "/dropship/new-products").handler(new GetNewProductHandler());
		
		router.route(HttpMethod.GET, "/dropship/list-campaigns").handler(new CampaignDropshipGetHandler());
		
		router.route(HttpMethod.GET, "/dropship/new-products").handler(new GetNewProductHandler());

		return router;
	}

	private Router initFulfillment() {

		Router router = Router.router(vertx);
		router.route(HttpMethod.POST, "/fulfillment/export").handler(new ExportProductHandler());

		router.route(HttpMethod.GET, "/get-shipping-config").handler(new GetShippingConfigHandler());
		router.route(HttpMethod.POST, "/create-design-print/:id").handler(new DesignPrintCreateHandler());
		router.route(HttpMethod.POST, "/assigned-to-partner").handler(new AssignedItemToPartnerHandler());
		return router;
	}

	private Router exposedAPI() {
		Router router = Router.router(vertx);
		router.route(HttpMethod.POST, "/dropship-api/order/v1").handler(new DropshipCampApiOrderCreateHandler());
		router.route(HttpMethod.POST, "/dropship-api/order/v2").handler(new DropshipCustomApiOrderCreateV2Handler());
		router.route(HttpMethod.POST, "/dropship-api/order/v1/cancel").handler(new DropshipApiOrderCancelHandler());
		router.route(HttpMethod.GET, "/dropship-api/order/v1/:id").handler(new DropshipApiOrderStatusHandler());
		router.route(HttpMethod.GET, "/dropship-api/order/v2/check-log/:id").handler(new DropshipOrderCustomApiCheckLogHandler());
		return router;
	}
	
	private Router initShopifyAppRouter() {
		Router router = Router.router(vertx);
		router.route(HttpMethod.GET, "/shopify-app/get-products").handler(new ShopifyFetchProductHandler());
		router.route(HttpMethod.GET, "/shopify-app/product/:id").handler(new ShopifyLookupProductHandler());
		router.route(HttpMethod.GET, "/shopify-app/products").handler(new ShopifySearchProductHandler());
		router.route(HttpMethod.POST, "/shopify-app/product").handler(new ShopifyAddProductHandler());
		router.route(HttpMethod.PUT, "/shopify-app/product/:id/add-variant").handler(new ShopifyAddProductVariantHandler());
		router.route(HttpMethod.PUT, "/shopify-app/product/edit-variant/:id").handler(new ShopifyEditProductVariantHandler());
		router.route(HttpMethod.GET, "/shopify-app/get-base").handler(new ShopifyListBaseHandler());
		router.route(HttpMethod.GET, "/shopify-app/currency-exchange").handler(new CurrencyExchangeRateHandler());
		router.route(HttpMethod.PUT, "/shopify-app/product/:id/desc").handler(new ShopifyEditDescriptionHandler());
		router.route(HttpMethod.PUT, "/shopify-app/product/variants-delete").handler(new ShopifyDeleteProductVariantHandler());
		router.route(HttpMethod.PUT, "/shopify-app/product/variants-unsync").handler(new ShopifyUnsyncProductVariantHandler());
		router.route(HttpMethod.PUT, "/shopify-app/product/prices").handler(new ShopifyEditPricesHandler());
		router.route(HttpMethod.PUT, "/shopify-app/product/products-delete").handler(new ShopifyDeleteProductHandler());
		router.route(HttpMethod.GET, "/shopify-app/get-collections").handler(new ShopifyGetCollectionsHandler());
		router.route(HttpMethod.PUT, "/shopify-app/product/variant-sync/:id").handler(new ShopifySyncProductVariantHandler());
		router.route(HttpMethod.POST, "/shopify-app/connect").handler(new ShopifyAppConnectAuthHandler());
		router.route(HttpMethod.POST, "/shopify-app/match-store").handler(new ShopifyAppMatchStoreHandler());
		router.route(HttpMethod.GET, "/shopify-app/check-store").handler(new ShopifyAppCheckStoreHandler());
		router.route(HttpMethod.GET, "/shopify-app/sync-order-manual").handler(new ShopifySyncManualOrderHandler());
		router.route(HttpMethod.GET, "/shopify-app/sync-orders").handler(new ShopifySyncOrdersHandler());
		router.route(HttpMethod.GET, "/shopify-app/get-exits-variant").handler(new ShopifyGetExitsVariantHandler());
		router.route(HttpMethod.GET, "/shopify-app/get-base-to-create-order").handler(new ShopifyListBaseToCreateOrderHandler());
		router.route(HttpMethod.GET, "/shopify-app/products-state").handler(new ShopifySearchProductStateHandler());
		
		router.route(HttpMethod.POST, "/shopify-notification/create-product").handler(new WebhookShopifyCreateProductHandler());
		router.route(HttpMethod.POST, "/shopify-notification/update-product").handler(new WebhookShopifyUpdateProductHandler());
		router.route(HttpMethod.POST, "/shopify-notification/delete-product").handler(new WebhookShopifyDeleteProductHandler());
		
		router.route(HttpMethod.POST, "/shopify-notification/create-order").handler(new WebhookShopifyCreateOrderHandler());
		router.route(HttpMethod.POST, "/shopify-notification/update-order").handler(new WebhookShopifyUpdateOrderHandler());
		router.route(HttpMethod.POST, "/shopify-notification/delete-order").handler(new WebhookShopifyDeleteOrderHandler());
		router.route(HttpMethod.POST, "/shopify-notification/cancelled-order").handler(new WebhookShopifyDeleteOrderHandler());
		router.route(HttpMethod.POST, "/shopify-notification/app-uninstalled").handler(new WebhookShopifyAppUninstalledHandler());
		router.route(HttpMethod.GET, "/shopify-notification/app-uninstalled").handler(new WebhookShopifyAppUninstalledHandler());
		router.route(HttpMethod.POST, "/shopify-notification/shop/redact").handler(new WebhookShopifyRedactHandler());
		
		router.route(HttpMethod.POST, "/shopify-app/campaign-create-v2").handler(new ShopifyCampaignCreateV2Handler());

		return  router;
	}
	
	private Router initPaymentRouter() {

		Router router = Router.router(vertx);
		router.route(HttpMethod.POST, "/payments/invoice").handler(new PaypalIPNHandler());

		return router;
	}
	
	private Router initWebhooksRouter() {
		Router router = Router.router(vertx);
		router.route(HttpMethod.POST, "/webhooks/trackingmore-b2d6e422-83b9").handler(new TrackingMoreEventHandler());
		router.route(HttpMethod.POST, "/webhooks/interloan").handler(new IterloanWebhookHandler());
		return router;
	}
	
	private Router initEtsyRouter(){
	    Router router = Router.router(vertx);
	    router.route(HttpMethod.GET, "/etsy/connect").handler(new EtsyConnectHandler());
	    router.route(HttpMethod.POST, "/etsy/connect-auth").handler(new EtsyConnectAuthHandler());

	    return router;
	  }
	
	private Router initRosalindaRouter() {
		Router router = Router.router(vertx);
		router.route(HttpMethod.POST,"/warehouse/update-orders").handler(new WareHouseCreateHandler());
		router.route(HttpMethod.GET,"/warehouse/status/:id").handler(new WareHouseGetStateHandler());
		router.route(HttpMethod.GET, "/warehouse/list-orders").handler(new RosalindaGetOrdersHandler());

		return router;
	}
	
	private Router initExportRouter() {
		Router router = Router.router(vertx);
		router.route(HttpMethod.POST,"/campaign/export-products/").handler(new CampaignExportProductHandler());

		return router;
	}
	
	private static final Logger LOGGER = Logger.getLogger(PSPVertical.class.getName());
}
