import java.util.HashMap;
import java.util.Map.Entry;

/*
 * Here happens all the raw 'treatment' of selected areas, such as filling, empty-ing, replacing etc...
 */

public class CuboidAction {
	static Server server = etc.getServer();
	static HashMap<String, CuboidSelection> playerSelection = new HashMap<String, CuboidSelection>();
	static Object lock = new Object();
	static int[] blocksToBeQueued = {37, 38, 39, 40, 50, 55, 63, 66, 69, 75, 76, 81, 83};
	
	public static CuboidSelection getPlayerSelection(String playerName){
		if ( !playerSelection.containsKey(playerName) ){
			playerSelection.put(playerName, new CuboidSelection());
		}
		return playerSelection.get(playerName);
	}
	
	public static boolean setPoint(String playerName, int X, int Y, int Z){
		return getPlayerSelection(playerName).selectCorner(X, Y, Z);
	}
	
	public static void setBothPoint(String playerName, int[] coords){
		CuboidSelection selection = getPlayerSelection(playerName);
		selection.firstCorner[0] = coords[0];
		selection.firstCorner[1] = coords[1];
		selection.firstCorner[2] = coords[2];
		selection.secondCorner[0] = coords[3];
		selection.secondCorner[1] = coords[4];
		selection.secondCorner[2] = coords[5];
		selection.status = false;
	}
	
	public static int[] getPoint(String playerName, boolean secondPoint){
		if (secondPoint){
			return getPlayerSelection(playerName).secondCorner;
		}
		else{
			return getPlayerSelection(playerName).firstCorner;
		}
	}
	
	public static boolean isUndoAble(String playerName){
		return getPlayerSelection(playerName).undoable;
	}
	
	public static boolean isReady(String playerName, boolean deuxPoints){
		CuboidSelection selection = getPlayerSelection(playerName);
		if ( deuxPoints && !selection.status && selection.firstCorner != null ){
			return true;
		}
		else if( !deuxPoints && selection.status ) {
			selection.status = false;
			return true;
		}
		return false;
	}
	
	public static void copyCuboid(String playerName, boolean manual){
		copyCuboid( getPlayerSelection(playerName), manual);
	}
	
	private static void copyCuboid(CuboidSelection selection, boolean manual){
		copyCuboid(selection, selection.firstCorner[0], selection.secondCorner[0], selection.firstCorner[1],
				selection.secondCorner[1], selection.firstCorner[2], selection.secondCorner[2]);
				
		if (!manual){
			selection.undoable = true;
		}
	}
	
	private static void copyCuboid(CuboidSelection selection, int Xmin, int Xmax, int Ymin, int Ymax, int Zmin, int Zmax){
		int Xsize = Xmax-Xmin+1;
		int Ysize = Ymax-Ymin+1;
		int Zsize = Zmax-Zmin+1;
		
		int[][][] tableaux = new int[Xsize][][];
		for (int i = 0; i<Xsize; i++){
			tableaux[i] = new int[Ysize][];
			for (int j = 0; j < Ysize; ++j) {
				tableaux[i][j] = new int[Zsize];
				 for (int k = 0; k < Zsize; ++k)
					 tableaux[i][j][k] = server.getBlockIdAt( Xmin+i,Ymin+j,Zmin+k );
			}
		}
		
		selection.lastCopiedCuboid = tableaux;
		selection.pastePoint = new int[]{Xmin, Ymin, Zmin};
		selection.undoable = true;
	}
			
	private static boolean shoudBeQueued(int blockType) {
		for (int shoudBeQueued : blocksToBeQueued ){
			if ( blockType == shoudBeQueued )
				return true;
		}
		return false;
	}
	
	public static byte paste(String playerName){
		// Paste will occur from North-East to South-West
		
		CuboidSelection selection = getPlayerSelection(playerName);
		
		int Xsize = selection.lastCopiedCuboid.length;
		if (Xsize==0){
			return 1;
		}
		int Ysize = selection.lastCopiedCuboid[0].length;
		int Zsize = selection.lastCopiedCuboid[0][0].length;
		
		selection.lastSelectedCuboid = new int[Xsize][][];
		
		int curX, curY, curZ;
		HashMap<int[], Integer> queuedBlocks = new HashMap<int[], Integer>();
		
		synchronized(lock){
			for (int i = 0; i<Xsize; i++){
				selection.lastSelectedCuboid[i] = new int[Ysize][];
				for (int j = 0; j < Ysize; ++j) {
					selection.lastSelectedCuboid[i][j] = new int[Zsize];
					 for (int k = 0; k < Zsize; ++k){
						 curX = selection.pastePoint[0]+i;
						 curY = selection.pastePoint[1]+j;
						 curZ = selection.pastePoint[2]+k;
						 selection.lastSelectedCuboid[i][j][k] = server.getBlockIdAt(curX,curY,curZ);
						 if ( shoudBeQueued(selection.lastCopiedCuboid[i][j][k]) ){
							 queuedBlocks.put( new int[]{ curX, curY, curZ}, selection.lastCopiedCuboid[i][j][k]);
						 }
						 else{
							 server.setBlockAt( selection.lastCopiedCuboid[i][j][k], curX, curY, curZ );
						 }
					 }
				}
			}
			
			for ( Entry<int[], Integer> queuedBlock : queuedBlocks.entrySet() ){
				server.setBlockAt( queuedBlock.getValue(), queuedBlock.getKey()[0], queuedBlock.getKey()[1],
						queuedBlock.getKey()[2] );
			}
		}
		
		selection.undoable = true;
		return 0;
	}

	public static byte undo(String playerName){
		CuboidSelection selection = getPlayerSelection(playerName);
		
		int[][][] toPaste;
		if ( selection.lastSelectedCuboid != null ){
			toPaste = selection.lastSelectedCuboid;
		}
		else{
			toPaste = selection.lastCopiedCuboid;
		}
		
		int Xsize = toPaste.length;
		if (Xsize==0){
			return 1;
		}
		int Ysize = toPaste[0].length;
		int Zsize = toPaste[0][0].length;
		
		synchronized(lock){
			for (int i = 0; i<Xsize; i++){
				for (int j = 0; j < Ysize; ++j) {
					 for (int k = 0; k < Zsize; ++k){
						 server.setBlockAt( toPaste[i][j][k], selection.pastePoint[0]+i, selection.pastePoint[1]+j,
								 selection.pastePoint[2]+k );
					 }
				}
			}
		}
		
		selection.lastSelectedCuboid = null;
		selection.undoable = false;
		
		return 0;
	}
	
	public static byte saveCuboid(String playerName, String cuboidName){
		CuboidSelection selection = getPlayerSelection(playerName);
		int Xsize = selection.secondCorner[0]-selection.firstCorner[0]+1;
		int Ysize = selection.secondCorner[1]-selection.firstCorner[1]+1;
		int Zsize = selection.secondCorner[2]-selection.firstCorner[2]+1;
		
		int[][][] tableaux = new int[Xsize][][];
		for (int i = 0; i<Xsize; i++){
			tableaux[i] = new int[Ysize][];
			for (int j = 0; j < Ysize; ++j) {
				tableaux[i][j] = new int[Zsize];
				 for (int k = 0; k < Zsize; ++k)
					 tableaux[i][j][k] = server.getBlockIdAt( selection.firstCorner[0]+i, selection.firstCorner[1]+j,
							 selection.firstCorner[2]+k );
			}
		}
		
		return new CuboidContent(playerName, cuboidName, tableaux).save();	
	}
	
	public static byte loadCuboid(String playerName, String cuboidName){
		CuboidSelection selection = getPlayerSelection(playerName);
		CuboidContent data = new CuboidContent(playerName, cuboidName);
		
		if ( data.loadReturnCode == 0 ){
			
			int[][][] tableau = data.getData();
			int Xsize = tableau.length;
			int Ysize = tableau[0].length;
			int Zsize = tableau[0][0].length;
			
			copyCuboid( selection, selection.firstCorner[0], selection.firstCorner[0]+Xsize, selection.firstCorner[1],
					selection.firstCorner[1]+Ysize, selection.firstCorner[2], selection.firstCorner[2]+Zsize );

			synchronized(lock){
				for (int i = 0; i<Xsize; i++){
					for (int j = 0; j < Ysize; ++j) {
						 for (int k = 0; k < Zsize; ++k)
							 server.setBlockAt( tableau[i][j][k], selection.firstCorner[0]+i, selection.firstCorner[1]+j,
									 selection.firstCorner[2]+k );
					}
				}
			}
		}
		
		return data.loadReturnCode;
	}
	
	public static int blocksCount(String playerName){
		CuboidSelection selection = getPlayerSelection(playerName);		
		int Xsize = selection.secondCorner[0]-selection.firstCorner[0]+1;
		int Ysize = selection.secondCorner[1]-selection.firstCorner[1]+1;
		int Zsize = selection.secondCorner[2]-selection.firstCorner[2]+1;
		return Xsize*Ysize*Zsize;
	}
	
	public static void emptyCuboid(String playerName){
		CuboidSelection selection = getPlayerSelection(playerName);
		copyCuboid(selection, false);
		
		synchronized(lock){
			for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
				for ( int j = selection.firstCorner[1]; j<= selection.secondCorner[1]; j++ ){
					for ( int k = selection.firstCorner[2]; k<= selection.secondCorner[2]; k++ ){
						if ( server.getBlockIdAt(i, j, k) == 79 ){	// Hack to remove ice
							server.setBlockAt(20,i,j,k);
						}
						server.setBlockAt(0,i,j,k);
					}
				}
			}
		}
		
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" emptied a cuboid");
	}
	
	public static void fillCuboid(String playerName, int bloctype){
		CuboidSelection selection = getPlayerSelection(playerName);
		copyCuboid(selection, false);
		
		synchronized(lock){	
			for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
				for ( int j = selection.firstCorner[1]; j<= selection.secondCorner[1]; j++ ){
					for ( int k = selection.firstCorner[2]; k<= selection.secondCorner[2]; k++ ){
						server.setBlockAt(bloctype,i,j,k);
					}
				}
			}
		}
		
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" filled a cuboid");
	}
	
	public static void replaceBlocks(String playerName, int[] replaceParams){
		CuboidSelection selection = getPlayerSelection(playerName);
		copyCuboid(selection, false);
		
		synchronized(lock){	
			int targetBlockIndex = replaceParams.length-1;
			for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
				for ( int j = selection.firstCorner[1]; j<= selection.secondCorner[1]; j++ ){
					for ( int k = selection.firstCorner[2]; k<= selection.secondCorner[2]; k++ ){
						for ( int l = 0; l < targetBlockIndex; l++ ){
							if( server.getBlockIdAt(i, j, k) == replaceParams[l] ){
								server.setBlockAt(replaceParams[targetBlockIndex],i,j,k);
							}
						}
					}
				}
			}
			if (CuboidPlugin.logging)
				CuboidPlugin.log.info(playerName+" replaced blocks inside a cuboid");
		}
	}
	
	public static void buildCuboidFaces(String playerName, int bloctype, boolean sixFaces){
		CuboidSelection selection = getPlayerSelection(playerName);
		copyCuboid(selection, false);
		
		synchronized(lock){
			for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
				for ( int j = selection.firstCorner[1]; j<= selection.secondCorner[1]; j++ ){
					server.setBlockAt(bloctype,i,j,selection.firstCorner[2]);
					server.setBlockAt(bloctype,i,j,selection.secondCorner[2]);
				}
			}		
			for ( int i = selection.firstCorner[1]; i<= selection.secondCorner[1]; i++ ){
				for ( int j = selection.firstCorner[2]; j<= selection.secondCorner[2]; j++ ){
					server.setBlockAt(bloctype,selection.firstCorner[0],i,j);
					server.setBlockAt(bloctype,selection.secondCorner[0],i,j);
				}
			}
			if (sixFaces){
				for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
					for ( int j = selection.firstCorner[2]; j<= selection.secondCorner[2]; j++ ){
						server.setBlockAt(bloctype,i,selection.firstCorner[1],j);
						server.setBlockAt(bloctype,i,selection.secondCorner[1],j);
					}
				}
			}
			if (CuboidPlugin.logging)
				CuboidPlugin.log.info(playerName+" built the "+((sixFaces)? "faces" : "walls")+" of a cuboid");
		}
	}
	
	public static void rotateCuboidContent(String playerName, int rotationType){	// TODO
		CuboidSelection selection = getPlayerSelection(playerName);
		copyCuboid(selection, false);
		
		synchronized(lock){
			if ( rotationType == 0){	// 90° clockwise
			}
			if ( rotationType == 1){	// 90° counet-clockwise
				
			}
			if ( rotationType == 2){	//	180°
				
			}
			if ( rotationType == 3){	// upside-down
							
			}
		}
		
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+"");
	}
	
	public static void moveCuboidContent(Player player, String movementType, int value){
		String playerName = player.getName();
		CuboidSelection selection = getPlayerSelection(playerName);
		copyCuboid(selection, false);
		
		synchronized(lock){						
			if( movementType.equalsIgnoreCase("East") ){
				copyCuboid(selection, selection.firstCorner[0], selection.secondCorner[0], selection.firstCorner[1],
						selection.secondCorner[1], selection.firstCorner[2]-value, selection.secondCorner[2]);
				int deleteIterator = 0;
				for ( int k = selection.secondCorner[2]; k>= selection.firstCorner[2]; k-- ){
					for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
						for ( int j = selection.firstCorner[1]; j<= selection.secondCorner[1]; j++ ){
							server.setBlockAt(selection.lastCopiedCuboid[i-selection.firstCorner[0]][j-selection.firstCorner[1]]
							    [k-selection.firstCorner[2]+1], i, j, k-value);
							if ( deleteIterator<value ){
								server.setBlockAt(0, i, j, k);	
							}
						}
					}
					deleteIterator++;
				}
				selection.firstCorner[2] -= value;
				selection.secondCorner[2] -= value;
			}
			else if( movementType.equalsIgnoreCase("North") ){
				copyCuboid(selection, selection.firstCorner[0]-value, selection.secondCorner[0], selection.firstCorner[1],
						selection.secondCorner[1], selection.firstCorner[2], selection.secondCorner[2]);
				int deleteIterator = 0;
				for ( int i = selection.secondCorner[0]; i>= selection.firstCorner[0]; i-- ){
					for ( int j = selection.firstCorner[1]; j<= selection.secondCorner[1]; j++ ){
						for ( int k = selection.firstCorner[2]; k<= selection.secondCorner[2]; k++ ){
							server.setBlockAt(selection.lastCopiedCuboid[i-selection.firstCorner[0]+1][j-selection.firstCorner[1]]
							    [k-selection.firstCorner[2]], i-value, j, k);
							if ( deleteIterator<value ){
								server.setBlockAt(0, i, j, k);	
							}
						}
					}
					deleteIterator++;
				}
				selection.firstCorner[0] -= value;
				selection.secondCorner[0] -= value;
			}
			else if( movementType.equalsIgnoreCase("South") ){
				copyCuboid(selection, selection.firstCorner[0], selection.secondCorner[0]+value, selection.firstCorner[1],
						selection.secondCorner[1], selection.firstCorner[2], selection.secondCorner[2]);
				int deleteIterator = 0;
				for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
					for ( int j = selection.firstCorner[1]; j<= selection.secondCorner[1]; j++ ){
						for ( int k = selection.firstCorner[2]; k<= selection.secondCorner[2]; k++ ){
							server.setBlockAt(selection.lastCopiedCuboid[i-selection.firstCorner[0]][j-selection.firstCorner[1]]
							    [k-selection.firstCorner[2]], i+value, j, k);
							if ( deleteIterator<value ){
								server.setBlockAt(0, i, j, k);	
							}
						}
					}
					deleteIterator++;
				}
				selection.firstCorner[0] += value;
				selection.secondCorner[0] += value;
			}
			else if( movementType.equalsIgnoreCase("West") ){
				copyCuboid(selection, selection.firstCorner[0], selection.secondCorner[0], selection.firstCorner[1],
						selection.secondCorner[1], selection.firstCorner[2], selection.secondCorner[2]+value);
				int deleteIterator = 0;
				for ( int k = selection.firstCorner[2]; k<= selection.secondCorner[2]; k++ ){
					for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
						for ( int j = selection.firstCorner[1]; j<= selection.secondCorner[1]; j++ ){
							server.setBlockAt(selection.lastCopiedCuboid[i-selection.firstCorner[0]][j-selection.firstCorner[1]]
							    [k-selection.firstCorner[2]], i, j, k+value);
							if ( deleteIterator<value ){
								server.setBlockAt(0, i, j, k);	
							}
						}
					}
					deleteIterator++;
				}
				selection.firstCorner[2] += value;
				selection.secondCorner[2] += value;
			}
			else if( movementType.equalsIgnoreCase("Up") ){
				copyCuboid(selection, selection.firstCorner[0], selection.secondCorner[0], selection.firstCorner[1],
						selection.secondCorner[1]+value, selection.firstCorner[2], selection.secondCorner[2]);
				int deleteIterator = 0;
				for ( int j = selection.firstCorner[1]; j<= selection.secondCorner[1]; j++ ){
					for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
						for ( int k = selection.firstCorner[2]; k<= selection.secondCorner[2]; k++ ){
							server.setBlockAt(selection.lastCopiedCuboid[i-selection.firstCorner[0]][j-selection.firstCorner[1]]
							    [k-selection.firstCorner[2]], i, j+value, k);
							if ( deleteIterator<value ){
								server.setBlockAt(0, i, j, k);	
							}
						}
					}
					deleteIterator++;
				}
				selection.firstCorner[1] += value;
				selection.secondCorner[1] += value;
			}
			else if( movementType.equalsIgnoreCase("Down") ){
				copyCuboid(selection, selection.firstCorner[0], selection.secondCorner[0], selection.firstCorner[1]-value,
						selection.secondCorner[1], selection.firstCorner[2], selection.secondCorner[2]);
				int deleteIterator = 0;
				for ( int j = selection.secondCorner[1]; j>= selection.firstCorner[1]; j-- ){
					for ( int i = selection.firstCorner[0]; i<= selection.secondCorner[0]; i++ ){
						for ( int k = selection.firstCorner[2]; k<= selection.secondCorner[2]; k++ ){
							server.setBlockAt(selection.lastCopiedCuboid[i-selection.firstCorner[0]][j-selection.firstCorner[1]+1]
							    [k-selection.firstCorner[2]], i, j-value, k);
							if ( deleteIterator<value ){
								server.setBlockAt(0, i, j, k);	
							}
						}
					}
					deleteIterator++;
				}
				selection.firstCorner[1] -= value;
				selection.secondCorner[1] -= value;
			}
			else{
				player.sendMessage(Colors.Rose + "Wrong parameter : " + movementType);
				return;
			}
			
			player.sendMessage(Colors.LightGreen + "Cuboid successfuly moved.");
			if (CuboidPlugin.logging)
				CuboidPlugin.log.info(playerName+" moved a cuboid : "+value+" block(s) " + movementType);
		}
	}
	
	public static void buildCircle(String playerName,int radius, int blocktype, int height, boolean fill){
		CuboidSelection selection = getPlayerSelection(playerName);
					
		int Xcenter = selection.firstCorner[0];
		int Ycenter = selection.firstCorner[1];
		int Zcenter = selection.firstCorner[2];
		int Xmin=Xcenter-radius;
		int Xmax=Xcenter+radius;
		int Zmin=Zcenter-radius;
		int Zmax=Zcenter+radius;
		int Ymin = (height+Ycenter >= Ycenter) ? Ycenter : height+Ycenter;
		int Ymax = (height+Ycenter <= Ycenter) ? Ycenter : height+Ycenter;
		
		copyCuboid(selection, Xmin, Xmax, Ymin, Ymax, Zmin, Zmax);

		synchronized(lock){
			for ( int i = Xmin; i<= Xmax; i++ ){
				for ( int j = Ymin; j<= Ymax; j++ ){
					for (int k = Zmin; k <=Zmax ; k++){
					    double diff = Math.sqrt( Math.pow(i-Xcenter, 2.0D) + Math.pow(k-Zcenter, 2.0D) );
					    if( diff<radius+0.5 && ( fill || (!fill && diff>radius-0.5) ) ){
					    	server.setBlockAt(blocktype,i,j,k);
					    }
					}
				}
			}
		}
		
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" built a "+((height!=0)? "cylinder" : "circle") );
	}
	
	public static void buildShpere(String playerName,int radius, int blocktype, boolean fill){
		CuboidSelection selection = getPlayerSelection(playerName);
		
		int Xcenter = selection.firstCorner[0];
		int Ycenter = selection.firstCorner[1];
		int Zcenter = selection.firstCorner[2];
		int Xmin=Xcenter-radius;
		int Xmax=Xcenter+radius;
		int Ymin=Ycenter-radius;
		int Ymax=Ycenter+radius;
		int Zmin=Zcenter-radius;
		int Zmax=Zcenter+radius;
		
		copyCuboid(selection, Xmin, Xmax, Ymin, Ymax, Zmin, Zmax);
		
		for ( int i = Xmin; i<= Xmax; i++ ){
			for ( int j = Ymin; j<= Ymax; j++ ){
				for (int k = Zmin; k <=Zmax ; k++){
				    double diff = Math.sqrt( Math.pow(i-Xcenter, 2.0D) + Math.pow(j-Ycenter, 2.0D) + Math.pow(k-Zcenter, 2.0D) );
				    if( diff<radius+0.5 && (fill || (!fill && diff>radius-0.5) ) ){
				    	server.setBlockAt(blocktype,i,j,k);
				    }
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" built a "+((fill)? "ball" : "sphere") );
	}
	
	public static void buildPyramid(String playerName, int radius, int blockType, boolean fill){
		CuboidSelection selection = getPlayerSelection(playerName);

		int Xcenter = selection.firstCorner[0];
		int Ycenter = selection.firstCorner[1];
		int Zcenter = selection.firstCorner[2];
		int Xmin = Xcenter-radius;
		int Xmax = Xcenter+radius;
		int Zmin = Zcenter-radius;
		int Zmax = Zcenter+radius;
		int Ymin = Ycenter;
		int Ymax = Ycenter+radius;
		
		copyCuboid(selection, Xmin, Xmax, Ymin, Ymax, Zmin, Zmax);

		for (int j = Ymin; j <= Ymax; j++){
			for (int i = Xmin; i <= Xmax; i++){
				for (int k = Zmin; k <= Zmax; k++){
					server.setBlockAt(blockType, i, j, k);
				}
			}
			Xmin += 1;
			Xmax -= 1;
			Zmin += 1;
			Zmax -= 1;
		}
		
		if (!fill && radius > 2){	// easy, but destructive way
			Xmin = Xcenter-radius+2;
			Xmax = Xcenter+radius-2;
			Zmin = Zcenter-radius+2;
			Zmax = Zcenter+radius-2;
			Ymin = Ycenter+1;
			Ymax = Ycenter+radius-1;
			for (int j = Ymin; j <= Ymax; j++){
				for (int i = Xmin; i <= Xmax; i++){
					for (int k = Zmin; k <= Zmax; k++){
						server.setBlockAt(0, i, j, k);
					}
				}
				Xmin += 1;
				Xmax -= 1;
				Zmin += 1;
				Zmax -= 1;
			}
		}
		
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" built a "+((fill)? "filled " : "") + "pyramid." );		
	}
	
	public static void updateChestsState(int firstX, int firstY, int firstZ, int secondX, int secondY, int secondZ){
		int startX = ( firstX <= secondX ) ? firstX : secondX;
		int startY = ( firstY <= secondY ) ? firstY : secondY;
		int startZ = ( firstZ <= secondZ ) ? firstZ : secondZ;
		
		int endX = ( firstX <= secondX  ) ? secondX : firstX;
		int endY = ( firstY <= secondY ) ? secondY : firstY;
		int endZ = ( firstZ <= secondZ ) ? secondZ : firstZ;
		
		for ( int i = startX; i<= endX; i++ ){
			for ( int j = startY; j<= endY; j++ ){
				for ( int k = startZ; k<= endZ; k++ ){
					if ( server.getBlockIdAt(i, j, k)==54 && server.getComplexBlock(i, j, k)!=null ){
						server.getComplexBlock(i, j, k).update();
					}
					
				}
			}
		}
	}

}
