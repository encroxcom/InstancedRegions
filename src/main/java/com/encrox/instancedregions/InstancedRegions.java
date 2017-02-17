package com.encrox.instancedregions;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolManager;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class InstancedRegions extends JavaPlugin {
	
	public static Logger logger;
	public static PluginDescriptionFile pdf;
	public static WorldGuardPlugin wg;
	public static WorldEditPlugin we;
	public static ProtocolManager pmgr;
	
	//test
	public static ArrayList<InstancedProtectedCuboidRegion> region;
	
	public void onEnable() {
		pdf = getDescription();
		logger = Logger.getLogger("Minecraft");
		if(setupMyself()) {
			getCommand("instance").setExecutor(new Commander(this));
			logger.info(pdf.getName() + " " + pdf.getVersion() + " has been enabled.");
		} else {
			logger.info(pdf.getName() + " " + pdf.getVersion() + " has been disabled.");
		}
	}
	
	private boolean setupMyself() {
		region = new ArrayList<InstancedProtectedCuboidRegion>();
		return true;
	}

}
