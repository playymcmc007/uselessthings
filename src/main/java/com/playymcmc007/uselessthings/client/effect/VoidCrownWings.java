package com.playymcmc007.uselessthings.client.effect;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

@OnlyIn(Dist.CLIENT)
public class VoidCrownWings {

    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation("minecraft", "textures/misc/white.png");
    private static RenderType wingType() {
        return RenderType.entityTranslucentEmissive(WHITE_TEXTURE);
    }

    private static float wingFlap = 0f;
    private static float prevWingFlap = 0f;
    private static float wingFlapSpeed = 0f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!hasActiveCrown(player)) return;

        prevWingFlap = wingFlap;

        // 移动/跳跃时翅膀扇动快，静止时慢
        float targetSpeed = player.isSprinting() ? 0.3f :
                player.isCrouching() ? 0.05f :
                        player.walkAnimation.speed() > 0.1f ? 0.15f : 0.03f;

        wingFlapSpeed += (targetSpeed - wingFlapSpeed) * 0.1f;
        wingFlap += wingFlapSpeed;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;
        if (!hasActiveCrown(localPlayer)) return;
        if (event.getEntity() != localPlayer) return;

        PoseStack ps = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        float pt = event.getPartialTick();

        renderWings(ps, buf, pt, localPlayer);
    }

    private static void renderWings(PoseStack ps, MultiBufferSource buf, float pt, LocalPlayer player) {
        VertexConsumer vc = buf.getBuffer(wingType());

        float flap = Mth.lerp(pt, prevWingFlap, wingFlap);
        float flapAngle = Math.abs(Mth.sin(flap)) * 25f; // 扇动角度 ±25度

        ps.pushPose();

        // 翅膀在玩家背部，身体后方
        ps.translate(0, 1.35, 0);
        ps.mulPose(Axis.YP.rotationDegrees(180 - player.yBodyRot));
        ps.scale(1.8f, 1.8f, 1.8f);

        float alpha = 0.85f + Mth.sin(flap * 0.7f) * 0.1f;  // 0.65 → 0.85

        // === 左翅 ===
        ps.pushPose();
        ps.translate(-0.15f, 0, 0.1f);
        ps.mulPose(Axis.ZP.rotationDegrees(20 - flapAngle)); // Z轴倾斜 + 扇动
        ps.mulPose(Axis.YP.rotationDegrees(25));             // 向后展开
        renderWing(ps, vc, alpha, true);
        ps.popPose();

        // === 右翅 ===
        ps.pushPose();
        ps.translate(0.15f, 0, 0.1f);
        ps.mulPose(Axis.ZP.rotationDegrees(-20 + flapAngle));
        ps.mulPose(Axis.YP.rotationDegrees(-25));
        renderWing(ps, vc, alpha, false);
        ps.popPose();

        // === 中央连接光晕 ===
        ps.pushPose();
        ps.translate(0, 0.1f, 0);
        float glowPulse = 1f + Mth.sin(flap * 0.5f) * 0.15f;
        ps.scale(0.12f * glowPulse, 0.5f * glowPulse, 0.05f);
        renderGlowQuad(ps, vc, 1f, 1f, 1f, alpha * 1.4f);
        ps.popPose();

        ps.popPose();
    }

    /**
     * 画一片翅膀（5段羽毛组成）
     */
    private static void renderWing(PoseStack ps, VertexConsumer vc, float alpha, boolean left) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();

        float sign = left ? 1f : -1f;

        // 翅膀形状：从身体向外扩展，逐渐收窄
        // 使用三角形+四边形拼出羽毛效果
        float[][] feathers = {
                {0.0f, 0.0f, -1.8f * sign,  1.0f, 0.3f},   // 最上（更长更宽）
                {0.0f, 0.0f, -2.2f * sign,  0.5f, 0.35f},  // 中上
                {0.0f, 0.0f, -2.0f * sign, -0.1f, 0.32f},  // 中
                {0.0f, 0.0f, -1.6f * sign, -0.6f, 0.26f},  // 中下
                {0.0f, 0.0f, -1.2f * sign, -0.9f, 0.18f},  // 最下
        };

        float r = 0.9f, g = 0.9f, b = 1.0f;

        for (int i = 0; i < feathers.length; i++) {
            float[] f = feathers[i];
            float sx = f[0], sy = f[1];  // 全部从 (0, 0) 出发
            float ex = f[2], ey = f[3];
            float w = f[4];
            float segAlpha = alpha * (1f - i * 0.03f); // 越下面越淡

            // 主羽毛四边形
            addVertex(vc, pose, normal, sx - w, sy, 0, 1f, 1f, 1f, segAlpha, 0, 0);
            addVertex(vc, pose, normal, ex - w * 0.3f, ey, 0, 1f, 1f, 1f, segAlpha, 1, 0);
            addVertex(vc, pose, normal, ex + w * 0.3f, ey, 0, 1f, 1f, 1f, segAlpha, 1, 1);
            addVertex(vc, pose, normal, sx + w, sy, 0, 1f, 1f, 1f, segAlpha, 0, 1);
            // 羽毛纹理 - 中间的线条
            float lineW = w * 0.2f;
            addVertex(vc, pose, normal, sx - lineW, sy, 0, 1f, 1f, 1f, segAlpha, 0, 0);
            addVertex(vc, pose, normal, ex - lineW * 0.1f, ey, 0, 1f, 1f, 1f, segAlpha * 0.9f, 1, 0);
            addVertex(vc, pose, normal, ex + lineW * 0.1f, ey, 0, 1f, 1f, 1f, segAlpha * 0.9f, 1, 1);
            addVertex(vc, pose, normal, sx + lineW, sy, 0, 1f, 1f, 1f, segAlpha, 0, 1);
        }

        // 翅膀尖端光点
        float[] lastF = feathers[1]; // 中间那片最长的
        float tipX = lastF[2];
        float tipY = lastF[3];
        float dotSize = 0.08f;
        addVertex(vc, pose, normal, tipX - dotSize, tipY - dotSize, 0, 1f, 1f, 1f, alpha, 0, 0);
        addVertex(vc, pose, normal, tipX + dotSize, tipY - dotSize, 0, 1f, 1f, 1f, alpha, 1, 0);
        addVertex(vc, pose, normal, tipX + dotSize, tipY + dotSize, 0, 1f, 1f, 1f, alpha, 1, 1);
        addVertex(vc, pose, normal, tipX - dotSize, tipY + dotSize, 0, 1f, 1f, 1f, alpha, 0, 1);
    }

    private static void renderGlowQuad(PoseStack ps, VertexConsumer vc,
                                       float r, float g, float b, float alpha) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float s = 0.5f;

        addVertex(vc, pose, normal, -s, -s, 0, r, g, b, alpha, 0, 0);
        addVertex(vc, pose, normal, s, -s, 0, r, g, b, alpha, 1, 0);
        addVertex(vc, pose, normal, s, s, 0, r, g, b, alpha * 0.7f, 1, 1);
        addVertex(vc, pose, normal, -s, s, 0, r, g, b, alpha * 0.7f, 0, 1);
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