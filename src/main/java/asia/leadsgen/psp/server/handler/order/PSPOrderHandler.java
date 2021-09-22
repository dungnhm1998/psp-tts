package asia.leadsgen.psp.server.handler.order;

import asia.leadsgen.psp.service.ShippingService;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

public class PSPOrderHandler{

	protected NumberFormat formatter = new DecimalFormat("#0.00");

	public Map<String, Integer> itemGroupQuantity;

	public void initItemGroupQuantity(){
		itemGroupQuantity = ShippingService.getAllShippingGroup();
	}

}
