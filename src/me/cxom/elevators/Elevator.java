package me.cxom.elevators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	private final List<String> floorList = new ArrayList<>();
	private final Map<String, Floor> floorMap = new HashMap<>();
	
	public static class Floor implements ConfigurationSerializable{
		private String name;
		private final int y;
		
		public Floor(String name, int y){
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

		@Override
		public Map<String, Object> serialize() {
			Map<String, Object> values = new HashMap<>();
			values.put("name", name);
			values.put("y", y);
			return values;
		}
		
		public static Floor deserialize(Map<String, Object> args){
			return new Floor((String) args.get("name"), (int) args.get("y"));
		}

	}
	
	public Floor getFloor(String floorName){
		return floorMap.get(floorName);
	}
	
	public Floor getFloor(int index) {
		return floorMap.get(floorList.get(index));
	}
	
	public int getFloorIndex(String floorName){
		return floorList.indexOf(floorName);
	}
	
	public List<String> getFloorNames(){
		return floorList;
	}
	
	public Elevator(Location elevator, Location shaft, List<Floor> floors){
		this.elevator = elevator;
		this.shaft = shaft;
		this.dx = elevator.getX() - shaft.getX();
		this.dz = elevator.getZ() - shaft.getZ();
		floors.forEach(floor -> {this.floorList.add(floor.getName());
								 this.floorMap.put(floor.getName(), floor);});
	}
	
	public void ride(int startFloorIndex, int endFloorIndex, Player player){
		
		player.addPotionEffect(PotionEffectType.INVISIBILITY.createEffect(50000, 1), false);
		
		Location elevator = player.getLocation();
		elevator.add(dx, 0, dz);
		elevator.setY(this.elevator.getY());
		player.teleport(elevator);
		player.playSound(elevator, Sound.BLOCK_SHULKER_BOX_CLOSE, 1, 1);
		player.setInvulnerable(true);
		TitleAPI.sendTitle(player, 10, 10, 10, "", ChatColor.AQUA + getFloor(startFloorIndex).getName());
		
		new BukkitRunnable(){
			
			int currentFloorIndex = startFloorIndex;
			int direction = endFloorIndex > currentFloorIndex ? 1 : -1;
			
			@Override
			public void run(){
				player.playSound(elevator, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
				currentFloorIndex += direction;
				if (currentFloorIndex == endFloorIndex){
					Location destination = player.getLocation().clone();
					destination.subtract(dx, 0, dz);
					destination.setY(getFloor(endFloorIndex).getY());
					player.setInvulnerable(false);
					player.teleport(destination);
					TitleAPI.sendTitle(player, 15, 15, 15, "", "Arrived: " + ChatColor.AQUA + getFloor(currentFloorIndex).getName());
					player.playSound(destination, Sound.BLOCK_SHULKER_BOX_OPEN, 1, 1);
					player.playSound(destination, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2, 3);
					player.removePotionEffect(PotionEffectType.INVISIBILITY);
					this.cancel();
					return;
				}
				TitleAPI.sendTitle(player, 10, 10, 10, "", ChatColor.RED + getFloor(currentFloorIndex).getName());
			}
		}.runTaskTimer(Elevators.getPlugin(), 40, 30);
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> values = new HashMap<>();
		values.put("elevator", elevator.serialize());
		values.put("shaft", shaft.serialize());
		List<Map<String, Object>> serializedFloors = new ArrayList<>();
		for (String floorName : floorList){
			serializedFloors.add(floorMap.get(floorName).serialize());
		}
		values.put("floors", serializedFloors);
		return values;
	}
	
	@SuppressWarnings("unchecked")
	public static Elevator deserialize(Map<String, Object> args){
		Location elevatorLoc = Location.deserialize((Map<String, Object>) args.get("elevator"));
		Location shaftLoc = Location.deserialize((Map<String, Object>) args.get("shaft"));
		List<Map<String, Object>> serializedFloors = (List<Map<String, Object>>) args.get("floors");
		List<Floor> floors = new ArrayList<>();
		for (Map<String, Object> serializedFloor : serializedFloors){
			floors.add(Floor.deserialize(serializedFloor));
		}
		return new Elevator(elevatorLoc, shaftLoc, floors);
	}
	
}
