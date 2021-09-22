package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;

public class OrderToFinanceObj implements Serializable {

	private static final long serialVersionUID = -7269007019142075250L;

	private String id;
	private String orderId;
	private String dateCreate;
	private String dateResponse;
	private String payload;
	private String response;
	private String state;
	private Boolean isDropship;

	private Order order;

	public OrderToFinanceObj() {
	}

	public OrderToFinanceObj(String orderId, String state, Boolean isDropship) {
		this.orderId = orderId;
		this.state = state;
		this.isDropship = isDropship;
	}

	public static OrderToFinanceObj fromMap(Map<String, Object> input) {
		OrderToFinanceObj obj = new OrderToFinanceObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setOrderId(ParamUtil.getString(input, AppParams.S_ORDER_ID));
		obj.setDateCreate(ParamUtil.getString(input, AppParams.D_CREATE));
		obj.setDateResponse(ParamUtil.getString(input, AppParams.D_RESPONSE));
		obj.setPayload(ParamUtil.getString(input, AppParams.S_PAYLOAD));
		obj.setResponse(ParamUtil.getString(input, AppParams.S_RESPONSE));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setIsDropship(ParamUtil.getBoolean(input, AppParams.N_DROPSHIP));
		return obj;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getOrderId() {
		return this.orderId;
	}

	public void setDateCreate(String dateCreate) {
		this.dateCreate = dateCreate;
	}

	public String getDateCreate() {
		return this.dateCreate;
	}

	public void setDateResponse(String dateResponse) {
		this.dateResponse = dateResponse;
	}

	public String getDateResponse() {
		return this.dateResponse;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getPayload() {
		return this.payload;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getResponse() {
		return this.response;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getState() {
		return this.state;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Boolean getIsDropship() {
		return isDropship;
	}

	public void setIsDropship(Boolean isDropship) {
		this.isDropship = isDropship;
	}

	public class Order {
		String id;
		String name;
		String email;
		String deliveryAddress;
		String transactionDate;
		String state;
		Double amount;
		String currency;
		Payment payment;
		List<Item> items;

		public Order() {

		}

		public class Payment {
			Double serviceCharge;
			String method;
			String billingAddress;
			String referenceId;
		}

		public class Item {
			String orderItemId;
			String id;
			String affiliateId;
			String campaignId;
			String name;
			Integer quantity;
			Double price;
			Double partnerBaseCost;
			Double baseCost;
			String partner;
			Double shipping;
			Double partnerShipping;
			Boolean isFreeship;
			Double discount;
			Double proccessingFee;
			Double tax;
		}
	}

	@Override
	public String toString() {
		return "OrderToFinanceObj [id=" + id + ", orderId=" + orderId + ", dateCreate=" + dateCreate + ", dateResponse="
				+ dateResponse + ", payload=" + payload + ", response=" + response + ", state=" + state
				+ ", isDropship=" + isDropship + ", order=" + order + "]";
	}

}
