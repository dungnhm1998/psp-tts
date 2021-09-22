package asia.leadsgen.psp.obj;

import java.util.ArrayList;

public class ISPAllOverV1Request {

	private ArrayList<ImgDirectory> directories;

	public ArrayList<ImgDirectory> getDirectories() {
		return directories;
	}

	public void setDirectories(ArrayList<ImgDirectory> directories) {
		this.directories = directories;
	}

	public ISPAllOverV1Request(ArrayList<ImgDirectory> directories) {
		super();
		this.directories = directories;
	}

}
