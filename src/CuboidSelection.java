public class CuboidSelection {	
	boolean status = false;	// status == false --> the first corner is to be selected
	boolean undoable = false;
	int[] firstCorner = null;
	int[] secondCorner = null;
	int[][][] lastCopiedCuboid = null;	// Populated with a /ccopy
	int[][][] lastSelectedCuboid = null;	//	Populated when using a /cload, different from above to enable /undo
	int[] pastePoint = null;
	
	public CuboidSelection(){
		this.status = false;
		this.undoable = false;
		this.firstCorner = null;
		this.secondCorner = null;
		this.lastCopiedCuboid = null;
		this.lastSelectedCuboid = null;
		this.pastePoint = null;
	}
	
	public boolean selectCorner(int X, int Y, int Z){
		
		//  first corner
		if ( !status ){
			this.firstCorner = new int[]{X, Y, Z};
			// this.secondCorner = null;
			this.undoable = false;
			this.pastePoint = new int[]{X, Y, Z};
		}
		
		// second corner
		else{
			this.secondCorner = new int[]{X, Y, Z};
			this.lastCopiedCuboid = null;
			this.lastSelectedCuboid = null;
			this.pastePoint = null;
			sortCorners();
		}
		
		status = !status;
		return status;
	}
	
	private void sortCorners(){
		// Gives all lower values to the first corner		
		int[] temp = new int[]{ this.secondCorner[0], this.secondCorner[1], this.secondCorner[2] };
		if( this.firstCorner[0] > this.secondCorner[0] ){
			this.secondCorner[0] = this.firstCorner[0];
			this.firstCorner[0] = temp[0];
		}
		if( this.firstCorner[1] > this.secondCorner[1] ){
			this.secondCorner[1] = this.firstCorner[1];
			this.firstCorner[1] = temp[1];
		}
		if( this.firstCorner[2] > this.secondCorner[2] ){
			this.secondCorner[2] = this.firstCorner[2];
			this.firstCorner[2] = temp[2];
		}
	}
	
}
