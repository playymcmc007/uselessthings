package com.playymcmc007.uselessthings.world.feature;

import com.mojang.serialization.Codec;
import com.playymcmc007.uselessthings.ModBlocks;
import com.playymcmc007.uselessthings.block.SuperSaplingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GiantTreeFeature extends Feature<NoneFeatureConfiguration> {

    // ========== 原木和树叶映射 ==========
    // 将超级树苗映射到对应的原木
    public static final Map<Block, BlockState> LOG_MAP = Map.of(
            ModBlocks.SUPER_OAK_SAPLING.get(), Blocks.OAK_LOG.defaultBlockState(),
            ModBlocks.SUPER_BIRCH_SAPLING.get(), Blocks.BIRCH_LOG.defaultBlockState(),
            ModBlocks.SUPER_SPRUCE_SAPLING.get(), Blocks.SPRUCE_LOG.defaultBlockState(),
            ModBlocks.SUPER_JUNGLE_SAPLING.get(), Blocks.JUNGLE_LOG.defaultBlockState(),
            ModBlocks.SUPER_ACACIA_SAPLING.get(), Blocks.ACACIA_LOG.defaultBlockState(),
            ModBlocks.SUPER_DARK_OAK_SAPLING.get(), Blocks.DARK_OAK_LOG.defaultBlockState(),
            ModBlocks.SUPER_MANGROVE_PROPAGULE.get(), Blocks.MANGROVE_LOG.defaultBlockState(),
            ModBlocks.SUPER_CHERRY_SAPLING.get(), Blocks.CHERRY_LOG.defaultBlockState()
    );

    // 将超级树苗映射到对应的树叶
    public static final Map<Block, BlockState> LEAVES_MAP = Map.of(
            ModBlocks.SUPER_OAK_SAPLING.get(), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
            ModBlocks.SUPER_BIRCH_SAPLING.get(), Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
            ModBlocks.SUPER_SPRUCE_SAPLING.get(), Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
            ModBlocks.SUPER_JUNGLE_SAPLING.get(), Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
            ModBlocks.SUPER_ACACIA_SAPLING.get(), Blocks.ACACIA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
            ModBlocks.SUPER_DARK_OAK_SAPLING.get(), Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
            ModBlocks.SUPER_MANGROVE_PROPAGULE.get(), Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
            ModBlocks.SUPER_CHERRY_SAPLING.get(), Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true)
    );

    // 记录生成的原木位置（用于后续树叶覆盖检查）
    private final Set<BlockPos> generatedLogs = new HashSet<>();

    // ========== 大树生成参数 ==========
    private static final int TRUNK_RADIUS = 40;       // 树干半径
    private static final int BRANCH_START = 50;       // 分叉起始高度（从树干底部往上50格）
    private static final int MIN_HEIGHT = 100;        // 树干最小高度
    private static final int HEIGHT_VARIATION = 50;   // 高度随机变化范围
    private static final int CANOPY_RADIUS = 500;     // 树冠半径
    private static final int CANOPY_HEIGHT = 50;      // 树冠厚度

    // 树干底部位置和实际高度
    private BlockPos basePos;
    private int treeActualHeight;

    // 树干半径缓存
    private final Map<Integer, Integer> trunkRadiusCache = new HashMap<>();

    // ========== 坐标旋转相关 ==========
    private boolean rotationActive = false;      // 是否启用旋转
    private Direction rotationDirection = Direction.UP;  // 旋转目标方向

    public GiantTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    // ========== 主要入口 ==========
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        // 清空上次生成的数据
        generatedLogs.clear();
        trunkRadiusCache.clear();

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rand = context.random();

        // 获取树苗方块
        Block sapling = level.getBlockState(origin).getBlock();
        if (!LOG_MAP.containsKey(sapling)) {
            return false;  // 不是超级树苗，不生成
        }

        BlockState log = LOG_MAP.get(sapling);
        BlockState leaves = LEAVES_MAP.get(sapling);

        // 获取树苗的朝向
        BlockState saplingState = level.getBlockState(context.origin());
        Direction facing = Direction.UP;
        if (saplingState.getBlock() instanceof SuperSaplingBlock) {
            facing = saplingState.getValue(SuperSaplingBlock.FACING);
        }
        // 根据朝向选择生成方式
        if (facing == Direction.UP) {
            // 正常竖着生成（朝上）
            generateWithMaterials(level, origin, log, leaves, rand);
        } else {
            // 横着或倒挂生成（启用坐标旋转）
            generateWithMaterialsRotated(level, origin, log, leaves, rand, facing);
        }
        return true;
    }

    // ========== 正常竖着生成 ==========
    public void generateWithMaterials(WorldGenLevel level, BlockPos pos,
                                      BlockState log, BlockState leaves,
                                      RandomSource random) {
        this.basePos = pos;
        this.treeActualHeight = MIN_HEIGHT + random.nextInt(HEIGHT_VARIATION);
        // 关闭旋转
        this.rotationActive = false;

        // 生成树干和树冠
        generateColossalTrunk(level, pos, treeActualHeight, log, leaves, random);
        generateUltimateCanopy(level, pos.above(treeActualHeight), leaves, random);
    }

    // ========== 旋转生成（用于非朝上的树苗） ==========
    private void generateWithMaterialsRotated(WorldGenLevel level, BlockPos pos,
                                              BlockState log, BlockState leaves,
                                              RandomSource random, Direction direction) {
        this.basePos = pos;
        this.treeActualHeight = MIN_HEIGHT + random.nextInt(HEIGHT_VARIATION);

        // 启用旋转，设置目标方向
        this.rotationActive = true;
        this.rotationDirection = direction;

        // 生成树干和树冠（内部调用 safeSetBlock 时会自动旋转坐标）
        generateColossalTrunk(level, pos, treeActualHeight, log, leaves, random);
        generateUltimateCanopy(level, pos.above(treeActualHeight), leaves, random);

        // 关闭旋转
        this.rotationActive = false;
    }

    // ========== 树干生成 ==========
    private void generateColossalTrunk(WorldGenLevel level, BlockPos base, int height,
                                       BlockState log, BlockState leaves, RandomSource rand) {
        for (int y = 0; y < height; y++) {
            int radius = calculateTrunkRadiusForTrunk(y);

            // 生成实心圆柱形树干
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + z*z <= radius*radius) {
                        safeSetBlock(level, base.offset(x, y, z), log);
                    }
                }
            }

            // 在分叉高度生成树枝
            if (y >= BRANCH_START && y % 2 == 0) {
                generatePrimaryBranches(level, base.above(y), radius + 1, log, leaves, rand);
            }
        }
    }

    // ========== 主分支生成 ==========
    private void generatePrimaryBranches(WorldGenLevel level, BlockPos startPos, int startRadius,
                                         BlockState log, BlockState leaves, RandomSource rand) {
        int branchCount = 40 + rand.nextInt(40);

        for (int i = 0; i < branchCount; i++) {
            double angle = 2 * Math.PI * (i + rand.nextFloat() * 0.7f) / branchCount;
            double xDir = Math.cos(angle);
            double zDir = Math.sin(angle);

            int length = 40 + rand.nextInt(40);
            float upwardSlope = 0.1f + rand.nextFloat() * 0.3f;

            BlockPos current = startPos;
            for (int l = 0; l < length; l++) {
                // 动态弯曲效果
                xDir += (rand.nextFloat() - 0.5f) * 0.2f;
                zDir += (rand.nextFloat() - 0.5f) * 0.2f;
                upwardSlope = Math.max(0, upwardSlope + (rand.nextFloat() - 0.5f) * 0.15f);

                current = current.offset(
                        (int)Math.round(xDir),
                        (int)Math.round(upwardSlope),
                        (int)Math.round(zDir)
                );

                safeSetBlock(level, current, log);

                // 每4格生成次级分支
                if (l > 3 && l % 4 == 0) {
                    generateSecondaryBranches(level, current, log, leaves, rand, 4 + rand.nextInt(4));
                }

                // 每个节点生成树叶球体
                generateLeafSphere(level, current, log, leaves, rand, 3 + rand.nextInt(3));
            }
        }
    }

    // ========== 次级分支生成 ==========
    private void generateSecondaryBranches(WorldGenLevel level, BlockPos start,
                                           BlockState log, BlockState leaves,
                                           RandomSource rand, int count) {
        for (int i = 0; i < count; i++) {
            float horizontalAngle = rand.nextFloat() * (float)Math.PI * 2;
            float verticalAngle = rand.nextFloat() * 0.4f - 0.2f;

            int length = 20 + rand.nextInt(20);
            BlockPos current = start;

            for (int l = 0; l < length; l++) {
                current = current.offset(
                        (int)Math.round(Math.cos(horizontalAngle) * (1 + rand.nextFloat() * 0.2)),
                        (int)Math.round(verticalAngle),
                        (int)Math.round(Math.sin(horizontalAngle) * (1 + rand.nextFloat() * 0.2))
                );

                safeSetBlock(level, current, log);
                generateLeafSphere(level, current, log, leaves, rand, 2 + rand.nextInt(2));

                // 60%几率生成三级分支
                if (l > 2 && rand.nextFloat() > 0.4f) {
                    generateTertiaryBranches(level, current, log, leaves, rand);
                }
            }
        }
    }

    // ========== 三级分支生成 ==========
    private void generateTertiaryBranches(WorldGenLevel level, BlockPos start,
                                          BlockState log, BlockState leaves,
                                          RandomSource rand) {
        int count = 10 + rand.nextInt(10);
        for (int i = 0; i < count; i++) {
            int length = 3 + rand.nextInt(4);
            BlockPos current = start;

            for (int l = 0; l < length; l++) {
                current = current.relative(Direction.getRandom(rand))
                        .above(rand.nextInt(2) - rand.nextInt(1));
                safeSetBlock(level, current, log);
                generateLeafSphere(level, current, log, leaves, rand, 1 + rand.nextInt(2));
            }
        }
    }

    // ========== 树冠生成 ==========
    private void generateUltimateCanopy(WorldGenLevel level, BlockPos canopyTop,
                                        BlockState leaves, RandomSource rand) {
        // 1. 顶部小圆帽
        generateCanopyTier(level, canopyTop,
                (int)(CANOPY_RADIUS * 0.25),
                (int)(CANOPY_HEIGHT * 0.15),
                0.1f, leaves, rand);

        // 2. 上过渡层
        generateCanopyTier(level, canopyTop.below((int)(CANOPY_HEIGHT * 0.15)),
                (int)(CANOPY_RADIUS * 0.55),
                (int)(CANOPY_HEIGHT * 0.2),
                0.2f, leaves, rand);

        // 3. 主冠层
        generateCanopyTier(level, canopyTop.below((int)(CANOPY_HEIGHT * 0.35)),
                (int)(CANOPY_RADIUS * 0.85),
                (int)(CANOPY_HEIGHT * 0.3),
                0.05f, leaves, rand);

        // 4. 下过渡层
        generateCanopyTier(level, canopyTop.below((int)(CANOPY_HEIGHT * 0.65)),
                (int)(CANOPY_RADIUS * 0.65),
                (int)(CANOPY_HEIGHT * 0.2),
                0.3f, leaves, rand);

        // 5. 底部边缘层
        generateCanopyTier(level, canopyTop.below((int)(CANOPY_HEIGHT * 0.85)),
                (int)(CANOPY_RADIUS * 0.4),
                (int)(CANOPY_HEIGHT * 0.15),
                0.6f, leaves, rand);
    }

    // ========== 单层树冠生成 ==========
    private void generateCanopyTier(WorldGenLevel level, BlockPos center, int maxRadius,
                                    int height, float droopFactor, BlockState leaves, RandomSource rand) {
        for (int layer = 0; layer < height; layer++) {
            float progress = (float)layer / height;
            int radius = (int)(maxRadius * (1 - progress * droopFactor));

            int currentTrunkRadius = getCachedTrunkRadius(center.getY() - layer);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x*x + z*z);
                    BlockPos pos = center.offset(x, -layer, z);
                    // 只在树干半径之外放置树叶
                    if (distance <= radius && distance > currentTrunkRadius) {
                        safeSetBlock(level, pos, leaves);

                        // 边缘下垂效果
                        if (distance > radius * 0.7) {
                            generateDanglingLeaves(level, pos, leaves, rand, 2 + rand.nextInt(4));
                        }
                    }
                }
            }
        }
    }

    // ========== 树叶球体生成 ==========
    private void generateLeafSphere(WorldGenLevel level, BlockPos center,
                                    BlockState log, BlockState leaves, RandomSource rand, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    if (distance <= radius && !level.getBlockState(pos).is(log.getBlock())) {
                        safeSetBlock(level, pos, leaves);
                    }
                }
            }
        }
    }

    // ========== 下垂树叶生成 ==========
    private void generateDanglingLeaves(WorldGenLevel level, BlockPos start,
                                        BlockState leaves, RandomSource rand, int length) {
        BlockPos current = start;
        for (int i = 0; i < length; i++) {
            current = current.below();
            if (level.getBlockState(current).isAir()) {
                safeSetBlock(level, current, leaves);
            } else {
                break;
            }
        }
    }

    // ========== 树干半径计算 ==========
    private int calculateTrunkRadiusForTrunk(int relativeY) {
        if (relativeY < 0 || relativeY > treeActualHeight) {
            return 0;
        }
        if (relativeY <= treeActualHeight * 0.8f) {
            return TRUNK_RADIUS;
        }
        int growthSteps = (relativeY - (int)(treeActualHeight * 0.8f)) / 2;
        return TRUNK_RADIUS + growthSteps;
    }

    // 用于树冠生成（传入世界坐标，需要缓存）
    private int calculateTrunkRadiusForCanopy(int worldY) {
        int relativeY = worldY - basePos.getY();
        if (relativeY < 0 || relativeY > treeActualHeight) {
            return 0;
        }
        if (relativeY <= treeActualHeight * 0.8f) {
            return TRUNK_RADIUS;
        }
        int growthSteps = (relativeY - (int)(treeActualHeight * 0.8f)) / 2;
        return TRUNK_RADIUS + growthSteps;
    }

    private int getCachedTrunkRadius(int worldY) {
        return trunkRadiusCache.computeIfAbsent(worldY,
                k -> calculateTrunkRadiusForCanopy(k));
    }

    // ========== 坐标旋转核心方法 ==========
    private BlockPos rotatePosition(BlockPos pos, Direction direction) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        switch (direction) {
            case NORTH:  // 朝北：原来的 Y 变成 Z（向前），Z 变成 Y（向上）
                return new BlockPos(x, z, -y);
            case SOUTH:  // 朝南
                return new BlockPos(x, -z, y);
            case WEST:   // 朝西
                return new BlockPos(-y, z, x);
            case EAST:   // 朝东
                return new BlockPos(y, z, -x);
            case DOWN:   // 朝下：Y轴翻转
                return new BlockPos(x, -y, z);
            default:     // UP 或其他
                return pos;
        }
    }

    // ========== 安全放置方块（带坐标旋转） ==========
    private void safeSetBlock(WorldGenLevel level, BlockPos pos, BlockState newState) {
        BlockPos finalPos = pos;
        BlockState finalState = newState;

        // 如果启用了旋转，对坐标进行变换
        if (rotationActive && rotationDirection != Direction.UP) {
            // 计算相对于 basePos 的偏移
            BlockPos relative = pos.subtract(basePos);
            // 旋转偏移
            BlockPos rotatedRelative = rotatePosition(relative, rotationDirection);
            // 计算新的世界坐标
            finalPos = basePos.offset(rotatedRelative);

            // 如果是原木，需要旋转轴向
            if (newState.getBlock() instanceof RotatedPillarBlock) {
                Direction.Axis originalAxis = newState.getValue(RotatedPillarBlock.AXIS);
                Direction.Axis newAxis = rotateAxis(originalAxis, rotationDirection);
                finalState = newState.setValue(RotatedPillarBlock.AXIS, newAxis);
            }
        }

        // 检查是否超出世界高度
        if (level.isOutsideBuildHeight(finalPos)) {
            return;
        }

        // 检查是否可以放置
        BlockState currentState = level.getBlockState(finalPos);
        boolean isTargetSapling = finalPos.equals(basePos) && currentState.getBlock() instanceof SuperSaplingBlock;
        if (currentState.isAir() || currentState.canBeReplaced() || isTargetSapling) {
            level.setBlock(finalPos, finalState, 2);
            // 记录原木位置
            if (finalState.getBlock() instanceof RotatedPillarBlock) {
                generatedLogs.add(finalPos);
            }
        }
    }
    private Direction.Axis rotateAxis(Direction.Axis axis, Direction direction) {
        switch (direction) {
            case UP:
            case DOWN:
                // 上下
                return axis;

            case NORTH:
            case SOUTH:
                // 南北
                if (axis == Direction.Axis.Y) return Direction.Axis.Z;
                if (axis == Direction.Axis.Z) return Direction.Axis.Y;
                return axis;

            case WEST:
            case EAST:
                // 东西
                if (axis == Direction.Axis.Y) return Direction.Axis.X;
                if (axis == Direction.Axis.X) return Direction.Axis.Y;
                return axis;

            default:
                return axis;
        }
    }
}