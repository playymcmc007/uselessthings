package com.playymcmc007.uselessthings.network;

import com.playymcmc007.uselessthings.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LaserManager {
    private static final Set<UUID> activeLasers = new HashSet<>();
    private static final int MAX_DISTANCE = 500;
    private static final double DESTROY_RADIUS = 1.0;
    private static final double LASER_DAMAGE = 3.0;      // 伤害值
    private static final double KNOCKBACK_FORCE = 2.5;

    public static void setLaserActive(Player player, boolean active) {
        if (active) {
            activeLasers.add(player.getUUID());
        } else {
            activeLasers.remove(player.getUUID());
        }
    }

    public static boolean isLaserActive(Player player) {
        return activeLasers.contains(player.getUUID());
    }

    public static void processLaser(Player player) {
        if (!isLaserActive(player)) return;
        if (!player.hasEffect(ModEffects.SUPER_LASER_EFFECT.get())) {
            setLaserActive(player, false);
            return;
        }

        Level level = player.level();

        Vec3 startPos = player.position().add(0, 1.2, 0);
        Vec3 direction = player.getViewVector(1.0F).normalize();
        Vec3 endPos = startPos.add(direction.scale(MAX_DISTANCE));

        destroyBlocks(level, startPos, direction, MAX_DISTANCE, DESTROY_RADIUS);

        // 2. 伤害生物
        damageEntities(level, player, startPos, endPos, DESTROY_RADIUS);
    }
    private static void destroyBlocks(Level level, Vec3 startPos, Vec3 direction,
                                      double maxDistance, double radius) {
        double step = 0.5;

        for (double distance = 0; distance < maxDistance; distance += step) {
            Vec3 centerPos = startPos.add(direction.scale(distance));

            int minX = (int) Math.floor(centerPos.x - radius);
            int maxX = (int) Math.floor(centerPos.x + radius);
            int minY = (int) Math.floor(centerPos.y - radius);
            int maxY = (int) Math.floor(centerPos.y + radius);
            int minZ = (int) Math.floor(centerPos.z - radius);
            int maxZ = (int) Math.floor(centerPos.z + radius);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (y < level.getMinBuildHeight() || y > level.getMaxBuildHeight()) continue;
                    for (int z = minZ; z <= maxZ; z++) {
                        double dx = x + 0.5 - centerPos.x;
                        double dy = y + 0.5 - centerPos.y;
                        double dz = z + 0.5 - centerPos.z;
                        if (dx*dx + dy*dy + dz*dz <= radius * radius) {
                            BlockPos blockPos = new BlockPos(x, y, z);
                            if (!level.getBlockState(blockPos).isAir()) {
                                level.destroyBlock(blockPos, false);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 伤害激光路径上的生物
     */
    private static void damageEntities(Level level, Player player, Vec3 startPos, Vec3 endPos, double radius) {
        // 计算激光的包围盒
        double minX = Math.min(startPos.x, endPos.x) - radius;
        double maxX = Math.max(startPos.x, endPos.x) + radius;
        double minY = Math.min(startPos.y, endPos.y) - radius;
        double maxY = Math.max(startPos.y, endPos.y) + radius;
        double minZ = Math.min(startPos.z, endPos.z) - radius;
        double maxZ = Math.max(startPos.z, endPos.z) + radius;

        AABB laserBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, laserBox,
                entity -> entity != player && entity.isAlive());

        Vec3 direction = endPos.subtract(startPos).normalize();

        for (LivingEntity entity : entities) {
            if (isEntityInLaser(entity, startPos, endPos, radius)) {
                entity.hurt(level.damageSources().indirectMagic(player, player), (float) LASER_DAMAGE);
                Vec3 knockbackDir = entity.position().subtract(startPos).normalize();
                double knockbackX = knockbackDir.x * KNOCKBACK_FORCE;
                double knockbackZ = knockbackDir.z * KNOCKBACK_FORCE;
                double knockbackY = 0.4;

                entity.setDeltaMovement(knockbackX, knockbackY, knockbackZ);
                entity.hurtMarked = true;
            }
        }
    }

    /**
     * 判断实体是否在激光圆柱范围内
     */
    private static boolean isEntityInLaser(Entity entity, Vec3 startPos, Vec3 endPos, double radius) {
        Vec3 entityPos = entity.position();

        // 计算点到线段的距离
        Vec3 startToEnd = endPos.subtract(startPos);
        Vec3 startToEntity = entityPos.subtract(startPos);

        double t = startToEntity.dot(startToEnd) / startToEnd.dot(startToEnd);
        t = Math.max(0, Math.min(1, t));  // 限制在 0-1 之间

        Vec3 closestPoint = startPos.add(startToEnd.scale(t));
        double distance = entityPos.distanceTo(closestPoint);

        return distance <= radius + 0.5;  // 加上0.5格余量，更容易击中
    }

    /**
     * Bresenham 3D 直线算法
     */
    private static Set<BlockPos> getLineBlocks(BlockPos start, BlockPos end) {
        Set<BlockPos> blocks = new HashSet<>();

        int x1 = start.getX(), y1 = start.getY(), z1 = start.getZ();
        int x2 = end.getX(), y2 = end.getY(), z2 = end.getZ();

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);

        int xs = x2 > x1 ? 1 : -1;
        int ys = y2 > y1 ? 1 : -1;
        int zs = z2 > z1 ? 1 : -1;

        int x = x1, y = y1, z = z1;

        if (dx >= dy && dx >= dz) {
            int p1 = 2 * dy - dx;
            int p2 = 2 * dz - dx;
            while (x != x2) {
                blocks.add(new BlockPos(x, y, z));
                if (p1 >= 0) {
                    y += ys;
                    p1 -= 2 * dx;
                }
                if (p2 >= 0) {
                    z += zs;
                    p2 -= 2 * dx;
                }
                p1 += 2 * dy;
                p2 += 2 * dz;
                x += xs;
            }
        } else if (dy >= dx && dy >= dz) {
            int p1 = 2 * dx - dy;
            int p2 = 2 * dz - dy;
            while (y != y2) {
                blocks.add(new BlockPos(x, y, z));
                if (p1 >= 0) {
                    x += xs;
                    p1 -= 2 * dy;
                }
                if (p2 >= 0) {
                    z += zs;
                    p2 -= 2 * dy;
                }
                p1 += 2 * dx;
                p2 += 2 * dz;
                y += ys;
            }
        } else {
            int p1 = 2 * dy - dz;
            int p2 = 2 * dx - dz;
            while (z != z2) {
                blocks.add(new BlockPos(x, y, z));
                if (p1 >= 0) {
                    y += ys;
                    p1 -= 2 * dz;
                }
                if (p2 >= 0) {
                    x += xs;
                    p2 -= 2 * dz;
                }
                p1 += 2 * dy;
                p2 += 2 * dx;
                z += zs;
            }
        }
        blocks.add(end);

        return blocks;
    }
}