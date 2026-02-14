package com.playymcmc007.uselessthings.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;

import javax.annotation.Nonnull;

public class GoatHornSawItem extends AxeItem {

    public GoatHornSawItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, @Nonnull LivingEntity attacker) {
        if (target instanceof Goat goat) {
            if (!target.level().isClientSide) {
                if (goat.hasLeftHorn() && goat.hasRightHorn()) {
                    goat.dropHorn();
                    goat.dropHorn();
                } else {
                    if (goat.hasLeftHorn()) {
                        goat.dropHorn();
                    }
                    if (goat.hasRightHorn()) {
                        goat.dropHorn();
                    }
                }

                target.level().playSound(null, target.blockPosition(),
                        SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);

                DamageSource damageSource;
                if (attacker instanceof Player player) {
                    damageSource = attacker.damageSources().playerAttack(player);
                } else {
                    damageSource = attacker.damageSources().mobAttack(attacker);
                }

                boolean actuallyHurt = goat.hurt(damageSource, 100.0F);

                if (actuallyHurt && goat.isDeadOrDying()) {
                    target.level().playSound(null, target.blockPosition(),
                            SoundEvents.GOAT_DEATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }

                if (attacker instanceof Player player) {
                    if (!player.getAbilities().instabuild) {
                        stack.hurtAndBreak(2, player, (p) -> p.broadcastBreakEvent(p.getUsedItemHand()));
                    }
                }

                return actuallyHurt;
            }
            return false;
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == net.minecraft.world.item.Items.IRON_INGOT;
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return super.canApplyAtEnchantingTable(stack, enchantment) ||
                enchantment.category == EnchantmentCategory.WEAPON ||
                enchantment.category == EnchantmentCategory.DIGGER ||
                enchantment == Enchantments.BLOCK_FORTUNE ||
                enchantment == Enchantments.SILK_TOUCH;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }
}