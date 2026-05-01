package com.playymcmc007.uselessthings.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class SuperBoneMealItem extends Item {

    private static final int RADIUS = 32;
    private static final double DROP_CHANCE = 0.5;

    public SuperBoneMealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos centerPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(centerPos);
        Block clickedBlock = clickedState.getBlock();

        boolean isValidTarget = clickedBlock == Blocks.GRASS_BLOCK;

        if (!isValidTarget) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            spawnBoneMealParticles(level, centerPos);
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        List<BlockPos> targets = getTargetBlocksInCircle(serverLevel, centerPos, RADIUS);

        int processed = 0;
        for (BlockPos pos : targets) {
            BlockState state = serverLevel.getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof BonemealableBlock bonemealable) {
                if (bonemealable.isValidBonemealTarget(serverLevel, pos, state, false)) {
                    if (serverLevel.random.nextDouble() < DROP_CHANCE) {
                        if (bonemealable.isBonemealSuccess(serverLevel, serverLevel.random, pos, state)) {
                            bonemealable.performBonemeal(serverLevel, serverLevel.random, pos, state);
                            processed++;
                        }
                    }
                }
            }

            level.playSound(
                    null,
                    centerPos,
                    SoundEvents.BONE_MEAL_USE,
                    SoundSource.BLOCKS,
                    1.0f, 1.0f
            );
        }

        if (!context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }


        return InteractionResult.SUCCESS;
    }
    private void spawnBoneMealParticles(Level level, BlockPos pos) {
        for (int i = 0; i < 30; i++) {
            double offsetX = level.random.nextDouble() - 0.5;
            double offsetY = level.random.nextDouble();
            double offsetZ = level.random.nextDouble() - 0.5;
            level.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + 0.5 + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    0, 0, 0
            );
        }
    }

    private List<BlockPos> getTargetBlocksInCircle(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> targets = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    for (int dy = -3; dy <= 32; dy++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        Block block = state.getBlock();

                        if (block instanceof BonemealableBlock) {
                            targets.add(pos);
                        }
                    }
                }
            }
        }

        return targets;
    }
}