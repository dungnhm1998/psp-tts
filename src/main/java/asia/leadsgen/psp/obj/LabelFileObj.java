package asia.leadsgen.psp.obj;

public class LabelFileObj {
	
	private String fileName;
	private String fileUrl;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	public LabelFileObj() {

	}

	public LabelFileObj(String fileName, String fileUrl) {
		super();
		this.fileName = fileName;
		this.fileUrl = fileUrl;
	}

}
