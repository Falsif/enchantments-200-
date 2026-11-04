package com.lunexa.breakeverywhere;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Keeps the list of loaded chunks of each dimension. */
public final class LoadedChunks {
	private static final Map<ResourceKey<Level>, LongOpenHashSet> LOADED = new HashMap<>();

	private LoadedChunks() {}

	public static void register() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) ->
				LOADED.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet()).add(chunk.getPos().toLong()));
		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			LongOpenHashSet set = LOADED.get(level.dimension());
			if (set != null) set.remove(chunk.getPos().toLong());
		});
		ServerWorldEvents.UNLOAD.register((server, level) -> LOADED.remove(level.dimension()));
	}

	public static long[] snapshot(ServerLevel level) {
		LongOpenHashSet set = LOADED.get(level.dimension());
		return set == null ? new long[0] : set.toLongArray();
	}

	public static int count(ServerLevel level) {
		LongOpenHashSet set = LOADED.get(level.dimension());
		return set == null ? 0 : set.size();
	}
}
