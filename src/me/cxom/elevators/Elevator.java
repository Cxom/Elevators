package me.cxom.elevators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.connorlinfoot.titleapi.TitleAPI;

public class Elevator{

	private final Location elevator;
	@SuppressWarnings("unused")
	private final Location shaft;
	private final double dx;
	private final double dz;

	public Inventory buttonPanel;
	
	private final List<Floor> floors;
	private final Map<String, Floor> nameToFloor = new HashMap<>();
	
	public class Floor{
		private String name;
		private final int y;
		
		private Floor(String name, int y){
			this.name = name;
			this.y = y;
		}
		
		public String getName(){
			return name;
		}
		
		public int getY(){
			return y;
		}
		
		@Override
		public boolean equals(Object o){
			if (! (o instanceof Floor)) return false;
			Floor f = (Floor) o;
			return name == f.getName() && y == f.getY();
		}
	}
	
	public Floor getFloor(String name){
		return nameToFloor.get(name);
	}
	
	public List<Floor> getFloors(){
		return floors;
	}
	
	private Elevator(Location elevator, Location shaft, List<Floor> floors){
		this.elevator = elevator;
		this.shaft = shaft;
		this.dx = elevator.getX() - shaft.getX();
		this.dz = elevator.getZ() - shaft.getZ();
		this.floors = floors;
		for(Floor floor : floors){
			this.nameToFloor.put(floor.getName(), floor);
		}
	}
	
	public void ride(Floor start, Floor end, Player player){
		
		player.addPotionEffect(PotionEffectType.INVISIBILITY.createEffect(50000, 1), false);
		
		Location elevator = player.getLocation();
		elevator.add(dx, 0, dz);
		elevator.setY(this.elevator.getY());
		player.teleport(elevator);
		player.playSound(elevator, Sound.BLOCK_SHULKER_BOX_CLOSE, 1, 1);
		player.setInvulnerable(true);
		TitleAPI.sendTitle(player, 5, 20, 15, "", ChatColor.RED + start.getName());
		
		Location destination = elevator.clone();
		destination.subtract(dx, 0, dz);
		destination.setY(end.getY());
		
		new BukkitRunnable(){
			
			int i = floors.indexOf(start);
			int j = floors.indexOf(end);
			int d = j > i ? 1 : -1;
			
			@Override
			public void run(){
				player.playSound(elevator, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
				i += d;
				Floor f = floors.get(i); 
				TitleAPI.sendTitle(player, 10, 10, 10, "", ChatColor.RED + f.getName());
				if (i == j){
					player.setInvulnerable(false);
					player.teleport(destination);
					player.sendMessage(ChatColor.AQUA + "You arrived!");
					player.playSound(destination, Sound.BLOCK_SHULKER_BOX_OPEN, 1, 1);
					player.playSound(destination, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2, 3);
					player.removePotionEffect(PotionEffectType.INVISIBILITY);
					this.cancel();
				}
			}
		}.runTaskTimer(Elevators.getPlugin(), 40, 30);
	}
		
}
