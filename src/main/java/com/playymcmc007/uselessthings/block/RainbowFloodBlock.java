package com.playymcmc007.uselessthings.block;

import com.playymcmc007.uselessthings.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

public class RainbowFloodBlock extends LiquidBlock {
    public RainbowFloodBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) {
            level.getChunkAt(pos).setUnsaved(true);
            level.sendBlockUpdated(pos, state, state, 2);
        }
        super.animateTick(state, level, pos, random);
    }
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            if (!itemEntity.isRemoved()) {
                level.explode(null,
                        itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                        1.0f,
                        false,
                        Level.ExplosionInteraction.MOB);

                itemEntity.discard();
            }
            return;
        }
        if (!level.isClientSide && entity instanceof LivingEntity livingEntity) {
            applyRainbowMagicDamage(level, livingEntity);
            depleteOxygenFast(livingEntity);
            applyVisualEffects(livingEntity);
        }
        super.entityInside(state, level, pos, entity);
    }
    private void depleteOxygenFast(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (player.isInWater()) {
                int currentOxygen = player.getAirSupply();
                int newOxygen = currentOxygen - 5;
                newOxygen = Math.max(newOxygen, -20);
                player.setAirSupply(newOxygen);
            }
        }
    }

    private void applyRainbowMagicDamage(Level level, LivingEntity entity) {
        entity.hurt(level.damageSources().magic(), 1);
        entity.invulnerableTime = -20;
    }
    private void applyVisualEffects(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
    }
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 5);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return;
        if (!level.isClientSide) {
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = pos.relative(direction);
                BlockState adjacentState = level.getBlockState(adjacentPos);

                if (canConvertBlock(adjacentState)) {
                    level.setBlockAndUpdate(adjacentPos, ModFluids.RAINBOW_FLOOD_BLOCK.get().defaultBlockState());

                    level.scheduleTick(adjacentPos, this, 5);
                }
            }

            level.scheduleTick(pos, this, 5);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (!level.isClientSide()) {
            level.scheduleTick(currentPos, this, 2);
        }
        return state;
    }

    private boolean canConvertBlock(BlockState state) {
        if (state.isAir() || state.getBlock() == this || state.getDestroySpeed(null, null) < 0 || !state.getFluidState().isEmpty() || !state.isSolid()){
            return false;
        }
        return true;
    }
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return false;
    }
}