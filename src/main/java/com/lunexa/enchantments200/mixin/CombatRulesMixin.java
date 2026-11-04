package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Config;
import net.minecraft.world.damagesource.CombatRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CombatRules.class)
public abstract class CombatRulesMixin {

	/**
	 * Vanilla caps enchantment protection at 20 points (80% less damage).
	 * Above 20 the reduction keeps growing smoothly up to protectionMaxReduction (99% by default).
	 * Protection CC on one piece = ~96%, on full armor = ~99%.
	 */
	@Inject(method = "getDamageAfterMagicAbsorb", at = @At("HEAD"), cancellable = true)
	private static void e200$protection(float damage, float protection, CallbackInfoReturnable<Float> cir) {
		if (protection <= 20.0F) return;
		double max = Math.max(0.8, Math.min(0.999, E200Config.get().protectionMaxReduction));
		double extra = protection - 20.0F;
		double reduction = 0.8 + (max - 0.8) * (1.0 - Math.exp(-extra / 100.0));
		cir.setReturnValue((float) (damage * (1.0 - reduction)));
	}
}
