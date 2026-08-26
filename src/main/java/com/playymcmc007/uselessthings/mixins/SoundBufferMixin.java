package com.playymcmc007.uselessthings.mixins;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.playymcmc007.uselessthings.client.EasterEggHandler;
import com.playymcmc007.uselessthings.client.ISoundBufferExtension;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import java.nio.ByteBuffer;
import java.util.OptionalInt;

@OnlyIn(Dist.CLIENT)
@Mixin(SoundBuffer.class)
public abstract class SoundBufferMixin implements ISoundBufferExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger("UselessThings/SoundBuffer");

    @Shadow private ByteBuffer data;
    @Shadow private boolean hasAlBuffer;
    @Shadow private int alBuffer;
    @Shadow private AudioFormat format;

    @Unique
    private byte[] uselessThings$originalBytes;

    // ---- 构造函数：备份原始音频字节 ----
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(ByteBuffer p_83798_, AudioFormat p_83799_, CallbackInfo ci) {
        if (p_83798_ != null) {
            byte[] bytes = new byte[p_83798_.remaining()];
            p_83798_.get(bytes);
            p_83798_.rewind();
            this.uselessThings$originalBytes = bytes;
        } else {
            this.uselessThings$originalBytes = new byte[0];
        }
    }

    // ---- 拦截 getAlBuffer，替换为带反转逻辑的实现 ----
    @Inject(method = "getAlBuffer", at = @At("HEAD"), cancellable = true)
    private void onGetAlBuffer(CallbackInfoReturnable<OptionalInt> cir) {
        if (hasAlBuffer) {
            return;
        }
        if (uselessThings$originalBytes == null || uselessThings$originalBytes.length == 0) {
            cir.setReturnValue(OptionalInt.empty());
            return;
        }

        boolean reverse = EasterEggHandler.isUpsideDown;
        byte[] targetBytes = reverse ? reverseAudio(uselessThings$originalBytes, format) : uselessThings$originalBytes.clone();

        ByteBuffer newData = ByteBuffer.allocateDirect(targetBytes.length);
        newData.put(targetBytes);
        newData.rewind();
        this.data = newData;

        int openAlFormat = audioFormatToOpenAl(format);
        int[] bufferId = new int[1];
        AL10.alGenBuffers(bufferId);
        if (checkALError("Creating buffer")) {
            cir.setReturnValue(OptionalInt.empty());
            return;
        }
        AL10.alBufferData(bufferId[0], openAlFormat, this.data, (int) format.getSampleRate());
        if (checkALError("Assigning buffer data")) {
            cir.setReturnValue(OptionalInt.empty());
            return;
        }

        this.alBuffer = bufferId[0];
        this.hasAlBuffer = true;
        this.data = null;
        cir.setReturnValue(OptionalInt.of(this.alBuffer));
    }

    // ---- 实现接口：刷新缓冲区（切换状态时调用） ----
    @Override
    public void uselessThings$refresh(boolean reverse) {
        // 删除旧缓冲区
        if (hasAlBuffer) {
            AL10.alDeleteBuffers(new int[]{alBuffer});
            checkALError("Deleting buffer on refresh");
            hasAlBuffer = false;
            alBuffer = 0;
        }
        // 重新生成
        if (uselessThings$originalBytes == null || uselessThings$originalBytes.length == 0) {
            return;
        }
        byte[] targetBytes = reverse ? reverseAudio(uselessThings$originalBytes, format) : uselessThings$originalBytes.clone();

        ByteBuffer newData = ByteBuffer.allocateDirect(targetBytes.length);
        newData.put(targetBytes);
        newData.rewind();
        this.data = newData;

        int openAlFormat = audioFormatToOpenAl(format);
        int[] bufferId = new int[1];
        AL10.alGenBuffers(bufferId);
        if (checkALError("Creating buffer")) return;
        AL10.alBufferData(bufferId[0], openAlFormat, this.data, (int) format.getSampleRate());
        if (checkALError("Assigning buffer data")) return;

        this.alBuffer = bufferId[0];
        this.hasAlBuffer = true;
        this.data = null;
    }

    // ---- 工具方法 ----
    @Unique
    private static byte[] reverseAudio(byte[] original, AudioFormat format) {
        int channels = format.getChannels();
        int sampleSize = format.getSampleSizeInBits();
        int bytesPerSample = sampleSize / 8;
        int frameSize = channels * bytesPerSample;
        if (frameSize <= 0 || original.length % frameSize != 0) {
            // 如果格式不兼容，回退到逐字节反转（但会失真）
            return reverseBytes(original);
        }
        int frameCount = original.length / frameSize;
        byte[] reversed = new byte[original.length];
        for (int i = 0; i < frameCount; i++) {
            int srcIndex = (frameCount - 1 - i) * frameSize;
            int dstIndex = i * frameSize;
            System.arraycopy(original, srcIndex, reversed, dstIndex, frameSize);
        }
        return reversed;
    }

    // 保留逐字节反转作为备用
    @Unique
    private static byte[] reverseBytes(byte[] original) {
        byte[] rev = new byte[original.length];
        for (int i = 0; i < original.length; i++) {
            rev[i] = original[original.length - 1 - i];
        }
        return rev;
    }

    @Unique
    private static int audioFormatToOpenAl(AudioFormat format) {
        Encoding encoding = format.getEncoding();
        int channels = format.getChannels();
        int sampleSize = format.getSampleSizeInBits();
        if (encoding.equals(Encoding.PCM_UNSIGNED) || encoding.equals(Encoding.PCM_SIGNED)) {
            if (channels == 1) {
                if (sampleSize == 8) return AL10.AL_FORMAT_MONO8;
                if (sampleSize == 16) return AL10.AL_FORMAT_MONO16;
            } else if (channels == 2) {
                if (sampleSize == 8) return AL10.AL_FORMAT_STEREO8;
                if (sampleSize == 16) return AL10.AL_FORMAT_STEREO16;
            }
        }
        throw new IllegalArgumentException("Invalid audio format: " + format);
    }

    @Unique
    private static boolean checkALError(String message) {
        int error = AL10.alGetError();
        if (error != 0) {
            LOGGER.error("{}: {}", message, alErrorToString(error));
            return true;
        }
        return false;
    }

    @Unique
    private static String alErrorToString(int errorCode) {
        switch (errorCode) {
            case 40961: return "Invalid name parameter.";
            case 40962: return "Invalid enumerated parameter value.";
            case 40963: return "Invalid parameter parameter value.";
            case 40964: return "Invalid operation.";
            case 40965: return "Unable to allocate memory.";
            default:    return "An unrecognized error occurred.";
        }
    }
}