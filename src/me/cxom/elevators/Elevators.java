package me.cxom.elevators;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import me.cxom.elevators.Elevator.Floor;
import me.cxom.elevators.configuration.ElevatorManager;

public class Elevators extends JavaPlugin implements Listener{

	private static Plugin plugin;

	@Override
	public void onEnable(){
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		Elevators.plugin = this;
		ConfigurationSerialization.registerClass(Elevator.Floor.class);
		ConfigurationSerialization.registerClass(Elevator.class);
		ElevatorManager.loadElevators();
	}

	@Override
	public void onDisable(){
		ElevatorManager.saveAll();
		ElevatorManager.clear();
		plugin = null;
	}
	
	public static Plugin getPlugin(){
		return plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if (! (sender instanceof Player)) return true;
		Player player = (Player) sender;
		if (args.length < 1) return false;
		if (args[0].equalsIgnoreCase("create")){
			new ElevatorCreator(player);
		} else if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("delete")){
			if (args.length < 2){
				player.sendMessage(ChatColor.RED + "You need to specify the name of the elevator. Try running '/elevator <edit|delete> <elevator-name>'.");
				return true;
			}
			Elevator elevator = ElevatorManager.getElevator(args[1]);
			if (elevator == null){
				player.sendMessage(ChatColor.RED + "You didn't specify a valid elevator name. Try referencing the sign if you can and don't "
						+ "remember the name. It " + ChatColor.ITALIC + "is" + ChatColor.RESET + ChatColor.RED + " case sensitive.");
				return true;
			}
			if (args[0].equalsIgnoreCase("edit")){
				if (ElevatorCreator.isEditing(player)){
					player.sendMessage(ChatColor.RED + "You can only edit one elevator at a time! Please 'finish' or 'quit' editing the one you are working on first.");
				} else if (ElevatorCreator.isBeingEdited(args[1])) {
					player.sendMessage(ChatColor.YELLOW + "Someone else is already editing this elevator. Only one person can edit an elevator at a time.");
				}
				elevator.edit(player, args[1]);
			} else {
				//elevator delete
				if (args.length < 3){
					player.sendMessage(ChatColor.YELLOW + "Please type the name of the elevator twice when running the command to confirm that you want to delete.");
					return true;
				} else if (!args[1].equals(args[2])) {
					player.sendMessage(ChatColor.RED + "The first elevator name you typed in does not match the second. The elevator has not been deleted.");
				}
				ElevatorManager.deleteElevator(args[1]);
				player.sendMessage(ChatColor.GOLD + "The elevator '" + args[1] + "' has been deleted.");
			}
		} else {
			return false;
		}
		return true;
	}
	
	@EventHandler
	public void onElevatorEnter(PlayerInteractEvent e){
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK
			&& e.getClickedBlock().getType() == Material.STONE_BUTTON
			&& e.getClickedBlock().getRelative(BlockFace.UP).getType() == Material.WALL_SIGN){
			
			Sign sign = (Sign) e.getClickedBlock().getRelative(BlockFace.UP).getState();
			String id = sign.getLine(0);
			String elevatorName, floorName;
			
			elevatorName = id.substring(0, id.lastIndexOf(' '));
			Elevator el = ElevatorManager.getElevator(elevatorName);
			if(el != null){
				
				try{	
					floorName = id.substring(id.lastIndexOf(':') + 1);
				}catch(IllegalArgumentException ex){
					e.getPlayer().sendMessage("Something wrong with this elevator?");
					return;
				};
				
				Floor floor = el.getFloor(floorName);
				if(floor != null){
					Inventory buttonPanel = Bukkit.createInventory(null, (int) Math.ceil(el.getFloorNames().size() / 9d) * 9, "Panel - " + sign.getLine(0));
					for(String fn : el.getFloorNames()){
						ItemStack is = new ItemStack(Material.PAPER);
						ItemMeta im = is.getItemMeta();
						im.setDisplayName(ChatColor.AQUA + fn);
						is.setItemMeta(im);
						buttonPanel.addItem(is);
					}
					
					int currentFloorIndex = el.getFloorIndex(floorName);
					ItemStack button = buttonPanel.getItem(currentFloorIndex);
					ItemStack buttonSelected = button.clone();
					buttonSelected.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
					buttonPanel.setItem(currentFloorIndex, buttonSelected);
					
					e.getPlayer().openInventory(buttonPanel);
				}
			}
		}
	}
	
	@EventHandler
	public void onMenuClick(InventoryClickEvent e){
		if (e.getClickedInventory() == null) return;
		if (! e.getInventory().getName().startsWith("Panel")) return;
		if (! (e.getCurrentItem() != null
			   && e.getCurrentItem().hasItemMeta())) return;
		
		String id = e.getInventory().getName().substring(e.getInventory().getName().indexOf('-') + 2);
		String elevatorName = id.substring(0, id.lastIndexOf(' '));
		String floorName = id.substring(id.indexOf(':') + 1);
		
		Elevator el = ElevatorManager.getElevator(elevatorName);
		int startIndex = el.getFloorIndex(floorName);
		int endIndex = e.getSlot();
		
		Player player = (Player) e.getWhoClicked();
		
		if (startIndex <  0 || startIndex >= el.getFloorNames().size()){
			player.sendMessage("What floor are you on!?");
			e.setCancelled(true);
			return;
		}else if(startIndex == endIndex){
			player.sendMessage(ChatColor.RED + "You are already on that floor!");
			e.setCancelled(true);
			return;
		}
		el.ride(startIndex, endIndex, player);
	}
	
	@EventHandler
	public void onMenuDrag(InventoryDragEvent e){
		if(e.getInventory().getName().equals("Panel")){
			e.setCancelled(true);
		}
	}
	
}
