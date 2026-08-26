package com.playymcmc007.uselessthings.item;

import com.playymcmc007.uselessthings.block.ExperienceStackedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import com.playymcmc007.uselessthings.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class ExperienceStackedStaffItem extends Item {
    public ExperienceStackedStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState clickedState = level.getBlockState(pos);

        if (!clickedState.canBeReplaced()) {
            pos = pos.relative(context.getClickedFace());
            clickedState = level.getBlockState(pos);
            if (!clickedState.canBeReplaced()) {
                return InteractionResult.PASS;
            }
        }

        if (!level.isClientSide && player != null) {
            if (!player.isCreative()) {
                int requiredExp = getExpForLevel(100);
                int playerExp = getPlayerTotalExperience(player);

                if (playerExp >= requiredExp && player.experienceLevel >= 100) {

                    player.giveExperiencePoints(-requiredExp);
                    boolean isWaterlogged = level.getFluidState(pos).getType() == Fluids.WATER;

                    level.setBlock(pos, ModBlocks.EXPERIENCE_STACKED_BLOCK.get().defaultBlockState()
                            .setValue(ExperienceStackedBlock.WATERLOGGED, isWaterlogged), 3);

                    level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1f, 1f);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                30, 0.5, 0.5, 0.5, 0.1);
                    }
                    stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(context.getHand()));
                    return InteractionResult.SUCCESS;
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§c需要100级经验！当前：" + player.experienceLevel + "级"),
                            true
                    );
                    return InteractionResult.FAIL;
                }
            } else {
                boolean isWaterlogged = level.getFluidState(pos).getType() == Fluids.WATER;
                level.setBlock(pos, ModBlocks.EXPERIENCE_STACKED_BLOCK.get().defaultBlockState()
                        .setValue(ExperienceStackedBlock.WATERLOGGED, isWaterlogged), 3);
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1f, 1f);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private int getExpForLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int)(2.5 * level * level - 40.5 * level + 360);
        return (int)(4.5 * level * level - 162.5 * level + 2220);
    }

    private int getPlayerTotalExperience(Player player) {
        int level = player.experienceLevel;
        int progress = (int)(player.experienceProgress * player.getXpNeededForNextLevel());

        if (level <= 16) {
            return level * level + 6 * level + progress;
        } else if (level <= 31) {
            return (int)(2.5 * level * level - 40.5 * level + 360) + progress;
        } else {
            return (int)(4.5 * level * level - 162.5 * level + 2220) + progress;
        }
    }
}