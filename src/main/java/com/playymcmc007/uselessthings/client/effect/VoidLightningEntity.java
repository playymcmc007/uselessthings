package com.playymcmc007.uselessthings.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.List;

public class VoidLightningEntity extends LightningBolt {

    private int life = 4; // 闪电存在tick数
    private boolean isVoid = true;

    public VoidLightningEntity(EntityType<? extends LightningBolt> type, Level level) {
        super(type, level);
    }

    /**
     * 创建纯伤害闪电（不破坏方块、不产生火焰、不生成骷髅陷阱）
     */
    public static VoidLightningEntity create(ServerLevel level, Vec3 pos, @Nullable LivingEntity cause) {
        VoidLightningEntity bolt = new VoidLightningEntity(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(pos.x, pos.y, pos.z);
        bolt.setCause(cause instanceof net.minecraft.world.entity.player.Player ?
                (net.minecraft.server.level.ServerPlayer) cause : null);
        bolt.isVoid = true;
        return bolt;
    }

    @Override
    public void tick() {
        this.baseTick();
        this.life--;

        if (this.life <= 0) {
            this.discard();
        }
    }
    @Override
    public void baseTick() {
        this.setOldPosAndRot();
    }
    @Override
    public void setVisualOnly(boolean visualOnly) {
        super.setVisualOnly(true); // 不产生方块效果
    }

    // 核心：重写伤害逻辑
    public void applyDamage(LivingEntity cause, ServerLevel level) {
        this.setPos(this.getX(), this.getY(), this.getZ());

        // 不产生火焰
        // 不破坏方块——调用父类方法但覆盖火焰生成

        // 闪电自带的基础伤害逻辑会正常触发
        // 我们额外增强：对周围所有实体造成伤害
        AABB area = new AABB(
                this.getX() - 3.0, this.getY(), this.getZ() - 3.0,
                this.getX() + 3.0, this.getY() + 6.0, this.getZ() + 3.0
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != cause);

        for (LivingEntity target : targets) {
            // 造成雷击伤害（5点 = 2.5心）
            target.hurt(level.damageSources().lightningBolt(), 5.0f);
        }
    }
}