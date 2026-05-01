package com.playymcmc007.uselessthings.world;

import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.Optional;
import java.util.OptionalLong;

public class ModDimensions {

    // 维度类型键
    public static final ResourceKey<DimensionType> NO_STONE_DIM_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE,
                    new ResourceLocation(UselessThings.MODID, "no_stone_dim_type"));

    // 维度键
    public static final ResourceKey<LevelStem> NO_STONE_DIMENSION =
            ResourceKey.create(Registries.LEVEL_STEM,
                    new ResourceLocation(UselessThings.MODID, "no_stone_dimension"));

    // 世界键（用于传送）
    public static final ResourceKey<net.minecraft.world.level.Level> NO_STONE_LEVEL =
            ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation(UselessThings.MODID, "no_stone_dimension"));

    /**
     * 注册维度类型
     */
    public static void bootstrapDimensionType(BootstapContext<DimensionType> context) {
        context.register(NO_STONE_DIM_TYPE, new DimensionType(
                OptionalLong.empty(), // fixedTime - 不固定时间
                true,                 // hasSkyLight - 有天空光照
                false,                // hasCeiling - 无天花板
                false,                // ultraWarm - 不是地狱
                true,                 // natural - 是自然维度
                1.0,                  // coordinateScale - 坐标缩放
                true,                 // bedWorks - 床可以工作
                false,                // respawnAnchorWorks - 重生锚不工作
                -64,                  // minY - 最小高度
                384,                  // height - 总高度
                384,                  // logicalHeight - 逻辑高度
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn - 无限燃烧标签
                BuiltinDimensionTypes.OVERWORLD_EFFECTS, // effectsLocation - 使用主世界的天空效果
                0.0f,                 // ambientLight - 环境光照
                new DimensionType.MonsterSettings(
                        false,        // piglinSafe - 猪灵不安全
                        true,         // hasRaids - 有袭击
                        UniformInt.of(0, 7), // monsterSpawnLightTest - 怪物生成光照测试
                        0              // monsterSpawnBlockLightLimit - 怪物生成方块光照限制
                )
        ));
    }

    /**
     * 注册维度
     */
    public static void bootstrapDimension(BootstapContext<LevelStem> context) {
        HolderGetter<Biome> biomeRegistry = context.lookup(Registries.BIOME);
        HolderGetter<NoiseGeneratorSettings> noiseSettingsRegistry = context.lookup(Registries.NOISE_SETTINGS);
        HolderGetter<DimensionType> dimensionTypeRegistry = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<MultiNoiseBiomeSourceParameterList> parameterListRegistry =
                context.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);

        // 获取我们注册的维度类型
        Holder<DimensionType> dimensionType = dimensionTypeRegistry.getOrThrow(NO_STONE_DIM_TYPE);

        // 获取原版的Overworld噪声设置
        Holder<NoiseGeneratorSettings> overworldNoiseSettings =
                noiseSettingsRegistry.getOrThrow(NoiseGeneratorSettings.OVERWORLD);

        // 创建生物群系源 - 使用主世界的参数列表
        Holder<MultiNoiseBiomeSourceParameterList> overworldParameterList;

        overworldParameterList = parameterListRegistry.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);


        // 从预设创建生物群系源
        BiomeSource biomeSource = MultiNoiseBiomeSource.createFromPreset(overworldParameterList);

        // 创建我们的无石头区块生成器
        NoStoneChunkGenerator chunkGenerator = new NoStoneChunkGenerator(biomeSource, overworldNoiseSettings);

        // 创建维度
        LevelStem dimension = new LevelStem(dimensionType, chunkGenerator);

        context.register(NO_STONE_DIMENSION, dimension);
    }
}