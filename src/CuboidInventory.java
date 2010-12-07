import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class CuboidInventory implements Serializable{	
	ArrayList<CuboidItem> outside;
	ArrayList<CuboidItem> inside;
	
	CuboidInventory(){
		this.outside = new ArrayList<CuboidItem>();
		this.inside = new ArrayList<CuboidItem>();
	}
}
