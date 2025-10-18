package com.playymcmc007.uselessthings.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class RainbowFloodBucketItem extends BucketItem {
    public RainbowFloodBucketItem(Supplier<? extends Fluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!level.isClientSide && entity instanceof LivingEntity livingEntity) {
            if (isInPlayerInventory(livingEntity, stack)) {
                applyContinuousDamage(livingEntity);
            }
        }
    }

    private boolean isInPlayerInventory(LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player player) {
            if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) {
                return true;
            }
            for (ItemStack inventoryStack : player.getInventory().items) {
                if (inventoryStack == stack) {
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (!level.isClientSide && result.getResult().consumesAction()) {
            if (!player.getAbilities().instabuild) {
                stack.setCount(0);
                return InteractionResultHolder.sidedSuccess(ItemStack.EMPTY, level.isClientSide());
            }
        }

        return result;
    }
    private void applyContinuousDamage(LivingEntity entity) {
        if (entity.tickCount % 10 == 0) {
            entity.hurt(entity.damageSources().magic(), 1.0F);
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING,
                    40, 0, false, false
            ));
        }
    }
}