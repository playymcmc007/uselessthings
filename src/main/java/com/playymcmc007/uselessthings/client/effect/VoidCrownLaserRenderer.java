package com.playymcmc007.uselessthings.client.effect;

import com.mojang.blaze3d.vertex.*;
import com.playymcmc007.uselessthings.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

@OnlyIn(Dist.CLIENT)
public class VoidCrownLaserRenderer {

    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation("minecraft", "textures/misc/white.png");

    private static RenderType laserGlowType() {
        return RenderType.entityTranslucentEmissive(WHITE_TEXTURE);
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!hasActiveCrown(player)) return;

        PoseStack ps = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
        float pt = event.getPartialTick();

        renderLasers(ps, buf, player, cam, pt);
    }

    private static void addVertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                  float x, float y, float z,
                                  float r, float g, float b, float alpha,
                                  float u, float v) {
        vc.vertex(pose, x, y, z)
                .color(r, g, b, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }

    private static void renderLasers(PoseStack ps, MultiBufferSource buf,
                                     LocalPlayer player, Vec3 cam, float pt) {
        VertexConsumer vc = buf.getBuffer(laserGlowType());
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();

        double px = player.getX() - cam.x;
        double py = player.getY() - cam.y;
        double pz = player.getZ() - cam.z;

        float tick = player.tickCount + pt;
        float orbitR = 1.0f;
        float laserLen = 3.0f;
        float laserY = 0.55f;
        float beamW = 0.40f;
        float innerW = 0.22f;
        float coreW = 0.10f;

        for (int i = 0; i < 8; i++) {
            float angle = tick * 0.05f + i * (float) Math.PI * 2 / 8;

            float dx = (float) Math.cos(angle);
            float dz = (float) Math.sin(angle);
            float perpX = -dz;
            float perpZ = dx;

            double sx = px + dx * orbitR;
            double sz = pz + dz * orbitR;
            double sy = py + laserY;
            double ex = px + dx * (orbitR + laserLen);
            double ez = pz + dz * (orbitR + laserLen);
            double ey = sy;

            float timePulse = Mth.sin(tick * 0.1f + i) * 0.08f;

            // 外层光晕
            renderBeamQuad(vc, pose, normal, sx, sy, sz, ex, ey, ez, perpX, perpZ, beamW + 0.15f,
                    1f, 0.85f, 0f, 0.15f + timePulse,
                    1f, 0.75f, 0f, 0.1f + timePulse);

            // 中层
            renderBeamQuad(vc, pose, normal, sx, sy, sz, ex, ey, ez, perpX, perpZ, beamW,
                    1f, 0.9f, 0f, 0.3f + timePulse,
                    1f, 0.85f, 0f, 0.45f + timePulse * 2f);

            // 内层
            renderBeamQuad(vc, pose, normal, sx, sy, sz, ex, ey, ez, perpX, perpZ, innerW,
                    1f, 1f, 0.5f, 0.4f,
                    1f, 0.95f, 0.3f, 0.75f);

            // 白芯
            renderBeamQuad(vc, pose, normal, sx, sy, sz, ex, ey, ez, perpX, perpZ, coreW,
                    1f, 1f, 0.9f, 0.5f,
                    1f, 1f, 0.7f, 0.9f);

            // 端点光球
            float dotSize = 0.25f;
            renderDot(vc, pose, normal, ex, ey, ez, dotSize, 1f, 0.9f, 0f, 0.7f);
            float bigDotSize = 0.45f;
            renderDot(vc, pose, normal, ex, ey, ez, bigDotSize, 1f, 0.8f, 0f, 0.3f);
            float innerDotSize = 0.18f;
            renderDot(vc, pose, normal, sx, sy, sz, innerDotSize, 1f, 0.7f, 0f, 0.4f);
        }

        // 轨道环
        int ringSegs = 64;
        float ringY = (float) py + laserY;
        float ringR = orbitR + 0.1f;
        float ringW = 0.06f;

        for (int i = 0; i < ringSegs; i++) {
            double a1 = Math.PI * 2 * i / ringSegs;
            double a2 = Math.PI * 2 * (i + 1) / ringSegs;

            float x1 = (float) (px + Math.cos(a1) * ringR);
            float z1 = (float) (pz + Math.sin(a1) * ringR);
            float x2 = (float) (px + Math.cos(a2) * ringR);
            float z2 = (float) (pz + Math.sin(a2) * ringR);

            float nx1 = -(float) Math.sin(a1), nz1 = (float) Math.cos(a1);
            float nx2 = -(float) Math.sin(a2), nz2 = (float) Math.cos(a2);

            float rAlpha = 0.35f + Mth.sin(tick * 0.08f) * 0.1f;

            addVertex(vc, pose, normal, x1 + nx1 * (ringW + 0.04f), ringY, z1 + nz1 * (ringW + 0.04f),
                    1f, 0.7f, 0f, rAlpha * 0.5f, 0, 0);
            addVertex(vc, pose, normal, x2 + nx2 * (ringW + 0.04f), ringY, z2 + nz2 * (ringW + 0.04f),
                    1f, 0.7f, 0f, rAlpha * 0.5f, 1, 0);
            addVertex(vc, pose, normal, x2 - nx2 * (ringW + 0.04f), ringY, z2 - nz2 * (ringW + 0.04f),
                    1f, 0.5f, 0f, rAlpha * 0.2f, 1, 1);
            addVertex(vc, pose, normal, x1 - nx1 * (ringW + 0.04f), ringY, z1 - nz1 * (ringW + 0.04f),
                    1f, 0.5f, 0f, rAlpha * 0.2f, 0, 1);

            addVertex(vc, pose, normal, x1 + nx1 * ringW, ringY, z1 + nz1 * ringW,
                    1f, 0.9f, 0.2f, rAlpha, 0, 0);
            addVertex(vc, pose, normal, x2 + nx2 * ringW, ringY, z2 + nz2 * ringW,
                    1f, 0.9f, 0.2f, rAlpha, 1, 0);
            addVertex(vc, pose, normal, x2 - nx2 * ringW, ringY, z2 - nz2 * ringW,
                    1f, 0.7f, 0f, rAlpha * 0.6f, 1, 1);
            addVertex(vc, pose, normal, x1 - nx1 * ringW, ringY, z1 - nz1 * ringW,
                    1f, 0.7f, 0f, rAlpha * 0.6f, 0, 1);
        }
    }

    private static void renderBeamQuad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                       double sx, double sy, double sz,
                                       double ex, double ey, double ez,
                                       float perpX, float perpZ, float width,
                                       float rs, float gs, float bs, float as,
                                       float re, float ge, float be, float ae) {
        float w = width;
        addVertex(vc, pose, normal, (float) sx + perpX * w, (float) sy, (float) sz + perpZ * w, rs, gs, bs, as, 0, 0);
        addVertex(vc, pose, normal, (float) ex + perpX * w, (float) ey, (float) ez + perpZ * w, re, ge, be, ae, 1, 0);
        addVertex(vc, pose, normal, (float) ex - perpX * w, (float) ey, (float) ez - perpZ * w, re, ge, be, ae, 1, 1);
        addVertex(vc, pose, normal, (float) sx - perpX * w, (float) sy, (float) sz - perpZ * w, rs, gs, bs, as, 0, 1);
    }

    private static void renderDot(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                  double x, double y, double z, float size,
                                  float r, float g, float b, float alpha) {
        addVertex(vc, pose, normal, (float) x - size, (float) y, (float) z - size, r, g, b, alpha, 0, 0);
        addVertex(vc, pose, normal, (float) x + size, (float) y, (float) z - size, r, g, b, alpha, 1, 0);
        addVertex(vc, pose, normal, (float) x + size, (float) y, (float) z + size, r, g, b, alpha * 0.7f, 1, 1);
        addVertex(vc, pose, normal, (float) x - size, (float) y, (float) z + size, r, g, b, alpha * 0.7f, 0, 1);
    }

    private static boolean hasActiveCrown(LocalPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.VOID_CROWN.get()
                    && stack.getOrCreateTag().getBoolean("void_crown_active")) {
                return true;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.getItem() == ModItems.VOID_CROWN.get()
                && offhand.getOrCreateTag().getBoolean("void_crown_active");
    }
}