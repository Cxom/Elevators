package me.cxom.elevators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
	
	private void hidePlayer(Player player) {
		Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(Elevators.getPlugin(), player));
	}
	
	private void showPlayer(Player player) {
		Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(Elevators.getPlugin(), player));
	}
	
	public void ride(int startFloorIndex, int endFloorIndex, Player player){
		
		hidePlayer(player);
		
		Location elevator = player.getLocation();
		elevator.add(dx, 0, dz);
		elevator.setY(this.elevator.getY());
		player.teleport(elevator);
		player.playSound(elevator, Sound.BLOCK_SHULKER_BOX_CLOSE, 1, 1);
		player.setInvulnerable(true);
		player.sendTitle("", ChatColor.AQUA + getFloor(startFloorIndex).getName(), 10, 10, 10);
		
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
					player.sendTitle("", "Arrived: " + ChatColor.AQUA + getFloor(currentFloorIndex).getName(), 15, 15, 15);
					player.playSound(destination, Sound.BLOCK_SHULKER_BOX_OPEN, 1, 1);
					player.playSound(destination, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2, 3);
					showPlayer(player);
					this.cancel();
					return;
				}
				player.sendTitle("", ChatColor.RED + getFloor(currentFloorIndex).getName(), 10, 10, 10);
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
		
	public void edit(Player player, String name){
		List<Floor> floors = new ArrayList<>();
		for (String floorName : floorList) {
			floors.add(floorMap.get(floorName));
		}
		new ElevatorCreator(player, name, elevator, shaft, floors);
	}
	
}
