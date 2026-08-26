package com.playymcmc007.uselessthings.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;

public class VoidBootsItem extends ArmorItem {

    // 记录每个方块被踩过的时间
    private static final Map<BlockPos, Long> steppedBlocks = new HashMap<>();
    // 记录玩家上一tick的位置，用于判断"离开"
    private static final Map<UUID, BlockPos> lastPlayerPos = new HashMap<>();
    private static final int DELAY_TICKS = 10; // 离开后10刻 = 0.5秒

    public VoidBootsItem(ArmorMaterial material, Properties properties) {
        super(material, Type.BOOTS, properties);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide || event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        if (boots.getItem() instanceof VoidBootsItem) {
            Level level = player.level();
            BlockPos currentPos = player.blockPosition();
            UUID playerUUID = player.getUUID();
            long gameTime = level.getGameTime();

            // 获取玩家上一tick的位置
            BlockPos previousPos = lastPlayerPos.get(playerUUID);

            // 如果玩家移动了，说明离开了上一个方块
            if (previousPos != null && !currentPos.equals(previousPos)) {
                // 记录离开的方块和时间
                steppedBlocks.put(previousPos, gameTime);
            }

            // 更新玩家当前位置
            lastPlayerPos.put(playerUUID, currentPos);

            // 检查所有待破坏的方块
            checkAndDestroy(level, player, gameTime);
        }
    }

    private void checkAndDestroy(Level level, Player player, long currentTime) {
        Iterator<Map.Entry<BlockPos, Long>> iterator = steppedBlocks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            BlockPos pos = entry.getKey();
            long leaveTime = entry.getValue();

            if (currentTime - leaveTime >= DELAY_TICKS) {
                // 破坏该方块下方所有方块
                for (int y = pos.getY(); y >= level.getMinBuildHeight(); y--) {
                    BlockPos currentPos = new BlockPos(pos.getX(), y, pos.getZ());
                    Block blockToDestroy = level.getBlockState(currentPos).getBlock();

                    if (blockToDestroy != Blocks.AIR && blockToDestroy != Blocks.BEDROCK &&
                            blockToDestroy.defaultDestroyTime() >= 0) {
                        level.destroyBlock(currentPos, false);
                    }
                }

                // 50%概率扣1点耐久
                if (level.random.nextDouble() < 0.5) {
                    ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
                    boots.hurtAndBreak(1, player, (p) -> {
                        p.broadcastBreakEvent(EquipmentSlot.FEET);
                    });
                }

                iterator.remove();
            }
        }
    }
}