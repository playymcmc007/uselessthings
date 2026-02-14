package com.playymcmc007.uselessthings.block;

import com.playymcmc007.uselessthings.world.StructureGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ArchitecturalFlowerBlock extends Block {
    private static final int GENERATION_DELAY = 200; // 10秒 (20 ticks/秒 * 10秒)
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    public ArchitecturalFlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            // 放置后安排结构生成
            level.scheduleTick(pos, this, GENERATION_DELAY);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide) {
            // 生成随机结构
            boolean success = StructureGenerator.generateRandomStructure(level, pos, random);

            // 无论生成是否成功，建筑花都会消失
            level.removeBlock(pos, false);

            if (!success) {
                // 生成失败时在建筑花位置产生爆炸
                StructureGenerator.createExplosion(level, pos);
            }
        }
    }

    // 让建筑花能够被随机tick，以便在自然生成时也能工作
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Blocks.CHORUS_FLOWER.getShape(state, level, pos, context);
    }
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }
}