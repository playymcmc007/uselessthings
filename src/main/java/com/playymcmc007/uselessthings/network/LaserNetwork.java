package com.playymcmc007.uselessthings.network;

import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class LaserNetwork {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UselessThings.MODID, "laser"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, LaserTogglePacket.class,
                LaserTogglePacket::encode,
                LaserTogglePacket::decode,
                LaserTogglePacket::handle);
    }
}