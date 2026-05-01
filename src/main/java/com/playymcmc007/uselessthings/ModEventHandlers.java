package com.playymcmc007.uselessthings;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.DataPackRegistryEvent;

import java.util.*;

public class ModEventHandlers {
    private static final Map<UUID, ActiveBeePoking> activePokings = new HashMap<>();
    private static final Map<BlockPos, UUID> hiveActiveEvents = new HashMap<>();
    private static final Map<Integer, UUID> targetToProcessMap = new HashMap<>();
    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ModEventHandlers.class);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(ModItems::addItemsToCreativeTab);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack heldItem = player.getItemInHand(event.getHand());

        // 检查是否手持捅蜂棍
        if (heldItem.getItem() == ModItems.HIVE_POKER.get()) {
            if (state.getBlock() instanceof BeehiveBlock) {
                // 阻止正常行为（如收集蜂蜜）
                event.setCanceled(true);

                // 开始捅蜂窝过程
                if (!level.isClientSide) {
                    Integer targetID = getBoundTargetID(heldItem);
                    Entity target = null;

                    if (targetID != null) {
                        // 使用绑定的生物作为目标
                        target = level.getEntity(targetID);
                        if (target == null || !target.isAlive()) {
                            player.displayClientMessage(Component.literal("绑定的目标不存在或已死亡！"), true);
                            return;
                        }
                    } else {
                        // 使用玩家自己作为目标
                        target = player;
                    }

                    // 检查目标是否已经在被攻击（统一检查所有实体）
                    if (targetToProcessMap.containsKey(target.getId())) {
                        String targetName = target == player ? "你" : target.getName().getString();
                        player.displayClientMessage(Component.literal(targetName + "已经在被蜂群攻击！"), true);
                        return;
                    }

                    // 消耗物品耐久（一次性使用）
                    if (!player.getAbilities().instabuild) {
                        heldItem.hurtAndBreak(1, player,
                                (p) -> p.broadcastBreakEvent(event.getHand()));
                    }
                    startBeePokingProcess(level, pos, target, heldItem);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        Entity target = event.getTarget();
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        // 检查是否主手持捅蜂窝棍，副手持命名牌
        if (mainHandItem.getItem() == ModItems.HIVE_POKER.get() &&
                offHandItem.getItem() == Items.NAME_TAG) {

            event.setCanceled(true);

            if (!level.isClientSide) {
                // 绑定目标到捅蜂窝棍
                bindTargetToBeePoker(mainHandItem, target);
                player.displayClientMessage(Component.literal("已绑定目标: " + target.getName().getString()), true);

                // 消耗命名牌（可选）
                if (!player.getAbilities().instabuild) {
                    offHandItem.shrink(1);
                }
            }
        }
    }

    private static Integer getBoundTargetID(ItemStack beePoker) {
        if (beePoker.hasTag() && beePoker.getTag().contains("BoundTargetID")) {
            return beePoker.getTag().getInt("BoundTargetID");
        }
        return null;
    }

    private static void bindTargetToBeePoker(ItemStack beePoker, Entity target) {
        if (!beePoker.hasTag()) {
            beePoker.setTag(new CompoundTag());
        }

        CompoundTag tag = beePoker.getTag();
        tag.putInt("BoundTargetID", target.getId());
        tag.putString("BoundTargetName", target.getName().getString());
        tag.putString("BoundTargetUUID", target.getUUID().toString());
        String customName = target.getName().getString() + " [" + target.getStringUUID() + "]";
        beePoker.setHoverName(Component.literal(customName));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 在每个服务器tick结束时更新所有活跃的捅蜂窝过程
            activePokings.entrySet().removeIf(entry -> entry.getValue().update());
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
                cleanupInvalidEntries();
                lastCleanupTime = currentTime;
            }
        }
    }

    private static void cleanupInvalidEntries() {
        // 清理 activePokings 中的无效条目
        activePokings.entrySet().removeIf(entry -> {
            ActiveBeePoking poking = entry.getValue();
            if (poking == null || poking.shouldBeCleanedUp()) {
                // 同时清理目标映射
                if (poking != null && poking.target != null) {
                    targetToProcessMap.remove(poking.target.getId());
                }
                return true;
            }
            return false;
        });

        // 清理 hiveActiveEvents 中的无效条目
        hiveActiveEvents.entrySet().removeIf(entry -> {
            BlockPos hivePos = entry.getKey();
            UUID processId = entry.getValue();

            // 检查过程是否还存在
            if (!activePokings.containsKey(processId)) {
                // 同时清理目标映射
                ActiveBeePoking poking = activePokings.get(processId);
                if (poking != null && poking.target != null) {
                    targetToProcessMap.remove(poking.target.getId());
                }
                return true;
            }

            // 检查蜂巢是否还存在
            ActiveBeePoking poking = activePokings.get(processId);
            return poking == null || !poking.isHiveStillValid();
        });

        // 清理 targetToProcessMap（确保一致性）
        targetToProcessMap.entrySet().removeIf(entry -> {
            UUID processId = entry.getValue();
            return !activePokings.containsKey(processId);
        });
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide) {
            // 清理玩家相关的过程
            UUID processId = targetToProcessMap.get(event.getEntity().getId());
            if (processId != null) {
                ActiveBeePoking poking = activePokings.get(processId);
                if (poking != null) {
                    poking.clearAllBees();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getEntity().level().isClientSide) {
            // 清理玩家相关的过程
            UUID processId = targetToProcessMap.get(event.getEntity().getId());
            if (processId != null) {
                ActiveBeePoking poking = activePokings.get(processId);
                if (poking != null) {
                    poking.clearAllBees();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            // 清理玩家相关的过程
            UUID processId = targetToProcessMap.get(player.getId());
            if (processId != null) {
                ActiveBeePoking poking = activePokings.get(processId);
                if (poking != null) {
                    poking.clearAllBees();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) {
            // 检查是否有以该实体为目标的捅蜂窝过程
            UUID processId = targetToProcessMap.get(event.getEntity().getId());
            if (processId != null) {
                ActiveBeePoking poking = activePokings.get(processId);
                if (poking != null) {
                    poking.clearAllBees();
                }
            }
        }
    }

    private static void startBeePokingProcess(Level level, BlockPos hivePos, Entity target, ItemStack beePoker){
        if (hiveActiveEvents.containsKey(hivePos)) {
            return;
        }

        // 二次检查目标是否已在被攻击
        if (targetToProcessMap.containsKey(target.getId())) {
            return;
        }

        UUID processId = UUID.randomUUID(); // 使用随机UUID作为过程ID

        // 获取蜂箱方块实体
        if (level.getBlockEntity(hivePos) instanceof BeehiveBlockEntity beehive) {
            // 获取蜂箱中的蜜蜂数量
            int beeCount = beehive.getOccupantCount();

            // 如果蜂箱中没有蜜蜂，设置一个默认数量
            if (beeCount < 1) {
                return;
            }

            // 创建新的捅蜂窝过程
            ActiveBeePoking poking = new ActiveBeePoking(level, hivePos, target, beeCount, beePoker, processId);
            activePokings.put(processId, poking);
            hiveActiveEvents.put(hivePos, processId);
            targetToProcessMap.put(target.getId(), processId); // 记录目标到过程的映射
            // 生成第一波蜜蜂
            poking.spawnNextWave();
        }
    }

    // 捅蜂窝过程的管理类
    private static class ActiveBeePoking {
        private final Level level;
        private final BlockPos hivePos;
        private final Entity target;
        private final int beesPerWave;
        private final List<Integer> currentBees = new ArrayList<>();
        private int ticksSinceLastWave = 0;
        private static final int WAVE_INTERVAL = 40; // 2秒间隔 (20 ticks = 1秒)
        private final long startTime;
        private static final long MAX_DURATION = 5 * 60 * 1000;
        private boolean shouldDestroyHive = false;
        private final ItemStack beePoker;
        private final UUID processId;

        public ActiveBeePoking(Level level, BlockPos hivePos, Entity target, int beesPerWave, ItemStack beePoker, UUID processId) {
            this.level = level;
            this.hivePos = hivePos;
            this.target = target;
            this.beesPerWave = beesPerWave;
            this.startTime = System.currentTimeMillis();
            this.beePoker = beePoker;
            this.processId = processId;
        }

        private void cleanupAllMappings() {
            // 清理蜂巢映射
            if (hivePos != null) {
                hiveActiveEvents.remove(hivePos);
            }

            // 清理目标映射
            if (target != null) {
                targetToProcessMap.remove(target.getId());
            }

            // 清理过程映射
            activePokings.remove(processId);
        }

        public boolean update() {
            // 检查目标是否还存在
            if (target == null || !target.isAlive() || level == null || target.isRemoved()) {
                cleanupAllMappings();
                return true;
            }

            if (!isHiveStillValid()) {
                cleanupAllMappings();
                return true; // 如果蜂巢被破坏，移除这个过程
            }

            if (System.currentTimeMillis() - startTime > MAX_DURATION) {
                shouldDestroyHive = true; // 标记要销毁蜂巢
                destroyHive(); // 销毁蜂巢
                clearAllBees();
                cleanupAllMappings();
                return true;
            }

            // 移除已经死亡或者失去刺的蜜蜂
            currentBees.removeIf(beeId -> {
                Bee bee = getBeeById(beeId);
                return bee == null || !bee.isAlive() || bee.hasStung();
            });

            // 如果当前没有活跃的蜜蜂，准备生成下一波
            if (currentBees.isEmpty()) {
                ticksSinceLastWave++;

                if (ticksSinceLastWave >= WAVE_INTERVAL) {
                    spawnNextWave();
                    ticksSinceLastWave = 0;
                }
            }

            return false; // 继续这个过程
        }

        private void destroyHive() {
            if (hivePos == null || level == null) {
                return;
            }

            BlockState hiveState = level.getBlockState(hivePos);
            if (!(hiveState.getBlock() instanceof BeehiveBlock)) {
                return; // 蜂巢已经被破坏或不是蜂巢
            }

            // 获取蜂巢方块实体以释放所有蜜蜂
            if (level.getBlockEntity(hivePos) instanceof BeehiveBlockEntity beehive) {
                // 释放所有蜜蜂（紧急状态，会攻击玩家）
                if (target instanceof Player) {
                    beehive.emptyAllLivingFromHive((Player) target, hiveState, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                } else {
                    beehive.emptyAllLivingFromHive(null, hiveState, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                }
            }
            // 销毁蜂巢并掉落物品
            level.destroyBlock(hivePos, false); // true 表示会掉落物品

            level.playSound(null, hivePos, hiveState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        public void clearAllBees() {
            // 清理所有映射
            cleanupAllMappings();

            // 如果是因为时间结束而清理，销毁蜂巢
            if (shouldDestroyHive && isHiveStillValid()) {
                destroyHive();
                shouldDestroyHive = false; // 重置标记，防止重复销毁
            }

            for (int beeId : currentBees) {
                Bee bee = getBeeById(beeId);
                if (bee != null && bee.isAlive()) {
                    bee.hurt(bee.damageSources().generic(), Float.MAX_VALUE);
                }
            }
            currentBees.clear();
        }

        public boolean shouldBeCleanedUp() {
            // 如果目标不存在、死亡或不在同一世界
            if (target == null || !target.isAlive() || level == null) {
                return true;
            }

            // 如果过程运行时间过长（安全上限）
            if (System.currentTimeMillis() - startTime > MAX_DURATION * 2) {
                return true; // 超过10分钟，强制清理
            }

            return false;
        }

        private void spawnNextWave() {
            if (!isHiveStillValid()) {
                return;
            }
            for (int i = 0; i < beesPerWave; i++) {
                Bee bee = createAngryBee();
                currentBees.add(bee.getId());
                level.addFreshEntity(bee);
            }
        }

        private boolean isHiveStillValid() {
            if (hivePos == null) {
                return false;
            }

            // 检查区块是否加载
            if (!level.isLoaded(hivePos)) {
                return false;
            }

            // 检查该位置是否还是蜂巢方块
            BlockState state = level.getBlockState(hivePos);
            if (!(state.getBlock() instanceof BeehiveBlock)) {
                return false;
            }

            // 检查蜂巢方块实体是否还存在
            return level.getBlockEntity(hivePos) instanceof BeehiveBlockEntity;
        }

        private Bee createAngryBee() {
            Bee bee = new Bee(EntityType.BEE, level) {
                @Override
                public boolean doHurtTarget(Entity target) {
                    // 先调用父类的方法执行攻击
                    boolean result = super.doHurtTarget(target);

                    // 如果攻击成功（刺中了目标），立即杀死蜜蜂
                    if (result) {
                        // 立即死亡，模拟针耗尽
                        this.hurt(this.damageSources().sting(this), Float.MAX_VALUE);
                    }

                    return result;
                }
                @Override
                public void die(DamageSource damageSource) {
                    // 在死亡前设置不掉落物品
                    super.die(damageSource);
                }

                @Override
                protected void dropAllDeathLoot(DamageSource damageSource) {
                    // 完全重写，不调用super
                }

                @Override
                protected void dropFromLootTable(DamageSource damageSource, boolean attackedRecently) {
                    // 阻止从战利品表掉落
                }

                @Override
                protected void dropCustomDeathLoot(DamageSource damageSource, int lootingMultiplier, boolean attackedRecently) {
                    // 阻止自定义掉落
                }

                @Override
                public boolean shouldDropExperience() {
                    return false; // 不掉落经验
                }

                @Override
                public boolean shouldDropLoot() {
                    return false; // 不掉落战利品
                }
            };

            BlockState hiveState = level.getBlockState(hivePos);
            if (hiveState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                Direction facing = hiveState.getValue(HorizontalDirectionalBlock.FACING);
                // 根据蜂巢朝向设置出口位置
                double offsetX = 0;
                double offsetZ = 0;

                switch (facing) {
                    case NORTH -> offsetZ = -0.85; // 北面：Z轴负方向
                    case SOUTH -> offsetZ = 0.85;  // 南面：Z轴正方向
                    case WEST -> offsetX = -0.85;  // 西面：X轴负方向
                    case EAST -> offsetX = 0.85;   // 东面：X轴正方向
                }

                bee.setPos(
                        hivePos.getX() + 0.5 + offsetX,
                        hivePos.getY() + 0.3,
                        hivePos.getZ() + 0.5 + offsetZ
                );
            } else {
                // 默认在蜂巢中心生成
                bee.setPos(hivePos.getX() + 0.5, hivePos.getY() + 0.5, hivePos.getZ() + 0.5);
            }
            if (target instanceof LivingEntity livingTarget) {
                bee.setTarget(livingTarget);
            } else {
                // 如果目标不是生物，蜜蜂就没有目标
                bee.setTarget(null);
            }
            bee.setRemainingPersistentAngerTime(1200); // 60秒愤怒时间

            // 给蜜蜂添加随机偏移
            bee.setDeltaMovement(
                    (level.random.nextDouble() - 0.5) * 0.5,
                    level.random.nextDouble() * 0.5,
                    (level.random.nextDouble() - 0.5) * 0.5
            );

            return bee;
        }

        private Bee getBeeById(int beeId) {
            if (level != null) {
                Entity entity = level.getEntity(beeId);
                return entity instanceof Bee ? (Bee) entity : null;
            }
            return null;
        }
    }
}