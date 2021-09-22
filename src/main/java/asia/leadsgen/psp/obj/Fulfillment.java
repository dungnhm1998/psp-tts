package asia.leadsgen.psp.obj;

import java.util.List;

public class Fulfillment {

	private String id;
	private String campaignId;
	private String campaignTitle;
	private int total;
	private PartnerObj partner;
	private List<FulfillmentDetail> fulfillmentDetails;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public PartnerObj getPartner() {
		return partner;
	}

	public void setPartner(PartnerObj partner) {
		this.partner = partner;
	}

	public List<FulfillmentDetail> getFulfillmentDetails() {
		return fulfillmentDetails;
	}

	public void setFulfillmentDetails(List<FulfillmentDetail> fulfillmentDetails) {
		this.fulfillmentDetails = fulfillmentDetails;
	}

	public Fulfillment() {
		super();
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}

	public String getCampaignTitle() {
		return campaignTitle;
	}

	public void setCampaignTitle(String campaignTitle) {
		this.campaignTitle = campaignTitle;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public Fulfillment(String id, String campaignId, String campaignTitle, int total, PartnerObj partner,
			List<FulfillmentDetail> fulfillmentDetails) {
		super();
		this.id = id;
		this.campaignId = campaignId;
		this.campaignTitle = campaignTitle;
		this.total = total;
		this.partner = partner;
		this.fulfillmentDetails = fulfillmentDetails;
	}

}
