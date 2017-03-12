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
			String name, floorName;
			try{	
				name = id.substring(0, id.lastIndexOf(' '));
				floorName = id.substring(id.lastIndexOf(':') + 1);
			}catch(IllegalArgumentException ex){
				e.getPlayer().sendMessage("Something wrong with this elevator?");
				return;
			};
			
			Elevator el = ElevatorManager.getElevator(name);
			if(el != null){
				Floor floor = el.getFloor(floorName);
				if(floor != null){
					Inventory buttonPanel = Bukkit.createInventory(null, (int) Math.ceil(el.getFloors().size() / 9d) * 9, "Panel - " + sign.getLine(0));
					for(Floor f : el.getFloors()){
						ItemStack is = new ItemStack(Material.PAPER);
						ItemMeta im = is.getItemMeta();
						im.setDisplayName(ChatColor.AQUA + f.getName());
						is.setItemMeta(im);
						buttonPanel.addItem(is);
					}
					
					int currentFloorNumber = floor.getIndex();
					ItemStack button = buttonPanel.getItem(currentFloorNumber);
					ItemStack buttonSelected = button.clone();
					buttonSelected.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
					buttonPanel.setItem(currentFloorNumber, buttonSelected);
					
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
		Floor start = el.getFloor(floorName);
		Floor end = el.getFloorByIndex(e.getSlot());
		
		Player player = (Player) e.getWhoClicked();
		
		if (start == null){
			player.sendMessage("What floor are you on!?");
			e.setCancelled(true);
			return;
		}else if(start.equals(end)){
			player.sendMessage(ChatColor.RED + "You are already on that floor!");
			e.setCancelled(true);
			return;
		}
		el.ride(start, end, player);
	}
	
	@EventHandler
	public void onMenuDrag(InventoryDragEvent e){
		if(e.getInventory().getName().equals("Panel")){
			e.setCancelled(true);
		}
	}
	
}
