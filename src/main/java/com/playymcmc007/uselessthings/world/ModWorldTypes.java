package com.playymcmc007.uselessthings.world;

import com.mojang.serialization.Codec;
import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModWorldTypes {

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATOR_CODECS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, UselessThings.MODID);

    public static final RegistryObject<Codec<NoStoneChunkGenerator>> NO_STONE_CHUNK_GENERATOR =
            CHUNK_GENERATOR_CODECS.register("no_stone_generator", () -> NoStoneChunkGenerator.CODEC);

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATOR_CODECS.register(modEventBus);
    }
}