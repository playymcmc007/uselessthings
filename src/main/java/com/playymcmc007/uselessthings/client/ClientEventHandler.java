package com.playymcmc007.uselessthings.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.playymcmc007.uselessthings.ModEffects;
import com.playymcmc007.uselessthings.UselessThings;
import com.playymcmc007.uselessthings.network.LaserTogglePacket;
import com.playymcmc007.uselessthings.network.LaserNetwork;
import com.playymcmc007.uselessthings.network.TeleportPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = UselessThings.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static boolean teleportKeyWasPressed = false;
    private static boolean laserLastKeyState = false;
    private static LaserLoopSound currentLaserSound = null;
    private static final SoundEvent LASER_SOUND = SoundEvent.createVariableRangeEvent(
            new ResourceLocation(UselessThings.MODID, "super_laser")
    );
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        long window = mc.getWindow().getWindow();

        // ========== 传送功能 (F24) ==========
        boolean isF24Pressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_F24);

        if (isF24Pressed) {
            if (!teleportKeyWasPressed) {
                teleportKeyWasPressed = true;

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
            teleportKeyWasPressed = false;
        }

        // ========== 激光开关 (Shift + P) ==========
        boolean isLaserKeyPressed = KeyBindings.LASER_KEY.isDown();

        if (isLaserKeyPressed) {
            if (!laserLastKeyState) {
                laserLastKeyState = true;
                // 切换激光状态
                boolean newState = !com.playymcmc007.uselessthings.network.LaserManager.isLaserActive(player);
                LaserNetwork.CHANNEL.sendToServer(new LaserTogglePacket(newState));
            }
        } else {
            laserLastKeyState = false;
        }
        boolean isLaserActive = com.playymcmc007.uselessthings.network.LaserManager.isLaserActive(player);
        boolean hasLaserEffect = player.hasEffect(ModEffects.SUPER_LASER_EFFECT.get());

        if (isLaserActive && hasLaserEffect) {
            if (currentLaserSound == null) {
                // 开始循环播放
                currentLaserSound = new LaserLoopSound(player);
                mc.getSoundManager().play(currentLaserSound);
            }
        } else {
            if (currentLaserSound != null) {
                // 停止播放
                mc.getSoundManager().stop(currentLaserSound);
                currentLaserSound = null;
            }
        }
    }

    // 循环音效类（像 NitwitSprayerItem 那样简单，但支持循环）
    private static class LaserLoopSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        public LaserLoopSound(LocalPlayer player) {
            super(LASER_SOUND, SoundSource.PLAYERS, net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;      // 关键：设置为循环播放
            this.delay = 0;
            this.volume = 0.6f;
            this.pitch = 1.0f;
            this.attenuation = Attenuation.LINEAR;
        }

        @Override
        public void tick() {
            if (player == null || !player.isAlive()) {
                return;
            }
            // 跟随玩家位置
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }
    }
}