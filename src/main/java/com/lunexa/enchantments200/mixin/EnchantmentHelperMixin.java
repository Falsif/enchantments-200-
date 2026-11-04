package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Levels;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

	/** Safety caps for effects that would break the game at level 200 (frost walker radius, riptide...). */
	@ModifyArgs(method = "runIterationOnItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;accept(Lnet/minecraft/core/Holder;I)V"))
	private static void e200$capLevel(Args args) {
		Holder<Enchantment> holder = args.get(0);
		int level = args.<Integer>get(1);
		args.set(1, E200Levels.effectiveLevel(holder, level));
	}

	@ModifyArgs(method = "runIterationOnItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;accept(Lnet/minecraft/core/Holder;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;)V"))
	private static void e200$capLevelInSlot(Args args) {
		Holder<Enchantment> holder = args.get(0);
		int level = args.<Integer>get(1);
		args.set(1, E200Levels.effectiveLevel(holder, level));
	}

	/** Enchanting table keeps vanilla levels (configurable). */
	@Redirect(method = "getAvailableEnchantmentResults", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
	private static int e200$tableMax(Enchantment enchantment) {
		return E200Levels.naturalMax(enchantment);
	}
}
