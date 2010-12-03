import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("serial")
public class Cuboid implements Serializable{
	// old data format, kept here for retro-compatibility
	String name;
	int[] coords;	 //	int[]{firstX, firstY, firstZ, secondX, secondY, secondZ}
	boolean protection;
	boolean restricted;
	boolean inventories;
	ArrayList<String> allowedPlayers;
	String welcomeMessage;
	String farewellMessage;
	String warning;
	ArrayList<String> presentPlayers;
	ArrayList<String> disallowedCommands;
	HashMap<String, CuboidInventory> playerInventories;
	
	public Cuboid(){
		this.name = "noname";
		this.coords = new int[6];
		this.allowedPlayers = new ArrayList<String>();
		this.protection = false;
		this.restricted = false;
		this.inventories = false;
		this.warning = null;
		this.welcomeMessage = null;
		this.farewellMessage = null;
		this.presentPlayers = new ArrayList<String>();
		this.disallowedCommands = new ArrayList<String>();
		this.playerInventories = new HashMap<String, CuboidInventory>();
	}
	
	public boolean contains(int X, int Y, int Z){
		if( X >= coords[0] && X <= coords[3] && Z >= coords[2] && Z <= coords[5] && Y >= coords[1] && Y <= coords[4])
			return true;
		return false;
	}
	
	public boolean isAllowed( Player player ){
		String playerName = player.getName().toLowerCase();
		for (String allowedPlayer : allowedPlayers){
			if ( allowedPlayer.equalsIgnoreCase(playerName) || allowedPlayer.equalsIgnoreCase("o:"+playerName) ){
				return true;
			}
			if ( allowedPlayer.startsWith("g:") && player.isInGroup(allowedPlayer.substring(2)) ){
				return true;
			}
		}
		return false;
	}
	
	public boolean isAllowed( String command ){
		for (String disallowed : disallowedCommands){
			if ( command.equals(disallowed) )
				return false;
		}
		return true;
	}
	
	public boolean isOwner( Player player ){
		String playerName = "o:" + player.getName();
		for( String allowedPlayer : allowedPlayers ){
			if ( allowedPlayer.equalsIgnoreCase(playerName) ){
				return true;
			}
		}
		return false;
	}
	
	public void allowPlayer( String playerName ){
		boolean done = false;
		boolean newIsOwner = false;
		if ( playerName.startsWith("o:") ){
			playerName = playerName.substring(2);	
			newIsOwner = true;
		}
		
		for ( int j = 0; j < this.allowedPlayers.size() && !done; j++ ){
			String allowedPlayer = this.allowedPlayers.get(j);
			
			// if the new player already is in the list as simple allowed
			if ( allowedPlayer.equalsIgnoreCase(playerName) ){
				// we switch him to owner if needed
				if ( newIsOwner ){
					this.allowedPlayers.set(j, "o:" + playerName);
				}
				// we've found the guy, no need to add him again. 
				done = true;
			}
			
			// if the new player already is an owner, no need to go further
			if ( allowedPlayer.equalsIgnoreCase("o:" + playerName) ){
				done = true;
			}
		}
		
		// If the player wasn't found, we add him.
		if ( !done ){
			this.allowedPlayers.add( ((newIsOwner) ? "o:" : "") + playerName );
		}
	}
	
	public void disallowPlayer( String playerName ){
		this.allowedPlayers.remove( playerName );
	}
	
	public void disallowCommand( String command ){
		if ( !disallowedCommands.contains(command) )
			disallowedCommands.add(command);
	}
	
	public void allowCommand( String command ){
		disallowedCommands.remove(command);
	}
	
	public void playerEnters( Player player ){
		this.presentPlayers.add(player.getName());
		if ( this.welcomeMessage != null )
			player.sendMessage(Colors.Yellow + this.welcomeMessage);
		if ( this.inventories ){
			CuboidInventory cuboidInventory;
			boolean newVisitor = true;
			if (playerInventories.containsKey(player.getName())){
				cuboidInventory = playerInventories.get(player.getName());
				newVisitor = false;
			}
			else{
				cuboidInventory = new CuboidInventory();
			}
			Inventory outsideInventory = player.getInventory();
			
			// storage of inventory
			cuboidInventory.outside = new ArrayList<CuboidItem>();
			for (int i=0; i<outsideInventory.getArray().length; i++){
				Item item = outsideInventory.getItemFromSlot(i);
				if (item != null)
					cuboidInventory.outside.add(new CuboidItem(item));;
			}
			outsideInventory.clearContents();
			outsideInventory.updateInventory();
			playerInventories.put(player.getName(), cuboidInventory);
			
			// restore old inventory
			if (!newVisitor){
				for (CuboidItem item : cuboidInventory.inside){
					player.giveItem( new Item(item.itemId, item.amount, item.slot) );
				}
			}
		}
	}
	
	public void playerLeaves( Player player ){
		this.presentPlayers.remove(player.getName());
		if ( this.farewellMessage != null )
			player.sendMessage(Colors.Yellow + this.farewellMessage);
		if ( this.inventories ){
			CuboidInventory cuboidInventory = playerInventories.get(player.getName());
			Inventory insideInventory = player.getInventory();
			
			// storage of inventory
			cuboidInventory.inside = new ArrayList<CuboidItem>();
			for (int i=0; i<insideInventory.getArray().length; i++){
				Item item = insideInventory.getItemFromSlot(i);
				if (item != null)
					cuboidInventory.inside.add(new CuboidItem(item));
			}
			insideInventory.clearContents();
			insideInventory.updateInventory();
			playerInventories.put(player.getName(), cuboidInventory);
			
			// restore old inventory
			for (CuboidItem item : cuboidInventory.outside){
				player.giveItem(new Item(item.itemId, item.amount, item.slot));
			}
		}
	}

	public void printInfos(Player player, boolean allowed, boolean players, boolean commands ){
		player.sendMessage(Colors.Yellow + "----    Area information    ----");
		player.sendMessage(Colors.Yellow + "Name : " + Colors.White + this.name);
		player.sendMessage(Colors.Yellow + "Protection : " + Colors.White + (this.protection ? "enabled" : "disabled"));
		player.sendMessage(Colors.Yellow + "Restriction : " + Colors.White + (this.restricted ? "enabled" : "disabled"));
		player.sendMessage(Colors.Yellow + "Area specific inventory : " + Colors.White + (this.inventories ? "yes" : "no"));
		if ( allowed ){
			printAllowedPlayers(player);
		}
		if ( players ){
			printPresentPlayers(player);
		}
		if ( commands ){
			printDisallowedCommands(player);
		}
	}
	
	public void printAllowedPlayers(Player player){
		if ( this.allowedPlayers.size() == 0 ){
			player.sendMessage(Colors.Yellow + "Allowed players : " + Colors.White + "<list is empty>");
			return;
		}
		String list = "";
		for ( String playerName : this.allowedPlayers){
			list += " " + playerName;
		}
		player.sendMessage(Colors.Yellow + "Allowed players :" + Colors.White + list);
	}
	
	public void printPresentPlayers(Player player){
		if ( this.presentPlayers.size() == 0 ){
			player.sendMessage(Colors.Yellow + "Present players : " + Colors.White + "<list is empty>");
			return;
		}
		String list = "";
		for ( String playerName : this.presentPlayers){
			list += " " + playerName;
		}
		player.sendMessage(Colors.Yellow + "Present players :" + Colors.White + list);
	}
	
	public void printDisallowedCommands(Player player){
		if ( this.disallowedCommands.size() == 0 ){
			player.sendMessage(Colors.Yellow + "Disallowed commands : " + Colors.White + "<list is empty>");
			return;
		}
		String list = "";
		for ( String command : this.disallowedCommands){
			list += " " + command;
		}
		player.sendMessage(Colors.Yellow + "Disallowed commands :" + Colors.White + list);
	}
}