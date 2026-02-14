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