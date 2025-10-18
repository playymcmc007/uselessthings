package com.playymcmc007.uselessthings.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class RainbowFloodType extends FluidType {
    private static final ResourceLocation STILL_TEXTURE = new ResourceLocation("block/water_still");
    private static final ResourceLocation FLOWING_TEXTURE = new ResourceLocation("block/water_flow");

    public RainbowFloodType(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                if (pos != null) {
                    long gameTime = getter instanceof Level ? ((Level) getter).getGameTime() : System.currentTimeMillis() / 50;
                    return calculateColorBasedOnPosition(pos, gameTime);
                }
                return getTintColor();
            }
            @Override
            public @NotNull Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                if (level != null && camera.getPosition() != null) {
                    BlockPos pos = new BlockPos((int)camera.getPosition().x, (int)camera.getPosition().y, (int)camera.getPosition().z);
                    int color = calculateColorBasedOnPosition(pos, level.getGameTime());

                    float r = ((color >> 16) & 0xFF) / 255.0F * 0.7f;
                    float g = ((color >> 8) & 0xFF) / 255.0F * 0.7f;
                    float b = (color & 0xFF) / 255.0F * 0.7f;

                    return new Vector3f(r, g, b);
                }
                return new Vector3f(0.4f, 0.4f, 0.8f);
            }

            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    if (mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                        RenderSystem.setShaderFogStart(2.0f);
                        RenderSystem.setShaderFogEnd(32.0f);
                    }
                    else if (mc.player.hasEffect(MobEffects.WATER_BREATHING)) {
                        RenderSystem.setShaderFogStart(1.5f);
                        RenderSystem.setShaderFogEnd(28.0f);
                    }
                    else {
                        RenderSystem.setShaderFogStart(1.0f);
                        RenderSystem.setShaderFogEnd(24.0f);
                    }
                } else {
                    RenderSystem.setShaderFogStart(1.0f);
                    RenderSystem.setShaderFogEnd(24.0f);
                }
            }
        });
    }

    public static int calculateColorBasedOnPosition(BlockPos pos, long gameTime) {
        if (pos == null) return 0xFFFFFFFF;

        double positionBase = (pos.getX() * 0.04 + pos.getZ() * 0.02) % 1.0;

        double timeFlow = (gameTime % 200) / 200.0;

        double hue = (positionBase + timeFlow) % 1.0;

        return java.awt.Color.HSBtoRGB((float)hue, 0.88f, 0.96f);
    }
}