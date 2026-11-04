package com.lunexa.breakeverywhere;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Runs on a real dedicated server with -Dbe.selftest=true, writes selftest-report.txt and stops the server. */
public final class SelfTest {
	private final List<String> lines = new ArrayList<>();
	private int pass;
	private int fail;

	private void check(String name, boolean ok, Object detail) {
		if (ok) pass++; else fail++;
		lines.add((ok ? "[PASS] " : "[FAIL] ") + name + " -> " + detail);
	}

	public static void run(MinecraftServer server) {
		SelfTest t = new SelfTest();
		try {
			t.runAll(server);
		} catch (Throwable e) {
			t.fail++;
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			t.lines.add("[FAIL] EXCEPTION " + sw);
		}
		t.lines.add("");
		t.lines.add((t.fail == 0 ? "RESULT: PASSED " : "RESULT: FAILED ") + t.pass + " passed, " + t.fail + " failed");
		try {
			Files.write(Path.of("selftest-report.txt"), t.lines, StandardCharsets.UTF_8);
		} catch (Exception e) {
			BreakEverywhere.LOGGER.error("could not write report", e);
		}
		for (String l : t.lines) BreakEverywhere.LOGGER.info("[SelfTest] {}", l);
		server.halt(false);
	}

	private static int countEverywhere(ServerLevel level, java.util.function.Predicate<BlockState> p) {
		int n = 0;
		for (long c : LoadedChunks.snapshot(level)) {
			LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(c), ChunkPos.getZ(c));
			if (chunk == null) continue;
			for (LevelChunkSection s : chunk.getSections()) {
				if (s.hasOnlyAir() || !s.maybeHas(p)) continue;
				for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
					if (p.test(s.getBlockState(x, y, z))) n++;
				}
			}
		}
		return n;
	}

	private void diag(ServerLevel level, java.util.function.Predicate<BlockState> p) {
		int shown = 0;
		for (long c : LoadedChunks.snapshot(level)) {
			int cx = ChunkPos.getX(c), cz = ChunkPos.getZ(c);
			LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
			if (chunk == null) continue;
			for (int y = -64; y < -60; y++) {
				BlockPos pos = new BlockPos(cx * 16 + 1, y, cz * 16 + 1);
				if (!p.test(chunk.getBlockState(pos))) continue;
				var viaLevel = level.getChunkAt(pos);
				boolean same = viaLevel == chunk;
				boolean set = level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				lines.add("[INFO] chunk " + cx + "," + cz + " y=" + y + " sameChunkObj=" + same
						+ " chunkStatus=" + chunk.getPersistedStatus() + " fullStatus=" + chunk.getFullStatus()
						+ " setBlock=" + set + " after(level)=" + level.getBlockState(pos) + " after(chunk)=" + chunk.getBlockState(pos)
						+ " sectionIdx=" + chunk.getSectionIndex(y) + " sections=" + chunk.getSections().length);
				if (++shown >= 4) return;
				break;
			}
		}
	}

	private void runAll(MinecraftServer server) {
		ServerLevel level = server.overworld();

		// Load a few far chunks so the wave has to travel.
		int[][] far = {{10, 10}, {12, -8}, {-9, 5}, {-14, -14}};
		for (int[] c : far) {
			level.setChunkForced(c[0], c[1], true);
			level.getChunk(c[0], c[1]);
		}
		int loaded = LoadedChunks.count(level);
		check("loaded chunks are tracked", loaded >= 5, loaded);

		int y = -40;
		List<BlockPos> logs = new ArrayList<>();
		for (int[] c : far) logs.add(new BlockPos(c[0] * 16 + 3, y, c[1] * 16 + 7));
		logs.add(new BlockPos(2, y, 2));
		for (BlockPos p : logs) level.setBlock(p, Blocks.OAK_LOG.defaultBlockState(), 3);
		BlockPos birchLog = new BlockPos(far[0][0] * 16 + 9, y, far[0][1] * 16 + 9);
		level.setBlock(birchLog, Blocks.BIRCH_LOG.defaultBlockState(), 3);
		BlockPos stone = new BlockPos(far[1][0] * 16 + 1, y, far[1][1] * 16 + 1);
		level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
		BlockPos oakLeaves = new BlockPos(far[2][0] * 16 + 4, y, far[2][1] * 16 + 4);
		BlockPos birchLeaves = new BlockPos(far[3][0] * 16 + 5, y, far[3][1] * 16 + 5);
		level.setBlock(oakLeaves, Blocks.OAK_LEAVES.defaultBlockState(), 3);
		level.setBlock(birchLeaves, Blocks.BIRCH_LEAVES.defaultBlockState(), 3);

		FakePlayer fp = FakePlayer.get(level);
		fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));

		// ---- break one oak log -> all logs everywhere ----
		BlockPos first = logs.get(logs.size() - 1);
		BlockState st = level.getBlockState(first);
		level.removeBlock(first, false);
		BreakJob job = BreakManager.onPlayerBreak(level, fp, first, st);
		check("wave started for a log", job != null, job == null ? "null" : job.totalChunks() + " chunks");
		BreakManager.runAllNow();
		int logsLeft = 0;
		for (BlockPos p : logs) if (!level.getBlockState(p).isAir()) logsLeft++;
		check("oak logs in far chunks broken", logsLeft == 0, "left=" + logsLeft);
		check("birch log broken too (all logs group)", level.getBlockState(birchLog).isAir(), level.getBlockState(birchLog));
		check("stone untouched", level.getBlockState(stone).is(Blocks.STONE), level.getBlockState(stone));
		int dropped = 0;
		for (BlockPos p : logs.subList(0, logs.size() - 1)) {
			for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(2))) {
				if (e.getItem().is(Items.OAK_LOG)) dropped += e.getItem().getCount();
			}
		}
		check("logs dropped as items where they were", dropped >= 4, dropped);
		check("leaves untouched by log wave", level.getBlockState(oakLeaves).is(Blocks.OAK_LEAVES), "ok");

		// ---- leaves ----
		BlockState leaves = level.getBlockState(oakLeaves);
		level.removeBlock(oakLeaves, false);
		level.setBlock(oakLeaves.above(5), Blocks.OAK_LEAVES.defaultBlockState(), 3);
		BreakManager.onPlayerBreak(level, fp, oakLeaves, leaves);
		BreakManager.runAllNow();
		check("all leaves broken (oak + birch)", level.getBlockState(birchLeaves).isAir() && level.getBlockState(oakLeaves.above(5)).isAir(),
				level.getBlockState(birchLeaves) + " / " + level.getBlockState(oakLeaves.above(5)));

		// ---- blacklist ----
		check("bedrock is blacklisted", BreakManager.onPlayerBreak(level, fp, new BlockPos(0, -64, 0), Blocks.BEDROCK.defaultBlockState()) == null, "ok");

		// ---- disabled ----
		BEConfig.get().enabled = false;
		check("nothing happens when OFF", BreakManager.onPlayerBreak(level, fp, stone, Blocks.STONE.defaultBlockState()) == null, "ok");
		BEConfig.get().enabled = true;

		// ---- stress: all dirt in all loaded chunks (flat world has 2 dirt layers) ----
		int dirtBefore = countEverywhere(level, s -> s.is(Blocks.DIRT));
		long t0 = System.nanoTime();
		BlockState dirt = Blocks.DIRT.defaultBlockState();
		BreakManager.onPlayerBreak(level, fp, new BlockPos(0, -63, 0), dirt);
		BreakManager.runAllNow();
		long ms = (System.nanoTime() - t0) / 1_000_000;
		int dirtAfter = countEverywhere(level, s -> s.is(Blocks.DIRT));
		int items = level.getEntitiesOfClass(ItemEntity.class, new AABB(-400, -70, -400, 400, 330, 400)).size();
		check("stress: all dirt broken", dirtBefore > 1000 && dirtAfter == 0,
				"before=" + dirtBefore + " after=" + dirtAfter + " broken=" + (BreakManager.lastFinished == null ? -1 : BreakManager.lastFinished.broken) + " in " + ms + " ms");
		lines.add("[INFO] setBlock failed=" + (BreakManager.lastFinished == null ? -1 : BreakManager.lastFinished.failed));
		diag(level, s -> s.is(Blocks.DIRT));
		check("stress: item entities limited (merged per chunk)", items < BEConfig.get().maxSeparateDrops + 2000, items);

		// ---- time budget per tick ----
		BlockPos gp = new BlockPos(0, -61, 0);
		BreakManager.onPlayerBreak(level, fp, gp, level.getBlockState(gp));
		BreakJob running = null;
		long tickStart = System.nanoTime();
		BreakManager.tick(server);
		long tickMs = (System.nanoTime() - tickStart) / 1_000_000;
		check("one tick stays near the budget", tickMs < BEConfig.get().millisPerTick + 60, tickMs + " ms (budget " + BEConfig.get().millisPerTick + ")");
		BreakManager.runAllNow();
		check("grass blocks all broken", countEverywhere(level, s -> s.is(Blocks.GRASS_BLOCK)) == 0, "ok");

		// ---- commands ----
		var src = server.createCommandSourceStack().withSuppressedOutput();
		try {
			var d = server.getCommands().getDispatcher();
			check("/breakall status", d.execute("breakall status", src) == 1, "ok");
			check("/breakall off", d.execute("breakall off", src) == 1 && !BEConfig.get().enabled, "ok");
			check("/breakall on", d.execute("breakall on", src) == 1 && BEConfig.get().enabled, "ok");
			check("/breakall stop", d.execute("breakall stop", src) >= 0, "ok");
		} catch (Exception e) {
			check("commands", false, e.toString());
		}
	}
}
