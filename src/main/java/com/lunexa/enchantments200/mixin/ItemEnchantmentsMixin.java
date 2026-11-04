package com.lunexa.enchantments200.mixin;

import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Minecraft stores enchantment levels only up to 255 (save format + check in the constructor). Lift it. */
@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsMixin {
	@ModifyConstant(method = {"<clinit>", "<init>"}, constant = @Constant(intValue = 255), require = 2)
	private static int e200$noLevelLimit(int original) {
		return Integer.MAX_VALUE;
	}
}
