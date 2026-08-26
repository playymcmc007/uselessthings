package com.playymcmc007.uselessthings.mixins;

import com.playymcmc007.uselessthings.client.AtlasFlagHolder;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {

    @Shadow
    private ResourceLocation location;

    @Inject(
            method = "upload(Lnet/minecraft/client/renderer/texture/SpriteLoader$Preparations;)V",
            at = @At("HEAD")
    )
    private void onUploadHead(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        String path = location.toString();
        // 只翻转 blocks 和 items 图集
        if (path.contains("blocks") || path.contains("items") || path.contains("entitys")) {
            AtlasFlagHolder.setAtlas(true);
        }
    }

    @Inject(
            method = "upload(Lnet/minecraft/client/renderer/texture/SpriteLoader$Preparations;)V",
            at = @At("RETURN")
    )
    private void onUploadReturn(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        AtlasFlagHolder.clear();
    }
}