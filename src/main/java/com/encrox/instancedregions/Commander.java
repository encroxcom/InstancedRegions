package com.encrox.instancedregions;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.BlockVector;

public class Commander implements CommandExecutor {
	
	private Plugin plugin;
	
	public Commander(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if(arg0 instanceof Player) {
			Player player = (Player)arg0;
			switch(arg3[0]) {
			case "create":
				InstancedProtectedCuboidRegion rg = new InstancedProtectedCuboidRegion(plugin, player.getWorld(), "testInstance", new BlockVector(Integer.parseInt(arg3[1]),0,Integer.parseInt(arg3[2])), new BlockVector(Integer.parseInt(arg3[3]),255,Integer.parseInt(arg3[4])));
				rg.addPlayer(player);
				InstancedRegions.region.add(rg);
				return true;
			case "dispose":
				Iterator<InstancedProtectedCuboidRegion> iter = InstancedRegions.region.iterator();
				while(iter.hasNext())
					iter.next().dispose();
				return true;
			/*case "test":
				InstancedRegions.region.addToChangeWhitelist(new BlockVector(Integer.parseInt(arg3[1]), Integer.parseInt(arg3[2]), Integer.parseInt(arg3[3])));
				player.sendBlockChange(new Location(player.getWorld(), Integer.parseInt(arg3[1]), Integer.parseInt(arg3[2]), Integer.parseInt(arg3[3])), Material.STONE, (byte) 0);
				return true;*/
			}
		}
		return false;
	}

}
