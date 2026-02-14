package com.playymcmc007.uselessthings;

import com.playymcmc007.uselessthings.block.ArchitecturalGreeneryBlock;
import com.playymcmc007.uselessthings.world.StructureGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

@Mod(UselessThings.MODID)
public class UselessThings {
    public static final String MODID = "uselessthings";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public UselessThings() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        ModItems.register();
        ModBlocks.register(modEventBus);
        ModFluids.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEffects.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModPotions.register(FMLJavaModLoadingContext.get().getModEventBus());
        modEventBus.addListener(this::commonSetup);
        forgeEventBus.register(this);
        ModEventHandlers.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerBlockColors);
        }
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
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
}