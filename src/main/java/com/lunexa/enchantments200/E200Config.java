package com.lunexa.enchantments200;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Settings stored in config/enchantments200.json. Reloaded with /enchant200 reload.
 */
public final class E200Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static volatile E200Config instance = new E200Config();

	/** Max level of every enchantment (1..255). 255 is the hard limit of Minecraft's item format. */
	public int maxLevel = 200;

	/** "sum": two books with the same enchantment add up (100 + 100 = 200). "vanilla": V + V = VI. */
	public String anvilMergeMode = "sum";

	/** Removes the "Too Expensive!" limit of the anvil. */
	public boolean removeTooExpensive = true;

	/** Highest XP-level cost of one anvil operation when removeTooExpensive is on. */
	public int anvilMaxCost = 30;

	/**
	 * Enchanting table, villagers and chest loot keep vanilla levels (Sharpness V etc.),
	 * so survival is not broken. High levels come from the anvil, commands and creative tab.
	 */
	public boolean vanillaLevelsForNaturalLoot = true;

	/** Maximum damage reduction from Protection-type enchantments above vanilla's 80% cap (0.8..0.999). */
	public double protectionMaxReduction = 0.99;

	/** Book names like "Book: Sharpness CC". */
	public boolean prettyBookNames = true;

	/**
	 * Effect level caps for enchantments that would crash, lag or kick players at level 200.
	 * The item still shows the real level; only the effect stops growing at this level. 0 = no cap.
	 */
	public Map<String, Integer> effectLevelCaps = defaultCaps();

	public static Map<String, Integer> defaultCaps() {
		Map<String, Integer> m = new LinkedHashMap<>();
		m.put("minecraft:frost_walker", 16);   // radius 200 = freezes ~125k blocks every step
		m.put("minecraft:riptide", 10);        // launches the player out of loaded chunks
		m.put("minecraft:loyalty", 10);        // trident overshoots and orbits forever
		m.put("minecraft:multishot", 16);      // 400 arrows per shot
		m.put("minecraft:quick_charge", 4);    // charge time would become zero/negative
		m.put("minecraft:lure", 5);            // wait time would become negative
		m.put("minecraft:wind_burst", 10);     // launches the player into the void
		m.put("minecraft:breach", 6);          // armor effectiveness would go below zero
		m.put("minecraft:knockback", 30);      // mobs fly through unloaded chunks
		m.put("minecraft:punch", 30);
		m.put("minecraft:soul_speed", 20);
		return m;
	}

	public static E200Config get() {
		return instance;
	}

	public int clampedMaxLevel() {
		return Math.max(1, Math.min(255, maxLevel));
	}

	public int clampedAnvilMaxCost() {
		return Math.max(1, Math.min(39, anvilMaxCost));
	}

	public boolean sumMode() {
		return !"vanilla".equalsIgnoreCase(anvilMergeMode);
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("enchantments200.json");
	}

	/** Loads (or creates) the config file. Returns a short status message. */
	public static synchronized String load() {
		Path p = path();
		E200Config cfg = new E200Config();
		String status;
		try {
			if (Files.exists(p)) {
				try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
					E200Config read = GSON.fromJson(r, E200Config.class);
					if (read != null) cfg = read;
				}
				if (cfg.effectLevelCaps == null) cfg.effectLevelCaps = new LinkedHashMap<>();
				if (cfg.anvilMergeMode == null) cfg.anvilMergeMode = "sum";
				status = "loaded " + p.getFileName();
			} else {
				status = "created " + p.getFileName();
			}
			save(cfg);
		} catch (Exception e) {
			Enchantments200.LOGGER.error("[Enchantments 200] Bad config {}, using defaults", p, e);
			cfg = new E200Config();
			status = "error in " + p.getFileName() + ": " + e.getMessage() + " (defaults used)";
		}
		instance = cfg;
		return status;
	}

	private static void save(E200Config cfg) {
		try {
			Files.createDirectories(path().getParent());
			try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
				GSON.toJson(cfg, w);
			}
		} catch (Exception e) {
			Enchantments200.LOGGER.warn("[Enchantments 200] Could not write config", e);
		}
	}
}
