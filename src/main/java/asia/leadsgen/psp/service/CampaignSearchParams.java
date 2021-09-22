package asia.leadsgen.psp.service;

import java.util.LinkedHashMap;
import java.util.Map;

import asia.leadsgen.psp.util.AppParams;
import oracle.jdbc.OracleTypes;

public class CampaignSearchParams {
	private String userId;
	private String domain;
	private String title;
	private String categories;
	private String tags;
	private String startTime;
	private String endTime;
	private int privateValue;
	private String state;
	private int page;
	private int pageSize;
	private String orderby;
	private String orderByDir;
	private boolean includeDropship;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCategories() {
		return categories;
	}

	public void setCategories(String categories) {
		this.categories = categories;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public int getPrivateValue() {
		return privateValue;
	}

	public void setPrivateValue(int privateValue) {
		this.privateValue = privateValue;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public String getOrderby() {
		return orderby;
	}

	public void setOrderby(String orderby) {
		if (AppParams.CREATE_DATE.equalsIgnoreCase(orderby)) {
			this.orderby = AppParams.D_CREATE;
		} else if (AppParams.TITLE.equalsIgnoreCase(orderby)) {
			this.orderby = AppParams.S_TITLE;
		} else {
			this.orderby = "";
		}
	}

	public String getOrderByDir() {
		return orderByDir;
	}

	public void setOrderByDir(String orderByDir) {
		
		if (!AppParams.DESC.equalsIgnoreCase(orderByDir)) {
			this.orderByDir = "asc";
		} else {
			this.orderByDir = AppParams.DESC;
		}
		
	}

	public boolean isIncludeDropship() {
		return includeDropship;
	}

	public void setIncludeDropship(boolean includeDropship) {
		this.includeDropship = includeDropship;
	}

	public CampaignSearchParams(String userId, String domain, String title, String categories, String tags,
			String startTime, String endTime, int privateValue, String state, int page, int pageSize, String orderby,
			boolean includeDropship) {
		super();
		this.userId = userId;
		this.domain = domain;
		this.title = title;
		this.categories = categories;
		this.tags = tags;
		this.startTime = startTime;
		this.endTime = endTime;
		this.privateValue = privateValue;
		this.state = state;
		this.page = page;
		this.pageSize = pageSize;
		this.orderby = orderby;
		this.includeDropship = includeDropship;
	}

	public Map getInputParamsMap() {
		Map inputParams = new LinkedHashMap<Integer, String>();
		if (this.isIncludeDropship()) {
			inputParams.put(1, userId);
			inputParams.put(2, title);
			inputParams.put(3, page);
			inputParams.put(4, pageSize);
			inputParams.put(5, orderby);
			inputParams.put(6, orderByDir);
		} else {
			inputParams.put(1, userId);
			inputParams.put(2, domain);
			inputParams.put(3, title);
			inputParams.put(4, categories);
			inputParams.put(5, tags);
			inputParams.put(6, startTime);
			inputParams.put(7, endTime);
			inputParams.put(8, privateValue);
			inputParams.put(9, state);
			inputParams.put(10, page);
			inputParams.put(11, pageSize);
			inputParams.put(12, orderby);
		}
		return inputParams;
	}

	public Map getOutputParamsTypesMap() {
		Map<Integer, Integer> outputParamsTypes = new LinkedHashMap<>();
		if (this.isIncludeDropship()) {
			outputParamsTypes.put(7, OracleTypes.NUMBER);
			outputParamsTypes.put(8, OracleTypes.VARCHAR);
			outputParamsTypes.put(9, OracleTypes.NUMBER);
			outputParamsTypes.put(10, OracleTypes.CURSOR);
		} else {
			outputParamsTypes.put(13, OracleTypes.NUMBER);
			outputParamsTypes.put(14, OracleTypes.VARCHAR);
			outputParamsTypes.put(15, OracleTypes.NUMBER);
			outputParamsTypes.put(16, OracleTypes.CURSOR);
		}

		return outputParamsTypes;
	}

	public Map getOutputParamsNamesMap() {
		Map<Integer, String> outputParamsNames = new LinkedHashMap<>();
		if (this.isIncludeDropship()) {
			outputParamsNames.put(7, AppParams.RESULT_CODE);
			outputParamsNames.put(8, AppParams.RESULT_MSG);
			outputParamsNames.put(9, AppParams.RESULT_TOTAL);
			outputParamsNames.put(10, AppParams.RESULT_DATA);
		} else {
			outputParamsNames.put(13, AppParams.RESULT_CODE);
			outputParamsNames.put(14, AppParams.RESULT_MSG);
			outputParamsNames.put(15, AppParams.RESULT_TOTAL);
			outputParamsNames.put(16, AppParams.RESULT_DATA);
		}
		return outputParamsNames;
	}

	@Override
	public String toString() {
		return "CampaignSearchParams [userId=" + userId + ", domain=" + domain + ", title=" + title + ", categories="
				+ categories + ", tags=" + tags + ", startTime=" + startTime + ", endTime=" + endTime
				+ ", privateValue=" + privateValue + ", state=" + state + ", page=" + page + ", pageSize=" + pageSize
				+ ", orderby=" + orderby + ", includeDropship=" + includeDropship + "]";
	}

}
