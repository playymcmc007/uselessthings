package com.playymcmc007.uselessthings.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;

public class TouchOfZeroItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger("TouchOfZero");

    public TouchOfZeroItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            BlockPos pos = player.blockPosition();
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            int minY = serverLevel.getMinBuildHeight();
            int maxY = serverLevel.getMaxBuildHeight();

            AABB chunkBox = new AABB(
                    chunkX * 16.0, minY, chunkZ * 16.0,
                    chunkX * 16.0 + 16.0, maxY, chunkZ * 16.0 + 16.0
            );

            if (hasUltimateSkeletonsEntity(serverLevel, chunkBox)) {
                // 自定义断开消息
                Component reason = Component.literal("杀不死我还躲不起吗？");
                // 向所有玩家发送系统消息（可选）
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(reason, false);

                // 手动断开所有玩家，并传入自定义原因
                for (ServerPlayer serverPlayer : serverLevel.getServer().getPlayerList().getPlayers()) {
                    serverPlayer.connection.disconnect(reason);
                }

                // 强制关闭服务器（此时玩家已断开，不会显示默认消息）
                serverLevel.getServer().halt(false);

                // 返回成功（虽然服务器即将关闭）
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            List<Entity> entities = serverLevel.getEntities(null, chunkBox);
            entities.removeIf(e -> e instanceof Player);

            if (entities.isEmpty()) {
                player.displayClientMessage(Component.literal("你触碰到了虚空，但此处空无一物"), true);
            } else {
                int erased = 0;
                for (Entity entity : entities) {
                    if (tryEraseEntity(serverLevel, entity)) erased++;
                }
                player.displayClientMessage(Component.literal("归零之触轻抚区块，" + erased + " 个存在归于虚无"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private boolean hasUltimateSkeletonsEntity(ServerLevel level, AABB box) {
        List<Entity> entities = level.getEntities(null, box);
        for (Entity e : entities) {
            // 正确获取实体的注册名
            net.minecraft.resources.ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            if (key != null && "ultimateskeletons".equals(key.getNamespace())) {
                return true;
            }
        }
        return false;
    }

    private boolean tryEraseEntity(ServerLevel level, Entity entity) {
        String entityName = entity.getName().getString();

        // 第1层：遍历所有伤害类型
        if (entity instanceof LivingEntity living) {
            if (tryAllDamageTypes(living, level)) {
                return true;
            } else {
                LOGGER.warn("实体 {} (UUID: {}) 所有伤害类型均未生效，尝试下一层", entityName, entity.getUUID());
            }
        }

        // 第2层：强制负血量
        if (entity instanceof LivingEntity living) {
            living.setHealth(-Float.MAX_VALUE);
            if (!living.isAlive()) {
                return true;
            } else {
                LOGGER.warn("实体 {} (UUID: {}) 强制负血量未生效，尝试下一层", entityName, entity.getUUID());
            }
        }

        // 第3层：kill()
        entity.kill();
        if (!entity.isAlive()) {
            return true;
        } else {
            LOGGER.warn("实体 {} (UUID: {}) kill() 未生效，尝试下一层", entityName, entity.getUUID());
        }

        // 第4层：remove(DISCARDED)
        entity.remove(Entity.RemovalReason.DISCARDED);
        if (entity.isRemoved()) {
            return true;
        } else {
            LOGGER.warn("实体 {} (UUID: {}) remove(DISCARDED) 未生效，尝试下一层", entityName, entity.getUUID());
        }

        // 第5层：discard()
        entity.discard();
        if (entity.isRemoved()) {
            return true;
        } else {
            LOGGER.warn("实体 {} (UUID: {}) discard() 未生效，尝试下一层", entityName, entity.getUUID());
        }

        // 所有手段均失败
        LOGGER.warn("实体 {} (UUID: {}) 所有抹除手段均失败，可能为不可移除实体", entityName, entity.getUUID());
        return false;
    }

    private boolean tryAllDamageTypes(LivingEntity living, ServerLevel level) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (Holder.Reference<DamageType> holder : registry.holders().toList()) {
            DamageSource source = new DamageSource(holder);
            living.hurt(source, Float.MAX_VALUE);
            if (!living.isAlive()) {
                return true;
            }
        }
        return false;
    }
}