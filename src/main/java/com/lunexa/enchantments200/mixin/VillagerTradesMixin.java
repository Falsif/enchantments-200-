package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Levels;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EnchantBookForEmeralds")
public abstract class VillagerTradesMixin {

	/** Librarians keep selling vanilla levels (configurable). */
	@Redirect(method = "getOffer", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
	private int e200$tradeMax(Enchantment enchantment) {
		return E200Levels.naturalMax(enchantment);
	}
}
