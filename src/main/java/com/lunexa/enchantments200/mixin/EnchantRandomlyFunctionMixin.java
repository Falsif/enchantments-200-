package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Levels;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyFunctionMixin {

	/** Chest/fishing loot books keep vanilla levels (configurable). */
	@Redirect(method = "enchantItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
	private static int e200$lootMax(Enchantment enchantment) {
		return E200Levels.naturalMax(enchantment);
	}
}
