package Yeah_Zero.Subtitle_Highlight.Mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防御性补丁：26.x 起 InputConstants.isKeyDown 直接用 SDL 扫描码下标访问键盘状态数组，
 * 任何模组只要把越界的键值（例如旧版的 -1 = GLFW_KEY_UNKNOWN）注册进 KeyMapping，
 * 进入世界时 KeyMapping.setAll 就会抛 IndexOutOfBoundsException 直接崩游戏。
 * 这里对越界值做跳过处理，并记录一次日志，方便定位是哪个键值引起的。
 */
@Mixin(KeyMapping.class)
public class KeyMappingGuardMixin {
    @Unique
    private static final Logger SUBTITLE_HIGHLIGHT_KEY_GUARD_LOGGER = LoggerFactory.getLogger("subtitle_highlight/keyguard");

    @Unique
    private static final Set<Integer> SUBTITLE_HIGHLIGHT_REPORTED_KEYS = ConcurrentHashMap.newKeySet();

    @Redirect(
            method = "setAll",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(I)Z")
    )
    private static boolean subtitle_highlight$guardInvalidKey(int key) {
        if (key < 0 || key >= 512) {
            if (SUBTITLE_HIGHLIGHT_REPORTED_KEYS.add(key)) {
                SUBTITLE_HIGHLIGHT_KEY_GUARD_LOGGER.warn(
                        "Skipped out-of-range key code {} registered by another mod; "
                                + "InputConstants.isKeyDown would have thrown IndexOutOfBoundsException",
                        key);
            }
            return false;
        }
        return InputConstants.isKeyDown(key);
    }
}
