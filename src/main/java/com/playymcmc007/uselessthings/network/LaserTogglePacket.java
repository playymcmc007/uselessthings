package com.playymcmc007.uselessthings.network;

import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LaserTogglePacket {
    private final boolean active;

    public LaserTogglePacket(boolean active) {
        this.active = active;
    }

    public static void encode(LaserTogglePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static LaserTogglePacket decode(FriendlyByteBuf buf) {
        return new LaserTogglePacket(buf.readBoolean());
    }

    public static void handle(LaserTogglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                LaserManager.setLaserActive(player, msg.active);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}