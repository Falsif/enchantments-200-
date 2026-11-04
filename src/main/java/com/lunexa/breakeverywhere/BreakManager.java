package com.lunexa.breakeverywhere;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public final class BreakManager {
	private static final List<BreakJob> JOBS = new ArrayList<>();
	public static BreakJob lastFinished;

	private BreakManager() {}

	public static int activeJobs() {
		return JOBS.size();
	}

	public static void stopAll() {
		JOBS.clear();
	}

	public static boolean isBlacklisted(Block block) {
		return BEConfig.get().blacklist.contains(BuiltInRegistries.BLOCK.getKey(block).toString());
	}

	/** Called after a player broke a block. Returns the started job or null. */
	public static BreakJob onPlayerBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
		BEConfig cfg = BEConfig.get();
		if (!cfg.enabled || state.isAir()) return null;
		Block block = state.getBlock();
		if (isBlacklisted(block)) return null;

		String key;
		Component name;
		Predicate<BlockState> match;
		if (cfg.groupAllLeaves && state.is(BlockTags.LEAVES)) {
			key = "#leaves";
			name = Component.translatableWithFallback("breakeverywhere.group.leaves", "all leaves");
			match = s -> s.is(BlockTags.LEAVES) && !isBlacklisted(s.getBlock());
		} else if (cfg.groupAllLogs && state.is(BlockTags.LOGS)) {
			key = "#logs";
			name = Component.translatableWithFallback("breakeverywhere.group.logs", "all logs");
			match = s -> s.is(BlockTags.LOGS) && !isBlacklisted(s.getBlock());
		} else {
			key = BuiltInRegistries.BLOCK.getKey(block).toString();
			name = block.getName();
			match = s -> s.is(block);
		}
		key = level.dimension().location() + "|" + key;
		for (BreakJob j : JOBS) {
			if (j.key.equals(key)) return null; // already breaking this everywhere
		}

		boolean drops = cfg.dropItems && (!player.isCreative() || cfg.dropsInCreative);
		BreakJob job = new BreakJob(level, key, name, match, player, player.getMainHandItem().copy(), drops, pos);
		JOBS.add(job);
		if (cfg.showProgress) {
			player.displayClientMessage(Component.translatableWithFallback("breakeverywhere.start",
					"Breaking %s everywhere (%s loaded chunks)...", name, job.totalChunks()).withStyle(ChatFormatting.GOLD), true);
		}
		return job;
	}

	public static void tick(MinecraftServer server) {
		if (JOBS.isEmpty()) return;
		long deadline = System.nanoTime() + Math.max(1, BEConfig.get().millisPerTick) * 1_000_000L;
		Iterator<BreakJob> it = JOBS.iterator();
		while (it.hasNext()) {
			BreakJob job = it.next();
			boolean done = job.step(deadline);
			if (done) {
				it.remove();
				finish(job);
			} else if (BEConfig.get().showProgress && server.getTickCount() % 5 == 0) {
				job.player.displayClientMessage(Component.translatableWithFallback("breakeverywhere.progress",
						"Breaking %s: %s blocks, chunk %s/%s", job.name, job.broken, job.doneChunks(), job.totalChunks())
						.withStyle(ChatFormatting.YELLOW), true);
			}
			if (System.nanoTime() > deadline) break;
		}
	}

	private static void finish(BreakJob job) {
		lastFinished = job;
		BreakEverywhere.LOGGER.info("[Break Everywhere] {} -> {} blocks in {} chunks", job.key, job.broken, job.totalChunks());
		if (BEConfig.get().showProgress && !job.player.hasDisconnected()) {
			job.player.displayClientMessage(Component.translatableWithFallback("breakeverywhere.done",
					"Done: %s broken everywhere (%s blocks)", job.name, job.broken).withStyle(ChatFormatting.GREEN), true);
		}
	}

	/** For tests: finish everything right now. */
	public static void runAllNow() {
		while (!JOBS.isEmpty()) {
			BreakJob job = JOBS.remove(0);
			job.step(Long.MAX_VALUE);
			lastFinished = job;
		}
	}
}
