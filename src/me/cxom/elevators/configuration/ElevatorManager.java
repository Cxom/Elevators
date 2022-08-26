package me.cxom.elevators.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.cxom.elevators.Elevator;
import me.cxom.elevators.Elevators;

public class ElevatorManager {

	private static File elevatorsFolder;
	
	private static Map<String, Elevator> elevators = new HashMap<>();
	
	public static Elevator getElevator(String name){
		return elevators.get(name);
	}
	
	public static void loadElevators(){
		
		elevatorsFolder = new File(Elevators.getPlugin().getDataFolder()
				+ File.separator + "Elevators");
		
		
		if(!elevatorsFolder.exists()){
			elevatorsFolder.mkdirs();
		}
		
		if(elevatorsFolder.listFiles() != null){
			for (File elevatorf : Arrays.asList(elevatorsFolder.listFiles())) {
				FileConfiguration elevatorfc = new YamlConfiguration();
				try {
					elevatorfc.load(elevatorf);
					Elevator elevator = (Elevator) elevatorfc.get("data");
					if (elevator != null){
						elevators.put(elevatorf.getName().substring(0, elevatorf.getName().length() - 4), elevator);
					}
				} catch (IOException | InvalidConfigurationException e) {
					Bukkit.getLogger().warning("Could not load " + elevatorf.getName() + "!");
					e.printStackTrace();
				}
			}
		}
	}
	
	public static boolean saveElevator(String name){
		Elevator elevator = elevators.get(name);
		if (elevator == null) return false;
		return saveElevator(name, elevator);
	}
	
	public static boolean saveElevator(String name, Elevator elevator){
		File elevatorf = new File(elevatorsFolder, name + ".yml");
		try {
			if (!elevatorf.exists()){
				elevatorf.createNewFile();
			}
		} catch (IOException ex){
			Bukkit.getLogger().warning("Could not create file for " + elevatorf.getName() + ".");
			return false;
		}
		FileConfiguration elevatorfc = YamlConfiguration.loadConfiguration(elevatorf);
		elevatorfc.set("data", elevator);
		try {
			elevatorfc.save(elevatorf);
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static void saveAll(){
		elevators.entrySet().stream().forEach(elevator -> {
			if (!saveElevator(elevator.getKey(), elevator.getValue())){
				Bukkit.getLogger().warning("Elevator " + elevator.getKey() + " failed to save!");
			}
		});
	}

	public static void clear() {
		elevators.clear();
	}

	public static void addElevator(String name, Elevator elevator) {
		elevators.put(name, elevator);
		saveElevator(name, elevator);
	}
	
	public static void deleteElevator(String name){
		elevators.remove(name);
		File elevatorf = new File(elevatorsFolder, name + ".yml");
		elevatorf.delete();
	}

	public static List<String> getElevatorNameList() {
		return List.copyOf(elevators.keySet());
	}
	
}
