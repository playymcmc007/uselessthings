package com.playymcmc007.uselessthings;

import com.playymcmc007.uselessthings.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UselessThings.MODID);

    public static final RegistryObject<Item> ARCHITECTURAL_GREENERY = ITEMS.register("architectural_greenery",
            () -> new BlockItem(ModBlocks.ARCHITECTURAL_GREENERY.get(), new Item.Properties()));

    public static final RegistryObject<Item> ARCHITECTURAL_FLOWER = ITEMS.register("architectural_flower",
            () -> new BlockItem(ModBlocks.ARCHITECTURAL_FLOWER.get(), new Item.Properties()));

    public static final RegistryObject<Item> RAINBOW_FLOOD_BUCKET = ITEMS.register("rainbow_flood_bucket",
            () -> new RainbowFloodBucketItem(ModFluids.RAINBOW_FLOOD, new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)));
    public static final RegistryObject<Item> NITWIT_SPRAYER = ITEMS.register("nitwit_sprayer",
            () -> new NitwitSprayerItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(250)
            ));
    public static final RegistryObject<Item> HIVE_POKER = ITEMS.register("hive_poker",
            () -> new Item(new Item
                    .Properties()
                    .durability(1)
            ) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            List<Component> tooltip, TooltipFlag flag) {
                    if (stack.hasTag() && stack.getTag().contains("BoundTargetID")) {
                        int targetID = stack.getTag().getInt("BoundTargetID");
                        String targetName = stack.getTag().getString("BoundTargetName");
                        tooltip.add(Component.literal("已绑定目标: " + targetName));
                        tooltip.add(Component.literal("实体ID: " + targetID));
                    }
                    super.appendHoverText(stack, level, tooltip, flag);
                }
            });
    public static final RegistryObject<Item> PLANT_STAFF = ITEMS.register("plant_staff",
            () -> new BlockItem(ModBlocks.PLANT_STAFF.get(),
                    new Item.Properties().stacksTo(16)) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, level, tooltip, flag);
                    tooltip.add(Component.translatable("tooltip.uselessthings.plant_staff"));
                }
            });
    public static final RegistryObject<Item> ILLUSION_PETAL = ITEMS.register("illusion_petal",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOAT_HORN_SAW = ITEMS.register("goat_horn_saw",
            () -> new GoatHornSawItem(Tiers.IRON, 3, -2.4F, new Item.Properties()
                    .stacksTo(1)
                    .durability(250)));
    public static void register() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
    @SubscribeEvent
    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ARCHITECTURAL_GREENERY.get());
            event.accept(ARCHITECTURAL_FLOWER.get());
        }

        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ARCHITECTURAL_GREENERY.get());
            event.accept(ARCHITECTURAL_FLOWER.get());
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS){
            event.accept(PLANT_STAFF.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RAINBOW_FLOOD_BUCKET.get());
            event.accept(NITWIT_SPRAYER.get());
            event.accept(HIVE_POKER.get());
            event.accept(GOAT_HORN_SAW.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS){
            event.accept(ILLUSION_PETAL.get());
        }
    }
}