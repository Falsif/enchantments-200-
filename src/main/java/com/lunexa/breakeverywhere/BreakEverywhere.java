package com.lunexa.breakeverywhere;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreakEverywhere implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Break Everywhere");

	@Override
	public void onInitialize() {
		LOGGER.info("[Break Everywhere] config: {}", BEConfig.load());
		LoadedChunks.register();

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world instanceof ServerLevel level && player instanceof ServerPlayer sp) {
				BreakManager.onPlayerBreak(level, sp, pos, state);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(BreakManager::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> BreakManager.stopAll());
		CommandRegistrationCallback.EVENT.register((d, ctx, sel) -> registerCommands(d));

		if (Boolean.getBoolean("be.selftest")) {
			LOGGER.warn("[Break Everywhere] SELF TEST MODE: the server will run tests and stop.");
			ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
		}
	}

	private static Component tr(String key, String fallback, Object... args) {
		return Component.translatableWithFallback(key, fallback, args);
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("breakall")
				.executes(BreakEverywhere::status)
				.then(Commands.literal("status").executes(BreakEverywhere::status))
				.then(Commands.literal("on").requires(s -> s.hasPermission(2)).executes(c -> setEnabled(c, true)))
				.then(Commands.literal("off").requires(s -> s.hasPermission(2)).executes(c -> setEnabled(c, false)))
				.then(Commands.literal("stop").requires(s -> s.hasPermission(2)).executes(c -> {
					int n = BreakManager.activeJobs();
					BreakManager.stopAll();
					c.getSource().sendSuccess(() -> tr("breakeverywhere.cmd.stopped", "Stopped %s running waves", n), true);
					return n;
				}))
				.then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(c -> {
					String st = BEConfig.load();
					c.getSource().sendSuccess(() -> tr("breakeverywhere.cmd.reload", "Break Everywhere config: %s", st), true);
					return 1;
				})));
	}

	private static int setEnabled(CommandContext<CommandSourceStack> c, boolean on) {
		BEConfig.get().enabled = on;
		BEConfig.save();
		if (!on) BreakManager.stopAll();
		c.getSource().sendSuccess(() -> (on
				? tr("breakeverywhere.cmd.on", "Break Everywhere: ON")
				: tr("breakeverywhere.cmd.off", "Break Everywhere: OFF")).copy().withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED), true);
		return 1;
	}

	private static int status(CommandContext<CommandSourceStack> c) {
		boolean on = BEConfig.get().enabled;
		int loaded = LoadedChunks.count(c.getSource().getLevel());
		c.getSource().sendSuccess(() -> tr("breakeverywhere.cmd.status",
				"Break Everywhere: %s, loaded chunks here: %s, running waves: %s. Commands: /breakall on|off|stop|reload",
				on ? "ON" : "OFF", loaded, BreakManager.activeJobs()), false);
		return on ? 1 : 0;
	}
}
