package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Levels;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla "Ingredients" and search tabs add an enchanted book for every level from 1 to max.
 * With max = 1 000 000 000 that is billions of items and the creative inventory would freeze.
 * These two lists keep vanilla levels; the "Enchantments 200" tab has the big ones.
 */
@Mixin(CreativeModeTabs.class)
public abstract class CreativeModeTabsMixin {
	@Inject(method = {"generateEnchantmentBookTypesOnlyMaxLevel", "generateEnchantmentBookTypesAllLevels"}, at = @At("HEAD"))
	private static void e200$start(CreativeModeTab.Output output, HolderLookup<Enchantment> lookup, CreativeModeTab.TabVisibility visibility, CallbackInfo ci) {
		E200Levels.beginForcedVanilla();
	}

	@Inject(method = {"generateEnchantmentBookTypesOnlyMaxLevel", "generateEnchantmentBookTypesAllLevels"}, at = @At("RETURN"))
	private static void e200$end(CreativeModeTab.Output output, HolderLookup<Enchantment> lookup, CreativeModeTab.TabVisibility visibility, CallbackInfo ci) {
		E200Levels.endForcedVanilla();
	}
}
