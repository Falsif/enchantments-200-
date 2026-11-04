package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Config;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.Items;
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
	@Shadow private String itemName;
	@Shadow public int repairItemCountCost;

	protected AnvilMenuMixin(MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access) {
		super(type, id, inventory, access);
	}

	/**
	 * Vanilla anvil math multiplies cost by the level (int): with levels in the millions it overflows and
	 * the result disappears. When big levels are involved, merge enchantments with our own safe math.
	 */
	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void e200$bigLevels(CallbackInfo ci) {
		ItemStack left = this.inputSlots.getItem(0);
		ItemStack right = this.inputSlots.getItem(1);
		if (left.isEmpty() || right.isEmpty()) return;
		ItemEnchantments le = EnchantmentHelper.getEnchantmentsForCrafting(left);
		ItemEnchantments re = EnchantmentHelper.getEnchantmentsForCrafting(right);
		if (!e200$hasBig(le) && !e200$hasBig(re)) return;
		boolean rightBook = right.has(DataComponents.STORED_ENCHANTMENTS);
		if (!rightBook && !left.is(right.getItem())) return;

		E200Config cfg = E200Config.get();
		boolean creative = this.player.getAbilities().instabuild;
		ItemStack out = left.copy();
		ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(le);
		boolean applied = false;
		for (Object2IntMap.Entry<Holder<Enchantment>> e : re.entrySet()) {
			Holder<Enchantment> h = e.getKey();
			boolean ok = creative || left.is(Items.ENCHANTED_BOOK) || h.value().canEnchant(left);
			for (Holder<Enchantment> other : m.keySet()) {
				if (!other.equals(h) && !Enchantment.areCompatible(h, other)) ok = false;
			}
			if (!ok) continue;
			long a = m.getLevel(h);
			long b = e.getIntValue();
			long level;
			if (a <= 0) level = b;
			else if (cfg.sumMode()) level = a + b;
			else level = a == b ? a + 1 : Math.max(a, b);
			level = Math.min(level, h.value().getMaxLevel());
			m.set(h, (int) level);
			applied = true;
		}
		ci.cancel();
		if (!applied) {
			this.resultSlots.setItem(0, ItemStack.EMPTY);
			this.cost.set(0);
			return;
		}
		EnchantmentHelper.setEnchantments(out, m.toImmutable());

		if (!rightBook && left.isDamageableItem()) {
			int l = left.getMaxDamage() - left.getDamageValue();
			int r = right.getMaxDamage() - right.getDamageValue() + left.getMaxDamage() * 12 / 100;
			out.setDamageValue(Math.max(0, left.getMaxDamage() - (l + r)));
		}
		if (this.itemName != null && !StringUtil.isBlank(this.itemName)) {
			if (!this.itemName.equals(left.getHoverName().getString())) out.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
		} else if (left.has(DataComponents.CUSTOM_NAME)) {
			out.remove(DataComponents.CUSTOM_NAME);
		}
		int repair = Math.max(left.getOrDefault(DataComponents.REPAIR_COST, 0), right.getOrDefault(DataComponents.REPAIR_COST, 0));
		out.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(repair));

		this.repairItemCountCost = 0;
		this.resultSlots.setItem(0, out);
		this.cost.set(cfg.removeTooExpensive ? cfg.clampedAnvilMaxCost() : 39);
		this.broadcastChanges();
	}

	private static boolean e200$hasBig(ItemEnchantments e) {
		for (Object2IntMap.Entry<Holder<Enchantment>> x : e.entrySet()) {
			if (x.getIntValue() > 1000) return true;
		}
		return false;
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
					int sum = (int) Math.min(holder.value().getMaxLevel(), (long) a + b);
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
