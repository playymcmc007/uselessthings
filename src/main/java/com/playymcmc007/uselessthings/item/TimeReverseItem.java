package com.playymcmc007.uselessthings.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class TimeReverseItem extends Item {

    public TimeReverseItem(Properties properties) {
        super(properties);
    }

    @Override
    @ParametersAreNonnullByDefault
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            ServerLevelData levelData = (ServerLevelData) serverLevel.getLevelData();

            long currentTime = levelData.getDayTime();
            long newTime = -currentTime;

            levelData.setDayTime(newTime);

            // 消耗耐久
            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));

            player.displayClientMessage(
                    Component.translatable("message.uselessthings.time_reverse.success")
                            .withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(" " + currentTime + " → " + newTime + " ticks")
                                    .withStyle(ChatFormatting.GOLD)),
                    false
            );

            if (newTime < 0) {
                player.displayClientMessage(
                        Component.translatable("message.uselessthings.time_reverse.negative_time")
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                        true
                );
            }
            else {
                player.displayClientMessage(
                        Component.translatable("message.uselessthings.time_reverse.positive_time")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                        true
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.uselessthings.time_reverse.line1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.uselessthings.time_reverse.line2")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.uselessthings.time_reverse.line3")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}