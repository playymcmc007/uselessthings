package com.playymcmc007.uselessthings;

import com.playymcmc007.uselessthings.item.RainbowFloodBucketItem;
import net.minecraft.world.item.*;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UselessThings.MODID);

    // 注册建筑绿植物品
    public static final RegistryObject<Item> ARCHITECTURAL_GREENERY = ITEMS.register("architectural_greenery",
            () -> new BlockItem(ModBlocks.ARCHITECTURAL_GREENERY.get(), new Item.Properties()));

    // 注册建筑花物品
    public static final RegistryObject<Item> ARCHITECTURAL_FLOWER = ITEMS.register("architectural_flower",
            () -> new BlockItem(ModBlocks.ARCHITECTURAL_FLOWER.get(), new Item.Properties()));

    public static final RegistryObject<Item> RAINBOW_FLOOD_BUCKET = ITEMS.register("rainbow_flood_bucket",
            () -> new RainbowFloodBucketItem(ModFluids.RAINBOW_FLOOD, new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)));
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
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RAINBOW_FLOOD_BUCKET.get());
        }
    }
}