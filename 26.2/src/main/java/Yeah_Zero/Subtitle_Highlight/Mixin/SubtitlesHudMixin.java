package Yeah_Zero.Subtitle_Highlight.Mixin;

import Yeah_Zero.Subtitle_Highlight.Configure.Manager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * 26.x 版字幕 HUD 修改。
 *
 * <p>1.21.11 及更早版本里，原版 {@code SubtitleOverlay.render(DrawContext)} 逐个绘制字幕行，
 * 本模组通过 {@code @Redirect}/{@code @ModifyArgs} 调整位置、缩放、背景色与文字颜色。
 * 26.1 起原版把渲染拆成两段：{@code extractRenderState(GuiGraphicsExtractor)} 负责收集状态并
 * 直接往 {@code GuiGraphicsExtractor} 里填绘制指令，{@code DrawContext}/{@code PoseStack} 都不再存在。
 * 因此这里的注入点整体改为 {@code extractRenderState}：
 *
 * <ul>
 *   <li>{@code @ModifyConstant} 把写死的 3000ms 字幕存活时长换成配置里的 {@code maxDuration}；</li>
 *   <li>{@code @ModifyArgs} 调整 {@code Matrix3x2fStack.translate/scale} 实现位置与缩放；</li>
 *   <li>{@code @ModifyArgs} 调整 {@code fill} 的背景色；</li>
 *   <li>{@code @ModifyArgs} 调整 {@code text} 的文字颜色，按配置着色且不随播放进度变暗。</li>
 * </ul>
 *
 * <p>原版在该方法里算出一个 0~255 的灰度 {@code alpha}（{@code clampedLerp(255, 75, 已播放比例)}），
 * 再包成 {@code ARGB.color(255, alpha, alpha, alpha)} 传给文字绘制。这里直接从这个入参反推出
 * 「已播放比例」，于是既不用读原版私有字段，也不用再拦 {@code SoundPlayedAt.time}。
 */
@Environment(EnvType.CLIENT)
@Mixin(SubtitleOverlay.class)
public class SubtitlesHudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * 把原版写死的字幕存活时长（3000ms）替换成配置的 {@code maxDuration}。
     * 该方法里 3000.0d 出现两次：一次给 {@code purgeOldInstances} 决定条目何时销毁，
     * 一次给透明度计算当分母，两处都必须换，否则字幕会先淡出、后消失。
     */
    @ModifyConstant(method = "extractRenderState", constant = @Constant(doubleValue = 3000.0D))
    private double modifyDisplayTime(double original) {
        long maxDuration = Manager.settings.maxDuration;
        return maxDuration > 0 ? maxDuration : original;
    }

    /** 调整字幕整体位置（原版按右下角居中排布，这里按配置的边距与缩放重算）。 */
    @ModifyArgs(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;"))
    private void modifyTranslation(Args args) {
        int scaledWidth = this.minecraft.getWindow().getGuiScaledWidth();
        int scaledHeight = this.minecraft.getWindow().getGuiScaledHeight();
        float scale = Math.abs(Manager.settings.scale);
        float width = -((float) args.get(0) - scaledWidth + 2);
        float height = -((float) args.get(1) - scaledHeight + 35);
        args.set(0, scaledWidth - (width + 1) * scale - Manager.settings.sideMargin);
        args.set(1, scaledHeight - (height + 5) * scale - Manager.settings.bottomMargin);
    }

    /** 调整字幕缩放。 */
    @ModifyArgs(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;"))
    private void modifyScale(Args args) {
        args.set(0, Manager.settings.scale);
        args.set(1, Manager.settings.scale);
    }

    /** 调整字幕背景条颜色。 */
    @ModifyArgs(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void modifyFill(Args args) {
        args.set(4, Manager.settings.backgroundColor);
    }

    /**
     * 调整字幕文字颜色：只按配置把字幕染成对应颜色，不做随时间变化的亮度渐变。
     *
     * <p>原版传进来的颜色是 {@code ARGB.color(255, alpha, alpha, alpha)}，alpha 会随播放进度从 255 降到 75，
     * 直接沿用会让字幕逐渐变暗。这里整段换成配置颜色并把 alpha 固定为不透明，字幕结束前颜色保持不变。
     */
    @ModifyArgs(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private void modifySubtitleDisplayColor(Args args) {
        Component text = args.get(1);

        TextColor styleColor = text.getStyle().getColor();
        int rgb = styleColor != null ? styleColor.getValue() & 0xFFFFFF : 0xFFFFFF;

        // 去掉字幕文本自带的颜色，否则原版会用样式色覆盖这里传进去的 ARGB。
        Style stripped = text.getStyle().withColor((TextColor) null);
        args.set(1, Component.literal(text.getString()).withStyle(stripped));

        args.set(4, 0xFF000000 | rgb);
    }
}
