package com.lunexa.enchantments200;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

public final class E200Text {
	private E200Text() {}

	private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
	private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

	/** Roman numeral for 1..399, plain number above (1 000 000 000). */
	public static String levelText(int level) {
		if (level <= 0 || level > 399) return String.format(java.util.Locale.ROOT, "%,d", level).replace(',', ' ');
		StringBuilder sb = new StringBuilder();
		int n = level;
		for (int i = 0; i < VALUES.length; i++) {
			while (n >= VALUES[i]) {
				n -= VALUES[i];
				sb.append(SYMBOLS[i]);
			}
		}
		return sb.toString();
	}

	public static MutableComponent levelComponent(int level) {
		// Uses the resource-pack translation for I..X if present, roman numerals otherwise.
		return Component.translatableWithFallback("enchantment.level." + level, levelText(level));
	}

	/** Same look as vanilla: gray (red for curses), level shown unless it is a level-1-only enchantment at I. */
	public static Component fullName(Holder<Enchantment> holder, int level) {
		MutableComponent c = holder.value().description().copy();
		ComponentUtils.mergeStyles(c, Style.EMPTY.withColor(holder.is(EnchantmentTags.CURSE) ? ChatFormatting.RED : ChatFormatting.GRAY));
		if (level != 1 || E200Levels.vanillaMax(holder.value()) != 1) {
			c.append(CommonComponents.SPACE).append(levelComponent(level));
		}
		return c;
	}

	/** Uncolored "Sharpness CC" for item names. */
	public static MutableComponent nameWithLevel(Holder<Enchantment> holder, int level) {
		MutableComponent c = holder.value().description().copy();
		if (level != 1 || E200Levels.vanillaMax(holder.value()) != 1) {
			c.append(CommonComponents.SPACE).append(levelComponent(level));
		}
		return c;
	}
}
