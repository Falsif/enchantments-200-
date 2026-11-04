package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Config;
import com.lunexa.enchantments200.E200Levels;
import com.lunexa.enchantments200.E200Text;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

	/** Every enchantment: max level 200 (or the config value). Keeps a higher max from other mods. */
	@Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
	private void e200$maxLevel(CallbackInfoReturnable<Integer> cir) {
		int vanilla = ((Enchantment) (Object) this).definition().maxLevel();
		if (E200Levels.inNatural()) {
			cir.setReturnValue(vanilla);
			return;
		}
		cir.setReturnValue(Math.max(vanilla, E200Config.get().clampedMaxLevel()));
	}

	/** Roman numerals for every level (vanilla only has translations for I..X). */
	@Inject(method = "getFullname", at = @At("HEAD"), cancellable = true)
	private static void e200$fullName(Holder<Enchantment> holder, int level, CallbackInfoReturnable<Component> cir) {
		cir.setReturnValue(E200Text.fullName(holder, level));
	}
}
