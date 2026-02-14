// ChaosEffect.java
package com.playymcmc007.uselessthings.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ChaosEffect extends MobEffect {

    public ChaosEffect() {
        super(MobEffectCategory.NEUTRAL, 0x9370DB);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            applyRandomEffects(entity, amplifier);
            entity.removeEffect(this);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    private void applyRandomEffects(LivingEntity entity, int chaosAmplifier) {
        RandomSource random = entity.getRandom();
        int effectDuration = 1200 * (chaosAmplifier + 1);
        List<MobEffectInstance> positiveEffects = new ArrayList<>();
        List<MobEffectInstance> negativeEffects = new ArrayList<>();
        List<MobEffectInstance> neutralEffects = new ArrayList<>();

        for (net.minecraft.world.effect.MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            if (effect == this) continue;

            MobEffectCategory category = effect.getCategory();
            int effectAmplifier = chaosAmplifier;
            MobEffectInstance instance = new MobEffectInstance(effect, effectDuration, effectAmplifier);
            switch (category) {
                case BENEFICIAL:
                    positiveEffects.add(instance);
                    break;
                case HARMFUL:
                    negativeEffects.add(instance);
                    break;
                case NEUTRAL:
                    neutralEffects.add(instance);
                    break;
            }
        }

        int choice = random.nextInt(3);

        switch (choice) {
            case 0:
                for (MobEffectInstance effect : positiveEffects) {
                    entity.addEffect(effect);
                }
                if (entity instanceof Player player) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§a幸运降临！获得了所有正面效果！"),
                            true
                    );
                }
                break;

            case 1:
                for (MobEffectInstance effect : negativeEffects) {
                    entity.addEffect(effect);
                }
                if (entity instanceof Player player) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§c厄运缠身！获得了所有负面效果！"),
                            true
                    );
                }
                break;

            case 2:
                for (MobEffectInstance effect : neutralEffects) {
                    entity.addEffect(effect);
                }
                if (entity instanceof Player player) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§7平衡之道！获得了所有中性效果！"),
                            true
                    );
                }
                break;
        }
    }
}