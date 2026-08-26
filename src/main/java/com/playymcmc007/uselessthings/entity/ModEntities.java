package com.playymcmc007.uselessthings.entity;

import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, UselessThings.MODID);

    public static final RegistryObject<EntityType<TestEntity>> TEST_ENTITY =
            ENTITY_TYPES.register("test_entity",
                    () -> EntityType.Builder.of(TestEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.5f)
                            .build(new ResourceLocation(UselessThings.MODID, "test_entity").toString()));

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}