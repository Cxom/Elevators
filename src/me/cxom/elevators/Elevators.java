package me.cxom.elevators;

import java.util.HashMap;
import java.util.Map;

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

public class Elevators extends JavaPlugin implements Listener{

	private static Plugin plugin;
	private static Map<String, Elevator> elevators = new HashMap<>();

	@Override
	public void onEnable(){
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		Elevators.plugin = this;
		ConfigurationSerialization.registerClass(Elevator.Floor.class);
		ConfigurationSerialization.registerClass(Elevator.class);
	}

	public static Plugin getPlugin(){
		return plugin;
	}
	
	public static Elevator getElevator(String name){
		return elevators.get(name);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
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
			
			Elevator el = Elevators.getElevator(name);
			if(el != null){
				Floor floor = el.getFloor(floorName);
				if(floor != null){
					Inventory buttonPanel = Bukkit.createInventory(null, (int) Math.ceil(el.getFloors().size() / 9d) * 9, "Button Panel - " + sign.getLine(0));
					for(Floor f : el.getFloors()){
						ItemStack is = new ItemStack(Material.PAPER);
						ItemMeta im = is.getItemMeta();
						im.setDisplayName(ChatColor.AQUA + f.getName());
						is.setItemMeta(im);
						buttonPanel.addItem(is);
					}
					
					int i = el.getFloors().indexOf(floor);
					ItemStack button = buttonPanel.getItem(i);
					ItemStack buttonSelected = button.clone();
					buttonSelected.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
					buttonPanel.setItem(i, buttonSelected);
					
					e.getPlayer().openInventory(buttonPanel);
				}
			}
		}
	}
	
	@EventHandler
	public void onMenuClick(InventoryClickEvent e){
		if (e.getClickedInventory() == null) return;
		if (! e.getInventory().getName().startsWith("Button Panel")) return;
		if (! (e.getCurrentItem() != null
			   && e.getCurrentItem().hasItemMeta()
			   && e.getCurrentItem().getItemMeta().hasLore())) return;
		
		String id = e.getInventory().getName().substring(e.getInventory().getName().indexOf('-') + 2);
		String elevatorName = id.substring(0, id.lastIndexOf(' '));
		String floorName = id.substring(id.indexOf(':') + 1);
		
		Elevator el = Elevators.getElevator(elevatorName);
		Floor start = el.getFloor(floorName);
		Floor end = el.getFloors().get(e.getSlot());
		
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
		if(e.getInventory().getName().equals("Button Panel")){
			e.setCancelled(true);
		}
	}
	
}
