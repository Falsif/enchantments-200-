package com.lunexa.breakeverywhere;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** One "break everywhere" wave: walks loaded chunks from the nearest to the farthest. */
public final class BreakJob {
	final ServerLevel level;
	final String key;
	final Component name;
	final Predicate<BlockState> match;
	final ItemStack tool;
	final ServerPlayer player;
	final boolean drops;
	private final it.unimi.dsi.fastutil.longs.LongArrayList chunks = new it.unimi.dsi.fastutil.longs.LongArrayList();
	private final it.unimi.dsi.fastutil.longs.LongOpenHashSet seen = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
	private int index;
	int broken;
	public int failed;
	private int separateDrops;

	BreakJob(ServerLevel level, String key, Component name, Predicate<BlockState> match, ServerPlayer player,
			 ItemStack tool, boolean drops, BlockPos origin) {
		this.level = level;
		this.key = key;
		this.name = name;
		this.match = match;
		this.player = player;
		this.tool = tool;
		this.drops = drops;
		int ox = origin.getX() >> 4;
		int oz = origin.getZ() >> 4;
		long[] sorted = Arrays.stream(Arrays.stream(LoadedChunks.snapshot(level)).boxed().toArray(Long[]::new))
				.sorted(Comparator.comparingLong(c -> {
					long dx = ChunkPos.getX(c) - ox;
					long dz = ChunkPos.getZ(c) - oz;
					return dx * dx + dz * dz;
				}))
				.mapToLong(Long::longValue).toArray();
		for (long c : sorted) addChunk(c);
	}

	/** Chunks that load while the wave is running are added to it. */
	void addChunk(long c) {
		if (seen.add(c)) chunks.add(c);
	}

	public int totalChunks() {
		return chunks.size();
	}

	public int doneChunks() {
		return index;
	}

	public boolean finished() {
		return index >= chunks.size();
	}

	/** Works until the deadline (System.nanoTime) or until done. Returns true when finished. */
	boolean step(long deadline) {
		while (index < chunks.size()) {
			long c = chunks.getLong(index++);
			processChunk(ChunkPos.getX(c), ChunkPos.getZ(c));
			if (System.nanoTime() > deadline) break;
		}
		return finished();
	}

	private void processChunk(int cx, int cz) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
		if (chunk == null) return;
		LevelChunkSection[] sections = chunk.getSections();
		List<BlockPos> found = new ArrayList<>();
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			if (section == null || section.hasOnlyAir() || !section.maybeHas(match)) continue;
			int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						if (match.test(section.getBlockState(x, y, z))) {
							found.add(new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z));
						}
					}
				}
			}
		}
		if (found.isEmpty()) return;

		int maxSeparate = Math.max(0, BEConfig.get().maxSeparateDrops);
		int flags = BEConfig.get().updateNeighbors ? Block.UPDATE_ALL : (Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		// Next to a chunk that is not loaded, updating neighbors would load (and generate) that chunk,
		// which then joins the wave and loads the next one... an endless chain. So no neighbor updates there.
		int edgeFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
		boolean west = level.getChunkSource().getChunkNow(cx - 1, cz) != null;
		boolean east = level.getChunkSource().getChunkNow(cx + 1, cz) != null;
		boolean north = level.getChunkSource().getChunkNow(cx, cz - 1) != null;
		boolean south = level.getChunkSource().getChunkNow(cx, cz + 1) != null;
		boolean allNeighbors = west && east && north && south;
		List<ItemStack> merged = new ArrayList<>();
		BlockPos mergedPos = null;

		for (BlockPos pos : found) {
			BlockState state = level.getBlockState(pos);
			if (!match.test(state)) continue; // already removed by a neighbor update
			if (drops) {
				BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
				List<ItemStack> list = Block.getDrops(state, level, pos, be, player, tool);
				if (separateDrops < maxSeparate) {
					for (ItemStack s : list) Block.popResource(level, pos, s);
					separateDrops++;
				} else {
					if (mergedPos == null) mergedPos = pos;
					for (ItemStack s : list) addMerged(merged, s);
				}
				state.spawnAfterBreak(level, pos, tool, true);
			}
			int f = flags;
			if (!allNeighbors) {
				int lx = pos.getX() & 15;
				int lz = pos.getZ() & 15;
				if ((lx == 0 && !west) || (lx == 15 && !east) || (lz == 0 && !north) || (lz == 15 && !south)) f = edgeFlags;
			}
			if (level.setBlock(pos, state.getFluidState().createLegacyBlock(), f)) {
				broken++;
			} else {
				failed++;
			}
		}
		if (mergedPos != null) {
			for (ItemStack s : merged) Block.popResource(level, mergedPos, s);
		}
	}

	private static void addMerged(List<ItemStack> merged, ItemStack stack) {
		if (stack.isEmpty()) return;
		for (ItemStack m : merged) {
			if (ItemStack.isSameItemSameComponents(m, stack) && m.getCount() < m.getMaxStackSize()) {
				int move = Math.min(stack.getCount(), m.getMaxStackSize() - m.getCount());
				m.grow(move);
				stack.shrink(move);
				if (stack.isEmpty()) return;
			}
		}
		merged.add(stack.copy());
	}
}
