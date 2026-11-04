package com.lunexa.enchantments200.client;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class Enchantments200Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Shows "Level 200 / 200" under enchanted books.
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
			if (stored == null || stored.isEmpty()) return;
			boolean many = stored.size() > 1;
			for (Object2IntMap.Entry<Holder<Enchantment>> e : stored.entrySet()) {
				int max = e.getKey().value().getMaxLevel();
				Component line = many
						? Component.translatableWithFallback("tooltip.enchantments200.level_named", "%s: level %s / %s",
								e.getKey().value().description(), e.getIntValue(), max)
						: Component.translatableWithFallback("tooltip.enchantments200.level", "Level %s / %s", e.getIntValue(), max);
				lines.add(line.copy().withStyle(ChatFormatting.DARK_PURPLE));
			}
		});
	}
}
