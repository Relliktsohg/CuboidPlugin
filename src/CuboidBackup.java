import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/*
 * Serializing the content of a cuboid area
 * Needs a lot of work (a chest-content management, for instance)
 */

@SuppressWarnings("serial")
public class CuboidBackup implements Serializable {
	
	private String name;
	private int[][][] cuboidData;
	private int[] coords;
	
	CuboidBackup( CuboidB cuboid, boolean store ){
		this.name = cuboid.name;
		this.coords = cuboid.coords;
		if (store)
			storeCuboidData();
	}
		
	public int[][][] getData(){
		return this.cuboidData;	
	}
	
	private void storeCuboidData(){
		Server server = etc.getServer();
        int Xsize = this.coords[3] - this.coords[0] +1;
		int Ysize = this.coords[4] - this.coords[1] +1;
		int Zsize = this.coords[5] - this.coords[2] +1;
		this.cuboidData = new int[Xsize][][];
		for (int i = 0; i < Xsize; i++){
			this.cuboidData[i] = new int[Ysize][];
			for (int j = 0; j < Ysize; ++j) {
				this.cuboidData[i][j] = new int[Zsize];
				for (int k = 0; k < Zsize; ++k){
					this.cuboidData[i][j][k] = server.getBlockIdAt(this.coords[0]+i, this.coords[1]+j, this.coords[2]+k);
				}
			}
		}
	}
	
	private void restoreCuboidData(){
		Server server = etc.getServer();
		int Xsize = cuboidData.length;
		int Ysize = cuboidData[0].length;
		int Zsize = cuboidData[0][0].length;
		for (int i = 0; i<Xsize; i++){
			for (int j = 0; j < Ysize; ++j) {
				for (int k = 0; k < Zsize; ++k){
					server.setBlockAt(this.cuboidData[i][j][k], this.coords[0]+i, this.coords[1]+j, this.coords[2]+k);
				}
			}
		}
	}
	
	public byte writeToDisk(){
		// checking folders
		File cuboidFolder = new File("cuboids");
        try {
            if (!cuboidFolder.exists()) {
            	cuboidFolder.mkdir();
            }
            File subFolder = new File("cuboids/areaBackups");
            try {
	            if (!subFolder.exists()){
	            	subFolder.mkdir();
	            }
            }
            catch( Exception e){
            	e.printStackTrace();
            	return 1;
            }
        }
        catch( Exception e){
        	e.printStackTrace();
        	return 1;
        }
        
        // writing
		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					new BufferedOutputStream(
							new FileOutputStream(
									new File("cuboids/areaBackups/" + this.name + ".cuboid"))));
	        oos.writeObject(this.cuboidData);
	        oos.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			return 2;
	    }
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info("New cuboidArea backup : " + this.name);
		return 0;
	}
	
	public byte loadFromDisc(){
		try {
			ObjectInputStream ois = new ObjectInputStream(
					new BufferedInputStream(
							new FileInputStream(
									new File("cuboids/areaBackups/" + this.name + ".cuboid"))));
	        try {
	        	this.cuboidData = (int[][][])( ois.readObject() );
	        }
	        catch (Exception e) {
	        	e.printStackTrace();
	        	return 3;
	        }
	        ois.close();
		}
		catch (FileNotFoundException e) {
				e.printStackTrace();
				return 1;
	    }
		catch (IOException e) {
			e.printStackTrace();
			return 2;
		}
		
		restoreCuboidData();
		
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info("Loaded cuboidArea backup : "+this.name);
		return 0;
	}
	
	public boolean deleteFromDisc(){
		File fileToDelete = new File("cuboids/areaBackups/" + this.name + ".cuboid");
		if ( fileToDelete.exists() ){
			return fileToDelete.delete();
		}
		return true;
	}
}
