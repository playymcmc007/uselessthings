package com.playymcmc007.uselessthings;

import com.playymcmc007.uselessthings.effect.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, UselessThings.MODID);

    public static final RegistryObject<MobEffect> CHAOS_EFFECT = EFFECTS.register(
            "chaos",
            ChaosEffect::new
    );
    public static final RegistryObject<MobEffect> SUPER_LASER_EFFECT = EFFECTS.register(
            "super_laser",
            SuperLaserEffect::new
    );
    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}