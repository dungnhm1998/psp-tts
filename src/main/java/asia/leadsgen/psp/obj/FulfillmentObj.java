package asia.leadsgen.psp.obj;

import java.sql.Date;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

import lombok.Data;

@Data
public class FulfillmentObj implements SQLData {
	
	public static final String SQL_TYPE = "FULFILLMENT_TYPE";
	
	private String id;
	private String campaignId;
	private String campaignTitle;
	private String sellerId;
	private String sellerName;
	private int quantity;
	private Date start;
	private Date end;
	private Date assigned;
	private Date accepted;
	private Date complete;
	private String state;
	private String partnerId;
	private Date create;
	private Date update;
	private String sellerEmail;
	private String label;
	private Date dueDate;
	private String location;
	private int creatingLabel;
	private int hasPayout;

	
	@Override
	public String getSQLTypeName() throws SQLException {
		return SQL_TYPE;
	}

	@Override
	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		id = stream.readString();
		campaignId = stream.readString();
		campaignTitle = stream.readString();
		sellerId = stream.readString();
		sellerName = stream.readString();
		quantity = stream.readInt();
		start = stream.readDate();
		end = stream.readDate();
		assigned = stream.readDate();
		accepted = stream.readDate();
		complete = stream.readDate();
		state = stream.readString();
		partnerId = stream.readString();
		create = stream.readDate();
		update = stream.readDate();
		sellerEmail = stream.readString();
		label = stream.readString();
		dueDate = stream.readDate();
		location = stream.readString();
		creatingLabel = stream.readInt();
		hasPayout = stream.readInt();
	}

	@Override
	public void writeSQL(SQLOutput stream) throws SQLException {
		stream.writeString(id);
		stream.writeString(campaignId);
		stream.writeString(campaignTitle);
		stream.writeString(sellerId);
		stream.writeString(sellerName);
		stream.writeInt(quantity);
		stream.writeDate(start);
		stream.writeDate(end);
		stream.writeDate(assigned);
		stream.writeDate(accepted);
		stream.writeDate(complete);
		stream.writeString(state);
		stream.writeString(partnerId);
		stream.writeDate(create);
		stream.writeDate(update);
		stream.writeString(sellerEmail);
		stream.writeString(label);
		stream.writeDate(dueDate);
		stream.writeString(location);
		stream.writeInt(creatingLabel);
		stream.writeInt(hasPayout);
	}
	
	public static class Builder {
		private String id;
		private String campaignId;
		private String campaignTitle;
		private String sellerId;
		private String sellerName;
		private int quantity;
		private Date start;
		private Date end;
		private Date assigned;
		private Date accepted;
		private Date complete;
		private String state;
		private String partnerId;
		private Date create;
		private Date update;
		private String sellerEmail;
		private String label;
		private Date dueDate;
		private String location;
		private int creatingLabel;
		private int hasPayout;
		
		public Builder(String id) {
			this.id = id;
		}
		
		public Builder campaignId(String campaignId) {
			this.campaignId = campaignId;
			return this;
		}
		
		public Builder campaignTitle(String campaignTitle) {
			this.campaignTitle = campaignTitle;
			return this;
		}
		
		public Builder sellerId(String sellerId) {
			this.sellerId = sellerId;
			return this;
		}
		
		public Builder sellerName(String sellerName) {
			this.sellerName = sellerName;
			return this;
		}
		
		public Builder quantity(int quantity) {
			this.quantity = quantity;
			return this;
		}
		
		public Builder start(Date start) {
			this.start = start;
			return this;
		}
		
		public Builder end(Date end) {
			this.end = end;
			return this;
		}
		
		public Builder assigned(Date assigned) {
			this.assigned = assigned;
			return this;
		}
		
		public Builder accepted(Date accepted) {
			this.accepted = accepted;
			return this;
		}
		
		public Builder complete(Date complete) {
			this.complete = complete;
			return this;
		}
		
		public Builder state(String state) {
			this.state = state;
			return this;
		}
		
		public Builder partnerId(String partnerId) {
			this.partnerId = partnerId;
			return this;
		}
		
		public Builder create(Date create) {
			this.create = create;
			return this;
		}
		
		public Builder update(Date update) {
			this.update = update;
			return this;
		}
		
		public Builder sellerEmail(String sellerEmail) {
			this.sellerEmail = sellerEmail;
			return this;
		}
		
		public Builder label(String label) {
			this.label = label;
			return this;
		}
		
		public Builder dueDate(Date dueDate) {
			this.dueDate = dueDate;
			return this;
		}
		
		public Builder location(String location) {
			this.location = location;
			return this;
		}
		
		public Builder creatingLabel(int creatingLabel) {
			this.creatingLabel = creatingLabel;
			return this;
		}
		
		public Builder hasPayout(int hasPayout) {
			this.hasPayout = hasPayout;
			return this;
		}
		
		public FulfillmentObj build() {
			FulfillmentObj obj = new FulfillmentObj();
			obj.setId(this.id);
			obj.setCampaignId(this.campaignId);
			obj.setCampaignTitle(this.campaignTitle);
			obj.setSellerId(this.sellerId);
			obj.setSellerName(this.sellerName);
			obj.setQuantity(this.quantity);
			obj.setStart(this.start);
			obj.setEnd(this.end);
			obj.setAssigned(this.assigned);
			obj.setAccepted(this.accepted);
			obj.setComplete(this.complete);
			obj.setState(this.state);
			obj.setPartnerId(this.partnerId);
			obj.setCreate(this.create);
			obj.setUpdate(this.update);
			obj.setSellerEmail(this.sellerEmail);
			obj.setLabel(this.label);
			obj.setDueDate(this.dueDate);
			obj.setLocation(this.location);
			obj.setCreatingLabel(this.creatingLabel);
			obj.setHasPayout(this.hasPayout);
			return obj;
		}
	}
	
}
