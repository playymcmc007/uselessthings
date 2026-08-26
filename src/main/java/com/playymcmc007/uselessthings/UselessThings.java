package com.playymcmc007.uselessthings;

import com.playymcmc007.uselessthings.block.ArchitecturalGreeneryBlock;
import com.playymcmc007.uselessthings.block.SuperChunkBlock;
import com.playymcmc007.uselessthings.client.effect.*;
import com.playymcmc007.uselessthings.entity.*;
import com.playymcmc007.uselessthings.light.DynamicLightManager;
import com.playymcmc007.uselessthings.network.LaserNetwork;
import com.playymcmc007.uselessthings.network.TeleportPacket;
import com.playymcmc007.uselessthings.world.ModWorldTypes;
import com.playymcmc007.uselessthings.world.StructureGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DataPackRegistryEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

import java.util.Set;
import java.util.UUID;

@Mod(UselessThings.MODID)
public class UselessThings {
    public static final String MODID = "uselessthings";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final ResourceLocation NO_STONE_DIMENSION_ID =
            new ResourceLocation(MODID, "no_stone_dimension");
    public static final ResourceKey<Level> NO_STONE_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, NO_STONE_DIMENSION_ID);

    public UselessThings() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        ModItems.register();
        ModBlocks.register(modEventBus);
        ModFluids.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEffects.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModPotions.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEntities.register(modEventBus);
        ModWorldTypes.register(modEventBus);
        ModFeatures.register(modEventBus);
        LaserNetwork.register();
        modEventBus.addListener(this::commonSetup);
        forgeEventBus.register(this);
        ModEventHandlers.register();

        // 客户端特效事件
        MinecraftForge.EVENT_BUS.register(VoidCrownEffectHandler.class);
        MinecraftForge.EVENT_BUS.register(DynamicLightManager.class);
        MinecraftForge.EVENT_BUS.register(VoidCrownLaserDamage.class);
        MinecraftForge.EVENT_BUS.register(VoidCrownLaserRenderer.class);
        MinecraftForge.EVENT_BUS.register(VoidLightningParticle.class);
        MinecraftForge.EVENT_BUS.register(VoidCrownDeathExplosion.class);
        MinecraftForge.EVENT_BUS.register(VoidCrownProjectileShield.class);
        MinecraftForge.EVENT_BUS.register(VoidCrownWings.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerBlockColors);
        }
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册网络包
            TeleportPacket.register();
        });
    }
    private void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (state.getBlock() instanceof ArchitecturalGreeneryBlock) {
                        int color = ((ArchitecturalGreeneryBlock) state.getBlock())
                                .getColor(state);
                        return color;
                    }
                    return 0xFFFFFF;
                },
                ModBlocks.ARCHITECTURAL_GREENERY.get()
        );
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (event.getServer().overworld() instanceof ServerLevel serverLevel) {
            StructureGenerator.preloadTemplates(serverLevel);//不能删，删了无法生成建筑
        }
    }
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        SuperChunkBlock.shutdown();
    }
    private void registerDataPackEntries(DataPackRegistryEvent.NewRegistry event) {
        // 注册维度类型和维度的数据包注册表
        event.dataPackRegistry(Registries.DIMENSION_TYPE, DimensionType.DIRECT_CODEC);
        event.dataPackRegistry(Registries.LEVEL_STEM, LevelStem.CODEC);
        LOGGER.info("Registered dimension data pack registries");
    }
}