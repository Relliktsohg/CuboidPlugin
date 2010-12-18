import java.util.TimerTask;

public class CuboidHealJob extends TimerTask{
	String playerName;
	CuboidC cuboid;
	
	public CuboidHealJob( String playerName, CuboidC cuboid ){
		this.playerName = playerName;
		this.cuboid = cuboid;
	}
	public void run(){
		Player player = etc.getServer().matchPlayer(this.playerName);
		if ( player!= null && this.cuboid.contains((int)player.getX(), (int)player.getY(), (int)player.getZ()) ){
			if ( player.getHealth() > 0 ){
				player.setHealth(player.getHealth()+CuboidAreas.healPower);	
			}
			if ( player.getHealth() < 20 ){
				CuboidAreas.healTimer.schedule(new CuboidHealJob(this.playerName, this.cuboid), CuboidAreas.healDelay);
			}
		}
		
	}
}