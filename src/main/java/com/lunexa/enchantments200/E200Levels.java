package com.lunexa.enchantments200;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;
import java.util.Optional;

public final class E200Levels {
	private E200Levels() {}

	/** Vanilla (data pack) max level, ignoring this mod. */
	public static int vanillaMax(Enchantment e) {
		return e.definition().maxLevel();
	}

	private static final ThreadLocal<int[]> NATURAL = ThreadLocal.withInitial(() -> new int[1]);

	public static void beginNatural() {
		NATURAL.get()[0]++;
	}

	public static void endNatural() {
		int[] d = NATURAL.get();
		if (d[0] > 0) d[0]--;
	}

	/** True while vanilla code picks levels for the enchanting table (only when the config asks for vanilla levels). */
	public static boolean inNatural() {
		return NATURAL.get()[0] > 0 && E200Config.get().vanillaLevelsForNaturalLoot;
	}

	/** Max level used for enchanting table / villagers / loot. */
	public static int naturalMax(Enchantment e) {
		return E200Config.get().vanillaLevelsForNaturalLoot ? vanillaMax(e) : e.getMaxLevel();
	}

	/** Level actually used for the effect (after safety caps). */
	public static int effectiveLevel(Holder<Enchantment> holder, int level) {
		Map<String, Integer> caps = E200Config.get().effectLevelCaps;
		if (caps == null || caps.isEmpty() || level <= 1) return level;
		Optional<ResourceKey<Enchantment>> key = holder.unwrapKey();
		if (key.isEmpty()) return level;
		Integer cap = caps.get(key.get().location().toString());
		if (cap == null || cap <= 0) return level;
		return Math.min(level, cap);
	}
}
