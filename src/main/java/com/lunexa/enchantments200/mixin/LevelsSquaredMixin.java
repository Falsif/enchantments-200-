package com.lunexa.enchantments200.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla computes "levels_squared" (Efficiency) as level * level in int.
 * Above level 46340 that overflows and becomes NEGATIVE. Compute it without overflow.
 */
@Mixin(targets = "net.minecraft.world.item.enchantment.LevelBasedValue$LevelsSquared")
public abstract class LevelsSquaredMixin {
	@Shadow
	public abstract float added();

	@Inject(method = "calculate", at = @At("HEAD"), cancellable = true)
	private void e200$noOverflow(int level, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue((float) ((double) level * (double) level) + this.added());
	}
}
