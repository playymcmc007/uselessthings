package com.playymcmc007.uselessthings;

import com.playymcmc007.uselessthings.block.SuperChunkBlock;
import com.playymcmc007.uselessthings.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
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
    public static final RegistryObject<Item> SUPER_CHUNK_BLOCK_TICKET = ITEMS.register("super_chunk_block_ticket",
            () -> new BlockItem(ModBlocks.SUPER_CHUNK_BLOCK.get(), new Item.Properties()) {
                @Override
                protected BlockState getPlacementState(BlockPlaceContext context) {
                    BlockState state = super.getPlacementState(context);
                    if (state != null) {
                        state = state.setValue(SuperChunkBlock.DIRECT_MODE, false);
                    }
                    return state;
                }
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, level, tooltip, flag);
                    tooltip.add(Component.translatable("tooltip.uselessthings.super_chunk_block.ticket1"));
                    tooltip.add(Component.translatable("tooltip.uselessthings.super_chunk_block.ticket2"));
                }
                @Override
                public String getDescriptionId() {
                    return "block.uselessthings.super_chunk_block.ticket";
                }
            });

    public static final RegistryObject<Item> SUPER_CHUNK_BLOCK_DIRECT = ITEMS.register("super_chunk_block_direct",
            () -> new BlockItem(ModBlocks.SUPER_CHUNK_BLOCK.get(), new Item.Properties()) {
                @Override
                protected BlockState getPlacementState(BlockPlaceContext context) {
                    BlockState state = super.getPlacementState(context);
                    if (state != null) {
                        // 直接模式 (true)
                        state = state.setValue(SuperChunkBlock.DIRECT_MODE, true);
                    }
                    return state;
                }

                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, level, tooltip, flag);
                    tooltip.add(Component.translatable("tooltip.uselessthings.super_chunk_block.direct1"));
                    tooltip.add(Component.translatable("tooltip.uselessthings.super_chunk_block.direct2"));
                }
                @Override
                public String getDescriptionId() {
                    return "block.uselessthings.super_chunk_block.direct";
                }
            });
    public static final RegistryObject<Item> SUPER_BONE_MEAL = ITEMS.register("super_bone_meal",
            () -> new SuperBoneMealItem(new Item.Properties()));
    public static final RegistryObject<Item> SUPER_OAK_SAPLING = ITEMS.register("super_oak_sapling",
            () -> new BlockItem(ModBlocks.SUPER_OAK_SAPLING.get(), new Item.Properties()));

    public static final RegistryObject<Item> SUPER_SPRUCE_SAPLING = ITEMS.register("super_spruce_sapling",
            () -> new BlockItem(ModBlocks.SUPER_SPRUCE_SAPLING.get(), new Item.Properties()));

    public static final RegistryObject<Item> SUPER_BIRCH_SAPLING = ITEMS.register("super_birch_sapling",
            () -> new BlockItem(ModBlocks.SUPER_BIRCH_SAPLING.get(), new Item.Properties()));

    public static final RegistryObject<Item> SUPER_JUNGLE_SAPLING = ITEMS.register("super_jungle_sapling",
            () -> new BlockItem(ModBlocks.SUPER_JUNGLE_SAPLING.get(), new Item.Properties()));

    public static final RegistryObject<Item> SUPER_ACACIA_SAPLING = ITEMS.register("super_acacia_sapling",
            () -> new BlockItem(ModBlocks.SUPER_ACACIA_SAPLING.get(), new Item.Properties()));

    public static final RegistryObject<Item> SUPER_DARK_OAK_SAPLING = ITEMS.register("super_dark_oak_sapling",
            () -> new BlockItem(ModBlocks.SUPER_DARK_OAK_SAPLING.get(), new Item.Properties()));

    public static final RegistryObject<Item> SUPER_MANGROVE_PROPAGULE = ITEMS.register("super_mangrove_propagule",
            () -> new BlockItem(ModBlocks.SUPER_MANGROVE_PROPAGULE.get(), new Item.Properties()));

    public static final RegistryObject<Item> SUPER_CHERRY_SAPLING = ITEMS.register("super_cherry_sapling",
            () -> new BlockItem(ModBlocks.SUPER_CHERRY_SAPLING.get(), new Item.Properties()));
    public static final RegistryObject<Item> TIME_REVERSE = ITEMS.register("time_reverse",
            () -> new TimeReverseItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(2)
                    .rarity(Rarity.EPIC)
            ));
    public static final RegistryObject<Item> VOID_CROWN = ITEMS.register("void_crown",
            () -> new VoidCrownItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .fireResistant()
            ));

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
            event.accept(SUPER_OAK_SAPLING.get());
            event.accept(SUPER_SPRUCE_SAPLING.get());
            event.accept(SUPER_BIRCH_SAPLING.get());
            event.accept(SUPER_JUNGLE_SAPLING.get());
            event.accept(SUPER_ACACIA_SAPLING.get());
            event.accept(SUPER_DARK_OAK_SAPLING.get());
            event.accept(SUPER_MANGROVE_PROPAGULE.get());
            event.accept(SUPER_CHERRY_SAPLING.get());
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS){
            event.accept(PLANT_STAFF.get());
            event.accept(SUPER_CHUNK_BLOCK_TICKET.get());
            event.accept(SUPER_CHUNK_BLOCK_DIRECT.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RAINBOW_FLOOD_BUCKET.get());
            event.accept(NITWIT_SPRAYER.get());
            event.accept(HIVE_POKER.get());
            event.accept(GOAT_HORN_SAW.get());
            event.accept(TIME_REVERSE.get());
            event.accept(VOID_CROWN.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS){
            event.accept(ILLUSION_PETAL.get());
            event.accept(SUPER_BONE_MEAL.get());
        }
    }
}