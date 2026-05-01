package com.playymcmc007.uselessthings.event;

import com.playymcmc007.uselessthings.ModItems;
import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = UselessThings.MODID)
public class SuperBoneMealDropHandler {

    private static final ResourceKey<net.minecraft.world.level.biome.Biome> DARK_FOREST =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft", "dark_forest"));
    private static final int REQUIRED_LOG_STACKS = 10;
    private static final int REQUIRED_BONE_BLOCK_STACKS = 10;
    private static final double DROP_CHANCE = 0.01; // 1%

    @SubscribeEvent
    public static void onLivingDrop(LivingDropsEvent event) {
        // 1. 必须是骷髅
        if (!(event.getEntity() instanceof Skeleton skeleton)) return;

        // 2. 必须被玩家杀死
        Player killer = (Player) skeleton.getKillCredit();
        if (killer == null) return;

        // 3. 必须在黑森林
        var biome = skeleton.level().getBiome(skeleton.blockPosition());
        if (!biome.is(DARK_FOREST)) return;

        // 4. 必须手持木剑
        ItemStack mainHand = killer.getMainHandItem();
        if (!mainHand.is(Items.WOODEN_SWORD)) return;

        // 5. 检查玩家是否有足够的资源（仅检查，不消耗）
        if (!hasEnoughResources(killer)) return;

        // 6. 随机概率掉落
        if (skeleton.level().random.nextDouble() < DROP_CHANCE) {
            // 掉落成功，现在消耗资源
            consumeResources(killer);
            skeleton.spawnAtLocation(ModItems.SUPER_BONE_MEAL.get());
        }
        // 没有掉落则什么都不做，不消耗资源
    }

    /**
     * 仅检查玩家是否有足够的资源，不消耗
     */
    private static boolean hasEnoughResources(Player player) {
        int totalLogs = 0;
        int totalBoneBlocks = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ItemTags.LOGS)) {
                totalLogs += stack.getCount();
            } else if (stack.is(Blocks.BONE_BLOCK.asItem())) {
                totalBoneBlocks += stack.getCount();
            }
        }

        return totalLogs >= REQUIRED_LOG_STACKS * 64 &&
                totalBoneBlocks >= REQUIRED_BONE_BLOCK_STACKS * 64;
    }

    /**
     * 消耗玩家资源（仅在掉落成功时调用）
     */
    private static void consumeResources(Player player) {
        // 收集原木槽位
        List<Integer> logSlots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.LOGS)) {
                logSlots.add(i);
            }
        }

        // 收集骨块槽位
        List<Integer> boneBlockSlots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(Blocks.BONE_BLOCK.asItem())) {
                boneBlockSlots.add(i);
            }
        }

        // 消耗原木
        int logsToRemove = REQUIRED_LOG_STACKS * 64;
        for (int slot : logSlots) {
            ItemStack stack = player.getInventory().getItem(slot);
            int remove = Math.min(logsToRemove, stack.getCount());
            stack.shrink(remove);
            logsToRemove -= remove;
            if (logsToRemove == 0) break;
        }

        // 消耗骨块
        int bonesToRemove = REQUIRED_BONE_BLOCK_STACKS * 64;
        for (int slot : boneBlockSlots) {
            ItemStack stack = player.getInventory().getItem(slot);
            int remove = Math.min(bonesToRemove, stack.getCount());
            stack.shrink(remove);
            bonesToRemove -= remove;
            if (bonesToRemove == 0) break;
        }
    }
}