package com.playymcmc007.uselessthings.event;

import com.playymcmc007.uselessthings.network.LaserManager;
import com.playymcmc007.uselessthings.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ServerLaserHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.player;

        // 如果玩家没有效果，强制关闭激光
        if (!player.hasEffect(ModEffects.SUPER_LASER_EFFECT.get())) {
            if (LaserManager.isLaserActive(player)) {
                LaserManager.setLaserActive(player, false);
            }
            return;
        }

        // 处理激光破坏方块
        if (LaserManager.isLaserActive(player)) {
            LaserManager.processLaser(player);
        }
    }
}