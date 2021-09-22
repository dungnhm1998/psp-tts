package asia.leadsgen.psp.server.handler.stock;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import asia.leadsgen.psp.error.SystemError;
import asia.leadsgen.psp.exception.BadRequestException;
import asia.leadsgen.psp.service_fulfill.StockService;
import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.GetterUtil;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Created by HungDX on 23-Apr-16.
 */
public class ListSkuOutStock implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext routingContext) {
		
		
		routingContext.vertx().executeBlocking(future -> {

			try {
				String base_id = routingContext.request().getParam(AppParams.BASE_ID);
				String baseName = routingContext.request().getParam(AppParams.BASE_NAME);
				String colorName = routingContext.request().getParam(AppParams.COLOR_NAME);
				String sizeName = routingContext.request().getParam(AppParams.SIZE_NAME);
				int page = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE), 1);
                int pageSize = GetterUtil.getInteger(routingContext.request().getParam(AppParams.PAGE_SIZE), 10);
				Map resultData = StockService.getListSkuOutStock(base_id, page, pageSize, baseName, colorName, sizeName);
				routingContext.put(AppParams.RESPONSE_CODE, HttpResponseStatus.OK.code());
				routingContext.put(AppParams.RESPONSE_MSG, HttpResponseStatus.OK.reasonPhrase());
				routingContext.put(AppParams.RESPONSE_DATA, resultData);

				routingContext.next();

			} catch (Exception e) {
				e.printStackTrace();
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


	private static final Logger LOGGER = Logger.getLogger(ListSkuOutStock.class.getName());
}
