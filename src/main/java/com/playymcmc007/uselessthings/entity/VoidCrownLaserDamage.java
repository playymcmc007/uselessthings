package com.playymcmc007.uselessthings.entity;

import com.playymcmc007.uselessthings.ModItems;
import com.playymcmc007.uselessthings.entity.VoidLightningEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.*;

public class VoidCrownLaserDamage {

    private static final Map<UUID, Map<UUID, Integer>> damagedByLaser = new HashMap<>();
    private static final Map<UUID, Integer> lightningTimers = new HashMap<>();

    // 激光参数
    private static final float LASER_LENGTH = 3.0f;
    private static final float LASER_RADIUS = 0.5f;
    private static final float ORBIT_RADIUS = 1.0f;
    private static final float LASER_HEIGHT = 0.55f;
    private static final float LASER_DAMAGE = 10.0f;
    private static final int LASER_COOLDOWN = 10;

    // 雷击参数
    private static final int LIGHTNING_INTERVAL = 10;
    private static final float LIGHTNING_RANGE = 15.0f;
    private static final int INVINCIBLE_REDUCE_RANGE = 10;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER) return;
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (!hasActiveCrown(player)) {
            damagedByLaser.remove(player.getUUID());
            lightningTimers.remove(player.getUUID());
            return;
        }

        UUID playerId = player.getUUID();
        ServerLevel serverLevel = (ServerLevel) player.level();

        // ============ 10格内生物无敌帧降到2刻 ============
        AABB invArea = new AABB(
                player.getX() - INVINCIBLE_REDUCE_RANGE,
                player.getY() - INVINCIBLE_REDUCE_RANGE,
                player.getZ() - INVINCIBLE_REDUCE_RANGE,
                player.getX() + INVINCIBLE_REDUCE_RANGE,
                player.getY() + INVINCIBLE_REDUCE_RANGE,
                player.getZ() + INVINCIBLE_REDUCE_RANGE
        );

        List<LivingEntity> nearbyAll = player.level().getEntitiesOfClass(
                LivingEntity.class, invArea,
                e -> e.isAlive() && e != player
        );

        for (LivingEntity target : nearbyAll) {
            if (target.invulnerableTime > 2) {
                target.invulnerableTime = 2;
            }
        }

        // ============ 激光伤害 ============
        Map<UUID, Integer> laserDamaged = damagedByLaser.computeIfAbsent(playerId, k -> new HashMap<>());
        laserDamaged.entrySet().removeIf(e -> e.getValue() <= 0);
        laserDamaged.replaceAll((k, v) -> v - 1);

        float baseHeight = (float) player.getY() + LASER_HEIGHT;

        for (int i = 0; i < 8; i++) {
            float angle = (player.tickCount * 0.05f) + (i * (float) Math.PI * 2 / 8);

            Vec3 laserStart = new Vec3(
                    player.getX() + Math.cos(angle) * ORBIT_RADIUS,
                    baseHeight,
                    player.getZ() + Math.sin(angle) * ORBIT_RADIUS
            );
            Vec3 outward = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize();
            Vec3 laserEnd = laserStart.add(outward.scale(LASER_LENGTH));

            Vec3 min = new Vec3(
                    Math.min(laserStart.x, laserEnd.x) - LASER_RADIUS,
                    baseHeight - LASER_RADIUS,
                    Math.min(laserStart.z, laserEnd.z) - LASER_RADIUS
            );
            Vec3 max = new Vec3(
                    Math.max(laserStart.x, laserEnd.x) + LASER_RADIUS,
                    baseHeight + LASER_RADIUS,
                    Math.max(laserStart.z, laserEnd.z) + LASER_RADIUS
            );

            AABB beamAABB = new AABB(min, max);

            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, beamAABB)) {
                if (target == player) continue;
                if (target.isDeadOrDying()) continue;
                UUID targetId = target.getUUID();
                if (laserDamaged.getOrDefault(targetId, 0) > 0) continue;

                Vec3 closestPoint = closestPointOnSegment(laserStart, laserEnd, target.position());
                double dist = closestPoint.distanceTo(target.position());
                if (dist < LASER_RADIUS + target.getBbWidth() / 2) {
                    target.hurt(player.damageSources().playerAttack(player), LASER_DAMAGE);
                    target.setSecondsOnFire(2);
                    laserDamaged.put(targetId, LASER_COOLDOWN);
                }
            }
        }

        // ============ 雷击：范围内每个生物都劈 ============
        int timer = lightningTimers.getOrDefault(playerId, 0);

        if (timer <= 0) {
            AABB lightningArea = new AABB(
                    player.getX() - LIGHTNING_RANGE,
                    player.getY() - 2.0,
                    player.getZ() - LIGHTNING_RANGE,
                    player.getX() + LIGHTNING_RANGE,
                    player.getY() + 12.0,
                    player.getZ() + LIGHTNING_RANGE
            );

            List<LivingEntity> targets = player.level().getEntitiesOfClass(
                    LivingEntity.class, lightningArea,
                    e -> e.isAlive() && e != player
            );

            // 每个生物都劈
            for (LivingEntity target : targets) {
                VoidLightningEntity lightning = VoidLightningEntity.create(
                        serverLevel,
                        target.position(),
                        player
                );
                serverLevel.addFreshEntity(lightning);

                target.hurt(serverLevel.damageSources().lightningBolt(), 5.0f);
            }

            lightningTimers.put(playerId, LIGHTNING_INTERVAL);
        } else {
            lightningTimers.put(playerId, timer - 1);
        }
    }

    private static Vec3 closestPointOnSegment(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double abLenSq = ab.dot(ab);
        if (abLenSq == 0) return a;
        double t = ab.dot(p.subtract(a)) / abLenSq;
        t = Math.max(0, Math.min(1, t));
        return a.add(ab.scale(t));
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