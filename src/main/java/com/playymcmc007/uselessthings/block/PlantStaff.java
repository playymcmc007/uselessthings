package com.playymcmc007.uselessthings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class PlantStaff extends BushBlock {
    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    public static final IntegerProperty PROGRESS = IntegerProperty.create("progress", 0, 32);
    public static final BooleanProperty IS_UPPER = BooleanProperty.create("is_upper");
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    // 缓存实例
    private static PlantStaffCache cache;

    public PlantStaff(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ACTIVATED, false)
                .setValue(PROGRESS, 0)
                .setValue(IS_UPPER, false));

        // 确保缓存已初始化
        if (cache == null) {
            cache = PlantStaffCache.getInstance();
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED, PROGRESS, IS_UPPER);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!state.getValue(IS_UPPER) && !level.isClientSide) {
            BlockPos abovePos = pos.above();
            if (level.getBlockState(abovePos).canBeReplaced()) {
                // 同时设置上下部分的状态
                level.setBlock(pos, state.setValue(ACTIVATED, true), 3);
                level.setBlock(abovePos,
                        this.defaultBlockState()
                                .setValue(IS_UPPER, true)
                                .setValue(ACTIVATED, true),
                        3);

                // 检查底部方块是否可种植
                if (cache.isPlantableBlock(level.getBlockState(pos.below()))) {
                    level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                            SoundSource.BLOCKS, 2.0F, 1.0F);
                    level.scheduleTick(pos, this, 15);
                }
            }
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(IS_UPPER)) {
            BlockState belowState = level.getBlockState(pos.below());
            return belowState.is(this) && !belowState.getValue(IS_UPPER);
        } else {
            return cache.isPlantableBlock(level.getBlockState(pos.below()));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!state.getValue(IS_UPPER)) {
                BlockPos abovePos = pos.above();
                if (level.getBlockState(abovePos).is(this)) {
                    level.destroyBlock(abovePos, true);
                }
            } else {
                BlockPos belowPos = pos.below();
                if (level.getBlockState(belowPos).is(this)) {
                    level.destroyBlock(belowPos, true);
                }
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        if (!state.getValue(IS_UPPER) && state.getValue(ACTIVATED)) {
            int currentProgress = state.getValue(PROGRESS);

            if (currentProgress < 32) {
                convertBlocksInRadius(level, pos, currentProgress + 1, random);

                BlockState newState = state.setValue(PROGRESS, currentProgress + 1);
                level.setBlock(pos, newState, 3);

                BlockPos abovePos = pos.above();
                BlockState aboveState = level.getBlockState(abovePos);
                if (aboveState.is(this)) {
                    level.setBlock(abovePos, aboveState.setValue(PROGRESS, currentProgress + 1), 3);
                    float volume = 1.5F + (currentProgress * 0.02F);
                    float pitch = 0.8F + (currentProgress * 0.01F);
                    level.playSound(null, pos, SoundEvents.BONE_MEAL_USE,
                            SoundSource.BLOCKS, volume, pitch);
                }

                level.scheduleTick(pos, this, 15);
            } else {
                // 转化完成，变成枯萎的灌木
                level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), 3);
                BlockPos abovePos = pos.above();
                if (level.getBlockState(abovePos).is(this)) {
                    level.destroyBlock(abovePos, false);
                }

                // 完成音效
                level.playSound(null, pos, SoundEvents.GRASS_BREAK,
                        SoundSource.BLOCKS, 3.0F, 0.8F);

                // 清除高度缓存
                cache.clearCache();
            }
        }
    }

    // 优化：预计算圆形位置，避免重复计算
    private List<BlockPos> getCirclePositions(int radius) {
        return cache.getCirclePositions(radius);
    }

    // 优化：改进的高度计算算法
    private int getBaseHeightForRadius(ServerLevel level, BlockPos staffBase, int radius, RandomSource random) {
        // 检查缓存
        Integer cachedHeight = cache.getRadiusBaseHeight(radius);
        if (cachedHeight != null) {
            return cachedHeight;
        }

        // 如果第一圈，直接使用法杖高度
        if (radius == 1) {
            int height = staffBase.getY();
            cache.setRadiusBaseHeight(radius, height);
            return height;
        }

        // 否则采样地形高度
        int sampledHeight = sampleTerrainHeight(level, staffBase, radius, random);

        // 应用平滑过渡
        int previousHeight = cache.getRadiusBaseHeightOrDefault(radius - 1, staffBase.getY());

        // 使用加权平均保持地形连续性
        int smoothedHeight = (previousHeight * 3 + sampledHeight) / 4;

        cache.setRadiusBaseHeight(radius, smoothedHeight);

        return smoothedHeight;
    }

    // 优化：高效地形采样方法
    private int sampleTerrainHeight(ServerLevel level, BlockPos staffBase, int radius, RandomSource random) {
        // 使用更高效的采样策略 - 只采样关键点
        int samples = Math.min(radius * 2, 16); // 限制采样数量
        int totalHeight = 0;
        int validSamples = 0;

        for (int i = 0; i < samples; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int x = (int) (Math.cos(angle) * radius);
            int z = (int) (Math.sin(angle) * radius);

            BlockPos samplePos = new BlockPos(
                    staffBase.getX() + x,
                    staffBase.getY(),
                    staffBase.getZ() + z
            );

            // 快速查找可种植高度
            int height = findPlantableHeight(level, samplePos);
            if (height != Integer.MIN_VALUE) {
                totalHeight += height;
                validSamples++;
            }
        }

        return validSamples > 0 ? totalHeight / validSamples : staffBase.getY();
    }

    // 优化：使用二分查找加速高度查找
    private int findPlantableHeight(ServerLevel level, BlockPos center) {
        // 向上搜索
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos checkPos = center.above(dy);
            BlockPos belowPos = checkPos.below();

            if (canPlantFlowerHereFast(level.getBlockState(checkPos), level.getBlockState(belowPos))) {
                return checkPos.getY();
            }
        }

        // 向下搜索
        for (int dy = -1; dy >= -8; dy--) {
            BlockPos checkPos = center.above(dy);
            BlockPos belowPos = checkPos.below();

            if (canPlantFlowerHereFast(level.getBlockState(checkPos), level.getBlockState(belowPos))) {
                return checkPos.getY();
            }
        }

        return Integer.MIN_VALUE; // 未找到
    }

    // 优化：快速检查方法
    private boolean canPlantFlowerHereFast(BlockState currentState, BlockState belowState) {
        // 快速路径检查
        if (!currentState.isAir() &&
                !currentState.canBeReplaced() &&
                !currentState.is(PlantStaffCache.Tags.REPLACEABLE)) {
            return false;
        }
        if (currentState.getFluidState().isSource() ||
                currentState.getFluidState().getAmount() > 0) {
            return false; // 如果是流体，不能种花
        }
        return cache.isPlantableBlock(belowState);
    }

    // 优化：批量处理位置转换
    private void convertBlocksInRadius(ServerLevel level, BlockPos staffBase, int radius, RandomSource random) {
        // 获取基准高度
        int baseHeight = getBaseHeightForRadius(level, staffBase, radius, random);

        // 获取预计算的圆环位置
        List<BlockPos> circleOffsets = getCirclePositions(radius);
        List<Integer> foundHeights = new ArrayList<>(circleOffsets.size());

        // 批量处理位置
        for (BlockPos offset : circleOffsets) {
            BlockPos centerPos = new BlockPos(
                    staffBase.getX() + offset.getX(),
                    baseHeight,
                    staffBase.getZ() + offset.getZ()
            );

            int height = findPlantableHeight(level, centerPos);
            if (height != Integer.MIN_VALUE) {
                BlockPos flowerPos = new BlockPos(
                        centerPos.getX(),
                        height,
                        centerPos.getZ()
                );
                placeRandomFlower(level, flowerPos, random);
                foundHeights.add(height);
            }
        }

        // 计算下一圈的基准高度
        if (!foundHeights.isEmpty()) {
            int sum = 0;
            for (int height : foundHeights) {
                sum += height;
            }
            int averageHeight = sum / foundHeights.size();
            cache.setRadiusBaseHeight(radius + 1, averageHeight);
        } else {
            // 如果没有找到合适的位置，使用当前基准高度
            cache.setRadiusBaseHeight(radius + 1, baseHeight);
        }

        // 添加粒子效果
        if (radius % 4 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double px = staffBase.getX() + Math.cos(angle) * radius;
                double pz = staffBase.getZ() + Math.sin(angle) * radius;
                double py = baseHeight + 0.5 + random.nextDouble();

                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        px, py, pz, 1, 0, 0.05, 0, 0.1);
            }
        }
    }

    // 优化：改进的随机花朵放置方法
    private void placeRandomFlower(ServerLevel level, BlockPos flowerPos, RandomSource random) {
        // 从过滤后的花朵列表中随机选择
        List<Block> filteredFlowers = cache.getFilteredFlowers();
        if (filteredFlowers.isEmpty()) return;

        Block randomFlower = filteredFlowers.get(random.nextInt(filteredFlowers.size()));

        // 检查是否高花
        if (isTallFlowerFast(randomFlower)) {
            BlockPos abovePos = flowerPos.above();
            BlockState aboveState = level.getBlockState(abovePos);

            if (aboveState.isAir() || aboveState.canBeReplaced()) {
                level.setBlock(flowerPos, randomFlower.defaultBlockState(), 3);

                if (randomFlower instanceof DoublePlantBlock) {
                    DoublePlantBlock.placeAt(level, randomFlower.defaultBlockState(), flowerPos, 3);
                } else {
                    level.setBlock(abovePos, randomFlower.defaultBlockState(), 3);
                }

                addParticles(level, flowerPos, random);
            } else {
                // 尝试寻找非高花
                Block alternativeFlower = findNonTallFlower(filteredFlowers, random);
                level.setBlock(flowerPos, alternativeFlower.defaultBlockState(), 3);
                addParticles(level, flowerPos, random);
            }
        } else {
            // 普通花，直接放置
            level.setBlock(flowerPos, randomFlower.defaultBlockState(), 3);
            addParticles(level, flowerPos, random);
        }
    }

    private Block findNonTallFlower(List<Block> flowers, RandomSource random) {
        for (int i = 0; i < 10; i++) {
            Block flower = flowers.get(random.nextInt(flowers.size()));
            if (!isTallFlowerFast(flower)) {
                return flower;
            }
        }
        // 如果找不到非高花，返回第一个花朵
        return flowers.get(0);
    }

    private void addParticles(ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
            double py = pos.getY() + 0.2 + random.nextDouble() * 0.3;
            double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;

            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    px, py, pz, 1, 0, 0, 0, 0.05);
        }
    }

    // 优化：快速高花检查（简化为标签检查）
    private boolean isTallFlowerFast(Block flower) {
        // 先检查是否是 DoublePlantBlock 或 TallGrassBlock 类型（这些通常是高花）
        if (flower instanceof DoublePlantBlock || flower instanceof TallGrassBlock) {
            return true;
        }

        // 最后检查标签
        return flower.builtInRegistryHolder().is(PlantStaffCache.Tags.TALL_FLOWERS);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter reader, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(IS_UPPER) && state.getValue(ACTIVATED)) {
            int progress = state.getValue(PROGRESS);
            if (progress > 0) {
                // 粒子效果
                for (int i = 0; i < Math.min(progress / 4, 5); i++) {
                    double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
                    double py = pos.getY() + random.nextDouble() * 1.5;
                    double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;

                    if (progress < 10) {
                        level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                                px, py, pz, 0, 0.01, 0);
                    } else if (progress < 20) {
                        level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                                px, py, pz, 0, 0.01, 0);
                    } else {
                        level.addParticle(net.minecraft.core.particles.ParticleTypes.GLOW,
                                px, py, pz, 0, 0.01, 0);
                    }
                }
            }
        }
    }
}