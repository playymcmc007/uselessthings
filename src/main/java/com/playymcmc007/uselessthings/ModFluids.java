package com.playymcmc007.uselessthings;

import com.playymcmc007.uselessthings.block.RainbowFloodBlock;
import com.playymcmc007.uselessthings.fluid.RainbowFloodType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, UselessThings.MODID);

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, UselessThings.MODID);

    public static final RegistryObject<FluidType> RAINBOW_FLOOD_TYPE = FLUID_TYPES.register("rainbow_flood",
            () -> new RainbowFloodType(FluidType.Properties.create()
                    .density(1500).viscosity(1000).temperature(300)));

    public static final RegistryObject<FlowingFluid> RAINBOW_FLOOD = FLUIDS.register("rainbow_flood",
            () -> new ForgeFlowingFluid.Source(ModFluids.RAINBOW_PROPERTIES));

    public static final RegistryObject<FlowingFluid> RAINBOW_FLOOD_FLOWING = FLUIDS.register("rainbow_flood_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.RAINBOW_PROPERTIES));

    public static final RegistryObject<LiquidBlock> RAINBOW_FLOOD_BLOCK = ModBlocks.BLOCKS.register("rainbow_flood_block",
            () -> new RainbowFloodBlock(ModFluids.RAINBOW_FLOOD, Block.Properties.of()
                    .noCollission().strength(100.0F).noLootTable()));

    public static final ForgeFlowingFluid.Properties RAINBOW_PROPERTIES = new ForgeFlowingFluid.Properties(
            RAINBOW_FLOOD_TYPE, RAINBOW_FLOOD, RAINBOW_FLOOD_FLOWING)
            .block(ModFluids.RAINBOW_FLOOD_BLOCK)
            .bucket(ModItems.RAINBOW_FLOOD_BUCKET)
            .tickRate(2)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1);

    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
        FLUID_TYPES.register(eventBus);
    }
}