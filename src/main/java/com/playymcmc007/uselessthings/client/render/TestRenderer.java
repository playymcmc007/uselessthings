package com.playymcmc007.uselessthings.client.render;

import com.playymcmc007.uselessthings.UselessThings;
import com.playymcmc007.uselessthings.client.model.TestModel;
import com.playymcmc007.uselessthings.entity.TestEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TestRenderer extends MobRenderer<TestEntity, TestModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(UselessThings.MODID, "textures/entity/test.png");

    public TestRenderer(EntityRendererProvider.Context context) {
        super(context, new TestModel(context.bakeLayer(TestModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(TestEntity entity) {
        return TEXTURE;
    }
}