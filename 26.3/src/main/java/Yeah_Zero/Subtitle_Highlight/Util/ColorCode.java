package Yeah_Zero.Subtitle_Highlight.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public enum ColorCode {
    BLACK('0'), DARK_BLUE('1'), DARK_GREEN('2'), DARK_AQUA('3'), DARK_RED('4'), DARK_PURPLE('5'), GOLD('6'), GRAY('7'), DARK_GRAY('8'), BLUE('9'), GREEN('a'), AQUA('b'), RED('c'), LIGHT_PURPLE('d'), YELLOW('e'), WHITE('f');
    private final ChatFormatting formatting;

    ColorCode(char colorCode) {
        this.formatting = ChatFormatting.getByCode(colorCode);
    }

    public static Component formatTranslation(ChatFormatting formatting) {
        return Component.translatable("formatting_code." + formatting.name().toLowerCase(java.util.Locale.ROOT)).withStyle(formatting);
    }

    public static Component colorTranslation(Enum<ColorCode> enumType) {
        if (enumType instanceof ColorCode colorCode) {
            return formatTranslation(colorCode.getFormatting());
        } else {
            return Component.literal("????");
        }
    }

    public ChatFormatting getFormatting() {
        return this.formatting;
    }

    public static ColorCode fromName(String name) {
        for (ColorCode colorCode : ColorCode.values()) {
            if (colorCode.name().toLowerCase().equals(name)) {
                return colorCode;
            }
        }
        return null;
    }
}