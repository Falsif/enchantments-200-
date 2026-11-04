package com.lunexa.breakeverywhere;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** config/breakeverywhere.json, reloaded with /breakall reload. */
public final class BEConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static volatile BEConfig instance = new BEConfig();

	/** Master switch (also /breakall on|off). */
	public boolean enabled = true;

	/** Breaking any leaves breaks ALL kinds of leaves (oak, birch, spruce...). */
	public boolean groupAllLeaves = true;

	/** Breaking any log breaks ALL kinds of logs/wood/stems. */
	public boolean groupAllLogs = true;

	/** Drop items where each block was. */
	public boolean dropItems = true;

	/** Vanilla creative mode drops nothing; set true to drop anyway. */
	public boolean dropsInCreative = false;

	/**
	 * After this many separate drops in one wave, drops of each chunk are merged into stacks
	 * and dropped at one spot of that chunk (otherwise breaking stone = millions of items = crash).
	 */
	public int maxSeparateDrops = 3000;

	/** Milliseconds per server tick the mod may spend breaking (keeps 20 TPS). */
	public int millisPerTick = 25;

	/** Update neighbors (sand falls, torches pop off, water flows). false = faster. */
	public boolean updateNeighbors = true;

	/** Show progress in the action bar. */
	public boolean showProgress = true;

	/** Blocks that never break everywhere. */
	public List<String> blacklist = defaultBlacklist();

	public static List<String> defaultBlacklist() {
		List<String> l = new ArrayList<>();
		for (String s : new String[]{"bedrock", "barrier", "light", "end_portal_frame", "end_portal", "end_gateway",
				"nether_portal", "command_block", "chain_command_block", "repeating_command_block", "structure_block",
				"jigsaw", "structure_void", "spawner", "trial_spawner", "vault", "reinforced_deepslate"}) {
			l.add("minecraft:" + s);
		}
		return l;
	}

	public static BEConfig get() {
		return instance;
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("breakeverywhere.json");
	}

	public static synchronized String load() {
		Path p = path();
		BEConfig cfg = new BEConfig();
		String status;
		try {
			if (Files.exists(p)) {
				try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
					BEConfig read = GSON.fromJson(r, BEConfig.class);
					if (read != null) cfg = read;
				}
				if (cfg.blacklist == null) cfg.blacklist = new ArrayList<>();
				status = "loaded";
			} else {
				status = "created";
			}
			instance = cfg;
			save();
		} catch (Exception e) {
			BreakEverywhere.LOGGER.error("[Break Everywhere] Bad config {}, using defaults", p, e);
			instance = new BEConfig();
			status = "error: " + e.getMessage() + " (defaults used)";
		}
		return status;
	}

	public static void save() {
		try {
			Files.createDirectories(path().getParent());
			try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
				GSON.toJson(instance, w);
			}
		} catch (Exception e) {
			BreakEverywhere.LOGGER.warn("[Break Everywhere] Could not write config", e);
		}
	}
}
