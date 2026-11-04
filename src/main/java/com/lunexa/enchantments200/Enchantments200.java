package com.lunexa.enchantments200;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class Enchantments200 implements ModInitializer {
	public static final String MOD_ID = "enchantments200";
	public static final Logger LOGGER = LoggerFactory.getLogger("Enchantments 200");

	public static final ResourceKey<CreativeModeTab> TAB_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MOD_ID, "books"));

	public static final Item BILLION_ZOMBIE_EGG = new BillionZombieEggItem(new Item.Properties());

	@Override
	public void onInitialize() {
		LOGGER.info("[Enchantments 200] config: {}", E200Config.load());

		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "billion_zombie_spawn_egg"), BILLION_ZOMBIE_EGG);
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(BILLION_ZOMBIE_EGG));

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricItemGroup.builder()
				.title(Component.translatableWithFallback("itemGroup.enchantments200.books", "Enchantments 200"))
				.icon(() -> {
					ItemStack icon = new ItemStack(Items.ENCHANTED_BOOK);
					icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
					return icon;
				})
				.displayItems((params, output) -> {
					output.accept(BILLION_ZOMBIE_EGG);
					params.holders().lookup(Registries.ENCHANTMENT).ifPresent(reg ->
						reg.listElements()
								.sorted(Comparator.comparing(h -> h.key().location().toString()))
								.forEach(h -> {
									Holder<Enchantment> holder = h;
									for (int level : E200Books.creativeLevels(holder.value().getMaxLevel())) {
										output.accept(E200Books.book(holder, level));
									}
								}));
				})
				.build());

		CommandRegistrationCallback.EVENT.register(E200Commands::register);

		if (Boolean.getBoolean("e200.selftest")) {
			LOGGER.warn("[Enchantments 200] SELF TEST MODE: the server will run tests and stop.");
			ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
		}
	}
}
