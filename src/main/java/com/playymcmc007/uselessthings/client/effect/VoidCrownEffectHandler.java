package com.playymcmc007.uselessthings.client.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.playymcmc007.uselessthings.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class VoidCrownEffectHandler {

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;
    private static float crownRotation = 0f;
    private static float ringRotation = 0f;
    private static float runeRotation = 0f;
    private static float globalPulse = 0f;
    private static boolean wasActiveLastTick = false;

    private static final List<Shockwave> shockwaves = new ArrayList<>();
    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation("minecraft", "textures/misc/white.png");

    private static class Shockwave {
        float age = 0;
        float maxAge = 50;
        float originX, originY, originZ;

        Shockwave(double x, double y, double z) {
            this.originX = (float) x;
            this.originY = (float) y;
            this.originZ = (float) z;
        }

        boolean isAlive() { return age < maxAge; }
        float getProgress() { return age / maxAge; }
    }

    private static RenderType glowType() {
        return RenderType.entityTranslucentEmissive(WHITE_TEXTURE);
    }

    // ==================== Tick ====================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean active = hasActiveCrown(player);

        if (active) {
            tickCounter++;
            crownRotation += 3f;
            ringRotation += 1.5f;
            runeRotation -= 2f;
            globalPulse = Mth.sin(tickCounter * 0.04f);

            if (tickCounter % 40 == 0) {
                Vec3 pos = player.getPosition(1.0f);
                shockwaves.add(new Shockwave(pos.x, pos.y + 1.5, pos.z));
            }

            spawnMinimalParticles(player);

            if (!wasActiveLastTick) {
                spawnActivationBurst(player);
            }
        }

        wasActiveLastTick = active;

        for (Shockwave sw : shockwaves) sw.age++;
        shockwaves.removeIf(sw -> !sw.isAlive());
    }

    // ==================== 精简粒子 ====================
    private static void spawnMinimalParticles(LocalPlayer player) {
        Vec3 pos = player.getPosition(1.0f);  // tick里没有partialTick，用1.0
        double px = pos.x;
        double py = pos.y;
        double pz = pos.z;

        // 脚下光环粒子 - 每3tick
        if (tickCounter % 3 == 0) {
            for (int i = 0; i < 3; i++) {
                double angle = tickCounter * 0.06 + i * Math.PI * 2 / 3;
                player.level().addParticle(
                        ParticleTypes.END_ROD,
                        px + Math.cos(angle) * 0.9, py + 0.05, pz + Math.sin(angle) * 0.9,
                        0, 0.01, 0
                );
            }
        }

        // 头顶宝石粒子 - 每4tick
        if (tickCounter % 4 == 0) {
            player.level().addParticle(
                    ParticleTypes.FIREWORK,
                    px + (RANDOM.nextDouble() - 0.5) * 0.3,
                    py + 3.2 + RANDOM.nextDouble() * 0.5,
                    pz + (RANDOM.nextDouble() - 0.5) * 0.3,
                    0, 0.02, 0
            );
        }

        // 护盾表面微光 - 每5tick
        if (tickCounter % 5 == 0) {
            for (int i = 0; i < 4; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double pitch = RANDOM.nextDouble() * Math.PI;
                player.level().addParticle(
                        ParticleTypes.WITCH,
                        px + Math.cos(angle) * Math.sin(pitch) * 2.0,
                        py + 1.5 + Math.cos(pitch) * 2.5,
                        pz + Math.sin(angle) * Math.sin(pitch) * 2.0,
                        0, 0, 0
                );
            }
        }

        // 光柱底部粒子 - 每6tick
        if (tickCounter % 6 == 0) {
            for (int i = 0; i < 2; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                player.level().addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        px + Math.cos(angle) * 0.5, py + 0.1, pz + Math.sin(angle) * 0.5,
                        0, 0.04, 0
                );
            }
        }

        // 王冠尖刺粒子 - 每10tick
        if (tickCounter % 10 == 0) {
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(crownRotation + i * 72);
                player.level().addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        px + Math.cos(angle) * 0.25, py + 2.25, pz + Math.sin(angle) * 0.25,
                        0, 0.05, 0
                );
            }
        }
    }

    private static void spawnActivationBurst(LocalPlayer player) {
        Vec3 pos = player.getPosition(1.0f);
        double px = pos.x;
        double py = pos.y + 1.5;
        double pz = pos.z;

        for (int i = 0; i < 15; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double pitch = RANDOM.nextDouble() * Math.PI - Math.PI / 2;
            double speed = 0.1 + RANDOM.nextDouble() * 0.2;
            player.level().addParticle(
                    ParticleTypes.FIREWORK,
                    px, py, pz,
                    Math.cos(pitch) * Math.cos(angle) * speed,
                    Math.sin(pitch) * speed + 0.05,
                    Math.cos(pitch) * Math.sin(angle) * speed
            );
        }
    }

    // ==================== 屏幕覆盖特效 ====================
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!hasActiveCrown(player)) return;

        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        renderScreenVignette(w, h);
        renderScreenBorders(w, h);
    }

    private static void renderScreenVignette(int w, int h) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float alpha = 0.35f + globalPulse * 0.1f;
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        int barWidth = 40;

        // 顶部
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int y = 0; y < barWidth; y++) {
            float t = (float) y / barWidth;
            float a = alpha * (1f - t);
            buf.vertex(0, y, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(w, y, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(w, y + 1, 0).color(0.15f, 0.03f, 0.35f, a * 0.95f).endVertex();
            buf.vertex(0, y + 1, 0).color(0.15f, 0.03f, 0.35f, a * 0.95f).endVertex();
        }
        tess.end();

        // 底部
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int y = h - barWidth; y < h; y++) {
            float t = (float) (y - (h - barWidth)) / barWidth;
            float a = alpha * t;
            buf.vertex(0, y, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(w, y, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(w, y + 1, 0).color(0.15f, 0.03f, 0.35f, a * 1.05f).endVertex();
            buf.vertex(0, y + 1, 0).color(0.15f, 0.03f, 0.35f, a * 1.05f).endVertex();
        }
        tess.end();

        // 左侧
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int x = 0; x < barWidth; x++) {
            float t = (float) x / barWidth;
            float a = alpha * (1f - t);
            buf.vertex(x, 0, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(x + 1, 0, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(x + 1, h, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(x, h, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
        }
        tess.end();

        // 右侧
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int x = w - barWidth; x < w; x++) {
            float t = (float) (x - (w - barWidth)) / barWidth;
            float a = alpha * t;
            buf.vertex(x, 0, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(x + 1, 0, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(x + 1, h, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
            buf.vertex(x, h, 0).color(0.15f, 0.03f, 0.35f, a).endVertex();
        }
        tess.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderScreenBorders(int w, int h) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float pulse = (globalPulse + 1f) / 2f;
        float alpha = 0.25f + pulse * 0.15f;
        int barH = 4;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // 顶部光条
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int x = 0; x < w; x += 4) {
            float glow = Mth.sin((x + tickCounter) * 0.015f) * 0.5f + 0.5f;
            float r = 0.5f + glow * 0.5f;
            float g2 = 0.1f + glow * 0.3f;
            float b = 0.7f + glow * 0.3f;
            float a = alpha * glow;
            buf.vertex(x, 0, 0).color(r, g2, b, a).endVertex();
            buf.vertex(x + 4, 0, 0).color(r, g2, b, a).endVertex();
            buf.vertex(x + 4, barH, 0).color(r * 0.5f, g2 * 0.5f, b * 0.5f, a * 0.3f).endVertex();
            buf.vertex(x, barH, 0).color(r * 0.5f, g2 * 0.5f, b * 0.5f, a * 0.3f).endVertex();
        }
        tess.end();

        // 底部光条
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int x = 0; x < w; x += 4) {
            float glow = Mth.sin((x + tickCounter) * 0.015f + 2f) * 0.5f + 0.5f;
            float r = 0.5f + glow * 0.5f;
            float g2 = 0.1f + glow * 0.3f;
            float b = 0.7f + glow * 0.3f;
            float a = alpha * glow;
            buf.vertex(x, h - barH, 0).color(r * 0.5f, g2 * 0.5f, b * 0.5f, a * 0.3f).endVertex();
            buf.vertex(x + 4, h - barH, 0).color(r * 0.5f, g2 * 0.5f, b * 0.5f, a * 0.3f).endVertex();
            buf.vertex(x + 4, h, 0).color(r, g2, b, a).endVertex();
            buf.vertex(x, h, 0).color(r, g2, b, a).endVertex();
        }
        tess.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    // ==================== 脚下五芒星 ====================
    private static void renderPentagram(PoseStack ps, MultiBufferSource buf,
                                        LocalPlayer player, Vec3 cam, float pt) {
        VertexConsumer vc = buf.getBuffer(glowType());
        Vec3 pos = player.getPosition(pt);
        double px = pos.x - cam.x;
        double py = pos.y - cam.y;
        double pz = pos.z - cam.z;

        ps.pushPose();
        ps.translate(px, py + 0.015, pz);
        ps.mulPose(Axis.XP.rotationDegrees(90f));
        ps.mulPose(Axis.ZP.rotationDegrees(runeRotation * 0.3f));

        float scale = 1.0f + (globalPulse + 1f) * 0.1f;
        ps.scale(scale, scale, 1f);

        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();

        float alpha = 0.5f + (globalPulse + 1f) * 0.15f;
        float outerR = 1.0f;
        float innerR = 0.38f;
        float thickness = 0.04f;

        // 5个顶点 + 5个内顶点 = 五芒星
        int points = 5;
        for (int i = 0; i < points; i++) {
            // 外顶点
            double outerAngle = Math.PI * 2 * i / points - Math.PI / 2;
            float ox = (float) Math.cos(outerAngle) * outerR;
            float oy = (float) Math.sin(outerAngle) * outerR;

            // 下一个外顶点
            double nextOuterAngle = Math.PI * 2 * ((i + 2) % points) / points - Math.PI / 2;
            float nox = (float) Math.cos(nextOuterAngle) * outerR;
            float noy = (float) Math.sin(nextOuterAngle) * outerR;

            // 内顶点（两条线交叉的拐点）
            double innerAngle = Math.PI * 2 * (i + 0.5) / points - Math.PI / 2;
            float ix = (float) Math.cos(innerAngle) * innerR;
            float iy = (float) Math.sin(innerAngle) * innerR;

            // 外顶点 → 下一个外顶点（经过内顶点）
            renderThickLine(pose, normal, vc, ox, oy, ix, iy, thickness,
                    0.9f, 0.7f, 0.15f, alpha, 0.9f, 0.7f, 0.15f, alpha * 1.2f);
            renderThickLine(pose, normal, vc, ix, iy, nox, noy, thickness,
                    0.9f, 0.7f, 0.15f, alpha * 1.2f, 0.9f, 0.7f, 0.15f, alpha);
        }

        // 外环包裹
        renderThickRing(ps, vc, outerR + 0.05f, 0.03f, 64, 0.9f, 0.7f, 0.15f, alpha * 0.6f);
        renderThickRing(ps, vc, outerR, 0.05f, 64, 0.9f, 0.7f, 0.15f, alpha);
        renderThickRing(ps, vc, innerR + 0.02f, 0.025f, 48, 0.9f, 0.7f, 0.15f, alpha * 0.5f);

        ps.popPose();
    }

    /**
     * 粗线段（四边形带）
     */
    private static void renderThickLine(Matrix4f pose, Matrix3f normal, VertexConsumer vc,
                                        float x1, float y1, float x2, float y2, float thickness,
                                        float r1, float g1, float b1, float a1,
                                        float r2, float g2, float b2, float a2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;
        float nx = -dy / len * thickness;
        float ny = dx / len * thickness;

        addVertex(vc, pose, normal, x1 + nx, y1 + ny, 0, r1, g1, b1, a1, 0, 0);
        addVertex(vc, pose, normal, x2 + nx, y2 + ny, 0, r2, g2, b2, a2, 1, 0);
        addVertex(vc, pose, normal, x2 - nx, y2 - ny, 0, r2, g2, b2, a2, 1, 1);
        addVertex(vc, pose, normal, x1 - nx, y1 - ny, 0, r1, g1, b1, a1, 0, 1);
    }


    // ==================== 玩家身上特效 ====================
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;
        if (!hasActiveCrown(localPlayer)) return;
        if (event.getEntity() != localPlayer) return;

        PoseStack ps = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        float pt = event.getPartialTick();

        renderVoidShield(ps, buf, pt);
        renderFloatingCrown(ps, buf, pt);
        renderRuneCircles(ps, buf, pt);
    }

    // ==================== 世界空间特效 ====================
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

        renderGroundMagicCircles(ps, buf, player, cam, pt);
        renderShockwaves(ps, buf, cam);
        renderOrbitingShards(ps, buf, player, cam, pt);
        renderPentagram(ps, buf, player, cam, pt);
    }

    // ==================== 辅助顶点方法 ====================
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

    // ==================== 虚空护盾 ====================
    private static void renderVoidShield(PoseStack ps, MultiBufferSource buf, float pt) {
        VertexConsumer vc = buf.getBuffer(glowType());

        ps.pushPose();
        ps.translate(0, 1.2, 0);
        float pulse = 1f + globalPulse * 0.05f;
        ps.scale(2f * pulse, 2.5f * pulse, 2f * pulse);
        ps.mulPose(Axis.YP.rotationDegrees(ringRotation + pt * 1.5f));
        ps.mulPose(Axis.XP.rotationDegrees(15f));

        // 紫色粗环
        renderThickRing(ps, vc, 0.5f, 0.06f, 48, 0.3f, 0.1f, 0.8f, 0.55f);
        renderThickRing(ps, vc, 0.5f, 0.03f, 48, 0.5f, 0.2f, 0.95f, 0.4f);

        // 经线粗弧
        renderThickLongitudeArcs(ps, vc, 0.5f, 0.04f, 16, 0.3f, 0.1f, 0.8f, 0.35f);

        // 第二层金色
        ps.mulPose(Axis.YP.rotationDegrees(-ringRotation * 2.5f));
        renderThickRing(ps, vc, 0.48f, 0.04f, 36, 0.8f, 0.6f, 0.1f, 0.35f);
        renderThickRing(ps, vc, 0.52f, 0.04f, 36, 0.9f, 0.7f, 0.15f, 0.3f);

        // 垂直环
        ps.mulPose(Axis.XP.rotationDegrees(90f));
        renderThickRing(ps, vc, 0.5f, 0.05f, 40, 0.5f, 0.15f, 0.9f, 0.3f);

        ps.popPose();
    }

    // ==================== 悬浮王冠 ====================
    private static void renderFloatingCrown(PoseStack ps, MultiBufferSource buf, float pt) {
        VertexConsumer vc = buf.getBuffer(glowType());

        ps.pushPose();
        float bob = Mth.sin((tickCounter + pt) * 0.04f) * 0.15f;
        ps.translate(0, 2.3f + bob, 0);
        ps.mulPose(Axis.YP.rotationDegrees(crownRotation + pt * 3f));

        // 底座三重环
        for (int i = 0; i < 3; i++) {
            ps.pushPose();
            ps.translate(0, i * 0.04f, 0);
            float s = 0.32f - i * 0.02f;
            ps.scale(s, 1, s);
            renderThickRing(ps, vc, 0.5f, 0.05f, 48, 0.8f, 0.55f, 0.1f, 0.7f);
            ps.popPose();
        }

        // 5个粗尖刺
        for (int i = 0; i < 5; i++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(i * 72f));
            ps.translate(0.22f, 0.15f, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-20f));
            renderThickTriangle(ps, vc, 0.12f, 0.45f,
                    0.9f, 0.7f, 0.1f, 0.8f,
                    0.5f, 0.2f, 0.8f, 0.4f);
            ps.popPose();
        }

        // 尖刺顶部小宝石
        for (int i = 0; i < 5; i++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(i * 72f));
            ps.translate(0.22f, 0.45f, 0);
            ps.scale(0.05f, 0.05f, 0.05f);
            ps.mulPose(Axis.YP.rotationDegrees(tickCounter * 5f));
            renderFilledQuad(ps, vc, 0.9f, 0.5f, 0.5f, 0.9f);
            ps.popPose();
        }

        // 中央大宝石
        ps.pushPose();
        ps.translate(0, 0.25f, 0);
        float gemPulse = 1f + Mth.sin((tickCounter + pt) * 0.08f) * 0.2f;
        ps.scale(0.12f * gemPulse, 0.18f * gemPulse, 0.12f * gemPulse);
        ps.mulPose(Axis.YP.rotationDegrees(tickCounter * 4f));
        renderDiamond(ps, vc, 0.8f, 0.3f, 0.95f, 0.85f);
        ps.popPose();

        // 宝石光晕
        ps.pushPose();
        ps.translate(0, 0.25f, 0);
        float haloSize = 0.3f + Mth.sin((tickCounter + pt) * 0.06f) * 0.06f;
        for (int h = 0; h < 3; h++) {
            ps.pushPose();
            ps.mulPose(Axis.XP.rotationDegrees(90f));
            ps.mulPose(Axis.ZP.rotationDegrees(h * 60f + tickCounter * 2.5f));
            renderThickRing(ps, vc, haloSize, 0.04f, 32, 0.9f, 0.6f, 0.1f, 0.5f);
            ps.popPose();
        }
        ps.popPose();

        ps.popPose();
    }

    // ==================== 符文圆环 ====================
    private static void renderRuneCircles(PoseStack ps, MultiBufferSource buf, float pt) {
        VertexConsumer vc = buf.getBuffer(glowType());

        ps.pushPose();
        ps.translate(0, 1.15f, 0);
        ps.mulPose(Axis.XP.rotationDegrees(90f));

        for (int ring = 0; ring < 3; ring++) {
            ps.pushPose();

            float radius = 0.35f + ring * 0.3f;
            ps.scale(radius, radius, 1f);
            ps.mulPose(Axis.ZP.rotationDegrees(
                    runeRotation * (ring % 2 == 0 ? 1 : -1) + ring * 60f));

            float alpha = 0.55f - ring * 0.1f + (globalPulse + 1f) * 0.05f;
            float rf = ring == 0 ? 0.5f : (ring == 1 ? 0.8f : 0.3f);
            float gf = ring == 0 ? 0.2f : (ring == 1 ? 0.6f : 0.1f);
            float bf = ring == 0 ? 0.9f : (ring == 1 ? 0.1f : 0.7f);

            // 粗外环
            renderThickRing(ps, vc, 0.5f, 0.05f, 48, rf, gf, bf, alpha);
            renderThickRing(ps, vc, 0.5f, 0.02f, 48, rf * 1.2f, gf * 1.2f, bf * 1.2f, alpha * 0.6f);

            // 粗符文标记
            int runes = 8 + ring * 4;
            for (int i = 0; i < runes; i++) {
                ps.pushPose();
                ps.mulPose(Axis.ZP.rotationDegrees(i * 360f / runes));
                ps.translate(0, 0.46f, 0);
                ps.scale(0.04f, 0.08f, 1f);
                float pulse2 = 0.6f + Mth.sin((tickCounter + i * 10) * 0.1f) * 0.4f;
                renderFilledQuad(ps, vc, rf, gf, bf, alpha * pulse2);
                ps.popPose();
            }

            ps.popPose();
        }

        ps.popPose();
    }

    // ==================== 地面魔法阵 ====================
    private static void renderGroundMagicCircles(PoseStack ps, MultiBufferSource buf,
                                                 LocalPlayer player, Vec3 cam, float pt) {
        VertexConsumer vc = buf.getBuffer(glowType());
        Vec3 pos = player.getPosition(pt);
        double px = pos.x - cam.x;
        double pz = pos.z - cam.z;

        ps.pushPose();
        ps.translate(px, 0.4 + Mth.sin((tickCounter + pt) * 0.03f) * 0.15f, pz);
        ps.mulPose(Axis.XP.rotationDegrees(90f));
        ps.mulPose(Axis.ZP.rotationDegrees(-ringRotation * 0.7f));

        float scale2 = 1.8f + Mth.cos((tickCounter + pt) * 0.025f) * 0.15f;
        ps.scale(scale2, scale2, 1f);

        renderThickRing(ps, vc, 0.5f, 0.05f, 56, 0.7f, 0.5f, 0.1f, 0.4f);
        renderOctagon(ps, vc, 0.35f, 0.04f, 0.9f, 0.7f, 0.1f, 0.35f);

        ps.popPose();
    }

    // ==================== 震荡波 ====================
    private static void renderShockwaves(PoseStack ps, MultiBufferSource buf, Vec3 cam) {
        VertexConsumer vc = buf.getBuffer(glowType());

        for (Shockwave sw : shockwaves) {
            float progress = sw.getProgress();
            float alpha = (1f - progress) * 0.5f;
            float radius = progress * 6f;

            ps.pushPose();
            ps.translate(sw.originX - cam.x, sw.originY - cam.y, sw.originZ - cam.z);
            ps.scale(radius, 1f, radius);

            renderThickRing(ps, vc, 0.5f, 0.06f, 48, 0.7f, 0.3f, 0.9f, alpha);

            ps.pushPose();
            ps.scale(0.85f, 1f, 0.85f);
            renderThickRing(ps, vc, 0.5f, 0.04f, 48, 0.9f, 0.6f, 0.1f, alpha * 0.7f);
            ps.popPose();

            ps.popPose();
        }
    }

    // ==================== 环绕碎片 ====================
    private static void renderOrbitingShards(PoseStack ps, MultiBufferSource buf,
                                             LocalPlayer player, Vec3 cam, float pt) {
        VertexConsumer vc = buf.getBuffer(glowType());
        Vec3 pos = player.getPosition(pt);
        double px = pos.x - cam.x;
        double py = pos.y - cam.y + 1.5;
        double pz = pos.z - cam.z;

        int shards = 10;
        for (int i = 0; i < shards; i++) {
            float angle = (tickCounter + pt) * 2f + i * 360f / shards;
            float height = Mth.sin((tickCounter + pt + i * 10) * 0.03f) * 1.5f;
            float radius = 2f + Mth.sin((tickCounter + i) * 0.04f) * 0.5f;

            ps.pushPose();
            ps.translate(
                    px + Math.cos(Math.toRadians(angle)) * radius,
                    py + height,
                    pz + Math.sin(Math.toRadians(angle)) * radius);
            ps.mulPose(Axis.YP.rotationDegrees(angle * 3f));
            ps.mulPose(Axis.XP.rotationDegrees(angle * 2f));
            ps.scale(0.06f, 0.06f, 0.06f);

            renderDiamond(ps, vc, 0.7f, 0.4f, 0.9f, 0.6f + (globalPulse + 1f) * 0.2f);

            ps.popPose();
        }
    }

    // ==================== 几何形状渲染工具 ====================

    /**
     * 粗环 - 用四边形带组成
     */
    private static void renderThickRing(PoseStack ps, VertexConsumer vc,
                                        float radius, float thickness, int segments,
                                        float r, float g, float b, float alpha) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float innerR = radius - thickness;
        float outerR = radius + thickness;

        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments;
            double a2 = Math.PI * 2 * (i + 1) / segments;

            float ix1 = (float) Math.cos(a1) * innerR;
            float iy1 = (float) Math.sin(a1) * innerR;
            float ix2 = (float) Math.cos(a2) * innerR;
            float iy2 = (float) Math.sin(a2) * innerR;
            float ox1 = (float) Math.cos(a1) * outerR;
            float oy1 = (float) Math.sin(a1) * outerR;
            float ox2 = (float) Math.cos(a2) * outerR;
            float oy2 = (float) Math.sin(a2) * outerR;

            addVertex(vc, pose, normal, ix1, iy1, 0, r, g, b, alpha, 0, 0);
            addVertex(vc, pose, normal, ix2, iy2, 0, r, g, b, alpha, 1, 0);
            addVertex(vc, pose, normal, ox2, oy2, 0, r * 1.1f, g * 1.1f, b * 1.1f, alpha * 0.8f, 1, 1);
            addVertex(vc, pose, normal, ox1, oy1, 0, r * 1.1f, g * 1.1f, b * 1.1f, alpha * 0.8f, 0, 1);
        }
    }

    /**
     * 粗经线弧
     */
    private static void renderThickLongitudeArcs(PoseStack ps, VertexConsumer vc,
                                                 float radius, float thickness, int count,
                                                 float r, float g, float b, float alpha) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float halfT = thickness / 2;

        for (int i = 0; i < count; i++) {
            double theta = Math.PI * 2 * i / count;
            float cx = (float) Math.cos(theta);
            float cz = (float) Math.sin(theta);

            int arcSegs = 20;
            for (int j = 0; j < arcSegs; j++) {
                double phi1 = Math.PI * j / arcSegs - Math.PI / 2;
                double phi2 = Math.PI * (j + 1) / arcSegs - Math.PI / 2;

                float y1 = (float) Math.sin(phi1) * radius;
                float r1 = (float) Math.cos(phi1) * radius;
                float y2 = (float) Math.sin(phi2) * radius;
                float r2 = (float) Math.cos(phi2) * radius;

                float dy1 = (float) Math.cos(phi1) * halfT;
                float dr1 = -(float) Math.sin(phi1) * halfT;
                float dy2 = (float) Math.cos(phi2) * halfT;
                float dr2 = -(float) Math.sin(phi2) * halfT;

                addVertex(vc, pose, normal,
                        cx * (r1 - dr1), y1 - dy1, cz * (r1 - dr1),
                        r, g, b, alpha, 0, 0);
                addVertex(vc, pose, normal,
                        cx * (r2 - dr2), y2 - dy2, cz * (r2 - dr2),
                        r, g, b, alpha, 1, 0);
                addVertex(vc, pose, normal,
                        cx * (r2 + dr2), y2 + dy2, cz * (r2 + dr2),
                        r, g, b, alpha * 0.7f, 1, 1);
                addVertex(vc, pose, normal,
                        cx * (r1 + dr1), y1 + dy1, cz * (r1 + dr1),
                        r, g, b, alpha * 0.7f, 0, 1);
            }
        }
    }

    /**
     * 粗三角形
     */
    private static void renderThickTriangle(PoseStack ps, VertexConsumer vc,
                                            float width, float height,
                                            float r1, float g1, float b1, float a1,
                                            float r2, float g2, float b2, float a2) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float hw = width / 2f;
        float th = 0.03f;

        addVertex(vc, pose, normal, -th, 0, -th, r1, g1, b1, a1, 0, 0);
        addVertex(vc, pose, normal, hw, height, 0, r2, g2, b2, a2, 1, 0);
        addVertex(vc, pose, normal, -hw, height, 0, r2, g2, b2, a2, 0, 1);
        addVertex(vc, pose, normal, th, 0, th, r1, g1, b1, a1, 1, 1);
    }

    /**
     * 填充四边形
     */
    private static void renderFilledQuad(PoseStack ps, VertexConsumer vc,
                                         float r, float g, float b, float alpha) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float s = 0.5f;

        addVertex(vc, pose, normal, -s, -s, 0, r, g, b, alpha, 0, 0);
        addVertex(vc, pose, normal, s, -s, 0, r, g, b, alpha, 1, 0);
        addVertex(vc, pose, normal, s, s, 0, r, g, b, alpha * 0.8f, 1, 1);
        addVertex(vc, pose, normal, -s, s, 0, r, g, b, alpha * 0.8f, 0, 1);
    }

    /**
     * 菱形/八面体形状
     */
    private static void renderDiamond(PoseStack ps, VertexConsumer vc,
                                      float r, float g, float b, float alpha) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float s = 0.5f;

        // 上半
        addVertex(vc, pose, normal, 0, s, 0, r, g, b, alpha, 0.5f, 0);
        addVertex(vc, pose, normal, s, 0, s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 1, 0.5f);
        addVertex(vc, pose, normal, s, 0, -s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 1, 1);
        addVertex(vc, pose, normal, -s, 0, -s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 0, 1);

        addVertex(vc, pose, normal, 0, s, 0, r, g, b, alpha, 0.5f, 0);
        addVertex(vc, pose, normal, -s, 0, -s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 0, 0.5f);
        addVertex(vc, pose, normal, -s, 0, s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 0, 1);
        addVertex(vc, pose, normal, s, 0, s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 1, 0.5f);

        // 下半
        addVertex(vc, pose, normal, 0, -s, 0, r * 0.4f, g * 0.4f, b * 0.4f, alpha * 0.5f, 0.5f, 0);
        addVertex(vc, pose, normal, s, 0, s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 1, 0.5f);
        addVertex(vc, pose, normal, -s, 0, s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 0, 0.5f);
        addVertex(vc, pose, normal, -s, 0, -s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 0, 1);

        addVertex(vc, pose, normal, 0, -s, 0, r * 0.4f, g * 0.4f, b * 0.4f, alpha * 0.5f, 0.5f, 0);
        addVertex(vc, pose, normal, -s, 0, -s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 0, 1);
        addVertex(vc, pose, normal, s, 0, -s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 1, 1);
        addVertex(vc, pose, normal, s, 0, s, r * 0.7f, g * 0.7f, b * 0.7f, alpha * 0.7f, 1, 0.5f);
    }

    /**
     * 粗六芒星
     */
    private static void renderHexagram(PoseStack ps, VertexConsumer vc) {
        float alpha = 0.45f + (globalPulse + 1f) * 0.1f;
        float size = 0.43f;
        float thick = 0.03f;

        for (int tri = 0; tri < 2; tri++) {
            float r = tri == 0 ? 0.9f : 0.7f;
            float g2 = tri == 0 ? 0.6f : 0.5f;
            float b2 = tri == 0 ? 0.15f : 0.9f;
            float rotation = tri == 0 ? 0 : 60f + ringRotation * 0.5f;

            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(rotation));
            Matrix4f pose = ps.last().pose();
            Matrix3f normal = ps.last().normal();

            for (int i = 0; i < 3; i++) {
                double a1 = Math.toRadians(i * 120);
                double a2 = Math.toRadians((i + 1) * 120);
                float x1c = (float) Math.cos(a1) * size;
                float y1c = (float) Math.sin(a1) * size;
                float x2c = (float) Math.cos(a2) * size;
                float y2c = (float) Math.sin(a2) * size;
                float dx = (y2c - y1c) * thick / size;
                float dy = -(x2c - x1c) * thick / size;

                addVertex(vc, pose, normal, x1c + dx, y1c + dy, 0, r, g2, b2, alpha * 0.2f, 0, 0);
                addVertex(vc, pose, normal, x2c + dx, y2c + dy, 0, r, g2, b2, alpha * 0.5f, 1, 0);
                addVertex(vc, pose, normal, x2c - dx, y2c - dy, 0, r, g2, b2, alpha * 0.5f, 1, 1);
                addVertex(vc, pose, normal, x1c - dx, y1c - dy, 0, r, g2, b2, alpha * 0.2f, 0, 1);
            }
            ps.popPose();
        }
    }

    /**
     * 粗八角形
     */
    private static void renderOctagon(PoseStack ps, VertexConsumer vc,
                                      float radius, float thickness,
                                      float r, float g, float b, float alpha) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        int sides = 8;
        float innerR = radius - thickness;
        float outerR = radius + thickness;

        for (int i = 0; i < sides; i++) {
            double a1 = Math.PI * 2 * i / sides - Math.PI / sides;
            double a2 = Math.PI * 2 * (i + 1) / sides - Math.PI / sides;

            addVertex(vc, pose, normal,
                    (float) Math.cos(a1) * innerR, (float) Math.sin(a1) * innerR, 0,
                    r, g, b, alpha * 0.7f, 0, 0);
            addVertex(vc, pose, normal,
                    (float) Math.cos(a2) * innerR, (float) Math.sin(a2) * innerR, 0,
                    r, g, b, alpha * 0.7f, 1, 0);
            addVertex(vc, pose, normal,
                    (float) Math.cos(a2) * outerR, (float) Math.sin(a2) * outerR, 0,
                    r, g, b, alpha, 1, 1);
            addVertex(vc, pose, normal,
                    (float) Math.cos(a1) * outerR, (float) Math.sin(a1) * outerR, 0,
                    r, g, b, alpha, 0, 1);
        }
    }

    /**
     * 粗辐射线
     */
    private static void renderRadialLines(PoseStack ps, VertexConsumer vc,
                                          float innerR, float outerR, int count,
                                          float r, float g, float b, float alpha, float thickness) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();

        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            float dx = -(float) Math.sin(angle) * thickness;
            float dy = (float) Math.cos(angle) * thickness;
            float ix = (float) Math.cos(angle) * innerR;
            float iy = (float) Math.sin(angle) * innerR;
            float ox = (float) Math.cos(angle) * outerR;
            float oy = (float) Math.sin(angle) * outerR;

            addVertex(vc, pose, normal, ix + dx, iy + dy, 0, r, g, b, alpha * 0.3f, 0, 0);
            addVertex(vc, pose, normal, ox + dx, oy + dy, 0, r, g, b, alpha, 1, 0);
            addVertex(vc, pose, normal, ox - dx, oy - dy, 0, r, g, b, alpha, 1, 1);
            addVertex(vc, pose, normal, ix - dx, iy - dy, 0, r, g, b, alpha * 0.3f, 0, 1);
        }
    }

    // ==================== 工具方法 ====================
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