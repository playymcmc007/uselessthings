package com.playymcmc007.uselessthings.entity;

import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UselessThings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModAttributes {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // 将 TestEntity 的属性注册到对应的实体类型
        event.put(ModEntities.TEST_ENTITY.get(), TestEntity.createAttributes().build());
    }
}