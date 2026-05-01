package com.playymcmc007.uselessthings.entity;

import com.playymcmc007.uselessthings.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class VoidCrownDeathExplosion {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!hasActiveCrown(player)) return;

        Level level = player.level();
        level.explode(
                player,
                player.getX(), player.getY(), player.getZ(),
                6.0f,
                false,
                Level.ExplosionInteraction.NONE
        );
    }

    private static boolean hasActiveCrown(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.VOID_CROWN.get()
                    && stack.getOrCreateTag().getBoolean("void_crown_active")) {
                return true;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.getItem() == ModItems.VOID_CROWN.get()
                && offhand.getOrCreateTag().getBoolean("void_crown_active");
    }
}