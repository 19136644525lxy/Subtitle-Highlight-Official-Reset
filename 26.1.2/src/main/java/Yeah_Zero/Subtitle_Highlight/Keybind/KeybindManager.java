package Yeah_Zero.Subtitle_Highlight.Keybind;

import Yeah_Zero.Subtitle_Highlight.Configure.Manager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class KeybindManager {
    private static KeyMapping toggleSubtitles;
    private static KeyMapping increaseScale;
    private static KeyMapping decreaseScale;
    private static KeyMapping reloadConfig;
    private static KeyMapping saveConfig;
    private static KeyMapping toggleIkunEasterEgg;
    private static KeyMapping increaseOpacity;
    private static KeyMapping decreaseOpacity;

    public static void registerKeybinds() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("subtitle_highlight", "keybinds"));

        toggleSubtitles = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.toggle_subtitles",
                InputConstants.UNKNOWN.getValue(),
                category
        ));

        increaseScale = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.increase_scale",
                InputConstants.UNKNOWN.getValue(),
                category
        ));

        decreaseScale = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.decrease_scale",
                InputConstants.UNKNOWN.getValue(),
                category
        ));

        reloadConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.reload_config",
                InputConstants.UNKNOWN.getValue(),
                category
        ));

        saveConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.save_config",
                InputConstants.UNKNOWN.getValue(),
                category
        ));

        toggleIkunEasterEgg = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.toggle_ikun",
                InputConstants.UNKNOWN.getValue(),
                category
        ));

        increaseOpacity = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.increase_opacity",
                InputConstants.UNKNOWN.getValue(),
                category
        ));

        decreaseOpacity = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.decrease_opacity",
                InputConstants.UNKNOWN.getValue(),
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(KeybindManager::handleKeybinds);
    }

    private static void handleKeybinds(Minecraft client) {
        if (client.player == null) return;

        while (toggleSubtitles.consumeClick()) {
            boolean newValue = !client.options.showSubtitles().get();
            client.options.showSubtitles().set(newValue);
            client.options.save();
            client.player.sendOverlayMessage(Component.translatable(newValue ? "subtitle_highlight.keybind.toggle_subtitles.on" : "subtitle_highlight.keybind.toggle_subtitles.off"));
        }

        while (increaseScale.consumeClick()) {
            Manager.settings.scale = Math.min(Manager.settings.scale + 0.1f, 3.0f);
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.scale.changed", String.format("%.1f", Manager.settings.scale)));
        }

        while (decreaseScale.consumeClick()) {
            Manager.settings.scale = Math.max(Manager.settings.scale - 0.1f, 0.5f);
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.scale.changed", String.format("%.1f", Manager.settings.scale)));
        }

        while (reloadConfig.consumeClick()) {
            Manager.load();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.reload.success"));
        }

        while (saveConfig.consumeClick()) {
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.save.success"));
        }

        while (toggleIkunEasterEgg.consumeClick()) {
            Manager.settings.ikunEasterEgg = !Manager.settings.ikunEasterEgg;
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable(Manager.settings.ikunEasterEgg ? "subtitle_highlight.keybind.ikun.on" : "subtitle_highlight.keybind.ikun.off"));
        }

        while (increaseOpacity.consumeClick()) {
            int alpha = (Manager.settings.backgroundColor >>> 24) & 0xFF;
            alpha = Math.min(alpha + 17, 255);
            Manager.settings.backgroundColor = (alpha << 24) | (Manager.settings.backgroundColor & 0x00FFFFFF);
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.opacity.changed", String.format("%.0f%%", (alpha / 255.0) * 100)));
        }

        while (decreaseOpacity.consumeClick()) {
            int alpha = (Manager.settings.backgroundColor >>> 24) & 0xFF;
            alpha = Math.max(alpha - 17, 0);
            Manager.settings.backgroundColor = (alpha << 24) | (Manager.settings.backgroundColor & 0x00FFFFFF);
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.opacity.changed", String.format("%.0f%%", (alpha / 255.0) * 100)));
        }
    }
}