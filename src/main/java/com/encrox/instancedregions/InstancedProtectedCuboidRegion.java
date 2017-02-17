package com.encrox.instancedregions;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.encrox.instancedregions.chunkmap.ChunkData;
import com.encrox.instancedregions.chunkmap.ChunkMapManager;
import com.encrox.instancedregions.types.BlockState;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class InstancedProtectedCuboidRegion extends ProtectedCuboidRegion implements Listener {
	
	private ArrayList<Player> players;
	private ArrayList<DereferencedBlock> blockBreaks, blockPlaces;
	private ArrayList<BlockVector> changeWhitelist;
	private ProtocolManager pmgr;
	private PacketAdapter mapAdapter, spawnEntityAdapter, spawnExperienceOrbAdapter, spawnMobAdapter, spawnPlayerAdapter, blockBreakAdapter, blockChangeAdapter, multiBlockChangeAdapter;
	private World world;

	public InstancedProtectedCuboidRegion(Plugin plugin, final World world, String id, BlockVector pt1, BlockVector pt2) {
		super(id, pt1, pt2);
		Bukkit.getPluginManager().registerEvents(this, plugin);
		players = new ArrayList<Player>();
		blockBreaks = new ArrayList<DereferencedBlock>();
		blockPlaces = new ArrayList<DereferencedBlock>();
		changeWhitelist = new ArrayList<BlockVector>();
		this.world = world;
		pmgr = ProtocolLibrary.getProtocolManager();
		//catch chunk data
		mapAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK){
			@Override
			public void onPacketSending(PacketEvent event) {
				Player player = event.getPlayer();
				if(world.equals(player.getWorld()) && players.contains(event.getPlayer())) {
					PacketContainer packet = event.getPacket();
					StructureModifier<Integer> ints = packet.getIntegers();
					StructureModifier<byte[]> byteArray = packet.getByteArrays();
					StructureModifier<Boolean> bools = packet.getBooleans();
					ChunkData chunkData = new ChunkData();
					chunkData.chunkX = ints.read(0);
					chunkData.chunkZ = ints.read(1);
					chunkData.groundUpContinuous = bools.read(0);
					chunkData.primaryBitMask = ints.read(2);
					chunkData.data = byteArray.read(0);
					chunkData.isOverworld = event.getPlayer().getWorld().getWorldType() == WorldType.NORMAL;
					ChunkMapManager cmm = new ChunkMapManager(chunkData);
					ArrayList<ProtectedRegion> col = new ArrayList<ProtectedRegion>();
					col.add(new ProtectedCuboidRegion("current", new BlockVector(chunkData.chunkX<<4, 0, chunkData.chunkZ<<4), new BlockVector((chunkData.chunkX<<4)|15, 256, (chunkData.chunkZ<<4)|15)));
					try {
						if(getIntersectingRegions(col).isEmpty()) {
							PacketContainer unloadPacket = new PacketContainer(PacketType.Play.Server.UNLOAD_CHUNK);
							unloadPacket.getIntegers().write(0, chunkData.chunkX).write(1, chunkData.chunkZ);
							pmgr.sendServerPacket(player, unloadPacket);
							cmm.init();
							for(int y = 0; y<16; y++) {
								for(int z = 0; z<16; z++) {
									for(int x = 0; x<16; x++) {
										try {
											cmm.readNextBlock();
										} catch(Exception e1) {
											
										}
										//int blockData = cmm.readNextBlock();
										BlockState blockState = new BlockState();
										//ChunkMapManager.blockDataToState(blockData, blockState);
										blockState.id = 0;
										blockState.meta = 0;
										//blockData = ChunkMapManager.blockStateToData(blockState);
										cmm.writeOutputBlock(ChunkMapManager.blockStateToData(blockState));
									}
								}
							}
							cmm.finalizeOutput();
							byteArray.write(0, cmm.createOutput());
						}
					} catch(Exception e) {
						
					}
				}
			}
		};
		//catch entity objects (chests, signs ...)
		spawnEntityAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY){
			@Override
			public void onPacketSending(PacketEvent event) {
				Player player = event.getPlayer();
				if(world.equals(player.getWorld()) && players.contains(event.getPlayer())) {
					PacketContainer packet = event.getPacket();
					StructureModifier<Double> doubles = packet.getDoubles();
					double x = doubles.read(0);
					double y = doubles.read(1);
					double z = doubles.read(2);
					if(!contains(new Vector(x, y, z))) {
						event.setCancelled(true);
					}
				}
			}
		};
		//catch experience orbs
		spawnExperienceOrbAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB){
			@Override
			public void onPacketSending(PacketEvent event) {
				Player player = event.getPlayer();
				if(world.equals(player.getWorld()) && players.contains(event.getPlayer())) {
					PacketContainer packet = event.getPacket();
					StructureModifier<Double> doubles = packet.getDoubles();
					double x = doubles.read(0);
					double y = doubles.read(1);
					double z = doubles.read(2);
					if(!contains(new Vector(x, y, z))) {
						event.setCancelled(true);
					}
				}
			}
		};
		//catch mobs
		spawnMobAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY_LIVING){
			@Override
			public void onPacketSending(PacketEvent event) {
				Player player = event.getPlayer();
				if(world.equals(player.getWorld()) && players.contains(event.getPlayer())) {
					PacketContainer packet = event.getPacket();
					StructureModifier<Double> doubles = packet.getDoubles();
					double x = doubles.read(0);
					double y = doubles.read(1);
					double z = doubles.read(2);
					if(!contains(new Vector(x, y, z))) {
						event.setCancelled(true);
					}
				}
			}
		};
		//catch players
		spawnPlayerAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_ENTITY_SPAWN){
			@Override
			public void onPacketSending(PacketEvent event) {
				Player player = event.getPlayer();
				if(world.equals(player.getWorld()) && players.contains(event.getPlayer())) {
					PacketContainer packet = event.getPacket();
					StructureModifier<UUID> uuids = packet.getUUIDs();
					UUID uuid = uuids.read(0);
					for(int i = 0, size = players.size(); i<size; i++) {
						if(!players.get(i).getUniqueId().equals(uuid)) {
							event.setCancelled(true);
						}
					}
				}
			}
		};
		//catch block break animations
		blockBreakAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_BREAK_ANIMATION){
			@Override
			public void onPacketSending(PacketEvent event) {
				Player player = event.getPlayer();
				if(world.equals(player.getWorld()) && players.contains(event.getPlayer())) {
					PacketContainer packet = event.getPacket();
					long position = packet.getLongs().read(0);
					long x = position >> 38;
					long y = (position >> 26) & 0xFFF;
					long z = position << 38 >> 38;
					InstancedRegions.logger.info("block break: " + x + ", " + y + ", " + z);
				}
			}
		};
		//catch block changes
		blockChangeAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE){
			@Override
			public void onPacketSending(PacketEvent event) {
				Player player = event.getPlayer();
				if(world.equals(player.getWorld()) && players.contains(event.getPlayer())) {
					//InstancedRegions.logger.info("BLOCK_CHANGE");//erst pasten, dann region newen
					PacketContainer packet = event.getPacket();
					StructureModifier<BlockPosition> bps = packet.getBlockPositionModifier();
					BlockPosition bp = bps.read(0);
					//InstancedRegions.logger.info("BLOCK_CHANGE at: " + bp.getX() + ", " + bp.getY() + ", " + bp.getZ());
					BlockVector bv = new BlockVector(bp.getX(), bp.getY(), bp.getZ());
					if(!contains(bv)) {
						if(changeWhitelist.contains(bv)) {
							changeWhitelist.remove(bv);
						} else {
							event.setCancelled(true);
						}
					}
				}
			}
		};
		//catch multi block changes
		multiBlockChangeAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MULTI_BLOCK_CHANGE){
			@Override
			public void onPacketSending(PacketEvent event) {
				Player player = event.getPlayer();
				if(world.equals(player.getWorld()) && players.contains(event.getPlayer())) {
					try {
						//InstancedRegions.logger.info("MULTI_BLOCK_CHANGE");
						PacketContainer packet = event.getPacket();
						StructureModifier<int[]> ints = packet.getIntegerArrays();
						int[] coords = ints.read(0);
						int chunkX = coords[0];
						int chunkZ = coords[1];
						ArrayList<ProtectedRegion> col = new ArrayList<ProtectedRegion>();
						col.add(new ProtectedCuboidRegion("current", new BlockVector(chunkX<<4, 0, chunkZ<<4), new BlockVector((chunkX<<4)|15, 256, (chunkZ<<4)|15)));
						if(getIntersectingRegions(col).isEmpty()) {
							event.setCancelled(true);
						}
					} catch(Exception e) {
						
					}
				}
			}
		};
	}
	
	public void addPlayer(Player player) {
		players.add(player);
	}
	
	public void removePlayer(Player player) {
		players.remove(player);
	}
	
	public void apply() {
		pmgr.addPacketListener(mapAdapter);
		pmgr.addPacketListener(spawnEntityAdapter);
		pmgr.addPacketListener(spawnExperienceOrbAdapter);
		pmgr.addPacketListener(spawnMobAdapter);
		pmgr.addPacketListener(spawnPlayerAdapter);
		pmgr.addPacketListener(blockBreakAdapter);
		pmgr.addPacketListener(blockChangeAdapter);
		pmgr.addPacketListener(multiBlockChangeAdapter);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		DereferencedBlock block = new DereferencedBlock(event.getBlock());
		if(players.contains(event.getPlayer()) && contains(block.location.getBlockX(), block.location.getBlockY(), block.location.getBlockZ())) {
			blockBreaks.add(block);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		DereferencedBlock block = new DereferencedBlock(event.getBlock());
		if(players.contains(event.getPlayer()) && contains(block.location.getBlockX(), block.location.getBlockY(), block.location.getBlockZ())) {
			blockPlaces.add(block);
		}
	}
	
	public void addToChangeWhitelist(BlockVector bv) {
		changeWhitelist.add(bv);
	}
	
	public void dispose() {
		DereferencedBlock current;
		for(int i = 0, size = blockBreaks.size(); i<size; i++) {
			current = blockBreaks.get(i);
			world.getBlockAt(current.location).setType(current.type);
		}
		for(int i = 0, size = blockPlaces.size(); i<size; i++) {
			current = blockPlaces.get(i);
			world.getBlockAt(current.location).setType(Material.AIR);
		}
		pmgr.removePacketListener(mapAdapter);
		pmgr.removePacketListener(spawnEntityAdapter);
		pmgr.removePacketListener(spawnExperienceOrbAdapter);
		pmgr.removePacketListener(spawnMobAdapter);
		pmgr.removePacketListener(spawnPlayerAdapter);
		pmgr.removePacketListener(blockBreakAdapter);
		pmgr.removePacketListener(blockChangeAdapter);
		pmgr.removePacketListener(multiBlockChangeAdapter);
	}

}
