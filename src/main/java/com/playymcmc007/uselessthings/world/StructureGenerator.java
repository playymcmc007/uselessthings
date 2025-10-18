package com.playymcmc007.uselessthings.world;

import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StructureGenerator {
    private static List<ResourceLocation> availableTemplates = null;
    private static long lastCheckTime = 0;
    private static final ConcurrentMap<ResourceLocation, StructureTemplate> preloadedTemplates = new ConcurrentHashMap<>();
    private static boolean isPreloaded = false;

    private static final List<ResourceLocation> FALLBACK_TEMPLATES = List.of(
            new ResourceLocation("minecraft:village/plains/town_centers"),
            new ResourceLocation("minecraft:village/desert/town_centers"),
            new ResourceLocation("minecraft:village/savanna/town_centers"),
            new ResourceLocation("minecraft:village/snowy/town_centers"),
            new ResourceLocation("minecraft:village/taiga/town_centers"),
            new ResourceLocation("minecraft:pillager_outpost/feature_plates"),
            new ResourceLocation("minecraft:mansion"),
            new ResourceLocation("minecraft:monument"),
            new ResourceLocation("minecraft:fortress/bridge"),
            new ResourceLocation("minecraft:endcity/city_center")
    );

    public static void preloadTemplates(ServerLevel level) {
        if (isPreloaded) return;

        level.getServer().execute(() -> {
            try {
                StructureTemplateManager templateManager = level.getStructureManager();
                for (ResourceLocation templateId : templateManager.listTemplates().toList()) {
                    templateManager.get(templateId).ifPresent(template -> {
                        preloadedTemplates.put(templateId, template);
                    });
                }
                isPreloaded = true;
                availableTemplates = new ArrayList<>(preloadedTemplates.keySet());
            } catch (Exception e) {
                UselessThings.LOGGER.error("preloadTemplates发生错误", e);
            }
        });
    }

    public static boolean generateRandomStructure(ServerLevel level, BlockPos pos, RandomSource random) {
        try {
            // 如果还没预加载，使用备用方案
            if (!isPreloaded) {
                ResourceLocation templateId = FALLBACK_TEMPLATES.get(random.nextInt(FALLBACK_TEMPLATES.size()));
                level.getServer().execute(() -> {
                    boolean success = placeTemplateWithFallback(level, templateId, pos, random);
                    if (!success) {
                        createExplosion(level, pos);
                    }
                });
                return true;
            }

            if (availableTemplates == null || availableTemplates.isEmpty()) {
                availableTemplates = new ArrayList<>(preloadedTemplates.keySet());
            }

            if (level.getGameTime() - lastCheckTime > 600) {
                refreshTemplatesList(level);
                lastCheckTime = level.getGameTime();
            }

            ResourceLocation templateId;
            if (availableTemplates.isEmpty()) {
                templateId = FALLBACK_TEMPLATES.get(random.nextInt(FALLBACK_TEMPLATES.size()));
            } else {
                templateId = availableTemplates.get(random.nextInt(availableTemplates.size()));
            }

            // 使用预加载的模板
            StructureTemplate template = preloadedTemplates.get(templateId);
            if (template != null) {
                boolean success = placeTemplate(level, template, pos, random);
                if (!success) {
                    createExplosion(level, pos);
                }
                return success;
            } else {
                // 如果预加载的模板不存在，使用备用方案
                boolean success = placeTemplateWithFallback(level, templateId, pos, random);
                if (!success) {
                    createExplosion(level, pos);
                }
                return success;
            }

        } catch (Exception e) {
            createExplosion(level, pos);
            UselessThings.LOGGER.error("generateRandomStructure发生错误", e);
            return false;
        }
    }

    private static void refreshTemplatesList(ServerLevel level) {
        level.getServer().execute(() -> {
            try {
                StructureTemplateManager templateManager = level.getStructureManager();
                List<ResourceLocation> newTemplates = new ArrayList<>();

                for (ResourceLocation templateId : templateManager.listTemplates().toList()) {
                    if (preloadedTemplates.containsKey(templateId) || templateManager.get(templateId).isPresent()) {
                        newTemplates.add(templateId);
                    }
                }

                availableTemplates = newTemplates;
            } catch (Exception e) {
                UselessThings.LOGGER.error("refreshTemplatesList发生错误", e);
            }
        });
    }

    private static boolean placeTemplate(ServerLevel level, StructureTemplate template, BlockPos pos, RandomSource random) {
        try {
            Vec3i size = template.getSize();

            if (!areChunksLoaded(level, pos, size)) {
                return false;
            }

            BlockPos placementPos = pos.offset(-size.getX() / 2, 0, -size.getZ() / 2);

            StructurePlaceSettings placementSettings = new StructurePlaceSettings()
                    .setRandom(random)
                    .setIgnoreEntities(true)
                    .setKeepLiquids(false);

            boolean success = template.placeInWorld(level, placementPos, placementPos,
                    placementSettings, random, 2);

            if (success) {
                cleanUpJigsawBlocksOptimized(level, placementPos, size);
            }

            return success;

        } catch (Exception e) {
            UselessThings.LOGGER.error("placeTemplate发生错误", e);
            return false;
        }
    }

    private static boolean placeTemplateWithFallback(ServerLevel level, ResourceLocation templateId, BlockPos pos, RandomSource random) {
        try {
            StructureTemplateManager templateManager = level.getStructureManager();
            Optional<StructureTemplate> templateOptional = templateManager.get(templateId);

            if (templateOptional.isEmpty()) {
                return false;
            }

            StructureTemplate template = templateOptional.get();
            Vec3i size = template.getSize();

            if (!areChunksLoaded(level, pos, size)) {
                return false;
            }

            BlockPos placementPos = pos.offset(-size.getX() / 2, 0, -size.getZ() / 2);

            StructurePlaceSettings placementSettings = new StructurePlaceSettings()
                    .setRandom(random)
                    .setIgnoreEntities(true)
                    .setKeepLiquids(false);

            boolean success = template.placeInWorld(level, placementPos, placementPos,
                    placementSettings, random, 2);

            if (success) {
                cleanUpJigsawBlocksOptimized(level, placementPos, size);
            }

            return success;

        } catch (Exception e) {
            UselessThings.LOGGER.error("placeTemplateWithFallback发生错误", e);
            return false;
        }
    }

    private static void cleanUpJigsawBlocksOptimized(ServerLevel level, BlockPos startPos, Vec3i size) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    mutablePos.set(startPos.getX() + x, startPos.getY() + y, startPos.getZ() + z);
                    var blockState = level.getBlockState(mutablePos);
                    var block = blockState.getBlock();
                    if (block == Blocks.JIGSAW ||
                            block == Blocks.STRUCTURE_BLOCK ||
                            block == Blocks.STRUCTURE_VOID ||
                            block == Blocks.LIGHT ||
                            block == Blocks.BARRIER) {
                        level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static boolean areChunksLoaded(ServerLevel level, BlockPos pos, Vec3i size) {
        BlockPos minPos = pos.offset(-size.getX() / 2, 0, -size.getZ() / 2);
        BlockPos maxPos = minPos.offset(size);

        int minChunkX = minPos.getX() >> 4;
        int minChunkZ = minPos.getZ() >> 4;
        int maxChunkX = maxPos.getX() >> 4;
        int maxChunkZ = maxPos.getZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void createExplosion(ServerLevel level, BlockPos pos) {
        level.explode(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                2.0f,
                false,
                Level.ExplosionInteraction.NONE
        );
    }
}