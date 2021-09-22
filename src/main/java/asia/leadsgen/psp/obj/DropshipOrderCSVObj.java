/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package asia.leadsgen.psp.obj;

/**
 *
 * @author HIEPHV
 */
public class DropshipOrderCSVObj {

    private String id;
    private String name;
    private String email;
    private String financial_status;
    private String paid_at;
    private String created_at;
    private String lineitem_quantity;
    private String lineitem_name;
    private String lineitem_sku;
    private String shipping_name;
    private String shipping_street;
    private String shipping_address1;
    private String shipping_address2;
    private String shipping_company;
    private String shipping_city;
    private String shipping_zip;
    private String shipping_province;
    private String shipping_country;
    private String shipping_phone;
    private String notes;
    private String design_front_url;
    private String design_back_url;
    private String mockup_front_url;
    private String mockup_back_url;
    private String check_vaild_adress;
    private boolean is_order_sku;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFinancial_status() {
        return financial_status;
    }

    public void setFinancial_status(String financial_status) {
        this.financial_status = financial_status;
    }

    public String getPaid_at() {
        return paid_at;
    }

    public void setPaid_at(String paid_at) {
        this.paid_at = paid_at;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getLineitem_quantity() {
        return lineitem_quantity;
    }

    public void setLineitem_quantity(String lineitem_quantity) {
        this.lineitem_quantity = lineitem_quantity;
    }

    public String getLineitem_name() {
        return lineitem_name;
    }

    public void setLineitem_name(String lineitem_name) {
        this.lineitem_name = lineitem_name;
    }

    public String getLineitem_sku() {
        return lineitem_sku;
    }

    public void setLineitem_sku(String lineitem_sku) {
        this.lineitem_sku = lineitem_sku.trim();
    }

    public String getShipping_name() {
        return shipping_name;
    }

    public void setShipping_name(String shipping_name) {
        this.shipping_name = shipping_name;
    }

    public String getShipping_street() {
        return shipping_street;
    }

    public void setShipping_street(String shipping_street) {
        this.shipping_street = shipping_street;
    }

    public String getShipping_address1() {
        return shipping_address1;
    }

    public void setShipping_address1(String shipping_address1) {
        this.shipping_address1 = shipping_address1;
    }

    public String getShipping_address2() {
        return shipping_address2;
    }

    public void setShipping_address2(String shipping_address2) {
        this.shipping_address2 = shipping_address2;
    }

    public String getShipping_company() {
        return shipping_company;
    }

    public void setShipping_company(String shipping_company) {
        this.shipping_company = shipping_company;
    }

    public String getShipping_city() {
        return shipping_city;
    }

    public void setShipping_city(String shipping_city) {
        this.shipping_city = shipping_city;
    }

    public String getShipping_zip() {
        return shipping_zip;
    }

    public void setShipping_zip(String shipping_zip) {
        this.shipping_zip = shipping_zip;
    }

    public String getShipping_province() {
        return shipping_province;
    }

    public void setShipping_province(String shipping_province) {
        this.shipping_province = shipping_province;
    }

    public String getShipping_country() {
        return shipping_country;
    }

    public void setShipping_country(String shipping_country) {
        this.shipping_country = shipping_country;
    }

    public String getShipping_phone() {
        return shipping_phone;
    }

    public void setShipping_phone(String shipping_phone) {
        this.shipping_phone = shipping_phone;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

	public String getDesign_front_url() {
		return design_front_url;
	}
	public void setDesign_front_url(String design_front_url) {
		this.design_front_url = design_front_url;
	}

	public String getDesign_back_url() {
		return design_back_url;
	}
	public void setDesign_back_url(String design_back_url) {
		this.design_back_url = design_back_url;
	}

	public String getMockup_front_url() {
		return mockup_front_url;
	}
	public void setMockup_front_url(String mockup_front_url) {
		this.mockup_front_url = mockup_front_url;
	}

	public String getMockup_back_url() {
		return mockup_back_url;
	}
	public void setMockup_back_url(String mockup_back_url) {
		this.mockup_back_url = mockup_back_url;
	}

	public String getCheck_vaild_adress() {
		return check_vaild_adress;
	}
	public void setCheck_vaild_adress(String check_vaild_adress) {
		this.check_vaild_adress = check_vaild_adress;
	}

	public boolean isIs_order_sku() {
		return is_order_sku;
	}
	public void setIs_order_sku(boolean is_order_sku) {
		this.is_order_sku = is_order_sku;
	}

	@Override
	public String toString() {
		return "DropshipOrderCSVObj [id=" + id + ", name=" + name + ", email=" + email + ", financial_status="
				+ financial_status + ", paid_at=" + paid_at + ", created_at=" + created_at + ", lineitem_quantity="
				+ lineitem_quantity + ", lineitem_name=" + lineitem_name + ", lineitem_sku=" + lineitem_sku
				+ ", shipping_name=" + shipping_name + ", shipping_street=" + shipping_street + ", shipping_address1="
				+ shipping_address1 + ", shipping_address2=" + shipping_address2 + ", shipping_company="
				+ shipping_company + ", shipping_city=" + shipping_city + ", shipping_zip=" + shipping_zip
				+ ", shipping_province=" + shipping_province + ", shipping_country=" + shipping_country
				+ ", shipping_phone=" + shipping_phone + ", notes=" + notes + ", design_front_url=" + design_front_url
				+ ", design_back_url=" + design_back_url + ", mockup_front_url=" + mockup_front_url
				+ ", mockup_back_url=" + mockup_back_url + ", check_vaild_adress=" + check_vaild_adress
				+ ", is_order_sku=" + is_order_sku + "]";
	}
}
