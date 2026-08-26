package com.playymcmc007.uselessthings;

import com.playymcmc007.uselessthings.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, UselessThings.MODID);
    public static final RegistryObject<Block> ARCHITECTURAL_GREENERY = BLOCKS.register("architectural_greenery",
            () -> new ArchitecturalGreeneryBlock(BlockBehaviour.Properties.copy(Blocks.MOSS_BLOCK).mapColor(MapColor.COLOR_GREEN)));
    public static final RegistryObject<Block> ARCHITECTURAL_FLOWER = BLOCKS.register("architectural_flower",
            () -> new ArchitecturalFlowerBlock(BlockBehaviour.Properties.of().sound(SoundType.GRASS).mapColor(MapColor.COLOR_PINK)));
    public static final RegistryObject<Block> PLANT_STAFF = BLOCKS.register("plant_staff",
            () -> new PlantStaff(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .sound(SoundType.GRASS)
                    .instabreak()
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(state ->
                            state.getValue(PlantStaff.ACTIVATED) ?
                                    Math.min(state.getValue(PlantStaff.PROGRESS) / 2, 7) : 0)
                    .offsetType(BlockBehaviour.OffsetType.XZ)));
    public static final RegistryObject<Block> SUPER_CHUNK_BLOCK = BLOCKS.register("super_chunk_block",
        () -> new SuperChunkBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final RegistryObject<Block> SUPER_OAK_SAPLING = BLOCKS.register("super_oak_sapling",
            () -> new SuperSaplingBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
    public static final RegistryObject<Block> SUPER_SPRUCE_SAPLING = BLOCKS.register("super_spruce_sapling",
            () -> new SuperSaplingBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_SAPLING)));
    public static final RegistryObject<Block> SUPER_BIRCH_SAPLING = BLOCKS.register("super_birch_sapling",
            () -> new SuperSaplingBlock(BlockBehaviour.Properties.copy(Blocks.BIRCH_SAPLING)));
    public static final RegistryObject<Block> SUPER_JUNGLE_SAPLING = BLOCKS.register("super_jungle_sapling",
            () -> new SuperSaplingBlock(BlockBehaviour.Properties.copy(Blocks.JUNGLE_SAPLING)));
    public static final RegistryObject<Block> SUPER_ACACIA_SAPLING = BLOCKS.register("super_acacia_sapling",
            () -> new SuperSaplingBlock(BlockBehaviour.Properties.copy(Blocks.ACACIA_SAPLING)));
    public static final RegistryObject<Block> SUPER_DARK_OAK_SAPLING = BLOCKS.register("super_dark_oak_sapling",
            () -> new SuperSaplingBlock(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_SAPLING)));
    public static final RegistryObject<Block> SUPER_MANGROVE_PROPAGULE = BLOCKS.register("super_mangrove_propagule",
            () -> new SuperSaplingBlock(BlockBehaviour.Properties.copy(Blocks.MANGROVE_PROPAGULE)));
    public static final RegistryObject<Block> SUPER_CHERRY_SAPLING = BLOCKS.register("super_cherry_sapling",
            () -> new SuperSaplingBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_SAPLING)));
    public static final RegistryObject<Block> INVISIBLE_LIGHT = BLOCKS.register("invisible_light",
            () -> new InvisibleLightBlock());
    public static final RegistryObject<Block> EXPERIENCE_STACKED_BLOCK = BLOCKS.register("experience_stacked_block",
            () -> new ExperienceStackedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .sound(SoundType.GLASS)
                    .strength(0.5f)
                    .noOcclusion()
            ));
    public static void initCache() {
        PlantStaffCache.init();
    }

    public static void register(IEventBus eventBus) {
        // 先初始化缓存
        initCache();
        // 再注册方块
        BLOCKS.register(eventBus);

        UselessThings.LOGGER.info("Registered PlantStaff block with cache initialized");
    }
}