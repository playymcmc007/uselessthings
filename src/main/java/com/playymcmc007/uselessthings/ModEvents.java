package com.playymcmc007.uselessthings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;
@Mod.EventBusSubscriber(modid = UselessThings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {
    private static final Random RANDOM = new Random();
    @SubscribeEvent
    public static void addWanderingTraderTrades(WandererTradesEvent event) {
        var trades = event.getGenericTrades();

        int randomPosition = 1 + RANDOM.nextInt(Math.max(1, trades.size()));
        trades.add(randomPosition, (trader, random) -> {
            ItemStack banner = createOminousBanner();
            return new MerchantOffer(
                    banner,
                    PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.WEAKNESS),
                    new ItemStack(ModItems.NITWIT_SPRAYER.get()),
                    1, 0, 0.0f
            );
        });
    }
    //不详旗帜的代码太麻烦了，写了两套备用方案防止有mod把格式崩了
    private static ItemStack createOminousBanner() {
        ItemStack banner = new ItemStack(Items.WHITE_BANNER, 5);

        String nbtString = "{BlockEntityTag:{Patterns:[{Color:9,Pattern:\"mr\"},{Color:8,Pattern:\"bs\"},{Color:7,Pattern:\"cs\"},{Color:8,Pattern:\"bo\"},{Color:15,Pattern:\"ms\"},{Color:8,Pattern:\"hh\"},{Color:8,Pattern:\"mc\"},{Color:15,Pattern:\"bo\"}],id:\"minecraft:banner\"},HideFlags:32,display:{Name:'{\"color\":\"gold\",\"translate\":\"block.minecraft.ominous_banner\"}'}}";

        CompoundTag tag = parseNBTString(nbtString);
        if (tag != null) {
            banner.setTag(tag);
        } else {
            tag = buildNBTManually();
            banner.setTag(tag);
        }

        return banner;
    }

    private static CompoundTag parseNBTString(String nbtString) {
        try {
            return net.minecraft.nbt.TagParser.parseTag(nbtString);
        } catch (Exception e) {
            return null;
        }
    }

    private static CompoundTag buildNBTManually() {
        CompoundTag tag = new CompoundTag();

        ListTag patterns = new ListTag();
        patterns.add(createPatternTag(9, "mr"));
        patterns.add(createPatternTag(8, "bs"));
        patterns.add(createPatternTag(7, "cs"));
        patterns.add(createPatternTag(8, "bo"));
        patterns.add(createPatternTag(15, "ms"));
        patterns.add(createPatternTag(8, "hh"));
        patterns.add(createPatternTag(8, "mc"));
        patterns.add(createPatternTag(15, "bo"));

        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.put("Patterns", patterns);
        blockEntityTag.putString("id", "minecraft:banner");
        tag.put("BlockEntityTag", blockEntityTag);

        CompoundTag displayTag = new CompoundTag();
        displayTag.putString("Name", "{\"color\":\"gold\",\"translate\":\"block.minecraft.ominous_banner\"}");
        tag.put("display", displayTag);

        tag.putInt("HideFlags", 32);

        return tag;
    }

    private static CompoundTag createPatternTag(int color, String pattern) {
        CompoundTag patternTag = new CompoundTag();
        patternTag.putInt("Color", color);
        patternTag.putString("Pattern", pattern);
        return patternTag;
    }
}