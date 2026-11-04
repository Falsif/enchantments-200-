package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Config;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
	@Shadow @Final private DataSlot cost;

	protected AnvilMenuMixin(MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access) {
		super(type, id, inventory, access);
	}

	/** Removes the "Too Expensive!" (cost >= 40) limit. */
	@ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
	private int e200$noTooExpensive(int original) {
		return E200Config.get().removeTooExpensive ? Integer.MAX_VALUE : original;
	}

	@Inject(method = "createResult", at = @At("RETURN"))
	private void e200$afterResult(CallbackInfo ci) {
		E200Config cfg = E200Config.get();
		ItemStack out = this.resultSlots.getItem(0);
		if (out.isEmpty()) return;

		ItemStack left = this.inputSlots.getItem(0);
		ItemStack right = this.inputSlots.getItem(1);

		// Same enchantment on both sides: levels add up (100 + 100 = 200), up to the max level.
		if (cfg.sumMode() && !left.isEmpty() && !right.isEmpty()) {
			ItemEnchantments l = EnchantmentHelper.getEnchantmentsForCrafting(left);
			ItemEnchantments r = EnchantmentHelper.getEnchantmentsForCrafting(right);
			ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(out));
			boolean changed = false;
			for (Object2IntMap.Entry<Holder<Enchantment>> e : r.entrySet()) {
				Holder<Enchantment> holder = e.getKey();
				int a = l.getLevel(holder);
				int b = e.getIntValue();
				int now = result.getLevel(holder);
				if (a > 0 && b > 0 && now > 0) {
					int sum = Math.min(holder.value().getMaxLevel(), a + b);
					if (sum > now) {
						result.set(holder, sum);
						changed = true;
					}
				}
			}
			if (changed) {
				EnchantmentHelper.setEnchantments(out, result.toImmutable());
			}
		}

		if (cfg.removeTooExpensive && this.cost.get() > cfg.clampedAnvilMaxCost()) {
			this.cost.set(cfg.clampedAnvilMaxCost());
		}
	}
}
