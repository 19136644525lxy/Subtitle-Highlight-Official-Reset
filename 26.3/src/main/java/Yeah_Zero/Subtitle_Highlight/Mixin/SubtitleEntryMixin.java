package Yeah_Zero.Subtitle_Highlight.Mixin;

import Yeah_Zero.Subtitle_Highlight.Configure.Manager;
import Yeah_Zero.Subtitle_Highlight.Configure.Settings;
import Yeah_Zero.Subtitle_Highlight.Data.SubtitleTypeLoader;
import Yeah_Zero.Subtitle_Highlight.Util.ColorCode;
import Yeah_Zero.Subtitle_Highlight.Util.SplitKeyArrays;
import Yeah_Zero.Subtitle_Highlight.api.SubtitleAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.components.SubtitleOverlay$Subtitle")
public class SubtitleEntryMixin {
    @Shadow
    @Final
    private Component text;

    private static final Logger PERF_LOGGER = LoggerFactory.getLogger("subtitle_highlight-performance");
    /**
     * 性能统计默认关闭：字幕渲染是每帧热路径，统计本身会引入计时与全局锁开销。
     * 需要排查时以 -Dsubtitle_highlight.debug.performance=true 启动游戏开启。
     */
    private static final boolean PERF_DEBUG = Boolean.getBoolean("subtitle_highlight.debug.performance");

    private static long totalTime = 0;
    private static int callCount = 0;
    private static long maxTime = 0;
    private static long minTime = Long.MAX_VALUE;
    private static long lastLogTime = 0;
    private static final long LOG_INTERVAL = 10000;

    @Inject(at = @At("RETURN"), method = "getText()Lnet/minecraft/network/chat/Component;", cancellable = true)
    private void colorizeSubtitle(CallbackInfoReturnable<Component> cir) {
        long startTime = PERF_DEBUG ? System.nanoTime() : 0L;

        try {
            MutableComponent subtitleText = ((MutableComponent) this.text).withStyle(ChatFormatting.RESET);
            if (subtitleText.getContents() instanceof TranslatableContents) {
                long customCheckStart = PERF_DEBUG ? System.nanoTime() : 0L;
                for (Settings.Custom custom : Manager.settings.customList) {
                    if (((TranslatableContents) subtitleText.getContents()).getKey().equals(custom.translationKey)) {
                        cir.setReturnValue(subtitleText.setStyle(subtitleText.getStyle().withColor(custom.color).withObfuscated(custom.obfuscated).withBold(custom.bold).withStrikethrough(custom.strikethrough).withUnderlined(custom.underline).withItalic(custom.italic)));
                        return;
                    }
                }
                if (PERF_DEBUG) {
                    long customCheckTime = System.nanoTime() - customCheckStart;
                    if (customCheckTime > 100000) {
                        PERF_LOGGER.warn("Custom list check took {}us for {} entries", customCheckTime / 1000, Manager.settings.customList.size());
                    }
                }

                String[] keyParts = ((TranslatableContents) subtitleText.getContents()).getKey().split("\\.");
                if (keyParts.length > 1 && keyParts[0].equals("subtitles")) {
                    String category = keyParts[1];
                    String subType = keyParts.length > 2 ? keyParts[2] : "";
                    switch (category) {
                        case "ambient", "weather" -> {
                            applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("", "ambient"), Manager.settings.colorSettings.ambient);
                            return;
                        }
                        case "block" -> {
                            if (subType.equals("generic")) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "generic"), Manager.settings.colorSettings.block.generic);
                                return;
                            }
                            String blockKey = subType;
                            String subKey = keyParts.length > 3 ? keyParts[3] : "";
                            
                            if (SplitKeyArrays.interactSet.contains(blockKey)) {
                                if ((blockKey.equals("anvil") && subKey.equals("land")) || (blockKey.equals("tripwire") && subKey.equals("click"))) {
                                    applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "dangerous"), Manager.settings.colorSettings.block.dangerous);
                                } else {
                                    applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "interact"), Manager.settings.colorSettings.block.interact);
                                }
                                return;
                            }
                            if (SplitKeyArrays.workingSet.contains(blockKey)) {
                                if ((blockKey.equals("beacon") && subKey.equals("power_select")) || (blockKey.equals("beehive") && subKey.equals("shear"))) {
                                    applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "interact"), Manager.settings.colorSettings.block.interact);
                                } else if (blockKey.equals("pointed_dripstone") && (subKey.startsWith("drip_lava") || subKey.equals("land"))) {
                                    applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "dangerous"), Manager.settings.colorSettings.block.dangerous);
                                } else {
                                    applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "working"), Manager.settings.colorSettings.block.working);
                                }
                                return;
                            }
                            if (SplitKeyArrays.dangerousBlocksSet.contains(blockKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "dangerous"), Manager.settings.colorSettings.block.dangerous);
                                return;
                            }
                            if (SplitKeyArrays.cropsSet.contains(blockKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "crop"), Manager.settings.colorSettings.block.crop);
                                return;
                            }
                            applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "other"), Manager.settings.colorSettings.block.other);
                            return;
                        }
                        case "chiseled_bookshelf", "ui" -> {
                            applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("block", "interact"), Manager.settings.colorSettings.block.interact);
                            return;
                        }
                        case "enchant", "particle" -> {
                            applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("", "enchant"), Manager.settings.colorSettings.enchant);
                            return;
                        }
                        case "entity" -> {
                            String entityKey = subType;
                            String subKey = keyParts.length > 3 ? keyParts[3] : "";
                            
                            if (entityKey.equals("generic") || entityKey.equals("player")) {
                                if (subKey.equals("attack")) {
                                    applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity.mob.player", "attack"), Manager.settings.colorSettings.entity.mob.player.attack);
                                } else if (SplitKeyArrays.hurtSet.contains(subKey)) {
                                    applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity.mob.player", "hurt"), Manager.settings.colorSettings.entity.mob.player.hurt);
                                } else {
                                    applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity.mob.player", "other"), Manager.settings.colorSettings.entity.mob.player.other);
                                }
                                return;
                            }
                            
                            if (SplitKeyArrays.friendlyMobsSet.contains(entityKey)) {
                                if (entityKey.equals("chicken") && Manager.settings.ikunEasterEgg) {
                                    cir.setReturnValue(Component.translatable("subtitles.entity.kun." + subKey).setStyle(subtitleText.getStyle().withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY)).withBold(true)));
                                } else {
                                    applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity.mob", "passive"), Manager.settings.colorSettings.entity.mob.passive);
                                }
                                return;
                            }
                            if (SplitKeyArrays.neutralMobsSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity.mob", "neutral"), Manager.settings.colorSettings.entity.mob.neutral);
                                return;
                            }
                            if (SplitKeyArrays.hostileMobsSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity.mob", "hostile"), Manager.settings.colorSettings.entity.mob.hostile);
                                return;
                            }
                            if (SplitKeyArrays.bossMobsSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity.mob", "boss"), Manager.settings.colorSettings.entity.mob.boss);
                                return;
                            }
                            if (SplitKeyArrays.vehiclesSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity", "vehicle"), Manager.settings.colorSettings.entity.vehicle);
                                return;
                            }
                            if (SplitKeyArrays.projectilesSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity", "projectile"), Manager.settings.colorSettings.entity.projectile);
                                return;
                            }
                            if (SplitKeyArrays.explosivesSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity", "explosive"), Manager.settings.colorSettings.entity.explosive);
                                return;
                            }
                            if (SplitKeyArrays.decorationsSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity", "decoration"), Manager.settings.colorSettings.entity.decoration);
                                return;
                            }
                            applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity", "other"), Manager.settings.colorSettings.entity.other);
                            return;
                        }
                        case "event" -> {
                            applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("entity.mob", "hostile"), Manager.settings.colorSettings.entity.mob.hostile);
                            return;
                        }
                        case "item" -> {
                            String itemKey = subType;
                            
                            if (SplitKeyArrays.weaponsSet.contains(itemKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("item", "weapon"), Manager.settings.colorSettings.item.weapon);
                                return;
                            }
                            if (SplitKeyArrays.armorsSet.contains(itemKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("item", "armor"), Manager.settings.colorSettings.item.armor);
                                return;
                            }
                            if (SplitKeyArrays.toolsSet.contains(itemKey)) {
                                applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("item", "tool"), Manager.settings.colorSettings.item.tool);
                                return;
                            }
                            applyColor(cir, subtitleText, SubtitleTypeLoader.getColor("item", "other"), Manager.settings.colorSettings.item.other);
                            return;
                        }
                    }
                }
            }
            long apiStart = PERF_DEBUG ? System.nanoTime() : 0L;
            Component processedText = SubtitleAPI.processSubtitle(subtitleText, Manager.settings);
            if (PERF_DEBUG) {
                long apiTime = System.nanoTime() - apiStart;
                if (apiTime > 100000) {
                    PERF_LOGGER.warn("SubtitleAPI.processSubtitle took {}us", apiTime / 1000);
                }
            }
            
            ColorCode colorCode = resolveColor(SubtitleTypeLoader.getColor("", "other"), Manager.settings.colorSettings.other);
            if (colorCode != null) {
                processedText = ((MutableComponent) processedText).withStyle(colorCode.getFormatting());
            }
            
            cir.setReturnValue(processedText);
        } finally {
            if (PERF_DEBUG) {
                recordPerfStats(System.nanoTime() - startTime);
            }
        }
    }

    private static void recordPerfStats(long elapsed) {
        synchronized (SubtitleEntryMixin.class) {
            totalTime += elapsed;
            callCount++;
            maxTime = Math.max(maxTime, elapsed);
            minTime = Math.min(minTime, elapsed);

            long now = System.currentTimeMillis();
            if (now - lastLogTime >= LOG_INTERVAL) {
                if (callCount > 0) {
                    PERF_LOGGER.info("Subtitle colorize stats - Calls: {}, Avg: {}us, Min: {}us, Max: {}us",
                            callCount, String.format("%.2f", (double) totalTime / callCount / 1000), minTime / 1000, maxTime / 1000);
                }
                totalTime = 0;
                callCount = 0;
                maxTime = 0;
                minTime = Long.MAX_VALUE;
                lastLogTime = now;
            }

            if (elapsed > 1000000) {
                PERF_LOGGER.warn("colorizeSubtitle took {}ms - potential performance issue", elapsed / 1000000);
            }
        }
    }
    
    private void applyColor(CallbackInfoReturnable<Component> cir, MutableComponent subtitleText, ColorCode dataColor, ColorCode configColor) {
        ColorCode color = resolveColor(dataColor, configColor);
        if (color != null) {
            cir.setReturnValue(subtitleText.withStyle(color.getFormatting()));
        }
    }

    /**
     * 解析最终颜色。默认配置界面优先；开启 {@code Manager.settings.datapackPriority} 后数据包优先。
     */
    private static ColorCode resolveColor(ColorCode dataColor, ColorCode configColor) {
        if (Manager.settings.datapackPriority) {
            return dataColor != null ? dataColor : configColor;
        }
        return configColor != null ? configColor : dataColor;
    }
}