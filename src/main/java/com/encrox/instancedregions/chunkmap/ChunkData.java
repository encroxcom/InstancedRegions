/**
 * @author Aleksey Terzi
 *
 */

package com.encrox.instancedregions.chunkmap;

public class ChunkData {
	public int chunkX;
	public int chunkZ;
	public boolean groundUpContinuous;
	public int primaryBitMask;
	public byte[] data;
	public boolean isOverworld;
	public boolean useCache;
}
