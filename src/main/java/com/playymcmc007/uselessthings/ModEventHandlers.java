package com.playymcmc007.uselessthings;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ModEventHandlers {
    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册创意标签事件
        bus.addListener(ModItems::addItemsToCreativeTab);
    }
}