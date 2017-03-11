package me.cxom.elevators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.connorlinfoot.titleapi.TitleAPI;

public class Elevator implements ConfigurationSerializable{

	private final Location elevator;
	private final Location shaft;
	private final double dx;
	private final double dz;
	
	private final List<String> floorNameMap = new ArrayList<>();
	private final Map<String, Floor> floorMap = new HashMap<>();
	
	public static class Floor implements ConfigurationSerializable{
		private String name;
		private final int y;
		private final int number;
		
		public Floor(String name, int y, int number){
			this.name = name;
			this.y = y;
			this.number = number;
		}
		
		public String getName(){
			return name;
		}
		
		public int getY(){
			return y;
		}
		
		public int getNumber(){
			return number;
		}
		
		@Override
		public boolean equals(Object o){
			if (! (o instanceof Floor)) return false;
			Floor f = (Floor) o;
			return name == f.getName() && y == f.getY();
		}

		@Override
		public Map<String, Object> serialize() {
			Map<String, Object> values = new HashMap<>();
			values.put("name", name);
			values.put("y", y);
			values.put("number", number);
			return values;
		}
		
		public static Floor deserialize(Map<String, Object> args){
			return new Floor((String) args.get("name"), (int) args.get("y"), (int) args.get("number"));
		}
	}
	
	public Floor getFloor(String name){
		return floorMap.get(name);
	}
	
	public Floor getFloor(int index){
		return floorMap.get(floorNameMap.get(index));
	}
	
	public Collection<Floor> getFloors(){
		return floorMap.values();
	}
	
	public Elevator(Location elevator, Location shaft, List<Floor> floors){
		this.elevator = elevator;
		this.shaft = shaft;
		this.dx = elevator.getX() - shaft.getX();
		this.dz = elevator.getZ() - shaft.getZ();
		floors.forEach(floor -> {this.floorNameMap.add(floor.getName());
								 this.floorMap.put(floor.getName(), floor);});
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
		
		new BukkitRunnable(){
			
			int i = start.getNumber();
			int j = end.getNumber();
			int d = j > i ? 1 : -1;
			
			@Override
			public void run(){
				player.playSound(elevator, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
				i += d;
				Floor f = getFloor(i); 
				TitleAPI.sendTitle(player, 10, 10, 10, "", ChatColor.RED + f.getName());
				if (i == j){
					Location destination = player.getLocation().clone();
					destination.subtract(dx, 0, dz);
					destination.setY(end.getY());
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

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> values = new HashMap<>();
		values.put("elevator", elevator.serialize());
		values.put("shaft", shaft.serialize());
		List<Map<String, Object>> floorValues = floorMap.values().stream().map(Floor::serialize).collect(Collectors.toList());
		values.put("floors", floorValues);
		return values;
	}
	
	@SuppressWarnings("unchecked")
	public static Elevator deserialize(Map<String, Object> args){
		Location elevatorLoc = Location.deserialize((Map<String, Object>) args.get("elevator"));
		Location shaftLoc = Location.deserialize((Map<String, Object>) args.get("shaft"));
		List<Map<String, Object>> floorValues = (List<Map<String, Object>>) args.get("floors");
		List<Floor> floors = new ArrayList<>();
		for (Map<String, Object> floorValue : floorValues){
			floors.add(Floor.deserialize(floorValue));
		}
		return new Elevator(elevatorLoc, shaftLoc, floors);
	}
		
}
