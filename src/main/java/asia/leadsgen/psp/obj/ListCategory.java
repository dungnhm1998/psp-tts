package asia.leadsgen.psp.obj;

import java.io.Serializable;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ListCategory implements Serializable {
	private static final long serialVersionUID = 1L;

	@SerializedName("data")
	@Expose
	List<CategoryObj> data;

}
