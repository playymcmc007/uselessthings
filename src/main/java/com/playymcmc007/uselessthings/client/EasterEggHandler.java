package com.playymcmc007.uselessthings.client;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.playymcmc007.uselessthings.UselessThings;
import com.playymcmc007.uselessthings.client.ISoundBufferExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = UselessThings.MODID, value = Dist.CLIENT)
public class EasterEggHandler {

    public static boolean isUpsideDown = false;

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final String REVERSE_ALPHABET = "zyxwvutsrqponmlkjihgfedcba";
    private static int alphabetIndex = 0;

    // ---- 按键检测 ----
    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;
        // 只允许在主菜单界面触发彩蛋
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen)) {
            return;
        }

        int key = event.getKeyCode();

        // ---- 字母表彩蛋（同样只在主菜单） ----
        if (key < GLFW.GLFW_KEY_A || key > GLFW.GLFW_KEY_Z) {
            alphabetIndex = 0;
            return;
        }

        char c = Character.toLowerCase((char) key);
        String targetSeq = isUpsideDown ? ALPHABET : REVERSE_ALPHABET;
        if (c == targetSeq.charAt(alphabetIndex)) {
            alphabetIndex++;
            if (alphabetIndex == 26) {
                isUpsideDown = !isUpsideDown;
                alphabetIndex = 0;
                applyState();
                mc.gui.getChat().addMessage(Component.literal(
                        isUpsideDown ? "§c[!] 混乱模式已激活！" : "§a[!] 模式已停用！"
                ));
            }
        } else {
            alphabetIndex = 0;
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (!isUpsideDown) return;
        var pose = event.getGuiGraphics().pose();
        pose.pushPose();
        int w = event.getScreen().width;
        int h = event.getScreen().height;
        pose.translate(w / 2.0f, h / 2.0f, 0);
        pose.scale(-1.0f, -1.0f, 1.0f);
        pose.translate(-w / 2.0f, -h / 2.0f, 0);
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!isUpsideDown) return;
        event.getGuiGraphics().pose().popPose();
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiOverlayEvent.Pre event) {
        if (!isUpsideDown) return;
        var pose = event.getGuiGraphics().pose();
        pose.pushPose();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();
        pose.translate(w / 2.0f, h / 2.0f, 0);
        pose.scale(-1.0f, -1.0f, 1.0f);
        pose.translate(-w / 2.0f, -h / 2.0f, 0);
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiOverlayEvent.Post event) {
        if (!isUpsideDown) return;
        event.getGuiGraphics().pose().popPose();
    }

    private static void applyState() {
        Minecraft mc = Minecraft.getInstance();
        mc.reloadResourcePacks();
    }

}