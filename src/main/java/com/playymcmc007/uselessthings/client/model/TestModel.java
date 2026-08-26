package com.playymcmc007.uselessthings.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.playymcmc007.uselessthings.UselessThings;
import com.playymcmc007.uselessthings.entity.TestEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class TestModel extends HierarchicalModel<TestEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(UselessThings.MODID, "test"), "main");

    private final ModelPart root;
    private final ModelPart shenti;   // 身体
    private final ModelPart tou;       // 头
    private final ModelPart zuotui;    // 左腿
    private final ModelPart youtui;    // 右腿
    private final ModelPart shou1;     // 左手
    private final ModelPart shou2;     // 右手

    public TestModel(ModelPart root) {
        this.root = root;
        this.shenti = root.getChild("shenti");
        this.tou = root.getChild("tou");
        this.zuotui = root.getChild("zuotui");
        this.youtui = root.getChild("youtui");
        this.shou1 = root.getChild("shou1");
        this.shou2 = root.getChild("shou2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 身体
        PartDefinition shenti = partdefinition.addOrReplaceChild("shenti",
                CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, -12.0F, -6.0F, 3.0F, 9.0F, 7.0F),
                PartPose.offset(-1.0F, 21.0F, 2.0F));

        // 头
        PartDefinition tou = partdefinition.addOrReplaceChild("tou",
                CubeListBuilder.create().texOffs(0, 16).addBox(-5.0F, -11.0F, -4.0F, 8.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        // 左腿
        PartDefinition zuotui = partdefinition.addOrReplaceChild("zuotui",
                CubeListBuilder.create().texOffs(30, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(-0.5F, 18.0F, -2.5F));

        // 右腿
        PartDefinition youtui = partdefinition.addOrReplaceChild("youtui",
                CubeListBuilder.create().texOffs(20, 30).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(-0.5F, 18.0F, 1.5F));

        // 左手
        PartDefinition shou1 = partdefinition.addOrReplaceChild("shou1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -2.0F, 42.0F, 4.0F, 4.0F),
                PartPose.offset(-0.5F, 11.0F, 5.0F));

        // 右手
        PartDefinition shou2 = partdefinition.addOrReplaceChild("shou2",
                CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -2.0F, -2.0F, 42.0F, 4.0F, 4.0F),
                PartPose.offset(-0.5F, 11.0F, -6.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TestEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 重置所有骨骼旋转
        shenti.xRot = 0;
        shenti.yRot = 0;
        shenti.zRot = 0;
        tou.xRot = 0;
        tou.yRot = 0;
        tou.zRot = 0;
        zuotui.xRot = 0;
        zuotui.zRot = 0;
        youtui.xRot = 0;
        youtui.zRot = 0;
        shou1.xRot = 0;
        shou1.zRot = 0;
        shou2.xRot = 0;
        shou2.zRot = 0;

        // 行走动画 - 腿交替摆动
        float walkSpeed = limbSwing * 0.8f;
        float walkAmount = limbSwingAmount;

        zuotui.zRot = (float) Math.sin(walkSpeed) * walkAmount * 0.8f;
        youtui.zRot = (float) Math.sin(walkSpeed + Math.PI) * walkAmount * 0.8f;

        // 手臂与腿相反方向摆动
        shou1.zRot = (float) Math.sin(walkSpeed + Math.PI) * walkAmount * 0.4f;
        shou2.zRot = (float) Math.sin(walkSpeed) * walkAmount * 0.4f;

        // ★ 让整个身体跟随头部转向 ★
        shenti.yRot = netHeadYaw * ((float) Math.PI / 180f);

        // 头部跟随视线（微量旋转，更自然）
        tou.xRot = headPitch * ((float) Math.PI / 180f);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        shenti.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        tou.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        zuotui.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        youtui.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        shou1.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        shou2.render(poseStack, vertexConsumer, packedLight, packedOverlay);
    }
}