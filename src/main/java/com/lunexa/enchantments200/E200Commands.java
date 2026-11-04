package com.lunexa.enchantments200;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class E200Commands {
	private E200Commands() {}

	private static Component tr(String key, String fallback, Object... args) {
		return Component.translatableWithFallback(key, fallback, args);
	}

	public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx, Commands.CommandSelection selection) {
		d.register(Commands.literal("enchant200")
				.executes(E200Commands::help)
				.then(Commands.literal("help").executes(E200Commands::help))
				.then(Commands.literal("list").executes(E200Commands::list))
				.then(Commands.literal("reload")
						.requires(s -> s.hasPermission(2))
						.executes(E200Commands::reload))
				.then(Commands.literal("give")
						.requires(s -> s.hasPermission(2))
						.then(Commands.argument("targets", EntityArgument.players())
								.then(Commands.argument("enchantment", ResourceArgument.resource(ctx, Registries.ENCHANTMENT))
										.executes(c -> give(c, -1))
										.then(Commands.argument("level", IntegerArgumentType.integer(1))
												.executes(c -> give(c, IntegerArgumentType.getInteger(c, "level")))))))
				.then(Commands.literal("max")
						.requires(s -> s.hasPermission(2))
						.executes(E200Commands::maxAll)
						.then(Commands.argument("enchantment", ResourceArgument.resource(ctx, Registries.ENCHANTMENT))
								.executes(E200Commands::maxOne)))
				.then(Commands.literal("apply")
						.requires(s -> s.hasPermission(2))
						.then(Commands.argument("enchantment", ResourceArgument.resource(ctx, Registries.ENCHANTMENT))
								.executes(c -> apply(c, -1))
								.then(Commands.argument("level", IntegerArgumentType.integer(1))
										.executes(c -> apply(c, IntegerArgumentType.getInteger(c, "level")))))));
	}

	private static int help(CommandContext<CommandSourceStack> c) {
		CommandSourceStack s = c.getSource();
		s.sendSuccess(() -> tr("e200.help.title", "=== Enchantments 200 (max level %s) ===", E200Config.get().clampedMaxLevel()).copy().withStyle(ChatFormatting.LIGHT_PURPLE), false);
		line(s, "/enchant200 help", "e200.help.help", "list of commands");
		line(s, "/enchant200 list", "e200.help.list", "all enchantments (click one to get a book)");
		line(s, "/enchant200 give <player> <enchantment> [level]", "e200.help.give", "give a book, e.g. /enchant200 give Steve sharpness 200");
		line(s, "/enchant200 max [enchantment]", "e200.help.max", "get a max-level book (no argument = all books)");
		line(s, "/enchant200 apply <enchantment> [level]", "e200.help.apply", "enchant the item in your hand directly");
		line(s, "/enchant200 reload", "e200.help.reload", "reload config/enchantments200.json");
		return 1;
	}

	private static void line(CommandSourceStack s, String cmd, String key, String fallback) {
		int cut = cmd.length();
		int a = cmd.indexOf(" <");
		int b = cmd.indexOf(" [");
		if (a >= 0) cut = Math.min(cut, a);
		if (b >= 0) cut = Math.min(cut, b);
		String suggest = cut < cmd.length() ? cmd.substring(0, cut) + " " : cmd;
		MutableComponent m = Component.literal(cmd).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)
						.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest)))
				.append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
				.append(tr(key, fallback).copy().withStyle(ChatFormatting.GRAY));
		s.sendSuccess(() -> m, false);
	}

	private static List<Holder.Reference<Enchantment>> all(CommandSourceStack s) {
		Registry<Enchantment> reg = s.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
		return reg.holders().sorted(Comparator.comparing(h -> h.key().location().toString())).toList();
	}

	private static String shortId(Holder.Reference<Enchantment> h) {
		ResourceLocation id = h.key().location();
		return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
	}

	private static int list(CommandContext<CommandSourceStack> c) {
		CommandSourceStack s = c.getSource();
		List<Holder.Reference<Enchantment>> list = all(s);
		s.sendSuccess(() -> tr("e200.list.title", "Enchantments (%s):", list.size()).copy().withStyle(ChatFormatting.LIGHT_PURPLE), false);
		for (Holder.Reference<Enchantment> h : list) {
			int max = h.value().getMaxLevel();
			String cmd = "/enchant200 give @s " + shortId(h) + " " + max;
			MutableComponent m = Component.literal("• ").withStyle(ChatFormatting.DARK_PURPLE)
					.append(E200Text.fullName(h, max))
					.append(Component.literal("  " + shortId(h)).withStyle(ChatFormatting.DARK_GRAY))
					.withStyle(Style.EMPTY
							.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(cmd))));
			s.sendSuccess(() -> m, false);
		}
		return list.size();
	}

	private static int reload(CommandContext<CommandSourceStack> c) {
		String status = E200Config.load();
		c.getSource().sendSuccess(() -> tr("e200.reload", "Enchantments 200 config: %s", status), true);
		return 1;
	}

	private static int checkLevel(CommandSourceStack s, Holder<Enchantment> h, int requested) {
		int max = h.value().getMaxLevel();
		int level = requested < 0 ? max : requested;
		if (level > max) {
			s.sendFailure(tr("e200.error.level", "Level %s is above the max level %s (see config/enchantments200.json)", level, max));
			return -1;
		}
		return level;
	}

	private static int give(CommandContext<CommandSourceStack> c, int requested) throws CommandSyntaxException {
		CommandSourceStack s = c.getSource();
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
		Holder.Reference<Enchantment> h = ResourceArgument.getEnchantment(c, "enchantment");
		int level = checkLevel(s, h, requested);
		if (level < 0) return 0;
		for (ServerPlayer p : targets) {
			E200Books.give(p, E200Books.book(h, level));
		}
		Component name = E200Text.fullName(h, level);
		if (targets.size() == 1) {
			Component who = targets.iterator().next().getDisplayName();
			s.sendSuccess(() -> tr("e200.give.one", "Gave a book %s to %s", name, who), true);
		} else {
			s.sendSuccess(() -> tr("e200.give.many", "Gave a book %s to %s players", name, targets.size()), true);
		}
		return targets.size();
	}

	private static int maxOne(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		CommandSourceStack s = c.getSource();
		ServerPlayer p = s.getPlayerOrException();
		Holder.Reference<Enchantment> h = ResourceArgument.getEnchantment(c, "enchantment");
		int max = h.value().getMaxLevel();
		E200Books.give(p, E200Books.book(h, max));
		Component name = E200Text.fullName(h, max);
		s.sendSuccess(() -> tr("e200.max.one", "Got a book %s", name), false);
		return 1;
	}

	private static int maxAll(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		CommandSourceStack s = c.getSource();
		ServerPlayer p = s.getPlayerOrException();
		List<Holder.Reference<Enchantment>> list = all(s);
		for (Holder.Reference<Enchantment> h : list) {
			E200Books.give(p, E200Books.book(h, h.value().getMaxLevel()));
		}
		s.sendSuccess(() -> tr("e200.max.all", "Got %s max-level books (extra ones are dropped at your feet)", list.size()), false);
		return list.size();
	}

	private static int apply(CommandContext<CommandSourceStack> c, int requested) throws CommandSyntaxException {
		CommandSourceStack s = c.getSource();
		ServerPlayer p = s.getPlayerOrException();
		Holder.Reference<Enchantment> h = ResourceArgument.getEnchantment(c, "enchantment");
		int level = checkLevel(s, h, requested);
		if (level < 0) return 0;
		ItemStack stack = p.getMainHandItem();
		if (stack.isEmpty()) {
			s.sendFailure(tr("e200.error.hand", "Hold an item in your main hand"));
			return 0;
		}
		if (stack.is(Items.BOOK)) {
			stack.shrink(1);
			E200Books.give(p, E200Books.book(h, level));
		} else {
			EnchantmentHelper.updateEnchantments(stack, m -> m.set(h, level));
		}
		Component name = E200Text.fullName(h, level);
		s.sendSuccess(() -> tr("e200.apply", "Applied %s", name), true);
		return 1;
	}
}
