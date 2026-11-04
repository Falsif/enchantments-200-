package com.lunexa.enchantments200;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BillionZombieEggItem extends Item {
	public BillionZombieEggItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
		BlockPos pos = ctx.getClickedPos();
		BlockState state = level.getBlockState(pos);
		BlockPos spawnPos = state.getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(ctx.getClickedFace());
		if (BillionZombie.spawn(serverLevel, spawnPos) != null) {
			ctx.getItemInHand().consume(1, ctx.getPlayer());
		}
		return InteractionResult.CONSUME;
	}
}
