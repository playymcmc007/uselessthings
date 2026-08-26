package com.playymcmc007.uselessthings.network;

import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class TeleportPacket {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UselessThings.MODID, "teleport"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private final ResourceKey<Level> targetDimension;

    public TeleportPacket(ResourceKey<Level> targetDimension) {
        this.targetDimension = targetDimension;
    }

    public TeleportPacket(FriendlyByteBuf buf) {
        this.targetDimension = ResourceKey.create(
                Registries.DIMENSION,
                buf.readResourceLocation()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(targetDimension.location());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // 直接传送玩家
                teleportPlayer(player, targetDimension);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void teleportPlayer(ServerPlayer player, ResourceKey<Level> targetDim) {
        ServerLevel targetLevel = player.getServer().getLevel(targetDim);
        if (targetLevel != null) {
            player.teleportTo(
                    targetLevel,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYRot(),
                    player.getXRot()
            );
        }
    }

    public static void register() {
        CHANNEL.registerMessage(
                0,
                TeleportPacket.class,
                TeleportPacket::encode,
                TeleportPacket::new,
                TeleportPacket::handle
        );
    }
}