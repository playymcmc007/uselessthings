package com.playymcmc007.uselessthings.block;

import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Supplier;

public class PlantStaffCache {
    private static PlantStaffCache instance;

    public static class Tags {
        public static final TagKey<Block> FLOWERS = BlockTags.create(new ResourceLocation("minecraft", "flowers"));
        public static final TagKey<Block> REPLACEABLE = BlockTags.create(new ResourceLocation("minecraft", "replaceable"));
        public static final TagKey<Block> SAPLINGS = BlockTags.create(new ResourceLocation("minecraft", "saplings"));
        public static final TagKey<Block> LEAVES = BlockTags.create(new ResourceLocation("minecraft", "leaves"));
        public static final TagKey<Block> TALL_FLOWERS = BlockTags.create(new ResourceLocation("minecraft", "tall_flowers"));
    }
    //仙人掌花也是花
    private boolean isCactusRelated(String path) {
        return path.contains("cactus") || path.contains("cacti");
    }
    //湮灭玫瑰（末影花），单独处理
    private static final Set<ResourceLocation> EXCLUDED_FLOWERS = Set.of(
            new ResourceLocation("naturesaura", "end_flower")
    );
    private final Int2IntOpenHashMap radiusBaseHeights = new Int2IntOpenHashMap(16);

    private final Map<Integer, List<BlockPos>> circlePositionsCache = new HashMap<>();

    // 过滤后的花朵列表
    private final Supplier<List<Block>> filteredFlowers;

    // 植物方块集合
    private final Set<Block> plantableBlocksSet;

    private PlantStaffCache() {
        this.filteredFlowers = Suppliers.memoize(this::initFilteredFlowers);
        this.plantableBlocksSet = initPlantableBlocks();
    }

    public static PlantStaffCache getInstance() {
        if (instance == null) {
            instance = new PlantStaffCache();
        }
        return instance;
    }

    public static void init() {
        getInstance();
    }

    //  可种植方块集合
    private Set<Block> initPlantableBlocks() {
        Set<Block> plantableBlocks = new HashSet<>(Arrays.asList(
                Blocks.GRASS_BLOCK,
                Blocks.MYCELIUM,
                Blocks.PODZOL,
                Blocks.DIRT,
                Blocks.COARSE_DIRT,
                Blocks.ROOTED_DIRT,
                Blocks.FARMLAND,
                Blocks.MUD,
                Blocks.MUDDY_MANGROVE_ROOTS
        ));
        return Collections.unmodifiableSet(plantableBlocks);
    }
    // 过滤花朵列表
    private List<Block> initFilteredFlowers() {
        List<Block> flowers = new ArrayList<>();
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
            if (blockId != null && EXCLUDED_FLOWERS.contains(blockId)) {
                continue;
            }
            String path = blockId.getPath().toLowerCase();

            if (isCactusRelated(path)) {
                continue;
            }
            if (block.builtInRegistryHolder().is(Tags.FLOWERS) &&
                    !block.builtInRegistryHolder().is(Tags.SAPLINGS) &&
                    !block.builtInRegistryHolder().is(Tags.LEAVES) &&
                    !isWaterRelatedOrSpecialPlant(block)) {
                flowers.add(block);
            }
        }
        return Collections.unmodifiableList(flowers);
    }

    // 检查是否为水相关，如水稻
    private boolean isWaterRelatedOrSpecialPlant(Block block) {
        BlockState defaultState = block.defaultBlockState();
        if (defaultState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return defaultState.getValue(BlockStateProperties.WATERLOGGED);
        }
        return false;
    }

    public Integer getRadiusBaseHeight(int radius) {
        synchronized (radiusBaseHeights) {
            return radiusBaseHeights.containsKey(radius) ?
                    radiusBaseHeights.get(radius) : null;
        }
    }

    public int getRadiusBaseHeightOrDefault(int radius, int defaultValue) {
        synchronized (radiusBaseHeights) {
            return radiusBaseHeights.getOrDefault(radius, defaultValue);
        }
    }

    public void setRadiusBaseHeight(int radius, int height) {
        synchronized (radiusBaseHeights) {
            radiusBaseHeights.put(radius, height);
        }
    }

    public List<BlockPos> getCirclePositions(int radius) {
        synchronized (circlePositionsCache) {
            return circlePositionsCache.computeIfAbsent(radius, r -> {
                List<BlockPos> positions = new ArrayList<>();
                int rSquared = r * r;

                for (int x = -r; x <= r; x++) {
                    int maxZ = (int) Math.sqrt(rSquared - x * x);
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double distance = Math.sqrt(x * x + z * z);
                        if (distance <= r && distance > r - 1) {
                            positions.add(new BlockPos(x, 0, z));
                        }
                    }
                }
                return Collections.unmodifiableList(positions);
            });
        }
    }

    // 获取过滤后的花朵列表
    public List<Block> getFilteredFlowers() {
        return filteredFlowers.get();
    }

    // 检查是否是可种植方块
    public boolean isPlantableBlock(BlockState state) {
        return plantableBlocksSet.contains(state.getBlock());
    }

    // 清理缓存
    public void clearCache() {
        synchronized (radiusBaseHeights) {
            radiusBaseHeights.clear();
        }
        synchronized (circlePositionsCache) {
            circlePositionsCache.clear();
        }
    }
}