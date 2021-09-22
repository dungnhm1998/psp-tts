package asia.leadsgen.psp.obj;

import java.util.ArrayList;
import java.util.List;

public class CMSCreatePkgLabelsResult {

	private int success;

	private int fail;

	private List<CMSCreateLabelResult> createLabelResults;

	public CMSCreatePkgLabelsResult() {
		super();
	}

	public CMSCreatePkgLabelsResult(int success, int fail, List<CMSCreateLabelResult> createLabelResults) {
		super();
		this.success = success;
		this.fail = fail;
		this.createLabelResults = createLabelResults;
	}

	public int getSuccess() {
		return success;
	}

	public void setSuccess(int success) {
		this.success = success;
	}

	public int getFail() {
		return fail;
	}

	public void setFail(int fail) {
		this.fail = fail;
	}

	public List<CMSCreateLabelResult> getCreateLabelResults() {
		return createLabelResults;
	}

	public void setCreateLabelResults(List<CMSCreateLabelResult> createLabelResults) {
		this.createLabelResults = createLabelResults;
	}

	@Override
	public String toString() {
		return "CMSCreatePkgLabelsResult [success=" + success + ", fail=" + fail + ", createLabelResults="
				+ createLabelResults + "]";
	}

	public void addLabelResult(CMSCreateLabelResult createLabelResult) {
		if (this.createLabelResults == null) {
			this.createLabelResults = new ArrayList<>();
		}
		this.createLabelResults.add(createLabelResult);
	}
}
