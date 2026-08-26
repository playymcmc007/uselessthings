package com.playymcmc007.uselessthings.client.effect;

import com.mojang.blaze3d.vertex.*;
import com.playymcmc007.uselessthings.ModEffects;
import com.playymcmc007.uselessthings.network.LaserManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

@OnlyIn(Dist.CLIENT)
public class SuperLaserRenderer {

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

        if (!player.hasEffect(ModEffects.SUPER_LASER_EFFECT.get())) return;
        if (!LaserManager.isLaserActive(player)) return;

        PoseStack ps = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
        float pt = event.getPartialTick();

        renderLaserBeam(ps, buf, player, cam, pt);
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

    /**
     * 获取彩虹色
     */
    private static float[] getRainbowColor(float tick, float speed, float offset) {
        float phase = (tick * speed + offset) % (float) (2 * Math.PI);
        float r = (Mth.sin(phase) + 1) / 2;
        float g = (Mth.sin(phase + 2.094f) + 1) / 2;
        float b = (Mth.sin(phase + 4.188f) + 1) / 2;
        return new float[]{r, g, b};
    }

    private static void renderLaserBeam(PoseStack ps, MultiBufferSource buf,
                                        LocalPlayer player, Vec3 cam, float pt) {
        VertexConsumer vc = buf.getBuffer(laserGlowType());
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();

        // 获取玩家位置和视角
        Vec3 playerPos = player.position();
        Vec3 lookVec = player.getViewVector(pt);

        // 激光起点：胸口位置
        double startX = playerPos.x - cam.x;
        double startY = playerPos.y + 1.2 - cam.y;
        double startZ = playerPos.z - cam.z;

        // 终点：500格距离
        double maxDistance = 500.0;
        double endX = startX + lookVec.x * maxDistance;
        double endY = startY + lookVec.y * maxDistance;
        double endZ = startZ + lookVec.z * maxDistance;

        // 计算垂直于视线方向的向量
        Vec3 direction = new Vec3(lookVec.x, lookVec.y, lookVec.z).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = direction.cross(up).normalize();
        Vec3 actualUp = right.cross(direction).normalize();

        float tick = player.tickCount + pt;
        float rotationSpeed = 1.0f;
        float baseSpeed = 0.05f;
        float spinAngle = tick * rotationSpeed * baseSpeed;

        // ========== 脉动效果计算 ==========
        // 基础脉动：sin波，范围 0.85 ~ 1.15（变化±15%）
        float pulseScale = 0.85f + Mth.sin(tick * 0.8f) * 0.15f;
        // 快速小脉动：增加细节
        float quickPulse = 0.95f + Mth.sin(tick * 2.5f) * 0.05f;
        // 综合脉动系数
        float overallPulse = pulseScale * quickPulse;

        // 各层基础宽度（使用脉动系数）
        float baseBeamW = 0.4f;
        float baseInnerW = 0.22f;
        float baseCoreW = 0.1f;
        float baseOuterW = baseBeamW + 0.25f;

        // 应用脉动（外层脉动幅度稍大，内层稍小）
        float beamW = baseBeamW * overallPulse;
        float innerW = baseInnerW * (0.9f + Mth.sin(tick * 1.2f) * 0.1f);
        float coreW = baseCoreW * (0.95f + Mth.sin(tick * 2.0f) * 0.05f);
        float outerW = baseOuterW * (overallPulse * 1.1f);

        // 颜色脉动（亮度变化）
        float colorPulse = 0.7f + Mth.sin(tick * 0.6f) * 0.3f;

        float pulse = Mth.sin(tick * 0.3f) * 0.1f + 0.2f;
        float pulseFast = Mth.sin(tick * 0.8f) * 0.05f + 0.15f;

        // 计算旋转后的方向向量
        float cos = Mth.cos(spinAngle);
        float sin = Mth.sin(spinAngle);

        float rx = (float) (right.x * cos + actualUp.x * sin);
        float ry = (float) (right.y * cos + actualUp.y * sin);
        float rz = (float) (right.z * cos + actualUp.z * sin);

        float ux = (float) (actualUp.x * cos - right.x * sin);
        float uy = (float) (actualUp.y * cos - right.y * sin);
        float uz = (float) (actualUp.z * cos - right.z * sin);

        // ========== 动态颜色 ==========
        float colorSpeed = 0.8f;
        float[] outerColor = getRainbowColor(tick, colorSpeed, 0);
        float[] midColor = getRainbowColor(tick, colorSpeed, 2.0f);
        float[] innerColor = getRainbowColor(tick, colorSpeed, 4.0f);
        float[] coreColor = getRainbowColor(tick, colorSpeed * 1.5f, 1.0f);

        // 应用颜色脉动（让颜色也忽明忽暗）
        float rPulse = colorPulse;
        float gPulse = 0.5f + colorPulse * 0.5f;

        // 外层光晕
        renderBeamQuad(vc, pose, normal,
                startX, startY, startZ, endX, endY, endZ,
                rx, ry, rz, ux, uy, uz, outerW,
                outerColor[0] * 0.8f, outerColor[1] * 0.4f, outerColor[2] * 0.1f, pulse * 0.25f * colorPulse,
                outerColor[0] * 0.6f, outerColor[1] * 0.3f, outerColor[2] * 0.05f, pulse * 0.18f * colorPulse);

        // 中层
        renderBeamQuad(vc, pose, normal,
                startX, startY, startZ, endX, endY, endZ,
                rx, ry, rz, ux, uy, uz, beamW,
                midColor[0], midColor[1] * 0.7f, midColor[2] * 0.2f, 0.55f * colorPulse,
                midColor[0] * 0.9f, midColor[1] * 0.6f, midColor[2] * 0.1f, 0.45f * colorPulse);

        // 内层
        renderBeamQuad(vc, pose, normal,
                startX, startY, startZ, endX, endY, endZ,
                rx, ry, rz, ux, uy, uz, innerW,
                innerColor[0], innerColor[1] * 0.9f, innerColor[2] * 0.3f, 0.75f * (0.8f + colorPulse * 0.2f),
                innerColor[0] * 0.95f, innerColor[1] * 0.85f, innerColor[2] * 0.2f, 0.65f * (0.8f + colorPulse * 0.2f));

        float coreBrightness = 0.8f + colorPulse * 0.2f;
        renderBeamQuad(vc, pose, normal,
                startX, startY, startZ, endX, endY, endZ,
                rx, ry, rz, ux, uy, uz, coreW,
                1f, 1f * coreBrightness, 0.7f * coreBrightness, 0.95f,
                1f, 0.9f * coreBrightness, 0.5f * coreBrightness, 0.85f);
    }

    private static void renderBeamQuad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                       double sx, double sy, double sz,
                                       double ex, double ey, double ez,
                                       float rx, float ry, float rz,
                                       float ux, float uy, float uz,
                                       float width,
                                       float rs, float gs, float bs, float as,
                                       float re, float ge, float be, float ae) {
        float w = width;

        // 起点四个角
        float startRX = (float) sx + rx * w;
        float startRY = (float) sy + ry * w;
        float startRZ = (float) sz + rz * w;

        float startRXn = (float) sx - rx * w;
        float startRYn = (float) sy - ry * w;
        float startRZn = (float) sz - rz * w;

        float startUX = (float) sx + ux * w;
        float startUY = (float) sy + uy * w;
        float startUZ = (float) sz + uz * w;

        float startUXn = (float) sx - ux * w;
        float startUYn = (float) sy - uy * w;
        float startUZn = (float) sz - uz * w;

        // 终点四个角
        float endRX = (float) ex + rx * w;
        float endRY = (float) ey + ry * w;
        float endRZ = (float) ez + rz * w;

        float endRXn = (float) ex - rx * w;
        float endRYn = (float) ey - ry * w;
        float endRZn = (float) ez - rz * w;

        float endUX = (float) ex + ux * w;
        float endUY = (float) ey + uy * w;
        float endUZ = (float) ez + uz * w;

        float endUXn = (float) ex - ux * w;
        float endUYn = (float) ey - uy * w;
        float endUZn = (float) ez - uz * w;

        // 四个面
        addVertex(vc, pose, normal, startRX, startRY, startRZ, rs, gs, bs, as, 0, 0);
        addVertex(vc, pose, normal, endRX, endRY, endRZ, re, ge, be, ae, 1, 0);
        addVertex(vc, pose, normal, endUX, endUY, endUZ, re, ge, be, ae, 1, 1);
        addVertex(vc, pose, normal, startUX, startUY, startUZ, rs, gs, bs, as, 0, 1);

        addVertex(vc, pose, normal, startUX, startUY, startUZ, rs, gs, bs, as, 0, 0);
        addVertex(vc, pose, normal, endUX, endUY, endUZ, re, ge, be, ae, 1, 0);
        addVertex(vc, pose, normal, endRXn, endRYn, endRZn, re, ge, be, ae, 1, 1);
        addVertex(vc, pose, normal, startRXn, startRYn, startRZn, rs, gs, bs, as, 0, 1);

        addVertex(vc, pose, normal, startRXn, startRYn, startRZn, rs, gs, bs, as, 0, 0);
        addVertex(vc, pose, normal, endRXn, endRYn, endRZn, re, ge, be, ae, 1, 0);
        addVertex(vc, pose, normal, endUXn, endUYn, endUZn, re, ge, be, ae, 1, 1);
        addVertex(vc, pose, normal, startUXn, startUYn, startUZn, rs, gs, bs, as, 0, 1);

        addVertex(vc, pose, normal, startUXn, startUYn, startUZn, rs, gs, bs, as, 0, 0);
        addVertex(vc, pose, normal, endUXn, endUYn, endUZn, re, ge, be, ae, 1, 0);
        addVertex(vc, pose, normal, endRX, endRY, endRZ, re, ge, be, ae, 1, 1);
        addVertex(vc, pose, normal, startRX, startRY, startRZ, rs, gs, bs, as, 0, 1);
    }

    private static void renderDot(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                  double x, double y, double z, float size,
                                  float r, float g, float b, float alpha) {
        addVertex(vc, pose, normal, (float) x - size, (float) y, (float) z - size, r, g, b, alpha, 0, 0);
        addVertex(vc, pose, normal, (float) x + size, (float) y, (float) z - size, r, g, b, alpha, 1, 0);
        addVertex(vc, pose, normal, (float) x + size, (float) y, (float) z + size, r, g, b, alpha * 0.7f, 1, 1);
        addVertex(vc, pose, normal, (float) x - size, (float) y, (float) z + size, r, g, b, alpha * 0.7f, 0, 1);
    }
}