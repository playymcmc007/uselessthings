package com.playymcmc007.uselessthings.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.playymcmc007.uselessthings.UselessThings;
import cpw.mods.util.Lazy;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = UselessThings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {
    public static final String KEY_CATEGORY = "key.category.uselessthings";
    public static final String KEY_LASER = "key.uselessthings.laser";

    public static final KeyMapping LASER_KEY = new KeyMapping(
            KEY_LASER,
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            KEY_CATEGORY
    );
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(LASER_KEY);
    }
}