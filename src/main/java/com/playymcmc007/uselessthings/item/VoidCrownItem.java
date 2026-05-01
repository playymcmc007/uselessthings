package com.playymcmc007.uselessthings.item;

import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.List;

public class VoidCrownItem extends Item {

    public static final String TAG_ACTIVE = "void_crown_active";

    public VoidCrownItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();

        if (!level.isClientSide()) {
            boolean active = !tag.getBoolean(TAG_ACTIVE);
            tag.putBoolean(TAG_ACTIVE, active);

            player.displayClientMessage(
                    Component.literal(active ? "✦ 虚空之冕 · 觉醒 ✦" : "✦ 虚空之冕 · 沉寂 ✦")
                            .withStyle(active ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)
                            .withStyle(ChatFormatting.BOLD),
                    true
            );
        }

        // 音效 - 纯装饰
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.5F, 0.5F);
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.0F);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // 永久闪光
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean active = tag.getBoolean(TAG_ACTIVE);

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("✦ ❖ ✦ 虚 空 王 冠 ✦ ❖ ✦")
                .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal(" 右键切换特效显示")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal(active ? "◉ 特效: 开启" : "◎ 特效: 关闭")
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("\"虚无之中，唯我独尊\"")
                .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));

        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(TAG_ACTIVE)) {
            return Component.literal("✦ 虚空王冠 ✦")
                    .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD);
        }
        return super.getName(stack);
    }
}