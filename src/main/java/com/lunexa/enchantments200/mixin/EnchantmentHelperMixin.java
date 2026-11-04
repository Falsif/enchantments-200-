package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Levels;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import java.util.List;
import java.util.stream.Stream;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

	/** Safety caps for effects that would break the game at level 200 (frost walker radius, riptide...). */
	@ModifyArgs(method = "runIterationOnItem*", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;accept(Lnet/minecraft/core/Holder;I)V"))
	private static void e200$capLevel(Args args) {
		Holder<Enchantment> holder = args.get(0);
		int level = args.<Integer>get(1);
		args.set(1, E200Levels.effectiveLevel(holder, level));
	}

	@ModifyArgs(method = "runIterationOnItem*", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;accept(Lnet/minecraft/core/Holder;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;)V"))
	private static void e200$capLevelInSlot(Args args) {
		Holder<Enchantment> holder = args.get(0);
		int level = args.<Integer>get(1);
		args.set(1, E200Levels.effectiveLevel(holder, level));
	}

	/** Enchanting table keeps vanilla levels (configurable). The level loop is inside a lambda, so we mark the whole call. */
	@Inject(method = "getAvailableEnchantmentResults", at = @At("HEAD"))
	private static void e200$tableStart(int power, ItemStack stack, Stream<Holder<Enchantment>> pool, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
		E200Levels.beginNatural();
	}

	@Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"))
	private static void e200$tableEnd(int power, ItemStack stack, Stream<Holder<Enchantment>> pool, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
		E200Levels.endNatural();
	}
}
