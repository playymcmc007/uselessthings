package com.playymcmc007.uselessthings.mixins;

import com.mojang.blaze3d.platform.NativeImage;
import com.playymcmc007.uselessthings.client.AtlasFlagHolder;
import com.playymcmc007.uselessthings.client.EasterEggHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NativeImage.class)
public class NativeImageMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("UselessThings/NativeImage");
    @Shadow private int width;
    @Shadow private int height;

    @Inject(method = "_upload", at = @At("HEAD"))
    private void onUploadHead(int level, int xOffset, int yOffset, int skipPixels, int skipRows,
                              int uploadWidth, int uploadHeight, boolean mipmap, boolean linear,
                              boolean clamp, boolean close, CallbackInfo ci) {
        // 只翻转图集纹理，且整张上传
        if (!AtlasFlagHolder.isAtlas()) return;
        if (uploadWidth != width || uploadHeight != height) return;
        if (!EasterEggHandler.isUpsideDown) return;

        LOGGER.debug("Flipping atlas texture {}x{}", width, height);
        ((NativeImage) (Object) this).flipY();
    }
}