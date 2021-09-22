package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.google.gson.annotations.SerializedName;

import asia.leadsgen.psp.util.AppParams;
import asia.leadsgen.psp.util.ParamUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CategoryObj implements Serializable {
	private static final long serialVersionUID = 1L;
	private String id;
	@SerializedName("parent_id")
	private String parentId;
	private String name;
	private String desc;
	private Integer visible;
	private String state;
	private String dateCreate;
	private String dateUpdate;
	@SerializedName("domain_id")
	private String domainId;
	private String domain;
	private Integer position;
	private String slug;
	private String userId;

	List<CategoryObj> subCategories;

	public int compareTo(CategoryObj compareObj) {
		// descending order
		return compareObj.getPosition() - this.position;

	}

	public static CategoryObj fromMap(Map<String, Object> input) {
		CategoryObj obj = new CategoryObj();
		obj.setId(ParamUtil.getString(input, AppParams.S_ID));
		obj.setParentId(ParamUtil.getString(input, AppParams.S_PARENT_ID));
		obj.setName(ParamUtil.getString(input, AppParams.S_NAME));
		obj.setDesc(ParamUtil.getString(input, AppParams.S_DESC));
		obj.setVisible(ParamUtil.getInt(input, AppParams.N_VISIBLE, 0));
		obj.setState(ParamUtil.getString(input, AppParams.S_STATE));
		obj.setDateCreate(ParamUtil.getString(input, AppParams.D_CREATE));
		obj.setDateUpdate(ParamUtil.getString(input, AppParams.D_UPDATE));
		obj.setDomainId(ParamUtil.getString(input, AppParams.S_DOMAIN_ID));
		obj.setDomain(ParamUtil.getString(input, AppParams.S_DOMAIN));
		obj.setPosition(ParamUtil.getInt(input, AppParams.N_POSITION, 0));
		obj.setSlug(ParamUtil.getString(input, AppParams.S_SLUG));
		return obj;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> obj = new LinkedHashMap<String, Object>();
		obj.put(AppParams.ID, this.id);
		obj.put(AppParams.PARENT_ID, this.parentId);
		obj.put(AppParams.NAME, this.name);
		obj.put(AppParams.DESC, this.desc);
		obj.put(AppParams.VISIBLE, this.visible);
		obj.put(AppParams.STATE, this.state);
		obj.put(AppParams.CREATE_DATE, this.dateCreate);
		obj.put(AppParams.UPDATE_DATE, this.dateUpdate);
		obj.put(AppParams.DOMAIN_ID, this.domainId);
		obj.put(AppParams.DOMAIN, this.domain);
		obj.put(AppParams.POSITION, this.position);
		obj.put(AppParams.SLUG, this.slug);
		return obj;
	}

	public Map<String, Object> toMapWithSubCategories() {
		Map<String, Object> obj = new LinkedHashMap<String, Object>();
		obj.put(AppParams.ID, this.id);
		obj.put(AppParams.PARENT_ID, this.parentId);
		obj.put(AppParams.NAME, this.name);
		obj.put(AppParams.DESC, this.desc);
		obj.put(AppParams.VISIBLE, this.visible);
		obj.put(AppParams.STATE, this.state);
		obj.put(AppParams.CREATE_DATE, this.dateCreate);
		obj.put(AppParams.UPDATE_DATE, this.dateUpdate);
		obj.put(AppParams.DOMAIN_ID, this.domainId);
		obj.put(AppParams.DOMAIN, this.domain);
		obj.put(AppParams.POSITION, this.position);
		obj.put(AppParams.SLUG, this.slug);

		if (subCategories != null && subCategories.isEmpty() == false && StringUtils.isEmpty(this.parentId)) {
			List<Map<String, Object>> subCategoriesMapList = subCategories.stream().map(o -> o.toMap())
					.collect(Collectors.toList());
			obj.put(AppParams.SUB_CATEGORIES, subCategoriesMapList);
		}

		return obj;
	}

}
