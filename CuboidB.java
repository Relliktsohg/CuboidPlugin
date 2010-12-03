import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * This is what contains the data about cuboid areas.
 */

@SuppressWarnings("serial")
public class CuboidB implements Serializable{	
	String name= "noname";
	int[] coords = new int[6];	//	int[]{firstX, firstY, firstZ, secondX, secondY, secondZ}	
	boolean protection = false;
	boolean restricted = false;
	boolean inventories = false;
	boolean PvP = true;
	boolean heal = false;
	boolean creeper = true;
	boolean sanctuary = false;
	ArrayList<String> allowedPlayers = new ArrayList<String>();
	String welcomeMessage = null;
	String farewellMessage = null;
	String warning = null;
	ArrayList<String> presentPlayers = new ArrayList<String>();
	ArrayList<String> disallowedCommands = new ArrayList<String>();
	HashMap<String, CuboidInventory> playerInventories = new HashMap<String, CuboidInventory>();
	
	public CuboidB(){}
	
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
		if ( !presentPlayers.contains(player.getName()) ){
			this.presentPlayers.add(player.getName());
			if ( this.welcomeMessage != null )
				player.sendMessage(Colors.Yellow + this.welcomeMessage);
		}
		// I had to separate the inventory-switching from the rest, to enable nested cuboids
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
			
			cuboidInventory.outside = new ArrayList<CuboidItem>();
			for (int i=0; i<outsideInventory.getArray().length; i++){
				Item item = outsideInventory.getItemFromSlot(i);
				if (item != null)
					cuboidInventory.outside.add(new CuboidItem(item));;
			}
			outsideInventory.clearContents();
			outsideInventory.updateInventory();
			playerInventories.put(player.getName(), cuboidInventory);
			
			if (!newVisitor){
				for (CuboidItem item : cuboidInventory.inside){
					player.giveItem( new Item(item.itemId, item.amount, item.slot) );
				}
			}
		}
	}
	
	public void playerLeaves( Player player ){
		int X = (int)player.getX();
		int Y = (int)player.getY();
		int Z = (int)player.getZ();
		if ( X<coords[0] || X>coords[3] || Y<coords[1] || Y>coords[4] || Z<coords[2] || Z>coords[5] ){
			this.presentPlayers.remove(player.getName());
			if ( this.farewellMessage != null )
				player.sendMessage(Colors.Yellow + this.farewellMessage);
		}
		if ( this.inventories ){
			CuboidInventory cuboidInventory = playerInventories.get(player.getName());
			Inventory insideInventory = player.getInventory();
			
			cuboidInventory.inside = new ArrayList<CuboidItem>();
			for (int i=0; i<insideInventory.getArray().length; i++){
				Item item = insideInventory.getItemFromSlot(i);
				if (item != null)
					cuboidInventory.inside.add(new CuboidItem(item));
			}
			insideInventory.clearContents();
			insideInventory.updateInventory();
			playerInventories.put(player.getName(), cuboidInventory);
			
			for (CuboidItem item : cuboidInventory.outside){
				player.giveItem(new Item(item.itemId, item.amount, item.slot));
			}
		}
	}

	public void printInfos(Player player, boolean players, boolean commands ){
		player.sendMessage(Colors.Yellow + "----    " + this.name + "    ----");
		String flags = "";
		boolean noflag = true;
		if ( this.protection ){
			flags += " protection";
			noflag = false;
		}
		if ( this.restricted ){
			flags += " restricted";
			noflag = false;
		}
		if ( this.inventories ){
			flags += " inventory";
			noflag = false;
		}
		if ( !this.PvP ){
			flags += " no-PvP";
			noflag = false;
		}
		if ( this.heal ){
			flags += " heal";
			noflag = false;
		}
		if ( !this.creeper ){
			flags += " creeper-free";
			noflag = false;
		}
		if ( this.sanctuary ){
			flags += " sanctuary";
			noflag = false;
		}
		if ( this.welcomeMessage!=null ){
			flags += " welcome";
			noflag = false;
		}
		if ( this.farewellMessage!=null ){
			flags += " farewell";
			noflag = false;
		}
		if ( noflag ){
			flags += " <none>";
		}
		
		player.sendMessage(Colors.Yellow + "Flags :" + Colors.White + flags);
		printAllowedPlayers(player);
		
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