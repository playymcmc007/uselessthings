// ModPotions.java
package com.playymcmc007.uselessthings;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, UselessThings.MODID);

    public static final RegistryObject<Potion> CHAOS_POTION = POTIONS.register(
            "chaos_potion",
            () -> new Potion(
                    new MobEffectInstance(ModEffects.CHAOS_EFFECT.get(), 1, 0)
            )
    );

    public static final RegistryObject<Potion> STRONG_CHAOS_POTION = POTIONS.register(
            "strong_chaos_potion",
            () -> new Potion(
                    "chaos_potion",
                    new MobEffectInstance(ModEffects.CHAOS_EFFECT.get(), 1, 1)
            )
    );

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}