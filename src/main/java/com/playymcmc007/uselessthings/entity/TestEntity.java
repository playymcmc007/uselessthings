package com.playymcmc007.uselessthings.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// 改为继承 Monster（敌对生物）
public class TestEntity extends Monster {
    public TestEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // 浮水
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 攻击玩家（近战）
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        // 随机漫步
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        // 看向玩家
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // 随机转头
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        // 目标选择：受到伤害时反击
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        // 目标选择：主动攻击玩家
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)      // 生命值 30
                .add(Attributes.MOVEMENT_SPEED, 0.35D)  // 移动速度（比普通生物快）
                .add(Attributes.ATTACK_DAMAGE, 4.0D)    // 攻击力 4（2颗心）
                .add(Attributes.FOLLOW_RANGE, 16.0D);   // 追踪范围 16格
    }
}