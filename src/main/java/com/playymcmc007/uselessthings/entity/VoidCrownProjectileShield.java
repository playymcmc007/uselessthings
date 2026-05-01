package com.playymcmc007.uselessthings.entity;

import com.playymcmc007.uselessthings.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public class VoidCrownProjectileShield {

    private static final float SHIELD_RANGE = 5.0f;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER) return;
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (!hasActiveCrown(player)) return;

        AABB area = new AABB(
                player.getX() - SHIELD_RANGE,
                player.getY() - SHIELD_RANGE,
                player.getZ() - SHIELD_RANGE,
                player.getX() + SHIELD_RANGE,
                player.getY() + SHIELD_RANGE,
                player.getZ() + SHIELD_RANGE
        );

        for (Projectile proj : player.level().getEntitiesOfClass(Projectile.class, area)) {
            // 不删玩家自己射出的（可选，如果你想要无差别删除就删掉这个判断）
            if (proj.getOwner() == player) continue;

            // 清除弹射物
            proj.discard();
        }
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