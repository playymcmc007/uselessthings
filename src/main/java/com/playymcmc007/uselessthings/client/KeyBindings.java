package com.playymcmc007.uselessthings.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = UselessThings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
    }
}