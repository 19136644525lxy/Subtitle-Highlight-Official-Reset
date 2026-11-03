package Yeah_Zero.Subtitle_Highlight.Mixin;

import Yeah_Zero.Subtitle_Highlight.Configure.Manager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.hud.SubtitlesHud")
public class SubtitlesHudMixin {
    /** 原版字幕条目默认存活时长（毫秒）。 */
    private static final long VANILLA_SUBTITLE_DURATION_MS = 3000;
    private static double durationRatio;

    /**
     * 字幕条目的起始时间字段。开发环境（Yarn 映射）名为 {@code startTime}，
     * 生产环境（Intermediary 映射）名为 {@code field_1472}；两者都尝试，避免开发环境静默退化为固定透明度。
     */
    private static final ClassValue<java.lang.reflect.Field> START_TIME_FIELD = new ClassValue<>() {
        @Override
        protected java.lang.reflect.Field computeValue(Class<?> type) {
            for (String name : new String[]{"startTime", "field_1472"}) {
                try {
                    java.lang.reflect.Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException | RuntimeException ignored) {
                    // 尝试下一个候选名
                }
            }
            return null;
        }
    };

    private static Long readStartTime(Object subtitleEntry) {
        java.lang.reflect.Field field = START_TIME_FIELD.get(subtitleEntry.getClass());
        if (field == null) {
            return null;
        }
        try {
            return field.getLong(subtitleEntry);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @Shadow
    @Final
    private MinecraftClient client;

    @Redirect(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;", ordinal = 0))
    private Object getSubtitleEntry(java.util.Iterator instance) {
        Object subtitleEntry = instance.next();
        Long startTime = readStartTime(subtitleEntry);
        long maxDuration = Manager.settings.maxDuration;
        durationRatio = (startTime == null || maxDuration <= 0)
                ? 0.0
                : (Util.getMeasuringTimeMs() - startTime) / (double) maxDuration;
        return subtitleEntry;
    }

    @Redirect(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/SubtitlesHud$SubtitleEntry;getTime()J"))
    private long redirectSubtitleTime(Object instance) {
        Long startTime = readStartTime(instance);
        if (startTime == null) {
            return 0;
        }
        double notifDisplayTime = this.client.options.getNotificationDisplayTime().getValue();
        return (long) (startTime - VANILLA_SUBTITLE_DURATION_MS * notifDisplayTime + Manager.settings.maxDuration * notifDisplayTime);
    }

    @ModifyVariable(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("STORE"), ordinal = 7)
    private int modifyDirectionDisplayColor(int originalValue) {
        return MathHelper.floor(MathHelper.clampedLerp(255 * Manager.settings.startRatio, 255 * Manager.settings.endRatio, durationRatio));
    }

    @ModifyArgs(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    private void modifySubtitleDisplayColor(Args args) {
        Text text = args.get(1);
        args.set(1, Text.literal(text.getString()).setStyle(text.getStyle().withColor((TextColor) null)));
        int red, green, blue;
        if (text.getStyle().getColor() != null) {
            red = (text.getStyle().getColor().getRgb() >> 16) & 255;
            green = (text.getStyle().getColor().getRgb() >> 8) & 255;
            blue = text.getStyle().getColor().getRgb() & 255;
        } else {
            red = green = blue = 255;
        }
        int redRemaining = MathHelper.floor(MathHelper.clampedLerp(red * Manager.settings.startRatio, red * Manager.settings.endRatio, durationRatio));
        int greenRemaining = MathHelper.floor(MathHelper.clampedLerp(green * Manager.settings.startRatio, green * Manager.settings.endRatio, durationRatio));
        int blueRemaining = MathHelper.floor(MathHelper.clampedLerp(blue * Manager.settings.startRatio, blue * Manager.settings.endRatio, durationRatio));
        args.set(4, (redRemaining << 16 | greenRemaining << 8 | blueRemaining) - 16777216);
    }

    @ModifyArgs(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private void modifyTranslation(Args args) {
        float width = -((float) args.get(0) - this.client.getWindow().getScaledWidth() + 2);
        float height = -((float) args.get(1) - this.client.getWindow().getScaledHeight() + 35);
        args.set(0, this.client.getWindow().getScaledWidth() - (width + 1) * Math.abs(Manager.settings.scale) - Manager.settings.sideMargin);
        args.set(1, this.client.getWindow().getScaledHeight() - (height + 5) * Math.abs(Manager.settings.scale) - Manager.settings.bottomMargin);
    }

    @ModifyArgs(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void modifyScale(Args args) {
        args.set(0, Manager.settings.scale);
        args.set(1, Manager.settings.scale);
    }

    @ModifyArgs(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void modifyFill(Args args) {
        args.set(4, Manager.settings.backgroundColor);
    }
}