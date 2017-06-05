package me.cxom.elevators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.cxom.elevators.Elevator.Floor;
import me.cxom.elevators.configuration.ElevatorManager;

public class ElevatorCreator implements Listener{

	private static Set<UUID> editors = new HashSet<>();
	
	public static boolean isEditing(Player player){
		return editors.contains(player.getUniqueId());
	}
	
	private static Set<String> elevatorsBeingEdited = new HashSet<>();
	
	public static boolean isBeingEdited(String elevatorName){
		return elevatorsBeingEdited.contains(elevatorName);
	}
	
	private UUID creator;
	
	private String name;
	private String oldName;
	
	private Location elevatorLoc;
	private Location shaftLoc;
	private List<Floor> floors = new ArrayList<>();
	
	private boolean editing = false;
	private boolean changes = false;
	private boolean quit = false;
	
	public ElevatorCreator(Player player){
		this.creator = player.getUniqueId();
		editors.add(this.creator);
		displayHelp(player);
		Bukkit.getServer().getPluginManager().registerEvents(this, Elevators.getPlugin());
	}
	
	public ElevatorCreator(Player creator, String name, Location elevatorLoc, Location shaftLoc, List<Floor> floors){
		this(creator);
		this.name = name;
		this.oldName = name;
		this.elevatorLoc = elevatorLoc;
		this.shaftLoc = shaftLoc;
		this.floors = floors;
		this.editing = true;
		elevatorsBeingEdited.add(oldName);
	}
	
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e){
		if (e.getPlayer().getUniqueId().equals(creator)){
			Player player = e.getPlayer();
			String[] args = e.getMessage().split(" ");
			switch (args[0].toLowerCase()){
			case "name":
				if (args.length < 2){
					player.sendMessage(ChatColor.RED + "You need to specify a value for the elevator name. Try typing 'name <name>'.");
				} else {
					name = args[1];
					if (ElevatorManager.getElevator(args[1]) != null){
						player.sendMessage(ChatColor.RED + "There is already an elevator with this name. Please choose something else or change that one.");
						break;
					}
					player.sendMessage(ChatColor.GREEN + "Set the elevator name to '" + name + "'.");
					changes = true;
				}
				break;
			case "elevator":
				elevatorLoc = e.getPlayer().getLocation().getBlock().getLocation();
				player.sendMessage(ChatColor.GREEN + "Elevator location updated to " + formatLocation(elevatorLoc) + ".");
				changes = true;
				break;
			case "shaft":
				shaftLoc = e.getPlayer().getLocation().getBlock().getLocation();
				player.sendMessage(ChatColor.GREEN + "Shaft location updated to " + formatLocation(shaftLoc) + ".");
				changes = true;
				break;
			case "floor":
				if (args.length < 2){
					player.sendMessage(ChatColor.RED + "You need to specify a value for the floor name. Try typing 'floor <floor-name>'.");
					break;
				}
				String floorName = args[1];
				int yValue = player.getLocation().getBlockY();
				int floorIndex = floors.size();
				if (args.length >= 3){
					try {
						floorIndex = Integer.parseInt(args[2]) - 1;
					} catch (NumberFormatException ex){
						player.sendMessage(ChatColor.RED + "Expected a number specifying where to put the floor. Try typing 'floor <floor-name> [floor-#]'.");
						break;
					}
				}
				if (floorIndex < 0 || floorIndex > floors.size()){
					player.sendMessage(ChatColor.RED + "The floor number must be between 1 and the number of floors + 1 (currently " + floors.size() + " + 1 = " + (floors.size() + 1) + ") inclusive.");
					break;
				}
				floors.add(floorIndex, new Floor(floorName, yValue));
				player.sendMessage(ChatColor.GREEN + "Added floor with name '" + floorName + "' at y-value " + yValue + ".");
				changes = true;
				break;
			case "delfloor":
				if (args.length < 2){
					player.sendMessage(ChatColor.RED + "You need to specify a value for the floor number to delete. Try typing 'delfloor <floor-#>'.");
					break;
				}
				int floorIndex2 = -1;
				try {
					floorIndex2 = Integer.parseInt(args[1]) - 1; // reset count to be from 0
				} catch (NumberFormatException ex){
					player.sendMessage(ChatColor.RED + "Expected a number specifying which floor to delete. Try typing 'delfloor <floor-#>'.");
					break;
				}
				if (floorIndex2 < 0 || floors.size() < floorIndex2){
					player.sendMessage(ChatColor.RED + "The floor number to delete must be between 1 and the number of floors (currently " + floors.size() + ") inclusive.");
					break;
				}
				Floor removed = floors.remove(floorIndex2);
				player.sendMessage(ChatColor.YELLOW + "Deleted floor #" + (floorIndex2 + 1) + " with name '" + removed.getName() + "' at y-value " + removed.getY() + ".");
				changes = true;
				break;
			case "help":
				displayHelp(player);
				break;
			case "status":
				player.sendMessage(ChatColor.GREEN + "-----Elevator Construction Status-----");
				player.sendMessage(ChatColor.GREEN + "Name: " + (name == null ? ChatColor.RED + "Not set" : ChatColor.GREEN + name));
				player.sendMessage(ChatColor.GREEN + "Elevator Location: " + (elevatorLoc == null ? ChatColor.RED + "Not set" : ChatColor.GREEN + formatLocation(elevatorLoc)));
				player.sendMessage(ChatColor.GREEN + "Shaft Location: " + (shaftLoc == null ? ChatColor.RED + "Not set" : ChatColor.GREEN + formatLocation(shaftLoc)));
				player.sendMessage(ChatColor.GREEN + "Floors: " + (floors.isEmpty() ? ChatColor.RED + "None defined!" : ""));
				int i = 1;
				for (Floor floor : floors){
					player.sendMessage(ChatColor.GREEN + " Floor #" + i++ + ": " + floor.getName() + " - Y-value at " + floor.getY());
				}
				player.sendMessage(ChatColor.GREEN + "This elevator is " + (!isValid() ? ChatColor.RED + "not ready" + ChatColor.GREEN : "ready") + " for 'finish'-ing.");
				player.sendMessage(ChatColor.GREEN + "--------------------------------------");
				break;
			case "finish":
				if (!isValid()){
					player.sendMessage(ChatColor.RED + "The elevator is not ready! Type 'status' to see what you are missing.");
					break;
				} else {
					finish();
					if (editing) {
						player.sendMessage(ChatColor.GREEN + "The elevator was editing successfully! Exiting Elevator Creation.");
					} else {
						player.sendMessage(ChatColor.GREEN + "The elevator was added successfully! Exiting Elevator Creation.");
					}
					quit();
					player.sendMessage(ChatColor.GOLD + "Exited Elevator Creation");
				}
				break;
			case "quit":
				if (quit || (editing && !changes)){
					quit();
					player.sendMessage(ChatColor.GOLD + "Exited Elevator Creation");
				} else {
					if (isValid()){
						if (editing){
							player.sendMessage(ChatColor.GREEN + "We seem to think you've made changes, are you sure you want to leave? (Type quit again to leave.)");
						} else {
							player.sendMessage(ChatColor.GREEN + "The elevator is valid for 'finish'-ing, are you sure you want to leave? (Type quit again to leave.)");
						}
					} else {
						player.sendMessage(ChatColor.YELLOW + "Are you sure you want to leave? Type 'quit' again to leave.");
					}
					quit = true;
				}
				break;
			default:
				return; //skips e.setCancelled(true), sends the chat message
			}
			e.setCancelled(true);
		}
	}
	
	private static String formatLocation(Location loc){
		return String.format("x:%.0f y:%.0f z:%.0f", loc.getX(), loc.getY(), loc.getZ());
	}
	
	private boolean isValid(){
		return name != null && elevatorLoc != null && shaftLoc != null && !floors.isEmpty();
	}
	
	private void displayHelp(Player player){
		player.sendMessage(ChatColor.GREEN + "------Elevator Construction Help------");
		player.sendMessage(ChatColor.DARK_GREEN + "In elevator creation mode, you have 9 subcommands that you can use to create an elevator."
				+ " Use them by simply typing them in chat. They are as follows: ");
		player.sendMessage(ChatColor.GREEN + "'name <name>' sets the name of the elevator to <name>.");
		player.sendMessage(ChatColor.GREEN + "'elevator' sets the location of the hidden elevator room to the block you're standing on.");
		player.sendMessage(ChatColor.GREEN + "'shaft' sets the location of the elevator shaft to the vertical column you're standing in (the y-value you set it at doesn't matter).");
		player.sendMessage(ChatColor.GREEN + "'floor <floor-name> [floor-#]' adds a floor with name <floor-name> at the block y-value you're standing at. "
				+ ChatColor.DARK_GREEN + "If you specify a [floor-#], it will insert the floor correspondingly into the list of floors, bumping all floors above it up by one.");
		player.sendMessage(ChatColor.GREEN + "'delfloor <floor-#>' deletes the floor with the given position. "
				+ ChatColor.DARK_GREEN + "Starts counting from 1 from the bottom floor (if you have a basement, be careful!).");
		player.sendMessage(ChatColor.GREEN + "'help' displays this help page.");
		player.sendMessage(ChatColor.GREEN + "'status' displays the creation status of all elements of the elevator.");
		player.sendMessage(ChatColor.GREEN + "'finish' attempts to end the creation process and add the elevator to the server.");
		player.sendMessage(ChatColor.GREEN + "'quit' will quit elevator creation mode without attempting to 'finish'. It will prompt for confirmation.");
		player.sendMessage(ChatColor.GREEN + "Anything else you type will be sent as a normal chat message.");
		player.sendMessage(ChatColor.DARK_GREEN + "Note: Try to keep elevator and floor names short (8-9 characters for elevators, 1-2 for floors, preferably). "
				+ "They need to be able to fit on a sign to work.");
		player.sendMessage(ChatColor.GREEN + "--------------------------------------");
	}
	
	private void finish(){
		if (editing && !name.equals(oldName)){
			ElevatorManager.deleteElevator(oldName);
		}
		ElevatorManager.addElevator(name, new Elevator(elevatorLoc, shaftLoc, floors));
	}
	
	private void quit(){
		editors.remove(creator);
		elevatorsBeingEdited.remove(oldName);
		creator = null;
		elevatorLoc = null;
		shaftLoc = null;
		floors.clear();
	}
	
}
