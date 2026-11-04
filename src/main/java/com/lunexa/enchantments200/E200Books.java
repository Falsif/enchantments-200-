package com.lunexa.enchantments200;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.TreeSet;

public final class E200Books {
	private E200Books() {}

	public static ItemStack book(Holder<Enchantment> holder, int level) {
		ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
		ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		m.set(holder, level);
		stack.set(DataComponents.STORED_ENCHANTMENTS, m.toImmutable());
		return stack;
	}

	public static int storedLevel(ItemStack stack, Holder<Enchantment> holder) {
		ItemEnchantments st = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		return st.getLevel(holder);
	}

	public static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	/** Levels shown in the creative tab for each enchantment. */
	public static TreeSet<Integer> creativeLevels(int max) {
		TreeSet<Integer> set = new TreeSet<>();
		for (int l : new int[]{1, 5, 10, 50, 100, 200, 1000, 1_000_000, 1_000_000_000}) {
			if (l <= max) set.add(l);
		}
		set.add(max);
		return set;
	}
}
