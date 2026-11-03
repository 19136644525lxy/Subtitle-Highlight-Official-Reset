package Yeah_Zero.Subtitle_Highlight.api;

import Yeah_Zero.Subtitle_Highlight.Configure.Settings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Environment(EnvType.CLIENT)
public class SubtitleAPI {
    /** 渲染线程会遍历该列表，注册可能来自其他模组的初始化线程，故使用写时复制列表。 */
    private static final List<SubtitleProcessor> processors = new CopyOnWriteArrayList<>();

    /**
     * 注册字幕处理器
     * @param processor 字幕处理器
     */
    public static void registerProcessor(SubtitleProcessor processor) {
        if (processor != null) {
            processors.add(processor);
        }
    }

    /**
     * 处理字幕
     * @param text 原始字幕文本
     * @param settings 配置
     * @return 处理后的字幕文本
     */
    public static Component processSubtitle(Component text, Settings settings) {
        Component processedText = text;
        for (SubtitleProcessor processor : processors) {
            processedText = processor.process(processedText, settings);
        }
        return processedText;
    }

    /**
     * 字幕处理器接口
     */
    public interface SubtitleProcessor {
        /**
         * 处理字幕
         * @param text 原始字幕文本
         * @param settings 配置
         * @return 处理后的字幕文本
         */
        Component process(Component text, Settings settings);
    }
}