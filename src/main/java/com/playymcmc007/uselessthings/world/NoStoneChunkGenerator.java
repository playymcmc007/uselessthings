package com.playymcmc007.uselessthings.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class NoStoneChunkGenerator extends NoiseBasedChunkGenerator {

    private final Holder<NoiseGeneratorSettings> settings;

    public static final Codec<NoStoneChunkGenerator> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(NoStoneChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.settings)
            ).apply(instance, instance.stable(NoStoneChunkGenerator::new))
    );

    public NoStoneChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
        this.settings = settings;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunk) {
        return super.fillFromNoise(executor, blender, random, structureManager, chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random,
                             net.minecraft.world.level.biome.BiomeManager biomeManager,
                             StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving carver) {
        super.applyCarvers(level, seed, random, biomeManager, structureManager, chunk, carver);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        super.buildSurface(level, structureManager, random, chunk);

        // 地表生成后替换石头
        replaceAllStoneWithDirt(chunk);
    }

    @Override
    public void createStructures(RegistryAccess registryAccess,
                                 ChunkGeneratorStructureState generatorState,
                                 StructureManager structureManager,
                                 ChunkAccess chunk,
                                 StructureTemplateManager templateManager) {
        // 先让原版生成结构
        super.createStructures(registryAccess, generatorState, structureManager, chunk, templateManager);

        // 结构生成后替换石头
        replaceAllStoneWithDirt(chunk);
    }

    @Override
    public void createReferences(WorldGenLevel level, StructureManager structureManager, ChunkAccess chunk) {
        super.createReferences(level, structureManager, chunk);

        // 结构引用生成后也检查一下
        replaceAllStoneWithDirt(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);

        // 地物和结构装饰后替换石头
        replaceAllStoneWithDirt(chunk);
    }

    private void replaceAllStoneWithDirt(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                        chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
                    }
                }
            }
        }
    }
}