package asia.leadsgen.psp.obj;

public class ImgFile {
	
	private String directory;
	private String name;
	private String url;
	
	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public ImgFile() {
		super();
	}

	public ImgFile(String directory, String name, String url) {
		super();
		this.directory = directory;
		this.name = name;
		this.url = url;
	}

}
