package com.playymcmc007.uselessthings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;

public class InvisibleLightBlock extends Block {

    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
    private static final int LIFETIME_TICKS = 4; // 只存在4tick，持续更新

    public InvisibleLightBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .replaceable()
                .noCollission()
                .instabreak()
                .air()
                .strength(-1.0F, 3600000.0F)
                .noOcclusion()
                .isValidSpawn((a, b, c, d) -> false)
                .isRedstoneConductor((a, b, c) -> false)
                .isSuffocating((a, b, c) -> false)
                .isViewBlocking((a, b, c) -> false)
                .lightLevel(state -> 15) // 最大光照等级
                .pushReaction(PushReaction.BLOCK)
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(LEVEL, 0);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // 光源本身也会散发微妙粒子
        if (random.nextFloat() < 0.3F) {
            level.addParticle(
                    ParticleTypes.GLOW,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                    pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                    0, 0.01, 0
            );
        }
    }
}