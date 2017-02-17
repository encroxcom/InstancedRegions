package com.encrox.instancedregions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class DereferencedBlock {
	
	public Material type;
	public Location location;
	public BlockState blockState;
	
	public DereferencedBlock(Block block) {
		type = block.getType();
		location = block.getLocation();
		blockState = block.getState();
	}

}
