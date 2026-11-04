package com.lunexa.enchantments200.mixin;

import com.lunexa.enchantments200.E200Config;
import com.lunexa.enchantments200.E200Text;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {

	/** "Book: Sharpness CC" instead of "Enchanted Book" for single-enchantment books. Always matches the real level. */
	@Inject(method = "getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
	private void e200$bookName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
		if ((Object) this != Items.ENCHANTED_BOOK || !E200Config.get().prettyBookNames) return;
		ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (stored == null || stored.size() != 1) return;
		Object2IntMap.Entry<Holder<Enchantment>> e = stored.entrySet().iterator().next();
		cir.setReturnValue(Component.translatableWithFallback("item.enchantments200.book", "Book: %s",
				E200Text.nameWithLevel(e.getKey(), e.getIntValue())));
	}
}
