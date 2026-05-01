package com.playymcmc007.uselessthings.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.playymcmc007.uselessthings.UselessThings;
import com.playymcmc007.uselessthings.network.TeleportPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = UselessThings.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static boolean keyWasPressed = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        long window = mc.getWindow().getWindow();
        boolean isF24Pressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_F24);

        if (isF24Pressed) {
            if (!keyWasPressed) {
                keyWasPressed = true;

                ResourceKey<Level> currentDim = player.level().dimension();
                ResourceKey<Level> targetDim = UselessThings.NO_STONE_DIMENSION;

                if (currentDim.equals(targetDim)) {
                    TeleportPacket.CHANNEL.sendToServer(
                            new TeleportPacket(Level.OVERWORLD)
                    );
                } else {
                    TeleportPacket.CHANNEL.sendToServer(
                            new TeleportPacket(targetDim)
                    );
                }
            }
        } else {
            keyWasPressed = false;
        }
    }
}