package com.lunexa.enchantments200;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** Zombie in full netherite armor with a netherite sword with Sharpness 1 000 000 000. */
public final class BillionZombie {
	public static final int SHARPNESS = 1_000_000_000;

	private BillionZombie() {}

	public static Zombie spawn(ServerLevel level, BlockPos pos) {
		Zombie z = EntityType.ZOMBIE.create(level);
		if (z == null) return null;
		z.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
		z.setYHeadRot(z.getYRot());
		z.setBaby(false);

		Registry<Enchantment> reg = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> sharp = reg.getHolderOrThrow(Enchantments.SHARPNESS);
		ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
		sword.enchant(sharp, SHARPNESS);

		z.setItemSlot(EquipmentSlot.MAINHAND, sword);
		z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
		z.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
		z.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
		z.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			z.setDropChance(slot, 0.0F); // the billion sword does not drop
		}
		z.setCustomName(Component.translatableWithFallback("entity.enchantments200.billion_zombie", "Billion Zombie")
				.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
		z.setPersistenceRequired();
		level.addFreshEntity(z);
		return z;
	}
}
