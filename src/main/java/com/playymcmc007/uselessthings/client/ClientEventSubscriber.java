package com.playymcmc007.uselessthings.client;

import com.playymcmc007.uselessthings.UselessThings;
import com.playymcmc007.uselessthings.client.effect.SuperLaserRenderer;
import com.playymcmc007.uselessthings.client.model.TestModel;
import com.playymcmc007.uselessthings.client.render.TestRenderer;
import com.playymcmc007.uselessthings.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = UselessThings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventSubscriber {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TestModel.LAYER_LOCATION, TestModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TEST_ENTITY.get(), TestRenderer::new);
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册超级激光渲染器
        MinecraftForge.EVENT_BUS.register(SuperLaserRenderer.class);
    }
}