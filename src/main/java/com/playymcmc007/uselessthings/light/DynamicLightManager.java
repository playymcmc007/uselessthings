package com.playymcmc007.uselessthings.light;

import com.playymcmc007.uselessthings.ModBlocks;
import com.playymcmc007.uselessthings.ModItems;
import com.playymcmc007.uselessthings.item.VoidCrownItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicLightManager {

    // 追踪每个玩家已有的光源位置
    private static final Map<UUID, Set<BlockPos>> playerLights = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Integer> lightTimers = new ConcurrentHashMap<>();
    private static final int LIGHT_DURATION = 3; // 光源存在tick数

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER) return;
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        UUID playerId = player.getUUID();
        boolean hasActiveCrown = hasActiveVoidCrown(player);

        if (hasActiveCrown) {
            // 玩家周围生成光源
            Set<BlockPos> currentLights = playerLights.computeIfAbsent(playerId, k -> new HashSet<>());
            Set<BlockPos> newLights = new HashSet<>();

            // 围绕玩家创建多层光源
            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();

            // 脚下光源
            BlockPos footPos = new BlockPos((int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
            placeLightIfNeeded(serverLevel, footPos, newLights);

            // 身体周围光环（3层，不同高度）
            for (int ring = 0; ring < 3; ring++) {
                double ringHeight = py + 1.0 + ring * 0.8;
                double ringRadius = 1.5 + ring * 0.5;
                int points = 6 + ring * 2;

                for (int i = 0; i < points; i++) {
                    double angle = (player.tickCount * 0.05) + (i * Math.PI * 2 / points);
                    int x = (int) Math.floor(px + Math.cos(angle) * ringRadius);
                    int y = (int) Math.floor(ringHeight);
                    int z = (int) Math.floor(pz + Math.sin(angle) * ringRadius);

                    BlockPos lightPos = new BlockPos(x, y, z);
                    placeLightIfNeeded(serverLevel, lightPos, newLights);
                }
            }

            // 头顶王冠光源
            BlockPos crownPos = new BlockPos((int) Math.floor(px), (int) Math.floor(py + 2.5), (int) Math.floor(pz));
            placeLightIfNeeded(serverLevel, crownPos, newLights);

            // 移除不再需要的光源
            for (BlockPos oldPos : currentLights) {
                if (!newLights.contains(oldPos)) {
                    removeLight(serverLevel, oldPos);
                }
            }

            playerLights.put(playerId, newLights);

        } else {
            // 玩家没有激活的王冠，移除所有光源
            Set<BlockPos> existingLights = playerLights.remove(playerId);
            if (existingLights != null) {
                for (BlockPos pos : existingLights) {
                    removeLight(serverLevel, pos);
                }
            }
        }

        // 更新光源计时器
        Iterator<Map.Entry<BlockPos, Integer>> it = lightTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            int timer = entry.getValue() - 1;
            if (timer <= 0) {
                BlockPos pos = entry.getKey();
                if (serverLevel.getBlockState(pos).getBlock() == ModBlocks.INVISIBLE_LIGHT.get()) {
                    serverLevel.removeBlock(pos, false);
                }
                it.remove();
            } else {
                entry.setValue(timer);
            }
        }
    }

    private static void placeLightIfNeeded(ServerLevel level, BlockPos pos, Set<BlockPos> lightSet) {
        // 检查位置是否有效
        if (!level.isLoaded(pos)) return;

        BlockState existingState = level.getBlockState(pos);

        // 如果该位置是空气或可替换方块，放置光源
        if (existingState.isAir() || existingState.canBeReplaced()) {
            level.setBlock(pos, ModBlocks.INVISIBLE_LIGHT.get().defaultBlockState(), 3);
            lightSet.add(pos);
            lightTimers.put(pos, LIGHT_DURATION);
        }
        // 如果已经是我们的光源，刷新计时器
        else if (existingState.getBlock() == ModBlocks.INVISIBLE_LIGHT.get()) {
            lightSet.add(pos);
            lightTimers.put(pos, LIGHT_DURATION);
        }
    }

    private static void removeLight(ServerLevel level, BlockPos pos) {
        if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() == ModBlocks.INVISIBLE_LIGHT.get()) {
            level.removeBlock(pos, false);
        }
    }

    private static boolean hasActiveVoidCrown(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.VOID_CROWN.get()
                    && stack.getOrCreateTag().getBoolean(VoidCrownItem.TAG_ACTIVE)) {
                return true;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.getItem() == ModItems.VOID_CROWN.get()
                && offhand.getOrCreateTag().getBoolean(VoidCrownItem.TAG_ACTIVE);
    }

    // 服务器关闭时清理
    public static void cleanup() {
        playerLights.clear();
        lightTimers.clear();
    }
}