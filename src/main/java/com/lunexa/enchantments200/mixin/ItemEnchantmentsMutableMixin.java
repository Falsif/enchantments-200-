package com.lunexa.enchantments200.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** ItemEnchantments.Mutable.set/upgrade clamp levels to 255. Lift it. */
@Mixin(targets = "net.minecraft.world.item.enchantment.ItemEnchantments$Mutable")
public abstract class ItemEnchantmentsMutableMixin {
	@ModifyConstant(method = {"set", "upgrade"}, constant = @Constant(intValue = 255), require = 2)
	private int e200$noLevelLimit(int original) {
		return Integer.MAX_VALUE;
	}
}
