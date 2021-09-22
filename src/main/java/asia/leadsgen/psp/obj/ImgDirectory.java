package asia.leadsgen.psp.obj;

import java.util.ArrayList;

public class ImgDirectory {

	private String name;
	private ArrayList<ImgFile> img_files;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<ImgFile> getImg_files() {
		return img_files;
	}

	public void setImg_files(ArrayList<ImgFile> img_files) {
		this.img_files = img_files;
	}

	public ImgDirectory(String name, ArrayList<ImgFile> img_files) {
		super();
		this.name = name;
		this.img_files = img_files;
	}

}
