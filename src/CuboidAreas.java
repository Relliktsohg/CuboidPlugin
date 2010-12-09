import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Scanner;


/*
 * This is the interface for stored cuboidB areas.
 * Loading / Saving happens here, with all needed retro-compatibility
 * In the longrun, I want to replace ArrayList<CuboidB> by a Hilbert R-tree, to optimize performance
 */

public class CuboidAreas {
	static String currentDataVersion = "B";
	static int addedHeight = 0;
	static boolean newestHavePriority = true;
	static ArrayList<CuboidB> listOfCuboids;
	
	@SuppressWarnings("unchecked")
	public static void loadCuboidAreas(){
		listOfCuboids = new ArrayList<CuboidB>();
		
		File dataSource = new File("cuboids/cuboidAreas.dat");
		if ( !dataSource.exists() ){
			if ( new File("cuboids/protectedCuboids.txt").exists() )
				readFromTxtFile();
			else
				CuboidPlugin.log.info("CuboidPlugin : No datafile to load from (its fine)");
			return;
		}
		try {
			File versionFile = new File("cuboids/cuboidAreas.version");
			if ( !versionFile.exists() ){
				readFromOldFormat(null);
				return;
			}
			String dataVersion = new Scanner(versionFile).nextLine();
			if ( dataVersion.equalsIgnoreCase(currentDataVersion) ){
				ObjectInputStream ois =
					new ObjectInputStream(
						new BufferedInputStream(
							new FileInputStream(
								new File("cuboids/cuboidAreas.dat"))));
		       listOfCuboids = (ArrayList<CuboidB>)( ois.readObject() );
		       ois.close();
		       CuboidPlugin.log.info("CuboidPlugin : cuboidAreas.dat (format " + currentDataVersion + ") loaded");
			}
			else{
				readFromOldFormat(dataVersion);
			}
		} catch (Exception e) {
			CuboidPlugin.log.severe("Cuboid plugin : Error while reading cuboidAreas.dat");
		}
	}
	
	public static void readFromTxtFile(){
		CuboidPlugin.log.info("CuboidPlugin : protectedCuboids.txt found, initializing conversion...");
		File dataSource = new File("cuboids/protectedCuboids.txt");
		try {
			Scanner scanner = new Scanner(dataSource);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("#") || line.equals("")) {
					continue;
				}
				String[] donnees = line.split(",");
				if (donnees.length < 8) {
					continue;
				}
				
				CuboidB newArea = new CuboidB();
				int[] tempCoords = new int[6];
				for (short i=0; i<6; i++){
					tempCoords[i] = Integer.parseInt(donnees[i]);
				}
				newArea.coords[0] = (tempCoords[0] <= tempCoords[3]) ? tempCoords[0] : tempCoords[3];
				newArea.coords[1] = (tempCoords[1] <= tempCoords[4]) ? tempCoords[1] : tempCoords[4];
				newArea.coords[2] = (tempCoords[2] <= tempCoords[5]) ? tempCoords[2] : tempCoords[5];
				newArea.coords[3] = (tempCoords[0] >= tempCoords[3]) ? tempCoords[0] : tempCoords[3];
				newArea.coords[4] = (tempCoords[1] >= tempCoords[4]) ? tempCoords[1] : tempCoords[4];
				newArea.coords[5] = (tempCoords[2] >= tempCoords[5]) ? tempCoords[2] : tempCoords[5];
				for (String owner : donnees[6].trim().split(" ")){
					newArea.allowedPlayers.add(owner);
				}
				newArea.name = donnees[7];
				newArea.protection = true;
				listOfCuboids.add(newArea);
			}
			scanner.close();
			CuboidPlugin.log.info("CuboidPlugin : Conversion to new format successful.");
			CuboidPlugin.log.info("CuboidPlugin : cuboids areas loaded.");
		} catch (Exception e) {
			CuboidPlugin.log.severe("Cuboid plugin : Error while reading protectedCuboids.txt");
		}	
	}
	
	@SuppressWarnings("unchecked")
	public static void readFromOldFormat( String dataVersion ){
		if ( dataVersion == null ){
			ArrayList<Cuboid> oldCuboids;
			try {
				ObjectInputStream ois =
					new ObjectInputStream(
						new BufferedInputStream(
							new FileInputStream(
								new File("cuboids/cuboidAreas.dat"))));
				oldCuboids = (ArrayList<Cuboid>)( ois.readObject() );
				ois.close();
			}
			catch (Exception e) {
				oldCuboids = new ArrayList<Cuboid>();
				CuboidPlugin.log.severe("Cuboid plugin : Error while reading cuboidAreas.dat");
			}
			
			listOfCuboids = new ArrayList<CuboidB>();
			for ( Cuboid oldCuboid : oldCuboids ){
				CuboidB newCuboid = new CuboidB();
				newCuboid.name = oldCuboid.name;
				newCuboid.coords = oldCuboid.coords;
				newCuboid.allowedPlayers = oldCuboid.allowedPlayers;
				newCuboid.protection = oldCuboid.protection;
				newCuboid.restricted = oldCuboid.restricted;
				newCuboid.inventories = oldCuboid.inventories;
				newCuboid.warning = oldCuboid.warning;
				newCuboid.welcomeMessage = oldCuboid.welcomeMessage;
				newCuboid.farewellMessage = oldCuboid.farewellMessage;
				newCuboid.presentPlayers = oldCuboid.presentPlayers;
				newCuboid.disallowedCommands = oldCuboid.disallowedCommands;
				newCuboid.playerInventories = oldCuboid.playerInventories;
				listOfCuboids.add( newCuboid );
			}
			
			CuboidPlugin.log.info("CuboidPlugin : conversion of cuboidAreas.dat from f.A to f.B sucessful");
			CuboidPlugin.log.info("CuboidPlugin : cuboidAreas.dat loaded.");
		}
		else{
			CuboidPlugin.log.severe("CuboidPlugin : unsupported data version");
			listOfCuboids = new ArrayList<CuboidB>();
		}
	}
	
	public static void writeCuboidAreas(){
		CuboidPlugin.log.info("CuboidPlugin : Saving data to hard drive...");
        try {
        	ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
    				new File("cuboids/cuboidAreas.dat"))));
			oos.writeObject(listOfCuboids);
			oos.close();
			FileWriter writer = new FileWriter("cuboids/cuboidAreas.version");
			writer.write(currentDataVersion);
			writer.close();
		} catch (IOException e1) {
			CuboidPlugin.log.severe("CuboidPlugin : Error while writing data");
		}
		CuboidPlugin.log.info("CuboidPlugin : Done.");
	}
	
	////////////////////////////
	////    DATA SENDING    ////
	////////////////////////////
	
	public static CuboidB findCuboidArea(int X, int Y, int Z){
		CuboidB lastEntry = null;
		for (CuboidB cuboid : listOfCuboids){
			if ( cuboid.contains(X, Y, Z) ){
				if ( newestHavePriority ){
					lastEntry = cuboid;
				}
				else{
					return cuboid;
				}
			}	
		}
		return lastEntry;
	}
	
	public static CuboidB findCuboidArea(String cuboidName){
		CuboidB lastEntry = null;
		for (CuboidB cuboid : listOfCuboids){
			if ( cuboid.name.equalsIgnoreCase(cuboidName) ){
				if ( newestHavePriority ){
					lastEntry = cuboid;
				}
				else{
					return cuboid;
				}
			}	
		}
		return lastEntry;
	}
	
	public static void refreshPlayerList(Location loc, Player player){
		for (CuboidB cuboid : listOfCuboids){
			if ( cuboid.contains((int)loc.x, (int)loc.y, (int)loc.z) && cuboid.presentPlayers.contains(player.getName())
					&& !cuboid.contains((int)player.getX(), (int)player.getY(), (int)player.getZ())){
				cuboid.presentPlayers.remove(player.getName());
			}	
		}
	}
	
	public static String displayCuboidsList(){
		if (listOfCuboids.size() == 0){
			return "<list is empty>";
		}
		
		String list = "";
		for (CuboidB cuboid : listOfCuboids){
			list += " " + cuboid.name;
		}
		return list.trim();
	}
	
	public static void displayOwnedList(Player player){
		String list = "";
		for (CuboidB cuboid : listOfCuboids){
			if ( cuboid.isOwner(player) )
				list += " " + cuboid.name;
		}
		if ( list.equalsIgnoreCase("") )
			player.sendMessage(Colors.Yellow + "Areas you own : "+ Colors.White + "<list is empty>");
		else
			player.sendMessage(Colors.Yellow + "Areas you own :" + Colors.White + list);
	}
	
	//////////////////////////////
	////    DATA TREATMENT    ////
	//////////////////////////////
	
	public static boolean createCuboidArea(Player player, String cuboidName){
		String playerName = player.getName();
		if ( findCuboidArea(cuboidName) != null ){
			player.sendMessage( Colors.Rose + "There is already an area with that name" );
			if (CuboidPlugin.logging)
				CuboidPlugin.log.info(playerName + " failed to create a cuboid area named " + cuboidName + " (aleady used)");
			return false;
		}
		int[] firstPoint = CuboidAction.getPoint(playerName, false);
		int[] secondPoint = CuboidAction.getPoint(playerName, true);
		
		CuboidB newCuboid = new CuboidB();
		for (short i = 0; i < 3; i++)
			newCuboid.coords[i] = firstPoint[i];
		for (short i = 0; i < 3; i++)
			newCuboid.coords[i+3] = secondPoint[i];
		newCuboid.allowedPlayers.add("o:" + playerName);
		newCuboid.name = cuboidName;
		if ( CuboidPlugin.protectionOnDefault ){
			newCuboid.protection = true;
		}
		if ( CuboidPlugin.restrictedOnDefault ){
			newCuboid.restricted = true;
		}
		if ( CuboidPlugin.localInventoryOnDefault ){
			newCuboid.inventories = true;
		}
		if ( CuboidPlugin.sanctuaryOnDefault ){
			newCuboid.sanctuary = true;	
		}
		if ( CuboidPlugin.creeperDisabledOnDefault ){
			newCuboid.creeper = false;
		}
		if ( CuboidPlugin.pvpDisabledOnDefault ){
			newCuboid.PvP = false;
		}
		if ( CuboidPlugin.healOnDefault ){
			newCuboid.heal = true;
		}
		listOfCuboids.add(newCuboid);
		
		player.sendMessage( Colors.LightGreen + "Cuboid area successfuly created" );
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" created a new cuboid area named "+cuboidName);
		return true;
	}
	
	public static boolean protectCuboidArea(Player player, ArrayList<String> ownersList, String cuboidName, boolean highProtect){
		String playerName = player.getName();
		if ( findCuboidArea(cuboidName) != null ){
			player.sendMessage( Colors.Rose + "There is already an area with that name" );
			if (CuboidPlugin.logging)
				CuboidPlugin.log.info(playerName + " failed to create a protected area named " + cuboidName + " (aleady used)");
			return false;
		}
		
		// Getting the corners' coordinates and correcting them if necessary
		int[] firstPoint = CuboidAction.getPoint(playerName, false);
		int[] secondPoint = CuboidAction.getPoint(playerName, true);
		if ( highProtect ){
			firstPoint[1] = 0;
			secondPoint[1] = 128;
		}
		else if( firstPoint[1] == secondPoint[1] ){
			firstPoint[1] -= addedHeight;
			secondPoint[1] += addedHeight;
		}
		
		CuboidB newCuboid = new CuboidB();
		for (short i = 0; i < 3; i++)
			newCuboid.coords[i] = firstPoint[i];
		for (short i = 0; i < 3; i++)
			newCuboid.coords[i+3] = secondPoint[i];
		newCuboid.allowedPlayers = ownersList;
		newCuboid.name = cuboidName;
		newCuboid.protection = true;
		
		listOfCuboids.add(newCuboid);
		CuboidAction.updateChestsState(firstPoint[0], firstPoint[1], firstPoint[2], secondPoint[0], secondPoint[1], secondPoint[2]);
		
		player.sendMessage( Colors.LightGreen + "Protected area successfuly created" );
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" created a new protected area named "+cuboidName);
		
		return true;
	}
	
	public static void moveCuboidArea(Player player, CuboidB cuboid) {
		String playerName = player.getName();
		int[] firstPoint = CuboidAction.getPoint(playerName, false);
		int[] secondPoint = CuboidAction.getPoint(playerName, true);
		
		if (cuboid.protection)
			CuboidAction.updateChestsState(cuboid.coords[0], cuboid.coords[1], cuboid.coords[2], cuboid.coords[3],
						cuboid.coords[4], cuboid.coords[5]);
		
		cuboid.coords[0] = firstPoint[0];
		cuboid.coords[1] = firstPoint[1];
		cuboid.coords[2] = firstPoint[2];
		cuboid.coords[3] = secondPoint[0];
		cuboid.coords[4] = secondPoint[1];
		cuboid.coords[5] = secondPoint[2];
		
		if (cuboid.protection)
			CuboidAction.updateChestsState(firstPoint[0], firstPoint[1], firstPoint[2], secondPoint[0], secondPoint[1],
					secondPoint[2]);
		
		byte returnCode = new CuboidBackup(cuboid, true).writeToDisk();
		player.sendMessage( Colors.LightGreen + "return code of backup : " + returnCode );
		
		player.sendMessage( Colors.LightGreen + "Cuboid area successfuly moved" );
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName + " moved a cuboid area named " + cuboid.name);
	}
	
	public static void removeCuboidArea(Player player, CuboidB cuboid){
		String playerName = player.getName();
		listOfCuboids.remove(cuboid);
		if (cuboid.protection)
			CuboidAction.updateChestsState(cuboid.coords[0], cuboid.coords[1], cuboid.coords[2], cuboid.coords[3],
					cuboid.coords[4], cuboid.coords[5]);
		
		if ( new CuboidBackup(cuboid, false).deleteFromDisc() ){
			player.sendMessage( Colors.LightGreen + "Cuboid area successfuly removed" );
			if (CuboidPlugin.logging)
				CuboidPlugin.log.info(playerName + " removed a cuboid area named " + cuboid.name);
		}
		else{
			player.sendMessage( Colors.Yellow + "Cuboid area removed, but unable to remove the backup file." );
			CuboidPlugin.log.info("Unable to delete a backup file when removing a cuboid area (" + cuboid.name + ")");
		}   
	}
	
	public static void treatAllowance( Player player, String[] list, CuboidB cuboid ){
		for (String data : list){
			if ( data.charAt(0)=='/' )
				cuboid.allowCommand(data);
			else
				cuboid.allowPlayer(data);
		}
		player.sendMessage(Colors.LightGreen + "Area's whitelist updated");
	}
	
	public static void treatDisallowance( Player player, String[] list, CuboidB cuboid ){	
		for (String data : list){
			if ( data.charAt(0)=='/' )
				cuboid.disallowCommand(data);
			else
				cuboid.disallowPlayer(data);
		}
		player.sendMessage(Colors.LightGreen + "Area's whitelist updated");
	}
}
